import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;



import java.io.IOException;

/** A TokenFilter is a TokenStream whose input is another TokenStream.
  <p>
  This is an abstract class; subclasses must override {@link #incrementToken()}.
  @see TokenStream
  */
public abstract class TokenFilter extends TokenStream {
  /** The source of tokens for this filter. */
  protected final TokenStream input;

  /** Construct a token stream filtering the given input. */
  protected TokenFilter(TokenStream input) {
    super(input);
    this.input = input;
  }
  
  /** 
   * {@inheritDoc}
   * <p> 
   * <b>NOTE:</b> 
   * The default implementation chains the call to the input TokenStream, so
   * be sure to call <code>super.end()</code> first when overriding this method.
   */
  @Override
  public void end() throws IOException {
    input.end();
  }
  
  /**
   * {@inheritDoc}
   * <p>
   * <b>NOTE:</b> 
   * The default implementation chains the call to the input TokenStream, so
   * be sure to call <code>super.close()</code> when overriding this method.
   */
  @Override
  public void close() throws IOException {
    input.close();
  }

  /**
   * {@inheritDoc}
   * <p>
   * <b>NOTE:</b> 
   * The default implementation chains the call to the input TokenStream, so
   * be sure to call <code>super.reset()</code> when overriding this method.
   */
  @Override
  public void reset() throws IOException {
    input.reset();
  }
}
