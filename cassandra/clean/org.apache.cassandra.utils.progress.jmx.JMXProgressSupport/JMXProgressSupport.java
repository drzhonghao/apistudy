import org.apache.cassandra.utils.progress.jmx.*;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

import org.apache.cassandra.utils.progress.ProgressEvent;
import org.apache.cassandra.utils.progress.ProgressListener;

/**
 * ProgressListener that translates ProgressEvent to JMX Notification message.
 */
public class JMXProgressSupport implements ProgressListener
{
    private final AtomicLong notificationSerialNumber = new AtomicLong();

    private final NotificationBroadcasterSupport broadcaster;

    public JMXProgressSupport(NotificationBroadcasterSupport broadcaster)
    {
        this.broadcaster = broadcaster;
    }

    @Override
    public void progress(String tag, ProgressEvent event)
    {
        Notification notification = new Notification("progress",
                                                     tag,
                                                     notificationSerialNumber.getAndIncrement(),
                                                     System.currentTimeMillis(),
                                                     event.getMessage());
        Map<String, Integer> userData = new HashMap<>();
        userData.put("type", event.getType().ordinal());
        userData.put("progressCount", event.getProgressCount());
        userData.put("total", event.getTotal());
        notification.setUserData(userData);
        broadcaster.sendNotification(notification);
    }
}
