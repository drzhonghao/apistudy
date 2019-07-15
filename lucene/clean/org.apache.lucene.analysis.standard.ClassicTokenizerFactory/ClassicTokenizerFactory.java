import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.analysis.standard.*;



import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

import java.util.Map;

/**
 * Factory for {@link ClassicTokenizer}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_clssc" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.ClassicTokenizerFactory" maxTokenLength="120"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 */
public class ClassicTokenizerFactory extends TokenizerFactory {
  private final int maxTokenLength;

  /** Creates a new ClassicTokenizerFactory */
  public ClassicTokenizerFactory(Map<String,String> args) {
    super(args);
    maxTokenLength = getInt(args, "maxTokenLength", StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public ClassicTokenizer create(AttributeFactory factory) {
    ClassicTokenizer tokenizer = new ClassicTokenizer(factory);
    tokenizer.setMaxTokenLength(maxTokenLength);
    return tokenizer;
  }
}
