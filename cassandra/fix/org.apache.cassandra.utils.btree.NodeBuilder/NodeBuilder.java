

import java.util.Comparator;
import org.apache.cassandra.utils.ObjectSizes;
import org.apache.cassandra.utils.btree.BTree;
import org.apache.cassandra.utils.btree.UpdateFunction;


final class NodeBuilder {
	private NodeBuilder parent;

	private NodeBuilder child;

	private int buildKeyPosition;

	private int buildChildPosition;

	private int maxBuildKeyPosition;

	private Object[] copyFrom;

	private int copyFromKeyPosition;

	private int copyFromChildPosition;

	private UpdateFunction updateFunction;

	private Comparator comparator;

	private Object upperBound;

	void clear() {
		NodeBuilder current = this;
		while ((current != null) && ((current.upperBound) != null)) {
			current.clearSelf();
			current = current.child;
		} 
		current = parent;
		while ((current != null) && ((current.upperBound) != null)) {
			current.clearSelf();
			current = current.parent;
		} 
	}

	void clearSelf() {
		reset(null, null, null, null);
		maxBuildKeyPosition = 0;
	}

	void reset(Object[] copyFrom, Object upperBound, UpdateFunction updateFunction, Comparator comparator) {
		this.copyFrom = copyFrom;
		this.upperBound = upperBound;
		this.updateFunction = updateFunction;
		this.comparator = comparator;
		maxBuildKeyPosition = Math.max(maxBuildKeyPosition, buildKeyPosition);
		buildKeyPosition = 0;
		buildChildPosition = 0;
		copyFromKeyPosition = 0;
		copyFromChildPosition = 0;
	}

	NodeBuilder finish() {
		assert (copyFrom) != null;
		if (((buildKeyPosition) + (buildChildPosition)) > 0) {
		}
		return isRoot() ? null : ascend();
	}

	NodeBuilder update(Object key) {
		assert (copyFrom) != null;
		int i = copyFromKeyPosition;
		boolean found;
		boolean owns = true;
		found = false;
		if (found) {
			Object prev = copyFrom[i];
			Object next = updateFunction.apply(prev, key);
			if (prev == next)
				return null;

			key = next;
		}else {
		}
		return ascend();
	}

	private static <V> int compareUpperBound(Comparator<V> comparator, Object value, Object upperBound) {
		return 0;
	}

	boolean isRoot() {
		return false;
	}

	NodeBuilder ascendToRoot() {
		NodeBuilder current = this;
		while (!(current.isRoot()))
			current = current.ascend();

		return current;
	}

	Object[] toNode() {
		return null;
	}

	private NodeBuilder ascend() {
		ensureParent();
		return parent;
	}

	private void copyKeys(int upToKeyPosition) {
		if ((copyFromKeyPosition) >= upToKeyPosition)
			return;

		int len = upToKeyPosition - (copyFromKeyPosition);
		ensureRoom(((buildKeyPosition) + len));
		if (len > 0) {
			copyFromKeyPosition = upToKeyPosition;
			buildKeyPosition += len;
		}
	}

	private void replaceNextKey(Object with) {
		ensureRoom(((buildKeyPosition) + 1));
		(copyFromKeyPosition)++;
	}

	void addNewKey(Object key) {
		ensureRoom(((buildKeyPosition) + 1));
	}

	private void copyChildren(int upToChildPosition) {
		if ((copyFromChildPosition) >= upToChildPosition)
			return;

		int len = upToChildPosition - (copyFromChildPosition);
		if (len > 0) {
			copyFromChildPosition = upToChildPosition;
			buildChildPosition += len;
		}
	}

	private void addExtraChild(Object[] child, Object upperBound) {
		ensureRoom(((buildKeyPosition) + 1));
	}

	private void finishChild(Object[] child) {
		(copyFromChildPosition)++;
	}

	private void ensureRoom(int nextBuildKeyPosition) {
		if ((buildChildPosition) > 0) {
		}
	}

	private Object[] buildFromRange(int offset, int keyLength, boolean isLeaf, boolean isExtra) {
		if (keyLength == 0)
			return copyFrom;

		Object[] a;
		if (isLeaf) {
			a = new Object[keyLength | 1];
		}else {
			a = new Object[2 + (keyLength * 2)];
			int[] indexOffsets = new int[keyLength + 1];
			int size = BTree.size(((Object[]) (a[keyLength])));
			for (int i = 0; i < keyLength; i++) {
				indexOffsets[i] = size;
				size += 1 + (BTree.size(((Object[]) (a[((keyLength + 1) + i)]))));
			}
			indexOffsets[keyLength] = size;
			a[((a.length) - 1)] = indexOffsets;
		}
		if (isExtra)
			updateFunction.allocated(ObjectSizes.sizeOfArray(a));
		else
			if ((a.length) != (copyFrom.length))
				updateFunction.allocated(((ObjectSizes.sizeOfArray(a)) - ((copyFrom.length) == 0 ? 0 : ObjectSizes.sizeOfArray(copyFrom))));


		return a;
	}

	private NodeBuilder ensureParent() {
		if ((parent) == null) {
			parent = new NodeBuilder();
			parent.child = this;
		}
		if ((parent.upperBound) == null) {
		}
		return parent;
	}

	NodeBuilder ensureChild() {
		if ((child) == null) {
			child = new NodeBuilder();
			child.parent = this;
		}
		return child;
	}
}

