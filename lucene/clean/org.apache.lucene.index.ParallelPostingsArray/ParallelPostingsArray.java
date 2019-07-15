import org.apache.lucene.index.*;


import org.apache.lucene.util.ArrayUtil;

class ParallelPostingsArray {
  final static int BYTES_PER_POSTING = 3 * Integer.BYTES;

  final int size;
  final int[] textStarts;
  final int[] intStarts;
  final int[] byteStarts;

  ParallelPostingsArray(final int size) {
    this.size = size;
    textStarts = new int[size];
    intStarts = new int[size];
    byteStarts = new int[size];
  }

  int bytesPerPosting() {
    return BYTES_PER_POSTING;
  }

  ParallelPostingsArray newInstance(int size) {
    return new ParallelPostingsArray(size);
  }

  final ParallelPostingsArray grow() {
    int newSize = ArrayUtil.oversize(size + 1, bytesPerPosting());
    ParallelPostingsArray newArray = newInstance(newSize);
    copyTo(newArray, size);
    return newArray;
  }

  void copyTo(ParallelPostingsArray toArray, int numToCopy) {
    System.arraycopy(textStarts, 0, toArray.textStarts, 0, numToCopy);
    System.arraycopy(intStarts, 0, toArray.intStarts, 0, numToCopy);
    System.arraycopy(byteStarts, 0, toArray.byteStarts, 0, numToCopy);
  }
}
