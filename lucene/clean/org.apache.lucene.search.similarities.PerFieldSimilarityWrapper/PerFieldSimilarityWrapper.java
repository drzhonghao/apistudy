import org.apache.lucene.search.similarities.*;



import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;

/**
 * Provides the ability to use a different {@link Similarity} for different fields.
 * <p>
 * Subclasses should implement {@link #get(String)} to return an appropriate
 * Similarity (for example, using field-specific parameter values) for the field.
 * 
 * @lucene.experimental
 */
public abstract class PerFieldSimilarityWrapper extends Similarity {
  
  /**
   * Sole constructor. (For invocation by subclass 
   * constructors, typically implicit.)
   */
  public PerFieldSimilarityWrapper() {}

  @Override
  public final long computeNorm(FieldInvertState state) {
    return get(state.getName()).computeNorm(state);
  }

  @Override
  public final SimWeight computeWeight(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
    PerFieldSimWeight weight = new PerFieldSimWeight();
    weight.delegate = get(collectionStats.field());
    weight.delegateWeight = weight.delegate.computeWeight(boost, collectionStats, termStats);
    return weight;
  }

  @Override
  public final SimScorer simScorer(SimWeight weight, LeafReaderContext context) throws IOException {
    PerFieldSimWeight perFieldWeight = (PerFieldSimWeight) weight;
    return perFieldWeight.delegate.simScorer(perFieldWeight.delegateWeight, context);
  }
  
  /** 
   * Returns a {@link Similarity} for scoring a field.
   */
  public abstract Similarity get(String name);
  
  static class PerFieldSimWeight extends SimWeight {
    Similarity delegate;
    SimWeight delegateWeight;
  }
}
