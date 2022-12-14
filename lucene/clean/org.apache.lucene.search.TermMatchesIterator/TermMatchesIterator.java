import org.apache.lucene.search.*;


import java.io.IOException;

import org.apache.lucene.index.PostingsEnum;

/**
 * A {@link MatchesIterator} over a single term's postings list
 */
class TermMatchesIterator implements MatchesIterator {

  private int upto;
  private int pos;
  private final PostingsEnum pe;

  /**
   * Create a new {@link TermMatchesIterator} for the given term and postings list
   */
  TermMatchesIterator(PostingsEnum pe) throws IOException {
    this.pe = pe;
    this.upto = pe.freq();
  }

  @Override
  public boolean next() throws IOException {
    if (upto-- > 0) {
      pos = pe.nextPosition();
      return true;
    }
    return false;
  }

  @Override
  public int startPosition() {
    return pos;
  }

  @Override
  public int endPosition() {
    return pos;
  }

  @Override
  public int startOffset() throws IOException {
    return pe.startOffset();
  }

  @Override
  public int endOffset() throws IOException {
    return pe.endOffset();
  }

}
