import org.apache.cassandra.utils.progress.*;


/**
 * Progress event
 */
public class ProgressEvent
{
    private final ProgressEventType type;
    private final int progressCount;
    private final int total;
    private final String message;

    public static ProgressEvent createNotification(String message)
    {
        return new ProgressEvent(ProgressEventType.NOTIFICATION, 0, 0, message);
    }

    public ProgressEvent(ProgressEventType type, int progressCount, int total)
    {
        this(type, progressCount, total, null);
    }

    public ProgressEvent(ProgressEventType type, int progressCount, int total, String message)
    {
        this.type = type;
        this.progressCount = progressCount;
        this.total = total;
        this.message = message;
    }

    public ProgressEventType getType()
    {
        return type;
    }

    public int getProgressCount()
    {
        return progressCount;
    }

    public int getTotal()
    {
        return total;
    }

    public double getProgressPercentage()
    {
        return total != 0 ? progressCount * 100 / (double) total : 0;
    }

    /**
     * @return Message attached to this event. Can be null.
     */
    public String getMessage()
    {
        return message;
    }
}
