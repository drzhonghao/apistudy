import org.apache.lucene.search.spans.SpanPositionRangeQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.*;



import org.apache.lucene.search.spans.FilterSpans.AcceptStatus;

import java.io.IOException;

/**
 * Matches spans near the beginning of a field.
 * <p>
 * This class is a simple extension of {@link SpanPositionRangeQuery} in that it assumes the
 * start to be zero and only checks the end boundary.
 */
public class SpanFirstQuery extends SpanPositionRangeQuery {

  /** Construct a SpanFirstQuery matching spans in <code>match</code> whose end
   * position is less than or equal to <code>end</code>. */
  public SpanFirstQuery(SpanQuery match, int end) {
    super(match, 0, end);
  }

  protected AcceptStatus acceptPosition(Spans spans) throws IOException {
    assert spans.startPosition() != spans.endPosition() : "start equals end: " + spans.startPosition();
    if (spans.startPosition() >= end)
      return AcceptStatus.NO_MORE_IN_CURRENT_DOC;
    else if (spans.endPosition() <= end)
      return AcceptStatus.YES;
    else
      return AcceptStatus.NO;
  }

  @Override
  public String toString(String field) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("spanFirst(");
    buffer.append(match.toString(field));
    buffer.append(", ");
    buffer.append(end);
    buffer.append(")");
    return buffer.toString();
  }

}
