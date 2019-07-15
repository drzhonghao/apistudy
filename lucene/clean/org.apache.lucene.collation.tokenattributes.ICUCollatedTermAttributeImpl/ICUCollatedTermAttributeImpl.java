import org.apache.lucene.collation.tokenattributes.*;



import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.apache.lucene.util.BytesRef;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RawCollationKey;

/**
 * Extension of {@link CharTermAttributeImpl} that encodes the term
 * text as a binary Unicode collation key instead of as UTF-8 bytes.
 */
public class ICUCollatedTermAttributeImpl extends CharTermAttributeImpl {
  private final Collator collator;
  private final RawCollationKey key = new RawCollationKey();

  /**
   * Create a new ICUCollatedTermAttributeImpl
   * @param collator Collation key generator
   */
  public ICUCollatedTermAttributeImpl(Collator collator) {
    // clone the collator: see http://userguide.icu-project.org/collation/architecture
    try {
      this.collator = (Collator) collator.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public BytesRef getBytesRef() {
    collator.getRawCollationKey(toString(), key);
    final BytesRef ref = this.builder.get();
    ref.bytes = key.bytes;
    ref.offset = 0;
    ref.length = key.size;
    return ref;
  }
}
