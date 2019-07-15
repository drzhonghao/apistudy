import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.*;


abstract class StringMSBRadixSorter extends MSBRadixSorter {

  StringMSBRadixSorter() {
    super(Integer.MAX_VALUE);
  }

  /** Get a {@link BytesRef} for the given index. */
  protected abstract BytesRef get(int i);

  @Override
  protected int byteAt(int i, int k) {
    BytesRef ref = get(i);
    if (ref.length <= k) {
      return -1;
    }
    return ref.bytes[ref.offset + k] & 0xff;
  }

  @Override
  protected Sorter getFallbackSorter(int k) {
    return new IntroSorter() {

      private void get(int i, int k, BytesRef scratch) {
        BytesRef ref = StringMSBRadixSorter.this.get(i);
        assert ref.length >= k;
        scratch.bytes = ref.bytes;
        scratch.offset = ref.offset + k;
        scratch.length = ref.length - k;
      }

      @Override
      protected void swap(int i, int j) {
        StringMSBRadixSorter.this.swap(i, j);
      }

      @Override
      protected int compare(int i, int j) {
        get(i, k, scratch1);
        get(j, k, scratch2);
        return scratch1.compareTo(scratch2);
      }

      @Override
      protected void setPivot(int i) {
        get(i, k, pivot);
      }

      @Override
      protected int comparePivot(int j) {
        get(j, k, scratch2);
        return pivot.compareTo(scratch2);
      }

      private final BytesRef pivot = new BytesRef(),
          scratch1 = new BytesRef(), scratch2 = new BytesRef();
    };
  }
}
