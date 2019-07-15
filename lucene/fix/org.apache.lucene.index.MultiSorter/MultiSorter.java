

import java.io.IOException;
import java.util.List;
import org.apache.lucene.index.CodecReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.PriorityQueue;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PackedLongValues;


@SuppressWarnings({ "unchecked", "rawtypes" })
final class MultiSorter {
	static MergeState.DocMap[] sort(Sort sort, List<CodecReader> readers) throws IOException {
		SortField[] fields = sort.getSort();
		final MultiSorter.ComparableProvider[][] comparables = new MultiSorter.ComparableProvider[fields.length][];
		for (int i = 0; i < (fields.length); i++) {
			comparables[i] = MultiSorter.getComparableProviders(readers, fields[i]);
		}
		int leafCount = readers.size();
		PriorityQueue<MultiSorter.LeafAndDocID> queue = new PriorityQueue<MultiSorter.LeafAndDocID>(leafCount) {
			@Override
			public boolean lessThan(MultiSorter.LeafAndDocID a, MultiSorter.LeafAndDocID b) {
				for (int i = 0; i < (comparables.length); i++) {
					int cmp = a.values[i].compareTo(b.values[i]);
					if (cmp != 0) {
						return cmp < 0;
					}
				}
				if ((a.readerIndex) != (b.readerIndex)) {
					return (a.readerIndex) < (b.readerIndex);
				}else {
					return (a.docID) < (b.docID);
				}
			}
		};
		PackedLongValues.Builder[] builders = new PackedLongValues.Builder[leafCount];
		for (int i = 0; i < leafCount; i++) {
			CodecReader reader = readers.get(i);
			MultiSorter.LeafAndDocID leaf = new MultiSorter.LeafAndDocID(i, reader.getLiveDocs(), reader.maxDoc(), comparables.length);
			for (int j = 0; j < (comparables.length); j++) {
				leaf.values[j] = comparables[j][i].getComparable(leaf.docID);
				assert (leaf.values[j]) != null;
			}
			queue.add(leaf);
			builders[i] = PackedLongValues.monotonicBuilder(PackedInts.COMPACT);
		}
		int mappedDocID = 0;
		int lastReaderIndex = 0;
		boolean isSorted = true;
		while ((queue.size()) != 0) {
			MultiSorter.LeafAndDocID top = queue.top();
			if (lastReaderIndex > (top.readerIndex)) {
				isSorted = false;
			}
			lastReaderIndex = top.readerIndex;
			builders[top.readerIndex].add(mappedDocID);
			if (((top.liveDocs) == null) || (top.liveDocs.get(top.docID))) {
				mappedDocID++;
			}
			(top.docID)++;
			if ((top.docID) < (top.maxDoc)) {
				for (int j = 0; j < (comparables.length); j++) {
					top.values[j] = comparables[j][top.readerIndex].getComparable(top.docID);
					assert (top.values[j]) != null;
				}
				queue.updateTop();
			}else {
				queue.pop();
			}
		} 
		if (isSorted) {
			return null;
		}
		MergeState.DocMap[] docMaps = new MergeState.DocMap[leafCount];
		for (int i = 0; i < leafCount; i++) {
			final PackedLongValues remapped = builders[i].build();
			final Bits liveDocs = readers.get(i).getLiveDocs();
			docMaps[i] = new MergeState.DocMap() {
				@Override
				public int get(int docID) {
					if ((liveDocs == null) || (liveDocs.get(docID))) {
						return ((int) (remapped.get(docID)));
					}else {
						return -1;
					}
				}
			};
		}
		return docMaps;
	}

	private static class LeafAndDocID {
		final int readerIndex;

		final Bits liveDocs;

		final int maxDoc;

		final Comparable[] values;

		int docID;

		public LeafAndDocID(int readerIndex, Bits liveDocs, int maxDoc, int numComparables) {
			this.readerIndex = readerIndex;
			this.liveDocs = liveDocs;
			this.maxDoc = maxDoc;
			this.values = new Comparable[numComparables];
		}
	}

	private interface ComparableProvider {
		public Comparable getComparable(int docID) throws IOException;
	}

	private static MultiSorter.ComparableProvider[] getComparableProviders(List<CodecReader> readers, SortField sortField) throws IOException {
		MultiSorter.ComparableProvider[] providers = new MultiSorter.ComparableProvider[readers.size()];
		final int reverseMul = (sortField.getReverse()) ? -1 : 1;
		return providers;
	}
}

