

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanScorer;
import org.apache.lucene.search.spans.Spans;


public abstract class SpanWeight extends Weight {
	public enum Postings {

		POSITIONS() {
			@Override
			public int getRequiredPostings() {
				return PostingsEnum.POSITIONS;
			}
		},
		PAYLOADS() {
			@Override
			public int getRequiredPostings() {
				return PostingsEnum.PAYLOADS;
			}
		},
		OFFSETS() {
			@Override
			public int getRequiredPostings() {
				return (PostingsEnum.PAYLOADS) | (PostingsEnum.OFFSETS);
			}
		};
		public abstract int getRequiredPostings();

		public SpanWeight.Postings atLeast(SpanWeight.Postings postings) {
			if ((postings.compareTo(this)) > 0)
				return postings;

			return this;
		}
	}

	protected final Similarity similarity;

	protected final Similarity.SimWeight simWeight;

	protected final String field;

	public SpanWeight(SpanQuery query, IndexSearcher searcher, Map<Term, TermContext> termContexts, float boost) throws IOException {
		super(query);
		this.field = query.getField();
		this.similarity = searcher.getSimilarity((termContexts != null));
		this.simWeight = buildSimWeight(query, searcher, termContexts, boost);
	}

	private Similarity.SimWeight buildSimWeight(SpanQuery query, IndexSearcher searcher, Map<Term, TermContext> termContexts, float boost) throws IOException {
		if (((termContexts == null) || ((termContexts.size()) == 0)) || ((query.getField()) == null))
			return null;

		TermStatistics[] termStats = new TermStatistics[termContexts.size()];
		int i = 0;
		for (Term term : termContexts.keySet()) {
			termStats[i] = searcher.termStatistics(term, termContexts.get(term));
			i++;
		}
		CollectionStatistics collectionStats = searcher.collectionStatistics(query.getField());
		return similarity.computeWeight(boost, collectionStats, termStats);
	}

	public abstract void extractTermContexts(Map<Term, TermContext> contexts);

	public abstract Spans getSpans(LeafReaderContext ctx, SpanWeight.Postings requiredPostings) throws IOException;

	@Override
	public SpanScorer scorer(LeafReaderContext context) throws IOException {
		final Spans spans = getSpans(context, SpanWeight.Postings.POSITIONS);
		if (spans == null) {
			return null;
		}
		final Similarity.SimScorer docScorer = getSimScorer(context);
		return null;
	}

	public Similarity.SimScorer getSimScorer(LeafReaderContext context) throws IOException {
		return (simWeight) == null ? null : similarity.simScorer(simWeight, context);
	}

	@Override
	public Explanation explain(LeafReaderContext context, int doc) throws IOException {
		SpanScorer scorer = scorer(context);
		if (scorer != null) {
			int newDoc = scorer.iterator().advance(doc);
			if (newDoc == doc) {
				Similarity.SimScorer docScorer = similarity.simScorer(simWeight, context);
			}
		}
		return Explanation.noMatch("no matching term");
	}
}

