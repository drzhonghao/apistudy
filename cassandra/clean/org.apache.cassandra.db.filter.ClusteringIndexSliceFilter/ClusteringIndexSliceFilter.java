import org.apache.cassandra.db.filter.*;


import java.io.IOException;
import java.util.List;
import java.nio.ByteBuffer;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.rows.*;
import org.apache.cassandra.db.partitions.CachedPartition;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.db.transform.Transformation;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;

/**
 * A filter over a single partition.
 */
public class ClusteringIndexSliceFilter extends AbstractClusteringIndexFilter
{
    static final InternalDeserializer deserializer = new SliceDeserializer();

    private final Slices slices;

    public ClusteringIndexSliceFilter(Slices slices, boolean reversed)
    {
        super(reversed);
        this.slices = slices;
    }

    public Slices requestedSlices()
    {
        return slices;
    }

    public boolean selectsAllPartition()
    {
        return slices.size() == 1 && !slices.hasLowerBound() && !slices.hasUpperBound();
    }

    public boolean selects(Clustering clustering)
    {
        return slices.selects(clustering);
    }

    public ClusteringIndexSliceFilter forPaging(ClusteringComparator comparator, Clustering lastReturned, boolean inclusive)
    {
        Slices newSlices = slices.forPaging(comparator, lastReturned, inclusive, reversed);
        return slices == newSlices
             ? this
             : new ClusteringIndexSliceFilter(newSlices, reversed);
    }

    public boolean isFullyCoveredBy(CachedPartition partition)
    {
        // Partition is guaranteed to cover the whole filter if it includes the filter start and finish bounds.

        // (note that since partition is the head of a partition, to have no lower bound is ok)
        if (!slices.hasUpperBound() || partition.isEmpty())
            return false;

        return partition.metadata().comparator.compare(slices.get(slices.size() - 1).end(), partition.lastRow().clustering()) <= 0;
    }

    public boolean isHeadFilter()
    {
        return !reversed && slices.size() == 1 && !slices.hasLowerBound();
    }

    // Given another iterator, only return the rows that match this filter
    public UnfilteredRowIterator filterNotIndexed(final ColumnFilter columnFilter, UnfilteredRowIterator iterator)
    {
        final Slices.InOrderTester tester = slices.inOrderTester(reversed);

        // Note that we don't filter markers because that's a bit trickier (we don't know in advance until when
        // the range extend) and it's harmless to leave them.
        class FilterNotIndexed extends Transformation
        {
            @Override
            public Row applyToRow(Row row)
            {
                return tester.includes(row.clustering()) ? row.filter(columnFilter, iterator.metadata()) : null;
            }

            @Override
            public Row applyToStatic(Row row)
            {
                return columnFilter.fetchedColumns().statics.isEmpty() ? Rows.EMPTY_STATIC_ROW : row.filter(columnFilter, iterator.metadata());
            }
        }
        return Transformation.apply(iterator, new FilterNotIndexed());
    }

    public Slices getSlices(CFMetaData metadata)
    {
        return slices;
    }

    public UnfilteredRowIterator getUnfilteredRowIterator(ColumnFilter columnFilter, Partition partition)
    {
        return partition.unfilteredIterator(columnFilter, slices, reversed);
    }

    public boolean shouldInclude(SSTableReader sstable)
    {
        List<ByteBuffer> minClusteringValues = sstable.getSSTableMetadata().minClusteringValues;
        List<ByteBuffer> maxClusteringValues = sstable.getSSTableMetadata().maxClusteringValues;

        if (minClusteringValues.isEmpty() || maxClusteringValues.isEmpty())
            return true;

        return slices.intersects(minClusteringValues, maxClusteringValues);
    }

    public String toString(CFMetaData metadata)
    {
        return String.format("slice(slices=%s, reversed=%b)", slices, reversed);
    }

    public String toCQLString(CFMetaData metadata)
    {
        StringBuilder sb = new StringBuilder();

        if (!selectsAllPartition())
            sb.append(slices.toCQLString(metadata));

        appendOrderByToCQLString(metadata, sb);

        return sb.toString();
    }

    public Kind kind()
    {
        return Kind.SLICE;
    }

    protected void serializeInternal(DataOutputPlus out, int version) throws IOException
    {
        Slices.serializer.serialize(slices, out, version);
    }

    protected long serializedSizeInternal(int version)
    {
        return Slices.serializer.serializedSize(slices, version);
    }

    private static class SliceDeserializer implements InternalDeserializer
    {
        public ClusteringIndexFilter deserialize(DataInputPlus in, int version, CFMetaData metadata, boolean reversed) throws IOException
        {
            Slices slices = Slices.serializer.deserialize(in, version, metadata);
            return new ClusteringIndexSliceFilter(slices, reversed);
        }
    }
}
