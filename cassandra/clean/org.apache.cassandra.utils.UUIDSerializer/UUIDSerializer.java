import org.apache.cassandra.utils.*;


import java.io.IOException;
import java.util.UUID;

import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;

public class UUIDSerializer implements IVersionedSerializer<UUID>
{
    public static UUIDSerializer serializer = new UUIDSerializer();

    public void serialize(UUID uuid, DataOutputPlus out, int version) throws IOException
    {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    public UUID deserialize(DataInputPlus in, int version) throws IOException
    {
        return new UUID(in.readLong(), in.readLong());
    }

    public long serializedSize(UUID uuid, int version)
    {
        return TypeSizes.sizeof(uuid.getMostSignificantBits()) + TypeSizes.sizeof(uuid.getLeastSignificantBits());
    }
}
