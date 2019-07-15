

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.AbstractBufferClusteringPrefix;
import org.apache.cassandra.db.AbstractClusteringPrefix;
import org.apache.cassandra.db.CBuilder;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringBoundOrBoundary;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;


public class Slice {
	public static final Slice.Serializer serializer = new Slice.Serializer();

	public static final Slice ALL = new Slice(ClusteringBound.BOTTOM, ClusteringBound.TOP) {
		@Override
		public boolean includes(ClusteringComparator comparator, ClusteringPrefix clustering) {
			return true;
		}

		@Override
		public boolean intersects(ClusteringComparator comparator, List<ByteBuffer> minClusteringValues, List<ByteBuffer> maxClusteringValues) {
			return true;
		}

		@Override
		public String toString(ClusteringComparator comparator) {
			return "ALL";
		}
	};

	private final ClusteringBound start;

	private final ClusteringBound end;

	private Slice(ClusteringBound start, ClusteringBound end) {
		assert (start.isStart()) && (end.isEnd());
		this.start = start;
		this.end = end;
	}

	public static Slice make(ClusteringBound start, ClusteringBound end) {
		if ((start == (ClusteringBound.BOTTOM)) && (end == (ClusteringBound.TOP)))
			return Slice.ALL;

		return new Slice(start, end);
	}

	public static Slice make(ClusteringComparator comparator, Object... values) {
		CBuilder builder = CBuilder.create(comparator);
		for (Object val : values) {
			if (val instanceof ByteBuffer)
				builder.add(((ByteBuffer) (val)));
			else
				builder.add(val);

		}
		return new Slice(builder.buildBound(true, true), builder.buildBound(false, true));
	}

	public static Slice make(Clustering clustering) {
		assert clustering != (Clustering.STATIC_CLUSTERING);
		ByteBuffer[] values = Slice.extractValues(clustering);
		return new Slice(ClusteringBound.inclusiveStartOf(values), ClusteringBound.inclusiveEndOf(values));
	}

	public static Slice make(Clustering start, Clustering end) {
		assert (start != (Clustering.STATIC_CLUSTERING)) && (end != (Clustering.STATIC_CLUSTERING));
		ByteBuffer[] startValues = Slice.extractValues(start);
		ByteBuffer[] endValues = Slice.extractValues(end);
		return new Slice(ClusteringBound.inclusiveStartOf(startValues), ClusteringBound.inclusiveEndOf(endValues));
	}

	private static ByteBuffer[] extractValues(ClusteringPrefix clustering) {
		ByteBuffer[] values = new ByteBuffer[clustering.size()];
		for (int i = 0; i < (clustering.size()); i++)
			values[i] = clustering.get(i);

		return values;
	}

	public ClusteringBound start() {
		return start;
	}

	public ClusteringBound end() {
		return end;
	}

	public ClusteringBound open(boolean reversed) {
		return reversed ? end : start;
	}

	public ClusteringBound close(boolean reversed) {
		return reversed ? start : end;
	}

	public boolean isEmpty(ClusteringComparator comparator) {
		return Slice.isEmpty(comparator, start(), end());
	}

	public static boolean isEmpty(ClusteringComparator comparator, ClusteringBound start, ClusteringBound end) {
		assert (start.isStart()) && (end.isEnd());
		return (comparator.compare(end, start)) <= 0;
	}

	public boolean includes(ClusteringComparator comparator, ClusteringPrefix bound) {
		return ((comparator.compare(start, bound)) <= 0) && ((comparator.compare(bound, end)) <= 0);
	}

	public Slice forPaging(ClusteringComparator comparator, Clustering lastReturned, boolean inclusive, boolean reversed) {
		if (lastReturned == null)
			return this;

		if (reversed) {
			int cmp = comparator.compare(lastReturned, start);
			if ((cmp < 0) || ((!inclusive) && (cmp == 0)))
				return null;

			cmp = comparator.compare(end, lastReturned);
			if ((cmp < 0) || (inclusive && (cmp == 0)))
				return this;

			ByteBuffer[] values = Slice.extractValues(lastReturned);
			return new Slice(start, (inclusive ? ClusteringBound.inclusiveEndOf(values) : ClusteringBound.exclusiveEndOf(values)));
		}else {
			int cmp = comparator.compare(end, lastReturned);
			if ((cmp < 0) || ((!inclusive) && (cmp == 0)))
				return null;

			cmp = comparator.compare(lastReturned, start);
			if ((cmp < 0) || (inclusive && (cmp == 0)))
				return this;

			ByteBuffer[] values = Slice.extractValues(lastReturned);
			return new Slice((inclusive ? ClusteringBound.inclusiveStartOf(values) : ClusteringBound.exclusiveStartOf(values)), end);
		}
	}

	public boolean intersects(ClusteringComparator comparator, List<ByteBuffer> minClusteringValues, List<ByteBuffer> maxClusteringValues) {
		for (int j = 0; (j < (minClusteringValues.size())) && (j < (maxClusteringValues.size())); j++) {
			ByteBuffer s = (j < (start.size())) ? start.get(j) : null;
			ByteBuffer f = (j < (end.size())) ? end.get(j) : null;
			if ((j > 0) && (((j < (end.size())) && ((comparator.compareComponent(j, f, minClusteringValues.get(j))) < 0)) || ((j < (start.size())) && ((comparator.compareComponent(j, s, maxClusteringValues.get(j))) > 0))))
				return false;

			if (((j >= (start.size())) || (j >= (end.size()))) || ((comparator.compareComponent(j, s, f)) != 0))
				break;

		}
		return true;
	}

	public String toString(CFMetaData metadata) {
		return toString(metadata.comparator);
	}

	public String toString(ClusteringComparator comparator) {
		StringBuilder sb = new StringBuilder();
		sb.append((start.isInclusive() ? "[" : "("));
		for (int i = 0; i < (start.size()); i++) {
			if (i > 0)
				sb.append(':');

			sb.append(comparator.subtype(i).getString(start.get(i)));
		}
		sb.append(", ");
		for (int i = 0; i < (end.size()); i++) {
			if (i > 0)
				sb.append(':');

			sb.append(comparator.subtype(i).getString(end.get(i)));
		}
		sb.append((end.isInclusive() ? "]" : ")"));
		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Slice))
			return false;

		Slice that = ((Slice) (other));
		return (this.start().equals(that.start())) && (this.end().equals(that.end()));
	}

	@Override
	public int hashCode() {
		return Objects.hash(start(), end());
	}

	public static class Serializer {
		public void serialize(Slice slice, DataOutputPlus out, int version, List<AbstractType<?>> types) throws IOException {
			ClusteringBound.serializer.serialize(slice.start, out, version, types);
			ClusteringBound.serializer.serialize(slice.end, out, version, types);
		}

		public long serializedSize(Slice slice, int version, List<AbstractType<?>> types) {
			return (ClusteringBound.serializer.serializedSize(slice.start, version, types)) + (ClusteringBound.serializer.serializedSize(slice.end, version, types));
		}

		public Slice deserialize(DataInputPlus in, int version, List<AbstractType<?>> types) throws IOException {
			ClusteringBound start = ((ClusteringBound) (ClusteringBound.serializer.deserialize(in, version, types)));
			ClusteringBound end = ((ClusteringBound) (ClusteringBound.serializer.deserialize(in, version, types)));
			return new Slice(start, end);
		}
	}
}

