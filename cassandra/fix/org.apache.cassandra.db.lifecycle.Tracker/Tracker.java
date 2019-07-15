

import com.codahale.metrics.Counter;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.Memtable;
import org.apache.cassandra.db.commitlog.CommitLogPosition;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.lifecycle.SSTableSet;
import org.apache.cassandra.db.lifecycle.View;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.metrics.StorageMetrics;
import org.apache.cassandra.metrics.TableMetrics;
import org.apache.cassandra.notifications.INotification;
import org.apache.cassandra.notifications.INotificationConsumer;
import org.apache.cassandra.notifications.MemtableDiscardedNotification;
import org.apache.cassandra.notifications.MemtableRenewedNotification;
import org.apache.cassandra.notifications.MemtableSwitchedNotification;
import org.apache.cassandra.notifications.SSTableAddedNotification;
import org.apache.cassandra.notifications.SSTableDeletingNotification;
import org.apache.cassandra.notifications.SSTableListChangedNotification;
import org.apache.cassandra.notifications.SSTableRepairStatusChanged;
import org.apache.cassandra.notifications.TruncationNotification;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.Throwables;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Tracker {
	private static final Logger logger = LoggerFactory.getLogger(Tracker.class);

	private final Collection<INotificationConsumer> subscribers = new CopyOnWriteArrayList<>();

	public final ColumnFamilyStore cfstore;

	final AtomicReference<View> view;

	public final boolean loadsstables;

	public Tracker(Memtable memtable, boolean loadsstables) {
		this.cfstore = (memtable != null) ? memtable.cfs : null;
		this.view = new AtomicReference<>();
		this.loadsstables = loadsstables;
		this.reset(memtable);
	}

	public LifecycleTransaction tryModify(SSTableReader sstable, OperationType operationType) {
		return tryModify(Collections.singleton(sstable), operationType);
	}

	public LifecycleTransaction tryModify(Iterable<SSTableReader> sstables, OperationType operationType) {
		if (Iterables.isEmpty(sstables)) {
		}
		return null;
	}

	Pair<View, View> apply(Function<View, View> function) {
		return apply(Predicates.<View>alwaysTrue(), function);
	}

	Throwable apply(Function<View, View> function, Throwable accumulate) {
		try {
			apply(function);
		} catch (Throwable t) {
			accumulate = Throwables.merge(accumulate, t);
		}
		return accumulate;
	}

	Pair<View, View> apply(Predicate<View> permit, Function<View, View> function) {
		while (true) {
			View cur = view.get();
			if (!(permit.apply(cur)))
				return null;

			View updated = function.apply(cur);
			if (view.compareAndSet(cur, updated))
				return Pair.create(cur, updated);

		} 
	}

	Throwable updateSizeTracking(Iterable<SSTableReader> oldSSTables, Iterable<SSTableReader> newSSTables, Throwable accumulate) {
		if (isDummy())
			return accumulate;

		long add = 0;
		for (SSTableReader sstable : newSSTables) {
			if (Tracker.logger.isTraceEnabled())
				Tracker.logger.trace("adding {} to list of files tracked for {}.{}", sstable.descriptor, cfstore.keyspace.getName(), cfstore.name);

			try {
				add += sstable.bytesOnDisk();
			} catch (Throwable t) {
				accumulate = Throwables.merge(accumulate, t);
			}
		}
		long subtract = 0;
		for (SSTableReader sstable : oldSSTables) {
			if (Tracker.logger.isTraceEnabled())
				Tracker.logger.trace("removing {} from list of files tracked for {}.{}", sstable.descriptor, cfstore.keyspace.getName(), cfstore.name);

			try {
				subtract += sstable.bytesOnDisk();
			} catch (Throwable t) {
				accumulate = Throwables.merge(accumulate, t);
			}
		}
		StorageMetrics.load.inc((add - subtract));
		cfstore.metric.liveDiskSpaceUsed.inc((add - subtract));
		cfstore.metric.totalDiskSpaceUsed.inc(add);
		return accumulate;
	}

	public void addInitialSSTables(Iterable<SSTableReader> sstables) {
		Throwables.maybeFail(updateSizeTracking(Tracker.emptySet(), sstables, null));
	}

	public void addSSTables(Iterable<SSTableReader> sstables) {
		addInitialSSTables(sstables);
		maybeIncrementallyBackup(sstables);
		notifyAdded(sstables);
	}

	@com.google.common.annotations.VisibleForTesting
	public void reset(Memtable memtable) {
	}

	public Throwable dropSSTablesIfInvalid(Throwable accumulate) {
		if ((!(isDummy())) && (!(cfstore.isValid())))
			accumulate = dropSSTables(accumulate);

		return accumulate;
	}

	public void dropSSTables() {
		Throwables.maybeFail(dropSSTables(null));
	}

	public Throwable dropSSTables(Throwable accumulate) {
		return dropSSTables(Predicates.<SSTableReader>alwaysTrue(), OperationType.UNKNOWN, accumulate);
	}

	public Throwable dropSSTables(final Predicate<SSTableReader> remove, OperationType operationType, Throwable accumulate) {
		return accumulate;
	}

	public void removeUnreadableSSTables(final File directory) {
		Throwables.maybeFail(dropSSTables(new Predicate<SSTableReader>() {
			public boolean apply(SSTableReader reader) {
				return reader.descriptor.directory.equals(directory);
			}
		}, OperationType.UNKNOWN, null));
	}

	public Memtable getMemtableFor(OpOrder.Group opGroup, CommitLogPosition commitLogPosition) {
		for (Memtable memtable : view.get().liveMemtables) {
			if (memtable.accepts(opGroup, commitLogPosition))
				return memtable;

		}
		throw new AssertionError(view.get().liveMemtables.toString());
	}

	public void markFlushing(Memtable memtable) {
	}

	public void replaceFlushed(Memtable memtable, Iterable<SSTableReader> sstables) {
		assert !(isDummy());
		if (Iterables.isEmpty(sstables)) {
			return;
		}
		sstables.forEach(SSTableReader::setupOnline);
		maybeIncrementallyBackup(sstables);
		Throwable fail;
		fail = updateSizeTracking(Tracker.emptySet(), sstables, null);
		notifyDiscarded(memtable);
		fail = notifyAdded(sstables, fail);
		if ((!(isDummy())) && (!(cfstore.isValid())))
			dropSSTables();

		Throwables.maybeFail(fail);
	}

	public Set<SSTableReader> getCompacting() {
		return null;
	}

	public Iterable<SSTableReader> getUncompacting() {
		return view.get().select(SSTableSet.NONCOMPACTING);
	}

	public Iterable<SSTableReader> getUncompacting(Iterable<SSTableReader> candidates) {
		return view.get().getUncompacting(candidates);
	}

	public void maybeIncrementallyBackup(final Iterable<SSTableReader> sstables) {
		if (!(DatabaseDescriptor.isIncrementalBackupsEnabled()))
			return;

		for (SSTableReader sstable : sstables) {
			File backupsDir = Directories.getBackupsDirectory(sstable.descriptor);
			sstable.createLinks(FileUtils.getCanonicalPath(backupsDir));
		}
	}

	Throwable notifySSTablesChanged(Collection<SSTableReader> removed, Collection<SSTableReader> added, OperationType compactionType, Throwable accumulate) {
		INotification notification = new SSTableListChangedNotification(added, removed, compactionType);
		for (INotificationConsumer subscriber : subscribers) {
			try {
				subscriber.handleNotification(notification, this);
			} catch (Throwable t) {
				accumulate = Throwables.merge(accumulate, t);
			}
		}
		return accumulate;
	}

	Throwable notifyAdded(Iterable<SSTableReader> added, Throwable accumulate) {
		INotification notification = new SSTableAddedNotification(added);
		for (INotificationConsumer subscriber : subscribers) {
			try {
				subscriber.handleNotification(notification, this);
			} catch (Throwable t) {
				accumulate = Throwables.merge(accumulate, t);
			}
		}
		return accumulate;
	}

	public void notifyAdded(Iterable<SSTableReader> added) {
		Throwables.maybeFail(notifyAdded(added, null));
	}

	public void notifySSTableRepairedStatusChanged(Collection<SSTableReader> repairStatusesChanged) {
		INotification notification = new SSTableRepairStatusChanged(repairStatusesChanged);
		for (INotificationConsumer subscriber : subscribers)
			subscriber.handleNotification(notification, this);

	}

	public void notifyDeleting(SSTableReader deleting) {
		INotification notification = new SSTableDeletingNotification(deleting);
		for (INotificationConsumer subscriber : subscribers)
			subscriber.handleNotification(notification, this);

	}

	public void notifyTruncated(long truncatedAt) {
		INotification notification = new TruncationNotification(truncatedAt);
		for (INotificationConsumer subscriber : subscribers)
			subscriber.handleNotification(notification, this);

	}

	public void notifyRenewed(Memtable renewed) {
		notify(new MemtableRenewedNotification(renewed));
	}

	public void notifySwitched(Memtable previous) {
		notify(new MemtableSwitchedNotification(previous));
	}

	public void notifyDiscarded(Memtable discarded) {
		notify(new MemtableDiscardedNotification(discarded));
	}

	private void notify(INotification notification) {
		for (INotificationConsumer subscriber : subscribers)
			subscriber.handleNotification(notification, this);

	}

	public boolean isDummy() {
		return ((cfstore) == null) || (!(DatabaseDescriptor.isDaemonInitialized()));
	}

	public void subscribe(INotificationConsumer consumer) {
		subscribers.add(consumer);
	}

	public void unsubscribe(INotificationConsumer consumer) {
		subscribers.remove(consumer);
	}

	private static Set<SSTableReader> emptySet() {
		return Collections.emptySet();
	}

	public View getView() {
		return view.get();
	}

	@com.google.common.annotations.VisibleForTesting
	public void removeUnsafe(Set<SSTableReader> toRemove) {
	}
}

