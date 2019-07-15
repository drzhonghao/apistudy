import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.*;



import java.io.IOException;
import org.apache.lucene.index.*;

/**
 * Position of a term in a document that takes into account the term offset within the phrase. 
 */
final class PhrasePositions {
  int position;         // position in doc
  int count;            // remaining pos in this doc
  int offset;           // position in phrase
  final int ord;                                  // unique across all PhrasePositions instances
  final PostingsEnum postings;            // stream of docs & positions
  PhrasePositions next;                           // used to make lists
  int rptGroup = -1; // >=0 indicates that this is a repeating PP
  int rptInd; // index in the rptGroup
  final Term[] terms; // for repetitions initialization 

  PhrasePositions(PostingsEnum postings, int o, int ord, Term[] terms) {
    this.postings = postings;
    offset = o;
    this.ord = ord;
    this.terms = terms;
  }

  final void firstPosition() throws IOException {
    count = postings.freq();  // read first pos
    nextPosition();
  }

  /**
   * Go to next location of this term current document, and set 
   * <code>position</code> as <code>location - offset</code>, so that a 
   * matching exact phrase is easily identified when all PhrasePositions 
   * have exactly the same <code>position</code>.
   */
  final boolean nextPosition() throws IOException {
    if (count-- > 0) {  // read subsequent pos's
      position = postings.nextPosition() - offset;
      return true;
    } else
      return false;
  }
  
  /** for debug purposes */
  @Override
  public String toString() {
    String s = "o:"+offset+" p:"+position+" c:"+count;
    if (rptGroup >=0 ) {
      s += " rpt:"+rptGroup+",i"+rptInd;
    }
    return s;
  }
}
