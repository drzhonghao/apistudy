

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.ToLongFunction;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.lucene.search.DisiPriorityQueue;
import org.apache.lucene.search.DisiWrapper;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Scorer.ChildScorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.PriorityQueue;


final class MinShouldMatchSumScorer extends Scorer {
	static long cost(LongStream costs, int numScorers, int minShouldMatch) {
		final PriorityQueue<Long> pq = new PriorityQueue<Long>(((numScorers - minShouldMatch) + 1)) {
			@Override
			protected boolean lessThan(Long a, Long b) {
				return a > b;
			}
		};
		costs.forEach(pq::insertWithOverflow);
		return StreamSupport.stream(pq.spliterator(), false).mapToLong(Number::longValue).sum();
	}

	final int minShouldMatch;

	DisiWrapper lead;

	int doc;

	int freq;

	final DisiPriorityQueue head;

	final DisiWrapper[] tail;

	int tailSize;

	final long cost;

	MinShouldMatchSumScorer(Weight weight, Collection<Scorer> scorers, int minShouldMatch) {
		super(weight);
		if (minShouldMatch > (scorers.size())) {
			throw new IllegalArgumentException("minShouldMatch should be <= the number of scorers");
		}
		if (minShouldMatch < 1) {
			throw new IllegalArgumentException("minShouldMatch should be >= 1");
		}
		this.minShouldMatch = minShouldMatch;
		this.doc = -1;
		head = new DisiPriorityQueue((((scorers.size()) - minShouldMatch) + 1));
		tail = new DisiWrapper[minShouldMatch - 1];
		for (Scorer scorer : scorers) {
			addLead(new DisiWrapper(scorer));
		}
		this.cost = MinShouldMatchSumScorer.cost(scorers.stream().map(Scorer::iterator).mapToLong(DocIdSetIterator::cost), scorers.size(), minShouldMatch);
	}

	@Override
	public final Collection<Scorer.ChildScorer> getChildren() throws IOException {
		List<Scorer.ChildScorer> matchingChildren = new ArrayList<>();
		updateFreq();
		for (DisiWrapper s = lead; s != null; s = s.next) {
			matchingChildren.add(new Scorer.ChildScorer(s.scorer, "SHOULD"));
		}
		return matchingChildren;
	}

	@Override
	public DocIdSetIterator iterator() {
		return TwoPhaseIterator.asDocIdSetIterator(twoPhaseIterator());
	}

	@Override
	public TwoPhaseIterator twoPhaseIterator() {
		DocIdSetIterator approximation = new DocIdSetIterator() {
			@Override
			public int docID() {
				assert (doc) == (lead.doc);
				return doc;
			}

			@Override
			public int nextDoc() throws IOException {
				for (DisiWrapper s = lead; s != null; s = s.next) {
					final DisiWrapper evicted = insertTailWithOverFlow(s);
					if (evicted != null) {
						if ((evicted.doc) == (doc)) {
							evicted.doc = evicted.iterator.nextDoc();
						}else {
							evicted.doc = evicted.iterator.advance(((doc) + 1));
						}
						head.add(evicted);
					}
				}
				setDocAndFreq();
				return doNext();
			}

			@Override
			public int advance(int target) throws IOException {
				for (DisiWrapper s = lead; s != null; s = s.next) {
					final DisiWrapper evicted = insertTailWithOverFlow(s);
					if (evicted != null) {
						evicted.doc = evicted.iterator.advance(target);
						head.add(evicted);
					}
				}
				DisiWrapper headTop = head.top();
				while ((headTop.doc) < target) {
					final DisiWrapper evicted = insertTailWithOverFlow(headTop);
					evicted.doc = evicted.iterator.advance(target);
				} 
				setDocAndFreq();
				return doNextCandidate();
			}

			@Override
			public long cost() {
				return cost;
			}
		};
		return new TwoPhaseIterator(approximation) {
			@Override
			public boolean matches() throws IOException {
				while ((freq) < (minShouldMatch)) {
					assert (freq) > 0;
					if (((freq) + (tailSize)) >= (minShouldMatch)) {
						advanceTail();
					}else {
						return false;
					}
				} 
				return true;
			}

			@Override
			public float matchCost() {
				return tail.length;
			}
		};
	}

	private void addLead(DisiWrapper lead) {
		lead.next = this.lead;
		this.lead = lead;
		freq += 1;
	}

	private void pushBackLeads() throws IOException {
		for (DisiWrapper s = lead; s != null; s = s.next) {
			addTail(s);
		}
	}

	private void advanceTail(DisiWrapper top) throws IOException {
		top.doc = top.iterator.advance(doc);
		if ((top.doc) == (doc)) {
			addLead(top);
		}else {
			head.add(top);
		}
	}

	private void advanceTail() throws IOException {
		final DisiWrapper top = popTail();
		advanceTail(top);
	}

	private void setDocAndFreq() {
		assert (head.size()) > 0;
		lead = head.pop();
		lead.next = null;
		freq = 1;
		doc = lead.doc;
		while (((head.size()) > 0) && ((head.top().doc) == (doc))) {
			addLead(head.pop());
		} 
	}

	private int doNext() throws IOException {
		while ((freq) < (minShouldMatch)) {
			assert (freq) > 0;
			if (((freq) + (tailSize)) >= (minShouldMatch)) {
				advanceTail();
			}else {
				pushBackLeads();
				setDocAndFreq();
			}
		} 
		return doc;
	}

	private int doNextCandidate() throws IOException {
		while (((freq) + (tailSize)) < (minShouldMatch)) {
			pushBackLeads();
			setDocAndFreq();
		} 
		return doc;
	}

	private void updateFreq() throws IOException {
		assert (freq) >= (minShouldMatch);
		for (int i = (tailSize) - 1; i >= 0; --i) {
			advanceTail(tail[i]);
		}
		tailSize = 0;
	}

	@Override
	public float score() throws IOException {
		updateFreq();
		double score = 0;
		for (DisiWrapper s = lead; s != null; s = s.next) {
			score += s.scorer.score();
		}
		return ((float) (score));
	}

	@Override
	public int docID() {
		assert (doc) == (lead.doc);
		return doc;
	}

	private DisiWrapper insertTailWithOverFlow(DisiWrapper s) {
		if ((tailSize) < (tail.length)) {
			addTail(s);
			return null;
		}else
			if ((tail.length) >= 1) {
				final DisiWrapper top = tail[0];
				if ((top.cost) < (s.cost)) {
					tail[0] = s;
					MinShouldMatchSumScorer.downHeapCost(tail, tailSize);
					return top;
				}
			}

		return s;
	}

	private void addTail(DisiWrapper s) {
		tail[tailSize] = s;
		MinShouldMatchSumScorer.upHeapCost(tail, tailSize);
		tailSize += 1;
	}

	private DisiWrapper popTail() {
		assert (tailSize) > 0;
		final DisiWrapper result = tail[0];
		tail[0] = tail[(--(tailSize))];
		MinShouldMatchSumScorer.downHeapCost(tail, tailSize);
		return result;
	}

	private static void upHeapCost(DisiWrapper[] heap, int i) {
		final DisiWrapper node = heap[i];
		final long nodeCost = node.cost;
		heap[i] = node;
	}

	private static void downHeapCost(DisiWrapper[] heap, int size) {
		int i = 0;
		final DisiWrapper node = heap[0];
	}
}

