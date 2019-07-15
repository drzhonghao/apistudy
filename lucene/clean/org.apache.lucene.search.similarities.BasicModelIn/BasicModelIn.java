import org.apache.lucene.search.similarities.*;



import org.apache.lucene.search.Explanation;
import static org.apache.lucene.search.similarities.SimilarityBase.log2;

/**
 * The basic tf-idf model of randomness.
 * @lucene.experimental
 */ 
public class BasicModelIn extends BasicModel {
  
  /** Sole constructor: parameter-free */
  public BasicModelIn() {}

  @Override
  public final float score(BasicStats stats, float tfn) {
    long N = stats.getNumberOfDocuments();
    long n = stats.getDocFreq();
    return tfn * (float)(log2((N + 1) / (n + 0.5)));
  }
  
  @Override
  public final Explanation explain(BasicStats stats, float tfn) {
    return Explanation.match(
        score(stats, tfn),
        getClass().getSimpleName() + ", computed from: ",
        Explanation.match(stats.getNumberOfDocuments(), "numberOfDocuments"),
        Explanation.match(stats.getDocFreq(), "docFreq"));
  }

  @Override
  public String toString() {
    return "I(n)";
  }
}
