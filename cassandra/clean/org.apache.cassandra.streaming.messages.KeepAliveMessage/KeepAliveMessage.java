import org.apache.cassandra.streaming.messages.StreamMessage;
import org.apache.cassandra.streaming.messages.*;


import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import org.apache.cassandra.io.util.DataOutputStreamPlus;
import org.apache.cassandra.streaming.StreamSession;

public class KeepAliveMessage extends StreamMessage
{
    public static Serializer<KeepAliveMessage> serializer = new Serializer<KeepAliveMessage>()
    {
        public KeepAliveMessage deserialize(ReadableByteChannel in, int version, StreamSession session) throws IOException
        {
            return new KeepAliveMessage();
        }

        public void serialize(KeepAliveMessage message, DataOutputStreamPlus out, int version, StreamSession session)
        {}
    };

    public KeepAliveMessage()
    {
        super(Type.KEEP_ALIVE);
    }

    public String toString()
    {
        return "keep-alive";
    }
}
