import org.apache.lucene.analysis.ko.dict.BinaryDictionary;
import org.apache.lucene.analysis.ko.dict.TokenInfoFST;
import org.apache.lucene.analysis.ko.dict.*;


import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;

import org.apache.lucene.store.InputStreamDataInput;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PositiveIntOutputs;

/**
 * Binary dictionary implementation for a known-word dictionary model:
 * Words are encoded into an FST mapping to a list of wordIDs.
 */
public final class TokenInfoDictionary extends BinaryDictionary {

  public static final String FST_FILENAME_SUFFIX = "$fst.dat";

  private final TokenInfoFST fst;
  
  private TokenInfoDictionary() throws IOException {
    super();
    InputStream is = null;
    FST<Long> fst = null;
    boolean success = false;
    try {
      is = getResource(FST_FILENAME_SUFFIX);
      is = new BufferedInputStream(is);
      fst = new FST<>(new InputStreamDataInput(is), PositiveIntOutputs.getSingleton());
      success = true;
    } finally {
      if (success) {
        IOUtils.close(is);
      } else {
        IOUtils.closeWhileHandlingException(is);
      }
    }
    this.fst = new TokenInfoFST(fst);
  }
  
  public TokenInfoFST getFST() {
    return fst;
  }
   
  public static TokenInfoDictionary getInstance() {
    return SingletonHolder.INSTANCE;
  }
  
  private static class SingletonHolder {
    static final TokenInfoDictionary INSTANCE;
    static {
      try {
        INSTANCE = new TokenInfoDictionary();
      } catch (IOException ioe) {
        throw new RuntimeException("Cannot load TokenInfoDictionary.", ioe);
      }
    }
   }
  
}
