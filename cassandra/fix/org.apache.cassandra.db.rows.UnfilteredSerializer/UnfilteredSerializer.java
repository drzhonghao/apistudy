

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import io.netty.util.concurrent.FastThreadLocal;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringBoundOrBoundary;
import org.apache.cassandra.db.ClusteringBoundary;
import org.apache.cassandra.db.Columns;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.rows.BTreeRow;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.ColumnData;
import org.apache.cassandra.db.rows.ComplexColumnData;
import org.apache.cassandra.db.rows.RangeTombstoneBoundMarker;
import org.apache.cassandra.db.rows.RangeTombstoneBoundaryMarker;
import org.apache.cassandra.db.rows.RangeTombstoneMarker;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.SerializationHelper;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.io.util.FileDataInput;
import org.apache.cassandra.utils.SearchIterator;
import org.apache.cassandra.utils.WrappedException;
import org.apache.cassandra.utils.btree.BTreeSearchIterator;

import static org.apache.cassandra.db.rows.Row.Deletion.LIVE;
import static org.apache.cassandra.db.rows.Unfiltered.Kind.RANGE_TOMBSTONE_MARKER;
import static org.apache.cassandra.db.rows.Unfiltered.Kind.ROW;


public class UnfilteredSerializer {
	public static final UnfilteredSerializer serializer = new UnfilteredSerializer();

	private static final int END_OF_PARTITION = 1;

	private static final int IS_MARKER = 2;

	private static final int HAS_TIMESTAMP = 4;

	private static final int HAS_TTL = 8;

	private static final int HAS_DELETION = 16;

	private static final int HAS_ALL_COLUMNS = 32;

	private static final int HAS_COMPLEX_DELETION = 64;

	private static final int EXTENSION_FLAG = 128;

	private static final int IS_STATIC = 1;

	@Deprecated
	private static final int HAS_SHADOWABLE_DELETION = 2;

	public void serialize(Unfiltered unfiltered, SerializationHeader header, DataOutputPlus out, int version) throws IOException {
		assert !(header.isForSSTable());
		serialize(unfiltered, header, out, 0, version);
	}

	public void serialize(Unfiltered unfiltered, SerializationHeader header, DataOutputPlus out, long previousUnfilteredSize, int version) throws IOException {
		if ((unfiltered.kind()) == (RANGE_TOMBSTONE_MARKER)) {
			serialize(((RangeTombstoneMarker) (unfiltered)), header, out, previousUnfilteredSize, version);
		}else {
			serialize(((Row) (unfiltered)), header, out, previousUnfilteredSize, version);
		}
	}

	public void serializeStaticRow(Row row, SerializationHeader header, DataOutputPlus out, int version) throws IOException {
		assert row.isStatic();
		serialize(row, header, out, 0, version);
	}

	private void serialize(Row row, SerializationHeader header, DataOutputPlus out, long previousUnfilteredSize, int version) throws IOException {
		int flags = 0;
		int extendedFlags = 0;
		boolean isStatic = row.isStatic();
		Columns headerColumns = header.columns(isStatic);
		LivenessInfo pkLiveness = row.primaryKeyLivenessInfo();
		Row.Deletion deletion = row.deletion();
		boolean hasComplexDeletion = row.hasComplexDeletion();
		boolean hasAllColumns = (row.size()) == (headerColumns.size());
		boolean hasExtendedFlags = UnfilteredSerializer.hasExtendedFlags(row);
		if (isStatic)
			extendedFlags |= UnfilteredSerializer.IS_STATIC;

		if (!(pkLiveness.isEmpty()))
			flags |= UnfilteredSerializer.HAS_TIMESTAMP;

		if (pkLiveness.isExpiring())
			flags |= UnfilteredSerializer.HAS_TTL;

		if (!(deletion.isLive())) {
			flags |= UnfilteredSerializer.HAS_DELETION;
			if (deletion.isShadowable())
				extendedFlags |= UnfilteredSerializer.HAS_SHADOWABLE_DELETION;

		}
		if (hasComplexDeletion)
			flags |= UnfilteredSerializer.HAS_COMPLEX_DELETION;

		if (hasAllColumns)
			flags |= UnfilteredSerializer.HAS_ALL_COLUMNS;

		if (hasExtendedFlags)
			flags |= UnfilteredSerializer.EXTENSION_FLAG;

		out.writeByte(((byte) (flags)));
		if (hasExtendedFlags)
			out.writeByte(((byte) (extendedFlags)));

		if (!isStatic)
			Clustering.serializer.serialize(row.clustering(), out, version, header.clusteringTypes());

		if (header.isForSSTable()) {
			try (DataOutputBuffer dob = DataOutputBuffer.scratchBuffer.get()) {
				serializeRowBody(row, flags, header, dob);
				out.writeUnsignedVInt(((dob.position()) + (TypeSizes.sizeofUnsignedVInt(previousUnfilteredSize))));
				out.writeUnsignedVInt(previousUnfilteredSize);
				out.write(dob.getData(), 0, dob.getLength());
			}
		}else {
			serializeRowBody(row, flags, header, out);
		}
	}

	@net.nicoulaj.compilecommand.annotations.Inline
	private void serializeRowBody(Row row, int flags, SerializationHeader header, DataOutputPlus out) throws IOException {
		boolean isStatic = row.isStatic();
		Columns headerColumns = header.columns(isStatic);
		LivenessInfo pkLiveness = row.primaryKeyLivenessInfo();
		Row.Deletion deletion = row.deletion();
		if ((flags & (UnfilteredSerializer.HAS_TIMESTAMP)) != 0)
			header.writeTimestamp(pkLiveness.timestamp(), out);

		if ((flags & (UnfilteredSerializer.HAS_TTL)) != 0) {
			header.writeTTL(pkLiveness.ttl(), out);
			header.writeLocalDeletionTime(pkLiveness.localExpirationTime(), out);
		}
		if ((flags & (UnfilteredSerializer.HAS_DELETION)) != 0)
			header.writeDeletionTime(deletion.time(), out);

		if ((flags & (UnfilteredSerializer.HAS_ALL_COLUMNS)) == 0)
			Columns.serializer.serializeSubset(Collections2.transform(row, ColumnData::column), headerColumns, out);

		SearchIterator<ColumnDefinition, ColumnDefinition> si = headerColumns.iterator();
		try {
			row.apply(( cd) -> {
				ColumnDefinition column = si.next(cd.column());
			}, false);
		} catch (WrappedException e) {
			if ((e.getCause()) instanceof IOException)
				throw ((IOException) (e.getCause()));

			throw e;
		}
	}

	private void writeComplexColumn(ComplexColumnData data, ColumnDefinition column, boolean hasComplexDeletion, LivenessInfo rowLiveness, SerializationHeader header, DataOutputPlus out) throws IOException {
		if (hasComplexDeletion)
			header.writeDeletionTime(data.complexDeletion(), out);

		out.writeUnsignedVInt(data.cellsCount());
		for (Cell cell : data) {
		}
	}

	private void serialize(RangeTombstoneMarker marker, SerializationHeader header, DataOutputPlus out, long previousUnfilteredSize, int version) throws IOException {
		out.writeByte(((byte) (UnfilteredSerializer.IS_MARKER)));
		ClusteringBoundOrBoundary.serializer.serialize(marker.clustering(), out, version, header.clusteringTypes());
		if (header.isForSSTable()) {
			out.writeUnsignedVInt(serializedMarkerBodySize(marker, header, previousUnfilteredSize, version));
			out.writeUnsignedVInt(previousUnfilteredSize);
		}
		if (marker.isBoundary()) {
			RangeTombstoneBoundaryMarker bm = ((RangeTombstoneBoundaryMarker) (marker));
			header.writeDeletionTime(bm.endDeletionTime(), out);
			header.writeDeletionTime(bm.startDeletionTime(), out);
		}else {
			header.writeDeletionTime(((RangeTombstoneBoundMarker) (marker)).deletionTime(), out);
		}
	}

	public long serializedSize(Unfiltered unfiltered, SerializationHeader header, int version) {
		assert !(header.isForSSTable());
		return serializedSize(unfiltered, header, 0, version);
	}

	public long serializedSize(Unfiltered unfiltered, SerializationHeader header, long previousUnfilteredSize, int version) {
		return (unfiltered.kind()) == (RANGE_TOMBSTONE_MARKER) ? serializedSize(((RangeTombstoneMarker) (unfiltered)), header, previousUnfilteredSize, version) : serializedSize(((Row) (unfiltered)), header, previousUnfilteredSize, version);
	}

	private long serializedSize(Row row, SerializationHeader header, long previousUnfilteredSize, int version) {
		long size = 1;
		if (UnfilteredSerializer.hasExtendedFlags(row))
			size += 1;

		if (!(row.isStatic()))
			size += Clustering.serializer.serializedSize(row.clustering(), version, header.clusteringTypes());

		return size + (serializedRowBodySize(row, header, previousUnfilteredSize, version));
	}

	private long serializedRowBodySize(Row row, SerializationHeader header, long previousUnfilteredSize, int version) {
		long size = 0;
		if (header.isForSSTable())
			size += TypeSizes.sizeofUnsignedVInt(previousUnfilteredSize);

		boolean isStatic = row.isStatic();
		Columns headerColumns = header.columns(isStatic);
		LivenessInfo pkLiveness = row.primaryKeyLivenessInfo();
		Row.Deletion deletion = row.deletion();
		boolean hasComplexDeletion = row.hasComplexDeletion();
		boolean hasAllColumns = (row.size()) == (headerColumns.size());
		if (!(pkLiveness.isEmpty()))
			size += header.timestampSerializedSize(pkLiveness.timestamp());

		if (pkLiveness.isExpiring()) {
			size += header.ttlSerializedSize(pkLiveness.ttl());
			size += header.localDeletionTimeSerializedSize(pkLiveness.localExpirationTime());
		}
		if (!(deletion.isLive()))
			size += header.deletionTimeSerializedSize(deletion.time());

		if (!hasAllColumns)
			size += Columns.serializer.serializedSubsetSize(Collections2.transform(row, ColumnData::column), header.columns(isStatic));

		SearchIterator<ColumnDefinition, ColumnDefinition> si = headerColumns.iterator();
		for (ColumnData data : row) {
			ColumnDefinition column = si.next(data.column());
			assert column != null;
		}
		return size;
	}

	private long sizeOfComplexColumn(ComplexColumnData data, ColumnDefinition column, boolean hasComplexDeletion, LivenessInfo rowLiveness, SerializationHeader header) {
		long size = 0;
		if (hasComplexDeletion)
			size += header.deletionTimeSerializedSize(data.complexDeletion());

		size += TypeSizes.sizeofUnsignedVInt(data.cellsCount());
		for (Cell cell : data) {
		}
		return size;
	}

	private long serializedSize(RangeTombstoneMarker marker, SerializationHeader header, long previousUnfilteredSize, int version) {
		assert !(header.isForSSTable());
		return (1 + (ClusteringBoundOrBoundary.serializer.serializedSize(marker.clustering(), version, header.clusteringTypes()))) + (serializedMarkerBodySize(marker, header, previousUnfilteredSize, version));
	}

	private long serializedMarkerBodySize(RangeTombstoneMarker marker, SerializationHeader header, long previousUnfilteredSize, int version) {
		long size = 0;
		if (header.isForSSTable())
			size += TypeSizes.sizeofUnsignedVInt(previousUnfilteredSize);

		if (marker.isBoundary()) {
			RangeTombstoneBoundaryMarker bm = ((RangeTombstoneBoundaryMarker) (marker));
			size += header.deletionTimeSerializedSize(bm.endDeletionTime());
			size += header.deletionTimeSerializedSize(bm.startDeletionTime());
		}else {
			size += header.deletionTimeSerializedSize(((RangeTombstoneBoundMarker) (marker)).deletionTime());
		}
		return size;
	}

	public void writeEndOfPartition(DataOutputPlus out) throws IOException {
		out.writeByte(((byte) (1)));
	}

	public long serializedSizeEndOfPartition() {
		return 1;
	}

	public Unfiltered deserialize(DataInputPlus in, SerializationHeader header, SerializationHelper helper, Row.Builder builder) throws IOException {
		while (true) {
			Unfiltered unfiltered = deserializeOne(in, header, helper, builder);
			if (unfiltered == null)
				return null;

			if (!(unfiltered.isEmpty()))
				return unfiltered;

		} 
	}

	private Unfiltered deserializeOne(DataInputPlus in, SerializationHeader header, SerializationHelper helper, Row.Builder builder) throws IOException {
		assert builder.isSorted();
		int flags = in.readUnsignedByte();
		if (UnfilteredSerializer.isEndOfPartition(flags))
			return null;

		int extendedFlags = UnfilteredSerializer.readExtendedFlags(in, flags);
		if ((UnfilteredSerializer.kind(flags)) == (RANGE_TOMBSTONE_MARKER)) {
			ClusteringBoundOrBoundary bound = ClusteringBoundOrBoundary.serializer.deserialize(in, helper.version, header.clusteringTypes());
			return deserializeMarkerBody(in, header, bound);
		}else {
			if (UnfilteredSerializer.isStatic(extendedFlags))
				throw new IOException(("Corrupt flags value for unfiltered partition (isStatic flag set): " + flags));

			builder.newRow(Clustering.serializer.deserialize(in, helper.version, header.clusteringTypes()));
			return deserializeRowBody(in, header, helper, flags, extendedFlags, builder);
		}
	}

	public Unfiltered deserializeTombstonesOnly(FileDataInput in, SerializationHeader header, SerializationHelper helper) throws IOException {
		while (true) {
			int flags = in.readUnsignedByte();
			if (UnfilteredSerializer.isEndOfPartition(flags))
				return null;

			int extendedFlags = UnfilteredSerializer.readExtendedFlags(in, flags);
			if ((UnfilteredSerializer.kind(flags)) == (RANGE_TOMBSTONE_MARKER)) {
				ClusteringBoundOrBoundary bound = ClusteringBoundOrBoundary.serializer.deserialize(in, helper.version, header.clusteringTypes());
				return deserializeMarkerBody(in, header, bound);
			}else {
				assert !(UnfilteredSerializer.isStatic(extendedFlags));
				if ((flags & (UnfilteredSerializer.HAS_DELETION)) != 0) {
					assert header.isForSSTable();
					boolean hasTimestamp = (flags & (UnfilteredSerializer.HAS_TIMESTAMP)) != 0;
					boolean hasTTL = (flags & (UnfilteredSerializer.HAS_TTL)) != 0;
					boolean deletionIsShadowable = (extendedFlags & (UnfilteredSerializer.HAS_SHADOWABLE_DELETION)) != 0;
					Clustering clustering = Clustering.serializer.deserialize(in, helper.version, header.clusteringTypes());
					long nextPosition = (in.readUnsignedVInt()) + (in.getFilePointer());
					in.readUnsignedVInt();
					if (hasTimestamp) {
						header.readTimestamp(in);
						if (hasTTL) {
							header.readTTL(in);
							header.readLocalDeletionTime(in);
						}
					}
					Row.Deletion deletion = new Row.Deletion(header.readDeletionTime(in), deletionIsShadowable);
					in.seek(nextPosition);
					return BTreeRow.emptyDeletedRow(clustering, deletion);
				}else {
					Clustering.serializer.skip(in, helper.version, header.clusteringTypes());
					skipRowBody(in);
				}
			}
		} 
	}

	public Row deserializeStaticRow(DataInputPlus in, SerializationHeader header, SerializationHelper helper) throws IOException {
		int flags = in.readUnsignedByte();
		assert ((!(UnfilteredSerializer.isEndOfPartition(flags))) && ((UnfilteredSerializer.kind(flags)) == (ROW))) && (UnfilteredSerializer.isExtended(flags)) : flags;
		int extendedFlags = in.readUnsignedByte();
		Row.Builder builder = BTreeRow.sortedBuilder();
		builder.newRow(Clustering.STATIC_CLUSTERING);
		return deserializeRowBody(in, header, helper, flags, extendedFlags, builder);
	}

	public RangeTombstoneMarker deserializeMarkerBody(DataInputPlus in, SerializationHeader header, ClusteringBoundOrBoundary bound) throws IOException {
		if (header.isForSSTable()) {
			in.readUnsignedVInt();
			in.readUnsignedVInt();
		}
		if (bound.isBoundary())
			return new RangeTombstoneBoundaryMarker(((ClusteringBoundary) (bound)), header.readDeletionTime(in), header.readDeletionTime(in));
		else
			return new RangeTombstoneBoundMarker(((ClusteringBound) (bound)), header.readDeletionTime(in));

	}

	public Row deserializeRowBody(DataInputPlus in, SerializationHeader header, SerializationHelper helper, int flags, int extendedFlags, Row.Builder builder) throws IOException {
		try {
			boolean isStatic = UnfilteredSerializer.isStatic(extendedFlags);
			boolean hasTimestamp = (flags & (UnfilteredSerializer.HAS_TIMESTAMP)) != 0;
			boolean hasTTL = (flags & (UnfilteredSerializer.HAS_TTL)) != 0;
			boolean hasDeletion = (flags & (UnfilteredSerializer.HAS_DELETION)) != 0;
			boolean deletionIsShadowable = (extendedFlags & (UnfilteredSerializer.HAS_SHADOWABLE_DELETION)) != 0;
			boolean hasComplexDeletion = (flags & (UnfilteredSerializer.HAS_COMPLEX_DELETION)) != 0;
			boolean hasAllColumns = (flags & (UnfilteredSerializer.HAS_ALL_COLUMNS)) != 0;
			Columns headerColumns = header.columns(isStatic);
			if (header.isForSSTable()) {
				in.readUnsignedVInt();
				in.readUnsignedVInt();
			}
			LivenessInfo rowLiveness = LivenessInfo.EMPTY;
			if (hasTimestamp) {
				long timestamp = header.readTimestamp(in);
				int ttl = (hasTTL) ? header.readTTL(in) : LivenessInfo.NO_TTL;
				int localDeletionTime = (hasTTL) ? header.readLocalDeletionTime(in) : LivenessInfo.NO_EXPIRATION_TIME;
				rowLiveness = LivenessInfo.withExpirationTime(timestamp, ttl, localDeletionTime);
			}
			builder.addPrimaryKeyLivenessInfo(rowLiveness);
			builder.addRowDeletion((hasDeletion ? new Row.Deletion(header.readDeletionTime(in), deletionIsShadowable) : LIVE));
			Columns columns = (hasAllColumns) ? headerColumns : Columns.serializer.deserializeSubset(headerColumns, in);
			final LivenessInfo livenessInfo = rowLiveness;
			try {
				columns.apply(( column) -> {
					try {
						if (column.isSimple())
							readSimpleColumn(column, in, header, helper, builder, livenessInfo);
						else
							readComplexColumn(column, in, header, helper, hasComplexDeletion, builder, livenessInfo);

					} catch (IOException e) {
						throw new WrappedException(e);
					}
				}, false);
			} catch (WrappedException e) {
				if ((e.getCause()) instanceof IOException)
					throw ((IOException) (e.getCause()));

				throw e;
			}
			return builder.build();
		} catch (RuntimeException | AssertionError e) {
			throw new IOException(("Error building row with data deserialized from " + in), e);
		}
	}

	private void readSimpleColumn(ColumnDefinition column, DataInputPlus in, SerializationHeader header, SerializationHelper helper, Row.Builder builder, LivenessInfo rowLiveness) throws IOException {
		if (helper.includes(column)) {
		}else {
		}
	}

	private void readComplexColumn(ColumnDefinition column, DataInputPlus in, SerializationHeader header, SerializationHelper helper, boolean hasComplexDeletion, Row.Builder builder, LivenessInfo rowLiveness) throws IOException {
		if (helper.includes(column)) {
			helper.startOfComplexColumn(column);
			if (hasComplexDeletion) {
				DeletionTime complexDeletion = header.readDeletionTime(in);
				if (!(helper.isDroppedComplexDeletion(complexDeletion)))
					builder.addComplexDeletion(column, complexDeletion);

			}
			int count = ((int) (in.readUnsignedVInt()));
			while ((--count) >= 0) {
			} 
			helper.endOfComplexColumn();
		}else {
			skipComplexColumn(in, column, header, hasComplexDeletion);
		}
	}

	public void skipRowBody(DataInputPlus in) throws IOException {
		int rowSize = ((int) (in.readUnsignedVInt()));
		in.skipBytesFully(rowSize);
	}

	public void skipStaticRow(DataInputPlus in, SerializationHeader header, SerializationHelper helper) throws IOException {
		int flags = in.readUnsignedByte();
		assert ((!(UnfilteredSerializer.isEndOfPartition(flags))) && ((UnfilteredSerializer.kind(flags)) == (ROW))) && (UnfilteredSerializer.isExtended(flags)) : "Flags is " + flags;
		int extendedFlags = in.readUnsignedByte();
		assert UnfilteredSerializer.isStatic(extendedFlags);
		skipRowBody(in);
	}

	public void skipMarkerBody(DataInputPlus in) throws IOException {
		int markerSize = ((int) (in.readUnsignedVInt()));
		in.skipBytesFully(markerSize);
	}

	private void skipComplexColumn(DataInputPlus in, ColumnDefinition column, SerializationHeader header, boolean hasComplexDeletion) throws IOException {
		if (hasComplexDeletion)
			header.skipDeletionTime(in);

		int count = ((int) (in.readUnsignedVInt()));
		while ((--count) >= 0) {
		} 
	}

	public static boolean isEndOfPartition(int flags) {
		return (flags & (UnfilteredSerializer.END_OF_PARTITION)) != 0;
	}

	public static Unfiltered.Kind kind(int flags) {
		return (flags & (UnfilteredSerializer.IS_MARKER)) != 0 ? RANGE_TOMBSTONE_MARKER : ROW;
	}

	public static boolean isStatic(int extendedFlags) {
		return (extendedFlags & (UnfilteredSerializer.IS_STATIC)) != 0;
	}

	private static boolean isExtended(int flags) {
		return (flags & (UnfilteredSerializer.EXTENSION_FLAG)) != 0;
	}

	public static int readExtendedFlags(DataInputPlus in, int flags) throws IOException {
		return UnfilteredSerializer.isExtended(flags) ? in.readUnsignedByte() : 0;
	}

	public static boolean hasExtendedFlags(Row row) {
		return (row.isStatic()) || (row.deletion().isShadowable());
	}
}

