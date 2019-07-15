import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.*;



import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

import java.util.Map;

/**
 * Factory for {@link StandardTokenizer}. 
 * <pre class="prettyprint">
 * &lt;fieldType name="text_stndrd" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.StandardTokenizerFactory" maxTokenLength="255"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 */
public class StandardTokenizerFactory extends TokenizerFactory {
  private final int maxTokenLength;
  
  /** Creates a new StandardTokenizerFactory */
  public StandardTokenizerFactory(Map<String,String> args) {
    super(args);
    maxTokenLength = getInt(args, "maxTokenLength", StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public StandardTokenizer create(AttributeFactory factory) {
    StandardTokenizer tokenizer = new StandardTokenizer(factory);
    tokenizer.setMaxTokenLength(maxTokenLength);
    return tokenizer;
  }
}
