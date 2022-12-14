import org.apache.cassandra.utils.progress.jmx.*;


import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.apache.cassandra.utils.progress.ProgressEvent;
import org.apache.cassandra.utils.progress.ProgressListener;

import static org.apache.cassandra.service.ActiveRepairService.Status;

/**
 * ProgressListener that translates ProgressEvent to legacy JMX Notification message (backward compatibility support)
 */
public class LegacyJMXProgressSupport implements ProgressListener
{
    protected static final Pattern SESSION_FAILED_MATCHER = Pattern.compile("Repair session .* for range .* failed with error .*");
    protected static final Pattern SESSION_SUCCESS_MATCHER = Pattern.compile("Repair session .* for range .* finished");

    private final AtomicLong notificationSerialNumber = new AtomicLong();
    private final ObjectName jmxObjectName;

    private final NotificationBroadcasterSupport broadcaster;

    public LegacyJMXProgressSupport(NotificationBroadcasterSupport broadcaster,
                                    ObjectName jmxObjectName)
    {
        this.broadcaster = broadcaster;
        this.jmxObjectName = jmxObjectName;
    }

    @Override
    public void progress(String tag, ProgressEvent event)
    {
        if (tag.startsWith("repair:"))
        {
            Optional<int[]> legacyUserData = getLegacyUserdata(tag, event);
            if (legacyUserData.isPresent())
            {
                Notification jmxNotification = new Notification("repair", jmxObjectName, notificationSerialNumber.incrementAndGet(), event.getMessage());
                jmxNotification.setUserData(legacyUserData.get());
                broadcaster.sendNotification(jmxNotification);
            }
        }
    }

    protected static Optional<int[]> getLegacyUserdata(String tag, ProgressEvent event)
    {
        Optional<Status> status = getStatus(event);
        if (status.isPresent())
        {
            int[] result = new int[2];
            result[0] = getCmd(tag);
            result[1] = status.get().ordinal();
            return Optional.of(result);
        }
        return Optional.empty();
    }

    protected static Optional<Status> getStatus(ProgressEvent event)
    {
        switch (event.getType())
        {
            case START:
                return Optional.of(Status.STARTED);
            case COMPLETE:
                return Optional.of(Status.FINISHED);
            case PROGRESS:
                if (SESSION_FAILED_MATCHER.matcher(event.getMessage()).matches())
                {
                    return Optional.of(Status.SESSION_FAILED);
                }
                else if (SESSION_SUCCESS_MATCHER.matcher(event.getMessage()).matches())
                {
                    return Optional.of(Status.SESSION_SUCCESS);
                }
        }

        return Optional.empty();
    }

    protected static int getCmd(String tag)
    {
        return Integer.parseInt(tag.split(":")[1]);
    }
}
