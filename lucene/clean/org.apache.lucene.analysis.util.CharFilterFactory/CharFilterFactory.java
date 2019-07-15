import org.apache.lucene.analysis.util.*;



import java.io.Reader;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.CharFilter;

/**
 * Abstract parent class for analysis factories that create {@link CharFilter}
 * instances.
 */
public abstract class CharFilterFactory extends AbstractAnalysisFactory {

  private static final AnalysisSPILoader<CharFilterFactory> loader =
      new AnalysisSPILoader<>(CharFilterFactory.class);
  
  /** looks up a charfilter by name from context classpath */
  public static CharFilterFactory forName(String name, Map<String,String> args) {
    return loader.newInstance(name, args);
  }
  
  /** looks up a charfilter class by name from context classpath */
  public static Class<? extends CharFilterFactory> lookupClass(String name) {
    return loader.lookupClass(name);
  }
  
  /** returns a list of all available charfilter names */
  public static Set<String> availableCharFilters() {
    return loader.availableServices();
  }

  /** 
   * Reloads the factory list from the given {@link ClassLoader}.
   * Changes to the factories are visible after the method ends, all
   * iterators ({@link #availableCharFilters()},...) stay consistent. 
   * 
   * <p><b>NOTE:</b> Only new factories are added, existing ones are
   * never removed or replaced.
   * 
   * <p><em>This method is expensive and should only be called for discovery
   * of new factories on the given classpath/classloader!</em>
   */
  public static void reloadCharFilters(ClassLoader classloader) {
    loader.reload(classloader);
  }

  /**
   * Initialize this factory via a set of key-value pairs.
   */
  protected CharFilterFactory(Map<String,String> args) {
    super(args);
  }

  /** Wraps the given Reader with a CharFilter. */
  public abstract Reader create(Reader input);
}
