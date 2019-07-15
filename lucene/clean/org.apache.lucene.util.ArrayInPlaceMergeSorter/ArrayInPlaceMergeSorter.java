import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.*;



import java.util.Comparator;

/**
 * An {@link InPlaceMergeSorter} for object arrays.
 * @lucene.internal
 */
final class ArrayInPlaceMergeSorter<T> extends InPlaceMergeSorter {

  private final T[] arr;
  private final Comparator<? super T> comparator;

  /** Create a new {@link ArrayInPlaceMergeSorter}. */
  public ArrayInPlaceMergeSorter(T[] arr, Comparator<? super T> comparator) {
    this.arr = arr;
    this.comparator = comparator;
  }

  @Override
  protected int compare(int i, int j) {
    return comparator.compare(arr[i], arr[j]);
  }

  @Override
  protected void swap(int i, int j) {
    ArrayUtil.swap(arr, i, j);
  }

}
