import org.apache.lucene.analysis.icu.ICUNormalizer2Filter;
import org.apache.lucene.analysis.icu.*;



import org.apache.lucene.analysis.TokenStream;

import com.ibm.icu.text.Normalizer2;

/**
 * A TokenFilter that applies search term folding to Unicode text,
 * applying foldings from UTR#30 Character Foldings.
 * <p>
 * This filter applies the following foldings from the report to unicode text:
 * <ul>
 * <li>Accent removal
 * <li>Case folding
 * <li>Canonical duplicates folding
 * <li>Dashes folding
 * <li>Diacritic removal (including stroke, hook, descender)
 * <li>Greek letterforms folding
 * <li>Han Radical folding
 * <li>Hebrew Alternates folding
 * <li>Jamo folding
 * <li>Letterforms folding
 * <li>Math symbol folding
 * <li>Multigraph Expansions: All
 * <li>Native digit folding
 * <li>No-break folding
 * <li>Overline folding
 * <li>Positional forms folding
 * <li>Small forms folding
 * <li>Space folding
 * <li>Spacing Accents folding
 * <li>Subscript folding
 * <li>Superscript folding
 * <li>Suzhou Numeral folding
 * <li>Symbol folding
 * <li>Underline folding
 * <li>Vertical forms folding
 * <li>Width folding
 * </ul>
 * <p>
 * Additionally, Default Ignorables are removed, and text is normalized to NFKC.
 * All foldings, case folding, and normalization mappings are applied recursively
 * to ensure a fully folded and normalized result.
 * </p>
 * <p>
 * A normalizer with additional settings such as a filter that lists characters not
 * to be normalized can be passed in the constructor.
 * </p>
 */
public final class ICUFoldingFilter extends ICUNormalizer2Filter {
  /**
   * A normalizer for search term folding to Unicode text,
   * applying foldings from UTR#30 Character Foldings.
   */
  public static final Normalizer2 NORMALIZER = Normalizer2.getInstance(
    // TODO: if the wrong version of the ICU jar is used, loading these data files may give a strange error.
    // maybe add an explicit check? http://icu-project.org/apiref/icu4j/com/ibm/icu/util/VersionInfo.html
    ICUFoldingFilter.class.getResourceAsStream("utr30.nrm"),
    "utr30", Normalizer2.Mode.COMPOSE);

  /**
   * Create a new ICUFoldingFilter on the specified input
   */
  public ICUFoldingFilter(TokenStream input) {
    super(input, NORMALIZER);
  }

  /**
   * Create a new ICUFoldingFilter on the specified input with the specified
   * normalizer
   */
  public ICUFoldingFilter(TokenStream input, Normalizer2 normalizer) {
    super(input, normalizer);
  }
}
