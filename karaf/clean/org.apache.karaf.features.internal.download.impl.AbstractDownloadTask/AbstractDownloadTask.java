import org.apache.karaf.features.internal.download.impl.DefaultFuture;
import org.apache.karaf.features.internal.download.impl.*;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.karaf.features.internal.download.StreamProvider;

public abstract class AbstractDownloadTask extends DefaultFuture<AbstractDownloadTask> implements Runnable, StreamProvider {

    protected final String url;
    protected ScheduledExecutorService executorService;

    public AbstractDownloadTask(ScheduledExecutorService executorService, String url) {
        this.executorService = executorService;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public File getFile() throws IOException {
        Object v = getValue();
        if (v instanceof File) {
            return (File) v;
        } else if (v instanceof IOException) {
            throw (IOException) v;
        } else {
            return null;
        }
    }

    @Override
    public InputStream open() throws IOException {
        return new FileInputStream(getFile());
    }

    public void setFile(File file) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        setValue(file);
    }

    public void setException(IOException exception) {
        if (exception == null) {
            throw new NullPointerException("exception");
        }
        setValue(exception);
    }

}
