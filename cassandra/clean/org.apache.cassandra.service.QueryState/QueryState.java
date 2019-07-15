import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.*;


import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.cassandra.tracing.Tracing;

/**
 * Represents the state related to a given query.
 */
public class QueryState
{
    private final ClientState clientState;
    private volatile UUID preparedTracingSession;

    public QueryState(ClientState clientState)
    {
        this.clientState = clientState;
    }

    /**
     * @return a QueryState object for internal C* calls (not limited by any kind of auth).
     */
    public static QueryState forInternalCalls()
    {
        return new QueryState(ClientState.forInternalCalls());
    }

    public ClientState getClientState()
    {
        return clientState;
    }

    /**
     * This clock guarantees that updates for the same QueryState will be ordered
     * in the sequence seen, even if multiple updates happen in the same millisecond.
     */
    public long getTimestamp()
    {
        return clientState.getTimestamp();
    }

    public boolean traceNextQuery()
    {
        if (preparedTracingSession != null)
        {
            return true;
        }

        double traceProbability = StorageService.instance.getTraceProbability();
        return traceProbability != 0 && ThreadLocalRandom.current().nextDouble() < traceProbability;
    }

    public void prepareTracingSession(UUID sessionId)
    {
        this.preparedTracingSession = sessionId;
    }

    public void createTracingSession(Map<String,ByteBuffer> customPayload)
    {
        UUID session = this.preparedTracingSession;
        if (session == null)
        {
            Tracing.instance.newSession(customPayload);
        }
        else
        {
            Tracing.instance.newSession(session, customPayload);
            this.preparedTracingSession = null;
        }
    }

    public InetAddress getClientAddress()
    {
        return clientState.isInternal
             ? null
             : clientState.getRemoteAddress().getAddress();
    }
}
