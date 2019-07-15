import org.apache.lucene.analysis.payloads.*;



import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

/**
 * Factory for {@link DelimitedPayloadTokenFilter}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_dlmtd" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.DelimitedPayloadTokenFilterFactory" encoder="float" delimiter="|"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 */
public class DelimitedPayloadTokenFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
  public static final String ENCODER_ATTR = "encoder";
  public static final String DELIMITER_ATTR = "delimiter";

  private final String encoderClass;
  private final char delimiter;

  private PayloadEncoder encoder;
  
  /** Creates a new DelimitedPayloadTokenFilterFactory */
  public DelimitedPayloadTokenFilterFactory(Map<String, String> args) {
    super(args);
    encoderClass = require(args, ENCODER_ATTR);
    delimiter = getChar(args, DELIMITER_ATTR, '|');
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public DelimitedPayloadTokenFilter create(TokenStream input) {
    return new DelimitedPayloadTokenFilter(input, delimiter, encoder);
  }

  @Override
  public void inform(ResourceLoader loader) {
    if (encoderClass.equals("float")){
      encoder = new FloatEncoder();
    } else if (encoderClass.equals("integer")){
      encoder = new IntegerEncoder();
    } else if (encoderClass.equals("identity")){
      encoder = new IdentityEncoder();
    } else {
      encoder = loader.newInstance(encoderClass, PayloadEncoder.class);
    }
  }
}
