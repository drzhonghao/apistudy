import org.apache.accumulo.core.util.format.DefaultFormatter;
import org.apache.accumulo.core.util.format.*;


import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

/**
 * Formatter that will aggregate entries for various display purposes.
 */
public abstract class AggregatingFormatter extends DefaultFormatter {
  @Override
  public String next() {
    Iterator<Entry<Key,Value>> si = super.getScannerIterator();
    checkState(true);
    while (si.hasNext())
      aggregateStats(si.next());
    return getStats();
  }

  /**
   * Generate statistics from each {@link Entry}, called for each entry to be iterated over.
   *
   * @param next
   *          the next entry to aggregate
   */
  protected abstract void aggregateStats(Entry<Key,Value> next);

  /**
   * Finalize the aggregation and return the result. Called once at the end.
   *
   * @return the aggregation results, suitable for printing to the console
   */
  protected abstract String getStats();
}
