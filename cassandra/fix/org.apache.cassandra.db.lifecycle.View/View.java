

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Memtable;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.lifecycle.SSTableIntervalTree;
import org.apache.cassandra.db.lifecycle.SSTableSet;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.utils.Interval;
import org.apache.cassandra.utils.IntervalTree;

import static org.apache.cassandra.io.sstable.format.SSTableReader.OpenReason.EARLY;


public class View {
	public final List<Memtable> liveMemtables;

	public final List<Memtable> flushingMemtables;

	final Set<SSTableReader> compacting;

	final Set<SSTableReader> sstables;

	final Map<SSTableReader, SSTableReader> sstablesMap;

	final Map<SSTableReader, SSTableReader> compactingMap;

	final SSTableIntervalTree intervalTree;

	View(List<Memtable> liveMemtables, List<Memtable> flushingMemtables, Map<SSTableReader, SSTableReader> sstables, Map<SSTableReader, SSTableReader> compacting, SSTableIntervalTree intervalTree) {
		assert liveMemtables != null;
		assert flushingMemtables != null;
		assert sstables != null;
		assert compacting != null;
		assert intervalTree != null;
		this.liveMemtables = liveMemtables;
		this.flushingMemtables = flushingMemtables;
		this.sstablesMap = sstables;
		this.sstables = sstablesMap.keySet();
		this.compactingMap = compacting;
		this.compacting = compactingMap.keySet();
		this.intervalTree = intervalTree;
	}

	public Memtable getCurrentMemtable() {
		return liveMemtables.get(((liveMemtables.size()) - 1));
	}

	public Iterable<Memtable> getAllMemtables() {
		return Iterables.concat(flushingMemtables, liveMemtables);
	}

	public Set<SSTableReader> liveSSTables() {
		return sstables;
	}

	public Iterable<SSTableReader> sstables(SSTableSet sstableSet, Predicate<SSTableReader> filter) {
		return Iterables.filter(select(sstableSet), filter);
	}

	@com.google.common.annotations.VisibleForTesting
	public Iterable<SSTableReader> allKnownSSTables() {
		return null;
	}

	public Iterable<SSTableReader> select(SSTableSet sstableSet) {
		switch (sstableSet) {
			case LIVE :
				return sstables;
			case NONCOMPACTING :
				return Iterables.filter(sstables, ( s) -> !(compacting.contains(s)));
			case CANONICAL :
				Set<SSTableReader> canonicalSSTables = new HashSet<>();
				for (SSTableReader sstable : compacting)
					if ((sstable.openReason) != (EARLY))
						canonicalSSTables.add(sstable);


				for (SSTableReader sstable : sstables)
					if ((!(compacting.contains(sstable))) && ((sstable.openReason) != (EARLY)))
						canonicalSSTables.add(sstable);


				return canonicalSSTables;
			default :
				throw new IllegalStateException();
		}
	}

	public Iterable<SSTableReader> getUncompacting(Iterable<SSTableReader> candidates) {
		return Iterables.filter(candidates, new Predicate<SSTableReader>() {
			public boolean apply(SSTableReader sstable) {
				return !(compacting.contains(sstable));
			}
		});
	}

	public boolean isEmpty() {
		return (((sstables.isEmpty()) && ((liveMemtables.size()) <= 1)) && ((flushingMemtables.size()) == 0)) && (((liveMemtables.size()) == 0) || ((liveMemtables.get(0).getOperations()) == 0));
	}

	@Override
	public String toString() {
		return String.format("View(pending_count=%d, sstables=%s, compacting=%s)", (((liveMemtables.size()) + (flushingMemtables.size())) - 1), sstables, compacting);
	}

	public Iterable<SSTableReader> liveSSTablesInBounds(PartitionPosition left, PartitionPosition right) {
		assert !(AbstractBounds.strictlyWrapsAround(left, right));
		if (intervalTree.isEmpty())
			return Collections.emptyList();

		PartitionPosition stopInTree = (right.isMinimum()) ? intervalTree.max() : right;
		return intervalTree.search(Interval.create(left, stopInTree));
	}

	public static List<SSTableReader> sstablesInBounds(PartitionPosition left, PartitionPosition right, SSTableIntervalTree intervalTree) {
		assert !(AbstractBounds.strictlyWrapsAround(left, right));
		if (intervalTree.isEmpty())
			return Collections.emptyList();

		PartitionPosition stopInTree = (right.isMinimum()) ? intervalTree.max() : right;
		return intervalTree.search(Interval.create(left, stopInTree));
	}

	public static Function<View, Iterable<SSTableReader>> selectFunction(SSTableSet sstableSet) {
		return ( view) -> view.select(sstableSet);
	}

	public static Function<View, Iterable<SSTableReader>> select(SSTableSet sstableSet, Predicate<SSTableReader> filter) {
		return ( view) -> view.sstables(sstableSet, filter);
	}

	public static Function<View, Iterable<SSTableReader>> select(SSTableSet sstableSet, DecoratedKey key) {
		assert sstableSet == (SSTableSet.LIVE);
		return ( view) -> view.intervalTree.search(key);
	}

	public static Function<View, Iterable<SSTableReader>> selectLive(AbstractBounds<PartitionPosition> rowBounds) {
		return ( view) -> view.liveSSTablesInBounds(rowBounds.left, rowBounds.right);
	}

	static Function<View, View> updateCompacting(final Set<SSTableReader> unmark, final Iterable<SSTableReader> mark) {
		if ((unmark.isEmpty()) && (Iterables.isEmpty(mark)))
			return Functions.identity();

		return new Function<View, View>() {
			public View apply(View view) {
				return null;
			}
		};
	}

	static Predicate<View> permitCompacting(final Iterable<SSTableReader> readers) {
		return new Predicate<View>() {
			public boolean apply(View view) {
				for (SSTableReader reader : readers)
					if (((view.compacting.contains(reader)) || ((view.sstablesMap.get(reader)) != reader)) || (reader.isMarkedCompacted()))
						return false;


				return true;
			}
		};
	}

	static Function<View, View> updateLiveSet(final Set<SSTableReader> remove, final Iterable<SSTableReader> add) {
		if ((remove.isEmpty()) && (Iterables.isEmpty(add)))
			return Functions.identity();

		return new Function<View, View>() {
			public View apply(View view) {
				return null;
			}
		};
	}

	static Function<View, View> switchMemtable(final Memtable newMemtable) {
		return new Function<View, View>() {
			public View apply(View view) {
				List<Memtable> newLive = ImmutableList.<Memtable>builder().addAll(view.liveMemtables).add(newMemtable).build();
				assert (newLive.size()) == ((view.liveMemtables.size()) + 1);
				return new View(newLive, view.flushingMemtables, view.sstablesMap, view.compactingMap, view.intervalTree);
			}
		};
	}

	static Function<View, View> markFlushing(final Memtable toFlush) {
		return new Function<View, View>() {
			public View apply(View view) {
				List<Memtable> live = view.liveMemtables;
				List<Memtable> flushing = view.flushingMemtables;
				List<Memtable> newLive = ImmutableList.copyOf(Iterables.filter(live, Predicates.not(Predicates.equalTo(toFlush))));
				List<Memtable> newFlushing = ImmutableList.copyOf(Iterables.concat(Iterables.filter(flushing, View.lessThan(toFlush)), ImmutableList.of(toFlush), Iterables.filter(flushing, Predicates.not(View.lessThan(toFlush)))));
				assert (newLive.size()) == ((live.size()) - 1);
				assert (newFlushing.size()) == ((flushing.size()) + 1);
				return new View(newLive, newFlushing, view.sstablesMap, view.compactingMap, view.intervalTree);
			}
		};
	}

	static Function<View, View> replaceFlushed(final Memtable memtable, final Iterable<SSTableReader> flushed) {
		return new Function<View, View>() {
			public View apply(View view) {
				List<Memtable> flushingMemtables = ImmutableList.copyOf(Iterables.filter(view.flushingMemtables, Predicates.not(Predicates.equalTo(memtable))));
				assert (flushingMemtables.size()) == ((view.flushingMemtables.size()) - 1);
				if ((flushed == null) || (Iterables.isEmpty(flushed)))
					return new View(view.liveMemtables, flushingMemtables, view.sstablesMap, view.compactingMap, view.intervalTree);

				return null;
			}
		};
	}

	private static <T extends Comparable<T>> Predicate<T> lessThan(final T lessThan) {
		return new Predicate<T>() {
			public boolean apply(T t) {
				return (t.compareTo(lessThan)) < 0;
			}
		};
	}
}

