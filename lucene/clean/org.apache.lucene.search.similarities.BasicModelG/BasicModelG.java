import org.apache.lucene.search.similarities.BasicModel;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.*;



import static org.apache.lucene.search.similarities.SimilarityBase.log2;

/**
 * Geometric as limiting form of the Bose-Einstein model.  The formula used in Lucene differs
 * slightly from the one in the original paper: {@code F} is increased by {@code 1}
 * and {@code N} is increased by {@code F}.
 * @lucene.experimental
 */
public class BasicModelG extends BasicModel {
  
  /** Sole constructor: parameter-free */
  public BasicModelG() {}

  @Override
  public final float score(BasicStats stats, float tfn) {
    // just like in BE, approximation only holds true when F << N, so we use lambda = F / (N + F)
    double F = stats.getTotalTermFreq() + 1;
    double N = stats.getNumberOfDocuments();
    double lambda = F / (N + F);
    // -log(1 / (lambda + 1)) -> log(lambda + 1)
    return (float)(log2(lambda + 1) + tfn * log2((1 + lambda) / lambda));
  }

  @Override
  public String toString() {
    return "G";
  }
}
