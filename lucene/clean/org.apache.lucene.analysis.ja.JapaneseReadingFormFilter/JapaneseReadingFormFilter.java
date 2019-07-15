import org.apache.lucene.analysis.ja.*;



import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.ja.util.ToStringUtil;

import java.io.IOException;

/**
 * A {@link org.apache.lucene.analysis.TokenFilter} that replaces the term
 * attribute with the reading of a token in either katakana or romaji form.
 * The default reading form is katakana.
 */

public final class JapaneseReadingFormFilter extends TokenFilter {
  private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
  private final ReadingAttribute readingAttr = addAttribute(ReadingAttribute.class);

  private StringBuilder buffer = new StringBuilder();
  private boolean useRomaji;

  public JapaneseReadingFormFilter(TokenStream input, boolean useRomaji) {
    super(input);
    this.useRomaji = useRomaji;
  }

  public JapaneseReadingFormFilter(TokenStream input) {
    this(input, false);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      String reading = readingAttr.getReading();
      
      if (useRomaji) {
        if (reading == null) {
          // if it's an OOV term, just try the term text
          buffer.setLength(0);
          ToStringUtil.getRomanization(buffer, termAttr);
          termAttr.setEmpty().append(buffer);
        } else {
          ToStringUtil.getRomanization(termAttr.setEmpty(), reading);
        }
      } else {
        // just replace the term text with the reading, if it exists
        if (reading != null) {
          termAttr.setEmpty().append(reading);
        }
      }
      return true;
    } else {
      return false;
    }
  }
}
