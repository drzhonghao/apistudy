

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.DisiWrapper;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;

import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;


public final class SynonymQuery extends Query {
	private final Term[] terms;

	public SynonymQuery(Term... terms) {
		this.terms = Objects.requireNonNull(terms).clone();
		String field = null;
		for (Term term : terms) {
			if (field == null) {
				field = term.field();
			}else
				if (!(term.field().equals(field))) {
					throw new IllegalArgumentException("Synonyms must be across the same field");
				}

		}
		if ((terms.length) > (BooleanQuery.getMaxClauseCount())) {
			throw new BooleanQuery.TooManyClauses();
		}
		Arrays.sort(this.terms);
	}

	public List<Term> getTerms() {
		return Collections.unmodifiableList(Arrays.asList(terms));
	}

	@Override
	public String toString(String field) {
		StringBuilder builder = new StringBuilder("Synonym(");
		for (int i = 0; i < (terms.length); i++) {
			if (i != 0) {
				builder.append(" ");
			}
			Query termQuery = new TermQuery(terms[i]);
			builder.append(termQuery.toString(field));
		}
		builder.append(")");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return (31 * (classHash())) + (Arrays.hashCode(terms));
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (Arrays.equals(terms, ((SynonymQuery) (other)).terms));
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		if ((terms.length) == 0) {
			return new BooleanQuery.Builder().build();
		}
		if ((terms.length) == 1) {
			return new TermQuery(terms[0]);
		}
		return this;
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		if (needsScores) {
			return new SynonymQuery.SynonymWeight(this, searcher, boost);
		}else {
			BooleanQuery.Builder bq = new BooleanQuery.Builder();
			for (Term term : terms) {
				bq.add(new TermQuery(term), SHOULD);
			}
			return searcher.rewrite(bq.build()).createWeight(searcher, needsScores, boost);
		}
	}

	class SynonymWeight extends Weight {
		private final TermContext[] termContexts;

		private final Similarity similarity;

		private final Similarity.SimWeight simWeight;

		SynonymWeight(Query query, IndexSearcher searcher, float boost) throws IOException {
			super(query);
			CollectionStatistics collectionStats = searcher.collectionStatistics(terms[0].field());
			long docFreq = 0;
			long totalTermFreq = 0;
			termContexts = new TermContext[terms.length];
			for (int i = 0; i < (termContexts.length); i++) {
				termContexts[i] = TermContext.build(searcher.getTopReaderContext(), terms[i]);
				TermStatistics termStats = searcher.termStatistics(terms[i], termContexts[i]);
				docFreq = Math.max(termStats.docFreq(), docFreq);
				if ((termStats.totalTermFreq()) == (-1)) {
					totalTermFreq = -1;
				}else
					if (totalTermFreq != (-1)) {
						totalTermFreq += termStats.totalTermFreq();
					}

			}
			TermStatistics pseudoStats = new TermStatistics(null, docFreq, totalTermFreq);
			this.similarity = searcher.getSimilarity(true);
			this.simWeight = similarity.computeWeight(boost, collectionStats, pseudoStats);
		}

		@Override
		public void extractTerms(Set<Term> terms) {
			for (Term term : SynonymQuery.this.terms) {
				terms.add(term);
			}
		}

		@Override
		public Matches matches(LeafReaderContext context, int doc) throws IOException {
			String field = terms[0].field();
			Terms terms = context.reader().terms(field);
			if ((terms == null) || ((terms.hasPositions()) == false)) {
				return super.matches(context, doc);
			}
			return null;
		}

		@Override
		public Explanation explain(LeafReaderContext context, int doc) throws IOException {
			Scorer scorer = scorer(context);
			if (scorer != null) {
				int newDoc = scorer.iterator().advance(doc);
				if (newDoc == doc) {
					final float freq;
					Similarity.SimScorer docScorer = similarity.simScorer(simWeight, context);
					freq = 0f;
					Explanation freqExplanation = Explanation.match(freq, ("termFreq=" + freq));
					Explanation scoreExplanation = docScorer.explain(doc, freqExplanation);
					return Explanation.match(scoreExplanation.getValue(), (((((("weight(" + (getQuery())) + " in ") + doc) + ") [") + (similarity.getClass().getSimpleName())) + "], result of:"), scoreExplanation);
				}
			}
			return Explanation.noMatch("no matching term");
		}

		@Override
		public Scorer scorer(LeafReaderContext context) throws IOException {
			Similarity.SimScorer simScorer = similarity.simScorer(simWeight, context);
			List<Scorer> subScorers = new ArrayList<>();
			for (int i = 0; i < (terms.length); i++) {
				TermState state = termContexts[i].get(context.ord);
				if (state != null) {
					TermsEnum termsEnum = context.reader().terms(terms[i].field()).iterator();
					termsEnum.seekExact(terms[i].bytes(), state);
					PostingsEnum postings = termsEnum.postings(null, PostingsEnum.FREQS);
				}
			}
			if (subScorers.isEmpty()) {
				return null;
			}else
				if ((subScorers.size()) == 1) {
					return subScorers.get(0);
				}else {
				}

			return null;
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return true;
		}
	}

	static class SynonymScorer {
		private final Similarity.SimScorer similarity;

		SynonymScorer(Similarity.SimScorer similarity, Weight weight, List<Scorer> subScorers) {
			this.similarity = similarity;
		}

		protected float score(DisiWrapper topList) throws IOException {
			return similarity.score(topList.doc, tf(topList));
		}

		final int tf(DisiWrapper topList) throws IOException {
			int tf = 0;
			for (DisiWrapper w = topList; w != null; w = w.next) {
			}
			return tf;
		}
	}
}

