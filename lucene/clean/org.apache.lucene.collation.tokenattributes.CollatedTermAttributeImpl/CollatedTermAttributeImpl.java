import org.apache.lucene.collation.tokenattributes.*;



import java.text.Collator;

import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.apache.lucene.util.BytesRef;

/**
 * Extension of {@link CharTermAttributeImpl} that encodes the term
 * text as a binary Unicode collation key instead of as UTF-8 bytes.
 */
public class CollatedTermAttributeImpl extends CharTermAttributeImpl {
  private final Collator collator;

  /**
   * Create a new CollatedTermAttributeImpl
   * @param collator Collation key generator
   */
  public CollatedTermAttributeImpl(Collator collator) {
    // clone in case JRE doesn't properly sync,
    // or to reduce contention in case they do
    this.collator = (Collator) collator.clone();
  }
  
  @Override
  public BytesRef getBytesRef() {
    final BytesRef ref = this.builder.get();
    ref.bytes = collator.getCollationKey(toString()).toByteArray();
    ref.offset = 0;
    ref.length = ref.bytes.length;
    return ref;
  }

}
