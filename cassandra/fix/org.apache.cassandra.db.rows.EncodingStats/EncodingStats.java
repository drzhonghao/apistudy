

import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import org.apache.cassandra.db.DeletionInfo;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.partitions.PartitionStatisticsCollector;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;


public class EncodingStats {
	private static final long TIMESTAMP_EPOCH;

	private static final int DELETION_TIME_EPOCH;

	private static final int TTL_EPOCH = 0;

	static {
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT-0"), Locale.US);
		c.set(Calendar.YEAR, 2015);
		c.set(Calendar.MONTH, Calendar.SEPTEMBER);
		c.set(Calendar.DAY_OF_MONTH, 22);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		TIMESTAMP_EPOCH = (c.getTimeInMillis()) * 1000;
		DELETION_TIME_EPOCH = ((int) ((c.getTimeInMillis()) / 1000));
	}

	public static final EncodingStats NO_STATS = new EncodingStats(EncodingStats.TIMESTAMP_EPOCH, EncodingStats.DELETION_TIME_EPOCH, EncodingStats.TTL_EPOCH);

	public static final EncodingStats.Serializer serializer = new EncodingStats.Serializer();

	public final long minTimestamp;

	public final int minLocalDeletionTime;

	public final int minTTL;

	public EncodingStats(long minTimestamp, int minLocalDeletionTime, int minTTL) {
		this.minTimestamp = (minTimestamp == (LivenessInfo.NO_TIMESTAMP)) ? EncodingStats.TIMESTAMP_EPOCH : minTimestamp;
		this.minLocalDeletionTime = (minLocalDeletionTime == (LivenessInfo.NO_EXPIRATION_TIME)) ? EncodingStats.DELETION_TIME_EPOCH : minLocalDeletionTime;
		this.minTTL = minTTL;
	}

	public EncodingStats mergeWith(EncodingStats that) {
		long minTimestamp = ((this.minTimestamp) == (EncodingStats.TIMESTAMP_EPOCH)) ? that.minTimestamp : (that.minTimestamp) == (EncodingStats.TIMESTAMP_EPOCH) ? this.minTimestamp : Math.min(this.minTimestamp, that.minTimestamp);
		int minDelTime = ((this.minLocalDeletionTime) == (EncodingStats.DELETION_TIME_EPOCH)) ? that.minLocalDeletionTime : (that.minLocalDeletionTime) == (EncodingStats.DELETION_TIME_EPOCH) ? this.minLocalDeletionTime : Math.min(this.minLocalDeletionTime, that.minLocalDeletionTime);
		int minTTL = ((this.minTTL) == (EncodingStats.TTL_EPOCH)) ? that.minTTL : (that.minTTL) == (EncodingStats.TTL_EPOCH) ? this.minTTL : Math.min(this.minTTL, that.minTTL);
		return new EncodingStats(minTimestamp, minDelTime, minTTL);
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if ((o == null) || ((getClass()) != (o.getClass())))
			return false;

		EncodingStats that = ((EncodingStats) (o));
		return (((this.minLocalDeletionTime) == (that.minLocalDeletionTime)) && ((this.minTTL) == (that.minTTL))) && ((this.minTimestamp) == (that.minTimestamp));
	}

	@Override
	public int hashCode() {
		return Objects.hash(minTimestamp, minLocalDeletionTime, minTTL);
	}

	@Override
	public String toString() {
		return String.format("EncodingStats(ts=%d, ldt=%d, ttl=%d)", minTimestamp, minLocalDeletionTime, minTTL);
	}

	public static class Collector implements PartitionStatisticsCollector {
		private boolean isTimestampSet;

		private long minTimestamp = Long.MAX_VALUE;

		private boolean isDelTimeSet;

		private int minDeletionTime = Integer.MAX_VALUE;

		private boolean isTTLSet;

		private int minTTL = Integer.MAX_VALUE;

		public void update(LivenessInfo info) {
			if (info.isEmpty())
				return;

			updateTimestamp(info.timestamp());
			if (info.isExpiring()) {
				updateTTL(info.ttl());
				updateLocalDeletionTime(info.localExpirationTime());
			}
		}

		public void update(Cell cell) {
			updateTimestamp(cell.timestamp());
			if (cell.isExpiring()) {
				updateTTL(cell.ttl());
				updateLocalDeletionTime(cell.localDeletionTime());
			}else
				if (cell.isTombstone()) {
					updateLocalDeletionTime(cell.localDeletionTime());
				}

		}

		public void update(DeletionTime deletionTime) {
			if (deletionTime.isLive())
				return;

			updateTimestamp(deletionTime.markedForDeleteAt());
			updateLocalDeletionTime(deletionTime.localDeletionTime());
		}

		public void updateTimestamp(long timestamp) {
			isTimestampSet = true;
			minTimestamp = Math.min(minTimestamp, timestamp);
		}

		public void updateLocalDeletionTime(int deletionTime) {
			isDelTimeSet = true;
			minDeletionTime = Math.min(minDeletionTime, deletionTime);
		}

		public void updateTTL(int ttl) {
			isTTLSet = true;
			minTTL = Math.min(minTTL, ttl);
		}

		public void updateColumnSetPerRow(long columnSetInRow) {
		}

		public void updateHasLegacyCounterShards(boolean hasLegacyCounterShards) {
		}

		public EncodingStats get() {
			return new EncodingStats((isTimestampSet ? minTimestamp : EncodingStats.TIMESTAMP_EPOCH), (isDelTimeSet ? minDeletionTime : EncodingStats.DELETION_TIME_EPOCH), (isTTLSet ? minTTL : EncodingStats.TTL_EPOCH));
		}

		public static EncodingStats collect(Row staticRow, Iterator<Row> rows, DeletionInfo deletionInfo) {
			EncodingStats.Collector collector = new EncodingStats.Collector();
			if (!(staticRow.isEmpty()))
				Rows.collectStats(staticRow, collector);

			while (rows.hasNext())
				Rows.collectStats(rows.next(), collector);

			return collector.get();
		}
	}

	public static class Serializer {
		public void serialize(EncodingStats stats, DataOutputPlus out) throws IOException {
			out.writeUnsignedVInt(((stats.minTimestamp) - (EncodingStats.TIMESTAMP_EPOCH)));
			out.writeUnsignedVInt(((stats.minLocalDeletionTime) - (EncodingStats.DELETION_TIME_EPOCH)));
			out.writeUnsignedVInt(((stats.minTTL) - (EncodingStats.TTL_EPOCH)));
		}

		public int serializedSize(EncodingStats stats) {
			return ((TypeSizes.sizeofUnsignedVInt(((stats.minTimestamp) - (EncodingStats.TIMESTAMP_EPOCH)))) + (TypeSizes.sizeofUnsignedVInt(((stats.minLocalDeletionTime) - (EncodingStats.DELETION_TIME_EPOCH))))) + (TypeSizes.sizeofUnsignedVInt(((stats.minTTL) - (EncodingStats.TTL_EPOCH))));
		}

		public EncodingStats deserialize(DataInputPlus in) throws IOException {
			long minTimestamp = (in.readUnsignedVInt()) + (EncodingStats.TIMESTAMP_EPOCH);
			int minLocalDeletionTime = ((int) (in.readUnsignedVInt())) + (EncodingStats.DELETION_TIME_EPOCH);
			int minTTL = ((int) (in.readUnsignedVInt())) + (EncodingStats.TTL_EPOCH);
			return new EncodingStats(minTimestamp, minLocalDeletionTime, minTTL);
		}
	}
}

