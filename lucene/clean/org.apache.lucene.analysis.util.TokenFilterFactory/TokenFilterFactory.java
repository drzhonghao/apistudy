import org.apache.lucene.analysis.util.*;



import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;

/**
 * Abstract parent class for analysis factories that create {@link org.apache.lucene.analysis.TokenFilter}
 * instances.
 */
public abstract class TokenFilterFactory extends AbstractAnalysisFactory {

  private static final AnalysisSPILoader<TokenFilterFactory> loader =
      new AnalysisSPILoader<>(TokenFilterFactory.class,
          new String[] { "TokenFilterFactory", "FilterFactory" });
  
  /** looks up a tokenfilter by name from context classpath */
  public static TokenFilterFactory forName(String name, Map<String,String> args) {
    return loader.newInstance(name, args);
  }
  
  /** looks up a tokenfilter class by name from context classpath */
  public static Class<? extends TokenFilterFactory> lookupClass(String name) {
    return loader.lookupClass(name);
  }
  
  /** returns a list of all available tokenfilter names from context classpath */
  public static Set<String> availableTokenFilters() {
    return loader.availableServices();
  }
  
  /** 
   * Reloads the factory list from the given {@link ClassLoader}.
   * Changes to the factories are visible after the method ends, all
   * iterators ({@link #availableTokenFilters()},...) stay consistent. 
   * 
   * <p><b>NOTE:</b> Only new factories are added, existing ones are
   * never removed or replaced.
   * 
   * <p><em>This method is expensive and should only be called for discovery
   * of new factories on the given classpath/classloader!</em>
   */
  public static void reloadTokenFilters(ClassLoader classloader) {
    loader.reload(classloader);
  }
  
  /**
   * Initialize this factory via a set of key-value pairs.
   */
  protected TokenFilterFactory(Map<String,String> args) {
    super(args);
  }

  /** Transform the specified input TokenStream */
  public abstract TokenStream create(TokenStream input);
}
