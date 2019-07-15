

import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSelector;
import org.apache.lucene.search.SortedSetSelector;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.Bits;

import static org.apache.lucene.search.SortField.Type.LONG;
import static org.apache.lucene.search.SortedSetSelector.Type.MAX;
import static org.apache.lucene.search.SortedSetSelector.Type.MIN;


public class BlockJoinSelector {
	private BlockJoinSelector() {
	}

	public enum Type {

		MIN,
		MAX;}

	public static Bits wrap(final Bits docsWithValue, BitSet parents, BitSet children) {
		return new Bits() {
			@Override
			public boolean get(int docID) {
				assert parents.get(docID) : "this selector may only be used on parent documents";
				if (docID == 0) {
					return false;
				}
				final int firstChild = (parents.prevSetBit((docID - 1))) + 1;
				for (int child = children.nextSetBit(firstChild); child < docID; child = children.nextSetBit((child + 1))) {
					if (docsWithValue.get(child)) {
						return true;
					}
				}
				return false;
			}

			@Override
			public int length() {
				return docsWithValue.length();
			}
		};
	}

	@Deprecated
	public static SortedDocValues wrap(SortedSetDocValues sortedSet, BlockJoinSelector.Type selection, BitSet parents, BitSet children) {
		return BlockJoinSelector.wrap(sortedSet, selection, parents, BlockJoinSelector.toIter(children));
	}

	public static SortedDocValues wrap(SortedSetDocValues sortedSet, BlockJoinSelector.Type selection, BitSet parents, DocIdSetIterator children) {
		SortedDocValues values;
		switch (selection) {
			case MIN :
				values = SortedSetSelector.wrap(sortedSet, MIN);
				break;
			case MAX :
				values = SortedSetSelector.wrap(sortedSet, MAX);
				break;
			default :
				throw new AssertionError();
		}
		return BlockJoinSelector.wrap(values, selection, parents, children);
	}

	@Deprecated
	public static SortedDocValues wrap(final SortedDocValues values, BlockJoinSelector.Type selection, BitSet parents, BitSet children) {
		return BlockJoinSelector.wrap(values, selection, parents, BlockJoinSelector.toIter(children));
	}

	public static SortedDocValues wrap(final SortedDocValues values, BlockJoinSelector.Type selection, BitSet parents, DocIdSetIterator children) {
		if ((values.docID()) != (-1)) {
			throw new IllegalArgumentException(("values iterator was already consumed: values.docID=" + (values.docID())));
		}
		return null;
	}

	@Deprecated
	public static NumericDocValues wrap(SortedNumericDocValues sortedNumerics, BlockJoinSelector.Type selection, BitSet parents, BitSet children) {
		return BlockJoinSelector.wrap(sortedNumerics, selection, parents, BlockJoinSelector.toIter(children));
	}

	protected static BitSetIterator toIter(BitSet children) {
		return new BitSetIterator(children, 0);
	}

	public static NumericDocValues wrap(SortedNumericDocValues sortedNumerics, BlockJoinSelector.Type selection, BitSet parents, DocIdSetIterator children) {
		NumericDocValues values;
		switch (selection) {
			case MIN :
				values = SortedNumericSelector.wrap(sortedNumerics, SortedNumericSelector.Type.MIN, LONG);
				break;
			case MAX :
				values = SortedNumericSelector.wrap(sortedNumerics, SortedNumericSelector.Type.MAX, LONG);
				break;
			default :
				throw new AssertionError();
		}
		return BlockJoinSelector.wrap(values, selection, parents, children);
	}

	@Deprecated
	public static NumericDocValues wrap(final NumericDocValues values, BlockJoinSelector.Type selection, BitSet parents, BitSet children) {
		return BlockJoinSelector.wrap(values, selection, parents, BlockJoinSelector.toIter(children));
	}

	public static NumericDocValues wrap(final NumericDocValues values, BlockJoinSelector.Type selection, BitSet parents, DocIdSetIterator children) {
		if ((values.docID()) != (-1)) {
			throw new IllegalArgumentException(("values iterator was already consumed: values.docID=" + (values.docID())));
		}
		return null;
	}
}

