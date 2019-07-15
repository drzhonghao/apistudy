import org.apache.lucene.search.similarities.BasicModel;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.*;



import static org.apache.lucene.search.similarities.SimilarityBase.log2;

/**
 * An approximation of the <em>I(n<sub>e</sub>)</em> model.
 * @lucene.experimental
 */ 
public class BasicModelIF extends BasicModel {
  
  /** Sole constructor: parameter-free */
  public BasicModelIF() {}

  @Override
  public final float score(BasicStats stats, float tfn) {
    long N = stats.getNumberOfDocuments();
    long F = stats.getTotalTermFreq();
    return tfn * (float)(log2(1 + (N + 1) / (F + 0.5)));
  }

  @Override
  public String toString() {
    return "I(F)";
  }
}
