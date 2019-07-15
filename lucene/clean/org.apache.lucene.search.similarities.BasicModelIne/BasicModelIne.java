import org.apache.lucene.search.similarities.BasicModel;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.*;



import static org.apache.lucene.search.similarities.SimilarityBase.log2;

/**
 * Tf-idf model of randomness, based on a mixture of Poisson and inverse
 * document frequency.
 * @lucene.experimental
 */ 
public class BasicModelIne extends BasicModel {
  
  /** Sole constructor: parameter-free */
  public BasicModelIne() {}

  @Override
  public final float score(BasicStats stats, float tfn) {
    long N = stats.getNumberOfDocuments();
    long F = stats.getTotalTermFreq();
    double ne = N * (1 - Math.pow((N - 1) / (double)N, F));
    return tfn * (float)(log2((N + 1) / (ne + 0.5)));
  }

  @Override
  public String toString() {
    return "I(ne)";
  }
}
