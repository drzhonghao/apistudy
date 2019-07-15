import org.apache.lucene.search.similarities.*;



import org.apache.lucene.search.Explanation;

/**
 * Computes lambda as {@code totalTermFreq+1 / numberOfDocuments+1}.
 * @lucene.experimental
 */
public class LambdaTTF extends Lambda {  
  
  /** Sole constructor: parameter-free */
  public LambdaTTF() {}

  @Override
  public final float lambda(BasicStats stats) {
    return (stats.getTotalTermFreq()+1F) / (stats.getNumberOfDocuments()+1F);
  }

  @Override
  public final Explanation explain(BasicStats stats) {
    return Explanation.match(
        lambda(stats),
        getClass().getSimpleName() + ", computed from: ",
        Explanation.match(stats.getTotalTermFreq(), "totalTermFreq"),
        Explanation.match(stats.getNumberOfDocuments(), "numberOfDocuments"));
  }
  
  @Override
  public String toString() {
    return "L";
  }
}
