import org.apache.lucene.index.*;



/**
 * Subreader slice from a parent composite reader.
 *
 * @lucene.internal
 */
public final class ReaderSlice {

  /** Zero-length {@code ReaderSlice} array. */
  public static final ReaderSlice[] EMPTY_ARRAY = new ReaderSlice[0];

  /** Document ID this slice starts from. */
  public final int start;

  /** Number of documents in this slice. */
  public final int length;

  /** Sub-reader index for this slice. */
  public final int readerIndex;

  /** Sole constructor. */
  public ReaderSlice(int start, int length, int readerIndex) {
    this.start = start;
    this.length = length;
    this.readerIndex = readerIndex;
  }

  @Override
  public String toString() {
    return "slice start=" + start + " length=" + length + " readerIndex=" + readerIndex;
  }
}
