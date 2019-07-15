

import java.io.IOException;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.LeafFieldComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.grouping.GroupDocs;
import org.apache.lucene.search.grouping.TopGroups;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.PriorityQueue;


public class BlockGroupingCollector extends SimpleCollector {
	private int[] pendingSubDocs;

	private float[] pendingSubScores;

	private int subDocUpto;

	private final Sort groupSort;

	private final int topNGroups;

	private final Weight lastDocPerGroup;

	private final boolean needsScores;

	private final FieldComparator<?>[] comparators;

	private final LeafFieldComparator[] leafComparators;

	private final int[] reversed;

	private final int compIDXEnd;

	private int bottomSlot;

	private boolean queueFull;

	private LeafReaderContext currentReaderContext;

	private int topGroupDoc;

	private int totalHitCount;

	private int totalGroupCount;

	private int docBase;

	private int groupEndDocID;

	private DocIdSetIterator lastDocPerGroupBits;

	private Scorer scorer;

	private final BlockGroupingCollector.GroupQueue groupQueue;

	private boolean groupCompetes;

	private static final class OneGroup {
		LeafReaderContext readerContext;

		int topGroupDoc;

		int[] docs;

		float[] scores;

		int count;

		int comparatorSlot;
	}

	private final class GroupQueue extends PriorityQueue<BlockGroupingCollector.OneGroup> {
		public GroupQueue(int size) {
			super(size);
		}

		@Override
		protected boolean lessThan(final BlockGroupingCollector.OneGroup group1, final BlockGroupingCollector.OneGroup group2) {
			assert group1 != group2;
			assert (group1.comparatorSlot) != (group2.comparatorSlot);
			final int numComparators = comparators.length;
			for (int compIDX = 0; compIDX < numComparators; compIDX++) {
				final int c = (reversed[compIDX]) * (comparators[compIDX].compare(group1.comparatorSlot, group2.comparatorSlot));
				if (c != 0) {
					return c > 0;
				}
			}
			return (group1.topGroupDoc) > (group2.topGroupDoc);
		}
	}

	private void processGroup() throws IOException {
		(totalGroupCount)++;
		if (groupCompetes) {
			if (!(queueFull)) {
				final BlockGroupingCollector.OneGroup og = new BlockGroupingCollector.OneGroup();
				og.count = subDocUpto;
				og.topGroupDoc = (docBase) + (topGroupDoc);
				og.docs = pendingSubDocs;
				pendingSubDocs = new int[10];
				if (needsScores) {
					og.scores = pendingSubScores;
					pendingSubScores = new float[10];
				}
				og.readerContext = currentReaderContext;
				og.comparatorSlot = bottomSlot;
				final BlockGroupingCollector.OneGroup bottomGroup = groupQueue.add(og);
				queueFull = (groupQueue.size()) == (topNGroups);
				if (queueFull) {
					bottomSlot = bottomGroup.comparatorSlot;
					for (int i = 0; i < (comparators.length); i++) {
						leafComparators[i].setBottom(bottomSlot);
					}
				}else {
					bottomSlot = groupQueue.size();
				}
			}else {
				final BlockGroupingCollector.OneGroup og = groupQueue.top();
				assert og != null;
				og.count = subDocUpto;
				og.topGroupDoc = (docBase) + (topGroupDoc);
				final int[] savDocs = og.docs;
				og.docs = pendingSubDocs;
				pendingSubDocs = savDocs;
				if (needsScores) {
					final float[] savScores = og.scores;
					og.scores = pendingSubScores;
					pendingSubScores = savScores;
				}
				og.readerContext = currentReaderContext;
				bottomSlot = groupQueue.updateTop().comparatorSlot;
				for (int i = 0; i < (comparators.length); i++) {
					leafComparators[i].setBottom(bottomSlot);
				}
			}
		}
		subDocUpto = 0;
	}

	public BlockGroupingCollector(Sort groupSort, int topNGroups, boolean needsScores, Weight lastDocPerGroup) {
		if (topNGroups < 1) {
			throw new IllegalArgumentException((("topNGroups must be >= 1 (got " + topNGroups) + ")"));
		}
		groupQueue = new BlockGroupingCollector.GroupQueue(topNGroups);
		pendingSubDocs = new int[10];
		if (needsScores) {
			pendingSubScores = new float[10];
		}
		this.needsScores = needsScores;
		this.lastDocPerGroup = lastDocPerGroup;
		this.groupSort = groupSort;
		this.topNGroups = topNGroups;
		final SortField[] sortFields = groupSort.getSort();
		comparators = new FieldComparator<?>[sortFields.length];
		leafComparators = new LeafFieldComparator[sortFields.length];
		compIDXEnd = (comparators.length) - 1;
		reversed = new int[sortFields.length];
		for (int i = 0; i < (sortFields.length); i++) {
			final SortField sortField = sortFields[i];
			comparators[i] = sortField.getComparator(topNGroups, i);
			reversed[i] = (sortField.getReverse()) ? -1 : 1;
		}
	}

	public TopGroups<?> getTopGroups(Sort withinGroupSort, int groupOffset, int withinGroupOffset, int maxDocsPerGroup, boolean fillSortFields) throws IOException {
		if ((subDocUpto) != 0) {
			processGroup();
		}
		if (groupOffset >= (groupQueue.size())) {
			return null;
		}
		int totalGroupedHitCount = 0;
		float maxScore = Float.MIN_VALUE;
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final GroupDocs<Object>[] groups = new GroupDocs[(groupQueue.size()) - groupOffset];
		for (int downTo = ((groupQueue.size()) - groupOffset) - 1; downTo >= 0; downTo--) {
			final BlockGroupingCollector.OneGroup og = groupQueue.pop();
			final TopDocsCollector<?> collector;
			if (withinGroupSort.equals(Sort.RELEVANCE)) {
				if (!(needsScores)) {
					throw new IllegalArgumentException("cannot sort by relevance within group: needsScores=false");
				}
				collector = TopScoreDocCollector.create(maxDocsPerGroup);
			}else {
				collector = TopFieldCollector.create(withinGroupSort, maxDocsPerGroup, fillSortFields, needsScores, needsScores, true);
			}
			LeafCollector leafCollector = collector.getLeafCollector(og.readerContext);
			for (int docIDX = 0; docIDX < (og.count); docIDX++) {
				final int doc = og.docs[docIDX];
				if (needsScores) {
				}
				leafCollector.collect(doc);
			}
			totalGroupedHitCount += og.count;
			final Object[] groupSortValues;
			if (fillSortFields) {
				groupSortValues = new Comparable<?>[comparators.length];
				for (int sortFieldIDX = 0; sortFieldIDX < (comparators.length); sortFieldIDX++) {
					groupSortValues[sortFieldIDX] = comparators[sortFieldIDX].value(og.comparatorSlot);
				}
			}else {
				groupSortValues = null;
			}
			final TopDocs topDocs = collector.topDocs(withinGroupOffset, maxDocsPerGroup);
			groups[downTo] = new GroupDocs<>(Float.NaN, topDocs.getMaxScore(), og.count, topDocs.scoreDocs, null, groupSortValues);
			maxScore = Math.max(maxScore, topDocs.getMaxScore());
		}
		return new TopGroups<>(new TopGroups<>(groupSort.getSort(), withinGroupSort.getSort(), totalHitCount, totalGroupedHitCount, groups, maxScore), totalGroupCount);
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		this.scorer = scorer;
		for (LeafFieldComparator comparator : leafComparators) {
			comparator.setScorer(scorer);
		}
	}

	@Override
	public void collect(int doc) throws IOException {
		if (doc > (groupEndDocID)) {
			if ((subDocUpto) != 0) {
				processGroup();
			}
			groupEndDocID = lastDocPerGroupBits.advance(doc);
			subDocUpto = 0;
			groupCompetes = !(queueFull);
		}
		(totalHitCount)++;
		if ((subDocUpto) == (pendingSubDocs.length)) {
			pendingSubDocs = ArrayUtil.grow(pendingSubDocs);
		}
		pendingSubDocs[subDocUpto] = doc;
		if (needsScores) {
			if ((subDocUpto) == (pendingSubScores.length)) {
				pendingSubScores = ArrayUtil.grow(pendingSubScores);
			}
			pendingSubScores[subDocUpto] = scorer.score();
		}
		(subDocUpto)++;
		if (groupCompetes) {
			if ((subDocUpto) == 1) {
				assert !(queueFull);
				for (LeafFieldComparator fc : leafComparators) {
					fc.copy(bottomSlot, doc);
					fc.setBottom(bottomSlot);
				}
				topGroupDoc = doc;
			}else {
				for (int compIDX = 0; ; compIDX++) {
					final int c = (reversed[compIDX]) * (leafComparators[compIDX].compareBottom(doc));
					if (c < 0) {
						return;
					}else
						if (c > 0) {
							break;
						}else
							if (compIDX == (compIDXEnd)) {
								return;
							}


				}
				for (LeafFieldComparator fc : leafComparators) {
					fc.copy(bottomSlot, doc);
					fc.setBottom(bottomSlot);
				}
				topGroupDoc = doc;
			}
		}else {
			for (int compIDX = 0; ; compIDX++) {
				final int c = (reversed[compIDX]) * (leafComparators[compIDX].compareBottom(doc));
				if (c < 0) {
					return;
				}else
					if (c > 0) {
						break;
					}else
						if (compIDX == (compIDXEnd)) {
							return;
						}


			}
			groupCompetes = true;
			for (LeafFieldComparator fc : leafComparators) {
				fc.copy(bottomSlot, doc);
				fc.setBottom(bottomSlot);
			}
			topGroupDoc = doc;
		}
	}

	@Override
	protected void doSetNextReader(LeafReaderContext readerContext) throws IOException {
		if ((subDocUpto) != 0) {
			processGroup();
		}
		subDocUpto = 0;
		docBase = readerContext.docBase;
		Scorer s = lastDocPerGroup.scorer(readerContext);
		if (s == null) {
			lastDocPerGroupBits = null;
		}else {
			lastDocPerGroupBits = s.iterator();
		}
		groupEndDocID = -1;
		currentReaderContext = readerContext;
		for (int i = 0; i < (comparators.length); i++) {
			leafComparators[i] = comparators[i].getLeafComparator(readerContext);
		}
	}

	@Override
	public boolean needsScores() {
		return needsScores;
	}
}

