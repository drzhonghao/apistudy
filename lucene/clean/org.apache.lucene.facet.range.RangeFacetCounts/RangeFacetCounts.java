import org.apache.lucene.facet.range.Range;
import org.apache.lucene.facet.range.*;


import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.search.Query;

/** Base class for range faceting.
 *
 *  @lucene.experimental */
abstract class RangeFacetCounts extends Facets {
  /** Ranges passed to constructor. */
  protected final Range[] ranges;

  /** Counts, initialized in by subclass. */
  protected final int[] counts;

  /** Optional: if specified, we first test this Query to
   *  see whether the document should be checked for
   *  matching ranges.  If this is null, all documents are
   *  checked. */
  protected final Query fastMatchQuery;

  /** Our field name. */
  protected final String field;

  /** Total number of hits. */
  protected int totCount;

  /** Create {@code RangeFacetCounts} */
  protected RangeFacetCounts(String field, Range[] ranges, Query fastMatchQuery) throws IOException {
    this.field = field;
    this.ranges = ranges;
    this.fastMatchQuery = fastMatchQuery;
    counts = new int[ranges.length];
  }

  @Override
  public FacetResult getTopChildren(int topN, String dim, String... path) {
    if (dim.equals(field) == false) {
      throw new IllegalArgumentException("invalid dim \"" + dim + "\"; should be \"" + field + "\"");
    }
    if (path.length != 0) {
      throw new IllegalArgumentException("path.length should be 0");
    }
    LabelAndValue[] labelValues = new LabelAndValue[counts.length];
    for(int i=0;i<counts.length;i++) {
      labelValues[i] = new LabelAndValue(ranges[i].label, counts[i]);
    }
    return new FacetResult(dim, path, totCount, labelValues, labelValues.length);
  }

  @Override
  public Number getSpecificValue(String dim, String... path) throws IOException {
    // TODO: should we impl this?
    throw new UnsupportedOperationException();
  }

  @Override
  public List<FacetResult> getAllDims(int topN) throws IOException {
    return Collections.singletonList(getTopChildren(topN, field));
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append("RangeFacetCounts totCount=");
    b.append(totCount);
    b.append(":\n");
    for(int i=0;i<ranges.length;i++) {
      b.append("  ");
      b.append(ranges[i].label);
      b.append(" -> count=");
      b.append(counts[i]);
      b.append('\n');
    }
    return b.toString();
  }
}
