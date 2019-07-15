import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.*;



/**
 * Implements the <em>Divergence from Independence (DFI)</em> model based on Chi-square statistics
 * (i.e., standardized Chi-squared distance from independence in term frequency tf).
 * <p>
 * DFI is both parameter-free and non-parametric:
 * <ul>
 * <li>parameter-free: it does not require any parameter tuning or training.</li>
 * <li>non-parametric: it does not make any assumptions about word frequency distributions on document collections.</li>
 * </ul>
 * <p>
 * It is highly recommended <b>not</b> to remove stopwords (very common terms: the, of, and, to, a, in, for, is, on, that, etc) with this similarity.
 * <p>
 * For more information see: <a href="http://dx.doi.org/10.1007/s10791-013-9225-4">A nonparametric term weighting method for information retrieval based on measuring the divergence from independence</a>
 *
 * @lucene.experimental
 * @see org.apache.lucene.search.similarities.IndependenceStandardized
 * @see org.apache.lucene.search.similarities.IndependenceSaturated
 * @see org.apache.lucene.search.similarities.IndependenceChiSquared
 */


public class DFISimilarity extends SimilarityBase {
  private final Independence independence;
  
  /**
   * Create DFI with the specified divergence from independence measure
   * @param independenceMeasure measure of divergence from independence
   */
  public DFISimilarity(Independence independenceMeasure) {
    this.independence = independenceMeasure;
  }

  @Override
  protected float score(BasicStats stats, float freq, float docLen) {

    final float expected = (stats.getTotalTermFreq() + 1) * docLen / (stats.getNumberOfFieldTokens() + 1);

    // if the observed frequency is less than or equal to the expected value, then return zero.
    if (freq <= expected) return 0;

    final float measure = independence.score(freq, expected);

    return stats.getBoost() * (float) log2(measure + 1);
  }

  /**
   * Returns the measure of independence
   */
  public Independence getIndependence() {
    return independence;
  }

  @Override
  public String toString() {
    return "DFI(" + independence + ")";
  }
}

