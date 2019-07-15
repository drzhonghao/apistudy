

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.AbstractBufferClusteringPrefix;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringBoundary;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.utils.memory.AbstractAllocator;

import static org.apache.cassandra.db.ClusteringPrefix.Kind.values;


public abstract class ClusteringBoundOrBoundary extends AbstractBufferClusteringPrefix {
	public static final ClusteringBoundOrBoundary.Serializer serializer = new ClusteringBoundOrBoundary.Serializer();

	protected ClusteringBoundOrBoundary(ClusteringPrefix.Kind kind, ByteBuffer[] values) {
		super(kind, values);
		assert ((values.length) > 0) || (!(kind.isBoundary()));
	}

	public static ClusteringBoundOrBoundary create(ClusteringPrefix.Kind kind, ByteBuffer[] values) {
		return null;
	}

	public boolean isBoundary() {
		return kind.isBoundary();
	}

	public boolean isOpen(boolean reversed) {
		return kind.isOpen(reversed);
	}

	public boolean isClose(boolean reversed) {
		return kind.isClose(reversed);
	}

	public static ClusteringBound inclusiveOpen(boolean reversed, ByteBuffer[] boundValues) {
		return null;
	}

	public static ClusteringBound exclusiveOpen(boolean reversed, ByteBuffer[] boundValues) {
		return null;
	}

	public static ClusteringBound inclusiveClose(boolean reversed, ByteBuffer[] boundValues) {
		return null;
	}

	public static ClusteringBound exclusiveClose(boolean reversed, ByteBuffer[] boundValues) {
		return null;
	}

	public static ClusteringBoundary inclusiveCloseExclusiveOpen(boolean reversed, ByteBuffer[] boundValues) {
		return null;
	}

	public static ClusteringBoundary exclusiveCloseInclusiveOpen(boolean reversed, ByteBuffer[] boundValues) {
		return null;
	}

	public ClusteringBoundOrBoundary copy(AbstractAllocator allocator) {
		ByteBuffer[] newValues = new ByteBuffer[size()];
		for (int i = 0; i < (size()); i++)
			newValues[i] = allocator.clone(get(i));

		return ClusteringBoundOrBoundary.create(kind(), newValues);
	}

	public String toString(CFMetaData metadata) {
		return toString(metadata.comparator);
	}

	public String toString(ClusteringComparator comparator) {
		StringBuilder sb = new StringBuilder();
		sb.append(kind()).append('(');
		for (int i = 0; i < (size()); i++) {
			if (i > 0)
				sb.append(", ");

			sb.append(comparator.subtype(i).getString(get(i)));
		}
		return sb.append(')').toString();
	}

	public abstract ClusteringBoundOrBoundary invert();

	public static class Serializer {
		public void serialize(ClusteringBoundOrBoundary bound, DataOutputPlus out, int version, List<AbstractType<?>> types) throws IOException {
			out.writeByte(bound.kind().ordinal());
			out.writeShort(bound.size());
		}

		public long serializedSize(ClusteringBoundOrBoundary bound, int version, List<AbstractType<?>> types) {
			return 0L;
		}

		public ClusteringBoundOrBoundary deserialize(DataInputPlus in, int version, List<AbstractType<?>> types) throws IOException {
			ClusteringPrefix.Kind kind = values()[in.readByte()];
			return deserializeValues(in, kind, version, types);
		}

		public void skipValues(DataInputPlus in, ClusteringPrefix.Kind kind, int version, List<AbstractType<?>> types) throws IOException {
			int size = in.readUnsignedShort();
			if (size == 0)
				return;

		}

		public ClusteringBoundOrBoundary deserializeValues(DataInputPlus in, ClusteringPrefix.Kind kind, int version, List<AbstractType<?>> types) throws IOException {
			int size = in.readUnsignedShort();
			if (size == 0) {
			}
			return null;
		}
	}
}

