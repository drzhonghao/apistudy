import org.apache.lucene.analysis.tr.*;



import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;

/**
 * Strips all characters after an apostrophe (including the apostrophe itself).
 * <p>
 * In Turkish, apostrophe is used to separate suffixes from proper names
 * (continent, sea, river, lake, mountain, upland, proper names related to
 * religion and mythology). This filter intended to be used before stem filters.
 * For more information, see <a href="http://www.ipcsit.com/vol57/015-ICNI2012-M021.pdf">
 * Role of Apostrophes in Turkish Information Retrieval</a>
 * </p>
 */
public final class ApostropheFilter extends TokenFilter {

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  public ApostropheFilter(TokenStream in) {
    super(in);
  }

  @Override
  public final boolean incrementToken() throws IOException {
    if (!input.incrementToken())
      return false;

    final char[] buffer = termAtt.buffer();
    final int length = termAtt.length();

    for (int i = 0; i < length; i++)
      if (buffer[i] == '\'' || buffer[i] == '\u2019') {
        termAtt.setLength(i);
        return true;
      }
    return true;
  }
}
