import org.apache.cassandra.repair.messages.RepairMessage;
import org.apache.cassandra.repair.messages.*;


import java.io.IOException;

import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.repair.RepairJobDesc;

/**
 * ValidationRequest
 *
 * @since 2.0
 */
public class ValidationRequest extends RepairMessage
{
    public static MessageSerializer serializer = new ValidationRequestSerializer();

    public final int gcBefore;

    public ValidationRequest(RepairJobDesc desc, int gcBefore)
    {
        super(Type.VALIDATION_REQUEST, desc);
        this.gcBefore = gcBefore;
    }

    @Override
    public String toString()
    {
        return "ValidationRequest{" +
                "gcBefore=" + gcBefore +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValidationRequest that = (ValidationRequest) o;
        return gcBefore == that.gcBefore;
    }

    @Override
    public int hashCode()
    {
        return gcBefore;
    }

    public static class ValidationRequestSerializer implements MessageSerializer<ValidationRequest>
    {
        public void serialize(ValidationRequest message, DataOutputPlus out, int version) throws IOException
        {
            RepairJobDesc.serializer.serialize(message.desc, out, version);
            out.writeInt(message.gcBefore);
        }

        public ValidationRequest deserialize(DataInputPlus dis, int version) throws IOException
        {
            RepairJobDesc desc = RepairJobDesc.serializer.deserialize(dis, version);
            return new ValidationRequest(desc, dis.readInt());
        }

        public long serializedSize(ValidationRequest message, int version)
        {
            long size = RepairJobDesc.serializer.serializedSize(message.desc, version);
            size += TypeSizes.sizeof(message.gcBefore);
            return size;
        }
    }
}
