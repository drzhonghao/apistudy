

import com.google.common.util.concurrent.RateLimiter;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.WriteType;
import org.apache.cassandra.db.commitlog.AbstractCommitLogSegmentManager;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.db.commitlog.CommitLogSegment;
import org.apache.cassandra.exceptions.WriteTimeoutException;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.DirectorySizeCalculator;
import org.apache.cassandra.utils.NoSpamLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.db.commitlog.CommitLogSegment.CDCState.CONTAINS;
import static org.apache.cassandra.db.commitlog.CommitLogSegment.CDCState.FORBIDDEN;
import static org.apache.cassandra.utils.NoSpamLogger.Level.WARN;


public class CommitLogSegmentManagerCDC extends AbstractCommitLogSegmentManager {
	static final Logger logger = LoggerFactory.getLogger(CommitLogSegmentManagerCDC.class);

	private final CommitLogSegmentManagerCDC.CDCSizeTracker cdcSizeTracker;

	void start() {
		cdcSizeTracker.start();
	}

	public void discard(CommitLogSegment segment, boolean delete) {
		cdcSizeTracker.processDiscardedSegment(segment);
		if ((segment.getCDCState()) == (CONTAINS)) {
		}else {
			if (delete) {
			}
		}
	}

	public void shutdown() {
		cdcSizeTracker.shutdown();
		super.shutdown();
	}

	private void throwIfForbidden(Mutation mutation, CommitLogSegment segment) throws WriteTimeoutException {
		if ((mutation.trackedByCDC()) && ((segment.getCDCState()) == (FORBIDDEN))) {
			cdcSizeTracker.submitOverflowSizeRecalculation();
			NoSpamLogger.log(CommitLogSegmentManagerCDC.logger, WARN, 10, TimeUnit.SECONDS, "Rejecting Mutation containing CDC-enabled table. Free up space in {}.", DatabaseDescriptor.getCDCLogLocation());
			throw new WriteTimeoutException(WriteType.CDC, ConsistencyLevel.LOCAL_ONE, 0, 1);
		}
	}

	void handleReplayedSegment(final File file) {
		CommitLogSegmentManagerCDC.logger.trace("Moving (Unopened) segment {} to cdc_raw directory after replay", file);
		FileUtils.renameWithConfirm(file.getAbsolutePath(), (((DatabaseDescriptor.getCDCLogLocation()) + (File.separator)) + (file.getName())));
		cdcSizeTracker.addFlushedSize(file.length());
	}

	public CommitLogSegment createSegment() {
	}

	private static class CDCSizeTracker extends DirectorySizeCalculator {
		private final RateLimiter rateLimiter = RateLimiter.create((1000.0 / (DatabaseDescriptor.getCDCDiskCheckInterval())));

		private ExecutorService cdcSizeCalculationExecutor;

		private CommitLogSegmentManagerCDC segmentManager;

		private volatile long unflushedCDCSize;

		private volatile long sizeInProgress = 0;

		CDCSizeTracker(CommitLogSegmentManagerCDC segmentManager, File path) {
			super(path);
			this.segmentManager = segmentManager;
		}

		public void start() {
			size = 0;
			unflushedCDCSize = 0;
			cdcSizeCalculationExecutor = new ThreadPoolExecutor(1, 1, 1000, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.DiscardPolicy());
		}

		void processNewSegment(CommitLogSegment segment) {
			submitOverflowSizeRecalculation();
		}

		void processDiscardedSegment(CommitLogSegment segment) {
			submitOverflowSizeRecalculation();
		}

		private long allowableCDCBytes() {
			return (((long) (DatabaseDescriptor.getCDCSpaceInMB())) * 1024) * 1024;
		}

		public void submitOverflowSizeRecalculation() {
			try {
				cdcSizeCalculationExecutor.submit(() -> recalculateOverflowSize());
			} catch (RejectedExecutionException e) {
			}
		}

		private void recalculateOverflowSize() {
			rateLimiter.acquire();
			calculateSize();
		}

		private int defaultSegmentSize() {
			return DatabaseDescriptor.getCommitLogSegmentSize();
		}

		private void calculateSize() {
			try {
				sizeInProgress = 0;
				Files.walkFileTree(path.toPath(), this);
				size = sizeInProgress;
			} catch (IOException ie) {
				CommitLog.instance.handleCommitError("Failed CDC Size Calculation", ie);
			}
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			sizeInProgress += attrs.size();
			return FileVisitResult.CONTINUE;
		}

		private void addFlushedSize(long toAdd) {
			size += toAdd;
		}

		private long totalCDCSizeOnDisk() {
			return (unflushedCDCSize) + (size);
		}

		public void shutdown() {
			cdcSizeCalculationExecutor.shutdown();
		}
	}

	@com.google.common.annotations.VisibleForTesting
	public long updateCDCTotalSize() {
		cdcSizeTracker.submitOverflowSizeRecalculation();
		try {
			Thread.sleep(((DatabaseDescriptor.getCDCDiskCheckInterval()) + 10));
		} catch (InterruptedException e) {
		}
		return cdcSizeTracker.totalCDCSizeOnDisk();
	}
}

