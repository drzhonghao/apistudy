

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FutureObjects;
import org.apache.lucene.util.PriorityQueue;


final class BooleanScorer extends BulkScorer {
	static final int SHIFT = 11;

	static final int SIZE = 1 << (BooleanScorer.SHIFT);

	static final int MASK = (BooleanScorer.SIZE) - 1;

	static final int SET_SIZE = 1 << ((BooleanScorer.SHIFT) - 6);

	static final int SET_MASK = (BooleanScorer.SET_SIZE) - 1;

	static class Bucket {
		double score;

		int freq;
	}

	private class BulkScorerAndDoc {
		final BulkScorer scorer;

		final long cost;

		int next;

		BulkScorerAndDoc(BulkScorer scorer) {
			this.scorer = scorer;
			this.cost = scorer.cost();
			this.next = -1;
		}

		void advance(int min) throws IOException {
			score(orCollector, null, min, min);
		}

		void score(LeafCollector collector, Bits acceptDocs, int min, int max) throws IOException {
			next = scorer.score(collector, acceptDocs, min, max);
		}
	}

	private static long cost(Collection<BulkScorer> scorers, int minShouldMatch) {
		final PriorityQueue<BulkScorer> pq = new PriorityQueue<BulkScorer>((((scorers.size()) - minShouldMatch) + 1)) {
			@Override
			protected boolean lessThan(BulkScorer a, BulkScorer b) {
				return (a.cost()) > (b.cost());
			}
		};
		for (BulkScorer scorer : scorers) {
			pq.insertWithOverflow(scorer);
		}
		long cost = 0;
		for (BulkScorer scorer = pq.pop(); scorer != null; scorer = pq.pop()) {
			cost += scorer.cost();
		}
		return cost;
	}

	static final class HeadPriorityQueue extends PriorityQueue<BooleanScorer.BulkScorerAndDoc> {
		public HeadPriorityQueue(int maxSize) {
			super(maxSize);
		}

		@Override
		protected boolean lessThan(BooleanScorer.BulkScorerAndDoc a, BooleanScorer.BulkScorerAndDoc b) {
			return (a.next) < (b.next);
		}
	}

	static final class TailPriorityQueue extends PriorityQueue<BooleanScorer.BulkScorerAndDoc> {
		public TailPriorityQueue(int maxSize) {
			super(maxSize);
		}

		@Override
		protected boolean lessThan(BooleanScorer.BulkScorerAndDoc a, BooleanScorer.BulkScorerAndDoc b) {
			return (a.cost) < (b.cost);
		}

		public BooleanScorer.BulkScorerAndDoc get(int i) {
			FutureObjects.checkIndex(i, size());
			return ((BooleanScorer.BulkScorerAndDoc) (getHeapArray()[(1 + i)]));
		}
	}

	final BooleanScorer.Bucket[] buckets = new BooleanScorer.Bucket[BooleanScorer.SIZE];

	final long[] matching = new long[BooleanScorer.SET_SIZE];

	final BooleanScorer.BulkScorerAndDoc[] leads = null;

	final BooleanScorer.HeadPriorityQueue head = null;

	final BooleanScorer.TailPriorityQueue tail = null;

	final int minShouldMatch = 0;

	final long cost = 0L;

	final class OrCollector implements LeafCollector {
		Scorer scorer;

		@Override
		public void setScorer(Scorer scorer) {
			this.scorer = scorer;
		}

		@Override
		public void collect(int doc) throws IOException {
			final int i = doc & (BooleanScorer.MASK);
			final int idx = i >>> 6;
			matching[idx] |= 1L << i;
			final BooleanScorer.Bucket bucket = buckets[i];
			(bucket.freq)++;
			bucket.score += scorer.score();
		}
	}

	final BooleanScorer.OrCollector orCollector = new BooleanScorer.OrCollector();

	@Override
	public long cost() {
		return cost;
	}

	private void scoreDocument(LeafCollector collector, int base, int i) throws IOException {
		final BooleanScorer.Bucket bucket = buckets[i];
		if ((bucket.freq) >= (minShouldMatch)) {
			final int doc = base | i;
			collector.collect(doc);
		}
		bucket.freq = 0;
		bucket.score = 0;
	}

	private void scoreMatches(LeafCollector collector, int base) throws IOException {
		long[] matching = this.matching;
		for (int idx = 0; idx < (matching.length); idx++) {
			long bits = matching[idx];
			while (bits != 0L) {
				int ntz = Long.numberOfTrailingZeros(bits);
				int doc = (idx << 6) | ntz;
				scoreDocument(collector, base, doc);
				bits ^= 1L << ntz;
			} 
		}
	}

	private void scoreWindowIntoBitSetAndReplay(LeafCollector collector, Bits acceptDocs, int base, int min, int max, BooleanScorer.BulkScorerAndDoc[] scorers, int numScorers) throws IOException {
		for (int i = 0; i < numScorers; ++i) {
			final BooleanScorer.BulkScorerAndDoc scorer = scorers[i];
			assert (scorer.next) < max;
			scorer.score(orCollector, acceptDocs, min, max);
		}
		scoreMatches(collector, base);
		Arrays.fill(matching, 0L);
	}

	private BooleanScorer.BulkScorerAndDoc advance(int min) throws IOException {
		assert (tail.size()) == ((minShouldMatch) - 1);
		final BooleanScorer.HeadPriorityQueue head = this.head;
		final BooleanScorer.TailPriorityQueue tail = this.tail;
		BooleanScorer.BulkScorerAndDoc headTop = head.top();
		BooleanScorer.BulkScorerAndDoc tailTop = tail.top();
		while ((headTop.next) < min) {
			if ((tailTop == null) || ((headTop.cost) <= (tailTop.cost))) {
				headTop.advance(min);
				headTop = head.updateTop();
			}else {
				final BooleanScorer.BulkScorerAndDoc previousHeadTop = headTop;
				tailTop.advance(min);
				headTop = head.updateTop(tailTop);
				tailTop = tail.updateTop(previousHeadTop);
			}
		} 
		return headTop;
	}

	private void scoreWindowMultipleScorers(LeafCollector collector, Bits acceptDocs, int windowBase, int windowMin, int windowMax, int maxFreq) throws IOException {
		while ((maxFreq < (minShouldMatch)) && ((maxFreq + (tail.size())) >= (minShouldMatch))) {
			final BooleanScorer.BulkScorerAndDoc candidate = tail.pop();
			candidate.advance(windowMin);
			if ((candidate.next) < windowMax) {
				leads[(maxFreq++)] = candidate;
			}else {
				head.add(candidate);
			}
		} 
		if (maxFreq >= (minShouldMatch)) {
			for (int i = 0; i < (tail.size()); ++i) {
				leads[(maxFreq++)] = tail.get(i);
			}
			tail.clear();
			scoreWindowIntoBitSetAndReplay(collector, acceptDocs, windowBase, windowMin, windowMax, leads, maxFreq);
		}
		for (int i = 0; i < maxFreq; ++i) {
			final BooleanScorer.BulkScorerAndDoc evicted = head.insertWithOverflow(leads[i]);
			if (evicted != null) {
				tail.add(evicted);
			}
		}
	}

	private void scoreWindowSingleScorer(BooleanScorer.BulkScorerAndDoc bulkScorer, LeafCollector collector, Bits acceptDocs, int windowMin, int windowMax, int max) throws IOException {
		assert (tail.size()) == 0;
		final int nextWindowBase = (head.top().next) & (~(BooleanScorer.MASK));
		final int end = Math.max(windowMax, Math.min(max, nextWindowBase));
		bulkScorer.score(collector, acceptDocs, windowMin, end);
	}

	private BooleanScorer.BulkScorerAndDoc scoreWindow(BooleanScorer.BulkScorerAndDoc top, LeafCollector collector, Bits acceptDocs, int min, int max) throws IOException {
		final int windowBase = (top.next) & (~(BooleanScorer.MASK));
		final int windowMin = Math.max(min, windowBase);
		final int windowMax = Math.min(max, (windowBase + (BooleanScorer.SIZE)));
		leads[0] = head.pop();
		int maxFreq = 1;
		while (((head.size()) > 0) && ((head.top().next) < windowMax)) {
			leads[(maxFreq++)] = head.pop();
		} 
		if (((minShouldMatch) == 1) && (maxFreq == 1)) {
			final BooleanScorer.BulkScorerAndDoc bulkScorer = leads[0];
			scoreWindowSingleScorer(bulkScorer, collector, acceptDocs, windowMin, windowMax, max);
			return head.add(bulkScorer);
		}else {
			scoreWindowMultipleScorers(collector, acceptDocs, windowBase, windowMin, windowMax, maxFreq);
			return head.top();
		}
	}

	@Override
	public int score(LeafCollector collector, Bits acceptDocs, int min, int max) throws IOException {
		BooleanScorer.BulkScorerAndDoc top = advance(min);
		while ((top.next) < max) {
			top = scoreWindow(top, collector, acceptDocs, min, max);
		} 
		return top.next;
	}
}

