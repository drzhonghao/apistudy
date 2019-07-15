

import com.google.common.collect.Iterables;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.index.sasi.SSTableIndex;
import org.apache.cassandra.index.sasi.conf.ColumnIndex;
import org.apache.cassandra.index.sasi.conf.view.TermTree;
import org.apache.cassandra.index.sasi.plan.Expression;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.utils.Interval;
import org.apache.cassandra.utils.IntervalTree;


public class View implements Iterable<SSTableIndex> {
	private final Map<Descriptor, SSTableIndex> view;

	private TermTree termTree = null;

	private final AbstractType<?> keyValidator;

	private final IntervalTree<View.Key, SSTableIndex, Interval<View.Key, SSTableIndex>> keyIntervalTree;

	public View(ColumnIndex index, Set<SSTableIndex> indexes) {
		this(index, Collections.<SSTableIndex>emptyList(), Collections.<SSTableReader>emptyList(), indexes);
	}

	public View(ColumnIndex index, Collection<SSTableIndex> currentView, Collection<SSTableReader> oldSSTables, Set<SSTableIndex> newIndexes) {
		Map<Descriptor, SSTableIndex> newView = new HashMap<>();
		AbstractType<?> validator = index.getValidator();
		List<Interval<View.Key, SSTableIndex>> keyIntervals = new ArrayList<>();
		for (SSTableIndex sstableIndex : Iterables.concat(currentView, newIndexes)) {
			SSTableReader sstable = sstableIndex.getSSTable();
			if (((oldSSTables.contains(sstable)) || (sstable.isMarkedCompacted())) || (newView.containsKey(sstable.descriptor))) {
				sstableIndex.release();
				continue;
			}
			newView.put(sstable.descriptor, sstableIndex);
			keyIntervals.add(Interval.create(new View.Key(sstableIndex.minKey(), index.keyValidator()), new View.Key(sstableIndex.maxKey(), index.keyValidator()), sstableIndex));
		}
		this.view = newView;
		this.keyValidator = index.keyValidator();
		this.keyIntervalTree = IntervalTree.build(keyIntervals);
		if ((keyIntervalTree.intervalCount()) != (termTree.intervalCount()))
			throw new IllegalStateException(String.format("mismatched sizes for intervals tree for keys vs terms: %d != %d", keyIntervalTree.intervalCount(), termTree.intervalCount()));

		termTree = null;
	}

	public Set<SSTableIndex> match(Expression expression) {
		return termTree.search(expression);
	}

	public List<SSTableIndex> match(ByteBuffer minKey, ByteBuffer maxKey) {
		return keyIntervalTree.search(Interval.create(new View.Key(minKey, keyValidator), new View.Key(maxKey, keyValidator), ((SSTableIndex) (null))));
	}

	public Iterator<SSTableIndex> iterator() {
		return view.values().iterator();
	}

	public Collection<SSTableIndex> getIndexes() {
		return view.values();
	}

	private static class Key implements Comparable<View.Key> {
		private final ByteBuffer key;

		private final AbstractType<?> comparator;

		public Key(ByteBuffer key, AbstractType<?> comparator) {
			this.key = key;
			this.comparator = comparator;
		}

		public int compareTo(View.Key o) {
			return comparator.compare(key, o.key);
		}
	}
}

