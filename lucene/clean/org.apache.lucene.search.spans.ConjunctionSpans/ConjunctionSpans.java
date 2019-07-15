import org.apache.lucene.search.spans.*;



import java.io.IOException;
import java.util.List;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.ConjunctionDISI;
import org.apache.lucene.search.TwoPhaseIterator;

/**
 * Common super class for multiple sub spans required in a document.
 */
abstract class ConjunctionSpans extends Spans {
  final Spans[] subSpans; // in query order
  final DocIdSetIterator conjunction; // use to move to next doc with all clauses
  boolean atFirstInCurrentDoc; // a first start position is available in current doc for nextStartPosition
  boolean oneExhaustedInCurrentDoc; // one subspans exhausted in current doc

  ConjunctionSpans(List<Spans> subSpans) {
    if (subSpans.size() < 2) {
      throw new IllegalArgumentException("Less than 2 subSpans.size():" + subSpans.size());
    }
    this.subSpans = subSpans.toArray(new Spans[subSpans.size()]);
    this.conjunction = ConjunctionDISI.intersectSpans(subSpans);
    this.atFirstInCurrentDoc = true; // ensure for doc -1 that start/end positions are -1
  }

  @Override
  public int docID() {
    return conjunction.docID();
  }

  @Override
  public long cost() {
    return conjunction.cost();
  }

  @Override
  public int nextDoc() throws IOException {
    return (conjunction.nextDoc() == NO_MORE_DOCS)
            ? NO_MORE_DOCS
            : toMatchDoc();
  }

  @Override
  public int advance(int target) throws IOException {
    return (conjunction.advance(target) == NO_MORE_DOCS)
            ? NO_MORE_DOCS
            : toMatchDoc();
  }

  int toMatchDoc() throws IOException {
    oneExhaustedInCurrentDoc = false;
    while (true) {
      if (twoPhaseCurrentDocMatches()) {
        return docID();
      }
      if (conjunction.nextDoc() == NO_MORE_DOCS) {
        return NO_MORE_DOCS;
      }
    }
  }


  abstract boolean twoPhaseCurrentDocMatches() throws IOException;

  /**
   * Return a {@link TwoPhaseIterator} view of this ConjunctionSpans.
   */
  @Override
  public TwoPhaseIterator asTwoPhaseIterator() {
    float totalMatchCost = 0;
    // Compute the matchCost as the total matchCost/positionsCostant of the sub spans.
    for (Spans spans : subSpans) {
      TwoPhaseIterator tpi = spans.asTwoPhaseIterator();
      if (tpi != null) {
        totalMatchCost += tpi.matchCost();
      } else {
        totalMatchCost += spans.positionsCost();
      }
    }
    final float matchCost = totalMatchCost;

    return new TwoPhaseIterator(conjunction) {
      @Override
      public boolean matches() throws IOException {
        return twoPhaseCurrentDocMatches();
      }

      @Override
      public float matchCost() {
        return matchCost;
      }
    };
  }

  @Override
  public float positionsCost() {
    throw new UnsupportedOperationException(); // asTwoPhaseIterator never returns null here.
  }

  public Spans[] getSubSpans() {
    return subSpans;
  }
}
