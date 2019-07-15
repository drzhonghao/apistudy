import org.apache.lucene.analysis.charfilter.*;


import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.util.ArrayUtil;

import java.io.Reader;
import java.util.Arrays;

/**
 * Base utility class for implementing a {@link CharFilter}.
 * You subclass this, and then record mappings by calling
 * {@link #addOffCorrectMap}, and then invoke the correct
 * method to correct an offset.
 */
public abstract class BaseCharFilter extends CharFilter {

  private int offsets[];
  private int diffs[];
  private int size = 0;
  
  public BaseCharFilter(Reader in) {
    super(in);
  }

  /** Retrieve the corrected offset. */
  @Override
  protected int correct(int currentOff) {
    if (offsets == null) {
      return currentOff;
    }

    int index = Arrays.binarySearch(offsets, 0, size, currentOff);
    if (index < -1) {
      index = -2 - index;
    }

    final int diff = index < 0 ? 0 : diffs[index];
    return currentOff + diff;
  }
  
  protected int getLastCumulativeDiff() {
    return offsets == null ?
      0 : diffs[size-1];
  }

  /**
   * <p>
   *   Adds an offset correction mapping at the given output stream offset.
   * </p>
   * <p>
   *   Assumption: the offset given with each successive call to this method
   *   will not be smaller than the offset given at the previous invocation.
   * </p>
   *
   * @param off The output stream offset at which to apply the correction
   * @param cumulativeDiff The input offset is given by adding this
   *                       to the output offset
   */
  protected void addOffCorrectMap(int off, int cumulativeDiff) {
    if (offsets == null) {
      offsets = new int[64];
      diffs = new int[64];
    } else if (size == offsets.length) {
      offsets = ArrayUtil.grow(offsets);
      diffs = ArrayUtil.grow(diffs);
    }
    
    assert (size == 0 || off >= offsets[size - 1])
        : "Offset #" + size + "(" + off + ") is less than the last recorded offset "
          + offsets[size - 1] + "\n" + Arrays.toString(offsets) + "\n" + Arrays.toString(diffs);
    
    if (size == 0 || off != offsets[size - 1]) {
      offsets[size] = off;
      diffs[size++] = cumulativeDiff;
    } else { // Overwrite the diff at the last recorded offset
      diffs[size - 1] = cumulativeDiff;
    }
  }
}