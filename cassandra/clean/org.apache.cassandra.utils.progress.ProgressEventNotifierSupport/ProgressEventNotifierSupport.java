import org.apache.cassandra.utils.progress.ProgressListener;
import org.apache.cassandra.utils.progress.ProgressEvent;
import org.apache.cassandra.utils.progress.*;


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Provides basic, thread safe ProgressEvent notification support
 */
public abstract class ProgressEventNotifierSupport implements ProgressEventNotifier
{
    private List<ProgressListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addProgressListener(ProgressListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeProgressListener(ProgressListener listener)
    {
        listeners.remove(listener);
    }

    protected void fireProgressEvent(String tag, ProgressEvent event)
    {
        for (ProgressListener listener : listeners)
        {
            listener.progress(tag, event);
        }
    }
}
