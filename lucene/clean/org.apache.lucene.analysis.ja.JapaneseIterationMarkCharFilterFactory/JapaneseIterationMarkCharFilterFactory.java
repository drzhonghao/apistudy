import org.apache.lucene.analysis.ja.*;



import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.ja.JapaneseIterationMarkCharFilter;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;

import java.io.Reader;
import java.util.Map;

/**
 * Factory for {@link org.apache.lucene.analysis.ja.JapaneseIterationMarkCharFilter}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_ja" class="solr.TextField" positionIncrementGap="100" autoGeneratePhraseQueries="false"&gt;
 *   &lt;analyzer&gt;
 *     &lt;charFilter class="solr.JapaneseIterationMarkCharFilterFactory normalizeKanji="true" normalizeKana="true"/&gt;
 *     &lt;tokenizer class="solr.JapaneseTokenizerFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 */
public class JapaneseIterationMarkCharFilterFactory extends CharFilterFactory implements MultiTermAwareComponent {

  private static final String NORMALIZE_KANJI_PARAM = "normalizeKanji";
  private static final String NORMALIZE_KANA_PARAM = "normalizeKana";

  private final boolean normalizeKanji;
  private final boolean normalizeKana;
  
  /** Creates a new JapaneseIterationMarkCharFilterFactory */
  public JapaneseIterationMarkCharFilterFactory(Map<String,String> args) {
    super(args);
    normalizeKanji = getBoolean(args, NORMALIZE_KANJI_PARAM, JapaneseIterationMarkCharFilter.NORMALIZE_KANJI_DEFAULT);
    normalizeKana = getBoolean(args, NORMALIZE_KANA_PARAM, JapaneseIterationMarkCharFilter.NORMALIZE_KANA_DEFAULT);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public CharFilter create(Reader input) {
    return new JapaneseIterationMarkCharFilter(input, normalizeKanji, normalizeKana);
  }

  @Override
  public AbstractAnalysisFactory getMultiTermComponent() {
    return this;
  }
}
