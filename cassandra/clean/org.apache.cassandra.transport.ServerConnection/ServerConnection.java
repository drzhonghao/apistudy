import org.apache.cassandra.transport.*;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.channel.Channel;
import org.apache.cassandra.auth.IAuthenticator;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.QueryState;

public class ServerConnection extends Connection
{
    private enum State { UNINITIALIZED, AUTHENTICATION, READY }

    private volatile IAuthenticator.SaslNegotiator saslNegotiator;
    private final ClientState clientState;
    private volatile State state;

    private final ConcurrentMap<Integer, QueryState> queryStates = new ConcurrentHashMap<>();

    public ServerConnection(Channel channel, ProtocolVersion version, Connection.Tracker tracker)
    {
        super(channel, version, tracker);
        this.clientState = ClientState.forExternalCalls(channel.remoteAddress());
        this.state = State.UNINITIALIZED;
    }

    private QueryState getQueryState(int streamId)
    {
        QueryState qState = queryStates.get(streamId);
        if (qState == null)
        {
            // In theory we shouldn't get any race here, but it never hurts to be careful
            QueryState newState = new QueryState(clientState);
            if ((qState = queryStates.putIfAbsent(streamId, newState)) == null)
                qState = newState;
        }
        return qState;
    }

    public QueryState validateNewMessage(Message.Type type, ProtocolVersion version, int streamId)
    {
        switch (state)
        {
            case UNINITIALIZED:
                if (type != Message.Type.STARTUP && type != Message.Type.OPTIONS)
                    throw new ProtocolException(String.format("Unexpected message %s, expecting STARTUP or OPTIONS", type));
                break;
            case AUTHENTICATION:
                // Support both SASL auth from protocol v2 and the older style Credentials auth from v1
                if (type != Message.Type.AUTH_RESPONSE && type != Message.Type.CREDENTIALS)
                    throw new ProtocolException(String.format("Unexpected message %s, expecting %s", type, version == ProtocolVersion.V1 ? "CREDENTIALS" : "SASL_RESPONSE"));
                break;
            case READY:
                if (type == Message.Type.STARTUP)
                    throw new ProtocolException("Unexpected message STARTUP, the connection is already initialized");
                break;
            default:
                throw new AssertionError();
        }
        return getQueryState(streamId);
    }

    public void applyStateTransition(Message.Type requestType, Message.Type responseType)
    {
        switch (state)
        {
            case UNINITIALIZED:
                if (requestType == Message.Type.STARTUP)
                {
                    if (responseType == Message.Type.AUTHENTICATE)
                        state = State.AUTHENTICATION;
                    else if (responseType == Message.Type.READY)
                        state = State.READY;
                }
                break;
            case AUTHENTICATION:
                // Support both SASL auth from protocol v2 and the older style Credentials auth from v1
                assert requestType == Message.Type.AUTH_RESPONSE || requestType == Message.Type.CREDENTIALS;

                if (responseType == Message.Type.READY || responseType == Message.Type.AUTH_SUCCESS)
                {
                    state = State.READY;
                    // we won't use the authenticator again, null it so that it can be GC'd
                    saslNegotiator = null;
                }
                break;
            case READY:
                break;
            default:
                throw new AssertionError();
        }
    }

    public IAuthenticator.SaslNegotiator getSaslNegotiator(QueryState queryState)
    {
        if (saslNegotiator == null)
            saslNegotiator = DatabaseDescriptor.getAuthenticator().newSaslNegotiator(queryState.getClientAddress());
        return saslNegotiator;
    }
}
