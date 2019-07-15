

import org.apache.lucene.util.MathUtil;
import org.apache.lucene.util.Sorter;


public abstract class IntroSorter extends Sorter {
	public IntroSorter() {
	}

	@Override
	public final void sort(int from, int to) {
		quicksort(from, to, (2 * (MathUtil.log((to - from), 2))));
	}

	void quicksort(int from, int to, int maxDepth) {
		final int mid = (from + to) >>> 1;
		if ((compare(from, mid)) > 0) {
			swap(from, mid);
		}
		if ((compare(mid, (to - 1))) > 0) {
			swap(mid, (to - 1));
			if ((compare(from, mid)) > 0) {
				swap(from, mid);
			}
		}
		int left = from + 1;
		int right = to - 2;
		setPivot(mid);
		for (; ;) {
			while ((comparePivot(right)) < 0) {
				--right;
			} 
			while ((left < right) && ((comparePivot(left)) >= 0)) {
				++left;
			} 
			if (left < right) {
				swap(left, right);
				--right;
			}else {
				break;
			}
		}
		quicksort(from, (left + 1), maxDepth);
		quicksort((left + 1), to, maxDepth);
	}

	@Override
	protected abstract void setPivot(int i);

	@Override
	protected abstract int comparePivot(int j);

	@Override
	protected int compare(int i, int j) {
		setPivot(i);
		return comparePivot(j);
	}
}

