import org.apache.lucene.util.bkd.*;


import org.apache.lucene.codecs.MutablePointValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntroSelector;
import org.apache.lucene.util.IntroSorter;
import org.apache.lucene.util.MSBRadixSorter;
import org.apache.lucene.util.RadixSelector;
import org.apache.lucene.util.Selector;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.packed.PackedInts;

/** Utility APIs for sorting and partitioning buffered points.
 *
 * @lucene.internal */
public final class MutablePointsReaderUtils {

  MutablePointsReaderUtils() {}

  /** Sort the given {@link MutablePointValues} based on its packed value then doc ID. */
  public static void sort(int maxDoc, int packedBytesLength,
                          MutablePointValues reader, int from, int to) {
    final int bitsPerDocId = PackedInts.bitsRequired(maxDoc - 1);
    new MSBRadixSorter(packedBytesLength + (bitsPerDocId + 7) / 8) {

      @Override
      protected void swap(int i, int j) {
        reader.swap(i, j);
      }

      @Override
      protected int byteAt(int i, int k) {
        if (k < packedBytesLength) {
          return Byte.toUnsignedInt(reader.getByteAt(i, k));
        } else {
          final int shift = bitsPerDocId - ((k - packedBytesLength + 1) << 3);
          return (reader.getDocID(i) >>> Math.max(0, shift)) & 0xff;
        }
      }

      @Override
      protected org.apache.lucene.util.Sorter getFallbackSorter(int k) {
        return new IntroSorter() {

          final BytesRef pivot = new BytesRef();
          final BytesRef scratch = new BytesRef();
          int pivotDoc;

          @Override
          protected void swap(int i, int j) {
            reader.swap(i, j);
          }

          @Override
          protected void setPivot(int i) {
            reader.getValue(i, pivot);
            pivotDoc = reader.getDocID(i);
          }

          @Override
          protected int comparePivot(int j) {
            if (k < packedBytesLength) {
              reader.getValue(j, scratch);
              int cmp = StringHelper.compare(packedBytesLength - k, pivot.bytes, pivot.offset + k, scratch.bytes, scratch.offset + k);
              if (cmp != 0) {
                return cmp;
              }
            }
            return pivotDoc - reader.getDocID(j);
          }
        };
      }

    }.sort(from, to);
  }

  /** Sort points on the given dimension. */
  public static void sortByDim(int sortedDim, int bytesPerDim, int[] commonPrefixLengths,
                               MutablePointValues reader, int from, int to,
                               BytesRef scratch1, BytesRef scratch2) {

    // No need for a fancy radix sort here, this is called on the leaves only so
    // there are not many values to sort
    final int offset = sortedDim * bytesPerDim + commonPrefixLengths[sortedDim];
    final int numBytesToCompare = bytesPerDim - commonPrefixLengths[sortedDim];
    new IntroSorter() {

      final BytesRef pivot = scratch1;
      int pivotDoc = -1;

      @Override
      protected void swap(int i, int j) {
        reader.swap(i, j);
      }

      @Override
      protected void setPivot(int i) {
        reader.getValue(i, pivot);
        pivotDoc = reader.getDocID(i);
      }

      @Override
      protected int comparePivot(int j) {
        reader.getValue(j, scratch2);
        int cmp = StringHelper.compare(numBytesToCompare, pivot.bytes, pivot.offset + offset, scratch2.bytes, scratch2.offset + offset);
        if (cmp == 0) {
          cmp = pivotDoc - reader.getDocID(j);
        }
        return cmp;
      }
    }.sort(from, to);
  }

  /** Partition points around {@code mid}. All values on the left must be less
   *  than or equal to it and all values on the right must be greater than or
   *  equal to it. */
  public static void partition(int maxDoc, int splitDim, int bytesPerDim, int commonPrefixLen,
                               MutablePointValues reader, int from, int to, int mid,
                               BytesRef scratch1, BytesRef scratch2) {
    final int offset = splitDim * bytesPerDim + commonPrefixLen;
    final int cmpBytes = bytesPerDim - commonPrefixLen;
    final int bitsPerDocId = PackedInts.bitsRequired(maxDoc - 1);
    new RadixSelector(cmpBytes + (bitsPerDocId + 7) / 8) {

      @Override
      protected Selector getFallbackSelector(int k) {
        return new IntroSelector() {

          final BytesRef pivot = scratch1;
          int pivotDoc;

          @Override
          protected void swap(int i, int j) {
            reader.swap(i, j);
          }

          @Override
          protected void setPivot(int i) {
            reader.getValue(i, pivot);
            pivotDoc = reader.getDocID(i);
          }

          @Override
          protected int comparePivot(int j) {
            if (k < cmpBytes) {
              reader.getValue(j, scratch2);
              int cmp = StringHelper.compare(cmpBytes - k, pivot.bytes, pivot.offset + offset + k, scratch2.bytes, scratch2.offset + offset + k);
              if (cmp != 0) {
                return cmp;
              }
            }
            return pivotDoc - reader.getDocID(j);
          }
        };
      }

      @Override
      protected void swap(int i, int j) {
        reader.swap(i, j);
      }

      @Override
      protected int byteAt(int i, int k) {
        if (k < cmpBytes) {
          return Byte.toUnsignedInt(reader.getByteAt(i, offset + k));
        } else {
          final int shift = bitsPerDocId - ((k - cmpBytes + 1) << 3);
          return (reader.getDocID(i) >>> Math.max(0, shift)) & 0xff;
        }
      }
    }.select(from, to, mid);
  }
}
