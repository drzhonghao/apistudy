import org.apache.cassandra.repair.messages.RepairMessage;
import org.apache.cassandra.repair.messages.*;


import java.io.IOException;
import java.util.Objects;

import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.repair.RepairJobDesc;
import org.apache.cassandra.utils.MerkleTrees;

/**
 * ValidationComplete message is sent when validation compaction completed successfully.
 *
 * @since 2.0
 */
public class ValidationComplete extends RepairMessage
{
    public static MessageSerializer serializer = new ValidationCompleteSerializer();

    /** Merkle hash tree response. Null if validation failed. */
    public final MerkleTrees trees;

    public ValidationComplete(RepairJobDesc desc)
    {
        super(Type.VALIDATION_COMPLETE, desc);
        trees = null;
    }

    public ValidationComplete(RepairJobDesc desc, MerkleTrees trees)
    {
        super(Type.VALIDATION_COMPLETE, desc);
        assert trees != null;
        this.trees = trees;
    }

    public boolean success()
    {
        return trees != null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof ValidationComplete))
            return false;

        ValidationComplete other = (ValidationComplete)o;
        return messageType == other.messageType &&
               desc.equals(other.desc);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(messageType, desc);
    }

    private static class ValidationCompleteSerializer implements MessageSerializer<ValidationComplete>
    {
        public void serialize(ValidationComplete message, DataOutputPlus out, int version) throws IOException
        {
            RepairJobDesc.serializer.serialize(message.desc, out, version);
            out.writeBoolean(message.success());
            if (message.trees != null)
                MerkleTrees.serializer.serialize(message.trees, out, version);
        }

        public ValidationComplete deserialize(DataInputPlus in, int version) throws IOException
        {
            RepairJobDesc desc = RepairJobDesc.serializer.deserialize(in, version);
            boolean success = in.readBoolean();

            if (success)
            {
                MerkleTrees trees = MerkleTrees.serializer.deserialize(in, version);
                return new ValidationComplete(desc, trees);
            }

            return new ValidationComplete(desc);
        }

        public long serializedSize(ValidationComplete message, int version)
        {
            long size = RepairJobDesc.serializer.serializedSize(message.desc, version);
            size += TypeSizes.sizeof(message.success());
            if (message.trees != null)
                size += MerkleTrees.serializer.serializedSize(message.trees, version);
            return size;
        }
    }
}
