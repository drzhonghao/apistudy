import org.apache.lucene.replicator.*;


import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.store.IndexInput;

/** 
 * An {@link InputStream} which wraps an {@link IndexInput}.
 * 
 * @lucene.experimental
 */
public final class IndexInputInputStream extends InputStream {
  
  private final IndexInput in;
  
  private long remaining;
  
  public IndexInputInputStream(IndexInput in) {
    this.in = in;
    remaining = in.length();
  }
  
  @Override
  public int read() throws IOException {
    if (remaining == 0) {
      return -1;
    } else {
      --remaining;
      return in.readByte();
    }
  }
  
  @Override
  public int available() throws IOException {
    return (int) in.length();
  }
  
  @Override
  public void close() throws IOException {
    in.close();
  }
  
  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }
  
  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (remaining == 0) {
      return -1;
    }
    if (remaining < len) {
      len = (int) remaining;
    }
    in.readBytes(b, off, len);
    remaining -= len;
    return len;
  }
  
  @Override
  public long skip(long n) throws IOException {
    if (remaining == 0) {
      return -1;
    }
    if (remaining < n) {
      n = remaining;
    }
    in.seek(in.getFilePointer() + n);
    remaining -= n;
    return n;
  }
  
}
