import org.apache.lucene.analysis.stempel.*;



import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.stempel.StempelFilter;
import org.apache.lucene.analysis.stempel.StempelStemmer;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * Factory for {@link StempelFilter} using a Polish stemming table.
 * @since 3.1.0
 */
public class StempelPolishStemFilterFactory extends TokenFilterFactory {  
  
  /** Creates a new StempelPolishStemFilterFactory */
  public StempelPolishStemFilterFactory(Map<String,String> args) {
    super(args);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public TokenStream create(TokenStream input) {
    return new StempelFilter(input, new StempelStemmer(PolishAnalyzer.getDefaultTable()));
  }
}
