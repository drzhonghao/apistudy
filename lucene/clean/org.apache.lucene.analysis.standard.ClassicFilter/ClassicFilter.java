import org.apache.lucene.analysis.standard.*;



import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/** Normalizes tokens extracted with {@link ClassicTokenizer}. */

public class ClassicFilter extends TokenFilter {

  /** Construct filtering <i>in</i>. */
  public ClassicFilter(TokenStream in) {
    super(in);
  }

  private static final String APOSTROPHE_TYPE = ClassicTokenizer.TOKEN_TYPES[ClassicTokenizer.APOSTROPHE];
  private static final String ACRONYM_TYPE = ClassicTokenizer.TOKEN_TYPES[ClassicTokenizer.ACRONYM];

  // this filters uses attribute type
  private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  
  /** Returns the next token in the stream, or null at EOS.
   * <p>Removes <tt>'s</tt> from the end of words.
   * <p>Removes dots from acronyms.
   */
  @Override
  public final boolean incrementToken() throws java.io.IOException {
    if (!input.incrementToken()) {
      return false;
    }

    final char[] buffer = termAtt.buffer();
    final int bufferLength = termAtt.length();
    final String type = typeAtt.type();

    if (type == APOSTROPHE_TYPE &&      // remove 's
        bufferLength >= 2 &&
        buffer[bufferLength-2] == '\'' &&
        (buffer[bufferLength-1] == 's' || buffer[bufferLength-1] == 'S')) {
      // Strip last 2 characters off
      termAtt.setLength(bufferLength - 2);
    } else if (type == ACRONYM_TYPE) {      // remove dots
      int upto = 0;
      for(int i=0;i<bufferLength;i++) {
        char c = buffer[i];
        if (c != '.')
          buffer[upto++] = c;
      }
      termAtt.setLength(upto);
    }

    return true;
  }
}
