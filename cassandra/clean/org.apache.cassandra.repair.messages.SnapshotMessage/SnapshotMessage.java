import org.apache.cassandra.repair.messages.RepairMessage;
import org.apache.cassandra.repair.messages.*;


import java.io.IOException;
import java.util.Objects;

import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.repair.RepairJobDesc;

public class SnapshotMessage extends RepairMessage
{
    public final static MessageSerializer serializer = new SnapshotMessageSerializer();

    public SnapshotMessage(RepairJobDesc desc)
    {
        super(Type.SNAPSHOT, desc);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof SnapshotMessage))
            return false;
        SnapshotMessage other = (SnapshotMessage) o;
        return messageType == other.messageType;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(messageType);
    }

    public static class SnapshotMessageSerializer implements MessageSerializer<SnapshotMessage>
    {
        public void serialize(SnapshotMessage message, DataOutputPlus out, int version) throws IOException
        {
            RepairJobDesc.serializer.serialize(message.desc, out, version);
        }

        public SnapshotMessage deserialize(DataInputPlus in, int version) throws IOException
        {
            RepairJobDesc desc = RepairJobDesc.serializer.deserialize(in, version);
            return new SnapshotMessage(desc);
        }

        public long serializedSize(SnapshotMessage message, int version)
        {
            return RepairJobDesc.serializer.serializedSize(message.desc, version);
        }
    }
}
