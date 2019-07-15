import org.apache.lucene.analysis.minhash.MinHashFilter;
import org.apache.lucene.analysis.minhash.*;


import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * {@link TokenFilterFactory} for {@link MinHashFilter}.
 * @since 6.2.0
 */
public class MinHashFilterFactory extends TokenFilterFactory {
  private int hashCount = MinHashFilter.DEFAULT_HASH_COUNT;
  
  private int bucketCount = MinHashFilter.DEFAULT_BUCKET_COUNT;

  private int hashSetSize = MinHashFilter.DEFAULT_HASH_SET_SIZE;
  
  private boolean withRotation;

  /**
   * Create a {@link MinHashFilterFactory}.
   */
  public MinHashFilterFactory(Map<String,String> args) {
    super(args);
    hashCount = getInt(args, "hashCount", MinHashFilter.DEFAULT_HASH_COUNT);
    bucketCount = getInt(args, "bucketCount", MinHashFilter.DEFAULT_BUCKET_COUNT);
    hashSetSize = getInt(args, "hashSetSize", MinHashFilter.DEFAULT_HASH_SET_SIZE);
    withRotation = getBoolean(args, "withRotation", bucketCount > 1);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.analysis.util.TokenFilterFactory#create(org.apache.lucene.analysis.TokenStream)
   */
  @Override
  public TokenStream create(TokenStream input) {
    return new MinHashFilter(input, hashCount, bucketCount, hashSetSize, withRotation);
  }

}
