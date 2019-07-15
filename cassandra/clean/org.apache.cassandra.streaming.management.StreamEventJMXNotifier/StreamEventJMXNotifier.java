import org.apache.cassandra.streaming.management.SessionInfoCompositeData;
import org.apache.cassandra.streaming.management.SessionCompleteEventCompositeData;
import org.apache.cassandra.streaming.management.ProgressInfoCompositeData;
import org.apache.cassandra.streaming.management.StreamStateCompositeData;
import org.apache.cassandra.streaming.management.*;


import java.util.concurrent.atomic.AtomicLong;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

import org.apache.cassandra.streaming.*;

/**
 */
public class StreamEventJMXNotifier extends NotificationBroadcasterSupport implements StreamEventHandler
{
    // interval in millisec to use for progress notification
    private static final long PROGRESS_NOTIFICATION_INTERVAL = 1000;

    private final AtomicLong seq = new AtomicLong();

    private long progressLastSent;

    public void handleStreamEvent(StreamEvent event)
    {
        Notification notif = null;
        switch (event.eventType)
        {
            case STREAM_PREPARED:
                notif = new Notification(StreamEvent.SessionPreparedEvent.class.getCanonicalName(),
                                                StreamManagerMBean.OBJECT_NAME,
                                                seq.getAndIncrement());
                notif.setUserData(SessionInfoCompositeData.toCompositeData(event.planId, ((StreamEvent.SessionPreparedEvent) event).session));
                break;
            case STREAM_COMPLETE:
                notif = new Notification(StreamEvent.SessionCompleteEvent.class.getCanonicalName(),
                                                StreamManagerMBean.OBJECT_NAME,
                                                seq.getAndIncrement());
                notif.setUserData(SessionCompleteEventCompositeData.toCompositeData((StreamEvent.SessionCompleteEvent) event));
                break;
            case FILE_PROGRESS:
                ProgressInfo progress = ((StreamEvent.ProgressEvent) event).progress;
                long current = System.currentTimeMillis();
                if (current - progressLastSent >= PROGRESS_NOTIFICATION_INTERVAL || progress.isCompleted())
                {
                    notif = new Notification(StreamEvent.ProgressEvent.class.getCanonicalName(),
                                             StreamManagerMBean.OBJECT_NAME,
                                             seq.getAndIncrement());
                    notif.setUserData(ProgressInfoCompositeData.toCompositeData(event.planId, progress));
                    progressLastSent = System.currentTimeMillis();
                }
                else
                {
                    return;
                }
                break;
        }
        sendNotification(notif);
    }

    public void onSuccess(StreamState result)
    {
        Notification notif = new Notification(StreamEvent.class.getCanonicalName() + ".success",
                                              StreamManagerMBean.OBJECT_NAME,
                                              seq.getAndIncrement());
        notif.setUserData(StreamStateCompositeData.toCompositeData(result));
        sendNotification(notif);
    }

    public void onFailure(Throwable t)
    {
        Notification notif = new Notification(StreamEvent.class.getCanonicalName() + ".failure",
                                              StreamManagerMBean.OBJECT_NAME,
                                              seq.getAndIncrement());
        notif.setUserData(t.fillInStackTrace().toString());
        sendNotification(notif);
    }
}
