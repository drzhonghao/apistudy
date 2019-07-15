

import java.io.IOException;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DiversifiedTopDocsCollector.ScoreDocKey;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.util.PriorityQueue;


public abstract class DiversifiedTopDocsCollector extends TopDocsCollector<org.apache.lucene.search.DiversifiedTopDocsCollector.ScoreDocKey> {
	DiversifiedTopDocsCollector.ScoreDocKey spare;

	private DiversifiedTopDocsCollector.ScoreDocKeyQueue globalQueue;

	private int numHits;

	private Map<Long, DiversifiedTopDocsCollector.ScoreDocKeyQueue> perKeyQueues;

	protected int maxNumPerKey;

	private Stack<DiversifiedTopDocsCollector.ScoreDocKeyQueue> sparePerKeyQueues = new Stack<>();

	protected abstract NumericDocValues getKeys(LeafReaderContext context);

	@Override
	public boolean needsScores() {
		return true;
	}

	@Override
	protected TopDocs newTopDocs(ScoreDoc[] results, int start) {
		if (results == null) {
			return TopDocsCollector.EMPTY_TOPDOCS;
		}
		float maxScore = Float.NaN;
		if (start == 0) {
			maxScore = results[0].score;
		}else {
			for (int i = globalQueue.size(); i > 1; i--) {
				globalQueue.pop();
			}
			maxScore = globalQueue.pop().score;
		}
		return new TopDocs(totalHits, results, maxScore);
	}

	protected DiversifiedTopDocsCollector.ScoreDocKey insert(DiversifiedTopDocsCollector.ScoreDocKey addition, int docBase, NumericDocValues keys) throws IOException {
		if (((globalQueue.size()) >= (numHits)) && (globalQueue.lessThan(addition, globalQueue.top()))) {
			return addition;
		}
		int leafDocID = (addition.doc) - docBase;
		long value;
		if (keys.advanceExact(leafDocID)) {
			value = keys.longValue();
		}else {
			value = 0;
		}
		addition.key = value;
		DiversifiedTopDocsCollector.ScoreDocKeyQueue thisKeyQ = perKeyQueues.get(addition.key);
		if (thisKeyQ == null) {
			if ((sparePerKeyQueues.size()) == 0) {
				thisKeyQ = new DiversifiedTopDocsCollector.ScoreDocKeyQueue(maxNumPerKey);
			}else {
				thisKeyQ = sparePerKeyQueues.pop();
			}
			perKeyQueues.put(addition.key, thisKeyQ);
		}
		DiversifiedTopDocsCollector.ScoreDocKey perKeyOverflow = thisKeyQ.insertWithOverflow(addition);
		if (perKeyOverflow == addition) {
			return addition;
		}
		if (perKeyOverflow == null) {
			DiversifiedTopDocsCollector.ScoreDocKey globalOverflow = globalQueue.insertWithOverflow(addition);
			perKeyGroupRemove(globalOverflow);
			return globalOverflow;
		}
		globalQueue.remove(perKeyOverflow);
		globalQueue.add(addition);
		return perKeyOverflow;
	}

	private void perKeyGroupRemove(DiversifiedTopDocsCollector.ScoreDocKey globalOverflow) {
		if (globalOverflow == null) {
			return;
		}
		DiversifiedTopDocsCollector.ScoreDocKeyQueue q = perKeyQueues.get(globalOverflow.key);
		DiversifiedTopDocsCollector.ScoreDocKey perKeyLowest = q.pop();
		assert globalOverflow == perKeyLowest;
		if ((q.size()) == 0) {
			perKeyQueues.remove(globalOverflow.key);
			sparePerKeyQueues.push(q);
		}
	}

	@Override
	public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
		final int base = context.docBase;
		final NumericDocValues keySource = getKeys(context);
		return new LeafCollector() {
			Scorer scorer;

			@Override
			public void setScorer(Scorer scorer) throws IOException {
				this.scorer = scorer;
			}

			@Override
			public void collect(int doc) throws IOException {
				float score = scorer.score();
				assert !(Float.isNaN(score));
				(totalHits)++;
				doc += base;
				if ((spare) == null) {
					spare = new DiversifiedTopDocsCollector.ScoreDocKey(doc, score);
				}else {
					spare.doc = doc;
					spare.score = score;
				}
				spare = insert(spare, base, keySource);
			}
		};
	}

	static class ScoreDocKeyQueue extends PriorityQueue<DiversifiedTopDocsCollector.ScoreDocKey> {
		ScoreDocKeyQueue(int size) {
			super(size);
		}

		@Override
		protected final boolean lessThan(DiversifiedTopDocsCollector.ScoreDocKey hitA, DiversifiedTopDocsCollector.ScoreDocKey hitB) {
			if ((hitA.score) == (hitB.score))
				return (hitA.doc) > (hitB.doc);
			else
				return (hitA.score) < (hitB.score);

		}
	}

	public static class ScoreDocKey extends ScoreDoc {
		Long key;

		protected ScoreDocKey(int doc, float score) {
			super(doc, score);
		}

		public Long getKey() {
			return key;
		}

		@Override
		public String toString() {
			return (((("key:" + (key)) + " doc=") + (doc)) + " s=") + (score);
		}
	}
}

