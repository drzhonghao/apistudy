import org.apache.cassandra.index.internal.composites.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.Operator;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.marshal.SetType;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.schema.IndexMetadata;

/**
 * Index on the collection element of the cell name of a collection.
 *
 * The row keys for this index are given by the collection element for
 * indexed columns.
 */
public class CollectionKeyIndex extends CollectionKeyIndexBase
{
    public CollectionKeyIndex(ColumnFamilyStore baseCfs, IndexMetadata indexDef)
    {
        super(baseCfs, indexDef);
    }

    public ByteBuffer getIndexedValue(ByteBuffer partitionKey,
                                      Clustering clustering,
                                      CellPath path,
                                      ByteBuffer cellValue)
    {
        return path.get(0);
    }

    public boolean isStale(Row data, ByteBuffer indexValue, int nowInSec)
    {
        Cell cell = data.getCell(indexedColumn, CellPath.create(indexValue));
        return cell == null || !cell.isLive(nowInSec);
    }

    public boolean supportsOperator(ColumnDefinition indexedColumn, Operator operator)
    {
        return operator == Operator.CONTAINS_KEY ||
               operator == Operator.CONTAINS && indexedColumn.type instanceof SetType;
    }
}
