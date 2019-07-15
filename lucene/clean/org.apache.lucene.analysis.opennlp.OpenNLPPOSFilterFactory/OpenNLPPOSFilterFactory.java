import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilter;
import org.apache.lucene.analysis.opennlp.*;


import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.opennlp.tools.OpenNLPOpsFactory;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * Factory for {@link OpenNLPPOSFilter}.
 *
 * <pre class="prettyprint">
 * &lt;fieldType name="text_opennlp_pos" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.OpenNLPTokenizerFactory" sentenceModel="filename" tokenizerModel="filename"/&gt;
 *     &lt;filter class="solr.OpenNLPPOSFilterFactory" posTaggerModel="filename"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 * @since 7.3.0
 */
public class OpenNLPPOSFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
  public static final String POS_TAGGER_MODEL = "posTaggerModel";

  private final String posTaggerModelFile;

  public OpenNLPPOSFilterFactory(Map<String,String> args) {
    super(args);
    posTaggerModelFile = require(args, POS_TAGGER_MODEL);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public OpenNLPPOSFilter create(TokenStream in) {
    try {
      return new OpenNLPPOSFilter(in, OpenNLPOpsFactory.getPOSTagger(posTaggerModelFile));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public void inform(ResourceLoader loader) {
    try { // load and register the read-only model in cache with file/resource name
      OpenNLPOpsFactory.getPOSTaggerModel(posTaggerModelFile, loader);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
