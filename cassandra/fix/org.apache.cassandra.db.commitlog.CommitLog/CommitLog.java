

import com.google.common.annotations.VisibleForTesting;
import io.netty.util.concurrent.FastThreadLocal;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.ParameterizedClass;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.commitlog.AbstractCommitLogSegmentManager;
import org.apache.cassandra.db.commitlog.AbstractCommitLogService;
import org.apache.cassandra.db.commitlog.CommitLogArchiver;
import org.apache.cassandra.db.commitlog.CommitLogMBean;
import org.apache.cassandra.db.commitlog.CommitLogPosition;
import org.apache.cassandra.db.commitlog.CommitLogSegment;
import org.apache.cassandra.exceptions.WriteTimeoutException;
import org.apache.cassandra.io.compress.ICompressor;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.metrics.CommitLogMetrics;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.CompressionParams;
import org.apache.cassandra.security.EncryptionContext;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.config.Config.CommitFailurePolicy.die;
import static org.apache.cassandra.config.Config.CommitFailurePolicy.ignore;
import static org.apache.cassandra.config.Config.CommitFailurePolicy.stop;
import static org.apache.cassandra.config.Config.CommitFailurePolicy.stop_commit;


public class CommitLog implements CommitLogMBean {
	private static final Logger logger = LoggerFactory.getLogger(CommitLog.class);

	public static final CommitLog instance = CommitLog.construct();

	final long MAX_MUTATION_SIZE = DatabaseDescriptor.getMaxMutationSize();

	public final AbstractCommitLogSegmentManager segmentManager = null;

	public final CommitLogArchiver archiver;

	final CommitLogMetrics metrics;

	AbstractCommitLogService executor = null;

	volatile CommitLog.Configuration configuration;

	private static CommitLog construct() {
		CommitLog log = new CommitLog(CommitLogArchiver.construct());
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			mbs.registerMBean(log, new ObjectName("org.apache.cassandra.db:type=Commitlog"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return log.start();
	}

	@VisibleForTesting
	CommitLog(CommitLogArchiver archiver) {
		this.configuration = new CommitLog.Configuration(DatabaseDescriptor.getCommitLogCompression(), DatabaseDescriptor.getEncryptionContext());
		DatabaseDescriptor.createAllDirectories();
		this.archiver = archiver;
		metrics = new CommitLogMetrics();
		metrics.attach(executor, segmentManager);
		executor = null;
	}

	CommitLog start() {
		return this;
	}

	public int recoverSegmentsOnDisk() throws IOException {
		assert archiver.archivePending.isEmpty() : "Not all commit log archive tasks were completed before restore";
		archiver.maybeRestoreArchive();
		int replayed = 0;
		return replayed;
	}

	public int recoverFiles(File... clogs) throws IOException {
		return 0;
	}

	public void recoverPath(String path) throws IOException {
	}

	public void recover(String path) throws IOException {
		recoverPath(path);
	}

	public CommitLogPosition getCurrentPosition() {
		return null;
	}

	public void forceRecycleAllSegments(Iterable<UUID> droppedCfs) {
	}

	public void forceRecycleAllSegments() {
	}

	public void sync(boolean flush) throws IOException {
		segmentManager.sync(flush);
	}

	public void requestExtraSync() {
	}

	public CommitLogPosition add(Mutation mutation) throws WriteTimeoutException {
		assert mutation != null;
		try (final DataOutputBuffer dob = DataOutputBuffer.scratchBuffer.get()) {
			Mutation.serializer.serialize(mutation, dob, MessagingService.current_version);
			int size = dob.getLength();
			int totalSize = size + (CommitLogSegment.ENTRY_OVERHEAD_SIZE);
			if (totalSize > (MAX_MUTATION_SIZE)) {
				throw new IllegalArgumentException(String.format("Mutation of %s is too large for the maximum size of %s", FBUtilities.prettyPrintMemory(totalSize), FBUtilities.prettyPrintMemory(MAX_MUTATION_SIZE)));
			}
			CRC32 checksum = new CRC32();
		} catch (IOException e) {
		}
		return null;
	}

	public void discardCompletedSegments(final UUID cfId, final CommitLogPosition lowerBound, final CommitLogPosition upperBound) {
		CommitLog.logger.trace("discard completed log segments for {}-{}, table {}", lowerBound, upperBound, cfId);
		for (Iterator<CommitLogSegment> iter = segmentManager.getActiveSegments().iterator(); iter.hasNext();) {
			CommitLogSegment segment = iter.next();
			segment.markClean(cfId, lowerBound, upperBound);
			if (segment.isUnused()) {
				CommitLog.logger.debug("Commit log segment {} is unused", segment);
			}else {
				if (CommitLog.logger.isTraceEnabled())
					CommitLog.logger.trace("Not safe to delete{} commit log segment {}; dirty is {}", (iter.hasNext() ? "" : " active"), segment, segment.dirtyString());

			}
			if (segment.contains(upperBound))
				break;

		}
	}

	@Override
	public String getArchiveCommand() {
		return null;
	}

	@Override
	public String getRestoreCommand() {
		return null;
	}

	@Override
	public String getRestoreDirectories() {
		return null;
	}

	@Override
	public long getRestorePointInTime() {
		return archiver.restorePointInTime;
	}

	@Override
	public String getRestorePrecision() {
		return archiver.precision.toString();
	}

	public List<String> getActiveSegmentNames() {
		List<String> segmentNames = new ArrayList<>();
		for (CommitLogSegment seg : segmentManager.getActiveSegments())
			segmentNames.add(seg.getName());

		return segmentNames;
	}

	public List<String> getArchivingSegmentNames() {
		return new ArrayList<>(archiver.archivePending.keySet());
	}

	@Override
	public long getActiveContentSize() {
		long size = 0;
		for (CommitLogSegment seg : segmentManager.getActiveSegments())
			size += seg.contentSize();

		return size;
	}

	@Override
	public long getActiveOnDiskSize() {
		return segmentManager.onDiskSize();
	}

	@Override
	public Map<String, Double> getActiveSegmentCompressionRatios() {
		Map<String, Double> segmentRatios = new TreeMap<>();
		for (CommitLogSegment seg : segmentManager.getActiveSegments())
			segmentRatios.put(seg.getName(), ((1.0 * (seg.onDiskSize())) / (seg.contentSize())));

		return segmentRatios;
	}

	public void shutdownBlocking() throws InterruptedException {
		executor.shutdown();
		executor.awaitTermination();
		segmentManager.shutdown();
		segmentManager.awaitTermination();
	}

	public int resetUnsafe(boolean deleteSegments) throws IOException {
		stopUnsafe(deleteSegments);
		resetConfiguration();
		return restartUnsafe();
	}

	public void resetConfiguration() {
		configuration = new CommitLog.Configuration(DatabaseDescriptor.getCommitLogCompression(), DatabaseDescriptor.getEncryptionContext());
	}

	public void stopUnsafe(boolean deleteSegments) {
		executor.shutdown();
		try {
			executor.awaitTermination();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		segmentManager.stopUnsafe(deleteSegments);
		if ((DatabaseDescriptor.isCDCEnabled()) && deleteSegments)
			for (File f : new File(DatabaseDescriptor.getCDCLogLocation()).listFiles())
				FileUtils.deleteWithConfirm(f);


	}

	public int restartUnsafe() throws IOException {
		return start().recoverSegmentsOnDisk();
	}

	@VisibleForTesting
	public static boolean handleCommitError(String message, Throwable t) {
		JVMStabilityInspector.inspectCommitLogThrowable(t);
		switch (DatabaseDescriptor.getCommitFailurePolicy()) {
			case die :
			case stop :
				StorageService.instance.stopTransports();
			case stop_commit :
				CommitLog.logger.error(String.format("%s. Commit disk failure policy is %s; terminating thread", message, DatabaseDescriptor.getCommitFailurePolicy()), t);
				return false;
			case ignore :
				CommitLog.logger.error(message, t);
				return true;
			default :
				throw new AssertionError(DatabaseDescriptor.getCommitFailurePolicy());
		}
	}

	public static final class Configuration {
		private final ParameterizedClass compressorClass;

		private final ICompressor compressor;

		private EncryptionContext encryptionContext;

		public Configuration(ParameterizedClass compressorClass, EncryptionContext encryptionContext) {
			this.compressorClass = compressorClass;
			this.compressor = (compressorClass != null) ? CompressionParams.createCompressor(compressorClass) : null;
			this.encryptionContext = encryptionContext;
		}

		public boolean useCompression() {
			return (compressor) != null;
		}

		public boolean useEncryption() {
			return encryptionContext.isEnabled();
		}

		public ICompressor getCompressor() {
			return compressor;
		}

		public ParameterizedClass getCompressorClass() {
			return compressorClass;
		}

		public String getCompressorName() {
			return useCompression() ? compressor.getClass().getSimpleName() : "none";
		}

		public EncryptionContext getEncryptionContext() {
			return encryptionContext;
		}
	}
}

