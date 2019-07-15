import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.*;


import java.io.IOException;


/** 
 * Extension of IndexInput, computing checksum as it goes. 
 * Callers can retrieve the checksum via {@link #getChecksum()}.
 */
public abstract class ChecksumIndexInput extends IndexInput {
  
  /** resourceDescription should be a non-null, opaque string
   *  describing this resource; it's returned from
   *  {@link #toString}. */
  protected ChecksumIndexInput(String resourceDescription) {
    super(resourceDescription);
  }

  /** Returns the current checksum value */
  public abstract long getChecksum() throws IOException;

  /**
   * {@inheritDoc}
   *
   * {@link ChecksumIndexInput} can only seek forward and seeks are expensive
   * since they imply to read bytes in-between the current position and the
   * target position in order to update the checksum.
   */
  @Override
  public void seek(long pos) throws IOException {
    final long curFP = getFilePointer();
    final long skip = pos - curFP;
    if (skip < 0) {
      throw new IllegalStateException(getClass() + " cannot seek backwards (pos=" + pos + " getFilePointer()=" + curFP + ")");
    }
    skipBytes(skip);
  }
}
