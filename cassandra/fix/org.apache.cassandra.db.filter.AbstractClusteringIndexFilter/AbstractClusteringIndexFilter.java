

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.filter.ClusteringIndexFilter;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.ReversedType;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;

import static org.apache.cassandra.db.filter.ClusteringIndexFilter.Kind.values;


public abstract class AbstractClusteringIndexFilter implements ClusteringIndexFilter {
	static final ClusteringIndexFilter.Serializer serializer = new AbstractClusteringIndexFilter.FilterSerializer();

	protected final boolean reversed;

	protected AbstractClusteringIndexFilter(boolean reversed) {
		this.reversed = reversed;
	}

	public boolean isReversed() {
		return reversed;
	}

	protected abstract void serializeInternal(DataOutputPlus out, int version) throws IOException;

	protected abstract long serializedSizeInternal(int version);

	protected void appendOrderByToCQLString(CFMetaData metadata, StringBuilder sb) {
		if (reversed) {
			sb.append(" ORDER BY (");
			int i = 0;
			for (ColumnDefinition column : metadata.clusteringColumns())
				sb.append(((i++) == 0 ? "" : ", ")).append(column.name).append(((column.type) instanceof ReversedType ? " ASC" : " DESC"));

			sb.append(')');
		}
	}

	private static class FilterSerializer implements ClusteringIndexFilter.Serializer {
		public void serialize(ClusteringIndexFilter pfilter, DataOutputPlus out, int version) throws IOException {
			AbstractClusteringIndexFilter filter = ((AbstractClusteringIndexFilter) (pfilter));
			out.writeByte(filter.kind().ordinal());
			out.writeBoolean(filter.isReversed());
			filter.serializeInternal(out, version);
		}

		public ClusteringIndexFilter deserialize(DataInputPlus in, int version, CFMetaData metadata) throws IOException {
			ClusteringIndexFilter.Kind kind = values()[in.readUnsignedByte()];
			boolean reversed = in.readBoolean();
			return null;
		}

		public long serializedSize(ClusteringIndexFilter pfilter, int version) {
			AbstractClusteringIndexFilter filter = ((AbstractClusteringIndexFilter) (pfilter));
			return (1 + (TypeSizes.sizeof(filter.isReversed()))) + (filter.serializedSizeInternal(version));
		}
	}
}

