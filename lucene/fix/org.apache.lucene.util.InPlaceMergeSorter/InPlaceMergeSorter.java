

import org.apache.lucene.util.Sorter;


public abstract class InPlaceMergeSorter extends Sorter {
	public InPlaceMergeSorter() {
	}

	@Override
	public final void sort(int from, int to) {
		mergeSort(from, to);
	}

	void mergeSort(int from, int to) {
	}
}

