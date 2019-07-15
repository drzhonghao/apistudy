import org.apache.lucene.analysis.miscellaneous.*;


import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.IOUtils;

/**
 * A TokenStream that takes an array of input TokenStreams as sources, and
 * concatenates them together.
 *
 * Offsets from the second and subsequent sources are incremented to behave
 * as if all the inputs were from a single source.
 *
 * All of the input TokenStreams must have the same attribute implementations
 */
public final class ConcatenatingTokenStream extends TokenStream {

  private final TokenStream[] sources;
  private final OffsetAttribute[] sourceOffsets;
  private final OffsetAttribute offsetAtt;

  private int currentSource;
  private int offsetIncrement;

  /**
   * Create a new ConcatenatingTokenStream from a set of inputs
   * @param sources an array of TokenStream inputs to concatenate
   */
  public ConcatenatingTokenStream(TokenStream... sources) {
    super(combineSources(sources));
    this.sources = sources;
    this.offsetAtt = addAttribute(OffsetAttribute.class);
    this.sourceOffsets = new OffsetAttribute[sources.length];
    for (int i = 0; i < sources.length; i++) {
      this.sourceOffsets[i] = sources[i].addAttribute(OffsetAttribute.class);
    }
  }

  private static AttributeSource combineSources(TokenStream... sources) {
    AttributeSource base = sources[0].cloneAttributes();
    try {
      for (int i = 1; i < sources.length; i++) {
        Iterator<Class<? extends Attribute>> it = sources[i].getAttributeClassesIterator();
        while (it.hasNext()) {
          base.addAttribute(it.next());
        }
        // check attributes can be captured
        sources[i].copyTo(base);
      }
      return base;
    }
    catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Attempted to concatenate TokenStreams with different attribute types", e);
    }
  }

  @Override
  public boolean incrementToken() throws IOException {
    while (sources[currentSource].incrementToken() == false) {
      if (currentSource >= sources.length - 1)
        return false;
      sources[currentSource].end();
      OffsetAttribute att = sourceOffsets[currentSource];
      if (att != null)
        offsetIncrement += att.endOffset();
      currentSource++;
    }

    clearAttributes();
    sources[currentSource].copyTo(this);
    offsetAtt.setOffset(offsetAtt.startOffset() + offsetIncrement, offsetAtt.endOffset() + offsetIncrement);

    return true;
  }

  @Override
  public void end() throws IOException {
    sources[currentSource].end();
    super.end();
  }

  @Override
  public void reset() throws IOException {
    for (TokenStream source : sources) {
      source.reset();
    }
    super.reset();
  }

  @Override
  public void close() throws IOException {
    try {
      IOUtils.close(sources);
    }
    finally {
      super.close();
    }
  }
}
