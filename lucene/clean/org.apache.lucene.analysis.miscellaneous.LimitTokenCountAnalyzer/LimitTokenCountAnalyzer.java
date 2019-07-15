import org.apache.lucene.analysis.miscellaneous.*;



import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;

/**
 * This Analyzer limits the number of tokens while indexing. It is
 * a replacement for the maximum field length setting inside {@link org.apache.lucene.index.IndexWriter}.
 * @see LimitTokenCountFilter
 */
public final class LimitTokenCountAnalyzer extends AnalyzerWrapper {
  private final Analyzer delegate;
  private final int maxTokenCount;
  private final boolean consumeAllTokens;

  /**
   * Build an analyzer that limits the maximum number of tokens per field.
   * This analyzer will not consume any tokens beyond the maxTokenCount limit
   *
   * @see #LimitTokenCountAnalyzer(Analyzer,int,boolean)
   */
  public LimitTokenCountAnalyzer(Analyzer delegate, int maxTokenCount) {
    this(delegate, maxTokenCount, false);
  }
  /**
   * Build an analyzer that limits the maximum number of tokens per field.
   * @param delegate the analyzer to wrap
   * @param maxTokenCount max number of tokens to produce
   * @param consumeAllTokens whether all tokens from the delegate should be consumed even if maxTokenCount is reached.
   */
  public LimitTokenCountAnalyzer(Analyzer delegate, int maxTokenCount, boolean consumeAllTokens) {
    super(delegate.getReuseStrategy());
    this.delegate = delegate;
    this.maxTokenCount = maxTokenCount;
    this.consumeAllTokens = consumeAllTokens;
  }

  @Override
  protected Analyzer getWrappedAnalyzer(String fieldName) {
    return delegate;
  }

  @Override
  protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
    return new TokenStreamComponents(components.getTokenizer(),
      new LimitTokenCountFilter(components.getTokenStream(), maxTokenCount, consumeAllTokens));
  }
  
  @Override
  public String toString() {
    return "LimitTokenCountAnalyzer(" + delegate.toString() + ", maxTokenCount=" + maxTokenCount + ", consumeAllTokens=" + consumeAllTokens + ")";
  }
}
