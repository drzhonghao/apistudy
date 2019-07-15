

import org.apache.lucene.util.MathUtil;
import org.apache.lucene.util.Selector;
import org.apache.lucene.util.Sorter;


public abstract class IntroSelector extends Selector {
	@Override
	public final void select(int from, int to, int k) {
		final int maxDepth = 2 * (MathUtil.log((to - from), 2));
		quickSelect(from, to, k, maxDepth);
	}

	void slowSelect(int from, int to, int k) {
		new Sorter() {
			@Override
			protected void swap(int i, int j) {
				IntroSelector.this.swap(i, j);
			}

			@Override
			protected int compare(int i, int j) {
				return IntroSelector.this.compare(i, j);
			}

			public void sort(int from, int to) {
			}
		}.sort(from, to);
	}

	private void quickSelect(int from, int to, int k, int maxDepth) {
		assert from <= k;
		assert k < to;
		if ((to - from) == 1) {
			return;
		}
		if ((--maxDepth) < 0) {
			slowSelect(from, to, k);
			return;
		}
		final int mid = (from + to) >>> 1;
		if ((compare(from, (to - 1))) > 0) {
			swap(from, (to - 1));
		}
		if ((compare((to - 1), mid)) > 0) {
			swap((to - 1), mid);
			if ((compare(from, (to - 1))) > 0) {
				swap(from, (to - 1));
			}
		}
		setPivot((to - 1));
		int left = from + 1;
		int right = to - 2;
		for (; ;) {
			while ((comparePivot(left)) > 0) {
				++left;
			} 
			while ((left < right) && ((comparePivot(right)) <= 0)) {
				--right;
			} 
			if (left < right) {
				swap(left, right);
				--right;
			}else {
				break;
			}
		}
		swap(left, (to - 1));
		if (left == k) {
			return;
		}else
			if (left < k) {
				quickSelect((left + 1), to, k, maxDepth);
			}else {
				quickSelect(from, left, k, maxDepth);
			}

	}

	protected int compare(int i, int j) {
		setPivot(i);
		return comparePivot(j);
	}

	protected abstract void setPivot(int i);

	protected abstract int comparePivot(int j);
}

