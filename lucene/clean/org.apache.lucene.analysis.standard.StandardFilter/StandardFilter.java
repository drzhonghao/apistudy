import org.apache.lucene.analysis.standard.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * Normalizes tokens extracted with {@link StandardTokenizer}.
 */
public class StandardFilter extends TokenFilter {

  /** Sole constructor */
  public StandardFilter(TokenStream in) {
    super(in);
  }
  
  @Override
  public final boolean incrementToken() throws IOException {
    return input.incrementToken(); // TODO: add some niceties for the new grammar
  }
}
