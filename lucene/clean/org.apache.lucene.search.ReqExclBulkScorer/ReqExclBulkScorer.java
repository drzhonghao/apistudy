import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.*;



import java.io.IOException;

import org.apache.lucene.util.Bits;

final class ReqExclBulkScorer extends BulkScorer {

  private final BulkScorer req;
  private final DocIdSetIterator excl;

  ReqExclBulkScorer(BulkScorer req, DocIdSetIterator excl) {
    this.req = req;
    this.excl = excl;
  }

  @Override
  public int score(LeafCollector collector, Bits acceptDocs, int min, int max) throws IOException {
    int upTo = min;
    int exclDoc = excl.docID();

    while (upTo < max) {
      if (exclDoc < upTo) {
        exclDoc = excl.advance(upTo);
      }
      if (exclDoc == upTo) {
        // upTo is excluded so we can consider that we scored up to upTo+1
        upTo += 1;
        exclDoc = excl.nextDoc();
      } else {
        upTo = req.score(collector, acceptDocs, upTo, Math.min(exclDoc, max));
      }
    }

    if (upTo == max) {
      upTo = req.score(collector, acceptDocs, upTo, upTo);
    }

    return upTo;
  }

  @Override
  public long cost() {
    return req.cost();
  }

}
