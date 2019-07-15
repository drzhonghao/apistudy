import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.*;


import java.io.IOException;

import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;

/**
 * This message is sent back the truncate operation and basically specifies if
 * the truncate succeeded.
 */
public class TruncateResponse
{
    public static final TruncateResponseSerializer serializer = new TruncateResponseSerializer();

    public final String keyspace;
    public final String columnFamily;
    public final boolean success;

    public TruncateResponse(String keyspace, String columnFamily, boolean success)
    {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;
        this.success = success;
    }

    public MessageOut<TruncateResponse> createMessage()
    {
        return new MessageOut<TruncateResponse>(MessagingService.Verb.REQUEST_RESPONSE, this, serializer);
    }

    public static class TruncateResponseSerializer implements IVersionedSerializer<TruncateResponse>
    {
        public void serialize(TruncateResponse tr, DataOutputPlus out, int version) throws IOException
        {
            out.writeUTF(tr.keyspace);
            out.writeUTF(tr.columnFamily);
            out.writeBoolean(tr.success);
        }

        public TruncateResponse deserialize(DataInputPlus in, int version) throws IOException
        {
            String keyspace = in.readUTF();
            String columnFamily = in.readUTF();
            boolean success = in.readBoolean();
            return new TruncateResponse(keyspace, columnFamily, success);
        }

        public long serializedSize(TruncateResponse tr, int version)
        {
            return TypeSizes.sizeof(tr.keyspace)
                 + TypeSizes.sizeof(tr.columnFamily)
                 + TypeSizes.sizeof(tr.success);
        }
    }
}
