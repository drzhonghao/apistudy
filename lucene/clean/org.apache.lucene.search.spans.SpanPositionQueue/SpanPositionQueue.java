import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.*;



import org.apache.lucene.util.PriorityQueue;

class SpanPositionQueue extends PriorityQueue<Spans> {
  SpanPositionQueue(int maxSize) {
    super(maxSize, false); // do not prepopulate
  }

  protected boolean lessThan(Spans s1, Spans s2) {
    int start1 = s1.startPosition();
    int start2 = s2.startPosition();
    return (start1 < start2) ? true
          : (start1 == start2) ? s1.endPosition() < s2.endPosition()
          : false;
  }
}

