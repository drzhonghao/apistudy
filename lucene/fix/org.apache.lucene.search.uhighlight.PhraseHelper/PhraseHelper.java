

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.highlight.WeightedSpanTerm;
import org.apache.lucene.search.highlight.WeightedSpanTermExtractor;
import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanScorer;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.uhighlight.OffsetsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PriorityQueue;


public class PhraseHelper {
	public static final PhraseHelper NONE = new PhraseHelper(new MatchAllDocsQuery(), "_ignored_", ( s) -> false, ( spanQuery) -> null, ( query) -> null, true);

	private final String fieldName;

	private final Set<BytesRef> positionInsensitiveTerms;

	private final Set<SpanQuery> spanQueries;

	private final boolean willRewrite;

	private final Predicate<String> fieldMatcher;

	public PhraseHelper(Query query, String field, Predicate<String> fieldMatcher, Function<SpanQuery, Boolean> rewriteQueryPred, Function<Query, Collection<Query>> preExtractRewriteFunction, boolean ignoreQueriesNeedingRewrite) {
		this.fieldName = field;
		this.fieldMatcher = fieldMatcher;
		positionInsensitiveTerms = new HashSet<>();
		spanQueries = new HashSet<>();
		boolean[] mustRewriteHolder = new boolean[]{ false };
		Set<Term> extractPosInsensitiveTermsTarget = new TreeSet<Term>() {
			@Override
			public boolean add(Term term) {
				if (fieldMatcher.test(term.field())) {
					return positionInsensitiveTerms.add(term.bytes());
				}else {
					return false;
				}
			}
		};
		new WeightedSpanTermExtractor(field) {
			{
				setExpandMultiTermQuery(true);
				try {
					extract(query, 1.0F, null);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			protected void extract(Query query, float boost, Map<String, WeightedSpanTerm> terms) throws IOException {
				Collection<Query> newQueriesToExtract = preExtractRewriteFunction.apply(query);
				if (newQueriesToExtract != null) {
					for (Query newQuery : newQueriesToExtract) {
						extract(newQuery, boost, terms);
					}
				}else {
					super.extract(query, boost, terms);
				}
			}

			@Override
			protected boolean isQueryUnsupported(Class<? extends Query> clazz) {
				if (clazz.isAssignableFrom(MultiTermQuery.class)) {
					return true;
				}
				return true;
			}

			@Override
			protected void extractWeightedTerms(Map<String, WeightedSpanTerm> terms, Query query, float boost) throws IOException {
			}

			@Override
			protected void extractWeightedSpanTerms(Map<String, WeightedSpanTerm> terms, SpanQuery spanQuery, float boost) throws IOException {
				Set<String> fieldNameSet = new HashSet<>();
				collectSpanQueryFields(spanQuery, fieldNameSet);
				for (String spanField : fieldNameSet) {
					if (!(fieldMatcher.test(spanField))) {
						return;
					}
				}
				boolean mustRewriteQuery = mustRewriteQuery(spanQuery);
				if (ignoreQueriesNeedingRewrite && mustRewriteQuery) {
					return;
				}
				mustRewriteHolder[0] |= mustRewriteQuery;
				spanQueries.add(spanQuery);
			}

			@Override
			protected boolean mustRewriteQuery(SpanQuery spanQuery) {
				Boolean rewriteQ = rewriteQueryPred.apply(spanQuery);
				return rewriteQ != null ? rewriteQ : super.mustRewriteQuery(spanQuery);
			}
		};
		willRewrite = mustRewriteHolder[0];
	}

	public Set<SpanQuery> getSpanQueries() {
		return spanQueries;
	}

	public boolean hasPositionSensitivity() {
		return (spanQueries.isEmpty()) == false;
	}

	public boolean willRewrite() {
		return willRewrite;
	}

	public BytesRef[] getAllPositionInsensitiveTerms() {
		BytesRef[] result = positionInsensitiveTerms.toArray(new BytesRef[positionInsensitiveTerms.size()]);
		Arrays.sort(result);
		return result;
	}

	public void createOffsetsEnumsForSpans(LeafReader leafReader, int docId, List<OffsetsEnum> results) throws IOException {
		leafReader = new PhraseHelper.SingleFieldWithOffsetsFilterLeafReader(leafReader, fieldName);
		IndexSearcher searcher = new IndexSearcher(leafReader);
		searcher.setQueryCache(null);
		PriorityQueue<Spans> spansPriorityQueue = new PriorityQueue<Spans>(spanQueries.size()) {
			@Override
			protected boolean lessThan(Spans a, Spans b) {
				return (a.startPosition()) <= (b.startPosition());
			}
		};
		for (Query query : spanQueries) {
			Weight weight = searcher.createWeight(searcher.rewrite(query), false, 1);
			Scorer scorer = weight.scorer(leafReader.getContext());
			if (scorer == null) {
				continue;
			}
			TwoPhaseIterator twoPhaseIterator = scorer.twoPhaseIterator();
			if (twoPhaseIterator != null) {
				if (((twoPhaseIterator.approximation().advance(docId)) != docId) || (!(twoPhaseIterator.matches()))) {
					continue;
				}
			}else
				if ((scorer.iterator().advance(docId)) != docId) {
					continue;
				}

			Spans spans = ((SpanScorer) (scorer)).getSpans();
			assert (spans.docID()) == docId;
			if ((spans.nextStartPosition()) != (Spans.NO_MORE_POSITIONS)) {
				spansPriorityQueue.add(spans);
			}
		}
		PhraseHelper.OffsetSpanCollector spanCollector = new PhraseHelper.OffsetSpanCollector();
		while ((spansPriorityQueue.size()) > 0) {
			Spans spans = spansPriorityQueue.top();
			spans.collect(spanCollector);
			if ((spans.nextStartPosition()) == (Spans.NO_MORE_POSITIONS)) {
				spansPriorityQueue.pop();
			}else {
				spansPriorityQueue.updateTop();
			}
		} 
		results.addAll(spanCollector.termToOffsetsEnums.values());
	}

	private static final class SingleFieldWithOffsetsFilterLeafReader extends FilterLeafReader {
		final String fieldName;

		SingleFieldWithOffsetsFilterLeafReader(LeafReader in, String fieldName) {
			super(in);
			this.fieldName = fieldName;
		}

		@Override
		public FieldInfos getFieldInfos() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Terms terms(String field) throws IOException {
			return new FilterLeafReader.FilterTerms(super.terms(fieldName)) {
				@Override
				public TermsEnum iterator() throws IOException {
					return new FilterLeafReader.FilterTermsEnum(in.iterator()) {
						@Override
						public PostingsEnum postings(PostingsEnum reuse, int flags) throws IOException {
							return super.postings(reuse, (flags | (PostingsEnum.OFFSETS)));
						}
					};
				}
			};
		}

		@Override
		public NumericDocValues getNormValues(String field) throws IOException {
			return super.getNormValues(fieldName);
		}

		@Override
		public IndexReader.CacheHelper getCoreCacheHelper() {
			return null;
		}

		@Override
		public IndexReader.CacheHelper getReaderCacheHelper() {
			return null;
		}
	}

	private class OffsetSpanCollector implements SpanCollector {
		Map<BytesRef, PhraseHelper.SpanCollectedOffsetsEnum> termToOffsetsEnums = new HashMap<>();

		@Override
		public void collectLeaf(PostingsEnum postings, int position, Term term) throws IOException {
			if (!(fieldMatcher.test(term.field()))) {
				return;
			}
			PhraseHelper.SpanCollectedOffsetsEnum offsetsEnum = termToOffsetsEnums.get(term.bytes());
			if (offsetsEnum == null) {
				if (positionInsensitiveTerms.contains(term.bytes())) {
					return;
				}
				offsetsEnum = new PhraseHelper.SpanCollectedOffsetsEnum(term.bytes(), postings.freq());
				termToOffsetsEnums.put(term.bytes(), offsetsEnum);
			}
			offsetsEnum.add(postings.startOffset(), postings.endOffset());
		}

		@Override
		public void reset() {
		}
	}

	private static class SpanCollectedOffsetsEnum extends OffsetsEnum {
		private final BytesRef term;

		private final int[] startOffsets;

		private final int[] endOffsets;

		private int numPairs = 0;

		private int enumIdx = -1;

		private SpanCollectedOffsetsEnum(BytesRef term, int postingsFreq) {
			this.term = term;
			this.startOffsets = new int[postingsFreq];
			this.endOffsets = new int[postingsFreq];
		}

		void add(int startOffset, int endOffset) {
			assert (enumIdx) == (-1) : "bad state";
			int pairIdx = (numPairs) - 1;
			for (; pairIdx >= 0; pairIdx--) {
				int iStartOffset = startOffsets[pairIdx];
				int iEndOffset = endOffsets[pairIdx];
				int cmp = Integer.compare(iStartOffset, startOffset);
				if (cmp == 0) {
					cmp = Integer.compare(iEndOffset, endOffset);
				}
				if (cmp == 0) {
					return;
				}else
					if (cmp < 0) {
						break;
					}

			}
			final int shiftLen = (numPairs) - (pairIdx + 1);
			if (shiftLen > 0) {
				System.arraycopy(startOffsets, (pairIdx + 2), startOffsets, (pairIdx + 3), shiftLen);
				System.arraycopy(endOffsets, (pairIdx + 2), endOffsets, (pairIdx + 3), shiftLen);
			}
			startOffsets[(pairIdx + 1)] = startOffset;
			endOffsets[(pairIdx + 1)] = endOffset;
			(numPairs)++;
		}

		@Override
		public boolean nextPosition() throws IOException {
			return (++(enumIdx)) < (numPairs);
		}

		@Override
		public int freq() throws IOException {
			return numPairs;
		}

		@Override
		public BytesRef getTerm() throws IOException {
			return term;
		}

		@Override
		public int startOffset() throws IOException {
			return startOffsets[enumIdx];
		}

		@Override
		public int endOffset() throws IOException {
			return endOffsets[enumIdx];
		}
	}
}

