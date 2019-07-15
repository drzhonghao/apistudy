import org.apache.lucene.analysis.ja.*;



import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;

/**
 * Analyzer for Japanese that uses morphological analysis.
 * @see JapaneseTokenizer
 */
public class JapaneseAnalyzer extends StopwordAnalyzerBase {
  private final Mode mode;
  private final Set<String> stoptags;
  private final UserDictionary userDict;
  
  public JapaneseAnalyzer() {
    this(null, JapaneseTokenizer.DEFAULT_MODE, DefaultSetHolder.DEFAULT_STOP_SET, DefaultSetHolder.DEFAULT_STOP_TAGS);
  }
  
  public JapaneseAnalyzer(UserDictionary userDict, Mode mode, CharArraySet stopwords, Set<String> stoptags) {
    super(stopwords);
    this.userDict = userDict;
    this.mode = mode;
    this.stoptags = stoptags;
  }
  
  public static CharArraySet getDefaultStopSet(){
    return DefaultSetHolder.DEFAULT_STOP_SET;
  }
  
  public static Set<String> getDefaultStopTags(){
    return DefaultSetHolder.DEFAULT_STOP_TAGS;
  }
  
  /**
   * Atomically loads DEFAULT_STOP_SET, DEFAULT_STOP_TAGS in a lazy fashion once the 
   * outer class accesses the static final set the first time.
   */
  private static class DefaultSetHolder {
    static final CharArraySet DEFAULT_STOP_SET;
    static final Set<String> DEFAULT_STOP_TAGS;

    static {
      try {
        DEFAULT_STOP_SET = loadStopwordSet(true, JapaneseAnalyzer.class, "stopwords.txt", "#");  // ignore case
        final CharArraySet tagset = loadStopwordSet(false, JapaneseAnalyzer.class, "stoptags.txt", "#");
        DEFAULT_STOP_TAGS = new HashSet<>();
        for (Object element : tagset) {
          char chars[] = (char[]) element;
          DEFAULT_STOP_TAGS.add(new String(chars));
        }
      } catch (IOException ex) {
        // default set should always be present as it is part of the distribution (JAR)
        throw new RuntimeException("Unable to load default stopword or stoptag set");
      }
    }
  }
  
  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    Tokenizer tokenizer = new JapaneseTokenizer(userDict, true, mode);
    TokenStream stream = new JapaneseBaseFormFilter(tokenizer);
    stream = new JapanesePartOfSpeechStopFilter(stream, stoptags);
    stream = new CJKWidthFilter(stream);
    stream = new StopFilter(stream, stopwords);
    stream = new JapaneseKatakanaStemFilter(stream);
    stream = new LowerCaseFilter(stream);
    return new TokenStreamComponents(tokenizer, stream);
  }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    TokenStream result = new CJKWidthFilter(in);
    result = new LowerCaseFilter(result);
    return result;
  }
}
