import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.*;



import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;

/**
 * Factory for {@link ElisionFilter}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_elsn" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.StandardTokenizerFactory"/&gt;
 *     &lt;filter class="solr.LowerCaseFilterFactory"/&gt;
 *     &lt;filter class="solr.ElisionFilterFactory" 
 *       articles="stopwordarticles.txt" ignoreCase="true"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 */
public class ElisionFilterFactory extends TokenFilterFactory implements ResourceLoaderAware, MultiTermAwareComponent {
  private final String articlesFile;
  private final boolean ignoreCase;
  private CharArraySet articles;

  /** Creates a new ElisionFilterFactory */
  public ElisionFilterFactory(Map<String,String> args) {
    super(args);
    articlesFile = get(args, "articles");
    ignoreCase = getBoolean(args, "ignoreCase", false);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public void inform(ResourceLoader loader) throws IOException {
    if (articlesFile == null) {
      articles = FrenchAnalyzer.DEFAULT_ARTICLES;
    } else {
      articles = getWordSet(loader, articlesFile, ignoreCase);
    }
  }

  @Override
  public ElisionFilter create(TokenStream input) {
    return new ElisionFilter(input, articles);
  }

  @Override
  public AbstractAnalysisFactory getMultiTermComponent() {
    return this;
  }
}

