import org.apache.lucene.search.similarities.*;



import org.apache.lucene.search.Explanation;

/**
 * The probabilistic distribution used to model term occurrence
 * in information-based models.
 * @see IBSimilarity
 * @lucene.experimental
 */
public abstract class Distribution {
  
  /**
   * Sole constructor. (For invocation by subclass 
   * constructors, typically implicit.)
   */
  public Distribution() {}

  /** Computes the score. */
  public abstract float score(BasicStats stats, float tfn, float lambda);
  
  /** Explains the score. Returns the name of the model only, since
   * both {@code tfn} and {@code lambda} are explained elsewhere. */
  public Explanation explain(BasicStats stats, float tfn, float lambda) {
    return Explanation.match(
        score(stats, tfn, lambda), getClass().getSimpleName());
  }
  
  /**
   * Subclasses must override this method to return the name of the
   * distribution. 
   */
  @Override
  public abstract String toString();
}
