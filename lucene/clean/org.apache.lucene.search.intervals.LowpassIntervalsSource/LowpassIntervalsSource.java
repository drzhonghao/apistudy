import org.apache.lucene.search.intervals.IntervalsSource;
import org.apache.lucene.search.intervals.IntervalIterator;
import org.apache.lucene.search.intervals.IntervalFilter;
import org.apache.lucene.search.intervals.*;


import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;

class LowpassIntervalsSource extends IntervalsSource {

  final IntervalsSource in;
  private final int maxWidth;

  LowpassIntervalsSource(IntervalsSource in, int maxWidth) {
    this.in = in;
    this.maxWidth = maxWidth;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LowpassIntervalsSource that = (LowpassIntervalsSource) o;
    return maxWidth == that.maxWidth &&
        Objects.equals(in, that.in);
  }

  @Override
  public String toString() {
    return "MAXWIDTH/" + maxWidth + "(" + in + ")";
  }

  @Override
  public void extractTerms(String field, Set<Term> terms) {
    in.extractTerms(field, terms);
  }

  @Override
  public IntervalIterator intervals(String field, LeafReaderContext ctx) throws IOException {
    IntervalIterator i = in.intervals(field, ctx);
    if (i == null) {
      return null;
    }
    return new IntervalFilter(i) {
      @Override
      protected boolean accept() {
        return (i.end() - i.start()) + 1 <= maxWidth;
      }
    };
  }

  @Override
  public int hashCode() {
    return Objects.hash(in, maxWidth);
  }
}
