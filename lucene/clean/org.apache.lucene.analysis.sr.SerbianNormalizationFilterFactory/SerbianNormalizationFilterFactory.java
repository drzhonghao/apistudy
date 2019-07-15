import org.apache.lucene.analysis.sr.*;



import java.util.Arrays;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * Factory for {@link SerbianNormalizationFilter}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_srnorm" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.StandardTokenizerFactory"/&gt;
 *     &lt;filter class="solr.LowerCaseFilterFactory"/&gt;
 *     &lt;filter class="solr.SerbianNormalizationFilterFactory"
 *       haircut="bald"/&gt; 
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 * @since 5.0.0
 */
public class SerbianNormalizationFilterFactory extends TokenFilterFactory implements MultiTermAwareComponent {
  final String haircut;

  /** Creates a new SerbianNormalizationFilterFactory */
  public SerbianNormalizationFilterFactory(Map<String,String> args) {
    super(args);

  this.haircut = get(args, "haircut", Arrays.asList( "bald", "regular" ), "bald");
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public TokenStream create(TokenStream input) {
    if( this.haircut.equals( "regular" ) ) {
      return new SerbianNormalizationRegularFilter(input);
    } else {
      return new SerbianNormalizationFilter(input);
    }
  }

  @Override
  public AbstractAnalysisFactory getMultiTermComponent() {
    return this;
  }

}
