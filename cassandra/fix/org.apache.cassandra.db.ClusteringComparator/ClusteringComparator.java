

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.cassandra.db.Clusterable;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.io.sstable.IndexInfo;


public class ClusteringComparator implements Comparator<Clusterable> {
	private final List<AbstractType<?>> clusteringTypes;

	private final Comparator<IndexInfo> indexComparator;

	private final Comparator<IndexInfo> indexReverseComparator;

	private final Comparator<Clusterable> reverseComparator;

	private final Comparator<Row> rowComparator = ( r1, r2) -> compare(r1.clustering(), r2.clustering());

	public ClusteringComparator(AbstractType<?>... clusteringTypes) {
		this(ImmutableList.copyOf(clusteringTypes));
	}

	public ClusteringComparator(List<AbstractType<?>> clusteringTypes) {
		this.clusteringTypes = ImmutableList.copyOf(clusteringTypes);
		this.indexComparator = ( o1, o2) -> this.compare(o1.lastName, o2.lastName);
		this.indexReverseComparator = ( o1, o2) -> this.compare(o1.firstName, o2.firstName);
		this.reverseComparator = ( c1, c2) -> this.compare(c2, c1);
		for (AbstractType<?> type : clusteringTypes)
			type.checkComparable();

	}

	public int size() {
		return clusteringTypes.size();
	}

	public List<AbstractType<?>> subtypes() {
		return clusteringTypes;
	}

	public AbstractType<?> subtype(int i) {
		return clusteringTypes.get(i);
	}

	public Clustering make(Object... values) {
		if ((values.length) != (size()))
			throw new IllegalArgumentException(String.format("Invalid number of components, expecting %d but got %d", size(), values.length));

		for (Object val : values) {
			if (val instanceof ByteBuffer) {
			}else {
			}
		}
		return null;
	}

	public int compare(Clusterable c1, Clusterable c2) {
		return compare(c1.clustering(), c2.clustering());
	}

	public int compare(ClusteringPrefix c1, ClusteringPrefix c2) {
		int s1 = c1.size();
		int s2 = c2.size();
		int minSize = Math.min(s1, s2);
		for (int i = 0; i < minSize; i++) {
			int cmp = compareComponent(i, c1.get(i), c2.get(i));
			if (cmp != 0)
				return cmp;

		}
		if (s1 == s2)
			return ClusteringPrefix.Kind.compare(c1.kind(), c2.kind());

		return s1 < s2 ? c1.kind().comparedToClustering : -(c2.kind().comparedToClustering);
	}

	public int compare(Clustering c1, Clustering c2) {
		return compare(c1, c2, size());
	}

	public int compare(Clustering c1, Clustering c2, int size) {
		for (int i = 0; i < size; i++) {
			int cmp = compareComponent(i, c1.get(i), c2.get(i));
			if (cmp != 0)
				return cmp;

		}
		return 0;
	}

	public int compareComponent(int i, ByteBuffer v1, ByteBuffer v2) {
		if (v1 == null)
			return v2 == null ? 0 : -1;

		if (v2 == null)
			return 1;

		return clusteringTypes.get(i).compare(v1, v2);
	}

	public boolean isCompatibleWith(ClusteringComparator previous) {
		if ((this) == previous)
			return true;

		if ((size()) < (previous.size()))
			return false;

		for (int i = 0; i < (previous.size()); i++) {
			AbstractType<?> tprev = previous.subtype(i);
			AbstractType<?> tnew = subtype(i);
			if (!(tnew.isCompatibleWith(tprev)))
				return false;

		}
		return true;
	}

	public void validate(ClusteringPrefix clustering) {
		for (int i = 0; i < (clustering.size()); i++) {
			ByteBuffer value = clustering.get(i);
			if (value != null)
				subtype(i).validate(value);

		}
	}

	public Comparator<Row> rowComparator() {
		return rowComparator;
	}

	public Comparator<IndexInfo> indexComparator(boolean reversed) {
		return reversed ? indexReverseComparator : indexComparator;
	}

	public Comparator<Clusterable> reversed() {
		return reverseComparator;
	}

	@Override
	public String toString() {
		return String.format("comparator(%s)", Joiner.on(", ").join(clusteringTypes));
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if (!(o instanceof ClusteringComparator))
			return false;

		ClusteringComparator that = ((ClusteringComparator) (o));
		return this.clusteringTypes.equals(that.clusteringTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(clusteringTypes);
	}
}

