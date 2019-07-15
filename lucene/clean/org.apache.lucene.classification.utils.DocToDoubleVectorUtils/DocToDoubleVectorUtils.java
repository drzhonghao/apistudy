import org.apache.lucene.classification.utils.*;


import java.io.IOException;

import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

/**
 * utility class for converting Lucene {@link org.apache.lucene.document.Document}s to <code>Double</code> vectors.
 */
public class DocToDoubleVectorUtils {

  private DocToDoubleVectorUtils() {
    // no public constructors
  }

  /**
   * create a sparse <code>Double</code> vector given doc and field term vectors using local frequency of the terms in the doc
   *
   * @param docTerms   term vectors for a given document
   * @param fieldTerms field term vectors
   * @return a sparse vector of <code>Double</code>s as an array
   * @throws IOException in case accessing the underlying index fails
   */
  public static Double[] toSparseLocalFreqDoubleArray(Terms docTerms, Terms fieldTerms) throws IOException {
    TermsEnum fieldTermsEnum = fieldTerms.iterator();
    Double[] freqVector = null;
    if (docTerms != null && fieldTerms.size() > -1) {
      freqVector = new Double[(int) fieldTerms.size()];
      int i = 0;
      TermsEnum docTermsEnum = docTerms.iterator();
      BytesRef term;
      while ((term = fieldTermsEnum.next()) != null) {
        TermsEnum.SeekStatus seekStatus = docTermsEnum.seekCeil(term);
        if (seekStatus.equals(TermsEnum.SeekStatus.END)) {
          docTermsEnum = docTerms.iterator();
        }
        if (seekStatus.equals(TermsEnum.SeekStatus.FOUND)) {
          long termFreqLocal = docTermsEnum.totalTermFreq(); // the total number of occurrences of this term in the given document
          freqVector[i] = Long.valueOf(termFreqLocal).doubleValue();
        } else {
          freqVector[i] = 0d;
        }
        i++;
      }
    }
    return freqVector;
  }

  /**
   * create a dense <code>Double</code> vector given doc and field term vectors using local frequency of the terms in the doc
   *
   * @param docTerms term vectors for a given document
   * @return a dense vector of <code>Double</code>s as an array
   * @throws IOException in case accessing the underlying index fails
   */
  public static Double[] toDenseLocalFreqDoubleArray(Terms docTerms) throws IOException {
    Double[] freqVector = null;
    if (docTerms != null) {
      freqVector = new Double[(int) docTerms.size()];
      int i = 0;
      TermsEnum docTermsEnum = docTerms.iterator();

      while (docTermsEnum.next() != null) {
        long termFreqLocal = docTermsEnum.totalTermFreq(); // the total number of occurrences of this term in the given document
        freqVector[i] = Long.valueOf(termFreqLocal).doubleValue();
        i++;
      }
    }
    return freqVector;
  }
}
