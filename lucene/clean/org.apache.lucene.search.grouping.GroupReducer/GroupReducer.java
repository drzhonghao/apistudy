import org.apache.lucene.search.grouping.*;


import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;

/**
 * Concrete implementations of this class define what to collect for individual
 * groups during the second-pass of a grouping search.
 *
 * Each group is assigned a Collector returned by {@link #newCollector()}, and
 * {@link LeafCollector#collect(int)} is called for each document that is in
 * a group
 *
 * @see SecondPassGroupingCollector
 *
 * @param <T> the type of the value used for grouping
 * @param <C> the type of {@link Collector} used to reduce each group
 */
public abstract class GroupReducer<T, C extends Collector> {

  private final Map<T, GroupCollector<C>> groups = new HashMap<>();

  /**
   * Define which groups should be reduced.
   *
   * Called by {@link SecondPassGroupingCollector}
   */
  public void setGroups(Collection<SearchGroup<T>> groups) {
    for (SearchGroup<T> group : groups) {
      this.groups.put(group.groupValue, new GroupCollector<>(newCollector()));
    }
  }

  /**
   * Whether or not this reducer requires collected documents to be scored
   */
  public abstract boolean needsScores();

  /**
   * Creates a new Collector for each group
   */
  protected abstract C newCollector();

  /**
   * Get the Collector for a given group
   */
  public final C getCollector(T value) {
    return groups.get(value).collector;
  }

  /**
   * Collect a given document into a given group
   * @throws IOException on error
   */
  public final void collect(T value, int doc) throws IOException {
    GroupCollector<C> collector = groups.get(value);
    collector.leafCollector.collect(doc);
  }

  /**
   * Set the Scorer on all group collectors
   */
  public final void setScorer(Scorer scorer) throws IOException {
    for (GroupCollector<C> collector : groups.values()) {
      collector.leafCollector.setScorer(scorer);
    }
  }

  /**
   * Called when the parent {@link SecondPassGroupingCollector} moves to a new segment
   */
  public final void setNextReader(LeafReaderContext ctx) throws IOException {
    for (GroupCollector<C> collector : groups.values()) {
      collector.leafCollector = collector.collector.getLeafCollector(ctx);
    }
  }

  private static final class GroupCollector<C extends Collector> {

    final C collector;
    LeafCollector leafCollector;

    private GroupCollector(C collector) {
      this.collector = collector;
    }
  }

}
