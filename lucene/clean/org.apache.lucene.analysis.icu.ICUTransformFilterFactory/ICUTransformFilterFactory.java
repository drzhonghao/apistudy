import org.apache.lucene.analysis.icu.*;



import java.util.Arrays;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory; // javadocs
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import com.ibm.icu.text.Transliterator;

/**
 * Factory for {@link ICUTransformFilter}.
 * <p>
 * Supports the following attributes:
 * <ul>
 *   <li>id (mandatory): A Transliterator ID, one from {@link Transliterator#getAvailableIDs()}
 *   <li>direction (optional): Either 'forward' or 'reverse'. Default is forward.
 * </ul>
 * @see Transliterator
 * @since 3.1.0
 */
public class ICUTransformFilterFactory extends TokenFilterFactory implements MultiTermAwareComponent {
  private final Transliterator transliterator;
  
  // TODO: add support for custom rules
  /** Creates a new ICUTransformFilterFactory */
  public ICUTransformFilterFactory(Map<String,String> args) {
    super(args);
    String id = require(args, "id");
    String direction = get(args, "direction", Arrays.asList("forward", "reverse"), "forward", false);
    int dir = "forward".equals(direction) ? Transliterator.FORWARD : Transliterator.REVERSE;
    transliterator = Transliterator.getInstance(id, dir);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public TokenStream create(TokenStream input) {
    return new ICUTransformFilter(input, transliterator);
  }
  
  @Override
  public AbstractAnalysisFactory getMultiTermComponent() {
    return this;
  }
}
