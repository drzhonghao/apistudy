import org.apache.lucene.codecs.*;


import org.apache.lucene.index.OrdTermState;
import org.apache.lucene.index.TermState;

/**
 * Holds all state required for {@link PostingsReaderBase}
 * to produce a {@link org.apache.lucene.index.PostingsEnum} without re-seeking the
 * terms dict.
 *
 * @lucene.internal
 */
public class BlockTermState extends OrdTermState {
  /** how many docs have this term */
  public int docFreq;
  /** total number of occurrences of this term */
  public long totalTermFreq;

  /** the term's ord in the current block */
  public int termBlockOrd;
  /** fp into the terms dict primary file (_X.tim) that holds this term */
  // TODO: update BTR to nuke this
  public long blockFilePointer;

  /** Sole constructor. (For invocation by subclass 
   *  constructors, typically implicit.) */
  protected BlockTermState() {
  }

  @Override
  public void copyFrom(TermState _other) {
    assert _other instanceof BlockTermState : "can not copy from " + _other.getClass().getName();
    BlockTermState other = (BlockTermState) _other;
    super.copyFrom(_other);
    docFreq = other.docFreq;
    totalTermFreq = other.totalTermFreq;
    termBlockOrd = other.termBlockOrd;
    blockFilePointer = other.blockFilePointer;
  }

  @Override
  public String toString() {
    return "docFreq=" + docFreq + " totalTermFreq=" + totalTermFreq + " termBlockOrd=" + termBlockOrd + " blockFP=" + blockFilePointer;
  }
}
