import org.apache.lucene.search.join.*;


import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.Bits;

/** Utility class to check a block join index. */
public class CheckJoinIndex {

  private CheckJoinIndex() {}

  /**
   * Check that the given index is good to use for block joins.
   * @throws IllegalStateException if the index does not have an appropriate structure
   */
  public static void check(IndexReader reader, BitSetProducer parentsFilter) throws IOException {
    for (LeafReaderContext context : reader.leaves()) {
      if (context.reader().maxDoc() == 0) {
        continue;
      }
      final BitSet parents = parentsFilter.getBitSet(context);
      if (parents == null || parents.cardinality() == 0) {
        throw new IllegalStateException("Every segment should have at least one parent, but " + context.reader() + " does not have any");
      }
      if (parents.get(context.reader().maxDoc() - 1) == false) {
        throw new IllegalStateException("The last document of a segment must always be a parent, but " + context.reader() + " has a child as a last doc");
      }
      final Bits liveDocs = context.reader().getLiveDocs();
      if (liveDocs != null) {
        int prevParentDoc = -1;
        DocIdSetIterator it = new BitSetIterator(parents, 0L);
        for (int parentDoc = it.nextDoc(); parentDoc != DocIdSetIterator.NO_MORE_DOCS; parentDoc = it.nextDoc()) {
          final boolean parentIsLive = liveDocs.get(parentDoc);
          for (int child = prevParentDoc + 1; child != parentDoc; child++) {
            final boolean childIsLive = liveDocs.get(child);
            if (parentIsLive != childIsLive) {
              if (childIsLive) {
                throw new IllegalStateException("Parent doc " + parentDoc + " of segment " + context.reader() + " is live but has a deleted child document " + child);
              } else {
                throw new IllegalStateException("Parent doc " + parentDoc + " of segment " + context.reader() + " is deleted but has a live child document " + child);
              }
            }
          }
          prevParentDoc = parentDoc;
        }
      }
    }
  }

}
