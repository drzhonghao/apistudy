import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.*;


import org.apache.cassandra.db.context.CounterContext;

public abstract class CounterCells
{
    private CounterCells() {}

    private static final CounterContext contextManager = CounterContext.instance();

    public static boolean hasLegacyShards(Cell cell)
    {
        return contextManager.hasLegacyShards(cell.value());
    }
}
