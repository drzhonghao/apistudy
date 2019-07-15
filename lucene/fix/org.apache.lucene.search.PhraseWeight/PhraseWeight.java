

import java.io.IOException;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.MatchesIterator;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;


abstract class PhraseWeight extends Weight {
	final boolean needsScores;

	final Similarity.SimWeight stats;

	final Similarity similarity;

	final String field;

	protected PhraseWeight(Query query, String field, IndexSearcher searcher, boolean needsScores) throws IOException {
		super(query);
		this.needsScores = needsScores;
		this.field = field;
		this.similarity = searcher.getSimilarity(needsScores);
		this.stats = getStats(searcher);
	}

	protected abstract Similarity.SimWeight getStats(IndexSearcher searcher) throws IOException;

	@Override
	public Scorer scorer(LeafReaderContext context) throws IOException {
		Similarity.SimScorer simScorer = similarity.simScorer(stats, context);
		return null;
	}

	@Override
	public Explanation explain(LeafReaderContext context, int doc) throws IOException {
		Similarity.SimScorer simScorer = similarity.simScorer(stats, context);
		return null;
	}

	@Override
	public Matches matches(LeafReaderContext context, int doc) throws IOException {
		return Matches.forField(field, () -> {
			return new MatchesIterator() {
				boolean started = false;

				@Override
				public boolean next() throws IOException {
					if ((started) == false) {
						return started = true;
					}
					return false;
				}

				@Override
				public int startPosition() {
					return 0;
				}

				@Override
				public int endPosition() {
					return 0;
				}

				@Override
				public int startOffset() throws IOException {
					return 0;
				}

				@Override
				public int endOffset() throws IOException {
					return 0;
				}
			};
		});
	}

	@Override
	public boolean isCacheable(LeafReaderContext ctx) {
		return true;
	}
}

