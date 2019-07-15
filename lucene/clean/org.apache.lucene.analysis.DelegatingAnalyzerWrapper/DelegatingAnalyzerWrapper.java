import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.*;


import java.io.Reader;


/**
 * An analyzer wrapper, that doesn't allow to wrap components or readers.
 * By disallowing it, it means that the thread local resources can be delegated
 * to the delegate analyzer, and not also be allocated on this analyzer.
 * This wrapper class is the base class of all analyzers that just delegate to
 * another analyzer, e.g. per field name.
 * 
 * <p>This solves the problem of per field analyzer wrapper, where it also
 * maintains a thread local per field token stream components, while it can
 * safely delegate those and not also hold these data structures, which can
 * become expensive memory wise.
 * 
 * <p><b>Please note:</b> This analyzer uses a private {@link Analyzer.ReuseStrategy},
 * which is returned by {@link #getReuseStrategy()}. This strategy is used when
 * delegating. If you wrap this analyzer again and reuse this strategy, no
 * delegation is done and the given fallback is used.
 */
public abstract class DelegatingAnalyzerWrapper extends AnalyzerWrapper {
  
  /**
   * Constructor.
   * @param fallbackStrategy is the strategy to use if delegation is not possible
   *  This is to support the common pattern:
   *  {@code new OtherWrapper(thisWrapper.getReuseStrategy())} 
   */
  protected DelegatingAnalyzerWrapper(ReuseStrategy fallbackStrategy) {
    super(new DelegatingReuseStrategy(fallbackStrategy));
    // häckidy-hick-hack, because we cannot call super() with a reference to "this":
    ((DelegatingReuseStrategy) getReuseStrategy()).wrapper = this;
  }
  
  @Override
  protected final TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
    return super.wrapComponents(fieldName, components);
  }

  @Override
  protected final TokenStream wrapTokenStreamForNormalization(String fieldName, TokenStream in) {
    return super.wrapTokenStreamForNormalization(fieldName, in);
  }

  @Override
  protected final Reader wrapReader(String fieldName, Reader reader) {
    return super.wrapReader(fieldName, reader);
  }

  @Override
  protected final Reader wrapReaderForNormalization(String fieldName, Reader reader) {
    return super.wrapReaderForNormalization(fieldName, reader);
  }

  private static final class DelegatingReuseStrategy extends ReuseStrategy {
    DelegatingAnalyzerWrapper wrapper;
    private final ReuseStrategy fallbackStrategy;
    
    DelegatingReuseStrategy(ReuseStrategy fallbackStrategy) {
      this.fallbackStrategy = fallbackStrategy;
    }
    
    @Override
    public TokenStreamComponents getReusableComponents(Analyzer analyzer, String fieldName) {
      if (analyzer == wrapper) {
        final Analyzer wrappedAnalyzer = wrapper.getWrappedAnalyzer(fieldName);
        return wrappedAnalyzer.getReuseStrategy().getReusableComponents(wrappedAnalyzer, fieldName);
      } else {
        return fallbackStrategy.getReusableComponents(analyzer, fieldName);
      }
    }

    @Override
    public void setReusableComponents(Analyzer analyzer, String fieldName,  TokenStreamComponents components) {
      if (analyzer == wrapper) {
        final Analyzer wrappedAnalyzer = wrapper.getWrappedAnalyzer(fieldName);
        wrappedAnalyzer.getReuseStrategy().setReusableComponents(wrappedAnalyzer, fieldName, components);
      } else {
        fallbackStrategy.setReusableComponents(analyzer, fieldName, components);
      }
    }
  };
  
}
