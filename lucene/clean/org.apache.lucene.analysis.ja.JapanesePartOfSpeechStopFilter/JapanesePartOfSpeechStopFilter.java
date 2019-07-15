import org.apache.lucene.analysis.ja.*;



import java.util.Set;

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;

/**
 * Removes tokens that match a set of part-of-speech tags.
 */
public final class JapanesePartOfSpeechStopFilter extends FilteringTokenFilter {
  private final Set<String> stopTags;
  private final PartOfSpeechAttribute posAtt = addAttribute(PartOfSpeechAttribute.class);

  /**
   * Create a new {@link JapanesePartOfSpeechStopFilter}.
   * @param input    the {@link TokenStream} to consume
   * @param stopTags the part-of-speech tags that should be removed
   */
  public JapanesePartOfSpeechStopFilter(TokenStream input, Set<String> stopTags) {
    super(input);
    this.stopTags = stopTags;
  }

  @Override
  protected boolean accept() {
    final String pos = posAtt.getPartOfSpeech();
    return pos == null || !stopTags.contains(pos);
  }
}
