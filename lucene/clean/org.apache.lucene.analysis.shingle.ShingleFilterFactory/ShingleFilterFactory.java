import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.shingle.*;



import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

/** 
 * Factory for {@link ShingleFilter}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_shingle" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.ShingleFilterFactory" minShingleSize="2" maxShingleSize="2"
 *             outputUnigrams="true" outputUnigramsIfNoShingles="false" tokenSeparator=" " fillerToken="_"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 */
public class ShingleFilterFactory extends TokenFilterFactory {
  private final int minShingleSize;
  private final int maxShingleSize;
  private final boolean outputUnigrams;
  private final boolean outputUnigramsIfNoShingles;
  private final String tokenSeparator;
  private final String fillerToken;

  /** Creates a new ShingleFilterFactory */
  public ShingleFilterFactory(Map<String, String> args) {
    super(args);
    maxShingleSize = getInt(args, "maxShingleSize", ShingleFilter.DEFAULT_MAX_SHINGLE_SIZE);
    if (maxShingleSize < 2) {
      throw new IllegalArgumentException("Invalid maxShingleSize (" + maxShingleSize + ") - must be at least 2");
    }
    minShingleSize = getInt(args, "minShingleSize", ShingleFilter.DEFAULT_MIN_SHINGLE_SIZE);
    if (minShingleSize < 2) {
      throw new IllegalArgumentException("Invalid minShingleSize (" + minShingleSize + ") - must be at least 2");
    }
    if (minShingleSize > maxShingleSize) {
      throw new IllegalArgumentException
          ("Invalid minShingleSize (" + minShingleSize + ") - must be no greater than maxShingleSize (" + maxShingleSize + ")");
    }
    outputUnigrams = getBoolean(args, "outputUnigrams", true);
    outputUnigramsIfNoShingles = getBoolean(args, "outputUnigramsIfNoShingles", false);
    tokenSeparator = get(args, "tokenSeparator", ShingleFilter.DEFAULT_TOKEN_SEPARATOR);
    fillerToken = get(args, "fillerToken", ShingleFilter.DEFAULT_FILLER_TOKEN);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public ShingleFilter create(TokenStream input) {
    ShingleFilter r = new ShingleFilter(input, minShingleSize, maxShingleSize);
    r.setOutputUnigrams(outputUnigrams);
    r.setOutputUnigramsIfNoShingles(outputUnigramsIfNoShingles);
    r.setTokenSeparator(tokenSeparator);
    r.setFillerToken(fillerToken);
    return r;
  }
}

