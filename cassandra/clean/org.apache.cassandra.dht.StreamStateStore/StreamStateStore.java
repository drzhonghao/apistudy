import org.apache.cassandra.dht.*;


import java.util.Set;

import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.streaming.StreamEvent;
import org.apache.cassandra.streaming.StreamEventHandler;
import org.apache.cassandra.streaming.StreamRequest;
import org.apache.cassandra.streaming.StreamState;

/**
 * Store and update available ranges (data already received) to system keyspace.
 */
public class StreamStateStore implements StreamEventHandler
{
    public Set<Range<Token>> getAvailableRanges(String keyspace, IPartitioner partitioner)
    {
        return SystemKeyspace.getAvailableRanges(keyspace, partitioner);
    }

    /**
     * Check if given token's data is available in this node.
     *
     * @param keyspace keyspace name
     * @param token token to check
     * @return true if given token in the keyspace is already streamed and ready to be served.
     */
    public boolean isDataAvailable(String keyspace, Token token)
    {
        Set<Range<Token>> availableRanges = getAvailableRanges(keyspace, token.getPartitioner());
        for (Range<Token> range : availableRanges)
        {
            if (range.contains(token))
                return true;
        }
        return false;
    }

    /**
     * When StreamSession completes, make all keyspaces/ranges in session available to be served.
     *
     * @param event Stream event.
     */
    @Override
    public void handleStreamEvent(StreamEvent event)
    {
        if (event.eventType == StreamEvent.Type.STREAM_COMPLETE)
        {
            StreamEvent.SessionCompleteEvent se = (StreamEvent.SessionCompleteEvent) event;
            if (se.success)
            {
                Set<String> keyspaces = se.transferredRangesPerKeyspace.keySet();
                for (String keyspace : keyspaces)
                {
                    SystemKeyspace.updateTransferredRanges(se.description, se.peer, keyspace, se.transferredRangesPerKeyspace.get(keyspace));
                }
                for (StreamRequest request : se.requests)
                {
                    SystemKeyspace.updateAvailableRanges(request.keyspace, request.ranges);
                }
            }
        }
    }

    @Override
    public void onSuccess(StreamState streamState) {}

    @Override
    public void onFailure(Throwable throwable) {}
}
