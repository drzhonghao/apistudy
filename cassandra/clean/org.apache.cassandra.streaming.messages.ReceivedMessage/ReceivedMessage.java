import org.apache.cassandra.streaming.messages.StreamMessage;
import org.apache.cassandra.streaming.messages.*;


import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.UUID;

import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataInputPlus.DataInputStreamPlus;
import org.apache.cassandra.io.util.DataOutputStreamPlus;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.streaming.StreamSession;
import org.apache.cassandra.utils.UUIDSerializer;

public class ReceivedMessage extends StreamMessage
{
    public static Serializer<ReceivedMessage> serializer = new Serializer<ReceivedMessage>()
    {
        @SuppressWarnings("resource") // Not closing constructed DataInputPlus's as the channel needs to remain open.
        public ReceivedMessage deserialize(ReadableByteChannel in, int version, StreamSession session) throws IOException
        {
            DataInputPlus input = new DataInputStreamPlus(Channels.newInputStream(in));
            return new ReceivedMessage(UUIDSerializer.serializer.deserialize(input, MessagingService.current_version), input.readInt());
        }

        public void serialize(ReceivedMessage message, DataOutputStreamPlus out, int version, StreamSession session) throws IOException
        {
            UUIDSerializer.serializer.serialize(message.cfId, out, MessagingService.current_version);
            out.writeInt(message.sequenceNumber);
        }
    };

    public final UUID cfId;
    public final int sequenceNumber;

    public ReceivedMessage(UUID cfId, int sequenceNumber)
    {
        super(Type.RECEIVED);
        this.cfId = cfId;
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("Received (");
        sb.append(cfId).append(", #").append(sequenceNumber).append(')');
        return sb.toString();
    }
}
