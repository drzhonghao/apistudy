import org.apache.lucene.analysis.en.*;



import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * TokenFilter that removes possessives (trailing 's) from words.
 */
public final class EnglishPossessiveFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  public EnglishPossessiveFilter(TokenStream input) {
    super(input);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (!input.incrementToken()) {
      return false;
    }
    
    final char[] buffer = termAtt.buffer();
    final int bufferLength = termAtt.length();
    
    if (bufferLength >= 2 && 
        (buffer[bufferLength-2] == '\'' || 
         buffer[bufferLength-2] == '\u2019' || 
         buffer[bufferLength-2] == '\uFF07') &&
        (buffer[bufferLength-1] == 's' || buffer[bufferLength-1] == 'S')) {
      termAtt.setLength(bufferLength - 2); // Strip last 2 characters off
    }

    return true;
  }
}
