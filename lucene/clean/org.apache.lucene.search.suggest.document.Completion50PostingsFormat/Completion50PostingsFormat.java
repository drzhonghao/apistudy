import org.apache.lucene.search.suggest.document.*;


import org.apache.lucene.codecs.PostingsFormat;

/**
 * {@link org.apache.lucene.search.suggest.document.CompletionPostingsFormat}
 * for {@link org.apache.lucene.codecs.lucene50.Lucene50PostingsFormat}
 *
 * @lucene.experimental
 */
public class Completion50PostingsFormat extends CompletionPostingsFormat {

  /**
   * Sole Constructor
   */
  public Completion50PostingsFormat() {
    super();
  }

  @Override
  protected PostingsFormat delegatePostingsFormat() {
    return PostingsFormat.forName("Lucene50");
  }
}
