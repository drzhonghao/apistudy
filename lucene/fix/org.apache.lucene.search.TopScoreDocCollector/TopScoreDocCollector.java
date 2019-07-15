

import java.io.IOException;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.util.PriorityQueue;


public abstract class TopScoreDocCollector extends TopDocsCollector<ScoreDoc> {
	static abstract class ScorerLeafCollector implements LeafCollector {
		Scorer scorer;

		@Override
		public void setScorer(Scorer scorer) throws IOException {
			this.scorer = scorer;
		}
	}

	private static class SimpleTopScoreDocCollector extends TopScoreDocCollector {
		SimpleTopScoreDocCollector(int numHits) {
		}

		@Override
		public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
			final int docBase = context.docBase;
			return new TopScoreDocCollector.ScorerLeafCollector() {
				@Override
				public void collect(int doc) throws IOException {
					float score = scorer.score();
					assert score != (Float.NEGATIVE_INFINITY);
					assert !(Float.isNaN(score));
					(totalHits)++;
					if (score <= (pqTop.score)) {
						return;
					}
					pqTop.doc = doc + docBase;
					pqTop.score = score;
					pqTop = pq.updateTop();
				}
			};
		}
	}

	private static class PagingTopScoreDocCollector extends TopScoreDocCollector {
		private final ScoreDoc after;

		private int collectedHits;

		PagingTopScoreDocCollector(int numHits, ScoreDoc after) {
			this.after = after;
			this.collectedHits = 0;
		}

		@Override
		protected int topDocsSize() {
			return (collectedHits) < (pq.size()) ? collectedHits : pq.size();
		}

		@Override
		protected TopDocs newTopDocs(ScoreDoc[] results, int start) {
			return null;
		}

		@Override
		public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
			final int docBase = context.docBase;
			final int afterDoc = (after.doc) - (context.docBase);
			return new TopScoreDocCollector.ScorerLeafCollector() {
				@Override
				public void collect(int doc) throws IOException {
					float score = scorer.score();
					assert score != (Float.NEGATIVE_INFINITY);
					assert !(Float.isNaN(score));
					(totalHits)++;
					if ((score > (after.score)) || ((score == (after.score)) && (doc <= afterDoc))) {
						return;
					}
					if (score <= (pqTop.score)) {
						return;
					}
					(collectedHits)++;
					pqTop.doc = doc + docBase;
					pqTop.score = score;
					pqTop = pq.updateTop();
				}
			};
		}
	}

	public static TopScoreDocCollector create(int numHits) {
		return TopScoreDocCollector.create(numHits, null);
	}

	public static TopScoreDocCollector create(int numHits, ScoreDoc after) {
		if (numHits <= 0) {
			throw new IllegalArgumentException("numHits must be > 0; please use TotalHitCountCollector if you just need the total hit count");
		}
		if (after == null) {
			return new TopScoreDocCollector.SimpleTopScoreDocCollector(numHits);
		}else {
			return new TopScoreDocCollector.PagingTopScoreDocCollector(numHits, after);
		}
	}

	ScoreDoc pqTop;

	@Override
	protected TopDocs newTopDocs(ScoreDoc[] results, int start) {
		if (results == null) {
			return TopDocsCollector.EMPTY_TOPDOCS;
		}
		float maxScore = Float.NaN;
		if (start == 0) {
			maxScore = results[0].score;
		}else {
			for (int i = pq.size(); i > 1; i--) {
				pq.pop();
			}
			maxScore = pq.pop().score;
		}
		return new TopDocs(totalHits, results, maxScore);
	}

	@Override
	public boolean needsScores() {
		return true;
	}
}

