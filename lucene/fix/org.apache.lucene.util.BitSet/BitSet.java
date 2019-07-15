

import java.io.IOException;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Bits;


public abstract class BitSet implements Accountable , Bits {
	public static BitSet of(DocIdSetIterator it, int maxDoc) throws IOException {
		final long cost = it.cost();
		final int threshold = maxDoc >>> 7;
		BitSet set;
		if (cost < threshold) {
		}else {
		}
		set = null;
		set.or(it);
		set = null;
		return set;
	}

	public abstract void set(int i);

	public abstract void clear(int i);

	public abstract void clear(int startIndex, int endIndex);

	public abstract int cardinality();

	public int approximateCardinality() {
		return cardinality();
	}

	public abstract int prevSetBit(int index);

	public abstract int nextSetBit(int index);

	protected final void checkUnpositioned(DocIdSetIterator iter) {
		if ((iter.docID()) != (-1)) {
			throw new IllegalStateException(("This operation only works with an unpositioned iterator, got current position = " + (iter.docID())));
		}
	}

	public void or(DocIdSetIterator iter) throws IOException {
		checkUnpositioned(iter);
		for (int doc = iter.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = iter.nextDoc()) {
			set(doc);
		}
	}
}

