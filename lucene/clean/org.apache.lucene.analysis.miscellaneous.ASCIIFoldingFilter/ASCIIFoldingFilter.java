import org.apache.lucene.analysis.miscellaneous.*;


import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.ArrayUtil;

/**
 * This class converts alphabetic, numeric, and symbolic Unicode characters
 * which are not in the first 127 ASCII characters (the "Basic Latin" Unicode
 * block) into their ASCII equivalents, if one exists.
 *
 * Characters from the following Unicode blocks are converted; however, only
 * those characters with reasonable ASCII alternatives are converted:
 *
 * <ul>
 *   <li>C1 Controls and Latin-1 Supplement: <a href="http://www.unicode.org/charts/PDF/U0080.pdf">http://www.unicode.org/charts/PDF/U0080.pdf</a>
 *   <li>Latin Extended-A: <a href="http://www.unicode.org/charts/PDF/U0100.pdf">http://www.unicode.org/charts/PDF/U0100.pdf</a>
 *   <li>Latin Extended-B: <a href="http://www.unicode.org/charts/PDF/U0180.pdf">http://www.unicode.org/charts/PDF/U0180.pdf</a>
 *   <li>Latin Extended Additional: <a href="http://www.unicode.org/charts/PDF/U1E00.pdf">http://www.unicode.org/charts/PDF/U1E00.pdf</a>
 *   <li>Latin Extended-C: <a href="http://www.unicode.org/charts/PDF/U2C60.pdf">http://www.unicode.org/charts/PDF/U2C60.pdf</a>
 *   <li>Latin Extended-D: <a href="http://www.unicode.org/charts/PDF/UA720.pdf">http://www.unicode.org/charts/PDF/UA720.pdf</a>
 *   <li>IPA Extensions: <a href="http://www.unicode.org/charts/PDF/U0250.pdf">http://www.unicode.org/charts/PDF/U0250.pdf</a>
 *   <li>Phonetic Extensions: <a href="http://www.unicode.org/charts/PDF/U1D00.pdf">http://www.unicode.org/charts/PDF/U1D00.pdf</a>
 *   <li>Phonetic Extensions Supplement: <a href="http://www.unicode.org/charts/PDF/U1D80.pdf">http://www.unicode.org/charts/PDF/U1D80.pdf</a>
 *   <li>General Punctuation: <a href="http://www.unicode.org/charts/PDF/U2000.pdf">http://www.unicode.org/charts/PDF/U2000.pdf</a>
 *   <li>Superscripts and Subscripts: <a href="http://www.unicode.org/charts/PDF/U2070.pdf">http://www.unicode.org/charts/PDF/U2070.pdf</a>
 *   <li>Enclosed Alphanumerics: <a href="http://www.unicode.org/charts/PDF/U2460.pdf">http://www.unicode.org/charts/PDF/U2460.pdf</a>
 *   <li>Dingbats: <a href="http://www.unicode.org/charts/PDF/U2700.pdf">http://www.unicode.org/charts/PDF/U2700.pdf</a>
 *   <li>Supplemental Punctuation: <a href="http://www.unicode.org/charts/PDF/U2E00.pdf">http://www.unicode.org/charts/PDF/U2E00.pdf</a>
 *   <li>Alphabetic Presentation Forms: <a href="http://www.unicode.org/charts/PDF/UFB00.pdf">http://www.unicode.org/charts/PDF/UFB00.pdf</a>
 *   <li>Halfwidth and Fullwidth Forms: <a href="http://www.unicode.org/charts/PDF/UFF00.pdf">http://www.unicode.org/charts/PDF/UFF00.pdf</a>
 * </ul>
 *  
 * See: <a href="http://en.wikipedia.org/wiki/Latin_characters_in_Unicode">http://en.wikipedia.org/wiki/Latin_characters_in_Unicode</a>
 *
 * For example, '&agrave;' will be replaced by 'a'.
 */
public final class ASCIIFoldingFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final PositionIncrementAttribute posIncAttr = addAttribute(PositionIncrementAttribute.class);
  private final boolean preserveOriginal;
  private char[] output = new char[512];
  private int outputPos;
  private State state;

  public ASCIIFoldingFilter(TokenStream input)
  {
    this(input, false);
  }

  /**
   * Create a new {@link ASCIIFoldingFilter}.
   * 
   * @param input
   *          TokenStream to filter
   * @param preserveOriginal
   *          should the original tokens be kept on the input stream with a 0 position increment
   *          from the folded tokens?
   **/
  public ASCIIFoldingFilter(TokenStream input, boolean preserveOriginal)
  {
    super(input);
    this.preserveOriginal = preserveOriginal;
  }

  /**
   * Does the filter preserve the original tokens?
   */
  public boolean isPreserveOriginal() {
    return preserveOriginal;
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (state != null) {
      assert preserveOriginal : "state should only be captured if preserveOriginal is true";
      restoreState(state);
      posIncAttr.setPositionIncrement(0);
      state = null;
      return true;
    }
    if (input.incrementToken()) {
      final char[] buffer = termAtt.buffer();
      final int length = termAtt.length();

      // If no characters actually require rewriting then we
      // just return token as-is:
      for(int i = 0 ; i < length ; ++i) {
        final char c = buffer[i];
        if (c >= '\u0080')
        {
          foldToASCII(buffer, length);
          termAtt.copyBuffer(output, 0, outputPos);
          break;
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
    state = null;
  }

  /**
   * Converts characters above ASCII to their ASCII equivalents.  For example,
   * accents are removed from accented characters.
   * @param input The string to fold
   * @param length The number of characters in the input string
   */
  public void foldToASCII(char[] input, int length)
  {
    // Worst-case length required:
    final int maxSizeNeeded = 4 * length;
    if (output.length < maxSizeNeeded) {
      output = new char[ArrayUtil.oversize(maxSizeNeeded, Character.BYTES)];
    }

    outputPos = foldToASCII(input, 0, output, 0, length);
    if (preserveOriginal && needToPreserve(input, length)) {
      state = captureState();
    }
  }

  /**
   * Check if foldToASCII generated a different token.
   * @param input original term
   * @param inputLength length of the original term
   * @return true if foldToASCII generated a different token
   */
  private boolean needToPreserve(char[] input, int inputLength) {
    if(inputLength != outputPos) {
      return true;
    }
    for(int i = 0; i < inputLength; i++) {
      if(input[i] != output[i]) {
        return true;
      }
    }
    return false;
  }

  /**
   * Converts characters above ASCII to their ASCII equivalents.  For example,
   * accents are removed from accented characters.
   * @param input     The characters to fold
   * @param inputPos  Index of the first character to fold
   * @param output    The result of the folding. Should be of size &gt;= {@code length * 4}.
   * @param outputPos Index of output where to put the result of the folding
   * @param length    The number of characters to fold
   * @return length of output
   * @lucene.internal
   */
  public static final int foldToASCII(char input[], int inputPos, char output[], int outputPos, int length)
  {
    final int end = inputPos + length;
    for (int pos = inputPos; pos < end ; ++pos) {
      final char c = input[pos];

      // Quick test: if it's not in range then just keep current character
      if (c < '\u0080') {
        output[outputPos++] = c;
      } else {
        switch (c) {
          case '\u00C0': // À  [LATIN CAPITAL LETTER A WITH GRAVE]
          case '\u00C1': // Á  [LATIN CAPITAL LETTER A WITH ACUTE]
          case '\u00C2': // Â  [LATIN CAPITAL LETTER A WITH CIRCUMFLEX]
          case '\u00C3': // Ã  [LATIN CAPITAL LETTER A WITH TILDE]
          case '\u00C4': // Ä  [LATIN CAPITAL LETTER A WITH DIAERESIS]
          case '\u00C5': // Å  [LATIN CAPITAL LETTER A WITH RING ABOVE]
          case '\u0100': // Ā  [LATIN CAPITAL LETTER A WITH MACRON]
          case '\u0102': // Ă  [LATIN CAPITAL LETTER A WITH BREVE]
          case '\u0104': // Ą  [LATIN CAPITAL LETTER A WITH OGONEK]
          case '\u018F': // Ə  http://en.wikipedia.org/wiki/Schwa  [LATIN CAPITAL LETTER SCHWA]
          case '\u01CD': // Ǎ  [LATIN CAPITAL LETTER A WITH CARON]
          case '\u01DE': // Ǟ  [LATIN CAPITAL LETTER A WITH DIAERESIS AND MACRON]
          case '\u01E0': // Ǡ  [LATIN CAPITAL LETTER A WITH DOT ABOVE AND MACRON]
          case '\u01FA': // Ǻ  [LATIN CAPITAL LETTER A WITH RING ABOVE AND ACUTE]
          case '\u0200': // Ȁ  [LATIN CAPITAL LETTER A WITH DOUBLE GRAVE]
          case '\u0202': // Ȃ  [LATIN CAPITAL LETTER A WITH INVERTED BREVE]
          case '\u0226': // Ȧ  [LATIN CAPITAL LETTER A WITH DOT ABOVE]
          case '\u023A': // Ⱥ  [LATIN CAPITAL LETTER A WITH STROKE]
          case '\u1D00': // �?  [LATIN LETTER SMALL CAPITAL A]
          case '\u1E00': // �?  [LATIN CAPITAL LETTER A WITH RING BELOW]
          case '\u1EA0': // �?  [LATIN CAPITAL LETTER A WITH DOT BELOW]
          case '\u1EA2': // �?  [LATIN CAPITAL LETTER A WITH HOOK ABOVE]
          case '\u1EA4': // �?  [LATIN CAPITAL LETTER A WITH CIRCUMFLEX AND ACUTE]
          case '\u1EA6': // �?  [LATIN CAPITAL LETTER A WITH CIRCUMFLEX AND GRAVE]
          case '\u1EA8': // �?  [LATIN CAPITAL LETTER A WITH CIRCUMFLEX AND HOOK ABOVE]
          case '\u1EAA': // �?  [LATIN CAPITAL LETTER A WITH CIRCUMFLEX AND TILDE]
          case '\u1EAC': // �?  [LATIN CAPITAL LETTER A WITH CIRCUMFLEX AND DOT BELOW]
          case '\u1EAE': // �?  [LATIN CAPITAL LETTER A WITH BREVE AND ACUTE]
          case '\u1EB0': // �?  [LATIN CAPITAL LETTER A WITH BREVE AND GRAVE]
          case '\u1EB2': // �?  [LATIN CAPITAL LETTER A WITH BREVE AND HOOK ABOVE]
          case '\u1EB4': // �?  [LATIN CAPITAL LETTER A WITH BREVE AND TILDE]
          case '\u1EB6': // �?  [LATIN CAPITAL LETTER A WITH BREVE AND DOT BELOW]
          case '\u24B6': // �?  [CIRCLED LATIN CAPITAL LETTER A]
          case '\uFF21': // �?  [FULLWIDTH LATIN CAPITAL LETTER A]
            output[outputPos++] = 'A';
            break;
          case '\u00E0': // à  [LATIN SMALL LETTER A WITH GRAVE]
          case '\u00E1': // á  [LATIN SMALL LETTER A WITH ACUTE]
          case '\u00E2': // â  [LATIN SMALL LETTER A WITH CIRCUMFLEX]
          case '\u00E3': // ã  [LATIN SMALL LETTER A WITH TILDE]
          case '\u00E4': // ä  [LATIN SMALL LETTER A WITH DIAERESIS]
          case '\u00E5': // å  [LATIN SMALL LETTER A WITH RING ABOVE]
          case '\u0101': // ā  [LATIN SMALL LETTER A WITH MACRON]
          case '\u0103': // ă  [LATIN SMALL LETTER A WITH BREVE]
          case '\u0105': // ą  [LATIN SMALL LETTER A WITH OGONEK]
          case '\u01CE': // ǎ  [LATIN SMALL LETTER A WITH CARON]
          case '\u01DF': // ǟ  [LATIN SMALL LETTER A WITH DIAERESIS AND MACRON]
          case '\u01E1': // ǡ  [LATIN SMALL LETTER A WITH DOT ABOVE AND MACRON]
          case '\u01FB': // ǻ  [LATIN SMALL LETTER A WITH RING ABOVE AND ACUTE]
          case '\u0201': // ȁ  [LATIN SMALL LETTER A WITH DOUBLE GRAVE]
          case '\u0203': // ȃ  [LATIN SMALL LETTER A WITH INVERTED BREVE]
          case '\u0227': // ȧ  [LATIN SMALL LETTER A WITH DOT ABOVE]
          case '\u0250': // ɐ  [LATIN SMALL LETTER TURNED A]
          case '\u0259': // ə  [LATIN SMALL LETTER SCHWA]
          case '\u025A': // ɚ  [LATIN SMALL LETTER SCHWA WITH HOOK]
          case '\u1D8F': // �?  [LATIN SMALL LETTER A WITH RETROFLEX HOOK]
          case '\u1D95': // �?  [LATIN SMALL LETTER SCHWA WITH RETROFLEX HOOK]
          case '\u1E01': // �?  [LATIN SMALL LETTER A WITH RING BELOW]
          case '\u1E9A': // �?  [LATIN SMALL LETTER A WITH RIGHT HALF RING]
          case '\u1EA1': // �?  [LATIN SMALL LETTER A WITH DOT BELOW]
          case '\u1EA3': // �?  [LATIN SMALL LETTER A WITH HOOK ABOVE]
          case '\u1EA5': // �?  [LATIN SMALL LETTER A WITH CIRCUMFLEX AND ACUTE]
          case '\u1EA7': // �?  [LATIN SMALL LETTER A WITH CIRCUMFLEX AND GRAVE]
          case '\u1EA9': // �?  [LATIN SMALL LETTER A WITH CIRCUMFLEX AND HOOK ABOVE]
          case '\u1EAB': // �?  [LATIN SMALL LETTER A WITH CIRCUMFLEX AND TILDE]
          case '\u1EAD': // �?  [LATIN SMALL LETTER A WITH CIRCUMFLEX AND DOT BELOW]
          case '\u1EAF': // �?  [LATIN SMALL LETTER A WITH BREVE AND ACUTE]
          case '\u1EB1': // �?  [LATIN SMALL LETTER A WITH BREVE AND GRAVE]
          case '\u1EB3': // �?  [LATIN SMALL LETTER A WITH BREVE AND HOOK ABOVE]
          case '\u1EB5': // �?  [LATIN SMALL LETTER A WITH BREVE AND TILDE]
          case '\u1EB7': // �?  [LATIN SMALL LETTER A WITH BREVE AND DOT BELOW]
          case '\u2090': // �?  [LATIN SUBSCRIPT SMALL LETTER A]
          case '\u2094': // �?  [LATIN SUBSCRIPT SMALL LETTER SCHWA]
          case '\u24D0': // �?  [CIRCLED LATIN SMALL LETTER A]
          case '\u2C65': // �?  [LATIN SMALL LETTER A WITH STROKE]
          case '\u2C6F': // �?  [LATIN CAPITAL LETTER TURNED A]
          case '\uFF41': // �?  [FULLWIDTH LATIN SMALL LETTER A]
            output[outputPos++] = 'a';
            break;
          case '\uA732': // �?  [LATIN CAPITAL LETTER AA]
            output[outputPos++] = 'A';
            output[outputPos++] = 'A';
            break;
          case '\u00C6': // Æ  [LATIN CAPITAL LETTER AE]
          case '\u01E2': // Ǣ  [LATIN CAPITAL LETTER AE WITH MACRON]
          case '\u01FC': // Ǽ  [LATIN CAPITAL LETTER AE WITH ACUTE]
          case '\u1D01': // �?  [LATIN LETTER SMALL CAPITAL AE]
            output[outputPos++] = 'A';
            output[outputPos++] = 'E';
            break;
          case '\uA734': // �?  [LATIN CAPITAL LETTER AO]
            output[outputPos++] = 'A';                    
            output[outputPos++] = 'O';
            break;
          case '\uA736': // �?  [LATIN CAPITAL LETTER AU]
            output[outputPos++] = 'A';
            output[outputPos++] = 'U';
            break;
          case '\uA738': // �?  [LATIN CAPITAL LETTER AV]
          case '\uA73A': // �?  [LATIN CAPITAL LETTER AV WITH HORIZONTAL BAR]
            output[outputPos++] = 'A';
            output[outputPos++] = 'V';
            break;
          case '\uA73C': // �?  [LATIN CAPITAL LETTER AY]
            output[outputPos++] = 'A';
            output[outputPos++] = 'Y';
            break;
          case '\u249C': // �?  [PARENTHESIZED LATIN SMALL LETTER A]
            output[outputPos++] = '(';
            output[outputPos++] = 'a';
            output[outputPos++] = ')';
            break;
          case '\uA733': // �?  [LATIN SMALL LETTER AA]
            output[outputPos++] = 'a';
            output[outputPos++] = 'a';
            break;
          case '\u00E6': // æ  [LATIN SMALL LETTER AE]
          case '\u01E3': // ǣ  [LATIN SMALL LETTER AE WITH MACRON]
          case '\u01FD': // ǽ  [LATIN SMALL LETTER AE WITH ACUTE]
          case '\u1D02': // �?  [LATIN SMALL LETTER TURNED AE]
            output[outputPos++] = 'a';
            output[outputPos++] = 'e';
            break;
          case '\uA735': // �?  [LATIN SMALL LETTER AO]
            output[outputPos++] = 'a';
            output[outputPos++] = 'o';
            break;
          case '\uA737': // �?  [LATIN SMALL LETTER AU]
            output[outputPos++] = 'a';
            output[outputPos++] = 'u';
            break;
          case '\uA739': // �?  [LATIN SMALL LETTER AV]
          case '\uA73B': // �?  [LATIN SMALL LETTER AV WITH HORIZONTAL BAR]
            output[outputPos++] = 'a';
            output[outputPos++] = 'v';
            break;
          case '\uA73D': // �?  [LATIN SMALL LETTER AY]
            output[outputPos++] = 'a';
            output[outputPos++] = 'y';
            break;
          case '\u0181': // Ɓ  [LATIN CAPITAL LETTER B WITH HOOK]
          case '\u0182': // Ƃ  [LATIN CAPITAL LETTER B WITH TOPBAR]
          case '\u0243': // Ƀ  [LATIN CAPITAL LETTER B WITH STROKE]
          case '\u0299': // ʙ  [LATIN LETTER SMALL CAPITAL B]
          case '\u1D03': // �?  [LATIN LETTER SMALL CAPITAL BARRED B]
          case '\u1E02': // �?  [LATIN CAPITAL LETTER B WITH DOT ABOVE]
          case '\u1E04': // �?  [LATIN CAPITAL LETTER B WITH DOT BELOW]
          case '\u1E06': // �?  [LATIN CAPITAL LETTER B WITH LINE BELOW]
          case '\u24B7': // �?  [CIRCLED LATIN CAPITAL LETTER B]
          case '\uFF22': // �?  [FULLWIDTH LATIN CAPITAL LETTER B]
            output[outputPos++] = 'B';
            break;
          case '\u0180': // ƀ  [LATIN SMALL LETTER B WITH STROKE]
          case '\u0183': // ƃ  [LATIN SMALL LETTER B WITH TOPBAR]
          case '\u0253': // ɓ  [LATIN SMALL LETTER B WITH HOOK]
          case '\u1D6C': // �?  [LATIN SMALL LETTER B WITH MIDDLE TILDE]
          case '\u1D80': // �?  [LATIN SMALL LETTER B WITH PALATAL HOOK]
          case '\u1E03': // �?  [LATIN SMALL LETTER B WITH DOT ABOVE]
          case '\u1E05': // �?  [LATIN SMALL LETTER B WITH DOT BELOW]
          case '\u1E07': // �?  [LATIN SMALL LETTER B WITH LINE BELOW]
          case '\u24D1': // �?  [CIRCLED LATIN SMALL LETTER B]
          case '\uFF42': // �?  [FULLWIDTH LATIN SMALL LETTER B]
            output[outputPos++] = 'b';
            break;
          case '\u249D': // �?  [PARENTHESIZED LATIN SMALL LETTER B]
            output[outputPos++] = '(';                    
            output[outputPos++] = 'b';
            output[outputPos++] = ')';
            break;
          case '\u00C7': // Ç  [LATIN CAPITAL LETTER C WITH CEDILLA]
          case '\u0106': // Ć  [LATIN CAPITAL LETTER C WITH ACUTE]
          case '\u0108': // Ĉ  [LATIN CAPITAL LETTER C WITH CIRCUMFLEX]
          case '\u010A': // Ċ  [LATIN CAPITAL LETTER C WITH DOT ABOVE]
          case '\u010C': // Č  [LATIN CAPITAL LETTER C WITH CARON]
          case '\u0187': // Ƈ  [LATIN CAPITAL LETTER C WITH HOOK]
          case '\u023B': // Ȼ  [LATIN CAPITAL LETTER C WITH STROKE]
          case '\u0297': // ʗ  [LATIN LETTER STRETCHED C]
          case '\u1D04': // �?  [LATIN LETTER SMALL CAPITAL C]
          case '\u1E08': // �?  [LATIN CAPITAL LETTER C WITH CEDILLA AND ACUTE]
          case '\u24B8': // �?  [CIRCLED LATIN CAPITAL LETTER C]
          case '\uFF23': // �?  [FULLWIDTH LATIN CAPITAL LETTER C]
            output[outputPos++] = 'C';
            break;
          case '\u00E7': // ç  [LATIN SMALL LETTER C WITH CEDILLA]
          case '\u0107': // ć  [LATIN SMALL LETTER C WITH ACUTE]
          case '\u0109': // ĉ  [LATIN SMALL LETTER C WITH CIRCUMFLEX]
          case '\u010B': // ċ  [LATIN SMALL LETTER C WITH DOT ABOVE]
          case '\u010D': // č  [LATIN SMALL LETTER C WITH CARON]
          case '\u0188': // ƈ  [LATIN SMALL LETTER C WITH HOOK]
          case '\u023C': // ȼ  [LATIN SMALL LETTER C WITH STROKE]
          case '\u0255': // ɕ  [LATIN SMALL LETTER C WITH CURL]
          case '\u1E09': // �?  [LATIN SMALL LETTER C WITH CEDILLA AND ACUTE]
          case '\u2184': // �?  [LATIN SMALL LETTER REVERSED C]
          case '\u24D2': // �?  [CIRCLED LATIN SMALL LETTER C]
          case '\uA73E': // �?  [LATIN CAPITAL LETTER REVERSED C WITH DOT]
          case '\uA73F': // �?  [LATIN SMALL LETTER REVERSED C WITH DOT]
          case '\uFF43': // �?  [FULLWIDTH LATIN SMALL LETTER C]
            output[outputPos++] = 'c';
            break;
          case '\u249E': // �?  [PARENTHESIZED LATIN SMALL LETTER C]
            output[outputPos++] = '(';
            output[outputPos++] = 'c';
            output[outputPos++] = ')';
            break;
          case '\u00D0': // Ð  [LATIN CAPITAL LETTER ETH]
          case '\u010E': // Ď  [LATIN CAPITAL LETTER D WITH CARON]
          case '\u0110': // Đ  [LATIN CAPITAL LETTER D WITH STROKE]
          case '\u0189': // Ɖ  [LATIN CAPITAL LETTER AFRICAN D]
          case '\u018A': // Ɗ  [LATIN CAPITAL LETTER D WITH HOOK]
          case '\u018B': // Ƌ  [LATIN CAPITAL LETTER D WITH TOPBAR]
          case '\u1D05': // �?  [LATIN LETTER SMALL CAPITAL D]
          case '\u1D06': // �?  [LATIN LETTER SMALL CAPITAL ETH]
          case '\u1E0A': // �?  [LATIN CAPITAL LETTER D WITH DOT ABOVE]
          case '\u1E0C': // �?  [LATIN CAPITAL LETTER D WITH DOT BELOW]
          case '\u1E0E': // �?  [LATIN CAPITAL LETTER D WITH LINE BELOW]
          case '\u1E10': // �?  [LATIN CAPITAL LETTER D WITH CEDILLA]
          case '\u1E12': // �?  [LATIN CAPITAL LETTER D WITH CIRCUMFLEX BELOW]
          case '\u24B9': // �?  [CIRCLED LATIN CAPITAL LETTER D]
          case '\uA779': // �?  [LATIN CAPITAL LETTER INSULAR D]
          case '\uFF24': // �?  [FULLWIDTH LATIN CAPITAL LETTER D]
            output[outputPos++] = 'D';
            break;
          case '\u00F0': // ð  [LATIN SMALL LETTER ETH]
          case '\u010F': // ď  [LATIN SMALL LETTER D WITH CARON]
          case '\u0111': // đ  [LATIN SMALL LETTER D WITH STROKE]
          case '\u018C': // ƌ  [LATIN SMALL LETTER D WITH TOPBAR]
          case '\u0221': // ȡ  [LATIN SMALL LETTER D WITH CURL]
          case '\u0256': // ɖ  [LATIN SMALL LETTER D WITH TAIL]
          case '\u0257': // ɗ  [LATIN SMALL LETTER D WITH HOOK]
          case '\u1D6D': // �?  [LATIN SMALL LETTER D WITH MIDDLE TILDE]
          case '\u1D81': // �?  [LATIN SMALL LETTER D WITH PALATAL HOOK]
          case '\u1D91': // �?  [LATIN SMALL LETTER D WITH HOOK AND TAIL]
          case '\u1E0B': // �?  [LATIN SMALL LETTER D WITH DOT ABOVE]
          case '\u1E0D': // �?  [LATIN SMALL LETTER D WITH DOT BELOW]
          case '\u1E0F': // �?  [LATIN SMALL LETTER D WITH LINE BELOW]
          case '\u1E11': // �?  [LATIN SMALL LETTER D WITH CEDILLA]
          case '\u1E13': // �?  [LATIN SMALL LETTER D WITH CIRCUMFLEX BELOW]
          case '\u24D3': // �?  [CIRCLED LATIN SMALL LETTER D]
          case '\uA77A': // �?  [LATIN SMALL LETTER INSULAR D]
          case '\uFF44': // �?  [FULLWIDTH LATIN SMALL LETTER D]
            output[outputPos++] = 'd';
            break;
          case '\u01C4': // Ǆ  [LATIN CAPITAL LETTER DZ WITH CARON]
          case '\u01F1': // Ǳ  [LATIN CAPITAL LETTER DZ]
            output[outputPos++] = 'D';
            output[outputPos++] = 'Z';
            break;
          case '\u01C5': // ǅ  [LATIN CAPITAL LETTER D WITH SMALL LETTER Z WITH CARON]
          case '\u01F2': // ǲ  [LATIN CAPITAL LETTER D WITH SMALL LETTER Z]
            output[outputPos++] = 'D';
            output[outputPos++] = 'z';
            break;
          case '\u249F': // �?  [PARENTHESIZED LATIN SMALL LETTER D]
            output[outputPos++] = '(';
            output[outputPos++] = 'd';
            output[outputPos++] = ')';
            break;
          case '\u0238': // ȸ  [LATIN SMALL LETTER DB DIGRAPH]
            output[outputPos++] = 'd';
            output[outputPos++] = 'b';
            break;
          case '\u01C6': // ǆ  [LATIN SMALL LETTER DZ WITH CARON]
          case '\u01F3': // ǳ  [LATIN SMALL LETTER DZ]
          case '\u02A3': // ʣ  [LATIN SMALL LETTER DZ DIGRAPH]
          case '\u02A5': // ʥ  [LATIN SMALL LETTER DZ DIGRAPH WITH CURL]
            output[outputPos++] = 'd';
            output[outputPos++] = 'z';
            break;
          case '\u00C8': // È  [LATIN CAPITAL LETTER E WITH GRAVE]
          case '\u00C9': // É  [LATIN CAPITAL LETTER E WITH ACUTE]
          case '\u00CA': // Ê  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX]
          case '\u00CB': // Ë  [LATIN CAPITAL LETTER E WITH DIAERESIS]
          case '\u0112': // Ē  [LATIN CAPITAL LETTER E WITH MACRON]
          case '\u0114': // Ĕ  [LATIN CAPITAL LETTER E WITH BREVE]
          case '\u0116': // Ė  [LATIN CAPITAL LETTER E WITH DOT ABOVE]
          case '\u0118': // Ę  [LATIN CAPITAL LETTER E WITH OGONEK]
          case '\u011A': // Ě  [LATIN CAPITAL LETTER E WITH CARON]
          case '\u018E': // Ǝ  [LATIN CAPITAL LETTER REVERSED E]
          case '\u0190': // Ɛ  [LATIN CAPITAL LETTER OPEN E]
          case '\u0204': // Ȅ  [LATIN CAPITAL LETTER E WITH DOUBLE GRAVE]
          case '\u0206': // Ȇ  [LATIN CAPITAL LETTER E WITH INVERTED BREVE]
          case '\u0228': // Ȩ  [LATIN CAPITAL LETTER E WITH CEDILLA]
          case '\u0246': // Ɇ  [LATIN CAPITAL LETTER E WITH STROKE]
          case '\u1D07': // �?  [LATIN LETTER SMALL CAPITAL E]
          case '\u1E14': // �?  [LATIN CAPITAL LETTER E WITH MACRON AND GRAVE]
          case '\u1E16': // �?  [LATIN CAPITAL LETTER E WITH MACRON AND ACUTE]
          case '\u1E18': // �?  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX BELOW]
          case '\u1E1A': // �?  [LATIN CAPITAL LETTER E WITH TILDE BELOW]
          case '\u1E1C': // �?  [LATIN CAPITAL LETTER E WITH CEDILLA AND BREVE]
          case '\u1EB8': // �?  [LATIN CAPITAL LETTER E WITH DOT BELOW]
          case '\u1EBA': // �?  [LATIN CAPITAL LETTER E WITH HOOK ABOVE]
          case '\u1EBC': // �?  [LATIN CAPITAL LETTER E WITH TILDE]
          case '\u1EBE': // �?  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX AND ACUTE]
          case '\u1EC0': // �?  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX AND GRAVE]
          case '\u1EC2': // �?  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX AND HOOK ABOVE]
          case '\u1EC4': // �?  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX AND TILDE]
          case '\u1EC6': // �?  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX AND DOT BELOW]
          case '\u24BA': // �?  [CIRCLED LATIN CAPITAL LETTER E]
          case '\u2C7B': // �?  [LATIN LETTER SMALL CAPITAL TURNED E]
          case '\uFF25': // �?  [FULLWIDTH LATIN CAPITAL LETTER E]
            output[outputPos++] = 'E';
            break;
          case '\u00E8': // è  [LATIN SMALL LETTER E WITH GRAVE]
          case '\u00E9': // é  [LATIN SMALL LETTER E WITH ACUTE]
          case '\u00EA': // ê  [LATIN SMALL LETTER E WITH CIRCUMFLEX]
          case '\u00EB': // ë  [LATIN SMALL LETTER E WITH DIAERESIS]
          case '\u0113': // ē  [LATIN SMALL LETTER E WITH MACRON]
          case '\u0115': // ĕ  [LATIN SMALL LETTER E WITH BREVE]
          case '\u0117': // ė  [LATIN SMALL LETTER E WITH DOT ABOVE]
          case '\u0119': // ę  [LATIN SMALL LETTER E WITH OGONEK]
          case '\u011B': // ě  [LATIN SMALL LETTER E WITH CARON]
          case '\u01DD': // ǝ  [LATIN SMALL LETTER TURNED E]
          case '\u0205': // ȅ  [LATIN SMALL LETTER E WITH DOUBLE GRAVE]
          case '\u0207': // ȇ  [LATIN SMALL LETTER E WITH INVERTED BREVE]
          case '\u0229': // ȩ  [LATIN SMALL LETTER E WITH CEDILLA]
          case '\u0247': // ɇ  [LATIN SMALL LETTER E WITH STROKE]
          case '\u0258': // ɘ  [LATIN SMALL LETTER REVERSED E]
          case '\u025B': // ɛ  [LATIN SMALL LETTER OPEN E]
          case '\u025C': // ɜ  [LATIN SMALL LETTER REVERSED OPEN E]
          case '\u025D': // ɝ  [LATIN SMALL LETTER REVERSED OPEN E WITH HOOK]
          case '\u025E': // ɞ  [LATIN SMALL LETTER CLOSED REVERSED OPEN E]
          case '\u029A': // ʚ  [LATIN SMALL LETTER CLOSED OPEN E]
          case '\u1D08': // �?  [LATIN SMALL LETTER TURNED OPEN E]
          case '\u1D92': // �?  [LATIN SMALL LETTER E WITH RETROFLEX HOOK]
          case '\u1D93': // �?  [LATIN SMALL LETTER OPEN E WITH RETROFLEX HOOK]
          case '\u1D94': // �?  [LATIN SMALL LETTER REVERSED OPEN E WITH RETROFLEX HOOK]
          case '\u1E15': // �?  [LATIN SMALL LETTER E WITH MACRON AND GRAVE]
          case '\u1E17': // �?  [LATIN SMALL LETTER E WITH MACRON AND ACUTE]
          case '\u1E19': // �?  [LATIN SMALL LETTER E WITH CIRCUMFLEX BELOW]
          case '\u1E1B': // �?  [LATIN SMALL LETTER E WITH TILDE BELOW]
          case '\u1E1D': // �?  [LATIN SMALL LETTER E WITH CEDILLA AND BREVE]
          case '\u1EB9': // �?  [LATIN SMALL LETTER E WITH DOT BELOW]
          case '\u1EBB': // �?  [LATIN SMALL LETTER E WITH HOOK ABOVE]
          case '\u1EBD': // �?  [LATIN SMALL LETTER E WITH TILDE]
          case '\u1EBF': // �?  [LATIN SMALL LETTER E WITH CIRCUMFLEX AND ACUTE]
          case '\u1EC1': // �?  [LATIN SMALL LETTER E WITH CIRCUMFLEX AND GRAVE]
          case '\u1EC3': // �?  [LATIN SMALL LETTER E WITH CIRCUMFLEX AND HOOK ABOVE]
          case '\u1EC5': // �?  [LATIN SMALL LETTER E WITH CIRCUMFLEX AND TILDE]
          case '\u1EC7': // �?  [LATIN SMALL LETTER E WITH CIRCUMFLEX AND DOT BELOW]
          case '\u2091': // �?  [LATIN SUBSCRIPT SMALL LETTER E]
          case '\u24D4': // �?  [CIRCLED LATIN SMALL LETTER E]
          case '\u2C78': // �?  [LATIN SMALL LETTER E WITH NOTCH]
          case '\uFF45': // �?  [FULLWIDTH LATIN SMALL LETTER E]
            output[outputPos++] = 'e';
            break;
          case '\u24A0': // �?  [PARENTHESIZED LATIN SMALL LETTER E]
            output[outputPos++] = '(';
            output[outputPos++] = 'e';
            output[outputPos++] = ')';
            break;
          case '\u0191': // Ƒ  [LATIN CAPITAL LETTER F WITH HOOK]
          case '\u1E1E': // �?  [LATIN CAPITAL LETTER F WITH DOT ABOVE]
          case '\u24BB': // �?  [CIRCLED LATIN CAPITAL LETTER F]
          case '\uA730': // �?  [LATIN LETTER SMALL CAPITAL F]
          case '\uA77B': // �?  [LATIN CAPITAL LETTER INSULAR F]
          case '\uA7FB': // �?  [LATIN EPIGRAPHIC LETTER REVERSED F]
          case '\uFF26': // �?  [FULLWIDTH LATIN CAPITAL LETTER F]
            output[outputPos++] = 'F';
            break;
          case '\u0192': // ƒ  [LATIN SMALL LETTER F WITH HOOK]
          case '\u1D6E': // �?  [LATIN SMALL LETTER F WITH MIDDLE TILDE]
          case '\u1D82': // �?  [LATIN SMALL LETTER F WITH PALATAL HOOK]
          case '\u1E1F': // �?  [LATIN SMALL LETTER F WITH DOT ABOVE]
          case '\u1E9B': // �?  [LATIN SMALL LETTER LONG S WITH DOT ABOVE]
          case '\u24D5': // �?  [CIRCLED LATIN SMALL LETTER F]
          case '\uA77C': // �?  [LATIN SMALL LETTER INSULAR F]
          case '\uFF46': // �?  [FULLWIDTH LATIN SMALL LETTER F]
            output[outputPos++] = 'f';
            break;
          case '\u24A1': // �?  [PARENTHESIZED LATIN SMALL LETTER F]
            output[outputPos++] = '(';
            output[outputPos++] = 'f';
            output[outputPos++] = ')';
            break;
          case '\uFB00': // �?  [LATIN SMALL LIGATURE FF]
            output[outputPos++] = 'f';
            output[outputPos++] = 'f';
            break;
          case '\uFB03': // �?  [LATIN SMALL LIGATURE FFI]
            output[outputPos++] = 'f';
            output[outputPos++] = 'f';
            output[outputPos++] = 'i';
            break;
          case '\uFB04': // �?  [LATIN SMALL LIGATURE FFL]
            output[outputPos++] = 'f';
            output[outputPos++] = 'f';
            output[outputPos++] = 'l';
            break;
          case '\uFB01': // �?  [LATIN SMALL LIGATURE FI]
            output[outputPos++] = 'f';
            output[outputPos++] = 'i';
            break;
          case '\uFB02': // �?  [LATIN SMALL LIGATURE FL]
            output[outputPos++] = 'f';
            output[outputPos++] = 'l';
            break;
          case '\u011C': // Ĝ  [LATIN CAPITAL LETTER G WITH CIRCUMFLEX]
          case '\u011E': // Ğ  [LATIN CAPITAL LETTER G WITH BREVE]
          case '\u0120': // Ġ  [LATIN CAPITAL LETTER G WITH DOT ABOVE]
          case '\u0122': // Ģ  [LATIN CAPITAL LETTER G WITH CEDILLA]
          case '\u0193': // Ɠ  [LATIN CAPITAL LETTER G WITH HOOK]
          case '\u01E4': // Ǥ  [LATIN CAPITAL LETTER G WITH STROKE]
          case '\u01E5': // ǥ  [LATIN SMALL LETTER G WITH STROKE]
          case '\u01E6': // Ǧ  [LATIN CAPITAL LETTER G WITH CARON]
          case '\u01E7': // ǧ  [LATIN SMALL LETTER G WITH CARON]
          case '\u01F4': // Ǵ  [LATIN CAPITAL LETTER G WITH ACUTE]
          case '\u0262': // ɢ  [LATIN LETTER SMALL CAPITAL G]
          case '\u029B': // ʛ  [LATIN LETTER SMALL CAPITAL G WITH HOOK]
          case '\u1E20': // �?  [LATIN CAPITAL LETTER G WITH MACRON]
          case '\u24BC': // �?  [CIRCLED LATIN CAPITAL LETTER G]
          case '\uA77D': // �?  [LATIN CAPITAL LETTER INSULAR G]
          case '\uA77E': // �?  [LATIN CAPITAL LETTER TURNED INSULAR G]
          case '\uFF27': // �?  [FULLWIDTH LATIN CAPITAL LETTER G]
            output[outputPos++] = 'G';
            break;
          case '\u011D': // ĝ  [LATIN SMALL LETTER G WITH CIRCUMFLEX]
          case '\u011F': // ğ  [LATIN SMALL LETTER G WITH BREVE]
          case '\u0121': // ġ  [LATIN SMALL LETTER G WITH DOT ABOVE]
          case '\u0123': // ģ  [LATIN SMALL LETTER G WITH CEDILLA]
          case '\u01F5': // ǵ  [LATIN SMALL LETTER G WITH ACUTE]
          case '\u0260': // ɠ  [LATIN SMALL LETTER G WITH HOOK]
          case '\u0261': // ɡ  [LATIN SMALL LETTER SCRIPT G]
          case '\u1D77': // �?  [LATIN SMALL LETTER TURNED G]
          case '\u1D79': // �?  [LATIN SMALL LETTER INSULAR G]
          case '\u1D83': // �?  [LATIN SMALL LETTER G WITH PALATAL HOOK]
          case '\u1E21': // �?  [LATIN SMALL LETTER G WITH MACRON]
          case '\u24D6': // �?  [CIRCLED LATIN SMALL LETTER G]
          case '\uA77F': // �?  [LATIN SMALL LETTER TURNED INSULAR G]
          case '\uFF47': // �?  [FULLWIDTH LATIN SMALL LETTER G]
            output[outputPos++] = 'g';
            break;
          case '\u24A2': // �?  [PARENTHESIZED LATIN SMALL LETTER G]
            output[outputPos++] = '(';
            output[outputPos++] = 'g';
            output[outputPos++] = ')';
            break;
          case '\u0124': // Ĥ  [LATIN CAPITAL LETTER H WITH CIRCUMFLEX]
          case '\u0126': // Ħ  [LATIN CAPITAL LETTER H WITH STROKE]
          case '\u021E': // Ȟ  [LATIN CAPITAL LETTER H WITH CARON]
          case '\u029C': // ʜ  [LATIN LETTER SMALL CAPITAL H]
          case '\u1E22': // �?  [LATIN CAPITAL LETTER H WITH DOT ABOVE]
          case '\u1E24': // �?  [LATIN CAPITAL LETTER H WITH DOT BELOW]
          case '\u1E26': // �?  [LATIN CAPITAL LETTER H WITH DIAERESIS]
          case '\u1E28': // �?  [LATIN CAPITAL LETTER H WITH CEDILLA]
          case '\u1E2A': // �?  [LATIN CAPITAL LETTER H WITH BREVE BELOW]
          case '\u24BD': // �?  [CIRCLED LATIN CAPITAL LETTER H]
          case '\u2C67': // �?  [LATIN CAPITAL LETTER H WITH DESCENDER]
          case '\u2C75': // �?  [LATIN CAPITAL LETTER HALF H]
          case '\uFF28': // �?  [FULLWIDTH LATIN CAPITAL LETTER H]
            output[outputPos++] = 'H';
            break;
          case '\u0125': // ĥ  [LATIN SMALL LETTER H WITH CIRCUMFLEX]
          case '\u0127': // ħ  [LATIN SMALL LETTER H WITH STROKE]
          case '\u021F': // ȟ  [LATIN SMALL LETTER H WITH CARON]
          case '\u0265': // ɥ  [LATIN SMALL LETTER TURNED H]
          case '\u0266': // ɦ  [LATIN SMALL LETTER H WITH HOOK]
          case '\u02AE': // ʮ  [LATIN SMALL LETTER TURNED H WITH FISHHOOK]
          case '\u02AF': // ʯ  [LATIN SMALL LETTER TURNED H WITH FISHHOOK AND TAIL]
          case '\u1E23': // �?  [LATIN SMALL LETTER H WITH DOT ABOVE]
          case '\u1E25': // �?  [LATIN SMALL LETTER H WITH DOT BELOW]
          case '\u1E27': // �?  [LATIN SMALL LETTER H WITH DIAERESIS]
          case '\u1E29': // �?  [LATIN SMALL LETTER H WITH CEDILLA]
          case '\u1E2B': // �?  [LATIN SMALL LETTER H WITH BREVE BELOW]
          case '\u1E96': // �?  [LATIN SMALL LETTER H WITH LINE BELOW]
          case '\u24D7': // �?  [CIRCLED LATIN SMALL LETTER H]
          case '\u2C68': // �?  [LATIN SMALL LETTER H WITH DESCENDER]
          case '\u2C76': // �?  [LATIN SMALL LETTER HALF H]
          case '\uFF48': // �?  [FULLWIDTH LATIN SMALL LETTER H]
            output[outputPos++] = 'h';
            break;
          case '\u01F6': // Ƕ  http://en.wikipedia.org/wiki/Hwair  [LATIN CAPITAL LETTER HWAIR]
            output[outputPos++] = 'H';
            output[outputPos++] = 'V';
            break;
          case '\u24A3': // �?  [PARENTHESIZED LATIN SMALL LETTER H]
            output[outputPos++] = '(';
            output[outputPos++] = 'h';
            output[outputPos++] = ')';
            break;
          case '\u0195': // ƕ  [LATIN SMALL LETTER HV]
            output[outputPos++] = 'h';
            output[outputPos++] = 'v';
            break;
          case '\u00CC': // Ì  [LATIN CAPITAL LETTER I WITH GRAVE]
          case '\u00CD': // Í  [LATIN CAPITAL LETTER I WITH ACUTE]
          case '\u00CE': // Î  [LATIN CAPITAL LETTER I WITH CIRCUMFLEX]
          case '\u00CF': // Ï  [LATIN CAPITAL LETTER I WITH DIAERESIS]
          case '\u0128': // Ĩ  [LATIN CAPITAL LETTER I WITH TILDE]
          case '\u012A': // Ī  [LATIN CAPITAL LETTER I WITH MACRON]
          case '\u012C': // Ĭ  [LATIN CAPITAL LETTER I WITH BREVE]
          case '\u012E': // Į  [LATIN CAPITAL LETTER I WITH OGONEK]
          case '\u0130': // İ  [LATIN CAPITAL LETTER I WITH DOT ABOVE]
          case '\u0196': // Ɩ  [LATIN CAPITAL LETTER IOTA]
          case '\u0197': // Ɨ  [LATIN CAPITAL LETTER I WITH STROKE]
          case '\u01CF': // Ǐ  [LATIN CAPITAL LETTER I WITH CARON]
          case '\u0208': // Ȉ  [LATIN CAPITAL LETTER I WITH DOUBLE GRAVE]
          case '\u020A': // Ȋ  [LATIN CAPITAL LETTER I WITH INVERTED BREVE]
          case '\u026A': // ɪ  [LATIN LETTER SMALL CAPITAL I]
          case '\u1D7B': // �?  [LATIN SMALL CAPITAL LETTER I WITH STROKE]
          case '\u1E2C': // �?  [LATIN CAPITAL LETTER I WITH TILDE BELOW]
          case '\u1E2E': // �?  [LATIN CAPITAL LETTER I WITH DIAERESIS AND ACUTE]
          case '\u1EC8': // �?  [LATIN CAPITAL LETTER I WITH HOOK ABOVE]
          case '\u1ECA': // �?  [LATIN CAPITAL LETTER I WITH DOT BELOW]
          case '\u24BE': // �?  [CIRCLED LATIN CAPITAL LETTER I]
          case '\uA7FE': // �?  [LATIN EPIGRAPHIC LETTER I LONGA]
          case '\uFF29': // �?  [FULLWIDTH LATIN CAPITAL LETTER I]
            output[outputPos++] = 'I';
            break;
          case '\u00EC': // ì  [LATIN SMALL LETTER I WITH GRAVE]
          case '\u00ED': // í  [LATIN SMALL LETTER I WITH ACUTE]
          case '\u00EE': // î  [LATIN SMALL LETTER I WITH CIRCUMFLEX]
          case '\u00EF': // ï  [LATIN SMALL LETTER I WITH DIAERESIS]
          case '\u0129': // ĩ  [LATIN SMALL LETTER I WITH TILDE]
          case '\u012B': // ī  [LATIN SMALL LETTER I WITH MACRON]
          case '\u012D': // ĭ  [LATIN SMALL LETTER I WITH BREVE]
          case '\u012F': // į  [LATIN SMALL LETTER I WITH OGONEK]
          case '\u0131': // ı  [LATIN SMALL LETTER DOTLESS I]
          case '\u01D0': // ǐ  [LATIN SMALL LETTER I WITH CARON]
          case '\u0209': // ȉ  [LATIN SMALL LETTER I WITH DOUBLE GRAVE]
          case '\u020B': // ȋ  [LATIN SMALL LETTER I WITH INVERTED BREVE]
          case '\u0268': // ɨ  [LATIN SMALL LETTER I WITH STROKE]
          case '\u1D09': // �?  [LATIN SMALL LETTER TURNED I]
          case '\u1D62': // �?  [LATIN SUBSCRIPT SMALL LETTER I]
          case '\u1D7C': // �?  [LATIN SMALL LETTER IOTA WITH STROKE]
          case '\u1D96': // �?  [LATIN SMALL LETTER I WITH RETROFLEX HOOK]
          case '\u1E2D': // �?  [LATIN SMALL LETTER I WITH TILDE BELOW]
          case '\u1E2F': // �?  [LATIN SMALL LETTER I WITH DIAERESIS AND ACUTE]
          case '\u1EC9': // �?  [LATIN SMALL LETTER I WITH HOOK ABOVE]
          case '\u1ECB': // �?  [LATIN SMALL LETTER I WITH DOT BELOW]
          case '\u2071': // �?  [SUPERSCRIPT LATIN SMALL LETTER I]
          case '\u24D8': // �?  [CIRCLED LATIN SMALL LETTER I]
          case '\uFF49': // �?  [FULLWIDTH LATIN SMALL LETTER I]
            output[outputPos++] = 'i';
            break;
          case '\u0132': // Ĳ  [LATIN CAPITAL LIGATURE IJ]
            output[outputPos++] = 'I';
            output[outputPos++] = 'J';
            break;
          case '\u24A4': // �?  [PARENTHESIZED LATIN SMALL LETTER I]
            output[outputPos++] = '(';
            output[outputPos++] = 'i';
            output[outputPos++] = ')';
            break;
          case '\u0133': // ĳ  [LATIN SMALL LIGATURE IJ]
            output[outputPos++] = 'i';
            output[outputPos++] = 'j';
            break;
          case '\u0134': // Ĵ  [LATIN CAPITAL LETTER J WITH CIRCUMFLEX]
          case '\u0248': // Ɉ  [LATIN CAPITAL LETTER J WITH STROKE]
          case '\u1D0A': // �?  [LATIN LETTER SMALL CAPITAL J]
          case '\u24BF': // �?  [CIRCLED LATIN CAPITAL LETTER J]
          case '\uFF2A': // �?  [FULLWIDTH LATIN CAPITAL LETTER J]
            output[outputPos++] = 'J';
            break;
          case '\u0135': // ĵ  [LATIN SMALL LETTER J WITH CIRCUMFLEX]
          case '\u01F0': // ǰ  [LATIN SMALL LETTER J WITH CARON]
          case '\u0237': // ȷ  [LATIN SMALL LETTER DOTLESS J]
          case '\u0249': // ɉ  [LATIN SMALL LETTER J WITH STROKE]
          case '\u025F': // ɟ  [LATIN SMALL LETTER DOTLESS J WITH STROKE]
          case '\u0284': // ʄ  [LATIN SMALL LETTER DOTLESS J WITH STROKE AND HOOK]
          case '\u029D': // ʝ  [LATIN SMALL LETTER J WITH CROSSED-TAIL]
          case '\u24D9': // �?  [CIRCLED LATIN SMALL LETTER J]
          case '\u2C7C': // �?  [LATIN SUBSCRIPT SMALL LETTER J]
          case '\uFF4A': // �?  [FULLWIDTH LATIN SMALL LETTER J]
            output[outputPos++] = 'j';
            break;
          case '\u24A5': // �?  [PARENTHESIZED LATIN SMALL LETTER J]
            output[outputPos++] = '(';
            output[outputPos++] = 'j';
            output[outputPos++] = ')';
            break;
          case '\u0136': // Ķ  [LATIN CAPITAL LETTER K WITH CEDILLA]
          case '\u0198': // Ƙ  [LATIN CAPITAL LETTER K WITH HOOK]
          case '\u01E8': // Ǩ  [LATIN CAPITAL LETTER K WITH CARON]
          case '\u1D0B': // �?  [LATIN LETTER SMALL CAPITAL K]
          case '\u1E30': // �?  [LATIN CAPITAL LETTER K WITH ACUTE]
          case '\u1E32': // �?  [LATIN CAPITAL LETTER K WITH DOT BELOW]
          case '\u1E34': // �?  [LATIN CAPITAL LETTER K WITH LINE BELOW]
          case '\u24C0': // �?  [CIRCLED LATIN CAPITAL LETTER K]
          case '\u2C69': // �?  [LATIN CAPITAL LETTER K WITH DESCENDER]
          case '\uA740': // �?  [LATIN CAPITAL LETTER K WITH STROKE]
          case '\uA742': // �?  [LATIN CAPITAL LETTER K WITH DIAGONAL STROKE]
          case '\uA744': // �?  [LATIN CAPITAL LETTER K WITH STROKE AND DIAGONAL STROKE]
          case '\uFF2B': // �?  [FULLWIDTH LATIN CAPITAL LETTER K]
            output[outputPos++] = 'K';
            break;
          case '\u0137': // ķ  [LATIN SMALL LETTER K WITH CEDILLA]
          case '\u0199': // ƙ  [LATIN SMALL LETTER K WITH HOOK]
          case '\u01E9': // ǩ  [LATIN SMALL LETTER K WITH CARON]
          case '\u029E': // ʞ  [LATIN SMALL LETTER TURNED K]
          case '\u1D84': // �?  [LATIN SMALL LETTER K WITH PALATAL HOOK]
          case '\u1E31': // �?  [LATIN SMALL LETTER K WITH ACUTE]
          case '\u1E33': // �?  [LATIN SMALL LETTER K WITH DOT BELOW]
          case '\u1E35': // �?  [LATIN SMALL LETTER K WITH LINE BELOW]
          case '\u24DA': // �?  [CIRCLED LATIN SMALL LETTER K]
          case '\u2C6A': // �?  [LATIN SMALL LETTER K WITH DESCENDER]
          case '\uA741': // �?  [LATIN SMALL LETTER K WITH STROKE]
          case '\uA743': // �?  [LATIN SMALL LETTER K WITH DIAGONAL STROKE]
          case '\uA745': // �?  [LATIN SMALL LETTER K WITH STROKE AND DIAGONAL STROKE]
          case '\uFF4B': // �?  [FULLWIDTH LATIN SMALL LETTER K]
            output[outputPos++] = 'k';
            break;
          case '\u24A6': // �?  [PARENTHESIZED LATIN SMALL LETTER K]
            output[outputPos++] = '(';
            output[outputPos++] = 'k';
            output[outputPos++] = ')';
            break;
          case '\u0139': // Ĺ  [LATIN CAPITAL LETTER L WITH ACUTE]
          case '\u013B': // Ļ  [LATIN CAPITAL LETTER L WITH CEDILLA]
          case '\u013D': // Ľ  [LATIN CAPITAL LETTER L WITH CARON]
          case '\u013F': // Ŀ  [LATIN CAPITAL LETTER L WITH MIDDLE DOT]
          case '\u0141': // Ł  [LATIN CAPITAL LETTER L WITH STROKE]
          case '\u023D': // Ƚ  [LATIN CAPITAL LETTER L WITH BAR]
          case '\u029F': // ʟ  [LATIN LETTER SMALL CAPITAL L]
          case '\u1D0C': // �?  [LATIN LETTER SMALL CAPITAL L WITH STROKE]
          case '\u1E36': // �?  [LATIN CAPITAL LETTER L WITH DOT BELOW]
          case '\u1E38': // �?  [LATIN CAPITAL LETTER L WITH DOT BELOW AND MACRON]
          case '\u1E3A': // �?  [LATIN CAPITAL LETTER L WITH LINE BELOW]
          case '\u1E3C': // �?  [LATIN CAPITAL LETTER L WITH CIRCUMFLEX BELOW]
          case '\u24C1': // �?  [CIRCLED LATIN CAPITAL LETTER L]
          case '\u2C60': // �?  [LATIN CAPITAL LETTER L WITH DOUBLE BAR]
          case '\u2C62': // �?  [LATIN CAPITAL LETTER L WITH MIDDLE TILDE]
          case '\uA746': // �?  [LATIN CAPITAL LETTER BROKEN L]
          case '\uA748': // �?  [LATIN CAPITAL LETTER L WITH HIGH STROKE]
          case '\uA780': // �?  [LATIN CAPITAL LETTER TURNED L]
          case '\uFF2C': // �?  [FULLWIDTH LATIN CAPITAL LETTER L]
            output[outputPos++] = 'L';
            break;
          case '\u013A': // ĺ  [LATIN SMALL LETTER L WITH ACUTE]
          case '\u013C': // ļ  [LATIN SMALL LETTER L WITH CEDILLA]
          case '\u013E': // ľ  [LATIN SMALL LETTER L WITH CARON]
          case '\u0140': // ŀ  [LATIN SMALL LETTER L WITH MIDDLE DOT]
          case '\u0142': // ł  [LATIN SMALL LETTER L WITH STROKE]
          case '\u019A': // ƚ  [LATIN SMALL LETTER L WITH BAR]
          case '\u0234': // ȴ  [LATIN SMALL LETTER L WITH CURL]
          case '\u026B': // ɫ  [LATIN SMALL LETTER L WITH MIDDLE TILDE]
          case '\u026C': // ɬ  [LATIN SMALL LETTER L WITH BELT]
          case '\u026D': // ɭ  [LATIN SMALL LETTER L WITH RETROFLEX HOOK]
          case '\u1D85': // �?  [LATIN SMALL LETTER L WITH PALATAL HOOK]
          case '\u1E37': // �?  [LATIN SMALL LETTER L WITH DOT BELOW]
          case '\u1E39': // �?  [LATIN SMALL LETTER L WITH DOT BELOW AND MACRON]
          case '\u1E3B': // �?  [LATIN SMALL LETTER L WITH LINE BELOW]
          case '\u1E3D': // �?  [LATIN SMALL LETTER L WITH CIRCUMFLEX BELOW]
          case '\u24DB': // �?  [CIRCLED LATIN SMALL LETTER L]
          case '\u2C61': // �?  [LATIN SMALL LETTER L WITH DOUBLE BAR]
          case '\uA747': // �?  [LATIN SMALL LETTER BROKEN L]
          case '\uA749': // �?  [LATIN SMALL LETTER L WITH HIGH STROKE]
          case '\uA781': // �?  [LATIN SMALL LETTER TURNED L]
          case '\uFF4C': // �?  [FULLWIDTH LATIN SMALL LETTER L]
            output[outputPos++] = 'l';
            break;
          case '\u01C7': // Ǉ  [LATIN CAPITAL LETTER LJ]
            output[outputPos++] = 'L';
            output[outputPos++] = 'J';
            break;
          case '\u1EFA': // �?  [LATIN CAPITAL LETTER MIDDLE-WELSH LL]
            output[outputPos++] = 'L';
            output[outputPos++] = 'L';
            break;
          case '\u01C8': // ǈ  [LATIN CAPITAL LETTER L WITH SMALL LETTER J]
            output[outputPos++] = 'L';
            output[outputPos++] = 'j';
            break;
          case '\u24A7': // �?  [PARENTHESIZED LATIN SMALL LETTER L]
            output[outputPos++] = '(';
            output[outputPos++] = 'l';
            output[outputPos++] = ')';
            break;
          case '\u01C9': // ǉ  [LATIN SMALL LETTER LJ]
            output[outputPos++] = 'l';
            output[outputPos++] = 'j';
            break;
          case '\u1EFB': // �?  [LATIN SMALL LETTER MIDDLE-WELSH LL]
            output[outputPos++] = 'l';
            output[outputPos++] = 'l';
            break;
          case '\u02AA': // ʪ  [LATIN SMALL LETTER LS DIGRAPH]
            output[outputPos++] = 'l';
            output[outputPos++] = 's';
            break;
          case '\u02AB': // ʫ  [LATIN SMALL LETTER LZ DIGRAPH]
            output[outputPos++] = 'l';
            output[outputPos++] = 'z';
            break;
          case '\u019C': // Ɯ  [LATIN CAPITAL LETTER TURNED M]
          case '\u1D0D': // �?  [LATIN LETTER SMALL CAPITAL M]
          case '\u1E3E': // �?  [LATIN CAPITAL LETTER M WITH ACUTE]
          case '\u1E40': // �?  [LATIN CAPITAL LETTER M WITH DOT ABOVE]
          case '\u1E42': // �?  [LATIN CAPITAL LETTER M WITH DOT BELOW]
          case '\u24C2': // �?  [CIRCLED LATIN CAPITAL LETTER M]
          case '\u2C6E': // �?  [LATIN CAPITAL LETTER M WITH HOOK]
          case '\uA7FD': // �?  [LATIN EPIGRAPHIC LETTER INVERTED M]
          case '\uA7FF': // �?  [LATIN EPIGRAPHIC LETTER ARCHAIC M]
          case '\uFF2D': // �?  [FULLWIDTH LATIN CAPITAL LETTER M]
            output[outputPos++] = 'M';
            break;
          case '\u026F': // ɯ  [LATIN SMALL LETTER TURNED M]
          case '\u0270': // ɰ  [LATIN SMALL LETTER TURNED M WITH LONG LEG]
          case '\u0271': // ɱ  [LATIN SMALL LETTER M WITH HOOK]
          case '\u1D6F': // �?  [LATIN SMALL LETTER M WITH MIDDLE TILDE]
          case '\u1D86': // �?  [LATIN SMALL LETTER M WITH PALATAL HOOK]
          case '\u1E3F': // �?  [LATIN SMALL LETTER M WITH ACUTE]
          case '\u1E41': // �?  [LATIN SMALL LETTER M WITH DOT ABOVE]
          case '\u1E43': // �?  [LATIN SMALL LETTER M WITH DOT BELOW]
          case '\u24DC': // �?  [CIRCLED LATIN SMALL LETTER M]
          case '\uFF4D': // �?  [FULLWIDTH LATIN SMALL LETTER M]
            output[outputPos++] = 'm';
            break;
          case '\u24A8': // �?  [PARENTHESIZED LATIN SMALL LETTER M]
            output[outputPos++] = '(';
            output[outputPos++] = 'm';
            output[outputPos++] = ')';
            break;
          case '\u00D1': // Ñ  [LATIN CAPITAL LETTER N WITH TILDE]
          case '\u0143': // Ń  [LATIN CAPITAL LETTER N WITH ACUTE]
          case '\u0145': // Ņ  [LATIN CAPITAL LETTER N WITH CEDILLA]
          case '\u0147': // Ň  [LATIN CAPITAL LETTER N WITH CARON]
          case '\u014A': // Ŋ  http://en.wikipedia.org/wiki/Eng_(letter)  [LATIN CAPITAL LETTER ENG]
          case '\u019D': // Ɲ  [LATIN CAPITAL LETTER N WITH LEFT HOOK]
          case '\u01F8': // Ǹ  [LATIN CAPITAL LETTER N WITH GRAVE]
          case '\u0220': // Ƞ  [LATIN CAPITAL LETTER N WITH LONG RIGHT LEG]
          case '\u0274': // ɴ  [LATIN LETTER SMALL CAPITAL N]
          case '\u1D0E': // �?  [LATIN LETTER SMALL CAPITAL REVERSED N]
          case '\u1E44': // �?  [LATIN CAPITAL LETTER N WITH DOT ABOVE]
          case '\u1E46': // �?  [LATIN CAPITAL LETTER N WITH DOT BELOW]
          case '\u1E48': // �?  [LATIN CAPITAL LETTER N WITH LINE BELOW]
          case '\u1E4A': // �?  [LATIN CAPITAL LETTER N WITH CIRCUMFLEX BELOW]
          case '\u24C3': // �?  [CIRCLED LATIN CAPITAL LETTER N]
          case '\uFF2E': // �?  [FULLWIDTH LATIN CAPITAL LETTER N]
            output[outputPos++] = 'N';
            break;
          case '\u00F1': // ñ  [LATIN SMALL LETTER N WITH TILDE]
          case '\u0144': // ń  [LATIN SMALL LETTER N WITH ACUTE]
          case '\u0146': // ņ  [LATIN SMALL LETTER N WITH CEDILLA]
          case '\u0148': // ň  [LATIN SMALL LETTER N WITH CARON]
          case '\u0149': // ŉ  [LATIN SMALL LETTER N PRECEDED BY APOSTROPHE]
          case '\u014B': // ŋ  http://en.wikipedia.org/wiki/Eng_(letter)  [LATIN SMALL LETTER ENG]
          case '\u019E': // ƞ  [LATIN SMALL LETTER N WITH LONG RIGHT LEG]
          case '\u01F9': // ǹ  [LATIN SMALL LETTER N WITH GRAVE]
          case '\u0235': // ȵ  [LATIN SMALL LETTER N WITH CURL]
          case '\u0272': // ɲ  [LATIN SMALL LETTER N WITH LEFT HOOK]
          case '\u0273': // ɳ  [LATIN SMALL LETTER N WITH RETROFLEX HOOK]
          case '\u1D70': // �?  [LATIN SMALL LETTER N WITH MIDDLE TILDE]
          case '\u1D87': // �?  [LATIN SMALL LETTER N WITH PALATAL HOOK]
          case '\u1E45': // �?  [LATIN SMALL LETTER N WITH DOT ABOVE]
          case '\u1E47': // �?  [LATIN SMALL LETTER N WITH DOT BELOW]
          case '\u1E49': // �?  [LATIN SMALL LETTER N WITH LINE BELOW]
          case '\u1E4B': // �?  [LATIN SMALL LETTER N WITH CIRCUMFLEX BELOW]
          case '\u207F': // �?  [SUPERSCRIPT LATIN SMALL LETTER N]
          case '\u24DD': // �?  [CIRCLED LATIN SMALL LETTER N]
          case '\uFF4E': // �?  [FULLWIDTH LATIN SMALL LETTER N]
            output[outputPos++] = 'n';
            break;
          case '\u01CA': // Ǌ  [LATIN CAPITAL LETTER NJ]
            output[outputPos++] = 'N';
            output[outputPos++] = 'J';
            break;
          case '\u01CB': // ǋ  [LATIN CAPITAL LETTER N WITH SMALL LETTER J]
            output[outputPos++] = 'N';
            output[outputPos++] = 'j';
            break;
          case '\u24A9': // �?  [PARENTHESIZED LATIN SMALL LETTER N]
            output[outputPos++] = '(';
            output[outputPos++] = 'n';
            output[outputPos++] = ')';
            break;
          case '\u01CC': // ǌ  [LATIN SMALL LETTER NJ]
            output[outputPos++] = 'n';
            output[outputPos++] = 'j';
            break;
          case '\u00D2': // Ò  [LATIN CAPITAL LETTER O WITH GRAVE]
          case '\u00D3': // Ó  [LATIN CAPITAL LETTER O WITH ACUTE]
          case '\u00D4': // Ô  [LATIN CAPITAL LETTER O WITH CIRCUMFLEX]
          case '\u00D5': // Õ  [LATIN CAPITAL LETTER O WITH TILDE]
          case '\u00D6': // Ö  [LATIN CAPITAL LETTER O WITH DIAERESIS]
          case '\u00D8': // Ø  [LATIN CAPITAL LETTER O WITH STROKE]
          case '\u014C': // Ō  [LATIN CAPITAL LETTER O WITH MACRON]
          case '\u014E': // Ŏ  [LATIN CAPITAL LETTER O WITH BREVE]
          case '\u0150': // Ő  [LATIN CAPITAL LETTER O WITH DOUBLE ACUTE]
          case '\u0186': // Ɔ  [LATIN CAPITAL LETTER OPEN O]
          case '\u019F': // Ɵ  [LATIN CAPITAL LETTER O WITH MIDDLE TILDE]
          case '\u01A0': // Ơ  [LATIN CAPITAL LETTER O WITH HORN]
          case '\u01D1': // Ǒ  [LATIN CAPITAL LETTER O WITH CARON]
          case '\u01EA': // Ǫ  [LATIN CAPITAL LETTER O WITH OGONEK]
          case '\u01EC': // Ǭ  [LATIN CAPITAL LETTER O WITH OGONEK AND MACRON]
          case '\u01FE': // Ǿ  [LATIN CAPITAL LETTER O WITH STROKE AND ACUTE]
          case '\u020C': // Ȍ  [LATIN CAPITAL LETTER O WITH DOUBLE GRAVE]
          case '\u020E': // Ȏ  [LATIN CAPITAL LETTER O WITH INVERTED BREVE]
          case '\u022A': // Ȫ  [LATIN CAPITAL LETTER O WITH DIAERESIS AND MACRON]
          case '\u022C': // Ȭ  [LATIN CAPITAL LETTER O WITH TILDE AND MACRON]
          case '\u022E': // Ȯ  [LATIN CAPITAL LETTER O WITH DOT ABOVE]
          case '\u0230': // Ȱ  [LATIN CAPITAL LETTER O WITH DOT ABOVE AND MACRON]
          case '\u1D0F': // �?  [LATIN LETTER SMALL CAPITAL O]
          case '\u1D10': // �?  [LATIN LETTER SMALL CAPITAL OPEN O]
          case '\u1E4C': // �?  [LATIN CAPITAL LETTER O WITH TILDE AND ACUTE]
          case '\u1E4E': // �?  [LATIN CAPITAL LETTER O WITH TILDE AND DIAERESIS]
          case '\u1E50': // �?  [LATIN CAPITAL LETTER O WITH MACRON AND GRAVE]
          case '\u1E52': // �?  [LATIN CAPITAL LETTER O WITH MACRON AND ACUTE]
          case '\u1ECC': // �?  [LATIN CAPITAL LETTER O WITH DOT BELOW]
          case '\u1ECE': // �?  [LATIN CAPITAL LETTER O WITH HOOK ABOVE]
          case '\u1ED0': // �?  [LATIN CAPITAL LETTER O WITH CIRCUMFLEX AND ACUTE]
          case '\u1ED2': // �?  [LATIN CAPITAL LETTER O WITH CIRCUMFLEX AND GRAVE]
          case '\u1ED4': // �?  [LATIN CAPITAL LETTER O WITH CIRCUMFLEX AND HOOK ABOVE]
          case '\u1ED6': // �?  [LATIN CAPITAL LETTER O WITH CIRCUMFLEX AND TILDE]
          case '\u1ED8': // �?  [LATIN CAPITAL LETTER O WITH CIRCUMFLEX AND DOT BELOW]
          case '\u1EDA': // �?  [LATIN CAPITAL LETTER O WITH HORN AND ACUTE]
          case '\u1EDC': // �?  [LATIN CAPITAL LETTER O WITH HORN AND GRAVE]
          case '\u1EDE': // �?  [LATIN CAPITAL LETTER O WITH HORN AND HOOK ABOVE]
          case '\u1EE0': // �?  [LATIN CAPITAL LETTER O WITH HORN AND TILDE]
          case '\u1EE2': // �?  [LATIN CAPITAL LETTER O WITH HORN AND DOT BELOW]
          case '\u24C4': // �?  [CIRCLED LATIN CAPITAL LETTER O]
          case '\uA74A': // �?  [LATIN CAPITAL LETTER O WITH LONG STROKE OVERLAY]
          case '\uA74C': // �?  [LATIN CAPITAL LETTER O WITH LOOP]
          case '\uFF2F': // �?  [FULLWIDTH LATIN CAPITAL LETTER O]
            output[outputPos++] = 'O';
            break;
          case '\u00F2': // ò  [LATIN SMALL LETTER O WITH GRAVE]
          case '\u00F3': // ó  [LATIN SMALL LETTER O WITH ACUTE]
          case '\u00F4': // ô  [LATIN SMALL LETTER O WITH CIRCUMFLEX]
          case '\u00F5': // õ  [LATIN SMALL LETTER O WITH TILDE]
          case '\u00F6': // ö  [LATIN SMALL LETTER O WITH DIAERESIS]
          case '\u00F8': // ø  [LATIN SMALL LETTER O WITH STROKE]
          case '\u014D': // ō  [LATIN SMALL LETTER O WITH MACRON]
          case '\u014F': // ŏ  [LATIN SMALL LETTER O WITH BREVE]
          case '\u0151': // ő  [LATIN SMALL LETTER O WITH DOUBLE ACUTE]
          case '\u01A1': // ơ  [LATIN SMALL LETTER O WITH HORN]
          case '\u01D2': // ǒ  [LATIN SMALL LETTER O WITH CARON]
          case '\u01EB': // ǫ  [LATIN SMALL LETTER O WITH OGONEK]
          case '\u01ED': // ǭ  [LATIN SMALL LETTER O WITH OGONEK AND MACRON]
          case '\u01FF': // ǿ  [LATIN SMALL LETTER O WITH STROKE AND ACUTE]
          case '\u020D': // ȍ  [LATIN SMALL LETTER O WITH DOUBLE GRAVE]
          case '\u020F': // ȏ  [LATIN SMALL LETTER O WITH INVERTED BREVE]
          case '\u022B': // ȫ  [LATIN SMALL LETTER O WITH DIAERESIS AND MACRON]
          case '\u022D': // ȭ  [LATIN SMALL LETTER O WITH TILDE AND MACRON]
          case '\u022F': // ȯ  [LATIN SMALL LETTER O WITH DOT ABOVE]
          case '\u0231': // ȱ  [LATIN SMALL LETTER O WITH DOT ABOVE AND MACRON]
          case '\u0254': // ɔ  [LATIN SMALL LETTER OPEN O]
          case '\u0275': // ɵ  [LATIN SMALL LETTER BARRED O]
          case '\u1D16': // �?  [LATIN SMALL LETTER TOP HALF O]
          case '\u1D17': // �?  [LATIN SMALL LETTER BOTTOM HALF O]
          case '\u1D97': // �?  [LATIN SMALL LETTER OPEN O WITH RETROFLEX HOOK]
          case '\u1E4D': // �?  [LATIN SMALL LETTER O WITH TILDE AND ACUTE]
          case '\u1E4F': // �?  [LATIN SMALL LETTER O WITH TILDE AND DIAERESIS]
          case '\u1E51': // �?  [LATIN SMALL LETTER O WITH MACRON AND GRAVE]
          case '\u1E53': // �?  [LATIN SMALL LETTER O WITH MACRON AND ACUTE]
          case '\u1ECD': // �?  [LATIN SMALL LETTER O WITH DOT BELOW]
          case '\u1ECF': // �?  [LATIN SMALL LETTER O WITH HOOK ABOVE]
          case '\u1ED1': // �?  [LATIN SMALL LETTER O WITH CIRCUMFLEX AND ACUTE]
          case '\u1ED3': // �?  [LATIN SMALL LETTER O WITH CIRCUMFLEX AND GRAVE]
          case '\u1ED5': // �?  [LATIN SMALL LETTER O WITH CIRCUMFLEX AND HOOK ABOVE]
          case '\u1ED7': // �?  [LATIN SMALL LETTER O WITH CIRCUMFLEX AND TILDE]
          case '\u1ED9': // �?  [LATIN SMALL LETTER O WITH CIRCUMFLEX AND DOT BELOW]
          case '\u1EDB': // �?  [LATIN SMALL LETTER O WITH HORN AND ACUTE]
          case '\u1EDD': // �?  [LATIN SMALL LETTER O WITH HORN AND GRAVE]
          case '\u1EDF': // �?  [LATIN SMALL LETTER O WITH HORN AND HOOK ABOVE]
          case '\u1EE1': // �?  [LATIN SMALL LETTER O WITH HORN AND TILDE]
          case '\u1EE3': // �?  [LATIN SMALL LETTER O WITH HORN AND DOT BELOW]
          case '\u2092': // �?  [LATIN SUBSCRIPT SMALL LETTER O]
          case '\u24DE': // �?  [CIRCLED LATIN SMALL LETTER O]
          case '\u2C7A': // �?  [LATIN SMALL LETTER O WITH LOW RING INSIDE]
          case '\uA74B': // �?  [LATIN SMALL LETTER O WITH LONG STROKE OVERLAY]
          case '\uA74D': // �?  [LATIN SMALL LETTER O WITH LOOP]
          case '\uFF4F': // �?  [FULLWIDTH LATIN SMALL LETTER O]
            output[outputPos++] = 'o';
            break;
          case '\u0152': // Œ  [LATIN CAPITAL LIGATURE OE]
          case '\u0276': // ɶ  [LATIN LETTER SMALL CAPITAL OE]
            output[outputPos++] = 'O';
            output[outputPos++] = 'E';
            break;
          case '\uA74E': // �?  [LATIN CAPITAL LETTER OO]
            output[outputPos++] = 'O';
            output[outputPos++] = 'O';
            break;
          case '\u0222': // Ȣ  http://en.wikipedia.org/wiki/OU  [LATIN CAPITAL LETTER OU]
          case '\u1D15': // �?  [LATIN LETTER SMALL CAPITAL OU]
            output[outputPos++] = 'O';
            output[outputPos++] = 'U';
            break;
          case '\u24AA': // �?  [PARENTHESIZED LATIN SMALL LETTER O]
            output[outputPos++] = '(';
            output[outputPos++] = 'o';
            output[outputPos++] = ')';
            break;
          case '\u0153': // œ  [LATIN SMALL LIGATURE OE]
          case '\u1D14': // �?  [LATIN SMALL LETTER TURNED OE]
            output[outputPos++] = 'o';
            output[outputPos++] = 'e';
            break;
          case '\uA74F': // �?  [LATIN SMALL LETTER OO]
            output[outputPos++] = 'o';
            output[outputPos++] = 'o';
            break;
          case '\u0223': // ȣ  http://en.wikipedia.org/wiki/OU  [LATIN SMALL LETTER OU]
            output[outputPos++] = 'o';
            output[outputPos++] = 'u';
            break;
          case '\u01A4': // Ƥ  [LATIN CAPITAL LETTER P WITH HOOK]
          case '\u1D18': // �?  [LATIN LETTER SMALL CAPITAL P]
          case '\u1E54': // �?  [LATIN CAPITAL LETTER P WITH ACUTE]
          case '\u1E56': // �?  [LATIN CAPITAL LETTER P WITH DOT ABOVE]
          case '\u24C5': // �?  [CIRCLED LATIN CAPITAL LETTER P]
          case '\u2C63': // �?  [LATIN CAPITAL LETTER P WITH STROKE]
          case '\uA750': // �?  [LATIN CAPITAL LETTER P WITH STROKE THROUGH DESCENDER]
          case '\uA752': // �?  [LATIN CAPITAL LETTER P WITH FLOURISH]
          case '\uA754': // �?  [LATIN CAPITAL LETTER P WITH SQUIRREL TAIL]
          case '\uFF30': // �?  [FULLWIDTH LATIN CAPITAL LETTER P]
            output[outputPos++] = 'P';
            break;
          case '\u01A5': // ƥ  [LATIN SMALL LETTER P WITH HOOK]
          case '\u1D71': // �?  [LATIN SMALL LETTER P WITH MIDDLE TILDE]
          case '\u1D7D': // �?  [LATIN SMALL LETTER P WITH STROKE]
          case '\u1D88': // �?  [LATIN SMALL LETTER P WITH PALATAL HOOK]
          case '\u1E55': // �?  [LATIN SMALL LETTER P WITH ACUTE]
          case '\u1E57': // �?  [LATIN SMALL LETTER P WITH DOT ABOVE]
          case '\u24DF': // �?  [CIRCLED LATIN SMALL LETTER P]
          case '\uA751': // �?  [LATIN SMALL LETTER P WITH STROKE THROUGH DESCENDER]
          case '\uA753': // �?  [LATIN SMALL LETTER P WITH FLOURISH]
          case '\uA755': // �?  [LATIN SMALL LETTER P WITH SQUIRREL TAIL]
          case '\uA7FC': // �?  [LATIN EPIGRAPHIC LETTER REVERSED P]
          case '\uFF50': // �?  [FULLWIDTH LATIN SMALL LETTER P]
            output[outputPos++] = 'p';
            break;
          case '\u24AB': // �?  [PARENTHESIZED LATIN SMALL LETTER P]
            output[outputPos++] = '(';
            output[outputPos++] = 'p';
            output[outputPos++] = ')';
            break;
          case '\u024A': // Ɋ  [LATIN CAPITAL LETTER SMALL Q WITH HOOK TAIL]
          case '\u24C6': // �?  [CIRCLED LATIN CAPITAL LETTER Q]
          case '\uA756': // �?  [LATIN CAPITAL LETTER Q WITH STROKE THROUGH DESCENDER]
          case '\uA758': // �?  [LATIN CAPITAL LETTER Q WITH DIAGONAL STROKE]
          case '\uFF31': // �?  [FULLWIDTH LATIN CAPITAL LETTER Q]
            output[outputPos++] = 'Q';
            break;
          case '\u0138': // ĸ  http://en.wikipedia.org/wiki/Kra_(letter)  [LATIN SMALL LETTER KRA]
          case '\u024B': // ɋ  [LATIN SMALL LETTER Q WITH HOOK TAIL]
          case '\u02A0': // ʠ  [LATIN SMALL LETTER Q WITH HOOK]
          case '\u24E0': // �?  [CIRCLED LATIN SMALL LETTER Q]
          case '\uA757': // �?  [LATIN SMALL LETTER Q WITH STROKE THROUGH DESCENDER]
          case '\uA759': // �?  [LATIN SMALL LETTER Q WITH DIAGONAL STROKE]
          case '\uFF51': // �?  [FULLWIDTH LATIN SMALL LETTER Q]
            output[outputPos++] = 'q';
            break;
          case '\u24AC': // �?  [PARENTHESIZED LATIN SMALL LETTER Q]
            output[outputPos++] = '(';
            output[outputPos++] = 'q';
            output[outputPos++] = ')';
            break;
          case '\u0239': // ȹ  [LATIN SMALL LETTER QP DIGRAPH]
            output[outputPos++] = 'q';
            output[outputPos++] = 'p';
            break;
          case '\u0154': // Ŕ  [LATIN CAPITAL LETTER R WITH ACUTE]
          case '\u0156': // Ŗ  [LATIN CAPITAL LETTER R WITH CEDILLA]
          case '\u0158': // Ř  [LATIN CAPITAL LETTER R WITH CARON]
          case '\u0210': // Ȓ  [LATIN CAPITAL LETTER R WITH DOUBLE GRAVE]
          case '\u0212': // Ȓ  [LATIN CAPITAL LETTER R WITH INVERTED BREVE]
          case '\u024C': // Ɍ  [LATIN CAPITAL LETTER R WITH STROKE]
          case '\u0280': // ʀ  [LATIN LETTER SMALL CAPITAL R]
          case '\u0281': // ʁ  [LATIN LETTER SMALL CAPITAL INVERTED R]
          case '\u1D19': // �?  [LATIN LETTER SMALL CAPITAL REVERSED R]
          case '\u1D1A': // �?  [LATIN LETTER SMALL CAPITAL TURNED R]
          case '\u1E58': // �?  [LATIN CAPITAL LETTER R WITH DOT ABOVE]
          case '\u1E5A': // �?  [LATIN CAPITAL LETTER R WITH DOT BELOW]
          case '\u1E5C': // �?  [LATIN CAPITAL LETTER R WITH DOT BELOW AND MACRON]
          case '\u1E5E': // �?  [LATIN CAPITAL LETTER R WITH LINE BELOW]
          case '\u24C7': // �?  [CIRCLED LATIN CAPITAL LETTER R]
          case '\u2C64': // �?  [LATIN CAPITAL LETTER R WITH TAIL]
          case '\uA75A': // �?  [LATIN CAPITAL LETTER R ROTUNDA]
          case '\uA782': // �?  [LATIN CAPITAL LETTER INSULAR R]
          case '\uFF32': // �?  [FULLWIDTH LATIN CAPITAL LETTER R]
            output[outputPos++] = 'R';
            break;
          case '\u0155': // ŕ  [LATIN SMALL LETTER R WITH ACUTE]
          case '\u0157': // ŗ  [LATIN SMALL LETTER R WITH CEDILLA]
          case '\u0159': // ř  [LATIN SMALL LETTER R WITH CARON]
          case '\u0211': // ȑ  [LATIN SMALL LETTER R WITH DOUBLE GRAVE]
          case '\u0213': // ȓ  [LATIN SMALL LETTER R WITH INVERTED BREVE]
          case '\u024D': // ɍ  [LATIN SMALL LETTER R WITH STROKE]
          case '\u027C': // ɼ  [LATIN SMALL LETTER R WITH LONG LEG]
          case '\u027D': // ɽ  [LATIN SMALL LETTER R WITH TAIL]
          case '\u027E': // ɾ  [LATIN SMALL LETTER R WITH FISHHOOK]
          case '\u027F': // ɿ  [LATIN SMALL LETTER REVERSED R WITH FISHHOOK]
          case '\u1D63': // �?  [LATIN SUBSCRIPT SMALL LETTER R]
          case '\u1D72': // �?  [LATIN SMALL LETTER R WITH MIDDLE TILDE]
          case '\u1D73': // �?  [LATIN SMALL LETTER R WITH FISHHOOK AND MIDDLE TILDE]
          case '\u1D89': // �?  [LATIN SMALL LETTER R WITH PALATAL HOOK]
          case '\u1E59': // �?  [LATIN SMALL LETTER R WITH DOT ABOVE]
          case '\u1E5B': // �?  [LATIN SMALL LETTER R WITH DOT BELOW]
          case '\u1E5D': // �?  [LATIN SMALL LETTER R WITH DOT BELOW AND MACRON]
          case '\u1E5F': // �?  [LATIN SMALL LETTER R WITH LINE BELOW]
          case '\u24E1': // �?  [CIRCLED LATIN SMALL LETTER R]
          case '\uA75B': // �?  [LATIN SMALL LETTER R ROTUNDA]
          case '\uA783': // �?  [LATIN SMALL LETTER INSULAR R]
          case '\uFF52': // �?  [FULLWIDTH LATIN SMALL LETTER R]
            output[outputPos++] = 'r';
            break;
          case '\u24AD': // �?  [PARENTHESIZED LATIN SMALL LETTER R]
            output[outputPos++] = '(';
            output[outputPos++] = 'r';
            output[outputPos++] = ')';
            break;
          case '\u015A': // Ś  [LATIN CAPITAL LETTER S WITH ACUTE]
          case '\u015C': // Ŝ  [LATIN CAPITAL LETTER S WITH CIRCUMFLEX]
          case '\u015E': // Ş  [LATIN CAPITAL LETTER S WITH CEDILLA]
          case '\u0160': // Š  [LATIN CAPITAL LETTER S WITH CARON]
          case '\u0218': // Ș  [LATIN CAPITAL LETTER S WITH COMMA BELOW]
          case '\u1E60': // �?  [LATIN CAPITAL LETTER S WITH DOT ABOVE]
          case '\u1E62': // �?  [LATIN CAPITAL LETTER S WITH DOT BELOW]
          case '\u1E64': // �?  [LATIN CAPITAL LETTER S WITH ACUTE AND DOT ABOVE]
          case '\u1E66': // �?  [LATIN CAPITAL LETTER S WITH CARON AND DOT ABOVE]
          case '\u1E68': // �?  [LATIN CAPITAL LETTER S WITH DOT BELOW AND DOT ABOVE]
          case '\u24C8': // �?  [CIRCLED LATIN CAPITAL LETTER S]
          case '\uA731': // �?  [LATIN LETTER SMALL CAPITAL S]
          case '\uA785': // �?  [LATIN SMALL LETTER INSULAR S]
          case '\uFF33': // �?  [FULLWIDTH LATIN CAPITAL LETTER S]
            output[outputPos++] = 'S';
            break;
          case '\u015B': // ś  [LATIN SMALL LETTER S WITH ACUTE]
          case '\u015D': // ŝ  [LATIN SMALL LETTER S WITH CIRCUMFLEX]
          case '\u015F': // ş  [LATIN SMALL LETTER S WITH CEDILLA]
          case '\u0161': // š  [LATIN SMALL LETTER S WITH CARON]
          case '\u017F': // ſ  http://en.wikipedia.org/wiki/Long_S  [LATIN SMALL LETTER LONG S]
          case '\u0219': // ș  [LATIN SMALL LETTER S WITH COMMA BELOW]
          case '\u023F': // ȿ  [LATIN SMALL LETTER S WITH SWASH TAIL]
          case '\u0282': // ʂ  [LATIN SMALL LETTER S WITH HOOK]
          case '\u1D74': // �?  [LATIN SMALL LETTER S WITH MIDDLE TILDE]
          case '\u1D8A': // �?  [LATIN SMALL LETTER S WITH PALATAL HOOK]
          case '\u1E61': // �?  [LATIN SMALL LETTER S WITH DOT ABOVE]
          case '\u1E63': // �?  [LATIN SMALL LETTER S WITH DOT BELOW]
          case '\u1E65': // �?  [LATIN SMALL LETTER S WITH ACUTE AND DOT ABOVE]
          case '\u1E67': // �?  [LATIN SMALL LETTER S WITH CARON AND DOT ABOVE]
          case '\u1E69': // �?  [LATIN SMALL LETTER S WITH DOT BELOW AND DOT ABOVE]
          case '\u1E9C': // �?  [LATIN SMALL LETTER LONG S WITH DIAGONAL STROKE]
          case '\u1E9D': // �?  [LATIN SMALL LETTER LONG S WITH HIGH STROKE]
          case '\u24E2': // �?  [CIRCLED LATIN SMALL LETTER S]
          case '\uA784': // �?  [LATIN CAPITAL LETTER INSULAR S]
          case '\uFF53': // �?  [FULLWIDTH LATIN SMALL LETTER S]
            output[outputPos++] = 's';
            break;
          case '\u1E9E': // �?  [LATIN CAPITAL LETTER SHARP S]
            output[outputPos++] = 'S';
            output[outputPos++] = 'S';
            break;
          case '\u24AE': // �?  [PARENTHESIZED LATIN SMALL LETTER S]
            output[outputPos++] = '(';
            output[outputPos++] = 's';
            output[outputPos++] = ')';
            break;
          case '\u00DF': // ß  [LATIN SMALL LETTER SHARP S]
            output[outputPos++] = 's';
            output[outputPos++] = 's';
            break;
          case '\uFB06': // �?  [LATIN SMALL LIGATURE ST]
            output[outputPos++] = 's';
            output[outputPos++] = 't';
            break;
          case '\u0162': // Ţ  [LATIN CAPITAL LETTER T WITH CEDILLA]
          case '\u0164': // Ť  [LATIN CAPITAL LETTER T WITH CARON]
          case '\u0166': // Ŧ  [LATIN CAPITAL LETTER T WITH STROKE]
          case '\u01AC': // Ƭ  [LATIN CAPITAL LETTER T WITH HOOK]
          case '\u01AE': // Ʈ  [LATIN CAPITAL LETTER T WITH RETROFLEX HOOK]
          case '\u021A': // Ț  [LATIN CAPITAL LETTER T WITH COMMA BELOW]
          case '\u023E': // Ⱦ  [LATIN CAPITAL LETTER T WITH DIAGONAL STROKE]
          case '\u1D1B': // �?  [LATIN LETTER SMALL CAPITAL T]
          case '\u1E6A': // �?  [LATIN CAPITAL LETTER T WITH DOT ABOVE]
          case '\u1E6C': // �?  [LATIN CAPITAL LETTER T WITH DOT BELOW]
          case '\u1E6E': // �?  [LATIN CAPITAL LETTER T WITH LINE BELOW]
          case '\u1E70': // �?  [LATIN CAPITAL LETTER T WITH CIRCUMFLEX BELOW]
          case '\u24C9': // �?  [CIRCLED LATIN CAPITAL LETTER T]
          case '\uA786': // �?  [LATIN CAPITAL LETTER INSULAR T]
          case '\uFF34': // �?  [FULLWIDTH LATIN CAPITAL LETTER T]
            output[outputPos++] = 'T';
            break;
          case '\u0163': // ţ  [LATIN SMALL LETTER T WITH CEDILLA]
          case '\u0165': // ť  [LATIN SMALL LETTER T WITH CARON]
          case '\u0167': // ŧ  [LATIN SMALL LETTER T WITH STROKE]
          case '\u01AB': // ƫ  [LATIN SMALL LETTER T WITH PALATAL HOOK]
          case '\u01AD': // ƭ  [LATIN SMALL LETTER T WITH HOOK]
          case '\u021B': // ț  [LATIN SMALL LETTER T WITH COMMA BELOW]
          case '\u0236': // ȶ  [LATIN SMALL LETTER T WITH CURL]
          case '\u0287': // ʇ  [LATIN SMALL LETTER TURNED T]
          case '\u0288': // ʈ  [LATIN SMALL LETTER T WITH RETROFLEX HOOK]
          case '\u1D75': // �?  [LATIN SMALL LETTER T WITH MIDDLE TILDE]
          case '\u1E6B': // �?  [LATIN SMALL LETTER T WITH DOT ABOVE]
          case '\u1E6D': // �?  [LATIN SMALL LETTER T WITH DOT BELOW]
          case '\u1E6F': // �?  [LATIN SMALL LETTER T WITH LINE BELOW]
          case '\u1E71': // �?  [LATIN SMALL LETTER T WITH CIRCUMFLEX BELOW]
          case '\u1E97': // �?  [LATIN SMALL LETTER T WITH DIAERESIS]
          case '\u24E3': // �?  [CIRCLED LATIN SMALL LETTER T]
          case '\u2C66': // �?  [LATIN SMALL LETTER T WITH DIAGONAL STROKE]
          case '\uFF54': // �?  [FULLWIDTH LATIN SMALL LETTER T]
            output[outputPos++] = 't';
            break;
          case '\u00DE': // Þ  [LATIN CAPITAL LETTER THORN]
          case '\uA766': // �?  [LATIN CAPITAL LETTER THORN WITH STROKE THROUGH DESCENDER]
            output[outputPos++] = 'T';
            output[outputPos++] = 'H';
            break;
          case '\uA728': // �?  [LATIN CAPITAL LETTER TZ]
            output[outputPos++] = 'T';
            output[outputPos++] = 'Z';
            break;
          case '\u24AF': // �?  [PARENTHESIZED LATIN SMALL LETTER T]
            output[outputPos++] = '(';
            output[outputPos++] = 't';
            output[outputPos++] = ')';
            break;
          case '\u02A8': // ʨ  [LATIN SMALL LETTER TC DIGRAPH WITH CURL]
            output[outputPos++] = 't';
            output[outputPos++] = 'c';
            break;
          case '\u00FE': // þ  [LATIN SMALL LETTER THORN]
          case '\u1D7A': // �?  [LATIN SMALL LETTER TH WITH STRIKETHROUGH]
          case '\uA767': // �?  [LATIN SMALL LETTER THORN WITH STROKE THROUGH DESCENDER]
            output[outputPos++] = 't';
            output[outputPos++] = 'h';
            break;
          case '\u02A6': // ʦ  [LATIN SMALL LETTER TS DIGRAPH]
            output[outputPos++] = 't';
            output[outputPos++] = 's';
            break;
          case '\uA729': // �?  [LATIN SMALL LETTER TZ]
            output[outputPos++] = 't';
            output[outputPos++] = 'z';
            break;
          case '\u00D9': // Ù  [LATIN CAPITAL LETTER U WITH GRAVE]
          case '\u00DA': // Ú  [LATIN CAPITAL LETTER U WITH ACUTE]
          case '\u00DB': // Û  [LATIN CAPITAL LETTER U WITH CIRCUMFLEX]
          case '\u00DC': // Ü  [LATIN CAPITAL LETTER U WITH DIAERESIS]
          case '\u0168': // Ũ  [LATIN CAPITAL LETTER U WITH TILDE]
          case '\u016A': // Ū  [LATIN CAPITAL LETTER U WITH MACRON]
          case '\u016C': // Ŭ  [LATIN CAPITAL LETTER U WITH BREVE]
          case '\u016E': // Ů  [LATIN CAPITAL LETTER U WITH RING ABOVE]
          case '\u0170': // Ű  [LATIN CAPITAL LETTER U WITH DOUBLE ACUTE]
          case '\u0172': // Ų  [LATIN CAPITAL LETTER U WITH OGONEK]
          case '\u01AF': // Ư  [LATIN CAPITAL LETTER U WITH HORN]
          case '\u01D3': // Ǔ  [LATIN CAPITAL LETTER U WITH CARON]
          case '\u01D5': // Ǖ  [LATIN CAPITAL LETTER U WITH DIAERESIS AND MACRON]
          case '\u01D7': // Ǘ  [LATIN CAPITAL LETTER U WITH DIAERESIS AND ACUTE]
          case '\u01D9': // Ǚ  [LATIN CAPITAL LETTER U WITH DIAERESIS AND CARON]
          case '\u01DB': // Ǜ  [LATIN CAPITAL LETTER U WITH DIAERESIS AND GRAVE]
          case '\u0214': // Ȕ  [LATIN CAPITAL LETTER U WITH DOUBLE GRAVE]
          case '\u0216': // Ȗ  [LATIN CAPITAL LETTER U WITH INVERTED BREVE]
          case '\u0244': // Ʉ  [LATIN CAPITAL LETTER U BAR]
          case '\u1D1C': // �?  [LATIN LETTER SMALL CAPITAL U]
          case '\u1D7E': // �?  [LATIN SMALL CAPITAL LETTER U WITH STROKE]
          case '\u1E72': // �?  [LATIN CAPITAL LETTER U WITH DIAERESIS BELOW]
          case '\u1E74': // �?  [LATIN CAPITAL LETTER U WITH TILDE BELOW]
          case '\u1E76': // �?  [LATIN CAPITAL LETTER U WITH CIRCUMFLEX BELOW]
          case '\u1E78': // �?  [LATIN CAPITAL LETTER U WITH TILDE AND ACUTE]
          case '\u1E7A': // �?  [LATIN CAPITAL LETTER U WITH MACRON AND DIAERESIS]
          case '\u1EE4': // �?  [LATIN CAPITAL LETTER U WITH DOT BELOW]
          case '\u1EE6': // �?  [LATIN CAPITAL LETTER U WITH HOOK ABOVE]
          case '\u1EE8': // �?  [LATIN CAPITAL LETTER U WITH HORN AND ACUTE]
          case '\u1EEA': // �?  [LATIN CAPITAL LETTER U WITH HORN AND GRAVE]
          case '\u1EEC': // �?  [LATIN CAPITAL LETTER U WITH HORN AND HOOK ABOVE]
          case '\u1EEE': // �?  [LATIN CAPITAL LETTER U WITH HORN AND TILDE]
          case '\u1EF0': // �?  [LATIN CAPITAL LETTER U WITH HORN AND DOT BELOW]
          case '\u24CA': // �?  [CIRCLED LATIN CAPITAL LETTER U]
          case '\uFF35': // �?  [FULLWIDTH LATIN CAPITAL LETTER U]
            output[outputPos++] = 'U';
            break;
          case '\u00F9': // ù  [LATIN SMALL LETTER U WITH GRAVE]
          case '\u00FA': // ú  [LATIN SMALL LETTER U WITH ACUTE]
          case '\u00FB': // û  [LATIN SMALL LETTER U WITH CIRCUMFLEX]
          case '\u00FC': // ü  [LATIN SMALL LETTER U WITH DIAERESIS]
          case '\u0169': // ũ  [LATIN SMALL LETTER U WITH TILDE]
          case '\u016B': // ū  [LATIN SMALL LETTER U WITH MACRON]
          case '\u016D': // ŭ  [LATIN SMALL LETTER U WITH BREVE]
          case '\u016F': // ů  [LATIN SMALL LETTER U WITH RING ABOVE]
          case '\u0171': // ű  [LATIN SMALL LETTER U WITH DOUBLE ACUTE]
          case '\u0173': // ų  [LATIN SMALL LETTER U WITH OGONEK]
          case '\u01B0': // ư  [LATIN SMALL LETTER U WITH HORN]
          case '\u01D4': // ǔ  [LATIN SMALL LETTER U WITH CARON]
          case '\u01D6': // ǖ  [LATIN SMALL LETTER U WITH DIAERESIS AND MACRON]
          case '\u01D8': // ǘ  [LATIN SMALL LETTER U WITH DIAERESIS AND ACUTE]
          case '\u01DA': // ǚ  [LATIN SMALL LETTER U WITH DIAERESIS AND CARON]
          case '\u01DC': // ǜ  [LATIN SMALL LETTER U WITH DIAERESIS AND GRAVE]
          case '\u0215': // ȕ  [LATIN SMALL LETTER U WITH DOUBLE GRAVE]
          case '\u0217': // ȗ  [LATIN SMALL LETTER U WITH INVERTED BREVE]
          case '\u0289': // ʉ  [LATIN SMALL LETTER U BAR]
          case '\u1D64': // �?  [LATIN SUBSCRIPT SMALL LETTER U]
          case '\u1D99': // �?  [LATIN SMALL LETTER U WITH RETROFLEX HOOK]
          case '\u1E73': // �?  [LATIN SMALL LETTER U WITH DIAERESIS BELOW]
          case '\u1E75': // �?  [LATIN SMALL LETTER U WITH TILDE BELOW]
          case '\u1E77': // �?  [LATIN SMALL LETTER U WITH CIRCUMFLEX BELOW]
          case '\u1E79': // �?  [LATIN SMALL LETTER U WITH TILDE AND ACUTE]
          case '\u1E7B': // �?  [LATIN SMALL LETTER U WITH MACRON AND DIAERESIS]
          case '\u1EE5': // �?  [LATIN SMALL LETTER U WITH DOT BELOW]
          case '\u1EE7': // �?  [LATIN SMALL LETTER U WITH HOOK ABOVE]
          case '\u1EE9': // �?  [LATIN SMALL LETTER U WITH HORN AND ACUTE]
          case '\u1EEB': // �?  [LATIN SMALL LETTER U WITH HORN AND GRAVE]
          case '\u1EED': // �?  [LATIN SMALL LETTER U WITH HORN AND HOOK ABOVE]
          case '\u1EEF': // �?  [LATIN SMALL LETTER U WITH HORN AND TILDE]
          case '\u1EF1': // �?  [LATIN SMALL LETTER U WITH HORN AND DOT BELOW]
          case '\u24E4': // �?  [CIRCLED LATIN SMALL LETTER U]
          case '\uFF55': // �?  [FULLWIDTH LATIN SMALL LETTER U]
            output[outputPos++] = 'u';
            break;
          case '\u24B0': // �?  [PARENTHESIZED LATIN SMALL LETTER U]
            output[outputPos++] = '(';
            output[outputPos++] = 'u';
            output[outputPos++] = ')';
            break;
          case '\u1D6B': // �?  [LATIN SMALL LETTER UE]
            output[outputPos++] = 'u';
            output[outputPos++] = 'e';
            break;
          case '\u01B2': // Ʋ  [LATIN CAPITAL LETTER V WITH HOOK]
          case '\u0245': // Ʌ  [LATIN CAPITAL LETTER TURNED V]
          case '\u1D20': // �?  [LATIN LETTER SMALL CAPITAL V]
          case '\u1E7C': // �?  [LATIN CAPITAL LETTER V WITH TILDE]
          case '\u1E7E': // �?  [LATIN CAPITAL LETTER V WITH DOT BELOW]
          case '\u1EFC': // �?  [LATIN CAPITAL LETTER MIDDLE-WELSH V]
          case '\u24CB': // �?  [CIRCLED LATIN CAPITAL LETTER V]
          case '\uA75E': // �?  [LATIN CAPITAL LETTER V WITH DIAGONAL STROKE]
          case '\uA768': // �?  [LATIN CAPITAL LETTER VEND]
          case '\uFF36': // �?  [FULLWIDTH LATIN CAPITAL LETTER V]
            output[outputPos++] = 'V';
            break;
          case '\u028B': // ʋ  [LATIN SMALL LETTER V WITH HOOK]
          case '\u028C': // ʌ  [LATIN SMALL LETTER TURNED V]
          case '\u1D65': // �?  [LATIN SUBSCRIPT SMALL LETTER V]
          case '\u1D8C': // �?  [LATIN SMALL LETTER V WITH PALATAL HOOK]
          case '\u1E7D': // �?  [LATIN SMALL LETTER V WITH TILDE]
          case '\u1E7F': // �?  [LATIN SMALL LETTER V WITH DOT BELOW]
          case '\u24E5': // �?  [CIRCLED LATIN SMALL LETTER V]
          case '\u2C71': // �?  [LATIN SMALL LETTER V WITH RIGHT HOOK]
          case '\u2C74': // �?  [LATIN SMALL LETTER V WITH CURL]
          case '\uA75F': // �?  [LATIN SMALL LETTER V WITH DIAGONAL STROKE]
          case '\uFF56': // �?  [FULLWIDTH LATIN SMALL LETTER V]
            output[outputPos++] = 'v';
            break;
          case '\uA760': // �?  [LATIN CAPITAL LETTER VY]
            output[outputPos++] = 'V';
            output[outputPos++] = 'Y';
            break;
          case '\u24B1': // �?  [PARENTHESIZED LATIN SMALL LETTER V]
            output[outputPos++] = '(';
            output[outputPos++] = 'v';
            output[outputPos++] = ')';
            break;
          case '\uA761': // �?  [LATIN SMALL LETTER VY]
            output[outputPos++] = 'v';
            output[outputPos++] = 'y';
            break;
          case '\u0174': // Ŵ  [LATIN CAPITAL LETTER W WITH CIRCUMFLEX]
          case '\u01F7': // Ƿ  http://en.wikipedia.org/wiki/Wynn  [LATIN CAPITAL LETTER WYNN]
          case '\u1D21': // �?  [LATIN LETTER SMALL CAPITAL W]
          case '\u1E80': // �?  [LATIN CAPITAL LETTER W WITH GRAVE]
          case '\u1E82': // �?  [LATIN CAPITAL LETTER W WITH ACUTE]
          case '\u1E84': // �?  [LATIN CAPITAL LETTER W WITH DIAERESIS]
          case '\u1E86': // �?  [LATIN CAPITAL LETTER W WITH DOT ABOVE]
          case '\u1E88': // �?  [LATIN CAPITAL LETTER W WITH DOT BELOW]
          case '\u24CC': // �?  [CIRCLED LATIN CAPITAL LETTER W]
          case '\u2C72': // �?  [LATIN CAPITAL LETTER W WITH HOOK]
          case '\uFF37': // �?  [FULLWIDTH LATIN CAPITAL LETTER W]
            output[outputPos++] = 'W';
            break;
          case '\u0175': // ŵ  [LATIN SMALL LETTER W WITH CIRCUMFLEX]
          case '\u01BF': // ƿ  http://en.wikipedia.org/wiki/Wynn  [LATIN LETTER WYNN]
          case '\u028D': // ʍ  [LATIN SMALL LETTER TURNED W]
          case '\u1E81': // �?  [LATIN SMALL LETTER W WITH GRAVE]
          case '\u1E83': // �?  [LATIN SMALL LETTER W WITH ACUTE]
          case '\u1E85': // �?  [LATIN SMALL LETTER W WITH DIAERESIS]
          case '\u1E87': // �?  [LATIN SMALL LETTER W WITH DOT ABOVE]
          case '\u1E89': // �?  [LATIN SMALL LETTER W WITH DOT BELOW]
          case '\u1E98': // �?  [LATIN SMALL LETTER W WITH RING ABOVE]
          case '\u24E6': // �?  [CIRCLED LATIN SMALL LETTER W]
          case '\u2C73': // �?  [LATIN SMALL LETTER W WITH HOOK]
          case '\uFF57': // �?  [FULLWIDTH LATIN SMALL LETTER W]
            output[outputPos++] = 'w';
            break;
          case '\u24B2': // �?  [PARENTHESIZED LATIN SMALL LETTER W]
            output[outputPos++] = '(';
            output[outputPos++] = 'w';
            output[outputPos++] = ')';
            break;
          case '\u1E8A': // �?  [LATIN CAPITAL LETTER X WITH DOT ABOVE]
          case '\u1E8C': // �?  [LATIN CAPITAL LETTER X WITH DIAERESIS]
          case '\u24CD': // �?  [CIRCLED LATIN CAPITAL LETTER X]
          case '\uFF38': // �?  [FULLWIDTH LATIN CAPITAL LETTER X]
            output[outputPos++] = 'X';
            break;
          case '\u1D8D': // �?  [LATIN SMALL LETTER X WITH PALATAL HOOK]
          case '\u1E8B': // �?  [LATIN SMALL LETTER X WITH DOT ABOVE]
          case '\u1E8D': // �?  [LATIN SMALL LETTER X WITH DIAERESIS]
          case '\u2093': // �?  [LATIN SUBSCRIPT SMALL LETTER X]
          case '\u24E7': // �?  [CIRCLED LATIN SMALL LETTER X]
          case '\uFF58': // �?  [FULLWIDTH LATIN SMALL LETTER X]
            output[outputPos++] = 'x';
            break;
          case '\u24B3': // �?  [PARENTHESIZED LATIN SMALL LETTER X]
            output[outputPos++] = '(';
            output[outputPos++] = 'x';
            output[outputPos++] = ')';
            break;
          case '\u00DD': // Ý  [LATIN CAPITAL LETTER Y WITH ACUTE]
          case '\u0176': // Ŷ  [LATIN CAPITAL LETTER Y WITH CIRCUMFLEX]
          case '\u0178': // Ÿ  [LATIN CAPITAL LETTER Y WITH DIAERESIS]
          case '\u01B3': // Ƴ  [LATIN CAPITAL LETTER Y WITH HOOK]
          case '\u0232': // Ȳ  [LATIN CAPITAL LETTER Y WITH MACRON]
          case '\u024E': // Ɏ  [LATIN CAPITAL LETTER Y WITH STROKE]
          case '\u028F': // ʏ  [LATIN LETTER SMALL CAPITAL Y]
          case '\u1E8E': // �?  [LATIN CAPITAL LETTER Y WITH DOT ABOVE]
          case '\u1EF2': // �?  [LATIN CAPITAL LETTER Y WITH GRAVE]
          case '\u1EF4': // �?  [LATIN CAPITAL LETTER Y WITH DOT BELOW]
          case '\u1EF6': // �?  [LATIN CAPITAL LETTER Y WITH HOOK ABOVE]
          case '\u1EF8': // �?  [LATIN CAPITAL LETTER Y WITH TILDE]
          case '\u1EFE': // �?  [LATIN CAPITAL LETTER Y WITH LOOP]
          case '\u24CE': // �?  [CIRCLED LATIN CAPITAL LETTER Y]
          case '\uFF39': // �?  [FULLWIDTH LATIN CAPITAL LETTER Y]
            output[outputPos++] = 'Y';
            break;
          case '\u00FD': // ý  [LATIN SMALL LETTER Y WITH ACUTE]
          case '\u00FF': // ÿ  [LATIN SMALL LETTER Y WITH DIAERESIS]
          case '\u0177': // ŷ  [LATIN SMALL LETTER Y WITH CIRCUMFLEX]
          case '\u01B4': // ƴ  [LATIN SMALL LETTER Y WITH HOOK]
          case '\u0233': // ȳ  [LATIN SMALL LETTER Y WITH MACRON]
          case '\u024F': // ɏ  [LATIN SMALL LETTER Y WITH STROKE]
          case '\u028E': // ʎ  [LATIN SMALL LETTER TURNED Y]
          case '\u1E8F': // �?  [LATIN SMALL LETTER Y WITH DOT ABOVE]
          case '\u1E99': // �?  [LATIN SMALL LETTER Y WITH RING ABOVE]
          case '\u1EF3': // �?  [LATIN SMALL LETTER Y WITH GRAVE]
          case '\u1EF5': // �?  [LATIN SMALL LETTER Y WITH DOT BELOW]
          case '\u1EF7': // �?  [LATIN SMALL LETTER Y WITH HOOK ABOVE]
          case '\u1EF9': // �?  [LATIN SMALL LETTER Y WITH TILDE]
          case '\u1EFF': // �?  [LATIN SMALL LETTER Y WITH LOOP]
          case '\u24E8': // �?  [CIRCLED LATIN SMALL LETTER Y]
          case '\uFF59': // �?  [FULLWIDTH LATIN SMALL LETTER Y]
            output[outputPos++] = 'y';
            break;
          case '\u24B4': // �?  [PARENTHESIZED LATIN SMALL LETTER Y]
            output[outputPos++] = '(';
            output[outputPos++] = 'y';
            output[outputPos++] = ')';
            break;
          case '\u0179': // Ź  [LATIN CAPITAL LETTER Z WITH ACUTE]
          case '\u017B': // Ż  [LATIN CAPITAL LETTER Z WITH DOT ABOVE]
          case '\u017D': // Ž  [LATIN CAPITAL LETTER Z WITH CARON]
          case '\u01B5': // Ƶ  [LATIN CAPITAL LETTER Z WITH STROKE]
          case '\u021C': // Ȝ  http://en.wikipedia.org/wiki/Yogh  [LATIN CAPITAL LETTER YOGH]
          case '\u0224': // Ȥ  [LATIN CAPITAL LETTER Z WITH HOOK]
          case '\u1D22': // �?  [LATIN LETTER SMALL CAPITAL Z]
          case '\u1E90': // �?  [LATIN CAPITAL LETTER Z WITH CIRCUMFLEX]
          case '\u1E92': // �?  [LATIN CAPITAL LETTER Z WITH DOT BELOW]
          case '\u1E94': // �?  [LATIN CAPITAL LETTER Z WITH LINE BELOW]
          case '\u24CF': // �?  [CIRCLED LATIN CAPITAL LETTER Z]
          case '\u2C6B': // �?  [LATIN CAPITAL LETTER Z WITH DESCENDER]
          case '\uA762': // �?  [LATIN CAPITAL LETTER VISIGOTHIC Z]
          case '\uFF3A': // �?  [FULLWIDTH LATIN CAPITAL LETTER Z]
            output[outputPos++] = 'Z';
            break;
          case '\u017A': // ź  [LATIN SMALL LETTER Z WITH ACUTE]
          case '\u017C': // ż  [LATIN SMALL LETTER Z WITH DOT ABOVE]
          case '\u017E': // ž  [LATIN SMALL LETTER Z WITH CARON]
          case '\u01B6': // ƶ  [LATIN SMALL LETTER Z WITH STROKE]
          case '\u021D': // ȝ  http://en.wikipedia.org/wiki/Yogh  [LATIN SMALL LETTER YOGH]
          case '\u0225': // ȥ  [LATIN SMALL LETTER Z WITH HOOK]
          case '\u0240': // ɀ  [LATIN SMALL LETTER Z WITH SWASH TAIL]
          case '\u0290': // ʐ  [LATIN SMALL LETTER Z WITH RETROFLEX HOOK]
          case '\u0291': // ʑ  [LATIN SMALL LETTER Z WITH CURL]
          case '\u1D76': // �?  [LATIN SMALL LETTER Z WITH MIDDLE TILDE]
          case '\u1D8E': // �?  [LATIN SMALL LETTER Z WITH PALATAL HOOK]
          case '\u1E91': // �?  [LATIN SMALL LETTER Z WITH CIRCUMFLEX]
          case '\u1E93': // �?  [LATIN SMALL LETTER Z WITH DOT BELOW]
          case '\u1E95': // �?  [LATIN SMALL LETTER Z WITH LINE BELOW]
          case '\u24E9': // �?  [CIRCLED LATIN SMALL LETTER Z]
          case '\u2C6C': // �?  [LATIN SMALL LETTER Z WITH DESCENDER]
          case '\uA763': // �?  [LATIN SMALL LETTER VISIGOTHIC Z]
          case '\uFF5A': // �?  [FULLWIDTH LATIN SMALL LETTER Z]
            output[outputPos++] = 'z';
            break;
          case '\u24B5': // �?  [PARENTHESIZED LATIN SMALL LETTER Z]
            output[outputPos++] = '(';
            output[outputPos++] = 'z';
            output[outputPos++] = ')';
            break;
          case '\u2070': // �?  [SUPERSCRIPT ZERO]
          case '\u2080': // �?  [SUBSCRIPT ZERO]
          case '\u24EA': // �?  [CIRCLED DIGIT ZERO]
          case '\u24FF': // �?  [NEGATIVE CIRCLED DIGIT ZERO]
          case '\uFF10': // �?  [FULLWIDTH DIGIT ZERO]
            output[outputPos++] = '0';
            break;
          case '\u00B9': // ¹  [SUPERSCRIPT ONE]
          case '\u2081': // �?  [SUBSCRIPT ONE]
          case '\u2460': // �?  [CIRCLED DIGIT ONE]
          case '\u24F5': // �?  [DOUBLE CIRCLED DIGIT ONE]
          case '\u2776': // �?  [DINGBAT NEGATIVE CIRCLED DIGIT ONE]
          case '\u2780': // �?  [DINGBAT CIRCLED SANS-SERIF DIGIT ONE]
          case '\u278A': // �?  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT ONE]
          case '\uFF11': // �?  [FULLWIDTH DIGIT ONE]
            output[outputPos++] = '1';
            break;
          case '\u2488': // �?  [DIGIT ONE FULL STOP]
            output[outputPos++] = '1';
            output[outputPos++] = '.';
            break;
          case '\u2474': // �?  [PARENTHESIZED DIGIT ONE]
            output[outputPos++] = '(';
            output[outputPos++] = '1';
            output[outputPos++] = ')';
            break;
          case '\u00B2': // ²  [SUPERSCRIPT TWO]
          case '\u2082': // �?  [SUBSCRIPT TWO]
          case '\u2461': // �?  [CIRCLED DIGIT TWO]
          case '\u24F6': // �?  [DOUBLE CIRCLED DIGIT TWO]
          case '\u2777': // �?  [DINGBAT NEGATIVE CIRCLED DIGIT TWO]
          case '\u2781': // �?  [DINGBAT CIRCLED SANS-SERIF DIGIT TWO]
          case '\u278B': // �?  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT TWO]
          case '\uFF12': // �?  [FULLWIDTH DIGIT TWO]
            output[outputPos++] = '2';
            break;
          case '\u2489': // �?  [DIGIT TWO FULL STOP]
            output[outputPos++] = '2';
            output[outputPos++] = '.';
            break;
          case '\u2475': // �?  [PARENTHESIZED DIGIT TWO]
            output[outputPos++] = '(';
            output[outputPos++] = '2';
            output[outputPos++] = ')';
            break;
          case '\u00B3': // ³  [SUPERSCRIPT THREE]
          case '\u2083': // �?  [SUBSCRIPT THREE]
          case '\u2462': // �?  [CIRCLED DIGIT THREE]
          case '\u24F7': // �?  [DOUBLE CIRCLED DIGIT THREE]
          case '\u2778': // �?  [DINGBAT NEGATIVE CIRCLED DIGIT THREE]
          case '\u2782': // �?  [DINGBAT CIRCLED SANS-SERIF DIGIT THREE]
          case '\u278C': // �?  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT THREE]
          case '\uFF13': // �?  [FULLWIDTH DIGIT THREE]
            output[outputPos++] = '3';
            break;
          case '\u248A': // �?  [DIGIT THREE FULL STOP]
            output[outputPos++] = '3';
            output[outputPos++] = '.';
            break;
          case '\u2476': // �?  [PARENTHESIZED DIGIT THREE]
            output[outputPos++] = '(';
            output[outputPos++] = '3';
            output[outputPos++] = ')';
            break;
          case '\u2074': // �?  [SUPERSCRIPT FOUR]
          case '\u2084': // �?  [SUBSCRIPT FOUR]
          case '\u2463': // �?  [CIRCLED DIGIT FOUR]
          case '\u24F8': // �?  [DOUBLE CIRCLED DIGIT FOUR]
          case '\u2779': // �?  [DINGBAT NEGATIVE CIRCLED DIGIT FOUR]
          case '\u2783': // �?  [DINGBAT CIRCLED SANS-SERIF DIGIT FOUR]
          case '\u278D': // �?  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT FOUR]
          case '\uFF14': // �?  [FULLWIDTH DIGIT FOUR]
            output[outputPos++] = '4';
            break;
          case '\u248B': // �?  [DIGIT FOUR FULL STOP]
            output[outputPos++] = '4';
            output[outputPos++] = '.';
            break;
          case '\u2477': // �?  [PARENTHESIZED DIGIT FOUR]
            output[outputPos++] = '(';
            output[outputPos++] = '4';
            output[outputPos++] = ')';
            break;
          case '\u2075': // �?  [SUPERSCRIPT FIVE]
          case '\u2085': // �?  [SUBSCRIPT FIVE]
          case '\u2464': // �?  [CIRCLED DIGIT FIVE]
          case '\u24F9': // �?  [DOUBLE CIRCLED DIGIT FIVE]
          case '\u277A': // �?  [DINGBAT NEGATIVE CIRCLED DIGIT FIVE]
          case '\u2784': // �?  [DINGBAT CIRCLED SANS-SERIF DIGIT FIVE]
          case '\u278E': // �?  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT FIVE]
          case '\uFF15': // �?  [FULLWIDTH DIGIT FIVE]
            output[outputPos++] = '5';
            break;
          case '\u248C': // �?  [DIGIT FIVE FULL STOP]
            output[outputPos++] = '5';
            output[outputPos++] = '.';
            break;
          case '\u2478': // �?  [PARENTHESIZED DIGIT FIVE]
            output[outputPos++] = '(';
            output[outputPos++] = '5';
            output[outputPos++] = ')';
            break;
          case '\u2076': // �?  [SUPERSCRIPT SIX]
          case '\u2086': // �?  [SUBSCRIPT SIX]
          case '\u2465': // �?  [CIRCLED DIGIT SIX]
          case '\u24FA': // �?  [DOUBLE CIRCLED DIGIT SIX]
          case '\u277B': // �?  [DINGBAT NEGATIVE CIRCLED DIGIT SIX]
          case '\u2785': // �?  [DINGBAT CIRCLED SANS-SERIF DIGIT SIX]
          case '\u278F': // �?  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT SIX]
          case '\uFF16': // �?  [FULLWIDTH DIGIT SIX]
            output[outputPos++] = '6';
            break;
          case '\u248D': // �?  [DIGIT SIX FULL STOP]
            output[outputPos++] = '6';
            output[outputPos++] = '.';
            break;
          case '\u2479': // �?  [PARENTHESIZED DIGIT SIX]
            output[outputPos++] = '(';
            output[outputPos++] = '6';
            output[outputPos++] = ')';
            break;
          case '\u2077': // �?  [SUPERSCRIPT SEVEN]
          case '\u2087': // �?  [SUBSCRIPT SEVEN]
          case '\u2466': // �?  [CIRCLED DIGIT SEVEN]
          case '\u24FB': // �?  [DOUBLE CIRCLED DIGIT SEVEN]
          case '\u277C': // �?  [DINGBAT NEGATIVE CIRCLED DIGIT SEVEN]
          case '\u2786': // �?  [DINGBAT CIRCLED SANS-SERIF DIGIT SEVEN]
          case '\u2790': // �?  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT SEVEN]
          case '\uFF17': // �?  [FULLWIDTH DIGIT SEVEN]
            output[outputPos++] = '7';
            break;
          case '\u248E': // �?  [DIGIT SEVEN FULL STOP]
            output[outputPos++] = '7';
            output[outputPos++] = '.';
            break;
          case '\u247A': // �?  [PARENTHESIZED DIGIT SEVEN]
            output[outputPos++] = '(';
            output[outputPos++] = '7';
            output[outputPos++] = ')';
            break;
          case '\u2078': // �?  [SUPERSCRIPT EIGHT]
          case '\u2088': // �?  [SUBSCRIPT EIGHT]
          case '\u2467': // �?  [CIRCLED DIGIT EIGHT]
          case '\u24FC': // �?  [DOUBLE CIRCLED DIGIT EIGHT]
          case '\u277D': // �?  [DINGBAT NEGATIVE CIRCLED DIGIT EIGHT]
          case '\u2787': // �?  [DINGBAT CIRCLED SANS-SERIF DIGIT EIGHT]
          case '\u2791': // �?  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT EIGHT]
          case '\uFF18': // �?  [FULLWIDTH DIGIT EIGHT]
            output[outputPos++] = '8';
            break;
          case '\u248F': // �?  [DIGIT EIGHT FULL STOP]
            output[outputPos++] = '8';
            output[outputPos++] = '.';
            break;
          case '\u247B': // �?  [PARENTHESIZED DIGIT EIGHT]
            output[outputPos++] = '(';
            output[outputPos++] = '8';
            output[outputPos++] = ')';
            break;
          case '\u2079': // �?  [SUPERSCRIPT NINE]
          case '\u2089': // �?  [SUBSCRIPT NINE]
          case '\u2468': // �?  [CIRCLED DIGIT NINE]
          case '\u24FD': // �?  [DOUBLE CIRCLED DIGIT NINE]
          case '\u277E': // �?  [DINGBAT NEGATIVE CIRCLED DIGIT NINE]
          case '\u2788': // �?  [DINGBAT CIRCLED SANS-SERIF DIGIT NINE]
          case '\u2792': // �?  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT NINE]
          case '\uFF19': // �?  [FULLWIDTH DIGIT NINE]
            output[outputPos++] = '9';
            break;
          case '\u2490': // �?  [DIGIT NINE FULL STOP]
            output[outputPos++] = '9';
            output[outputPos++] = '.';
            break;
          case '\u247C': // �?  [PARENTHESIZED DIGIT NINE]
            output[outputPos++] = '(';
            output[outputPos++] = '9';
            output[outputPos++] = ')';
            break;
          case '\u2469': // �?  [CIRCLED NUMBER TEN]
          case '\u24FE': // �?  [DOUBLE CIRCLED NUMBER TEN]
          case '\u277F': // �?  [DINGBAT NEGATIVE CIRCLED NUMBER TEN]
          case '\u2789': // �?  [DINGBAT CIRCLED SANS-SERIF NUMBER TEN]
          case '\u2793': // �?  [DINGBAT NEGATIVE CIRCLED SANS-SERIF NUMBER TEN]
            output[outputPos++] = '1';
            output[outputPos++] = '0';
            break;
          case '\u2491': // �?  [NUMBER TEN FULL STOP]
            output[outputPos++] = '1';
            output[outputPos++] = '0';
            output[outputPos++] = '.';
            break;
          case '\u247D': // �?  [PARENTHESIZED NUMBER TEN]
            output[outputPos++] = '(';
            output[outputPos++] = '1';
            output[outputPos++] = '0';
            output[outputPos++] = ')';
            break;
          case '\u246A': // �?  [CIRCLED NUMBER ELEVEN]
          case '\u24EB': // �?  [NEGATIVE CIRCLED NUMBER ELEVEN]
            output[outputPos++] = '1';
            output[outputPos++] = '1';
            break;
          case '\u2492': // �?  [NUMBER ELEVEN FULL STOP]
            output[outputPos++] = '1';
            output[outputPos++] = '1';
            output[outputPos++] = '.';
            break;
          case '\u247E': // �?  [PARENTHESIZED NUMBER ELEVEN]
            output[outputPos++] = '(';
            output[outputPos++] = '1';
            output[outputPos++] = '1';
            output[outputPos++] = ')';
            break;
          case '\u246B': // �?  [CIRCLED NUMBER TWELVE]
          case '\u24EC': // �?  [NEGATIVE CIRCLED NUMBER TWELVE]
            output[outputPos++] = '1';
            output[outputPos++] = '2';
            break;
          case '\u2493': // �?  [NUMBER TWELVE FULL STOP]
            output[outputPos++] = '1';
            output[outputPos++] = '2';
            output[outputPos++] = '.';
            break;
          case '\u247F': // �?  [PARENTHESIZED NUMBER TWELVE]
            output[outputPos++] = '(';
            output[outputPos++] = '1';
            output[outputPos++] = '2';
            output[outputPos++] = ')';
            break;
          case '\u246C': // �?  [CIRCLED NUMBER THIRTEEN]
          case '\u24ED': // �?  [NEGATIVE CIRCLED NUMBER THIRTEEN]
            output[outputPos++] = '1';
            output[outputPos++] = '3';
            break;
          case '\u2494': // �?  [NUMBER THIRTEEN FULL STOP]
            output[outputPos++] = '1';
            output[outputPos++] = '3';
            output[outputPos++] = '.';
            break;
          case '\u2480': // �?  [PARENTHESIZED NUMBER THIRTEEN]
            output[outputPos++] = '(';
            output[outputPos++] = '1';
            output[outputPos++] = '3';
            output[outputPos++] = ')';
            break;
          case '\u246D': // �?  [CIRCLED NUMBER FOURTEEN]
          case '\u24EE': // �?  [NEGATIVE CIRCLED NUMBER FOURTEEN]
            output[outputPos++] = '1';
            output[outputPos++] = '4';
            break;
          case '\u2495': // �?  [NUMBER FOURTEEN FULL STOP]
            output[outputPos++] = '1';
            output[outputPos++] = '4';
            output[outputPos++] = '.';
            break;
          case '\u2481': // �?  [PARENTHESIZED NUMBER FOURTEEN]
            output[outputPos++] = '(';
            output[outputPos++] = '1';
            output[outputPos++] = '4';
            output[outputPos++] = ')';
            break;
          case '\u246E': // �?  [CIRCLED NUMBER FIFTEEN]
          case '\u24EF': // �?  [NEGATIVE CIRCLED NUMBER FIFTEEN]
            output[outputPos++] = '1';
            output[outputPos++] = '5';
            break;
          case '\u2496': // �?  [NUMBER FIFTEEN FULL STOP]
            output[outputPos++] = '1';
            output[outputPos++] = '5';
            output[outputPos++] = '.';
            break;
          case '\u2482': // �?  [PARENTHESIZED NUMBER FIFTEEN]
            output[outputPos++] = '(';
            output[outputPos++] = '1';
            output[outputPos++] = '5';
            output[outputPos++] = ')';
            break;
          case '\u246F': // �?  [CIRCLED NUMBER SIXTEEN]
          case '\u24F0': // �?  [NEGATIVE CIRCLED NUMBER SIXTEEN]
            output[outputPos++] = '1';
            output[outputPos++] = '6';
            break;
          case '\u2497': // �?  [NUMBER SIXTEEN FULL STOP]
            output[outputPos++] = '1';
            output[outputPos++] = '6';
            output[outputPos++] = '.';
            break;
          case '\u2483': // �?  [PARENTHESIZED NUMBER SIXTEEN]
            output[outputPos++] = '(';
            output[outputPos++] = '1';
            output[outputPos++] = '6';
            output[outputPos++] = ')';
            break;
          case '\u2470': // �?  [CIRCLED NUMBER SEVENTEEN]
          case '\u24F1': // �?  [NEGATIVE CIRCLED NUMBER SEVENTEEN]
            output[outputPos++] = '1';
            output[outputPos++] = '7';
            break;
          case '\u2498': // �?  [NUMBER SEVENTEEN FULL STOP]
            output[outputPos++] = '1';
            output[outputPos++] = '7';
            output[outputPos++] = '.';
            break;
          case '\u2484': // �?  [PARENTHESIZED NUMBER SEVENTEEN]
            output[outputPos++] = '(';
            output[outputPos++] = '1';
            output[outputPos++] = '7';
            output[outputPos++] = ')';
            break;
          case '\u2471': // �?  [CIRCLED NUMBER EIGHTEEN]
          case '\u24F2': // �?  [NEGATIVE CIRCLED NUMBER EIGHTEEN]
            output[outputPos++] = '1';
            output[outputPos++] = '8';
            break;
          case '\u2499': // �?  [NUMBER EIGHTEEN FULL STOP]
            output[outputPos++] = '1';
            output[outputPos++] = '8';
            output[outputPos++] = '.';
            break;
          case '\u2485': // �?  [PARENTHESIZED NUMBER EIGHTEEN]
            output[outputPos++] = '(';
            output[outputPos++] = '1';
            output[outputPos++] = '8';
            output[outputPos++] = ')';
            break;
          case '\u2472': // �?  [CIRCLED NUMBER NINETEEN]
          case '\u24F3': // �?  [NEGATIVE CIRCLED NUMBER NINETEEN]
            output[outputPos++] = '1';
            output[outputPos++] = '9';
            break;
          case '\u249A': // �?  [NUMBER NINETEEN FULL STOP]
            output[outputPos++] = '1';
            output[outputPos++] = '9';
            output[outputPos++] = '.';
            break;
          case '\u2486': // �?  [PARENTHESIZED NUMBER NINETEEN]
            output[outputPos++] = '(';
            output[outputPos++] = '1';
            output[outputPos++] = '9';
            output[outputPos++] = ')';
            break;
          case '\u2473': // �?  [CIRCLED NUMBER TWENTY]
          case '\u24F4': // �?  [NEGATIVE CIRCLED NUMBER TWENTY]
            output[outputPos++] = '2';
            output[outputPos++] = '0';
            break;
          case '\u249B': // �?  [NUMBER TWENTY FULL STOP]
            output[outputPos++] = '2';
            output[outputPos++] = '0';
            output[outputPos++] = '.';
            break;
          case '\u2487': // �?  [PARENTHESIZED NUMBER TWENTY]
            output[outputPos++] = '(';
            output[outputPos++] = '2';
            output[outputPos++] = '0';
            output[outputPos++] = ')';
            break;
          case '\u00AB': // «  [LEFT-POINTING DOUBLE ANGLE QUOTATION MARK]
          case '\u00BB': // »  [RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK]
          case '\u201C': // �?  [LEFT DOUBLE QUOTATION MARK]
          case '\u201D': // �?  [RIGHT DOUBLE QUOTATION MARK]
          case '\u201E': // �?  [DOUBLE LOW-9 QUOTATION MARK]
          case '\u2033': // �?  [DOUBLE PRIME]
          case '\u2036': // �?  [REVERSED DOUBLE PRIME]
          case '\u275D': // �?  [HEAVY DOUBLE TURNED COMMA QUOTATION MARK ORNAMENT]
          case '\u275E': // �?  [HEAVY DOUBLE COMMA QUOTATION MARK ORNAMENT]
          case '\u276E': // �?  [HEAVY LEFT-POINTING ANGLE QUOTATION MARK ORNAMENT]
          case '\u276F': // �?  [HEAVY RIGHT-POINTING ANGLE QUOTATION MARK ORNAMENT]
          case '\uFF02': // �?  [FULLWIDTH QUOTATION MARK]
            output[outputPos++] = '"';
            break;
          case '\u2018': // �?  [LEFT SINGLE QUOTATION MARK]
          case '\u2019': // �?  [RIGHT SINGLE QUOTATION MARK]
          case '\u201A': // �?  [SINGLE LOW-9 QUOTATION MARK]
          case '\u201B': // �?  [SINGLE HIGH-REVERSED-9 QUOTATION MARK]
          case '\u2032': // �?  [PRIME]
          case '\u2035': // �?  [REVERSED PRIME]
          case '\u2039': // �?  [SINGLE LEFT-POINTING ANGLE QUOTATION MARK]
          case '\u203A': // �?  [SINGLE RIGHT-POINTING ANGLE QUOTATION MARK]
          case '\u275B': // �?  [HEAVY SINGLE TURNED COMMA QUOTATION MARK ORNAMENT]
          case '\u275C': // �?  [HEAVY SINGLE COMMA QUOTATION MARK ORNAMENT]
          case '\uFF07': // �?  [FULLWIDTH APOSTROPHE]
            output[outputPos++] = '\'';
            break;
          case '\u2010': // �?  [HYPHEN]
          case '\u2011': // �?  [NON-BREAKING HYPHEN]
          case '\u2012': // �?  [FIGURE DASH]
          case '\u2013': // �?  [EN DASH]
          case '\u2014': // �?  [EM DASH]
          case '\u207B': // �?  [SUPERSCRIPT MINUS]
          case '\u208B': // �?  [SUBSCRIPT MINUS]
          case '\uFF0D': // �?  [FULLWIDTH HYPHEN-MINUS]
            output[outputPos++] = '-';
            break;
          case '\u2045': // �?  [LEFT SQUARE BRACKET WITH QUILL]
          case '\u2772': // �?  [LIGHT LEFT TORTOISE SHELL BRACKET ORNAMENT]
          case '\uFF3B': // �?  [FULLWIDTH LEFT SQUARE BRACKET]
            output[outputPos++] = '[';
            break;
          case '\u2046': // �?  [RIGHT SQUARE BRACKET WITH QUILL]
          case '\u2773': // �?  [LIGHT RIGHT TORTOISE SHELL BRACKET ORNAMENT]
          case '\uFF3D': // �?  [FULLWIDTH RIGHT SQUARE BRACKET]
            output[outputPos++] = ']';
            break;
          case '\u207D': // �?  [SUPERSCRIPT LEFT PARENTHESIS]
          case '\u208D': // �?  [SUBSCRIPT LEFT PARENTHESIS]
          case '\u2768': // �?  [MEDIUM LEFT PARENTHESIS ORNAMENT]
          case '\u276A': // �?  [MEDIUM FLATTENED LEFT PARENTHESIS ORNAMENT]
          case '\uFF08': // �?  [FULLWIDTH LEFT PARENTHESIS]
            output[outputPos++] = '(';
            break;
          case '\u2E28': // �?  [LEFT DOUBLE PARENTHESIS]
            output[outputPos++] = '(';
            output[outputPos++] = '(';
            break;
          case '\u207E': // �?  [SUPERSCRIPT RIGHT PARENTHESIS]
          case '\u208E': // �?  [SUBSCRIPT RIGHT PARENTHESIS]
          case '\u2769': // �?  [MEDIUM RIGHT PARENTHESIS ORNAMENT]
          case '\u276B': // �?  [MEDIUM FLATTENED RIGHT PARENTHESIS ORNAMENT]
          case '\uFF09': // �?  [FULLWIDTH RIGHT PARENTHESIS]
            output[outputPos++] = ')';
            break;
          case '\u2E29': // �?  [RIGHT DOUBLE PARENTHESIS]
            output[outputPos++] = ')';
            output[outputPos++] = ')';
            break;
          case '\u276C': // �?  [MEDIUM LEFT-POINTING ANGLE BRACKET ORNAMENT]
          case '\u2770': // �?  [HEAVY LEFT-POINTING ANGLE BRACKET ORNAMENT]
          case '\uFF1C': // �?  [FULLWIDTH LESS-THAN SIGN]
            output[outputPos++] = '<';
            break;
          case '\u276D': // �?  [MEDIUM RIGHT-POINTING ANGLE BRACKET ORNAMENT]
          case '\u2771': // �?  [HEAVY RIGHT-POINTING ANGLE BRACKET ORNAMENT]
          case '\uFF1E': // �?  [FULLWIDTH GREATER-THAN SIGN]
            output[outputPos++] = '>';
            break;
          case '\u2774': // �?  [MEDIUM LEFT CURLY BRACKET ORNAMENT]
          case '\uFF5B': // �?  [FULLWIDTH LEFT CURLY BRACKET]
            output[outputPos++] = '{';
            break;
          case '\u2775': // �?  [MEDIUM RIGHT CURLY BRACKET ORNAMENT]
          case '\uFF5D': // �?  [FULLWIDTH RIGHT CURLY BRACKET]
            output[outputPos++] = '}';
            break;
          case '\u207A': // �?  [SUPERSCRIPT PLUS SIGN]
          case '\u208A': // �?  [SUBSCRIPT PLUS SIGN]
          case '\uFF0B': // �?  [FULLWIDTH PLUS SIGN]
            output[outputPos++] = '+';
            break;
          case '\u207C': // �?  [SUPERSCRIPT EQUALS SIGN]
          case '\u208C': // �?  [SUBSCRIPT EQUALS SIGN]
          case '\uFF1D': // �?  [FULLWIDTH EQUALS SIGN]
            output[outputPos++] = '=';
            break;
          case '\uFF01': // �?  [FULLWIDTH EXCLAMATION MARK]
            output[outputPos++] = '!';
            break;
          case '\u203C': // �?  [DOUBLE EXCLAMATION MARK]
            output[outputPos++] = '!';
            output[outputPos++] = '!';
            break;
          case '\u2049': // �?  [EXCLAMATION QUESTION MARK]
            output[outputPos++] = '!';
            output[outputPos++] = '?';
            break;
          case '\uFF03': // �?  [FULLWIDTH NUMBER SIGN]
            output[outputPos++] = '#';
            break;
          case '\uFF04': // �?  [FULLWIDTH DOLLAR SIGN]
            output[outputPos++] = '$';
            break;
          case '\u2052': // �?  [COMMERCIAL MINUS SIGN]
          case '\uFF05': // �?  [FULLWIDTH PERCENT SIGN]
            output[outputPos++] = '%';
            break;
          case '\uFF06': // �?  [FULLWIDTH AMPERSAND]
            output[outputPos++] = '&';
            break;
          case '\u204E': // �?  [LOW ASTERISK]
          case '\uFF0A': // �?  [FULLWIDTH ASTERISK]
            output[outputPos++] = '*';
            break;
          case '\uFF0C': // �?  [FULLWIDTH COMMA]
            output[outputPos++] = ',';
            break;
          case '\uFF0E': // �?  [FULLWIDTH FULL STOP]
            output[outputPos++] = '.';
            break;
          case '\u2044': // �?  [FRACTION SLASH]
          case '\uFF0F': // �?  [FULLWIDTH SOLIDUS]
            output[outputPos++] = '/';
            break;
          case '\uFF1A': // �?  [FULLWIDTH COLON]
            output[outputPos++] = ':';
            break;
          case '\u204F': // �?  [REVERSED SEMICOLON]
          case '\uFF1B': // �?  [FULLWIDTH SEMICOLON]
            output[outputPos++] = ';';
            break;
          case '\uFF1F': // �?  [FULLWIDTH QUESTION MARK]
            output[outputPos++] = '?';
            break;
          case '\u2047': // �?  [DOUBLE QUESTION MARK]
            output[outputPos++] = '?';
            output[outputPos++] = '?';
            break;
          case '\u2048': // �?  [QUESTION EXCLAMATION MARK]
            output[outputPos++] = '?';
            output[outputPos++] = '!';
            break;
          case '\uFF20': // �?  [FULLWIDTH COMMERCIAL AT]
            output[outputPos++] = '@';
            break;
          case '\uFF3C': // �?  [FULLWIDTH REVERSE SOLIDUS]
            output[outputPos++] = '\\';
            break;
          case '\u2038': // �?  [CARET]
          case '\uFF3E': // �?  [FULLWIDTH CIRCUMFLEX ACCENT]
            output[outputPos++] = '^';
            break;
          case '\uFF3F': // �?  [FULLWIDTH LOW LINE]
            output[outputPos++] = '_';
            break;
          case '\u2053': // �?  [SWUNG DASH]
          case '\uFF5E': // �?  [FULLWIDTH TILDE]
            output[outputPos++] = '~';
            break;
          default:
            output[outputPos++] = c;
            break;
        }
      }
    }
    return outputPos;
  }
}
