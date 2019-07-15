import org.apache.lucene.analysis.ko.*;


import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ko.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Replaces term text with the {@link ReadingAttribute} which is
 * the Hangul transcription of Hanja characters.
 * @lucene.experimental
 */
public final class KoreanReadingFormFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final ReadingAttribute readingAtt = addAttribute(ReadingAttribute.class);

  public KoreanReadingFormFilter(TokenStream input) {
    super(input);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      String reading = readingAtt.getReading();
      if (reading != null) {
        termAtt.setEmpty().append(reading);
      }
      return true;
    } else {
      return false;
    }
  }
}
