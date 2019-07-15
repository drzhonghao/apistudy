import org.apache.lucene.analysis.miscellaneous.*;


import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/** 
 * A filter to correct offsets that illegally go backwards.
 *
 * @deprecated Fix the token filters that create broken offsets in the first place.
 */
@Deprecated
public final class FixBrokenOffsetsFilter extends TokenFilter {

  private int lastStartOffset;
  private int lastEndOffset;

  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

  public FixBrokenOffsetsFilter(TokenStream in) {
    super(in);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken() == false) {
      return false;
    }
    fixOffsets();
    return true;
  }

  @Override
  public void end() throws IOException {
    super.end();
    fixOffsets();
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    lastStartOffset = 0;
    lastEndOffset = 0;
  }

  private void fixOffsets() {
    int startOffset = offsetAtt.startOffset();
    int endOffset = offsetAtt.endOffset();
    if (startOffset < lastStartOffset) {
      startOffset = lastStartOffset;
    }
    if (endOffset < startOffset) {
      endOffset = startOffset;
    }
    offsetAtt.setOffset(startOffset, endOffset);
    lastStartOffset = startOffset;
    lastEndOffset = endOffset;
  }
}
