import org.apache.cassandra.streaming.messages.*;


import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.DataOutputBufferFixed;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.CompactEndpointSerializationHelper;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.utils.UUIDSerializer;

/**
 * StreamInitMessage is first sent from the node where {@link org.apache.cassandra.streaming.StreamSession} is started,
 * to initiate corresponding {@link org.apache.cassandra.streaming.StreamSession} on the other side.
 */
public class StreamInitMessage
{
    public static IVersionedSerializer<StreamInitMessage> serializer = new StreamInitMessageSerializer();

    public final InetAddress from;
    public final int sessionIndex;
    public final UUID planId;
    public final String description;

    // true if this init message is to connect for outgoing message on receiving side
    public final boolean isForOutgoing;
    public final boolean keepSSTableLevel;
    public final boolean isIncremental;

    public StreamInitMessage(InetAddress from, int sessionIndex, UUID planId, String description, boolean isForOutgoing, boolean keepSSTableLevel, boolean isIncremental)
    {
        this.from = from;
        this.sessionIndex = sessionIndex;
        this.planId = planId;
        this.description = description;
        this.isForOutgoing = isForOutgoing;
        this.keepSSTableLevel = keepSSTableLevel;
        this.isIncremental = isIncremental;
    }

    /**
     * Create serialized message.
     *
     * @param compress true if message is compressed
     * @param version Streaming protocol version
     * @return serialized message in ByteBuffer format
     */
    public ByteBuffer createMessage(boolean compress, int version)
    {
        int header = 0;
        // set compression bit.
        if (compress)
            header |= 4;
        // set streaming bit
        header |= 8;
        // Setting up the version bit
        header |= (version << 8);

        byte[] bytes;
        try
        {
            int size = (int)StreamInitMessage.serializer.serializedSize(this, version);
            try (DataOutputBuffer buffer = new DataOutputBufferFixed(size))
            {
                StreamInitMessage.serializer.serialize(this, buffer, version);
                bytes = buffer.getData();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        assert bytes.length > 0;

        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + bytes.length);
        buffer.putInt(MessagingService.PROTOCOL_MAGIC);
        buffer.putInt(header);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    private static class StreamInitMessageSerializer implements IVersionedSerializer<StreamInitMessage>
    {
        public void serialize(StreamInitMessage message, DataOutputPlus out, int version) throws IOException
        {
            CompactEndpointSerializationHelper.serialize(message.from, out);
            out.writeInt(message.sessionIndex);
            UUIDSerializer.serializer.serialize(message.planId, out, MessagingService.current_version);
            out.writeUTF(message.description);
            out.writeBoolean(message.isForOutgoing);
            out.writeBoolean(message.keepSSTableLevel);
            out.writeBoolean(message.isIncremental);
        }

        public StreamInitMessage deserialize(DataInputPlus in, int version) throws IOException
        {
            InetAddress from = CompactEndpointSerializationHelper.deserialize(in);
            int sessionIndex = in.readInt();
            UUID planId = UUIDSerializer.serializer.deserialize(in, MessagingService.current_version);
            String description = in.readUTF();
            boolean sentByInitiator = in.readBoolean();
            boolean keepSSTableLevel = in.readBoolean();
            boolean isIncremental = in.readBoolean();
            return new StreamInitMessage(from, sessionIndex, planId, description, sentByInitiator, keepSSTableLevel, isIncremental);
        }

        public long serializedSize(StreamInitMessage message, int version)
        {
            long size = CompactEndpointSerializationHelper.serializedSize(message.from);
            size += TypeSizes.sizeof(message.sessionIndex);
            size += UUIDSerializer.serializer.serializedSize(message.planId, MessagingService.current_version);
            size += TypeSizes.sizeof(message.description);
            size += TypeSizes.sizeof(message.isForOutgoing);
            size += TypeSizes.sizeof(message.keepSSTableLevel);
            size += TypeSizes.sizeof(message.isIncremental);
            return size;
        }
    }
}
