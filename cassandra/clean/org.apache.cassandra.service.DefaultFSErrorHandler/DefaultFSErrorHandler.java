import org.apache.cassandra.service.*;


import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.BlacklistedDirectories;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.io.FSError;
import org.apache.cassandra.io.FSErrorHandler;
import org.apache.cassandra.io.FSReadError;
import org.apache.cassandra.io.sstable.CorruptSSTableException;
import org.apache.cassandra.utils.JVMStabilityInspector;

public class DefaultFSErrorHandler implements FSErrorHandler
{
    private static final Logger logger = LoggerFactory.getLogger(DefaultFSErrorHandler.class);

    @Override
    public void handleCorruptSSTable(CorruptSSTableException e)
    {
        if (!StorageService.instance.isDaemonSetupCompleted())
            handleStartupFSError(e);

        JVMStabilityInspector.inspectThrowable(e);
        switch (DatabaseDescriptor.getDiskFailurePolicy())
        {
            case stop_paranoid:
                StorageService.instance.stopTransports();
                break;
        }
    }

    @Override
    public void handleFSError(FSError e)
    {
        if (!StorageService.instance.isDaemonSetupCompleted())
            handleStartupFSError(e);

        JVMStabilityInspector.inspectThrowable(e);
        switch (DatabaseDescriptor.getDiskFailurePolicy())
        {
            case stop_paranoid:
            case stop:
                StorageService.instance.stopTransports();
                break;
            case best_effort:
                // for both read and write errors mark the path as unwritable.
                BlacklistedDirectories.maybeMarkUnwritable(e.path);
                if (e instanceof FSReadError)
                {
                    File directory = BlacklistedDirectories.maybeMarkUnreadable(e.path);
                    if (directory != null)
                        Keyspace.removeUnreadableSSTables(directory);
                }
                break;
            case ignore:
                // already logged, so left nothing to do
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private static void handleStartupFSError(Throwable t)
    {
        switch (DatabaseDescriptor.getDiskFailurePolicy())
        {
            case stop_paranoid:
            case stop:
            case die:
                logger.error("Exiting forcefully due to file system exception on startup, disk failure policy \"{}\"",
                             DatabaseDescriptor.getDiskFailurePolicy(),
                             t);
                JVMStabilityInspector.killCurrentJVM(t, true);
                break;
            default:
                break;
        }
    }
}
