import org.apache.cassandra.transport.messages.*;


import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import org.apache.cassandra.auth.AuthenticatedUser;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.exceptions.AuthenticationException;
import org.apache.cassandra.metrics.AuthMetrics;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.transport.CBUtil;
import org.apache.cassandra.transport.Message;
import org.apache.cassandra.transport.ProtocolException;
import org.apache.cassandra.transport.ProtocolVersion;

/**
 * Message to indicate that the server is ready to receive requests.
 */
public class CredentialsMessage extends Message.Request
{
    public static final Message.Codec<CredentialsMessage> codec = new Message.Codec<CredentialsMessage>()
    {
        public CredentialsMessage decode(ByteBuf body, ProtocolVersion version)
        {
            if (version.isGreaterThan(ProtocolVersion.V1))
                throw new ProtocolException("Legacy credentials authentication is not supported in " +
                        "protocol versions > 1. Please use SASL authentication via a SaslResponse message");

            Map<String, String> credentials = CBUtil.readStringMap(body);
            return new CredentialsMessage(credentials);
        }

        public void encode(CredentialsMessage msg, ByteBuf dest, ProtocolVersion version)
        {
            CBUtil.writeStringMap(msg.credentials, dest);
        }

        public int encodedSize(CredentialsMessage msg, ProtocolVersion version)
        {
            return CBUtil.sizeOfStringMap(msg.credentials);
        }
    };

    public final Map<String, String> credentials;

    public CredentialsMessage()
    {
        this(new HashMap<String, String>());
    }

    private CredentialsMessage(Map<String, String> credentials)
    {
        super(Message.Type.CREDENTIALS);
        this.credentials = credentials;
    }

    public Message.Response execute(QueryState state, long queryStartNanoTime)
    {
        try
        {
            AuthenticatedUser user = DatabaseDescriptor.getAuthenticator().legacyAuthenticate(credentials);
            state.getClientState().login(user);
            AuthMetrics.instance.markSuccess();
        }
        catch (AuthenticationException e)
        {
            AuthMetrics.instance.markFailure();
            return ErrorMessage.fromException(e);
        }

        return new ReadyMessage();
    }

    @Override
    public String toString()
    {
        return "CREDENTIALS";
    }
}
