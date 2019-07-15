import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.*;



import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;

/**
 * A {@link Collector} implementation which wraps another
 * {@link Collector} and makes sure only documents with
 * scores &gt; 0 are collected.
 */
public class PositiveScoresOnlyCollector extends FilterCollector {

  public PositiveScoresOnlyCollector(Collector in) {
    super(in);
  }

  @Override
  public LeafCollector getLeafCollector(LeafReaderContext context)
      throws IOException {
    return new FilterLeafCollector(super.getLeafCollector(context)) {

      private Scorer scorer;

      @Override
      public void setScorer(Scorer scorer) throws IOException {
        this.scorer = new ScoreCachingWrappingScorer(scorer);
        in.setScorer(this.scorer);
      }

      @Override
      public void collect(int doc) throws IOException {
        if (scorer.score() > 0) {
          in.collect(doc);
        }
      }
      
    };
  }

}
