import org.apache.cassandra.transport.messages.StartupMessage;
import org.apache.cassandra.transport.messages.SupportedMessage;
import org.apache.cassandra.transport.messages.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;

import org.apache.cassandra.cql3.QueryProcessor;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.transport.FrameCompressor;
import org.apache.cassandra.transport.Message;
import org.apache.cassandra.transport.ProtocolVersion;

/**
 * Message to indicate that the server is ready to receive requests.
 */
public class OptionsMessage extends Message.Request
{
    public static final Message.Codec<OptionsMessage> codec = new Message.Codec<OptionsMessage>()
    {
        public OptionsMessage decode(ByteBuf body, ProtocolVersion version)
        {
            return new OptionsMessage();
        }

        public void encode(OptionsMessage msg, ByteBuf dest, ProtocolVersion version)
        {
        }

        public int encodedSize(OptionsMessage msg, ProtocolVersion version)
        {
            return 0;
        }
    };

    public OptionsMessage()
    {
        super(Message.Type.OPTIONS);
    }

    public Message.Response execute(QueryState state, long queryStartNanoTime)
    {
        List<String> cqlVersions = new ArrayList<String>();
        cqlVersions.add(QueryProcessor.CQL_VERSION.toString());

        List<String> compressions = new ArrayList<String>();
        if (FrameCompressor.SnappyCompressor.instance != null)
            compressions.add("snappy");
        // LZ4 is always available since worst case scenario it default to a pure JAVA implem.
        compressions.add("lz4");

        Map<String, List<String>> supported = new HashMap<String, List<String>>();
        supported.put(StartupMessage.CQL_VERSION, cqlVersions);
        supported.put(StartupMessage.COMPRESSION, compressions);
        supported.put(StartupMessage.PROTOCOL_VERSIONS, ProtocolVersion.supportedVersions());

        return new SupportedMessage(supported);
    }

    @Override
    public String toString()
    {
        return "OPTIONS";
    }
}
