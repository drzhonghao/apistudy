import org.apache.lucene.search.suggest.document.*;


import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.ConcatenateGraphFilter;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automaton;

/**
 * A {@link ConcatenateGraphFilter} but we can set the payload and provide access to config options.
 * @lucene.experimental
 */
public final class CompletionTokenStream extends TokenFilter {

  private final PayloadAttribute payloadAttr = addAttribute(PayloadAttribute.class);

  // package accessible on purpose
  final TokenStream inputTokenStream;
  final boolean preserveSep;
  final boolean preservePositionIncrements;
  final int maxGraphExpansions;

  private BytesRef payload; // note doesn't participate in TokenStream lifecycle; it's effectively constant

  CompletionTokenStream(TokenStream inputTokenStream) {
    this(inputTokenStream,
        ConcatenateGraphFilter.DEFAULT_PRESERVE_SEP,
        ConcatenateGraphFilter.DEFAULT_PRESERVE_POSITION_INCREMENTS,
        ConcatenateGraphFilter.DEFAULT_MAX_GRAPH_EXPANSIONS);
  }

  CompletionTokenStream(TokenStream inputTokenStream, boolean preserveSep, boolean preservePositionIncrements, int maxGraphExpansions) {
    super(new ConcatenateGraphFilter(inputTokenStream, preserveSep, preservePositionIncrements, maxGraphExpansions));
    this.inputTokenStream = inputTokenStream;
    this.preserveSep = preserveSep;
    this.preservePositionIncrements = preservePositionIncrements;
    this.maxGraphExpansions = maxGraphExpansions;
  }

  /**
   * Sets a payload available throughout successive token stream enumeration
   */
  public void setPayload(BytesRef payload) {
    this.payload = payload;
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      payloadAttr.setPayload(payload);
      return true;
    } else {
      return false;
    }
  }

  /** Delegates to...At
   * @see ConcatenateGraphFilter#toAutomaton()  */
  public Automaton toAutomaton() throws IOException {
    return ((ConcatenateGraphFilter)input).toAutomaton();
  }

  /** Delegates to...
   *  @see ConcatenateGraphFilter#toAutomaton(boolean) */
  public Automaton toAutomaton(boolean unicodeAware) throws IOException {
    return ((ConcatenateGraphFilter)input).toAutomaton(unicodeAware);
  }
}
