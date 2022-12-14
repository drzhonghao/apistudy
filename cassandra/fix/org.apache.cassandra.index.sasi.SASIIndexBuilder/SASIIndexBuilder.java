

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.db.compaction.CompactionInfo;
import org.apache.cassandra.db.compaction.CompactionInterruptedException;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.index.SecondaryIndexBuilder;
import org.apache.cassandra.index.sasi.conf.ColumnIndex;
import org.apache.cassandra.index.sasi.disk.PerSSTableIndexWriter;
import org.apache.cassandra.io.FSReadError;
import org.apache.cassandra.io.sstable.Component;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.KeyIterator;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.SSTableIdentityIterator;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.util.RandomAccessReader;
import org.apache.cassandra.utils.AbstractIterator;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.UUIDGen;

import static org.apache.cassandra.io.sstable.format.SSTableReader.Operator.EQ;


class SASIIndexBuilder extends SecondaryIndexBuilder {
	private final ColumnFamilyStore cfs;

	private final UUID compactionId = UUIDGen.getTimeUUID();

	private final SortedMap<SSTableReader, Map<ColumnDefinition, ColumnIndex>> sstables;

	private long bytesProcessed = 0;

	private final long totalSizeInBytes;

	public SASIIndexBuilder(ColumnFamilyStore cfs, SortedMap<SSTableReader, Map<ColumnDefinition, ColumnIndex>> sstables) {
		long totalIndexBytes = 0;
		for (SSTableReader sstable : sstables.keySet())
			totalIndexBytes += getPrimaryIndexLength(sstable);

		this.cfs = cfs;
		this.sstables = sstables;
		this.totalSizeInBytes = totalIndexBytes;
	}

	public void build() {
		AbstractType<?> keyValidator = cfs.metadata.getKeyValidator();
		for (Map.Entry<SSTableReader, Map<ColumnDefinition, ColumnIndex>> e : sstables.entrySet()) {
			SSTableReader sstable = e.getKey();
			Map<ColumnDefinition, ColumnIndex> indexes = e.getValue();
			try (RandomAccessReader dataFile = sstable.openDataReader()) {
				long previousKeyPosition = 0;
				try (final KeyIterator keys = new KeyIterator(sstable.descriptor, cfs.metadata)) {
					while (keys.hasNext()) {
						if (isStopRequested())
							throw new CompactionInterruptedException(getCompactionInfo());

						final DecoratedKey key = keys.next();
						final long keyPosition = keys.getKeyPosition();
						try {
							RowIndexEntry indexEntry = sstable.getPosition(key, EQ);
							dataFile.seek(indexEntry.position);
							ByteBufferUtil.readWithShortLength(dataFile);
							try (final SSTableIdentityIterator partition = SSTableIdentityIterator.create(sstable, dataFile, key)) {
								if (cfs.metadata.hasStaticColumns()) {
								}
								while (partition.hasNext()) {
								} 
							}
						} catch (IOException ex) {
							throw new FSReadError(ex, sstable.getFilename());
						}
						bytesProcessed += keyPosition - previousKeyPosition;
						previousKeyPosition = keyPosition;
					} 
				}
			}
		}
	}

	public CompactionInfo getCompactionInfo() {
		return new CompactionInfo(cfs.metadata, OperationType.INDEX_BUILD, bytesProcessed, totalSizeInBytes, compactionId);
	}

	private long getPrimaryIndexLength(SSTable sstable) {
		File primaryIndex = new File(sstable.getIndexFilename());
		return primaryIndex.exists() ? primaryIndex.length() : 0;
	}

	private void completeSSTable(PerSSTableIndexWriter indexWriter, SSTableReader sstable, Collection<ColumnIndex> indexes) {
		indexWriter.complete();
		for (ColumnIndex index : indexes) {
			File tmpIndex = new File(sstable.descriptor.filenameFor(index.getComponent()));
			if (!(tmpIndex.exists()))
				continue;

			index.update(Collections.<SSTableReader>emptyList(), Collections.singletonList(sstable));
		}
	}
}

