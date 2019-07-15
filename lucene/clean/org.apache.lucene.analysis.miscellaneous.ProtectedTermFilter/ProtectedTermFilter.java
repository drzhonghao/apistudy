import org.apache.lucene.analysis.miscellaneous.*;


import java.util.function.Function;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * A ConditionalTokenFilter that only applies its wrapped filters to tokens that
 * are not contained in a protected set.
 */
public class ProtectedTermFilter extends ConditionalTokenFilter {

  private final CharArraySet protectedTerms;

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  /**
   * Creates a new ProtectedTermFilter
   * @param protectedTerms  the set of terms to skip the wrapped filters for
   * @param input         the input TokenStream
   * @param inputFactory  a factory function to create the wrapped filter(s)
   */
  public ProtectedTermFilter(final CharArraySet protectedTerms, TokenStream input, Function<TokenStream, TokenStream> inputFactory) {
    super(input, inputFactory);
    this.protectedTerms = protectedTerms;
  }

  @Override
  protected boolean shouldFilter() {
    boolean b = protectedTerms.contains(termAtt.buffer(), 0, termAtt.length());
    return b == false;
  }

}
