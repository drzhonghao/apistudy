import org.apache.lucene.analysis.phonetic.*;


import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.language.DaitchMokotoffSoundex;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/**
 * Create tokens for phonetic matches based on Daitch–Mokotoff Soundex.
 *
 * @lucene.experimental
 */
public final class DaitchMokotoffSoundexFilter extends TokenFilter {
  /** true if encoded tokens should be added as synonyms */
  protected boolean inject = true;
  /** phonetic encoder */
  protected DaitchMokotoffSoundex encoder = new DaitchMokotoffSoundex();

  // output is a string such as ab|ac|...
  private static final Pattern pattern = Pattern.compile("([^|]+)");

  // matcher over any buffered output
  private final Matcher matcher = pattern.matcher("");

  // encoded representation
  private String encoded;
  // preserves all attributes for any buffered outputs
  private State state;

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final PositionIncrementAttribute posAtt = addAttribute(PositionIncrementAttribute.class);

  /**
   * Creates a DaitchMokotoffSoundexFilter by either adding encoded forms as synonyms (
   * <code>inject=true</code>) or replacing them.
   */
  public DaitchMokotoffSoundexFilter(TokenStream in, boolean inject) {
    super(in);
    this.inject = inject;
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (matcher.find()) {
      assert state != null && encoded != null;
      restoreState(state);
      termAtt.setEmpty().append(encoded, matcher.start(1), matcher.end(1));
      posAtt.setPositionIncrement(0);
      return true;
    }

    if (input.incrementToken()) {
      // pass through zero-length terms
      if (termAtt.length() == 0) {
        return true;
      }

      encoded = encoder.soundex(termAtt.toString());
      state = captureState();
      matcher.reset(encoded);

      if (!inject) {
        if (matcher.find()) {
          termAtt.setEmpty().append(encoded, matcher.start(1), matcher.end(1));
        }
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    matcher.reset("");
    state = null;
  }
}