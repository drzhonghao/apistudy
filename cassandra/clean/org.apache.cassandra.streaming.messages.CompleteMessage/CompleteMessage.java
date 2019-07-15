import org.apache.cassandra.streaming.messages.StreamMessage;
import org.apache.cassandra.streaming.messages.*;


import java.nio.channels.ReadableByteChannel;

import org.apache.cassandra.io.util.DataOutputStreamPlus;
import org.apache.cassandra.streaming.StreamSession;

public class CompleteMessage extends StreamMessage
{
    public static Serializer<CompleteMessage> serializer = new Serializer<CompleteMessage>()
    {
        public CompleteMessage deserialize(ReadableByteChannel in, int version, StreamSession session)
        {
            return new CompleteMessage();
        }

        public void serialize(CompleteMessage message, DataOutputStreamPlus out, int version, StreamSession session) {}
    };

    public CompleteMessage()
    {
        super(Type.COMPLETE);
    }

    @Override
    public String toString()
    {
        return "Complete";
    }
}
