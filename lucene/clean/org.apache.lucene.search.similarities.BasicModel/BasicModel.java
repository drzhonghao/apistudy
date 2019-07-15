import org.apache.lucene.search.similarities.*;



import org.apache.lucene.search.Explanation;

/**
 * This class acts as the base class for the specific <em>basic model</em>
 * implementations in the DFR framework. Basic models compute the
 * <em>informative content Inf<sub>1</sub> = -log<sub>2</sub>Prob<sub>1</sub>
 * </em>.
 * 
 * @see DFRSimilarity
 * @lucene.experimental
 */
public abstract class BasicModel {
  
  /**
   * Sole constructor. (For invocation by subclass 
   * constructors, typically implicit.)
   */
  public BasicModel() {}

  /** Returns the informative content score. */
  public abstract float score(BasicStats stats, float tfn);
  
  /**
   * Returns an explanation for the score.
   * <p>Most basic models use the number of documents and the total term
   * frequency to compute Inf<sub>1</sub>. This method provides a generic
   * explanation for such models. Subclasses that use other statistics must
   * override this method.</p>
   */
  public Explanation explain(BasicStats stats, float tfn) {
    return Explanation.match(
        score(stats, tfn),
        getClass().getSimpleName() + ", computed from: ",
        Explanation.match(stats.getNumberOfDocuments(), "numberOfDocuments"),
        Explanation.match(stats.getTotalTermFreq(), "totalTermFreq"));
  }
  
  /**
   * Subclasses must override this method to return the code of the
   * basic model formula. Refer to the original paper for the list. 
   */
  @Override
  public abstract String toString();
}
