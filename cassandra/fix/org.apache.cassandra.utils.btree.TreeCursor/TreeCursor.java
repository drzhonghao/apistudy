

import java.util.Comparator;
import org.apache.cassandra.utils.btree.BTree;


class TreeCursor<K> {
	TreeCursor(Comparator<? super K> comparator, Object[] node) {
	}

	void reset(boolean start) {
	}

	int moveOne(boolean forwards) {
		if (forwards) {
		}
		return 0;
	}

	boolean seekTo(K key, boolean forwards, boolean skipOne) {
		boolean tryOne = !skipOne;
		if (tryOne) {
			int cmp;
			cmp = 0;
			if (forwards ? cmp >= 0 : cmp <= 0) {
				return cmp == 0;
			}
		}
		boolean match;
		match = false;
		if (!match) {
		}
		match = false;
		return match;
	}

	void seekTo(int index) {
		if ((index < 0) | (index >= (BTree.size(rootNode())))) {
			if ((index < (-1)) | (index > (BTree.size(rootNode()))))
				throw new IndexOutOfBoundsException((((index + " not in range [0..") + (BTree.size(rootNode()))) + ")"));

			reset((index == (-1)));
			return;
		}
		while (true) {
		} 
	}

	Object[] rootNode() {
		return null;
	}

	K currentValue() {
		return null;
	}
}

