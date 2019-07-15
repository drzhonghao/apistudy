import org.apache.lucene.search.similarities.BasicModel;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.*;



import static org.apache.lucene.search.similarities.SimilarityBase.log2;

/**
 * Implements the Poisson approximation for the binomial model for DFR.
 * @lucene.experimental
 * <p>
 * WARNING: for terms that do not meet the expected random distribution
 * (e.g. stopwords), this model may give poor performance, such as
 * abnormally high scores for low tf values.
 */
public class BasicModelP extends BasicModel {
  /** {@code log2(Math.E)}, precomputed. */
  protected static double LOG2_E = log2(Math.E);
  
  /** Sole constructor: parameter-free */
  public BasicModelP() {}
  
  @Override
  public final float score(BasicStats stats, float tfn) {
    float lambda = (float)(stats.getTotalTermFreq()+1) / (stats.getNumberOfDocuments()+1);
    return (float)(tfn * log2(tfn / lambda)
        + (lambda + 1 / (12 * tfn) - tfn) * LOG2_E
        + 0.5 * log2(2 * Math.PI * tfn));
  }

  @Override
  public String toString() {
    return "P";
  }
}
