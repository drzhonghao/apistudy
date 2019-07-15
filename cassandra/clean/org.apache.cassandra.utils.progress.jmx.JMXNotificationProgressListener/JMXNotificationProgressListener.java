import org.apache.cassandra.utils.progress.jmx.*;


import java.util.Map;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;

import org.apache.cassandra.utils.progress.ProgressEvent;
import org.apache.cassandra.utils.progress.ProgressEventType;
import org.apache.cassandra.utils.progress.ProgressListener;

/**
 * JMXNotificationProgressListener uses JMX Notification API to convert JMX Notification message to progress event
 * and notifies its {@link ProgressListener}s.
 *
 * This is to be implemented in client tools side.
 */
public abstract class JMXNotificationProgressListener implements ProgressListener, NotificationListener
{
    /**
     * @param tag tag name to be checked
     * @return true if given tag for ProgressEvent is a target to consume. If this returns false, then
     *         {@link #progress} is not called for that event.
     */
    public abstract boolean isInterestedIn(String tag);

    /**
     * Called when receiving {@link JMXConnectionNotification#NOTIFS_LOST} message.
     */
    public void handleNotificationLost(long timestamp, String message) {}

    /**
     * Called when JMX connection is closed.
     * Specifically when {@link JMXConnectionNotification#CLOSED} message is received.
     */
    public void handleConnectionClosed(long timestamp, String message) {}

    /**
     * Called when JMX connection is failed.
     * Specifically when {@link JMXConnectionNotification#FAILED} message is received.
     */
    public void handleConnectionFailed(long timestamp, String message) {}

    @SuppressWarnings("unchecked")
    @Override
    public void handleNotification(Notification notification, Object handback)
    {
        switch (notification.getType())
        {
            case "progress":
                String tag = (String) notification.getSource();
                if (this.isInterestedIn(tag))
                {
                    Map<String, Integer> progress = (Map<String, Integer>) notification.getUserData();
                    String message = notification.getMessage();
                    ProgressEvent event = new ProgressEvent(ProgressEventType.values()[progress.get("type")],
                                                            progress.get("progressCount"),
                                                            progress.get("total"),
                                                            message);
                    this.progress(tag, event);
                }
                break;

            case JMXConnectionNotification.NOTIFS_LOST:
                handleNotificationLost(notification.getTimeStamp(), notification.getMessage());
                break;

            case JMXConnectionNotification.FAILED:
                handleConnectionFailed(notification.getTimeStamp(), notification.getMessage());
                break;

            case JMXConnectionNotification.CLOSED:
                handleConnectionClosed(notification.getTimeStamp(), notification.getMessage());
                break;
        }
    }
}
