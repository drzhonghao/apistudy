import org.apache.lucene.analysis.gl.*;



import org.apache.lucene.analysis.pt.RSLPStemmerBase;

/**
 * Minimal Stemmer for Galician
 * <p>
 * This follows the "RSLP-S" algorithm, but modified for Galician.
 * Hence this stemmer only applies the plural reduction step of:
 * "Regras do lematizador para o galego"
 * @see RSLPStemmerBase
 */
public class GalicianMinimalStemmer extends RSLPStemmerBase {
  
  private static final Step pluralStep = 
    parse(GalicianMinimalStemmer.class, "galician.rslp").get("Plural");
  
  public int stem(char s[], int len) {
    return pluralStep.apply(s, len);
  }
}
