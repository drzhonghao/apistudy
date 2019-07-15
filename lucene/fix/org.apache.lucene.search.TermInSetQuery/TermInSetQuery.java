

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.PrefixCodedTerms;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.DocIdSetBuilder;
import org.apache.lucene.util.RamUsageEstimator;

import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;


public class TermInSetQuery extends Query implements Accountable {
	private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(TermInSetQuery.class);

	static final int BOOLEAN_REWRITE_TERM_COUNT_THRESHOLD = 16;

	private final String field;

	private final PrefixCodedTerms termData;

	private final int termDataHashCode;

	public TermInSetQuery(String field, Collection<BytesRef> terms) {
		BytesRef[] sortedTerms = terms.toArray(new BytesRef[terms.size()]);
		boolean sorted = (terms instanceof SortedSet) && ((((SortedSet<BytesRef>) (terms)).comparator()) == null);
		if (!sorted) {
			ArrayUtil.timSort(sortedTerms);
		}
		PrefixCodedTerms.Builder builder = new PrefixCodedTerms.Builder();
		BytesRefBuilder previous = null;
		for (BytesRef term : sortedTerms) {
			if (previous == null) {
				previous = new BytesRefBuilder();
			}else
				if (previous.get().equals(term)) {
					continue;
				}

			builder.add(field, term);
			previous.copyBytes(term);
		}
		this.field = field;
		termData = builder.finish();
		termDataHashCode = termData.hashCode();
	}

	public TermInSetQuery(String field, BytesRef... terms) {
		this(field, Arrays.asList(terms));
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		final int threshold = Math.min(TermInSetQuery.BOOLEAN_REWRITE_TERM_COUNT_THRESHOLD, BooleanQuery.getMaxClauseCount());
		if ((termData.size()) <= threshold) {
			BooleanQuery.Builder bq = new BooleanQuery.Builder();
			PrefixCodedTerms.TermIterator iterator = termData.iterator();
			for (BytesRef term = iterator.next(); term != null; term = iterator.next()) {
				bq.add(new TermQuery(new Term(iterator.field(), BytesRef.deepCopyOf(term))), SHOULD);
			}
			return new ConstantScoreQuery(bq.build());
		}
		return super.rewrite(reader);
	}

	@Override
	public boolean equals(Object other) {
		return (sameClassAs(other)) && (equalsTo(getClass().cast(other)));
	}

	private boolean equalsTo(TermInSetQuery other) {
		return ((termDataHashCode) == (other.termDataHashCode)) && (termData.equals(other.termData));
	}

	@Override
	public int hashCode() {
		return (31 * (classHash())) + (termDataHashCode);
	}

	public PrefixCodedTerms getTermData() {
		return termData;
	}

	@Override
	public String toString(String defaultField) {
		StringBuilder builder = new StringBuilder();
		builder.append(field);
		builder.append(":(");
		PrefixCodedTerms.TermIterator iterator = termData.iterator();
		boolean first = true;
		for (BytesRef term = iterator.next(); term != null; term = iterator.next()) {
			if (!first) {
				builder.append(' ');
			}
			first = false;
			builder.append(Term.toString(term));
		}
		builder.append(')');
		return builder.toString();
	}

	@Override
	public long ramBytesUsed() {
		return (TermInSetQuery.BASE_RAM_BYTES_USED) + (termData.ramBytesUsed());
	}

	@Override
	public Collection<Accountable> getChildResources() {
		return Collections.emptyList();
	}

	private static class TermAndState {
		final String field;

		final TermsEnum termsEnum;

		final BytesRef term;

		final TermState state;

		final int docFreq;

		final long totalTermFreq;

		TermAndState(String field, TermsEnum termsEnum) throws IOException {
			this.field = field;
			this.termsEnum = termsEnum;
			this.term = BytesRef.deepCopyOf(termsEnum.term());
			this.state = termsEnum.termState();
			this.docFreq = termsEnum.docFreq();
			this.totalTermFreq = termsEnum.totalTermFreq();
		}
	}

	private static class WeightOrDocIdSet {
		final Weight weight;

		final DocIdSet set;

		WeightOrDocIdSet(Weight weight) {
			this.weight = Objects.requireNonNull(weight);
			this.set = null;
		}

		WeightOrDocIdSet(DocIdSet bitset) {
			this.set = bitset;
			this.weight = null;
		}
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
		return new ConstantScoreWeight(this, boost) {
			@Override
			public void extractTerms(Set<Term> terms) {
			}

			@Override
			public Matches matches(LeafReaderContext context, int doc) throws IOException {
				Terms terms = context.reader().terms(field);
				if ((terms == null) || ((terms.hasPositions()) == false)) {
					return super.matches(context, doc);
				}
				return null;
			}

			private TermInSetQuery.WeightOrDocIdSet rewrite(LeafReaderContext context) throws IOException {
				final LeafReader reader = context.reader();
				Terms terms = reader.terms(field);
				if (terms == null) {
					return null;
				}
				TermsEnum termsEnum = terms.iterator();
				PostingsEnum docs = null;
				PrefixCodedTerms.TermIterator iterator = termData.iterator();
				final int threshold = Math.min(TermInSetQuery.BOOLEAN_REWRITE_TERM_COUNT_THRESHOLD, BooleanQuery.getMaxClauseCount());
				assert (termData.size()) > threshold : "Query should have been rewritten";
				List<TermInSetQuery.TermAndState> matchingTerms = new ArrayList<>(threshold);
				DocIdSetBuilder builder = null;
				for (BytesRef term = iterator.next(); term != null; term = iterator.next()) {
					assert field.equals(iterator.field());
					if (termsEnum.seekExact(term)) {
						if (matchingTerms == null) {
							docs = termsEnum.postings(docs, PostingsEnum.NONE);
							builder.add(docs);
						}else
							if ((matchingTerms.size()) < threshold) {
								matchingTerms.add(new TermInSetQuery.TermAndState(field, termsEnum));
							}else {
								assert (matchingTerms.size()) == threshold;
								builder = new DocIdSetBuilder(reader.maxDoc(), terms);
								docs = termsEnum.postings(docs, PostingsEnum.NONE);
								builder.add(docs);
								for (TermInSetQuery.TermAndState t : matchingTerms) {
									t.termsEnum.seekExact(t.term, t.state);
									docs = t.termsEnum.postings(docs, PostingsEnum.NONE);
									builder.add(docs);
								}
								matchingTerms = null;
							}

					}
				}
				if (matchingTerms != null) {
					assert builder == null;
					BooleanQuery.Builder bq = new BooleanQuery.Builder();
					for (TermInSetQuery.TermAndState t : matchingTerms) {
						final TermContext termContext = new TermContext(searcher.getTopReaderContext());
						termContext.register(t.state, context.ord, t.docFreq, t.totalTermFreq);
						bq.add(new TermQuery(new Term(t.field, t.term), termContext), SHOULD);
					}
					Query q = new ConstantScoreQuery(bq.build());
					final Weight weight = searcher.rewrite(q).createWeight(searcher, needsScores, score());
					return new TermInSetQuery.WeightOrDocIdSet(weight);
				}else {
					assert builder != null;
					return new TermInSetQuery.WeightOrDocIdSet(builder.build());
				}
			}

			private Scorer scorer(DocIdSet set) throws IOException {
				if (set == null) {
					return null;
				}
				final DocIdSetIterator disi = set.iterator();
				if (disi == null) {
					return null;
				}
				return new ConstantScoreScorer(this, score(), disi);
			}

			@Override
			public BulkScorer bulkScorer(LeafReaderContext context) throws IOException {
				final TermInSetQuery.WeightOrDocIdSet weightOrBitSet = rewrite(context);
				if (weightOrBitSet == null) {
					return null;
				}else
					if ((weightOrBitSet.weight) != null) {
						return weightOrBitSet.weight.bulkScorer(context);
					}else {
						final Scorer scorer = scorer(weightOrBitSet.set);
						if (scorer == null) {
							return null;
						}
						return new Weight.DefaultBulkScorer(scorer);
					}

			}

			@Override
			public Scorer scorer(LeafReaderContext context) throws IOException {
				final TermInSetQuery.WeightOrDocIdSet weightOrBitSet = rewrite(context);
				if (weightOrBitSet == null) {
					return null;
				}else
					if ((weightOrBitSet.weight) != null) {
						return weightOrBitSet.weight.scorer(context);
					}else {
						return scorer(weightOrBitSet.set);
					}

			}

			@Override
			public boolean isCacheable(LeafReaderContext ctx) {
				return false;
			}
		};
	}
}

