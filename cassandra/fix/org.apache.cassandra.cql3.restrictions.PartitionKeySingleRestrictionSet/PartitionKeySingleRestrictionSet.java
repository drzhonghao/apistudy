

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.restrictions.SingleRestriction;
import org.apache.cassandra.cql3.statements.Bound;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.index.SecondaryIndexManager;


final class PartitionKeySingleRestrictionSet {
	protected final ClusteringComparator comparator;

	public PartitionKeySingleRestrictionSet(ClusteringComparator comparator) {
		this.comparator = comparator;
	}

	private PartitionKeySingleRestrictionSet(PartitionKeySingleRestrictionSet restrictionSet, SingleRestriction restriction) {
		this.comparator = restrictionSet.comparator;
	}

	private List<ByteBuffer> toByteBuffers(SortedSet<? extends ClusteringPrefix> clusterings) {
		List<ByteBuffer> l = new ArrayList<>(clusterings.size());
		for (ClusteringPrefix clustering : clusterings)
			l.add(CFMetaData.serializePartitionKey(clustering));

		return l;
	}

	public List<ByteBuffer> values(QueryOptions options) {
		return null;
	}

	public List<ByteBuffer> bounds(Bound bound, QueryOptions options) {
		return null;
	}

	public boolean hasBound(Bound b) {
		return false;
	}

	public boolean isInclusive(Bound b) {
		return false;
	}

	public void addRowFilterTo(RowFilter filter, SecondaryIndexManager indexManager, QueryOptions options) {
	}

	public boolean needFiltering(CFMetaData cfm) {
		return false;
	}

	public boolean hasUnrestrictedPartitionKeyComponents(CFMetaData cfm) {
		return false;
	}

	public boolean hasSlice() {
		return false;
	}
}

