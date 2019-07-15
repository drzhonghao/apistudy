import org.apache.lucene.payloads.*;


import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * SpanCollector for collecting payloads
 */
public class PayloadSpanCollector implements SpanCollector {

  private final Collection<byte[]> payloads = new ArrayList<>();

  @Override
  public void collectLeaf(PostingsEnum postings, int position, Term term) throws IOException {
    BytesRef payload = postings.getPayload();
    if (payload == null)
      return;
    final byte[] bytes = new byte[payload.length];
    System.arraycopy(payload.bytes, payload.offset, bytes, 0, payload.length);
    payloads.add(bytes);
  }

  @Override
  public void reset() {
    payloads.clear();
  }

  /**
   * @return the collected payloads
   */
  public Collection<byte[]> getPayloads() {
    return payloads;
  }
}
