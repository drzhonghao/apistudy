

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.lucene.facet.taxonomy.ParallelTaxonomyArrays;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.RamUsageEstimator;


class TaxonomyIndexArrays extends ParallelTaxonomyArrays implements Accountable {
	private final int[] parents;

	private volatile boolean initializedChildren = false;

	private int[] children;

	private int[] siblings;

	private TaxonomyIndexArrays(int[] parents) {
		this.parents = parents;
	}

	public TaxonomyIndexArrays(IndexReader reader) throws IOException {
		parents = new int[reader.maxDoc()];
		if ((parents.length) > 0) {
			initParents(reader, 0);
			parents[0] = TaxonomyReader.INVALID_ORDINAL;
		}
	}

	public TaxonomyIndexArrays(IndexReader reader, TaxonomyIndexArrays copyFrom) throws IOException {
		assert copyFrom != null;
		int[] copyParents = copyFrom.parents();
		this.parents = new int[reader.maxDoc()];
		System.arraycopy(copyParents, 0, parents, 0, copyParents.length);
		initParents(reader, copyParents.length);
		if (copyFrom.initializedChildren) {
			initChildrenSiblings(copyFrom);
		}
	}

	private synchronized final void initChildrenSiblings(TaxonomyIndexArrays copyFrom) {
		if (!(initializedChildren)) {
			children = new int[parents.length];
			siblings = new int[parents.length];
			if (copyFrom != null) {
				System.arraycopy(copyFrom.children(), 0, children, 0, copyFrom.children().length);
				System.arraycopy(copyFrom.siblings(), 0, siblings, 0, copyFrom.siblings().length);
				computeChildrenSiblings(copyFrom.parents.length);
			}else {
				computeChildrenSiblings(0);
			}
			initializedChildren = true;
		}
	}

	private void computeChildrenSiblings(int first) {
		for (int i = first; i < (parents.length); i++) {
			children[i] = TaxonomyReader.INVALID_ORDINAL;
		}
		if (first == 0) {
			first = 1;
			siblings[0] = TaxonomyReader.INVALID_ORDINAL;
		}
		for (int i = first; i < (parents.length); i++) {
			siblings[i] = children[parents[i]];
			children[parents[i]] = i;
		}
	}

	private void initParents(IndexReader reader, int first) throws IOException {
		if ((reader.maxDoc()) == first) {
			return;
		}
		int num = reader.maxDoc();
		for (int i = first; i < num; i++) {
		}
	}

	TaxonomyIndexArrays add(int ordinal, int parentOrdinal) {
		if (ordinal >= (parents.length)) {
			int[] newarray = ArrayUtil.grow(parents, (ordinal + 1));
			newarray[ordinal] = parentOrdinal;
			return new TaxonomyIndexArrays(newarray);
		}
		parents[ordinal] = parentOrdinal;
		return this;
	}

	@Override
	public int[] parents() {
		return parents;
	}

	@Override
	public int[] children() {
		if (!(initializedChildren)) {
			initChildrenSiblings(null);
		}
		return children;
	}

	@Override
	public int[] siblings() {
		if (!(initializedChildren)) {
			initChildrenSiblings(null);
		}
		return siblings;
	}

	@Override
	public synchronized long ramBytesUsed() {
		long ramBytesUsed = ((RamUsageEstimator.NUM_BYTES_OBJECT_HEADER) + (3 * (RamUsageEstimator.NUM_BYTES_OBJECT_REF))) + (RamUsageEstimator.NUM_BYTES_BOOLEAN);
		ramBytesUsed += RamUsageEstimator.shallowSizeOf(parents);
		if ((children) != null) {
			ramBytesUsed += RamUsageEstimator.shallowSizeOf(children);
		}
		if ((siblings) != null) {
			ramBytesUsed += RamUsageEstimator.shallowSizeOf(siblings);
		}
		return ramBytesUsed;
	}

	@Override
	public synchronized Collection<Accountable> getChildResources() {
		final List<Accountable> resources = new ArrayList<>();
		resources.add(Accountables.namedAccountable("parents", RamUsageEstimator.shallowSizeOf(parents)));
		if ((children) != null) {
			resources.add(Accountables.namedAccountable("children", RamUsageEstimator.shallowSizeOf(children)));
		}
		if ((siblings) != null) {
			resources.add(Accountables.namedAccountable("siblings", RamUsageEstimator.shallowSizeOf(siblings)));
		}
		return Collections.unmodifiableList(resources);
	}
}

