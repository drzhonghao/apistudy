import org.apache.lucene.search.join.*;


import java.io.IOException;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.OrdinalMap;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.LongBitSet;
import org.apache.lucene.util.LongValues;

/**
 * A collector that collects all ordinals from a specified field matching the query.
 *
 * @lucene.experimental
 */
final class GlobalOrdinalsCollector implements Collector {

  final String field;
  final LongBitSet collectedOrds;
  final OrdinalMap ordinalMap;

  GlobalOrdinalsCollector(String field, OrdinalMap ordinalMap, long valueCount) {
    this.field = field;
    this.ordinalMap = ordinalMap;
    this.collectedOrds = new LongBitSet(valueCount);
  }

  public LongBitSet getCollectorOrdinals() {
    return collectedOrds;
  }

  @Override
  public boolean needsScores() {
    return false;
  }

  @Override
  public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
    SortedDocValues docTermOrds = DocValues.getSorted(context.reader(), field);
    if (ordinalMap != null) {
      LongValues segmentOrdToGlobalOrdLookup = ordinalMap.getGlobalOrds(context.ord);
      return new OrdinalMapCollector(docTermOrds, segmentOrdToGlobalOrdLookup);
    } else {
      return new SegmentOrdinalCollector(docTermOrds);
    }
  }

  final class OrdinalMapCollector implements LeafCollector {

    private final SortedDocValues docTermOrds;
    private final LongValues segmentOrdToGlobalOrdLookup;

    OrdinalMapCollector(SortedDocValues docTermOrds, LongValues segmentOrdToGlobalOrdLookup) {
      this.docTermOrds = docTermOrds;
      this.segmentOrdToGlobalOrdLookup = segmentOrdToGlobalOrdLookup;
    }

    @Override
    public void collect(int doc) throws IOException {
      if (docTermOrds.advanceExact(doc)) {
        long segmentOrd = docTermOrds.ordValue();
        long globalOrd = segmentOrdToGlobalOrdLookup.get(segmentOrd);
        collectedOrds.set(globalOrd);
      }
    }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
    }
  }

  final class SegmentOrdinalCollector implements LeafCollector {

    private final SortedDocValues docTermOrds;

    SegmentOrdinalCollector(SortedDocValues docTermOrds) {
      this.docTermOrds = docTermOrds;
    }

    @Override
    public void collect(int doc) throws IOException {
      if (docTermOrds.advanceExact(doc)) {
        collectedOrds.set(docTermOrds.ordValue());
      }
    }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
    }
  }

}
