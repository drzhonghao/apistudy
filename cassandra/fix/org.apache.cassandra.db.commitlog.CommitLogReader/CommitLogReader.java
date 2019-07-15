

import com.google.common.annotations.VisibleForTesting;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.UnknownColumnFamilyException;
import org.apache.cassandra.db.commitlog.CommitLogDescriptor;
import org.apache.cassandra.db.commitlog.CommitLogPosition;
import org.apache.cassandra.db.commitlog.CommitLogReadHandler;
import org.apache.cassandra.db.commitlog.CommitLogSegmentReader;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.rows.SerializationHelper;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.util.DataInputBuffer;
import org.apache.cassandra.io.util.FileDataInput;
import org.apache.cassandra.io.util.RandomAccessReader;
import org.apache.cassandra.io.util.RebufferingInputStream;
import org.apache.cassandra.security.EncryptionContext;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.db.rows.SerializationHelper.Flag.LOCAL;


public class CommitLogReader {
	private static final Logger logger = LoggerFactory.getLogger(CommitLogReader.class);

	private static final int LEGACY_END_OF_SEGMENT_MARKER = 0;

	@VisibleForTesting
	public static final int ALL_MUTATIONS = -1;

	private final CRC32 checksum;

	private final Map<UUID, AtomicInteger> invalidMutations;

	private byte[] buffer;

	public CommitLogReader() {
		checksum = new CRC32();
		invalidMutations = new HashMap<>();
		buffer = new byte[4096];
	}

	public Set<Map.Entry<UUID, AtomicInteger>> getInvalidMutations() {
		return invalidMutations.entrySet();
	}

	public void readAllFiles(CommitLogReadHandler handler, File[] files) throws IOException {
		readAllFiles(handler, files, CommitLogPosition.NONE);
	}

	private static boolean shouldSkip(File file) throws IOException, ConfigurationException {
		CommitLogDescriptor desc = CommitLogDescriptor.fromFileName(file.getName());
		try (final RandomAccessReader reader = RandomAccessReader.open(file)) {
			CommitLogDescriptor.readHeader(reader, DatabaseDescriptor.getEncryptionContext());
			int end = reader.readInt();
			long filecrc = (reader.readInt()) & 4294967295L;
			return (end == 0) && (filecrc == 0);
		}
	}

	private static List<File> filterCommitLogFiles(File[] toFilter) {
		List<File> filtered = new ArrayList<>(toFilter.length);
		for (File file : toFilter) {
			try {
				if (CommitLogReader.shouldSkip(file)) {
					CommitLogReader.logger.info("Skipping playback of empty log: {}", file.getName());
				}else {
					filtered.add(file);
				}
			} catch (Exception e) {
				filtered.add(file);
			}
		}
		return filtered;
	}

	public void readAllFiles(CommitLogReadHandler handler, File[] files, CommitLogPosition minPosition) throws IOException {
		List<File> filteredLogs = CommitLogReader.filterCommitLogFiles(files);
		int i = 0;
		for (File file : filteredLogs) {
			i++;
			readCommitLogSegment(handler, file, minPosition, CommitLogReader.ALL_MUTATIONS, (i == (filteredLogs.size())));
		}
	}

	public void readCommitLogSegment(CommitLogReadHandler handler, File file, boolean tolerateTruncation) throws IOException {
		readCommitLogSegment(handler, file, CommitLogPosition.NONE, CommitLogReader.ALL_MUTATIONS, tolerateTruncation);
	}

	@VisibleForTesting
	public void readCommitLogSegment(CommitLogReadHandler handler, File file, int mutationLimit, boolean tolerateTruncation) throws IOException {
		readCommitLogSegment(handler, file, CommitLogPosition.NONE, mutationLimit, tolerateTruncation);
	}

	public void readCommitLogSegment(CommitLogReadHandler handler, File file, CommitLogPosition minPosition, int mutationLimit, boolean tolerateTruncation) throws IOException {
		CommitLogDescriptor desc = CommitLogDescriptor.fromFileName(file.getName());
		try (RandomAccessReader reader = RandomAccessReader.open(file)) {
			final long segmentIdFromFilename = desc.id;
			try {
				desc = CommitLogDescriptor.readHeader(reader, DatabaseDescriptor.getEncryptionContext());
			} catch (Exception e) {
				desc = null;
			}
			if (desc == null) {
				return;
			}
			if (segmentIdFromFilename != (desc.id)) {
			}
			if (shouldSkipSegmentId(file, desc, minPosition))
				return;

			CommitLogSegmentReader segmentReader;
			try {
			} catch (Exception e) {
				return;
			}
			try {
				CommitLogReader.ReadStatusTracker statusTracker = new CommitLogReader.ReadStatusTracker(mutationLimit, tolerateTruncation);
				segmentReader = null;
				for (CommitLogSegmentReader.SyncSegment syncSegment : segmentReader) {
					statusTracker.tolerateErrorsInSection = tolerateTruncation & (syncSegment.toleratesErrorsInSection);
					if (((desc.id) == (minPosition.segmentId)) && ((syncSegment.endPosition) < (minPosition.position)))
						continue;

					statusTracker.errorContext = String.format("Next section at %d in %s", syncSegment.fileStartPosition, desc.fileName());
					readSection(handler, syncSegment.input, minPosition, syncSegment.endPosition, statusTracker, desc);
					if (!(statusTracker.shouldContinue()))
						break;

				}
			} catch (RuntimeException re) {
				if ((re.getCause()) instanceof IOException)
					throw ((IOException) (re.getCause()));

				throw re;
			}
			CommitLogReader.logger.debug("Finished reading {}", file);
		}
	}

	private boolean shouldSkipSegmentId(File file, CommitLogDescriptor desc, CommitLogPosition minPosition) {
		if ((minPosition.segmentId) > (desc.id)) {
			CommitLogReader.logger.trace("Skipping read of fully-flushed {}", file);
			return true;
		}
		return false;
	}

	private void readSection(CommitLogReadHandler handler, FileDataInput reader, CommitLogPosition minPosition, int end, CommitLogReader.ReadStatusTracker statusTracker, CommitLogDescriptor desc) throws IOException {
		if (((desc.id) == (minPosition.segmentId)) && ((reader.getFilePointer()) < (minPosition.position)))
			reader.seek(minPosition.position);

		while (((statusTracker.shouldContinue()) && ((reader.getFilePointer()) < end)) && (!(reader.isEOF()))) {
			long mutationStart = reader.getFilePointer();
			if (CommitLogReader.logger.isTraceEnabled())
				CommitLogReader.logger.trace("Reading mutation at {}", mutationStart);

			long claimedCRC32;
			int serializedSize;
			try {
				if ((end - (reader.getFilePointer())) < 4) {
					CommitLogReader.logger.trace("Not enough bytes left for another mutation in this CommitLog segment, continuing");
					statusTracker.requestTermination();
					return;
				}
				serializedSize = reader.readInt();
				if (serializedSize == (CommitLogReader.LEGACY_END_OF_SEGMENT_MARKER)) {
					CommitLogReader.logger.trace("Encountered end of segment marker at {}", reader.getFilePointer());
					statusTracker.requestTermination();
					return;
				}
				if (serializedSize < 10) {
					return;
				}
				checksum.reset();
				if (serializedSize > (buffer.length))
					buffer = new byte[((int) (1.2 * serializedSize))];

				reader.readFully(buffer, 0, serializedSize);
			} catch (EOFException eof) {
				return;
			}
			checksum.update(buffer, 0, serializedSize);
			claimedCRC32 = 0l;
			if (claimedCRC32 != (checksum.getValue())) {
				continue;
			}
			long mutationPosition = reader.getFilePointer();
			readMutation(handler, buffer, serializedSize, minPosition, ((int) (mutationPosition)), desc);
			if (mutationPosition >= (minPosition.position))
				statusTracker.addProcessedMutation();

		} 
	}

	@VisibleForTesting
	protected void readMutation(CommitLogReadHandler handler, byte[] inputBuffer, int size, CommitLogPosition minPosition, final int entryLocation, final CommitLogDescriptor desc) throws IOException {
		boolean shouldReplay = entryLocation > (minPosition.position);
		final Mutation mutation;
		try (RebufferingInputStream bufIn = new DataInputBuffer(inputBuffer, 0, size)) {
			mutation = Mutation.serializer.deserialize(bufIn, desc.getMessagingVersion(), LOCAL);
			for (PartitionUpdate upd : mutation.getPartitionUpdates())
				upd.validate();

		} catch (UnknownColumnFamilyException ex) {
			if ((ex.cfId) == null)
				return;

			AtomicInteger i = invalidMutations.get(ex.cfId);
			if (i == null) {
				i = new AtomicInteger(1);
				invalidMutations.put(ex.cfId, i);
			}else
				i.incrementAndGet();

			return;
		} catch (Throwable t) {
			JVMStabilityInspector.inspectThrowable(t);
			File f = File.createTempFile("mutation", "dat");
			try (final DataOutputStream out = new DataOutputStream(new FileOutputStream(f))) {
				out.write(inputBuffer, 0, size);
			}
			return;
		}
		if (CommitLogReader.logger.isTraceEnabled())
			CommitLogReader.logger.trace("Read mutation for {}.{}: {}", mutation.getKeyspaceName(), mutation.key(), (("{" + (StringUtils.join(mutation.getPartitionUpdates().iterator(), ", "))) + "}"));

		if (shouldReplay)
			handler.handleMutation(mutation, size, entryLocation, desc);

	}

	private static class CommitLogFormat {
		public static long calculateClaimedChecksum(FileDataInput input, int commitLogVersion) throws IOException {
			switch (commitLogVersion) {
				case CommitLogDescriptor.VERSION_12 :
				case CommitLogDescriptor.VERSION_20 :
					return input.readLong();
				default :
					return (input.readInt()) & 4294967295L;
			}
		}

		public static void updateChecksum(CRC32 checksum, int serializedSize, int commitLogVersion) {
			switch (commitLogVersion) {
				case CommitLogDescriptor.VERSION_12 :
					checksum.update(serializedSize);
					break;
				default :
					FBUtilities.updateChecksumInt(checksum, serializedSize);
					break;
			}
		}

		public static long calculateClaimedCRC32(FileDataInput input, int commitLogVersion) throws IOException {
			switch (commitLogVersion) {
				case CommitLogDescriptor.VERSION_12 :
				case CommitLogDescriptor.VERSION_20 :
					return input.readLong();
				default :
					return (input.readInt()) & 4294967295L;
			}
		}
	}

	private static class ReadStatusTracker {
		private int mutationsLeft;

		public String errorContext = "";

		public boolean tolerateErrorsInSection;

		private boolean error;

		public ReadStatusTracker(int mutationLimit, boolean tolerateErrorsInSection) {
			this.mutationsLeft = mutationLimit;
			this.tolerateErrorsInSection = tolerateErrorsInSection;
		}

		public void addProcessedMutation() {
			if ((mutationsLeft) == (CommitLogReader.ALL_MUTATIONS))
				return;

			--(mutationsLeft);
		}

		public boolean shouldContinue() {
			return (!(error)) && (((mutationsLeft) != 0) || ((mutationsLeft) == (CommitLogReader.ALL_MUTATIONS)));
		}

		public void requestTermination() {
			error = true;
		}
	}
}

