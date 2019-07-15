import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.*;



import java.util.Comparator;

/**
 * An {@link IntroSorter} for object arrays.
 * @lucene.internal
 */
final class ArrayIntroSorter<T> extends IntroSorter {

  private final T[] arr;
  private final Comparator<? super T> comparator;
  private T pivot;

  /** Create a new {@link ArrayInPlaceMergeSorter}. */
  public ArrayIntroSorter(T[] arr, Comparator<? super T> comparator) {
    this.arr = arr;
    this.comparator = comparator;
    pivot = null;
  }

  @Override
  protected int compare(int i, int j) {
    return comparator.compare(arr[i], arr[j]);
  }

  @Override
  protected void swap(int i, int j) {
    ArrayUtil.swap(arr, i, j);
  }

  @Override
  protected void setPivot(int i) {
    pivot = arr[i];
  }

  @Override
  protected int comparePivot(int i) {
    return comparator.compare(pivot, arr[i]);
  }

}
