

import java.nio.ByteBuffer;
import java.util.List;
import org.apache.cassandra.db.AbstractBufferClusteringPrefix;
import org.apache.cassandra.db.CBuilder;
import org.apache.cassandra.db.ClusteringBoundOrBoundary;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.utils.memory.AbstractAllocator;

import static org.apache.cassandra.db.ClusteringPrefix.Kind.EXCL_END_BOUND;
import static org.apache.cassandra.db.ClusteringPrefix.Kind.EXCL_START_BOUND;
import static org.apache.cassandra.db.ClusteringPrefix.Kind.INCL_END_BOUND;
import static org.apache.cassandra.db.ClusteringPrefix.Kind.INCL_START_BOUND;


public class ClusteringBound extends ClusteringBoundOrBoundary {
	public static final ClusteringBound BOTTOM = new ClusteringBound(INCL_START_BOUND, AbstractBufferClusteringPrefix.EMPTY_VALUES_ARRAY);

	public static final ClusteringBound TOP = new ClusteringBound(INCL_END_BOUND, AbstractBufferClusteringPrefix.EMPTY_VALUES_ARRAY);

	protected ClusteringBound(ClusteringPrefix.Kind kind, ByteBuffer[] values) {
		super(kind, values);
	}

	public static ClusteringBound create(ClusteringPrefix.Kind kind, ByteBuffer[] values) {
		assert !(kind.isBoundary());
		return new ClusteringBound(kind, values);
	}

	public static ClusteringPrefix.Kind boundKind(boolean isStart, boolean isInclusive) {
		return isStart ? isInclusive ? INCL_START_BOUND : EXCL_START_BOUND : isInclusive ? INCL_END_BOUND : EXCL_END_BOUND;
	}

	public static ClusteringBound inclusiveStartOf(ByteBuffer... values) {
		return ClusteringBound.create(INCL_START_BOUND, values);
	}

	public static ClusteringBound inclusiveEndOf(ByteBuffer... values) {
		return ClusteringBound.create(INCL_END_BOUND, values);
	}

	public static ClusteringBound exclusiveStartOf(ByteBuffer... values) {
		return ClusteringBound.create(EXCL_START_BOUND, values);
	}

	public static ClusteringBound exclusiveEndOf(ByteBuffer... values) {
		return ClusteringBound.create(EXCL_END_BOUND, values);
	}

	public static ClusteringBound inclusiveStartOf(ClusteringPrefix prefix) {
		ByteBuffer[] values = new ByteBuffer[prefix.size()];
		for (int i = 0; i < (prefix.size()); i++)
			values[i] = prefix.get(i);

		return ClusteringBound.inclusiveStartOf(values);
	}

	public static ClusteringBound exclusiveStartOf(ClusteringPrefix prefix) {
		ByteBuffer[] values = new ByteBuffer[prefix.size()];
		for (int i = 0; i < (prefix.size()); i++)
			values[i] = prefix.get(i);

		return ClusteringBound.exclusiveStartOf(values);
	}

	public static ClusteringBound inclusiveEndOf(ClusteringPrefix prefix) {
		ByteBuffer[] values = new ByteBuffer[prefix.size()];
		for (int i = 0; i < (prefix.size()); i++)
			values[i] = prefix.get(i);

		return ClusteringBound.inclusiveEndOf(values);
	}

	public static ClusteringBound create(ClusteringComparator comparator, boolean isStart, boolean isInclusive, Object... values) {
		CBuilder builder = CBuilder.create(comparator);
		for (Object val : values) {
			if (val instanceof ByteBuffer)
				builder.add(((ByteBuffer) (val)));
			else
				builder.add(val);

		}
		return null;
	}

	@Override
	public ClusteringBound invert() {
		return ClusteringBound.create(kind().invert(), values);
	}

	public ClusteringBound copy(AbstractAllocator allocator) {
		return ((ClusteringBound) (super.copy(allocator)));
	}

	public boolean isStart() {
		return kind().isStart();
	}

	public boolean isEnd() {
		return !(isStart());
	}

	public boolean isInclusive() {
		return ((kind) == (INCL_START_BOUND)) || ((kind) == (INCL_END_BOUND));
	}

	public boolean isExclusive() {
		return ((kind) == (EXCL_START_BOUND)) || ((kind) == (EXCL_END_BOUND));
	}

	int compareTo(ClusteringComparator comparator, List<ByteBuffer> sstableBound) {
		for (int i = 0; i < (sstableBound.size()); i++) {
			if (i >= (size()))
				return isStart() ? -1 : 1;

			int cmp = comparator.compareComponent(i, get(i), sstableBound.get(i));
			if (cmp != 0)
				return cmp;

		}
		if ((size()) > (sstableBound.size()))
			return isStart() ? -1 : 1;

		return isInclusive() ? 0 : isStart() ? 1 : -1;
	}
}

