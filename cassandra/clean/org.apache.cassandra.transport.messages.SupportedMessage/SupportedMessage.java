import org.apache.cassandra.transport.messages.*;


import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;

import org.apache.cassandra.transport.CBUtil;
import org.apache.cassandra.transport.Message;
import org.apache.cassandra.transport.ProtocolVersion;

/**
 * Message to indicate that the server is ready to receive requests.
 */
public class SupportedMessage extends Message.Response
{
    public static final Message.Codec<SupportedMessage> codec = new Message.Codec<SupportedMessage>()
    {
        public SupportedMessage decode(ByteBuf body, ProtocolVersion version)
        {
            return new SupportedMessage(CBUtil.readStringToStringListMap(body));
        }

        public void encode(SupportedMessage msg, ByteBuf dest, ProtocolVersion version)
        {
            CBUtil.writeStringToStringListMap(msg.supported, dest);
        }

        public int encodedSize(SupportedMessage msg, ProtocolVersion version)
        {
            return CBUtil.sizeOfStringToStringListMap(msg.supported);
        }
    };

    public final Map<String, List<String>> supported;

    public SupportedMessage(Map<String, List<String>> supported)
    {
        super(Message.Type.SUPPORTED);
        this.supported = supported;
    }

    @Override
    public String toString()
    {
        return "SUPPORTED " + supported;
    }
}
