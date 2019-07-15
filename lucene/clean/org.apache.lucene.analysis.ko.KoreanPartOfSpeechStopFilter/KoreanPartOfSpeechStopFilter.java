import org.apache.lucene.analysis.ko.POS;
import org.apache.lucene.analysis.ko.*;



import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ko.tokenattributes.PartOfSpeechAttribute;

/**
 * Removes tokens that match a set of part-of-speech tags.
 * @lucene.experimental
 */
public final class KoreanPartOfSpeechStopFilter extends FilteringTokenFilter {
  private final Set<POS.Tag> stopTags;
  private final PartOfSpeechAttribute posAtt = addAttribute(PartOfSpeechAttribute.class);

  /**
   * Default list of tags to filter.
   */
  public static final Set<POS.Tag> DEFAULT_STOP_TAGS = Arrays.asList(
      POS.Tag.E,
      POS.Tag.IC,
      POS.Tag.J,
      POS.Tag.MAG,
      POS.Tag.MAJ,
      POS.Tag.MM,
      POS.Tag.SP,
      POS.Tag.SSC,
      POS.Tag.SSO,
      POS.Tag.SC,
      POS.Tag.SE,
      POS.Tag.XPN,
      POS.Tag.XSA,
      POS.Tag.XSN,
      POS.Tag.XSV,
      POS.Tag.UNA,
      POS.Tag.NA,
      POS.Tag.VSV
  ).stream().collect(Collectors.toSet());

  /**
   * Create a new {@link KoreanPartOfSpeechStopFilter} with the default
   * list of stop tags {@link #DEFAULT_STOP_TAGS}.
   *
   * @param input    the {@link TokenStream} to consume
   */
  public KoreanPartOfSpeechStopFilter(TokenStream input) {
    this(input, DEFAULT_STOP_TAGS);
  }

  /**
   * Create a new {@link KoreanPartOfSpeechStopFilter}.
   * @param input    the {@link TokenStream} to consume
   * @param stopTags the part-of-speech tags that should be removed
   */
  public KoreanPartOfSpeechStopFilter(TokenStream input, Set<POS.Tag> stopTags) {
    super(input);
    this.stopTags = stopTags;
  }

  @Override
  protected boolean accept() {
    final POS.Tag leftPOS = posAtt.getLeftPOS();
    return leftPOS == null || !stopTags.contains(leftPOS);
  }
}
