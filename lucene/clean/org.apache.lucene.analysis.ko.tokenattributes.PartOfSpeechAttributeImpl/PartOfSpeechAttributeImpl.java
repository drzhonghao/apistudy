import org.apache.lucene.analysis.ko.tokenattributes.*;


import org.apache.lucene.analysis.ko.POS.Type;
import org.apache.lucene.analysis.ko.POS.Tag;
import org.apache.lucene.analysis.ko.Token;
import org.apache.lucene.analysis.ko.dict.Dictionary.Morpheme;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/**
 * Part of Speech attributes for Korean.
 * @lucene.experimental
 */
public class PartOfSpeechAttributeImpl extends AttributeImpl implements PartOfSpeechAttribute, Cloneable {
  private Token token;

  @Override
  public Type getPOSType() {
    return token == null ? null : token.getPOSType();
  }

  @Override
  public Tag getLeftPOS() {
    return token == null ? null : token.getLeftPOS();
  }

  @Override
  public Tag getRightPOS() {
    return token == null ? null : token.getRightPOS();
  }

  @Override
  public Morpheme[] getMorphemes() {
    return token == null ? null : token.getMorphemes();
  }

  @Override
  public void setToken(Token token) {
    this.token = token;
  }

  @Override
  public void clear() {
    token = null;
  }

  @Override
  public void reflectWith(AttributeReflector reflector) {
    reflector.reflect(PartOfSpeechAttribute.class, "posType", getPOSType().name());
    Tag leftPOS = getLeftPOS();
    reflector.reflect(PartOfSpeechAttribute.class, "leftPOS", leftPOS.name() + "(" + leftPOS.description() + ")");
    Tag rightPOS = getRightPOS();
    reflector.reflect(PartOfSpeechAttribute.class, "rightPOS", rightPOS.name() + "(" + rightPOS.description() + ")");
    reflector.reflect(PartOfSpeechAttribute.class, "morphemes", displayMorphemes(getMorphemes()));
  }

  private String displayMorphemes(Morpheme[] morphemes) {
    if (morphemes == null) {
      return null;
    }
    StringBuilder builder = new StringBuilder();
    for (Morpheme morpheme : morphemes) {
      if (builder.length() > 0) {
        builder.append("+");
      }
      builder.append(morpheme.surfaceForm + "/" + morpheme.posTag.name() + "(" + morpheme.posTag.description() + ")");
    }
    return builder.toString();
  }

  @Override
  public void copyTo(AttributeImpl target) {
    PartOfSpeechAttribute t = (PartOfSpeechAttribute) target;
    t.setToken(token);
  }
}
