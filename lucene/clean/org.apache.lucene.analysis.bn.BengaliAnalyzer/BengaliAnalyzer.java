import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.bn.*;



import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.DecimalDigitFilter;
import org.apache.lucene.analysis.in.IndicNormalizationFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.IOException;
import java.io.Reader;

/**
 * Analyzer for Bengali.
 */
public final class BengaliAnalyzer extends StopwordAnalyzerBase {
  private final CharArraySet stemExclusionSet;
  
  /**
   * File containing default Bengali stopwords.
   * 
   * Default stopword list is from http://members.unine.ch/jacques.savoy/clef/bengaliST.txt
   * The stopword list is BSD-Licensed.
   */
  public final static String DEFAULT_STOPWORD_FILE = "stopwords.txt";
  private static final String STOPWORDS_COMMENT = "#";
  
  /**
   * Returns an unmodifiable instance of the default stop-words set.
   * @return an unmodifiable instance of the default stop-words set.
   */
  public static CharArraySet getDefaultStopSet(){
    return DefaultSetHolder.DEFAULT_STOP_SET;
  }
  
  /**
   * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer class 
   * accesses the static final set the first time.;
   */
  private static class DefaultSetHolder {
    static final CharArraySet DEFAULT_STOP_SET;

    static {
      try {
        DEFAULT_STOP_SET = loadStopwordSet(false, BengaliAnalyzer.class, DEFAULT_STOPWORD_FILE, STOPWORDS_COMMENT);
      } catch (IOException ex) {
        throw new RuntimeException("Unable to load default stopword set");
      }
    }
  }
  
  /**
   * Builds an analyzer with the given stop words
   * 
   * @param stopwords a stopword set
   * @param stemExclusionSet a stemming exclusion set
   */
  public BengaliAnalyzer(CharArraySet stopwords, CharArraySet stemExclusionSet) {
    super(stopwords);
    this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
  }
  
  /**
   * Builds an analyzer with the given stop words 
   * 
   * @param stopwords a stopword set
   */
  public BengaliAnalyzer(CharArraySet stopwords) {
    this(stopwords, CharArraySet.EMPTY_SET);
  }
  
  /**
   * Builds an analyzer with the default stop words:
   * {@link #DEFAULT_STOPWORD_FILE}.
   */
  public BengaliAnalyzer() {
    this(DefaultSetHolder.DEFAULT_STOP_SET);
  }

  /**
   * Creates
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
   * used to tokenize all the text in the provided {@link Reader}.
   * 
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
   *         built from a {@link StandardTokenizer} filtered with
   *         {@link LowerCaseFilter}, {@link DecimalDigitFilter}, {@link IndicNormalizationFilter},
   *         {@link BengaliNormalizationFilter}, {@link SetKeywordMarkerFilter}
   *         if a stem exclusion set is provided, {@link BengaliStemFilter}, and
   *         Bengali Stop words
   */
  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    final Tokenizer source = new StandardTokenizer();
    TokenStream result = new LowerCaseFilter(source);
    result = new DecimalDigitFilter(result);
    if (!stemExclusionSet.isEmpty())
      result = new SetKeywordMarkerFilter(result, stemExclusionSet);
    result = new IndicNormalizationFilter(result);
    result = new BengaliNormalizationFilter(result);
    result = new StopFilter(result, stopwords);
    result = new BengaliStemFilter(result);
    return new TokenStreamComponents(source, result);
  }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new StandardFilter(in);
    result = new LowerCaseFilter(result);
    result = new DecimalDigitFilter(result);
    result = new IndicNormalizationFilter(result);
    result = new BengaliNormalizationFilter(result);
    return result;
  }
}
