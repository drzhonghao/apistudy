import org.apache.cassandra.streaming.SessionInfo;
import org.apache.cassandra.streaming.*;


import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Current snapshot of streaming progress.
 */
public class StreamState implements Serializable
{
    public final UUID planId;
    public final String description;
    public final Set<SessionInfo> sessions;

    public StreamState(UUID planId, String description, Set<SessionInfo> sessions)
    {
        this.planId = planId;
        this.description = description;
        this.sessions = sessions;
    }

    public boolean hasFailedSession()
    {
        return Iterables.any(sessions, new Predicate<SessionInfo>()
        {
            public boolean apply(SessionInfo session)
            {
                return session.isFailed();
            }
        });
    }
}
