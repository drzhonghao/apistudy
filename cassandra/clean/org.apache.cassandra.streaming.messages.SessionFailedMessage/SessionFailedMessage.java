import org.apache.cassandra.streaming.messages.StreamMessage;
import org.apache.cassandra.streaming.messages.*;


import java.nio.channels.ReadableByteChannel;

import org.apache.cassandra.io.util.DataOutputStreamPlus;
import org.apache.cassandra.streaming.StreamSession;

public class SessionFailedMessage extends StreamMessage
{
    public static Serializer<SessionFailedMessage> serializer = new Serializer<SessionFailedMessage>()
    {
        public SessionFailedMessage deserialize(ReadableByteChannel in, int version, StreamSession session)
        {
            return new SessionFailedMessage();
        }

        public void serialize(SessionFailedMessage message, DataOutputStreamPlus out, int version, StreamSession session) {}
    };

    public SessionFailedMessage()
    {
        super(Type.SESSION_FAILED);
    }

    @Override
    public String toString()
    {
        return "Session Failed";
    }
}
