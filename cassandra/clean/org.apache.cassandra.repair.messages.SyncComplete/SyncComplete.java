import org.apache.cassandra.repair.messages.RepairMessage;
import org.apache.cassandra.repair.messages.*;


import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;

import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.repair.NodePair;
import org.apache.cassandra.repair.RepairJobDesc;

/**
 *
 * @since 2.0
 */
public class SyncComplete extends RepairMessage
{
    public static final MessageSerializer serializer = new SyncCompleteSerializer();

    /** nodes that involved in this sync */
    public final NodePair nodes;
    /** true if sync success, false otherwise */
    public final boolean success;

    public SyncComplete(RepairJobDesc desc, NodePair nodes, boolean success)
    {
        super(Type.SYNC_COMPLETE, desc);
        this.nodes = nodes;
        this.success = success;
    }

    public SyncComplete(RepairJobDesc desc, InetAddress endpoint1, InetAddress endpoint2, boolean success)
    {
        super(Type.SYNC_COMPLETE, desc);
        this.nodes = new NodePair(endpoint1, endpoint2);
        this.success = success;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof SyncComplete))
            return false;
        SyncComplete other = (SyncComplete)o;
        return messageType == other.messageType &&
               desc.equals(other.desc) &&
               success == other.success &&
               nodes.equals(other.nodes);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(messageType, desc, success, nodes);
    }

    private static class SyncCompleteSerializer implements MessageSerializer<SyncComplete>
    {
        public void serialize(SyncComplete message, DataOutputPlus out, int version) throws IOException
        {
            RepairJobDesc.serializer.serialize(message.desc, out, version);
            NodePair.serializer.serialize(message.nodes, out, version);
            out.writeBoolean(message.success);
        }

        public SyncComplete deserialize(DataInputPlus in, int version) throws IOException
        {
            RepairJobDesc desc = RepairJobDesc.serializer.deserialize(in, version);
            NodePair nodes = NodePair.serializer.deserialize(in, version);
            return new SyncComplete(desc, nodes, in.readBoolean());
        }

        public long serializedSize(SyncComplete message, int version)
        {
            long size = RepairJobDesc.serializer.serializedSize(message.desc, version);
            size += NodePair.serializer.serializedSize(message.nodes, version);
            size += TypeSizes.sizeof(message.success);
            return size;
        }
    }
}
