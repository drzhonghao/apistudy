

import java.nio.ByteBuffer;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.aggregation.GroupingState;


public abstract class GroupMaker {
	public static final GroupMaker GROUP_EVERYTHING = new GroupMaker() {
		public boolean isNewGroup(DecoratedKey partitionKey, Clustering clustering) {
			return false;
		}

		public boolean returnAtLeastOneRow() {
			return true;
		}
	};

	public static GroupMaker newInstance(ClusteringComparator comparator, int clusteringPrefixSize, GroupingState state) {
		return new GroupMaker.PkPrefixGroupMaker(comparator, clusteringPrefixSize, state);
	}

	public static GroupMaker newInstance(ClusteringComparator comparator, int clusteringPrefixSize) {
		return new GroupMaker.PkPrefixGroupMaker(comparator, clusteringPrefixSize);
	}

	public abstract boolean isNewGroup(DecoratedKey partitionKey, Clustering clustering);

	public boolean returnAtLeastOneRow() {
		return false;
	}

	private static final class PkPrefixGroupMaker extends GroupMaker {
		private final int clusteringPrefixSize;

		private final ClusteringComparator comparator;

		private ByteBuffer lastPartitionKey;

		private Clustering lastClustering;

		public PkPrefixGroupMaker(ClusteringComparator comparator, int clusteringPrefixSize, GroupingState state) {
			this(comparator, clusteringPrefixSize);
			this.lastPartitionKey = state.partitionKey();
		}

		public PkPrefixGroupMaker(ClusteringComparator comparator, int clusteringPrefixSize) {
			this.comparator = comparator;
			this.clusteringPrefixSize = clusteringPrefixSize;
		}

		@Override
		public boolean isNewGroup(DecoratedKey partitionKey, Clustering clustering) {
			boolean isNew = false;
			if (!(partitionKey.getKey().equals(lastPartitionKey))) {
				lastPartitionKey = partitionKey.getKey();
				isNew = true;
				if ((Clustering.STATIC_CLUSTERING) == clustering) {
					lastClustering = null;
					return true;
				}
			}else
				if (((lastClustering) != null) && ((comparator.compare(lastClustering, clustering, clusteringPrefixSize)) != 0)) {
					isNew = true;
				}

			lastClustering = clustering;
			return isNew;
		}
	}
}

