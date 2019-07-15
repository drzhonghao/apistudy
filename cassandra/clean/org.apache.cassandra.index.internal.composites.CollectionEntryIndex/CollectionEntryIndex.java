import org.apache.cassandra.index.internal.composites.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.CollectionType;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.schema.IndexMetadata;

/**
 * Index on the element and value of cells participating in a collection.
 *
 * The row keys for this index are a composite of the collection element
 * and value of indexed columns.
 */
public class CollectionEntryIndex extends CollectionKeyIndexBase
{
    public CollectionEntryIndex(ColumnFamilyStore baseCfs,
                                IndexMetadata indexDef)
    {
        super(baseCfs, indexDef);
    }

    public ByteBuffer getIndexedValue(ByteBuffer partitionKey,
                                      Clustering clustering,
                                      CellPath path, ByteBuffer cellValue)
    {
        return CompositeType.build(path.get(0), cellValue);
    }

    public boolean isStale(Row data, ByteBuffer indexValue, int nowInSec)
    {
        ByteBuffer[] components = ((CompositeType)functions.getIndexedValueType(indexedColumn)).split(indexValue);
        ByteBuffer mapKey = components[0];
        ByteBuffer mapValue = components[1];

        ColumnDefinition columnDef = indexedColumn;
        Cell cell = data.getCell(columnDef, CellPath.create(mapKey));
        if (cell == null || !cell.isLive(nowInSec))
            return true;

        AbstractType<?> valueComparator = ((CollectionType)columnDef.type).valueComparator();
        return valueComparator.compare(mapValue, cell.value()) != 0;
    }
}
