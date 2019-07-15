import org.apache.lucene.analysis.miscellaneous.*;


import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * A TokenFilter that only keeps tokens with text contained in the
 * required words.  This filter behaves like the inverse of StopFilter.
 * 
 * @since solr 1.3
 */
public final class KeepWordFilter extends FilteringTokenFilter {
  private final CharArraySet words;
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  /**
   * Create a new {@link KeepWordFilter}.
   * <p><b>NOTE</b>: The words set passed to this constructor will be directly
   * used by this filter and should not be modified.
   * @param in      the {@link TokenStream} to consume
   * @param words   the words to keep
   */
  public KeepWordFilter(TokenStream in, CharArraySet words) {
    super(in);
    this.words = words;
  }

  @Override
  public boolean accept() {
    return words.contains(termAtt.buffer(), 0, termAtt.length());
  }
}
