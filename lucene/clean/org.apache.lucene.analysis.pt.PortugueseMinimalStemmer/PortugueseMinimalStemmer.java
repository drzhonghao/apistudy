import org.apache.lucene.analysis.pt.RSLPStemmerBase;
import org.apache.lucene.analysis.pt.*;



/**
 * Minimal Stemmer for Portuguese
 * <p>
 * This follows the "RSLP-S" algorithm presented in:
 * <i>A study on the Use of Stemming for Monolingual Ad-Hoc Portuguese
 * Information Retrieval</i> (Orengo, et al)
 * which is just the plural reduction step of the RSLP
 * algorithm from <i>A Stemming Algorithm for the Portuguese Language</i>,
 * Orengo et al.
 * @see RSLPStemmerBase
 */
public class PortugueseMinimalStemmer extends RSLPStemmerBase {
  
  private static final Step pluralStep = 
    parse(PortugueseMinimalStemmer.class, "portuguese.rslp").get("Plural");
  
  public int stem(char s[], int len) {
    return pluralStep.apply(s, len);
  }
}
