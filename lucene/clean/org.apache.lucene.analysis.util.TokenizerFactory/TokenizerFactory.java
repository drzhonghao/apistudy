import org.apache.lucene.analysis.util.*;



import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.AttributeFactory;

import java.util.Map;
import java.util.Set;

/**
 * Abstract parent class for analysis factories that create {@link Tokenizer}
 * instances.
 */
public abstract class TokenizerFactory extends AbstractAnalysisFactory {

  private static final AnalysisSPILoader<TokenizerFactory> loader =
      new AnalysisSPILoader<>(TokenizerFactory.class);
  
  /** looks up a tokenizer by name from context classpath */
  public static TokenizerFactory forName(String name, Map<String,String> args) {
    return loader.newInstance(name, args);
  }
  
  /** looks up a tokenizer class by name from context classpath */
  public static Class<? extends TokenizerFactory> lookupClass(String name) {
    return loader.lookupClass(name);
  }
  
  /** returns a list of all available tokenizer names from context classpath */
  public static Set<String> availableTokenizers() {
    return loader.availableServices();
  }
  
  /** 
   * Reloads the factory list from the given {@link ClassLoader}.
   * Changes to the factories are visible after the method ends, all
   * iterators ({@link #availableTokenizers()},...) stay consistent. 
   * 
   * <p><b>NOTE:</b> Only new factories are added, existing ones are
   * never removed or replaced.
   * 
   * <p><em>This method is expensive and should only be called for discovery
   * of new factories on the given classpath/classloader!</em>
   */
  public static void reloadTokenizers(ClassLoader classloader) {
    loader.reload(classloader);
  }
  
  /**
   * Initialize this factory via a set of key-value pairs.
   */
  protected TokenizerFactory(Map<String,String> args) {
    super(args);
  }

  /** Creates a TokenStream of the specified input using the default attribute factory. */
  public final Tokenizer create() {
    return create(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY);
  }
  
  /** Creates a TokenStream of the specified input using the given AttributeFactory */
  abstract public Tokenizer create(AttributeFactory factory);
}
