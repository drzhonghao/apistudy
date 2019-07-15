import org.apache.lucene.analysis.pattern.*;



import java.io.Reader;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;

/**
 * Factory for {@link PatternReplaceCharFilter}. 
 * <pre class="prettyprint">
 * &lt;fieldType name="text_ptnreplace" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;charFilter class="solr.PatternReplaceCharFilterFactory" 
 *                    pattern="([^a-z])" replacement=""/&gt;
 *     &lt;tokenizer class="solr.KeywordTokenizerFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 * 
 * @since Solr 3.1
 */
public class PatternReplaceCharFilterFactory extends CharFilterFactory implements MultiTermAwareComponent {
  private final Pattern pattern;
  private final String replacement;

  /** Creates a new PatternReplaceCharFilterFactory */
  public PatternReplaceCharFilterFactory(Map<String, String> args) {
    super(args);
    pattern = getPattern(args, "pattern");
    replacement = get(args, "replacement", "");
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public CharFilter create(Reader input) {
    return new PatternReplaceCharFilter(pattern, replacement, input);
  }

  @Override
  public AbstractAnalysisFactory getMultiTermComponent() {
    return this;
  }
}
