

import java.io.File;
import org.apache.cassandra.db.commitlog.AbstractCommitLogSegmentManager;
import org.apache.cassandra.db.commitlog.CommitLogSegment;
import org.apache.cassandra.io.util.FileUtils;


public class CommitLogSegmentManagerStandard extends AbstractCommitLogSegmentManager {
	public void discard(CommitLogSegment segment, boolean delete) {
		if (delete) {
		}
	}

	void handleReplayedSegment(final File file) {
		FileUtils.deleteWithConfirm(file);
	}

	public CommitLogSegment createSegment() {
	}
}

