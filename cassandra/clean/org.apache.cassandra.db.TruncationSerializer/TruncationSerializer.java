import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.*;


import java.io.IOException;

import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;

/**
 * A truncate operation descriptor
 */
public class Truncation
{
    public static final IVersionedSerializer<Truncation> serializer = new TruncationSerializer();

    public final String keyspace;
    public final String columnFamily;

    public Truncation(String keyspace, String columnFamily)
    {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;
    }

    public MessageOut<Truncation> createMessage()
    {
        return new MessageOut<Truncation>(MessagingService.Verb.TRUNCATE, this, serializer);
    }

    public String toString()
    {
        return "Truncation(" + "keyspace='" + keyspace + '\'' + ", cf='" + columnFamily + "\')";
    }
}

class TruncationSerializer implements IVersionedSerializer<Truncation>
{
    public void serialize(Truncation t, DataOutputPlus out, int version) throws IOException
    {
        out.writeUTF(t.keyspace);
        out.writeUTF(t.columnFamily);
    }

    public Truncation deserialize(DataInputPlus in, int version) throws IOException
    {
        String keyspace = in.readUTF();
        String columnFamily = in.readUTF();
        return new Truncation(keyspace, columnFamily);
    }

    public long serializedSize(Truncation truncation, int version)
    {
        return TypeSizes.sizeof(truncation.keyspace) + TypeSizes.sizeof(truncation.columnFamily);
    }
}
