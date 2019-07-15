import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.*;


import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * This class implements Word Break rules from the Unicode Text Segmentation 
 * algorithm, as specified in 
 * <a href="http://unicode.org/reports/tr29/">Unicode Standard Annex #29</a>. 
 * <p>
 * Tokens produced are of the following types:
 * <ul>
 *   <li>&lt;ALPHANUM&gt;: A sequence of alphabetic and numeric characters</li>
 *   <li>&lt;NUM&gt;: A number</li>
 *   <li>&lt;SOUTHEAST_ASIAN&gt;: A sequence of characters from South and Southeast
 *       Asian languages, including Thai, Lao, Myanmar, and Khmer</li>
 *   <li>&lt;IDEOGRAPHIC&gt;: A single CJKV ideographic character</li>
 *   <li>&lt;HIRAGANA&gt;: A single hiragana character</li>
 *   <li>&lt;KATAKANA&gt;: A sequence of katakana characters</li>
 *   <li>&lt;HANGUL&gt;: A sequence of Hangul characters</li>
 * </ul>
 */
@SuppressWarnings("fallthrough")

public final class StandardTokenizerImpl {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private int ZZ_BUFFERSIZE = 255;

  /** lexical states */
  public static final int YYINITIAL = 0;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = { 
     0, 0
  };

  /** 
   * Translates characters to character classes
   */
  private static final String ZZ_CMAP_PACKED = 
    "\42\0\1\15\4\0\1\14\4\0\1\7\1\0\1\10\1\0\12\4"+
    "\1\6\1\7\5\0\32\1\4\0\1\11\1\0\32\1\57\0\1\1"+
    "\2\0\1\3\7\0\1\1\1\0\1\6\2\0\1\1\5\0\27\1"+
    "\1\0\37\1\1\0\u01ca\1\4\0\14\1\5\0\1\6\10\0\5\1"+
    "\7\0\1\1\1\0\1\1\21\0\160\3\5\1\1\0\2\1\2\0"+
    "\4\1\1\7\7\0\1\1\1\6\3\1\1\0\1\1\1\0\24\1"+
    "\1\0\123\1\1\0\213\1\1\0\7\3\236\1\11\0\46\1\2\0"+
    "\1\1\7\0\47\1\1\0\1\7\7\0\55\3\1\0\1\3\1\0"+
    "\2\3\1\0\2\3\1\0\1\3\10\0\33\16\5\0\3\16\1\1"+
    "\1\6\13\0\5\3\7\0\2\7\2\0\13\3\1\0\1\3\3\0"+
    "\53\1\25\3\12\4\1\0\1\4\1\7\1\0\2\1\1\3\143\1"+
    "\1\0\1\1\10\3\1\0\6\3\2\1\2\3\1\0\4\3\2\1"+
    "\12\4\3\1\2\0\1\1\17\0\1\3\1\1\1\3\36\1\33\3"+
    "\2\0\131\1\13\3\1\1\16\0\12\4\41\1\11\3\2\1\2\0"+
    "\1\7\1\0\1\1\5\0\26\1\4\3\1\1\11\3\1\1\3\3"+
    "\1\1\5\3\22\0\31\1\3\3\104\0\1\1\1\0\13\1\67\0"+
    "\33\3\1\0\4\3\66\1\3\3\1\1\22\3\1\1\7\3\12\1"+
    "\2\3\2\0\12\4\1\0\7\1\1\0\7\1\1\0\3\3\1\0"+
    "\10\1\2\0\2\1\2\0\26\1\1\0\7\1\1\0\1\1\3\0"+
    "\4\1\2\0\1\3\1\1\7\3\2\0\2\3\2\0\3\3\1\1"+
    "\10\0\1\3\4\0\2\1\1\0\3\1\2\3\2\0\12\4\2\1"+
    "\17\0\3\3\1\0\6\1\4\0\2\1\2\0\26\1\1\0\7\1"+
    "\1\0\2\1\1\0\2\1\1\0\2\1\2\0\1\3\1\0\5\3"+
    "\4\0\2\3\2\0\3\3\3\0\1\3\7\0\4\1\1\0\1\1"+
    "\7\0\12\4\2\3\3\1\1\3\13\0\3\3\1\0\11\1\1\0"+
    "\3\1\1\0\26\1\1\0\7\1\1\0\2\1\1\0\5\1\2\0"+
    "\1\3\1\1\10\3\1\0\3\3\1\0\3\3\2\0\1\1\17\0"+
    "\2\1\2\3\2\0\12\4\21\0\3\3\1\0\10\1\2\0\2\1"+
    "\2\0\26\1\1\0\7\1\1\0\2\1\1\0\5\1\2\0\1\3"+
    "\1\1\7\3\2\0\2\3\2\0\3\3\10\0\2\3\4\0\2\1"+
    "\1\0\3\1\2\3\2\0\12\4\1\0\1\1\20\0\1\3\1\1"+
    "\1\0\6\1\3\0\3\1\1\0\4\1\3\0\2\1\1\0\1\1"+
    "\1\0\2\1\3\0\2\1\3\0\3\1\3\0\14\1\4\0\5\3"+
    "\3\0\3\3\1\0\4\3\2\0\1\1\6\0\1\3\16\0\12\4"+
    "\21\0\3\3\1\0\10\1\1\0\3\1\1\0\27\1\1\0\12\1"+
    "\1\0\5\1\3\0\1\1\7\3\1\0\3\3\1\0\4\3\7\0"+
    "\2\3\1\0\2\1\6\0\2\1\2\3\2\0\12\4\22\0\2\3"+
    "\1\0\10\1\1\0\3\1\1\0\27\1\1\0\12\1\1\0\5\1"+
    "\2\0\1\3\1\1\7\3\1\0\3\3\1\0\4\3\7\0\2\3"+
    "\7\0\1\1\1\0\2\1\2\3\2\0\12\4\1\0\2\1\17\0"+
    "\2\3\1\0\10\1\1\0\3\1\1\0\51\1\2\0\1\1\7\3"+
    "\1\0\3\3\1\0\4\3\1\1\10\0\1\3\10\0\2\1\2\3"+
    "\2\0\12\4\12\0\6\1\2\0\2\3\1\0\22\1\3\0\30\1"+
    "\1\0\11\1\1\0\1\1\2\0\7\1\3\0\1\3\4\0\6\3"+
    "\1\0\1\3\1\0\10\3\22\0\2\3\15\0\60\20\1\21\2\20"+
    "\7\21\5\0\7\20\10\21\1\0\12\4\47\0\2\20\1\0\1\20"+
    "\2\0\2\20\1\0\1\20\2\0\1\20\6\0\4\20\1\0\7\20"+
    "\1\0\3\20\1\0\1\20\1\0\1\20\2\0\2\20\1\0\4\20"+
    "\1\21\2\20\6\21\1\0\2\21\1\20\2\0\5\20\1\0\1\20"+
    "\1\0\6\21\2\0\12\4\2\0\4\20\40\0\1\1\27\0\2\3"+
    "\6\0\12\4\13\0\1\3\1\0\1\3\1\0\1\3\4\0\2\3"+
    "\10\1\1\0\44\1\4\0\24\3\1\0\2\3\5\1\13\3\1\0"+
    "\44\3\11\0\1\3\71\0\53\20\24\21\1\20\12\4\6\0\6\20"+
    "\4\21\4\20\3\21\1\20\3\21\2\20\7\21\3\20\4\21\15\20"+
    "\14\21\1\20\1\21\12\4\4\21\2\20\46\1\1\0\1\1\5\0"+
    "\1\1\2\0\53\1\1\0\4\1\u0100\2\111\1\1\0\4\1\2\0"+
    "\7\1\1\0\1\1\1\0\4\1\2\0\51\1\1\0\4\1\2\0"+
    "\41\1\1\0\4\1\2\0\7\1\1\0\1\1\1\0\4\1\2\0"+
    "\17\1\1\0\71\1\1\0\4\1\2\0\103\1\2\0\3\3\40\0"+
    "\20\1\20\0\125\1\14\0\u026c\1\2\0\21\1\1\0\32\1\5\0"+
    "\113\1\3\0\3\1\17\0\15\1\1\0\4\1\3\3\13\0\22\1"+
    "\3\3\13\0\22\1\2\3\14\0\15\1\1\0\3\1\1\0\2\3"+
    "\14\0\64\20\40\21\3\0\1\20\4\0\1\20\1\21\2\0\12\4"+
    "\41\0\4\3\1\0\12\4\6\0\130\1\10\0\51\1\1\3\1\1"+
    "\5\0\106\1\12\0\35\1\3\0\14\3\4\0\14\3\12\0\12\4"+
    "\36\20\2\0\5\20\13\0\54\20\4\0\21\21\7\20\2\21\6\0"+
    "\12\4\1\20\3\0\2\20\40\0\27\1\5\3\4\0\65\20\12\21"+
    "\1\0\35\21\2\0\1\3\12\4\6\0\12\4\6\0\16\20\122\0"+
    "\5\3\57\1\21\3\7\1\4\0\12\4\21\0\11\3\14\0\3\3"+
    "\36\1\15\3\2\1\12\4\54\1\16\3\14\0\44\1\24\3\10\0"+
    "\12\4\3\0\3\1\12\4\44\1\122\0\3\3\1\0\25\3\4\1"+
    "\1\3\4\1\3\3\2\1\11\0\300\1\47\3\25\0\4\3\u0116\1"+
    "\2\0\6\1\2\0\46\1\2\0\6\1\2\0\10\1\1\0\1\1"+
    "\1\0\1\1\1\0\1\1\1\0\37\1\2\0\65\1\1\0\7\1"+
    "\1\0\1\1\3\0\3\1\1\0\7\1\3\0\4\1\2\0\6\1"+
    "\4\0\15\1\5\0\3\1\1\0\7\1\17\0\4\3\10\0\2\10"+
    "\12\0\1\10\2\0\1\6\2\0\5\3\20\0\2\11\3\0\1\7"+
    "\17\0\1\11\13\0\5\3\1\0\12\3\1\0\1\1\15\0\1\1"+
    "\20\0\15\1\63\0\41\3\21\0\1\1\4\0\1\1\2\0\12\1"+
    "\1\0\1\1\3\0\5\1\6\0\1\1\1\0\1\1\1\0\1\1"+
    "\1\0\4\1\1\0\13\1\2\0\4\1\5\0\5\1\4\0\1\1"+
    "\21\0\51\1\u032d\0\64\1\u0716\0\57\1\1\0\57\1\1\0\205\1"+
    "\6\0\4\1\3\3\2\1\14\0\46\1\1\0\1\1\5\0\1\1"+
    "\2\0\70\1\7\0\1\1\17\0\1\3\27\1\11\0\7\1\1\0"+
    "\7\1\1\0\7\1\1\0\7\1\1\0\7\1\1\0\7\1\1\0"+
    "\7\1\1\0\7\1\1\0\40\3\57\0\1\1\120\0\32\12\1\0"+
    "\131\12\14\0\326\12\57\0\1\1\1\0\1\12\31\0\11\12\6\3"+
    "\1\0\5\5\2\0\3\12\1\1\1\1\4\0\126\13\2\0\2\3"+
    "\2\5\3\13\133\5\1\0\4\5\5\0\51\1\3\0\136\2\21\0"+
    "\33\1\65\0\20\5\320\0\57\5\1\0\130\5\250\0\u19b6\12\112\0"+
    "\u51cd\12\63\0\u048d\1\103\0\56\1\2\0\u010d\1\3\0\20\1\12\4"+
    "\2\1\24\0\57\1\4\3\1\0\12\3\1\0\31\1\7\0\1\3"+
    "\120\1\2\3\45\0\11\1\2\0\147\1\2\0\4\1\1\0\4\1"+
    "\14\0\13\1\115\0\12\1\1\3\3\1\1\3\4\1\1\3\27\1"+
    "\5\3\30\0\64\1\14\0\2\3\62\1\21\3\13\0\12\4\6\0"+
    "\22\3\6\1\3\0\1\1\4\0\12\4\34\1\10\3\2\0\27\1"+
    "\15\3\14\0\35\2\3\0\4\3\57\1\16\3\16\0\1\1\12\4"+
    "\46\0\51\1\16\3\11\0\3\1\1\3\10\1\2\3\2\0\12\4"+
    "\6\0\33\20\1\21\4\0\60\20\1\21\1\20\3\21\2\20\2\21"+
    "\5\20\2\21\1\20\1\21\1\20\30\0\5\20\13\1\5\3\2\0"+
    "\3\1\2\3\12\0\6\1\2\0\6\1\2\0\6\1\11\0\7\1"+
    "\1\0\7\1\221\0\43\1\10\3\1\0\2\3\2\0\12\4\6\0"+
    "\u2ba4\2\14\0\27\2\4\0\61\2\u2104\0\u016e\12\2\0\152\12\46\0"+
    "\7\1\14\0\5\1\5\0\1\16\1\3\12\16\1\0\15\16\1\0"+
    "\5\16\1\0\1\16\1\0\2\16\1\0\2\16\1\0\12\16\142\1"+
    "\41\0\u016b\1\22\0\100\1\2\0\66\1\50\0\14\1\4\0\20\3"+
    "\1\7\2\0\1\6\1\7\13\0\7\3\14\0\2\11\30\0\3\11"+
    "\1\7\1\0\1\10\1\0\1\7\1\6\32\0\5\1\1\0\207\1"+
    "\2\0\1\3\7\0\1\10\4\0\1\7\1\0\1\10\1\0\12\4"+
    "\1\6\1\7\5\0\32\1\4\0\1\11\1\0\32\1\13\0\70\5"+
    "\2\3\37\2\3\0\6\2\2\0\6\2\2\0\6\2\2\0\3\2"+
    "\34\0\3\3\4\0\14\1\1\0\32\1\1\0\23\1\1\0\2\1"+
    "\1\0\17\1\2\0\16\1\42\0\173\1\105\0\65\1\210\0\1\3"+
    "\202\0\35\1\3\0\61\1\57\0\37\1\21\0\33\1\65\0\36\1"+
    "\2\0\44\1\4\0\10\1\1\0\5\1\52\0\236\1\2\0\12\4"+
    "\u0356\0\6\1\2\0\1\1\1\0\54\1\1\0\2\1\3\0\1\1"+
    "\2\0\27\1\252\0\26\1\12\0\32\1\106\0\70\1\6\0\2\1"+
    "\100\0\1\1\3\3\1\0\2\3\5\0\4\3\4\1\1\0\3\1"+
    "\1\0\33\1\4\0\3\3\4\0\1\3\40\0\35\1\203\0\66\1"+
    "\12\0\26\1\12\0\23\1\215\0\111\1\u03b7\0\3\3\65\1\17\3"+
    "\37\0\12\4\20\0\3\3\55\1\13\3\2\0\1\3\22\0\31\1"+
    "\7\0\12\4\6\0\3\3\44\1\16\3\1\0\12\4\100\0\3\3"+
    "\60\1\16\3\4\1\13\0\12\4\u04a6\0\53\1\15\3\10\0\12\4"+
    "\u0936\0\u036f\1\221\0\143\1\u0b9d\0\u042f\1\u33d1\0\u0239\1\u04c7\0\105\1"+
    "\13\0\1\1\56\3\20\0\4\3\15\1\u4060\0\1\5\1\13\u2163\0"+
    "\5\3\3\0\26\3\2\0\7\3\36\0\4\3\224\0\3\3\u01bb\0"+
    "\125\1\1\0\107\1\1\0\2\1\2\0\1\1\2\0\2\1\2\0"+
    "\4\1\1\0\14\1\1\0\1\1\1\0\7\1\1\0\101\1\1\0"+
    "\4\1\2\0\10\1\1\0\7\1\1\0\34\1\1\0\4\1\1\0"+
    "\5\1\1\0\1\1\3\0\7\1\1\0\u0154\1\2\0\31\1\1\0"+
    "\31\1\1\0\37\1\1\0\31\1\1\0\37\1\1\0\31\1\1\0"+
    "\37\1\1\0\31\1\1\0\37\1\1\0\31\1\1\0\10\1\2\0"+
    "\62\4\u1600\0\4\1\1\0\33\1\1\0\2\1\1\0\1\1\2\0"+
    "\1\1\1\0\12\1\1\0\4\1\1\0\1\1\1\0\1\1\6\0"+
    "\1\1\4\0\1\1\1\0\1\1\1\0\1\1\1\0\3\1\1\0"+
    "\2\1\1\0\1\1\2\0\1\1\1\0\1\1\1\0\1\1\1\0"+
    "\1\1\1\0\1\1\1\0\2\1\1\0\1\1\2\0\4\1\1\0"+
    "\7\1\1\0\4\1\1\0\4\1\1\0\1\1\1\0\12\1\1\0"+
    "\21\1\5\0\3\1\1\0\5\1\1\0\21\1\u032a\0\32\17\1\13"+
    "\u0dff\0\ua6d7\12\51\0\u1035\12\13\0\336\12\u3fe2\0\u021e\12\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\u05ee\0"+
    "\1\3\36\0\140\3\200\0\360\3\uffff\0\uffff\0\ufe12\0";

  /** 
   * Translates characters to character classes
   */
  private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

  /** 
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\1\0\1\1\1\2\1\3\1\4\1\5\1\1\1\6"+
    "\1\7\1\2\1\1\1\10\1\2\1\0\1\2\1\0"+
    "\1\4\1\0\2\2\2\0\1\1\1\0";

  private static int [] zzUnpackAction() {
    int [] result = new int[24];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /** 
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\22\0\44\0\66\0\110\0\132\0\154\0\176"+
    "\0\220\0\242\0\264\0\306\0\330\0\352\0\374\0\u010e"+
    "\0\u0120\0\154\0\u0132\0\u0144\0\u0156\0\264\0\u0168\0\u017a";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[24];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /** 
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpackTrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\2\1\3\1\4\1\2\1\5\1\6\3\2\1\7"+
    "\1\10\1\11\2\2\1\12\1\13\2\14\23\0\3\3"+
    "\1\15\1\0\1\16\1\0\1\16\1\17\2\0\1\16"+
    "\1\0\1\12\2\0\1\3\1\0\1\3\2\4\1\15"+
    "\1\0\1\16\1\0\1\16\1\17\2\0\1\16\1\0"+
    "\1\12\2\0\1\4\1\0\2\3\2\5\2\0\2\20"+
    "\1\21\2\0\1\20\1\0\1\12\2\0\1\5\3\0"+
    "\1\6\1\0\1\6\3\0\1\17\7\0\1\6\1\0"+
    "\2\3\1\22\1\5\1\23\3\0\1\22\4\0\1\12"+
    "\2\0\1\22\3\0\1\10\15\0\1\10\3\0\1\11"+
    "\15\0\1\11\1\0\2\3\1\12\1\15\1\0\1\16"+
    "\1\0\1\16\1\17\2\0\1\24\1\25\1\12\2\0"+
    "\1\12\3\0\1\26\13\0\1\27\1\0\1\26\3\0"+
    "\1\14\14\0\2\14\1\0\2\3\2\15\2\0\2\30"+
    "\1\17\2\0\1\30\1\0\1\12\2\0\1\15\1\0"+
    "\2\3\1\16\12\0\1\3\2\0\1\16\1\0\2\3"+
    "\1\17\1\15\1\23\3\0\1\17\4\0\1\12\2\0"+
    "\1\17\3\0\1\20\1\5\14\0\1\20\1\0\2\3"+
    "\1\21\1\5\1\23\3\0\1\21\4\0\1\12\2\0"+
    "\1\21\3\0\1\23\1\0\1\23\3\0\1\17\7\0"+
    "\1\23\1\0\2\3\1\24\1\15\4\0\1\17\4\0"+
    "\1\12\2\0\1\24\3\0\1\25\12\0\1\24\2\0"+
    "\1\25\3\0\1\27\13\0\1\27\1\0\1\27\3\0"+
    "\1\30\1\15\14\0\1\30";

  private static int [] zzUnpackTrans() {
    int [] result = new int[396];
    int offset = 0;
    offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackTrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String ZZ_ERROR_MSG[] = {
    "Unkown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\1\0\1\11\13\1\1\0\1\1\1\0\1\1\1\0"+
    "\2\1\2\0\1\1\1\0";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[24];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /** number of newlines encountered up to the start of the matched text */
  private int yyline;

  /** the number of characters up to the start of the matched text */
  private int yychar;

  /**
   * the number of characters from the last newline up to the start of the 
   * matched text
   */
  private int yycolumn;

  /** 
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** denotes if the user-EOF-code has already been executed */
  private boolean zzEOFDone;
  
  /** 
   * The number of occupied positions in zzBuffer beyond zzEndRead.
   * When a lead/high surrogate has been read from the input stream
   * into the final zzBuffer position, this will have a value of 1;
   * otherwise, it will have a value of 0.
   */
  private int zzFinalHighSurrogate = 0;

  /* user code: */
  /** Alphanumeric sequences */
  public static final int WORD_TYPE = StandardTokenizer.ALPHANUM;
  
  /** Numbers */
  public static final int NUMERIC_TYPE = StandardTokenizer.NUM;
  
  /**
   * Chars in class \p{Line_Break = Complex_Context} are from South East Asian
   * scripts (Thai, Lao, Myanmar, Khmer, etc.).  Sequences of these are kept 
   * together as as a single token rather than broken up, because the logic
   * required to break them at word boundaries is too complex for UAX#29.
   * <p>
   * See Unicode Line Breaking Algorithm: http://www.unicode.org/reports/tr14/#SA
   */
  public static final int SOUTH_EAST_ASIAN_TYPE = StandardTokenizer.SOUTHEAST_ASIAN;
  
  /** Ideographic token type */
  public static final int IDEOGRAPHIC_TYPE = StandardTokenizer.IDEOGRAPHIC;
  
  /** Hiragana token type */
  public static final int HIRAGANA_TYPE = StandardTokenizer.HIRAGANA;
  
  /** Katakana token type */
  public static final int KATAKANA_TYPE = StandardTokenizer.KATAKANA;

  /** Hangul token type */
  public static final int HANGUL_TYPE = StandardTokenizer.HANGUL;

  /** Character count processed so far */
  public final int yychar()
  {
    return yychar;
  }

  /**
   * Fills CharTermAttribute with the current token text.
   */
  public final void getText(CharTermAttribute t) {
    t.copyBuffer(zzBuffer, zzStartRead, zzMarkedPos-zzStartRead);
  }
  
  /**
   * Sets the scanner buffer size in chars
   */
   public final void setBufferSize(int numChars) {
     ZZ_BUFFERSIZE = numChars;
     char[] newZzBuffer = new char[ZZ_BUFFERSIZE];
     System.arraycopy(zzBuffer, 0, newZzBuffer, 0, Math.min(zzBuffer.length, ZZ_BUFFERSIZE));
     zzBuffer = newZzBuffer;
   }


  /**
   * Creates a new scanner
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public StandardTokenizerImpl(java.io.Reader in) {
    this.zzReader = in;
  }


  /** 
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] zzUnpackCMap(String packed) {
    char [] map = new char[0x110000];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < 2836) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }


  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   * 
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {

    /* first: make room (if you can) */
    if (zzStartRead > 0) {
      zzEndRead += zzFinalHighSurrogate;
      zzFinalHighSurrogate = 0;
      System.arraycopy(zzBuffer, zzStartRead,
                       zzBuffer, 0,
                       zzEndRead-zzStartRead);

      /* translate stored positions */
      zzEndRead-= zzStartRead;
      zzCurrentPos-= zzStartRead;
      zzMarkedPos-= zzStartRead;
      zzStartRead = 0;
    }


    /* fill the buffer with new input */
    int requested = zzBuffer.length - zzEndRead - zzFinalHighSurrogate;           
    int totalRead = 0;
    while (totalRead < requested) {
      int numRead = zzReader.read(zzBuffer, zzEndRead + totalRead, requested - totalRead);
      if (numRead == -1) {
        break;
      }
      totalRead += numRead;
    }

    if (totalRead > 0) {
      zzEndRead += totalRead;
      if (totalRead == requested) { /* possibly more input available */
        if (Character.isHighSurrogate(zzBuffer[zzEndRead - 1])) {
          --zzEndRead;
          zzFinalHighSurrogate = 1;
          if (totalRead == 1) { return true; }
        }
      }
      return false;
    }

    // totalRead = 0: End of stream
    return true;
  }

    
  /**
   * Closes the input stream.
   */
  public final void yyclose() throws java.io.IOException {
    zzAtEOF = true;            /* indicate end of file */
    zzEndRead = zzStartRead;  /* invalidate buffer    */

    if (zzReader != null)
      zzReader.close();
  }


  /**
   * Resets the scanner to read from a new input stream.
   * Does not close the old reader.
   *
   * All internal variables are reset, the old input stream 
   * <b>cannot</b> be reused (internal buffer is discarded and lost).
   * Lexical state is set to <tt>ZZ_INITIAL</tt>.
   *
   * Internal scan buffer is resized down to its initial length, if it has grown.
   *
   * @param reader   the new input stream 
   */
  public final void yyreset(java.io.Reader reader) {
    zzReader = reader;
    zzAtBOL  = true;
    zzAtEOF  = false;
    zzEOFDone = false;
    zzEndRead = zzStartRead = 0;
    zzCurrentPos = zzMarkedPos = 0;
    zzFinalHighSurrogate = 0;
    yyline = yychar = yycolumn = 0;
    zzLexicalState = YYINITIAL;
    if (zzBuffer.length > ZZ_BUFFERSIZE)
      zzBuffer = new char[ZZ_BUFFERSIZE];
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final String yytext() {
    return new String( zzBuffer, zzStartRead, zzMarkedPos-zzStartRead );
  }


  /**
   * Returns the character at position <tt>pos</tt> from the 
   * matched text. 
   * 
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch. 
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer[zzStartRead+pos];
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occured while scanning.
   *
   * In a wellformed scanner (no or only correct usage of 
   * yypushback(int) and a match-all fallback rule) this method 
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  } 


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public int getNextToken() throws java.io.IOException {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    char [] zzBufferL = zzBuffer;
    char [] zzCMapL = ZZ_CMAP;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      yychar+= zzMarkedPosL-zzStartRead;

      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;
  
      zzState = ZZ_LEXSTATE[zzLexicalState];

      // set up zzAction for empty match case:
      int zzAttributes = zzAttrL[zzState];
      if ( (zzAttributes & 1) == 1 ) {
        zzAction = zzState;
      }


      zzForAction: {
        while (true) {
    
          if (zzCurrentPosL < zzEndReadL) {
            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);
            zzCurrentPosL += Character.charCount(zzInput);
          }
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);
              zzCurrentPosL += Character.charCount(zzInput);
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
        case 1: 
          { /* Break so we don't hit fall-through warning: */ break; /* Not numeric, word, ideographic, hiragana, or SE Asian -- ignore it. */
          }
        case 9: break;
        case 2: 
          { return WORD_TYPE;
          }
        case 10: break;
        case 3: 
          { return HANGUL_TYPE;
          }
        case 11: break;
        case 4: 
          { return NUMERIC_TYPE;
          }
        case 12: break;
        case 5: 
          { return KATAKANA_TYPE;
          }
        case 13: break;
        case 6: 
          { return IDEOGRAPHIC_TYPE;
          }
        case 14: break;
        case 7: 
          { return HIRAGANA_TYPE;
          }
        case 15: break;
        case 8: 
          { return SOUTH_EAST_ASIAN_TYPE;
          }
        case 16: break;
        default: 
          if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
            zzAtEOF = true;
              {
                return YYEOF;
              }
          } 
          else {
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}