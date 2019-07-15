import org.apache.lucene.util.packed.*;



import java.io.IOException;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.RandomAccessInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.LongValues;
import org.apache.lucene.util.RamUsageEstimator;

/**
 * Retrieves an instance previously written by {@link DirectMonotonicWriter}.
 * @see DirectMonotonicWriter 
 */
public final class DirectMonotonicReader {

  /** An instance that always returns {@code 0}. */
  private static final LongValues EMPTY = new LongValues() {

    @Override
    public long get(long index) {
      return 0;
    }

  };

  /** In-memory metadata that needs to be kept around for
   *  {@link DirectMonotonicReader} to read data from disk. */
  public static class Meta implements Accountable {
    private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(Meta.class);

    final int blockShift;
    final int numBlocks;
    final long[] mins;
    final float[] avgs;
    final byte[] bpvs;
    final long[] offsets;

    Meta(long numValues, int blockShift) {
      this.blockShift = blockShift;
      long numBlocks = numValues >>> blockShift;
      if ((numBlocks << blockShift) < numValues) {
        numBlocks += 1;
      }
      this.numBlocks = (int) numBlocks;
      this.mins = new long[this.numBlocks];
      this.avgs = new float[this.numBlocks];
      this.bpvs = new byte[this.numBlocks];
      this.offsets = new long[this.numBlocks];
    }

    @Override
    public long ramBytesUsed() {
      return BASE_RAM_BYTES_USED
          + RamUsageEstimator.sizeOf(mins)
          + RamUsageEstimator.sizeOf(avgs)
          + RamUsageEstimator.sizeOf(bpvs)
          + RamUsageEstimator.sizeOf(offsets);
    }
  }

  /** Load metadata from the given {@link IndexInput}.
   *  @see DirectMonotonicReader#getInstance(Meta, RandomAccessInput) */
  public static Meta loadMeta(IndexInput metaIn, long numValues, int blockShift) throws IOException {
    Meta meta = new Meta(numValues, blockShift);
    for (int i = 0; i < meta.numBlocks; ++i) {
      meta.mins[i] = metaIn.readLong();
      meta.avgs[i] = Float.intBitsToFloat(metaIn.readInt());
      meta.offsets[i] = metaIn.readLong();
      meta.bpvs[i] = metaIn.readByte();
    }
    return meta;
  }

  /**
   * Retrieves an instance from the specified slice.
   */
  public static LongValues getInstance(Meta meta, RandomAccessInput data) throws IOException {
    final LongValues[] readers = new LongValues[meta.numBlocks];
    for (int i = 0; i < meta.mins.length; ++i) {
      if (meta.bpvs[i] == 0) {
        readers[i] = EMPTY;
      } else {
        readers[i] = DirectReader.getInstance(data, meta.bpvs[i], meta.offsets[i]);
      }
    }
    final int blockShift = meta.blockShift;

    final long[] mins = meta.mins;
    final float[] avgs = meta.avgs;
    return new LongValues() {

      @Override
      public long get(long index) {
        final int block = (int) (index >>> blockShift);
        final long blockIndex = index & ((1 << blockShift) - 1);
        final long delta = readers[block].get(blockIndex);
        return mins[block] + (long) (avgs[block] * blockIndex) + delta;
      }

    };
  }
}
