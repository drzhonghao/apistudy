import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.*;



import java.io.IOException;

/**
 * {@link LeafCollector} delegator.
 *
 * @lucene.experimental
 */
public abstract class FilterLeafCollector implements LeafCollector {

  protected final LeafCollector in;

  /** Sole constructor. */
  public FilterLeafCollector(LeafCollector in) {
    this.in = in;
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    in.setScorer(scorer);
  }

  @Override
  public void collect(int doc) throws IOException {
    in.collect(doc);
  }

  @Override
  public String toString() {
    String name = getClass().getSimpleName();
    if (name.length() == 0) {
      // an anonoymous subclass will have empty name?
      name = "FilterLeafCollector";
    }
    return name + "(" + in + ")";
  }

}
