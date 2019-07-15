

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.commitlog.AbstractCommitLogService;


class PeriodicCommitLogService extends AbstractCommitLogService {
	private static final long blockWhenSyncLagsNanos = ((long) ((DatabaseDescriptor.getCommitLogSyncPeriod()) * 1500000.0));
}

