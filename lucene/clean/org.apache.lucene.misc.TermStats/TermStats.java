import org.apache.lucene.misc.*;


import org.apache.lucene.util.BytesRef;

/**
 * Holder for a term along with its statistics
 * ({@link #docFreq} and {@link #totalTermFreq}).
 */
public final class TermStats {
  public BytesRef termtext;
  public String field;
  public int docFreq;
  public long totalTermFreq;
  
  TermStats(String field, BytesRef termtext, int df, long tf) {
    this.termtext = BytesRef.deepCopyOf(termtext);
    this.field = field;
    this.docFreq = df;
    this.totalTermFreq = tf;
  }
  
  String getTermText() {
    return termtext.utf8ToString();
  }

  @Override
  public String toString() {
    return("TermStats: term=" + termtext.utf8ToString() + " docFreq=" + docFreq + " totalTermFreq=" + totalTermFreq);
  }
}
