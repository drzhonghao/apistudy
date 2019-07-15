import org.apache.cassandra.hints.*;


import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;

/**
 * An empty successful response to a HintMessage.
 */
public final class HintResponse
{
    public static final IVersionedSerializer<HintResponse> serializer = new Serializer();

    static final HintResponse instance = new HintResponse();
    static final MessageOut<HintResponse> message =
        new MessageOut<>(MessagingService.Verb.REQUEST_RESPONSE, instance, serializer);

    private HintResponse()
    {
    }

    private static final class Serializer implements IVersionedSerializer<HintResponse>
    {
        public long serializedSize(HintResponse response, int version)
        {
            return 0;
        }

        public void serialize(HintResponse response, DataOutputPlus out, int version)
        {
        }

        public HintResponse deserialize(DataInputPlus in, int version)
        {
            return instance;
        }
    }
}
