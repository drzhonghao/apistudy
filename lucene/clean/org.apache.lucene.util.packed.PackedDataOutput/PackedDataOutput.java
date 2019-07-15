import org.apache.lucene.util.packed.*;



import java.io.IOException;

import org.apache.lucene.store.DataOutput;

/**
 * A {@link DataOutput} wrapper to write unaligned, variable-length packed
 * integers.
 * @see PackedDataInput
 * @lucene.internal
 */
public final class PackedDataOutput {

  final DataOutput out;
  long current;
  int remainingBits;

  /**
   * Create a new instance that wraps <code>out</code>.
   */
  public PackedDataOutput(DataOutput out) {
    this.out = out;
    current = 0;
    remainingBits = 8;
  }

  /**
   * Write a value using exactly <code>bitsPerValue</code> bits.
   */
  public void writeLong(long value, int bitsPerValue) throws IOException {
    assert bitsPerValue == 64 || (value >= 0 && value <= PackedInts.maxValue(bitsPerValue));
    while (bitsPerValue > 0) {
      if (remainingBits == 0) {
        out.writeByte((byte) current);
        current = 0L;
        remainingBits = 8;
      }
      final int bits = Math.min(remainingBits, bitsPerValue);
      current = current | (((value >>> (bitsPerValue - bits)) & ((1L << bits) - 1)) << (remainingBits - bits));
      bitsPerValue -= bits;
      remainingBits -= bits;
    }
  }

  /**
   * Flush pending bits to the underlying {@link DataOutput}.
   */
  public void flush() throws IOException {
    if (remainingBits < 8) {
      out.writeByte((byte) current);
    }
    remainingBits = 8;
    current = 0L;
  }

}
