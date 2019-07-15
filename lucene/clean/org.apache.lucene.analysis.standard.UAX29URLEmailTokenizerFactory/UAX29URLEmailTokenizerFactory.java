import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
import org.apache.lucene.analysis.standard.*;



import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

import java.util.Map;

/**
 * Factory for {@link UAX29URLEmailTokenizer}. 
 * <pre class="prettyprint">
 * &lt;fieldType name="text_urlemail" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.UAX29URLEmailTokenizerFactory" maxTokenLength="255"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 */
public class UAX29URLEmailTokenizerFactory extends TokenizerFactory {
  private final int maxTokenLength;

  /** Creates a new UAX29URLEmailTokenizerFactory */
  public UAX29URLEmailTokenizerFactory(Map<String,String> args) {
    super(args);
    maxTokenLength = getInt(args, "maxTokenLength", StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public UAX29URLEmailTokenizer create(AttributeFactory factory) {
    UAX29URLEmailTokenizer tokenizer = new UAX29URLEmailTokenizer(factory);
    tokenizer.setMaxTokenLength(maxTokenLength);
    return tokenizer;
  }
}
