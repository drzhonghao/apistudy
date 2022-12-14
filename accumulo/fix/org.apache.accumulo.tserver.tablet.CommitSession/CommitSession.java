

import java.util.List;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.tserver.InMemoryMap;
import org.apache.accumulo.tserver.log.DfsLogger;
import org.apache.accumulo.tserver.tablet.TabletCommitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommitSession {
	private static final Logger log = LoggerFactory.getLogger(CommitSession.class);

	private final long seq;

	private final InMemoryMap memTable;

	private final TabletCommitter committer;

	private int commitsInProgress;

	private long maxCommittedTime = Long.MIN_VALUE;

	CommitSession(TabletCommitter committer, long seq, InMemoryMap imm) {
		this.seq = seq;
		this.memTable = imm;
		this.committer = committer;
		commitsInProgress = 0;
	}

	public long getWALogSeq() {
		return seq;
	}

	public void decrementCommitsInProgress() {
		if ((commitsInProgress) < 1)
			throw new IllegalStateException(("commitsInProgress = " + (commitsInProgress)));

		(commitsInProgress)--;
		if ((commitsInProgress) == 0)
			committer.notifyAll();

	}

	public void incrementCommitsInProgress() {
		if ((commitsInProgress) < 0)
			throw new IllegalStateException(("commitsInProgress = " + (commitsInProgress)));

		(commitsInProgress)++;
	}

	public void waitForCommitsToFinish() {
		while ((commitsInProgress) > 0) {
			try {
				committer.wait(50);
			} catch (InterruptedException e) {
				CommitSession.log.warn("InterruptedException", e);
			}
		} 
	}

	public void abortCommit(List<Mutation> value) {
	}

	public void commit(List<Mutation> mutations) {
	}

	public TabletCommitter getTablet() {
		return committer;
	}

	public boolean beginUpdatingLogsUsed(DfsLogger copy, boolean mincFinish) {
		return committer.beginUpdatingLogsUsed(memTable, copy, mincFinish);
	}

	public void finishUpdatingLogsUsed() {
		committer.finishUpdatingLogsUsed();
	}

	public int getLogId() {
		return committer.getLogId();
	}

	public KeyExtent getExtent() {
		return committer.getExtent();
	}

	public void updateMaxCommittedTime(long time) {
		maxCommittedTime = Math.max(time, maxCommittedTime);
	}

	public long getMaxCommittedTime() {
		if ((maxCommittedTime) == (Long.MIN_VALUE))
			throw new IllegalStateException("Tried to read max committed time when it was never set");

		return maxCommittedTime;
	}

	public void mutate(List<Mutation> mutations) {
		memTable.mutate(mutations);
	}
}

