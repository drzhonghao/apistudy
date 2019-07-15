import org.apache.lucene.analysis.icu.*;



import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.icu.ICUFoldingFilter;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory; // javadocs
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import com.ibm.icu.text.FilteredNormalizer2;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;

/**
 * Factory for {@link ICUFoldingFilter}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_folded" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.ICUFoldingFilterFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 * @since 3.1.0
 */
public class ICUFoldingFilterFactory extends TokenFilterFactory implements MultiTermAwareComponent {
  private final Normalizer2 normalizer;

  /** Creates a new ICUFoldingFilterFactory */
  public ICUFoldingFilterFactory(Map<String,String> args) {
    super(args);

    Normalizer2 normalizer = ICUFoldingFilter.NORMALIZER;
    String filter = get(args, "filter");
    if (filter != null) {
      UnicodeSet set = new UnicodeSet(filter);
      if (!set.isEmpty()) {
        set.freeze();
        normalizer = new FilteredNormalizer2(normalizer, set);
      }
    }
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
    this.normalizer = normalizer;
  }

  @Override
  public TokenStream create(TokenStream input) {
    return new ICUFoldingFilter(input, normalizer);
  }

  @Override
  public AbstractAnalysisFactory getMultiTermComponent() {
    return this;
  }
}
