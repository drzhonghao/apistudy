import org.apache.lucene.search.similarities.*;



import org.apache.lucene.search.Explanation;

/**
 * Computes lambda as {@code docFreq+1 / numberOfDocuments+1}.
 * @lucene.experimental
 */
public class LambdaDF extends Lambda {
  
  /** Sole constructor: parameter-free */
  public LambdaDF() {}

  @Override
  public final float lambda(BasicStats stats) {
    return (stats.getDocFreq()+1F) / (stats.getNumberOfDocuments()+1F);
  }
  
  @Override
  public final Explanation explain(BasicStats stats) {
    return Explanation.match(
        lambda(stats),
        getClass().getSimpleName() + ", computed from: ",
        Explanation.match(stats.getDocFreq(), "docFreq"),
        Explanation.match(stats.getNumberOfDocuments(), "numberOfDocuments"));
  }
  
  @Override
  public String toString() {
    return "D";
  }
}
