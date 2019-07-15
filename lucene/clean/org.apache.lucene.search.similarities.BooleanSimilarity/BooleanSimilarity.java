import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.*;


import java.io.IOException;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;

/**
 * Simple similarity that gives terms a score that is equal to their query
 * boost. This similarity is typically used with disabled norms since neither
 * document statistics nor index statistics are used for scoring. That said,
 * if norms are enabled, they will be computed the same way as
 * {@link SimilarityBase} and {@link BM25Similarity} with
 * {@link SimilarityBase#setDiscountOverlaps(boolean) discounted overlaps}
 * so that the {@link Similarity} can be changed after the index has been
 * created.
 */
public class BooleanSimilarity extends Similarity {

  private static final Similarity BM25_SIM = new BM25Similarity();

  /** Sole constructor */
  public BooleanSimilarity() {}

  @Override
  public long computeNorm(FieldInvertState state) {
    return BM25_SIM.computeNorm(state);
  }

  @Override
  public SimWeight computeWeight(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
    return new BooleanWeight(boost);
  }

  private static class BooleanWeight extends SimWeight {
    final float boost;

    BooleanWeight(float boost) {
      this.boost = boost;
    }
  }

  @Override
  public SimScorer simScorer(SimWeight weight, LeafReaderContext context) throws IOException {
    final float boost = ((BooleanWeight) weight).boost;

    return new SimScorer() {

      @Override
      public float score(int doc, float freq) throws IOException {
        return boost;
      }

      @Override
      public Explanation explain(int doc, Explanation freq) throws IOException {
        Explanation queryBoostExpl = Explanation.match(boost, "query boost");
        return Explanation.match(
            queryBoostExpl.getValue(),
            "score(" + getClass().getSimpleName() + ", doc=" + doc + "), computed from:",
            queryBoostExpl);
      }

      @Override
      public float computeSlopFactor(int distance) {
        return 1f;
      }

      @Override
      public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
        return 1f;
      }
    };
  }

}
