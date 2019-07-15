import org.apache.lucene.analysis.ja.dict.BinaryDictionary;
import org.apache.lucene.analysis.ja.dict.*;



import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.InputStreamDataInput;
import org.apache.lucene.util.IOUtils;

/**
 * n-gram connection cost data
 */
public final class ConnectionCosts {
  
  public static final String FILENAME_SUFFIX = ".dat";
  public static final String HEADER = "kuromoji_cc";
  public static final int VERSION = 1;
  
  private final short[][] costs; // array is backward IDs first since get is called using the same backward ID consecutively. maybe doesn't matter.
  
  private ConnectionCosts() throws IOException {
    InputStream is = null;
    short[][] costs = null;
    boolean success = false;
    try {
      is = BinaryDictionary.getClassResource(getClass(), FILENAME_SUFFIX);
      is = new BufferedInputStream(is);
      final DataInput in = new InputStreamDataInput(is);
      CodecUtil.checkHeader(in, HEADER, VERSION, VERSION);
      int forwardSize = in.readVInt();
      int backwardSize = in.readVInt();
      costs = new short[backwardSize][forwardSize];
      int accum = 0;
      for (int j = 0; j < costs.length; j++) {
        final short[] a = costs[j];
        for (int i = 0; i < a.length; i++) {
          accum += in.readZInt();
          a[i] = (short)accum;
        }
      }
      success = true;
    } finally {
      if (success) {
        IOUtils.close(is);
      } else {
        IOUtils.closeWhileHandlingException(is);
      }
    }
    
    this.costs = costs;
  }
  
  public int get(int forwardId, int backwardId) {
    return costs[backwardId][forwardId];
  }
  
  public static ConnectionCosts getInstance() {
    return SingletonHolder.INSTANCE;
  }
  
  private static class SingletonHolder {
    static final ConnectionCosts INSTANCE;
    static {
      try {
        INSTANCE = new ConnectionCosts();
      } catch (IOException ioe) {
        throw new RuntimeException("Cannot load ConnectionCosts.", ioe);
      }
    }
   }
  
}
