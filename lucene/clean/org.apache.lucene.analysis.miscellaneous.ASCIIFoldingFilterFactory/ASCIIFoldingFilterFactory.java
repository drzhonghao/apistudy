import org.apache.lucene.analysis.miscellaneous.*;



import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.TokenStream;

/** 
 * Factory for {@link ASCIIFoldingFilter}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_ascii" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.ASCIIFoldingFilterFactory" preserveOriginal="false"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 */
public class ASCIIFoldingFilterFactory extends TokenFilterFactory implements MultiTermAwareComponent {
  private static final String PRESERVE_ORIGINAL = "preserveOriginal";

  private final boolean preserveOriginal;
  
  /** Creates a new ASCIIFoldingFilterFactory */
  public ASCIIFoldingFilterFactory(Map<String,String> args) {
    super(args);
    preserveOriginal = getBoolean(args, PRESERVE_ORIGINAL, false);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }
  
  @Override
  public ASCIIFoldingFilter create(TokenStream input) {
    return new ASCIIFoldingFilter(input, preserveOriginal);
  }

  @Override
  public AbstractAnalysisFactory getMultiTermComponent() {
    if (preserveOriginal) {
      // The main use-case for using preserveOriginal is to match regardless of
      // case but to give better scores to exact matches. Since most multi-term
      // queries return constant scores anyway, the multi-term component only
      // emits the folded token
      Map<String, String> args = new HashMap<>(getOriginalArgs());
      args.remove(PRESERVE_ORIGINAL);
      return new ASCIIFoldingFilterFactory(args);
    } else {
      return this;
    }
  }
}

