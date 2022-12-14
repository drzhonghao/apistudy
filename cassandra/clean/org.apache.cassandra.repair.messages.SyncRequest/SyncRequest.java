import org.apache.cassandra.repair.messages.RepairMessage;
import org.apache.cassandra.repair.messages.*;


import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.CompactEndpointSerializationHelper;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.repair.RepairJobDesc;

/**
 * Body part of SYNC_REQUEST repair message.
 * Request {@code src} node to sync data with {@code dst} node for range {@code ranges}.
 *
 * @since 2.0
 */
public class SyncRequest extends RepairMessage
{
    public static MessageSerializer serializer = new SyncRequestSerializer();

    public final InetAddress initiator;
    public final InetAddress src;
    public final InetAddress dst;
    public final Collection<Range<Token>> ranges;

    public SyncRequest(RepairJobDesc desc, InetAddress initiator, InetAddress src, InetAddress dst, Collection<Range<Token>> ranges)
    {
        super(Type.SYNC_REQUEST, desc);
        this.initiator = initiator;
        this.src = src;
        this.dst = dst;
        this.ranges = ranges;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof SyncRequest))
            return false;
        SyncRequest req = (SyncRequest)o;
        return messageType == req.messageType &&
               desc.equals(req.desc) &&
               initiator.equals(req.initiator) &&
               src.equals(req.src) &&
               dst.equals(req.dst) &&
               ranges.equals(req.ranges);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(messageType, desc, initiator, src, dst, ranges);
    }

    public static class SyncRequestSerializer implements MessageSerializer<SyncRequest>
    {
        public void serialize(SyncRequest message, DataOutputPlus out, int version) throws IOException
        {
            RepairJobDesc.serializer.serialize(message.desc, out, version);
            CompactEndpointSerializationHelper.serialize(message.initiator, out);
            CompactEndpointSerializationHelper.serialize(message.src, out);
            CompactEndpointSerializationHelper.serialize(message.dst, out);
            out.writeInt(message.ranges.size());
            for (Range<Token> range : message.ranges)
            {
                MessagingService.validatePartitioner(range);
                AbstractBounds.tokenSerializer.serialize(range, out, version);
            }
        }

        public SyncRequest deserialize(DataInputPlus in, int version) throws IOException
        {
            RepairJobDesc desc = RepairJobDesc.serializer.deserialize(in, version);
            InetAddress owner = CompactEndpointSerializationHelper.deserialize(in);
            InetAddress src = CompactEndpointSerializationHelper.deserialize(in);
            InetAddress dst = CompactEndpointSerializationHelper.deserialize(in);
            int rangesCount = in.readInt();
            List<Range<Token>> ranges = new ArrayList<>(rangesCount);
            for (int i = 0; i < rangesCount; ++i)
                ranges.add((Range<Token>) AbstractBounds.tokenSerializer.deserialize(in, MessagingService.globalPartitioner(), version));
            return new SyncRequest(desc, owner, src, dst, ranges);
        }

        public long serializedSize(SyncRequest message, int version)
        {
            long size = RepairJobDesc.serializer.serializedSize(message.desc, version);
            size += 3 * CompactEndpointSerializationHelper.serializedSize(message.initiator);
            size += TypeSizes.sizeof(message.ranges.size());
            for (Range<Token> range : message.ranges)
                size += AbstractBounds.tokenSerializer.serializedSize(range, version);
            return size;
        }
    }

    @Override
    public String toString()
    {
        return "SyncRequest{" +
                "initiator=" + initiator +
                ", src=" + src +
                ", dst=" + dst +
                ", ranges=" + ranges +
                "} " + super.toString();
    }
}
