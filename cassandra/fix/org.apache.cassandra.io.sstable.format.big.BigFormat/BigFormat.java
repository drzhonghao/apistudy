

import java.util.Collection;
import java.util.Set;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.io.sstable.Component;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.format.SSTableFlushObserver;
import org.apache.cassandra.io.sstable.format.SSTableFormat;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.SSTableWriter;
import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.io.sstable.format.big.BigTableWriter;
import org.apache.cassandra.io.sstable.metadata.MetadataCollector;
import org.apache.cassandra.io.sstable.metadata.StatsMetadata;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.utils.ChecksumType;


public class BigFormat implements SSTableFormat {
	public static final BigFormat instance = new BigFormat();

	public static final Version latestVersion = new BigFormat.BigVersion(BigFormat.BigVersion.current_version);

	private static final SSTableReader.Factory readerFactory = new BigFormat.ReaderFactory();

	private static final SSTableWriter.Factory writerFactory = new BigFormat.WriterFactory();

	private BigFormat() {
	}

	@Override
	public Version getLatestVersion() {
		return BigFormat.latestVersion;
	}

	@Override
	public Version getVersion(String version) {
		return new BigFormat.BigVersion(version);
	}

	@Override
	public SSTableWriter.Factory getWriterFactory() {
		return BigFormat.writerFactory;
	}

	@Override
	public SSTableReader.Factory getReaderFactory() {
		return BigFormat.readerFactory;
	}

	@Override
	public RowIndexEntry.IndexSerializer getIndexSerializer(CFMetaData metadata, Version version, SerializationHeader header) {
		return new RowIndexEntry.Serializer(metadata, version, header);
	}

	static class WriterFactory extends SSTableWriter.Factory {
		@Override
		public SSTableWriter open(Descriptor descriptor, long keyCount, long repairedAt, CFMetaData metadata, MetadataCollector metadataCollector, SerializationHeader header, Collection<SSTableFlushObserver> observers, LifecycleTransaction txn) {
			return new BigTableWriter(descriptor, keyCount, repairedAt, metadata, metadataCollector, header, observers, txn);
		}
	}

	static class ReaderFactory extends SSTableReader.Factory {
		@Override
		public SSTableReader open(Descriptor descriptor, Set<Component> components, CFMetaData metadata, Long maxDataAge, StatsMetadata sstableMetadata, SSTableReader.OpenReason openReason, SerializationHeader header) {
			return null;
		}
	}

	static class BigVersion extends Version {
		public static final String current_version = "mc";

		public static final String earliest_supported_version = "jb";

		private final boolean isLatestVersion;

		private final boolean hasSamplingLevel;

		private final boolean newStatsFile;

		private final ChecksumType compressedChecksumType;

		private final ChecksumType uncompressedChecksumType;

		private final boolean hasRepairedAt;

		private final boolean tracksLegacyCounterShards;

		private final boolean newFileName;

		public final boolean storeRows;

		public final int correspondingMessagingVersion;

		public final boolean hasBoundaries;

		private final boolean hasOldBfHashOrder;

		private final boolean hasCommitLogLowerBound;

		private final boolean hasCommitLogIntervals;

		private final boolean hasCompactionAncestors;

		BigVersion(String version) {
			super(BigFormat.instance, version);
			isLatestVersion = (version.compareTo(BigFormat.BigVersion.current_version)) == 0;
			hasSamplingLevel = (version.compareTo("ka")) >= 0;
			newStatsFile = (version.compareTo("ka")) >= 0;
			ChecksumType checksumType = ChecksumType.CRC32;
			if (((version.compareTo("ka")) >= 0) && ((version.compareTo("ma")) < 0))
				checksumType = ChecksumType.Adler32;

			this.uncompressedChecksumType = checksumType;
			checksumType = ChecksumType.CRC32;
			if (((version.compareTo("jb")) >= 0) && ((version.compareTo("ma")) < 0))
				checksumType = ChecksumType.Adler32;

			this.compressedChecksumType = checksumType;
			hasRepairedAt = (version.compareTo("ka")) >= 0;
			tracksLegacyCounterShards = (version.compareTo("ka")) >= 0;
			newFileName = (version.compareTo("la")) >= 0;
			hasOldBfHashOrder = (version.compareTo("ma")) < 0;
			hasCompactionAncestors = (version.compareTo("ma")) < 0;
			storeRows = (version.compareTo("ma")) >= 0;
			correspondingMessagingVersion = (storeRows) ? MessagingService.VERSION_30 : MessagingService.VERSION_21;
			hasBoundaries = (version.compareTo("ma")) < 0;
			hasCommitLogLowerBound = (((version.compareTo("lb")) >= 0) && ((version.compareTo("ma")) < 0)) || ((version.compareTo("mb")) >= 0);
			hasCommitLogIntervals = (version.compareTo("mc")) >= 0;
		}

		@Override
		public boolean isLatestVersion() {
			return isLatestVersion;
		}

		@Override
		public boolean hasSamplingLevel() {
			return hasSamplingLevel;
		}

		@Override
		public boolean hasNewStatsFile() {
			return newStatsFile;
		}

		@Override
		public ChecksumType compressedChecksumType() {
			return compressedChecksumType;
		}

		@Override
		public ChecksumType uncompressedChecksumType() {
			return uncompressedChecksumType;
		}

		@Override
		public boolean hasRepairedAt() {
			return hasRepairedAt;
		}

		@Override
		public boolean tracksLegacyCounterShards() {
			return tracksLegacyCounterShards;
		}

		@Override
		public boolean hasOldBfHashOrder() {
			return hasOldBfHashOrder;
		}

		@Override
		public boolean hasCompactionAncestors() {
			return hasCompactionAncestors;
		}

		@Override
		public boolean hasNewFileName() {
			return newFileName;
		}

		@Override
		public boolean hasCommitLogLowerBound() {
			return hasCommitLogLowerBound;
		}

		@Override
		public boolean hasCommitLogIntervals() {
			return hasCommitLogIntervals;
		}

		@Override
		public boolean storeRows() {
			return storeRows;
		}

		@Override
		public int correspondingMessagingVersion() {
			return correspondingMessagingVersion;
		}

		@Override
		public boolean hasBoundaries() {
			return hasBoundaries;
		}

		@Override
		public boolean isCompatible() {
			return ((version.compareTo(BigFormat.BigVersion.earliest_supported_version)) >= 0) && ((version.charAt(0)) <= (BigFormat.BigVersion.current_version.charAt(0)));
		}

		@Override
		public boolean isCompatibleForStreaming() {
			return (isCompatible()) && ((version.charAt(0)) == (BigFormat.BigVersion.current_version.charAt(0)));
		}
	}
}

