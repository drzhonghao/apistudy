import org.apache.karaf.features.internal.download.impl.AbstractDownloadTask;
import org.apache.karaf.features.internal.download.impl.*;


import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRetryableDownloadTask extends AbstractDownloadTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRetryableDownloadTask.class);

    private long scheduleDelay = 250;
    private int scheduleMaxRun = 9;
    private int scheduleNbRun = 0;

    private Exception previousException = null;

    public AbstractRetryableDownloadTask(ScheduledExecutorService executorService, String url) {
        super(executorService, url);
    }

    public long getScheduleDelay() {
        return scheduleDelay;
    }

    public void setScheduleDelay(long scheduleDelay) {
        this.scheduleDelay = scheduleDelay;
    }

    public int getScheduleMaxRun() {
        return scheduleMaxRun;
    }

    public void setScheduleMaxRun(int scheduleMaxRun) {
        this.scheduleMaxRun = scheduleMaxRun;
    }

    public void run() {
        try {
            try {
                File file = download(previousException);
                setFile(file);
            } catch (IOException e) {
                Retry retry = isRetryable(e);
                int retryCount = scheduleMaxRun;
                if (retry == Retry.QUICK_RETRY) {
                    retryCount = retryCount / 2; // arbitrary number...
                } else if (retry == Retry.NO_RETRY) {
                    retryCount = 0;
                }
                if (++scheduleNbRun < retryCount) {
                    previousException = e;
                    long delay = (long)(scheduleDelay * 3 / 2 + Math.random() * scheduleDelay / 2);
                    LOGGER.debug("Error downloading " + url + ": " + e.getMessage() + ". " + retry + " in approx " + delay + " ms.");
                    executorService.schedule(this, delay, TimeUnit.MILLISECONDS);
                    scheduleDelay *= 2;
                } else {
                    setException(new IOException("Error downloading " + url, e));
                }
            }
        } catch (Throwable e) {
            setException(new IOException("Error downloading " + url, e));
        }
    }

    protected Retry isRetryable(IOException e) {
        return Retry.DEFAULT_RETRY;
    }

    /**
     * Abstract download operation that may use <em>previous exception</em> as hint for optimized retry
     * @param previousException
     * @return
     * @throws Exception
     */
    protected abstract File download(Exception previousException) throws Exception;

    /**
     * What kind of retry may be attempted
     */
    protected enum Retry {
        /** Each retry would lead to the same result */
        NO_RETRY,
        /** It's ok to retry 2, 3 times, but no more */
        QUICK_RETRY,
        /** Retry with high expectation of success at some point */
        DEFAULT_RETRY
    }

}
