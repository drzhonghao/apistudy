import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.*;


import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;


/**
 * {@link Collector} delegator.
 *
 * @lucene.experimental
 */
public abstract class FilterCollector implements Collector {

  protected final Collector in;

  /** Sole constructor. */
  public FilterCollector(Collector in) {
    this.in = in;
  }

  @Override
  public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
    return in.getLeafCollector(context);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + in + ")";
  }

  @Override
  public boolean needsScores() {
    return in.needsScores();
  }
}
