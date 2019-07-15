import org.apache.lucene.analysis.core.*;



import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/** 
 * Filters {@link LetterTokenizer} with {@link LowerCaseFilter} and {@link StopFilter}.
 */
public final class StopAnalyzer extends StopwordAnalyzerBase {
  
  /** An unmodifiable set containing some common English words that are not usually useful
  for searching.*/
  public static final CharArraySet ENGLISH_STOP_WORDS_SET = StandardAnalyzer.ENGLISH_STOP_WORDS_SET;
  
  /** Builds an analyzer which removes words in
   *  {@link #ENGLISH_STOP_WORDS_SET}.
   */
  public StopAnalyzer() {
    this(ENGLISH_STOP_WORDS_SET);
  }

  /** Builds an analyzer with the stop words from the given set.
   * @param stopWords Set of stop words */
  public StopAnalyzer(CharArraySet stopWords) {
    super(stopWords);
  }

  /** Builds an analyzer with the stop words from the given path.
   * @see WordlistLoader#getWordSet(Reader)
   * @param stopwordsFile File to load stop words from */
  public StopAnalyzer(Path stopwordsFile) throws IOException {
    this(loadStopwordSet(stopwordsFile));
  }

  /** Builds an analyzer with the stop words from the given reader.
   * @see WordlistLoader#getWordSet(Reader)
   * @param stopwords Reader to load stop words from */
  public StopAnalyzer(Reader stopwords) throws IOException {
    this(loadStopwordSet(stopwords));
  }

  /**
   * Creates
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
   * used to tokenize all the text in the provided {@link Reader}.
   * 
   * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
   *         built from a {@link LowerCaseTokenizer} filtered with
   *         {@link StopFilter}
   */
  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    final Tokenizer source = new LowerCaseTokenizer();
    return new TokenStreamComponents(source, new StopFilter(source, stopwords));
  }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    return new LowerCaseFilter(in);
  }
}

