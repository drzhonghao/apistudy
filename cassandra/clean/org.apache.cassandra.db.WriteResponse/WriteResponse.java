import org.apache.cassandra.db.*;


import java.io.IOException;

import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;

/*
 * This empty response is sent by a replica to inform the coordinator that the write succeeded
 */
public final class WriteResponse
{
    public static final Serializer serializer = new Serializer();

    private static final WriteResponse instance = new WriteResponse();

    private WriteResponse()
    {
    }

    public static MessageOut<WriteResponse> createMessage()
    {
        return new MessageOut<>(MessagingService.Verb.REQUEST_RESPONSE, instance, serializer);
    }

    public static class Serializer implements IVersionedSerializer<WriteResponse>
    {
        public void serialize(WriteResponse wm, DataOutputPlus out, int version) throws IOException
        {
        }

        public WriteResponse deserialize(DataInputPlus in, int version) throws IOException
        {
            return instance;
        }

        public long serializedSize(WriteResponse response, int version)
        {
            return 0;
        }
    }
}
