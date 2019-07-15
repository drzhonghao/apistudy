import org.apache.cassandra.utils.*;


import java.io.IOException;

import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;

public class BooleanSerializer implements IVersionedSerializer<Boolean>
{
    public static BooleanSerializer serializer = new BooleanSerializer();

    public void serialize(Boolean b, DataOutputPlus out, int version) throws IOException
    {
        out.writeBoolean(b);
    }

    public Boolean deserialize(DataInputPlus in, int version) throws IOException
    {
        return in.readBoolean();
    }

    public long serializedSize(Boolean aBoolean, int version)
    {
        return 1;
    }
}
