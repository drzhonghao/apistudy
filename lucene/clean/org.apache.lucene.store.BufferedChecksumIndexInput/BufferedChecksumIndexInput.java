import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.*;



import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/** 
 * Simple implementation of {@link ChecksumIndexInput} that wraps
 * another input and delegates calls.
 */
public class BufferedChecksumIndexInput extends ChecksumIndexInput {
  final IndexInput main;
  final Checksum digest;

  /** Creates a new BufferedChecksumIndexInput */
  public BufferedChecksumIndexInput(IndexInput main) {
    super("BufferedChecksumIndexInput(" + main + ")");
    this.main = main;
    this.digest = new BufferedChecksum(new CRC32());
  }

  @Override
  public byte readByte() throws IOException {
    final byte b = main.readByte();
    digest.update(b);
    return b;
  }

  @Override
  public void readBytes(byte[] b, int offset, int len)
    throws IOException {
    main.readBytes(b, offset, len);
    digest.update(b, offset, len);
  }

  @Override
  public long getChecksum() {
    return digest.getValue();
  }

  @Override
  public void close() throws IOException {
    main.close();
  }

  @Override
  public long getFilePointer() {
    return main.getFilePointer();
  }

  @Override
  public long length() {
    return main.length();
  }

  @Override
  public IndexInput clone() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IndexInput slice(String sliceDescription, long offset, long length) throws IOException {
    throw new UnsupportedOperationException();
  }
}
