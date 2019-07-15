import org.apache.cassandra.streaming.StreamSummary;
import org.apache.cassandra.streaming.StreamSession;
import org.apache.cassandra.streaming.ProgressInfo;
import org.apache.cassandra.streaming.*;


import java.io.Serializable;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * Stream session info.
 */
public final class SessionInfo implements Serializable
{
    public final InetAddress peer;
    public final int sessionIndex;
    public final InetAddress connecting;
    /** Immutable collection of receiving summaries */
    public final Collection<StreamSummary> receivingSummaries;
    /** Immutable collection of sending summaries*/
    public final Collection<StreamSummary> sendingSummaries;
    /** Current session state */
    public final StreamSession.State state;

    private final Map<String, ProgressInfo> receivingFiles;
    private final Map<String, ProgressInfo> sendingFiles;

    public SessionInfo(InetAddress peer,
                       int sessionIndex,
                       InetAddress connecting,
                       Collection<StreamSummary> receivingSummaries,
                       Collection<StreamSummary> sendingSummaries,
                       StreamSession.State state)
    {
        this.peer = peer;
        this.sessionIndex = sessionIndex;
        this.connecting = connecting;
        this.receivingSummaries = ImmutableSet.copyOf(receivingSummaries);
        this.sendingSummaries = ImmutableSet.copyOf(sendingSummaries);
        this.receivingFiles = new ConcurrentHashMap<>();
        this.sendingFiles = new ConcurrentHashMap<>();
        this.state = state;
    }

    public boolean isFailed()
    {
        return state == StreamSession.State.FAILED;
    }

    /**
     * Update progress of receiving/sending file.
     *
     * @param newProgress new progress info
     */
    public void updateProgress(ProgressInfo newProgress)
    {
        assert peer.equals(newProgress.peer);

        Map<String, ProgressInfo> currentFiles = newProgress.direction == ProgressInfo.Direction.IN
                                                    ? receivingFiles : sendingFiles;
        currentFiles.put(newProgress.fileName, newProgress);
    }

    public Collection<ProgressInfo> getReceivingFiles()
    {
        return receivingFiles.values();
    }

    public Collection<ProgressInfo> getSendingFiles()
    {
        return sendingFiles.values();
    }

    /**
     * @return total number of files already received.
     */
    public long getTotalFilesReceived()
    {
        return getTotalFilesCompleted(receivingFiles.values());
    }

    /**
     * @return total number of files already sent.
     */
    public long getTotalFilesSent()
    {
        return getTotalFilesCompleted(sendingFiles.values());
    }

    /**
     * @return total size(in bytes) already received.
     */
    public long getTotalSizeReceived()
    {
        return getTotalSizeInProgress(receivingFiles.values());
    }

    /**
     * @return total size(in bytes) already sent.
     */
    public long getTotalSizeSent()
    {
        return getTotalSizeInProgress(sendingFiles.values());
    }

    /**
     * @return total number of files to receive in the session
     */
    public long getTotalFilesToReceive()
    {
        return getTotalFiles(receivingSummaries);
    }

    /**
     * @return total number of files to send in the session
     */
    public long getTotalFilesToSend()
    {
        return getTotalFiles(sendingSummaries);
    }

    /**
     * @return total size(in bytes) to receive in the session
     */
    public long getTotalSizeToReceive()
    {
        return getTotalSizes(receivingSummaries);
    }

    /**
     * @return total size(in bytes) to send in the session
     */
    public long getTotalSizeToSend()
    {
        return getTotalSizes(sendingSummaries);
    }

    private long getTotalSizeInProgress(Collection<ProgressInfo> files)
    {
        long total = 0;
        for (ProgressInfo file : files)
            total += file.currentBytes;
        return total;
    }

    private long getTotalFiles(Collection<StreamSummary> summaries)
    {
        long total = 0;
        for (StreamSummary summary : summaries)
            total += summary.files;
        return total;
    }

    private long getTotalSizes(Collection<StreamSummary> summaries)
    {
        long total = 0;
        for (StreamSummary summary : summaries)
            total += summary.totalSize;
        return total;
    }

    private long getTotalFilesCompleted(Collection<ProgressInfo> files)
    {
        Iterable<ProgressInfo> completed = Iterables.filter(files, new Predicate<ProgressInfo>()
        {
            public boolean apply(ProgressInfo input)
            {
                return input.isCompleted();
            }
        });
        return Iterables.size(completed);
    }
}
