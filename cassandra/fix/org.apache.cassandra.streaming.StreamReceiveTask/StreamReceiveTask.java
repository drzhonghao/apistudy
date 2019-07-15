

import com.google.common.collect.Iterables;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.apache.cassandra.concurrent.NamedThreadFactory;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.ViewDefinition;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.commitlog.CommitLogPosition;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.view.View;
import org.apache.cassandra.dht.Bounds;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.index.SecondaryIndexManager;
import org.apache.cassandra.io.sstable.ISSTableScanner;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.SSTableMultiWriter;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.streaming.StreamSession;
import org.apache.cassandra.streaming.StreamTask;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.Throwables;
import org.apache.cassandra.utils.concurrent.Refs;
import org.apache.cassandra.utils.concurrent.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StreamReceiveTask extends StreamTask {
	private static final Logger logger = LoggerFactory.getLogger(StreamReceiveTask.class);

	private static final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("StreamReceiveTask"));

	private final int totalFiles;

	private final long totalSize;

	private final LifecycleTransaction txn;

	private volatile boolean done = false;

	protected Collection<SSTableReader> sstables;

	private int remoteSSTablesReceived = 0;

	public StreamReceiveTask(StreamSession session, UUID cfId, int totalFiles, long totalSize) {
		super(session, cfId);
		this.totalFiles = totalFiles;
		this.totalSize = totalSize;
		this.txn = LifecycleTransaction.offline(OperationType.STREAM);
		this.sstables = new ArrayList<>(totalFiles);
	}

	public synchronized void received(SSTableMultiWriter sstable) {
		if (done) {
			StreamReceiveTask.logger.warn("[{}] Received sstable {} on already finished stream received task. Aborting sstable.", session.planId(), sstable.getFilename());
			Throwables.maybeFail(sstable.abort(null));
			return;
		}
		(remoteSSTablesReceived)++;
		assert cfId.equals(sstable.getCfId());
		Collection<SSTableReader> finished = null;
		try {
			finished = sstable.finish(true);
		} catch (Throwable t) {
			Throwables.maybeFail(sstable.abort(t));
		}
		txn.update(finished, false);
		sstables.addAll(finished);
		if ((remoteSSTablesReceived) == (totalFiles)) {
			done = true;
			StreamReceiveTask.executor.submit(new StreamReceiveTask.OnCompletionRunnable(this));
		}
	}

	public int getTotalNumberOfFiles() {
		return totalFiles;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public synchronized LifecycleTransaction getTransaction() {
		if (done)
			throw new RuntimeException(String.format("Stream receive task %s of cf %s already finished.", session.planId(), cfId));

		return txn;
	}

	private static class OnCompletionRunnable implements Runnable {
		private final StreamReceiveTask task;

		public OnCompletionRunnable(StreamReceiveTask task) {
			this.task = task;
		}

		public void run() {
			boolean hasViews = false;
			boolean hasCDC = false;
			ColumnFamilyStore cfs = null;
			try {
				Pair<String, String> kscf = Schema.instance.getCF(task.cfId);
				if (kscf == null) {
					task.sstables.clear();
					task.abortTransaction();
					return;
				}
				cfs = Keyspace.open(kscf.left).getColumnFamilyStore(kscf.right);
				hasViews = !(Iterables.isEmpty(View.findAll(kscf.left, kscf.right)));
				hasCDC = cfs.metadata.params.cdc;
				Collection<SSTableReader> readers = task.sstables;
				try (final Refs<SSTableReader> refs = Refs.ref(readers)) {
					if (hasViews || hasCDC) {
						for (SSTableReader reader : readers) {
							Keyspace ks = Keyspace.open(reader.getKeyspaceName());
							try (final ISSTableScanner scanner = reader.getScanner()) {
								while (scanner.hasNext()) {
									try (final UnfilteredRowIterator rowIterator = scanner.next()) {
										Mutation m = new Mutation(PartitionUpdate.fromIterator(rowIterator, ColumnFilter.all(cfs.metadata)));
										ks.apply(m, hasCDC, true, false);
									}
								} 
							}
						}
					}else {
						task.finishTransaction();
						StreamReceiveTask.logger.debug("[Stream #{}] Received {} sstables from {} ({})", task.session.planId(), readers.size(), task.session.peer, readers);
						cfs.addSSTables(readers);
						cfs.indexManager.buildAllIndexesBlocking(readers);
						if ((cfs.isRowCacheEnabled()) || (cfs.metadata.isCounter())) {
							List<Bounds<Token>> boundsToInvalidate = new ArrayList<>(readers.size());
							readers.forEach(( sstable) -> boundsToInvalidate.add(new Bounds<Token>(sstable.first.getToken(), sstable.last.getToken())));
							Set<Bounds<Token>> nonOverlappingBounds = Bounds.getNonOverlappingBounds(boundsToInvalidate);
							if (cfs.isRowCacheEnabled()) {
								int invalidatedKeys = cfs.invalidateRowCache(nonOverlappingBounds);
								if (invalidatedKeys > 0)
									StreamReceiveTask.logger.debug(("[Stream #{}] Invalidated {} row cache entries on table {}.{} after stream " + "receive task completed."), task.session.planId(), invalidatedKeys, cfs.keyspace.getName(), cfs.getTableName());

							}
							if (cfs.metadata.isCounter()) {
								int invalidatedKeys = cfs.invalidateCounterCache(nonOverlappingBounds);
								if (invalidatedKeys > 0)
									StreamReceiveTask.logger.debug(("[Stream #{}] Invalidated {} counter cache entries on table {}.{} after stream " + "receive task completed."), task.session.planId(), invalidatedKeys, cfs.keyspace.getName(), cfs.getTableName());

							}
						}
					}
				}
			} catch (Throwable t) {
				JVMStabilityInspector.inspectThrowable(t);
				task.session.onError(t);
			} finally {
				if (hasViews || hasCDC) {
					if (cfs != null)
						cfs.forceBlockingFlush();

					task.abortTransaction();
				}
			}
		}
	}

	public synchronized void abort() {
		if (done)
			return;

		done = true;
		abortTransaction();
		sstables.clear();
	}

	private synchronized void abortTransaction() {
		txn.abort();
	}

	private synchronized void finishTransaction() {
		txn.finish();
	}
}

