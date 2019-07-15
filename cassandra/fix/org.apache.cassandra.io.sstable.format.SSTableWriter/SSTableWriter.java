

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.io.sstable.Component;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableFlushObserver;
import org.apache.cassandra.io.sstable.format.SSTableFormat;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.io.sstable.format.big.BigFormat;
import org.apache.cassandra.io.sstable.metadata.MetadataCollector;
import org.apache.cassandra.io.sstable.metadata.MetadataComponent;
import org.apache.cassandra.io.sstable.metadata.MetadataType;
import org.apache.cassandra.io.sstable.metadata.StatsMetadata;
import org.apache.cassandra.io.util.DiskOptimizationStrategy;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.schema.CompressionParams;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.utils.ChecksumType;
import org.apache.cassandra.utils.concurrent.Transactional;


public abstract class SSTableWriter extends SSTable implements Transactional {
	protected long repairedAt;

	protected long maxDataAge = -1;

	protected final long keyCount;

	protected final MetadataCollector metadataCollector;

	protected final RowIndexEntry.IndexSerializer rowIndexEntrySerializer;

	protected final SerializationHeader header;

	protected final SSTableWriter.TransactionalProxy txnProxy = txnProxy();

	protected final Collection<SSTableFlushObserver> observers;

	protected abstract SSTableWriter.TransactionalProxy txnProxy();

	protected abstract class TransactionalProxy extends Transactional.AbstractTransactional {
		protected SSTableReader finalReader;

		protected boolean openResult;
	}

	protected SSTableWriter(Descriptor descriptor, long keyCount, long repairedAt, CFMetaData metadata, MetadataCollector metadataCollector, SerializationHeader header, Collection<SSTableFlushObserver> observers) {
		super(descriptor, SSTableWriter.components(metadata), metadata, DatabaseDescriptor.getDiskOptimizationStrategy());
		this.keyCount = keyCount;
		this.repairedAt = repairedAt;
		this.metadataCollector = metadataCollector;
		this.header = (header != null) ? header : SerializationHeader.makeWithoutStats(metadata);
		this.rowIndexEntrySerializer = descriptor.version.getSSTableFormat().getIndexSerializer(metadata, descriptor.version, header);
		this.observers = (observers == null) ? Collections.emptySet() : observers;
	}

	public static SSTableWriter create(Descriptor descriptor, Long keyCount, Long repairedAt, CFMetaData metadata, MetadataCollector metadataCollector, SerializationHeader header, Collection<Index> indexes, LifecycleTransaction txn) {
		return null;
	}

	public static SSTableWriter create(Descriptor descriptor, long keyCount, long repairedAt, int sstableLevel, SerializationHeader header, Collection<Index> indexes, LifecycleTransaction txn) {
		CFMetaData metadata = Schema.instance.getCFMetaData(descriptor);
		return SSTableWriter.create(metadata, descriptor, keyCount, repairedAt, sstableLevel, header, indexes, txn);
	}

	public static SSTableWriter create(CFMetaData metadata, Descriptor descriptor, long keyCount, long repairedAt, int sstableLevel, SerializationHeader header, Collection<Index> indexes, LifecycleTransaction txn) {
		MetadataCollector collector = new MetadataCollector(metadata.comparator).sstableLevel(sstableLevel);
		return SSTableWriter.create(descriptor, keyCount, repairedAt, metadata, collector, header, indexes, txn);
	}

	public static SSTableWriter create(String filename, long keyCount, long repairedAt, int sstableLevel, SerializationHeader header, Collection<Index> indexes, LifecycleTransaction txn) {
		return SSTableWriter.create(Descriptor.fromFilename(filename), keyCount, repairedAt, sstableLevel, header, indexes, txn);
	}

	@com.google.common.annotations.VisibleForTesting
	public static SSTableWriter create(String filename, long keyCount, long repairedAt, SerializationHeader header, Collection<Index> indexes, LifecycleTransaction txn) {
		Descriptor descriptor = Descriptor.fromFilename(filename);
		return SSTableWriter.create(descriptor, keyCount, repairedAt, 0, header, indexes, txn);
	}

	private static Set<Component> components(CFMetaData metadata) {
		Set<Component> components = new HashSet<Component>(Arrays.asList(Component.DATA, Component.PRIMARY_INDEX, Component.STATS, Component.SUMMARY, Component.TOC, Component.digestFor(BigFormat.latestVersion.uncompressedChecksumType())));
		if ((metadata.params.bloomFilterFpChance) < 1.0)
			components.add(Component.FILTER);

		if (metadata.params.compression.isEnabled()) {
			components.add(Component.COMPRESSION_INFO);
		}else {
			components.add(Component.CRC);
		}
		return components;
	}

	private static Collection<SSTableFlushObserver> observers(Descriptor descriptor, Collection<Index> indexes, OperationType operationType) {
		if (indexes == null)
			return Collections.emptyList();

		List<SSTableFlushObserver> observers = new ArrayList<>(indexes.size());
		for (Index index : indexes) {
			SSTableFlushObserver observer = index.getFlushObserver(descriptor, operationType);
			if (observer != null) {
				observer.begin();
				observers.add(observer);
			}
		}
		return ImmutableList.copyOf(observers);
	}

	public abstract void mark();

	public abstract RowIndexEntry append(UnfilteredRowIterator iterator);

	public abstract long getFilePointer();

	public abstract long getOnDiskFilePointer();

	public long getEstimatedOnDiskBytesWritten() {
		return getOnDiskFilePointer();
	}

	public abstract void resetAndTruncate();

	public SSTableWriter setRepairedAt(long repairedAt) {
		if (repairedAt > 0)
			this.repairedAt = repairedAt;

		return this;
	}

	public SSTableWriter setMaxDataAge(long maxDataAge) {
		this.maxDataAge = maxDataAge;
		return this;
	}

	public SSTableWriter setOpenResult(boolean openResult) {
		txnProxy.openResult = openResult;
		return this;
	}

	public abstract SSTableReader openEarly();

	public abstract SSTableReader openFinalEarly();

	public SSTableReader finish(long repairedAt, long maxDataAge, boolean openResult) {
		if (repairedAt > 0)
			this.repairedAt = repairedAt;

		this.maxDataAge = maxDataAge;
		return finish(openResult);
	}

	public SSTableReader finish(boolean openResult) {
		setOpenResult(openResult);
		txnProxy.finish();
		observers.forEach(SSTableFlushObserver::complete);
		return finished();
	}

	public SSTableReader finished() {
		return txnProxy.finalReader;
	}

	public final void prepareToCommit() {
		txnProxy.prepareToCommit();
	}

	public final Throwable commit(Throwable accumulate) {
		try {
			return txnProxy.commit(accumulate);
		} finally {
			observers.forEach(SSTableFlushObserver::complete);
		}
	}

	public final Throwable abort(Throwable accumulate) {
		return txnProxy.abort(accumulate);
	}

	public final void close() {
		txnProxy.close();
	}

	public final void abort() {
		txnProxy.abort();
	}

	protected Map<MetadataType, MetadataComponent> finalizeMetadata() {
		return metadataCollector.finalizeMetadata(getPartitioner().getClass().getCanonicalName(), metadata.params.bloomFilterFpChance, repairedAt, header);
	}

	protected StatsMetadata statsMetadata() {
		return ((StatsMetadata) (finalizeMetadata().get(MetadataType.STATS)));
	}

	public static void rename(Descriptor tmpdesc, Descriptor newdesc, Set<Component> components) {
		for (Component component : Sets.difference(components, Sets.newHashSet(Component.DATA, Component.SUMMARY))) {
			FileUtils.renameWithConfirm(tmpdesc.filenameFor(component), newdesc.filenameFor(component));
		}
		FileUtils.renameWithConfirm(tmpdesc.filenameFor(Component.DATA), newdesc.filenameFor(Component.DATA));
		FileUtils.renameWithOutConfirm(tmpdesc.filenameFor(Component.SUMMARY), newdesc.filenameFor(Component.SUMMARY));
	}

	public abstract static class Factory {
		public abstract SSTableWriter open(Descriptor descriptor, long keyCount, long repairedAt, CFMetaData metadata, MetadataCollector metadataCollector, SerializationHeader header, Collection<SSTableFlushObserver> observers, LifecycleTransaction txn);
	}
}

