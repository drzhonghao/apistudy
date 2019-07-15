import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.util.AttributeSource;

/**
 * This class can be used if the token attributes of a TokenStream
 * are intended to be consumed more than once. It caches
 * all token attribute states locally in a List when the first call to
 * {@link #incrementToken()} is called. Subsequent calls will used the cache.
 * <p>
 * <em>Important:</em> Like any proper TokenFilter, {@link #reset()} propagates
 * to the input, although only before {@link #incrementToken()} is called the
 * first time. Prior to  Lucene 5, it was never propagated.
 */
public final class CachingTokenFilter extends TokenFilter {
  private List<AttributeSource.State> cache = null;
  private Iterator<AttributeSource.State> iterator = null; 
  private AttributeSource.State finalState;
  
  /**
   * Create a new CachingTokenFilter around <code>input</code>. As with
   * any normal TokenFilter, do <em>not</em> call reset on the input; this filter
   * will do it normally.
   */
  public CachingTokenFilter(TokenStream input) {
    super(input);
  }

  /**
   * Propagates reset if incrementToken has not yet been called. Otherwise
   * it rewinds the iterator to the beginning of the cached list.
   */
  @Override
  public void reset() throws IOException {
    if (cache == null) {//first time
      input.reset();
    } else {
      iterator = cache.iterator();
    }
  }

  /** The first time called, it'll read and cache all tokens from the input. */
  @Override
  public final boolean incrementToken() throws IOException {
    if (cache == null) {//first-time
      // fill cache lazily
      cache = new ArrayList<>(64);
      fillCache();
      iterator = cache.iterator();
    }
    
    if (!iterator.hasNext()) {
      // the cache is exhausted, return false
      return false;
    }
    // Since the TokenFilter can be reset, the tokens need to be preserved as immutable.
    restoreState(iterator.next());
    return true;
  }

  @Override
  public final void end() {
    if (finalState != null) {
      restoreState(finalState);
    }
  }

  private void fillCache() throws IOException {
    while (input.incrementToken()) {
      cache.add(captureState());
    }
    // capture final state
    input.end();
    finalState = captureState();
  }

  /** If the underlying token stream was consumed and cached. */
  public boolean isCached() {
    return cache != null;
  }

}
