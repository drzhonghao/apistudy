import org.apache.lucene.analysis.ko.Token;
import org.apache.lucene.analysis.ko.POS;
import org.apache.lucene.analysis.ko.*;


import org.apache.lucene.analysis.ko.dict.Dictionary;

/**
 * A token that was generated from a compound.
 */
public class DecompoundToken extends Token {
  private final POS.Tag posTag;

  /**
   *  Creates a new DecompoundToken
   * @param posTag The part of speech of the token.
   * @param surfaceForm The surface form of the token.
   * @param startOffset The start offset of the token in the analyzed text.
   * @param endOffset The end offset of the token in the analyzed text.
   */
  public DecompoundToken(POS.Tag posTag, String surfaceForm, int startOffset, int endOffset) {
    super(surfaceForm.toCharArray(), 0, surfaceForm.length(), startOffset, endOffset);
    this.posTag = posTag;
  }

  @Override
  public String toString() {
    return "DecompoundToken(\"" + getSurfaceFormString() + "\" pos=" + getStartOffset() + " length=" + getLength() +
        " startOffset=" + getStartOffset() + " endOffset=" + getEndOffset() + ")";
  }

  @Override
  public POS.Type getPOSType() {
    return POS.Type.MORPHEME;
  }

  @Override
  public POS.Tag getLeftPOS() {
    return posTag;
  }

  @Override
  public POS.Tag getRightPOS() {
    return posTag;
  }

  @Override
  public String getReading() {
    return null;
  }

  @Override
  public Dictionary.Morpheme[] getMorphemes() {
    return null;
  }
}
