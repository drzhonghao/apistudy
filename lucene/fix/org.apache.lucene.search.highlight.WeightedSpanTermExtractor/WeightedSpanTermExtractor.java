

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queries.CommonTermsQuery;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SynonymQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.highlight.OffsetLimitTokenFilter;
import org.apache.lucene.search.highlight.PositionSpan;
import org.apache.lucene.search.highlight.TermVectorLeafReader;
import org.apache.lucene.search.highlight.TokenStreamFromTermVector;
import org.apache.lucene.search.highlight.WeightedSpanTerm;
import org.apache.lucene.search.join.ToChildBlockJoinQuery;
import org.apache.lucene.search.join.ToParentBlockJoinQuery;
import org.apache.lucene.search.spans.FieldMaskingSpanQuery;
import org.apache.lucene.search.spans.SpanFirstQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanPositionCheckQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.IOUtils;

import static org.apache.lucene.search.spans.SpanWeight.Postings.POSITIONS;


public class WeightedSpanTermExtractor {
	private String fieldName;

	private TokenStream tokenStream;

	private String defaultField;

	private boolean expandMultiTermQuery;

	private boolean cachedTokenStream;

	private boolean wrapToCaching = true;

	private int maxDocCharsToAnalyze;

	private boolean usePayloads = false;

	private LeafReader internalReader = null;

	public WeightedSpanTermExtractor() {
	}

	public WeightedSpanTermExtractor(String defaultField) {
		if (defaultField != null) {
			this.defaultField = defaultField;
		}
	}

	protected void extract(Query query, float boost, Map<String, WeightedSpanTerm> terms) throws IOException {
		if (query instanceof BoostQuery) {
			BoostQuery boostQuery = ((BoostQuery) (query));
			extract(boostQuery.getQuery(), (boost * (boostQuery.getBoost())), terms);
		}else
			if (query instanceof BooleanQuery) {
				for (BooleanClause clause : ((BooleanQuery) (query))) {
					if (!(clause.isProhibited())) {
						extract(clause.getQuery(), boost, terms);
					}
				}
			}else
				if (query instanceof PhraseQuery) {
					PhraseQuery phraseQuery = ((PhraseQuery) (query));
					Term[] phraseQueryTerms = phraseQuery.getTerms();
					if ((phraseQueryTerms.length) == 1) {
						extractWeightedSpanTerms(terms, new SpanTermQuery(phraseQueryTerms[0]), boost);
					}else {
						SpanQuery[] clauses = new SpanQuery[phraseQueryTerms.length];
						for (int i = 0; i < (phraseQueryTerms.length); i++) {
							clauses[i] = new SpanTermQuery(phraseQueryTerms[i]);
						}
						int positionGaps = 0;
						int[] positions = phraseQuery.getPositions();
						if ((positions.length) >= 2) {
							positionGaps = Math.max(0, ((((positions[((positions.length) - 1)]) - (positions[0])) - (positions.length)) + 1));
						}
						boolean inorder = (phraseQuery.getSlop()) == 0;
						SpanNearQuery sp = new SpanNearQuery(clauses, ((phraseQuery.getSlop()) + positionGaps), inorder);
						extractWeightedSpanTerms(terms, sp, boost);
					}
				}else
					if ((query instanceof TermQuery) || (query instanceof SynonymQuery)) {
						extractWeightedTerms(terms, query, boost);
					}else
						if (query instanceof SpanQuery) {
							extractWeightedSpanTerms(terms, ((SpanQuery) (query)), boost);
						}else
							if (query instanceof ConstantScoreQuery) {
								final Query q = ((ConstantScoreQuery) (query)).getQuery();
								if (q != null) {
									extract(q, boost, terms);
								}
							}else
								if (query instanceof CommonTermsQuery) {
									extractWeightedTerms(terms, query, boost);
								}else
									if (query instanceof DisjunctionMaxQuery) {
										for (Query clause : ((DisjunctionMaxQuery) (query))) {
											extract(clause, boost, terms);
										}
									}else
										if (query instanceof ToParentBlockJoinQuery) {
											extract(((ToParentBlockJoinQuery) (query)).getChildQuery(), boost, terms);
										}else
											if (query instanceof ToChildBlockJoinQuery) {
												extract(((ToChildBlockJoinQuery) (query)).getParentQuery(), boost, terms);
											}else
												if (query instanceof MultiPhraseQuery) {
													final MultiPhraseQuery mpq = ((MultiPhraseQuery) (query));
													final Term[][] termArrays = mpq.getTermArrays();
													final int[] positions = mpq.getPositions();
													if ((positions.length) > 0) {
														int maxPosition = positions[((positions.length) - 1)];
														for (int i = 0; i < ((positions.length) - 1); ++i) {
															if ((positions[i]) > maxPosition) {
																maxPosition = positions[i];
															}
														}
														@SuppressWarnings({ "unchecked", "rawtypes" })
														final List<SpanQuery>[] disjunctLists = new List[maxPosition + 1];
														int distinctPositions = 0;
														for (int i = 0; i < (termArrays.length); ++i) {
															final Term[] termArray = termArrays[i];
															List<SpanQuery> disjuncts = disjunctLists[positions[i]];
															if (disjuncts == null) {
																disjuncts = disjunctLists[positions[i]] = new ArrayList<>(termArray.length);
																++distinctPositions;
															}
															for (Term aTermArray : termArray) {
																disjuncts.add(new SpanTermQuery(aTermArray));
															}
														}
														int positionGaps = 0;
														int position = 0;
														final SpanQuery[] clauses = new SpanQuery[distinctPositions];
														for (List<SpanQuery> disjuncts : disjunctLists) {
															if (disjuncts != null) {
																clauses[(position++)] = new SpanOrQuery(disjuncts.toArray(new SpanQuery[disjuncts.size()]));
															}else {
																++positionGaps;
															}
														}
														if ((clauses.length) == 1) {
															extractWeightedSpanTerms(terms, clauses[0], boost);
														}else {
															final int slop = mpq.getSlop();
															final boolean inorder = slop == 0;
															SpanNearQuery sp = new SpanNearQuery(clauses, (slop + positionGaps), inorder);
															extractWeightedSpanTerms(terms, sp, boost);
														}
													}
												}else
													if (query instanceof MatchAllDocsQuery) {
													}else
														if (query instanceof CustomScoreQuery) {
															extract(((CustomScoreQuery) (query)).getSubQuery(), boost, terms);
														}else
															if (query instanceof FunctionScoreQuery) {
																extract(((FunctionScoreQuery) (query)).getWrappedQuery(), boost, terms);
															}else
																if (isQueryUnsupported(query.getClass())) {
																}else {
																	if ((query instanceof MultiTermQuery) && ((!(expandMultiTermQuery)) || (!(fieldNameComparator(((MultiTermQuery) (query)).getField()))))) {
																		return;
																	}
																	Query origQuery = query;
																	final IndexReader reader = getLeafContext().reader();
																	Query rewritten;
																	if (query instanceof MultiTermQuery) {
																		rewritten = MultiTermQuery.SCORING_BOOLEAN_REWRITE.rewrite(reader, ((MultiTermQuery) (query)));
																	}else {
																		rewritten = origQuery.rewrite(reader);
																	}
																	if (rewritten != origQuery) {
																		extract(rewritten, boost, terms);
																	}else {
																		extractUnknownQuery(query, terms);
																	}
																}














	}

	protected boolean isQueryUnsupported(Class<? extends Query> clazz) {
		if (clazz.getName().startsWith("org.apache.lucene.spatial.")) {
			return true;
		}
		if (clazz.getName().startsWith("org.apache.lucene.spatial3d.")) {
			return true;
		}
		return false;
	}

	protected void extractUnknownQuery(Query query, Map<String, WeightedSpanTerm> terms) throws IOException {
	}

	protected void extractWeightedSpanTerms(Map<String, WeightedSpanTerm> terms, SpanQuery spanQuery, float boost) throws IOException {
		Set<String> fieldNames;
		if ((fieldName) == null) {
			fieldNames = new HashSet<>();
			collectSpanQueryFields(spanQuery, fieldNames);
		}else {
			fieldNames = new HashSet<>(1);
			fieldNames.add(fieldName);
		}
		if ((defaultField) != null) {
			fieldNames.add(defaultField);
		}
		Map<String, SpanQuery> queries = new HashMap<>();
		Set<Term> nonWeightedTerms = new HashSet<>();
		final boolean mustRewriteQuery = mustRewriteQuery(spanQuery);
		final IndexSearcher searcher = new IndexSearcher(getLeafContext());
		searcher.setQueryCache(null);
		if (mustRewriteQuery) {
			for (final String field : fieldNames) {
				final SpanQuery rewrittenQuery = ((SpanQuery) (spanQuery.rewrite(getLeafContext().reader())));
				queries.put(field, rewrittenQuery);
				rewrittenQuery.createWeight(searcher, false, boost).extractTerms(nonWeightedTerms);
			}
		}else {
			spanQuery.createWeight(searcher, false, boost).extractTerms(nonWeightedTerms);
		}
		List<PositionSpan> spanPositions = new ArrayList<>();
		for (final String field : fieldNames) {
			final SpanQuery q;
			if (mustRewriteQuery) {
				q = queries.get(field);
			}else {
				q = spanQuery;
			}
			LeafReaderContext context = getLeafContext();
			SpanWeight w = ((SpanWeight) (searcher.createWeight(searcher.rewrite(q), false, 1)));
			Bits acceptDocs = context.reader().getLiveDocs();
			final Spans spans = w.getSpans(context, POSITIONS);
			if (spans == null) {
				return;
			}
			while ((spans.nextDoc()) != (Spans.NO_MORE_DOCS)) {
				if ((acceptDocs != null) && ((acceptDocs.get(spans.docID())) == false)) {
					continue;
				}
				while ((spans.nextStartPosition()) != (Spans.NO_MORE_POSITIONS)) {
					spanPositions.add(new PositionSpan(spans.startPosition(), ((spans.endPosition()) - 1)));
				} 
			} 
		}
		if ((spanPositions.size()) == 0) {
			return;
		}
		for (final Term queryTerm : nonWeightedTerms) {
			if (fieldNameComparator(queryTerm.field())) {
				WeightedSpanTerm weightedSpanTerm = terms.get(queryTerm.text());
				if (weightedSpanTerm == null) {
					weightedSpanTerm = new WeightedSpanTerm(boost, queryTerm.text());
					weightedSpanTerm.addPositionSpans(spanPositions);
					terms.put(queryTerm.text(), weightedSpanTerm);
				}else {
					if ((spanPositions.size()) > 0) {
						weightedSpanTerm.addPositionSpans(spanPositions);
					}
				}
			}
		}
	}

	protected void extractWeightedTerms(Map<String, WeightedSpanTerm> terms, Query query, float boost) throws IOException {
		Set<Term> nonWeightedTerms = new HashSet<>();
		final IndexSearcher searcher = new IndexSearcher(getLeafContext());
		searcher.createWeight(searcher.rewrite(query), false, 1).extractTerms(nonWeightedTerms);
		for (final Term queryTerm : nonWeightedTerms) {
			if (fieldNameComparator(queryTerm.field())) {
				WeightedSpanTerm weightedSpanTerm = new WeightedSpanTerm(boost, queryTerm.text());
				terms.put(queryTerm.text(), weightedSpanTerm);
			}
		}
	}

	protected boolean fieldNameComparator(String fieldNameToCheck) {
		boolean rv = (((fieldName) == null) || (fieldName.equals(fieldNameToCheck))) || (((defaultField) != null) && (defaultField.equals(fieldNameToCheck)));
		return rv;
	}

	protected LeafReaderContext getLeafContext() throws IOException {
		if ((internalReader) == null) {
			boolean cacheIt = (wrapToCaching) && (!((tokenStream) instanceof CachingTokenFilter));
			if ((tokenStream) instanceof TokenStreamFromTermVector) {
				cacheIt = false;
				Terms termVectorTerms = ((TokenStreamFromTermVector) (tokenStream)).getTermVectorTerms();
				if ((termVectorTerms.hasPositions()) && (termVectorTerms.hasOffsets())) {
					internalReader = new TermVectorLeafReader(WeightedSpanTermExtractor.DelegatingLeafReader.FIELD_NAME, termVectorTerms);
				}
			}
			if ((internalReader) == null) {
				final MemoryIndex indexer = new MemoryIndex(true, usePayloads);
				if (cacheIt) {
					assert !(cachedTokenStream);
					tokenStream = new CachingTokenFilter(new OffsetLimitTokenFilter(tokenStream, maxDocCharsToAnalyze));
					cachedTokenStream = true;
					indexer.addField(WeightedSpanTermExtractor.DelegatingLeafReader.FIELD_NAME, tokenStream);
				}else {
					indexer.addField(WeightedSpanTermExtractor.DelegatingLeafReader.FIELD_NAME, new OffsetLimitTokenFilter(tokenStream, maxDocCharsToAnalyze));
				}
				final IndexSearcher searcher = indexer.createSearcher();
				internalReader = ((LeafReaderContext) (searcher.getTopReaderContext())).reader();
			}
			this.internalReader = new WeightedSpanTermExtractor.DelegatingLeafReader(internalReader);
		}
		return internalReader.getContext();
	}

	static final class DelegatingLeafReader extends FilterLeafReader {
		private static final String FIELD_NAME = "shadowed_field";

		DelegatingLeafReader(LeafReader in) {
			super(in);
		}

		@Override
		public FieldInfos getFieldInfos() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Terms terms(String field) throws IOException {
			return super.terms(WeightedSpanTermExtractor.DelegatingLeafReader.FIELD_NAME);
		}

		@Override
		public NumericDocValues getNumericDocValues(String field) throws IOException {
			return super.getNumericDocValues(WeightedSpanTermExtractor.DelegatingLeafReader.FIELD_NAME);
		}

		@Override
		public BinaryDocValues getBinaryDocValues(String field) throws IOException {
			return super.getBinaryDocValues(WeightedSpanTermExtractor.DelegatingLeafReader.FIELD_NAME);
		}

		@Override
		public SortedDocValues getSortedDocValues(String field) throws IOException {
			return super.getSortedDocValues(WeightedSpanTermExtractor.DelegatingLeafReader.FIELD_NAME);
		}

		@Override
		public NumericDocValues getNormValues(String field) throws IOException {
			return super.getNormValues(WeightedSpanTermExtractor.DelegatingLeafReader.FIELD_NAME);
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

	public Map<String, WeightedSpanTerm> getWeightedSpanTerms(Query query, float boost, TokenStream tokenStream) throws IOException {
		return getWeightedSpanTerms(query, boost, tokenStream, null);
	}

	public Map<String, WeightedSpanTerm> getWeightedSpanTerms(Query query, float boost, TokenStream tokenStream, String fieldName) throws IOException {
		this.fieldName = fieldName;
		Map<String, WeightedSpanTerm> terms = new WeightedSpanTermExtractor.PositionCheckingMap<>();
		this.tokenStream = tokenStream;
		try {
			extract(query, boost, terms);
		} finally {
			IOUtils.close(internalReader);
		}
		return terms;
	}

	public Map<String, WeightedSpanTerm> getWeightedSpanTermsWithScores(Query query, float boost, TokenStream tokenStream, String fieldName, IndexReader reader) throws IOException {
		if (fieldName != null) {
			this.fieldName = fieldName;
		}else {
			this.fieldName = null;
		}
		this.tokenStream = tokenStream;
		Map<String, WeightedSpanTerm> terms = new WeightedSpanTermExtractor.PositionCheckingMap<>();
		extract(query, boost, terms);
		int totalNumDocs = reader.maxDoc();
		Set<String> weightedTerms = terms.keySet();
		Iterator<String> it = weightedTerms.iterator();
		try {
			while (it.hasNext()) {
				WeightedSpanTerm weightedSpanTerm = terms.get(it.next());
			} 
		} finally {
			IOUtils.close(internalReader);
		}
		return terms;
	}

	protected void collectSpanQueryFields(SpanQuery spanQuery, Set<String> fieldNames) {
		if (spanQuery instanceof FieldMaskingSpanQuery) {
			collectSpanQueryFields(((FieldMaskingSpanQuery) (spanQuery)).getMaskedQuery(), fieldNames);
		}else
			if (spanQuery instanceof SpanFirstQuery) {
				collectSpanQueryFields(((SpanFirstQuery) (spanQuery)).getMatch(), fieldNames);
			}else
				if (spanQuery instanceof SpanNearQuery) {
					for (final SpanQuery clause : ((SpanNearQuery) (spanQuery)).getClauses()) {
						collectSpanQueryFields(clause, fieldNames);
					}
				}else
					if (spanQuery instanceof SpanNotQuery) {
						collectSpanQueryFields(((SpanNotQuery) (spanQuery)).getInclude(), fieldNames);
					}else
						if (spanQuery instanceof SpanOrQuery) {
							for (final SpanQuery clause : ((SpanOrQuery) (spanQuery)).getClauses()) {
								collectSpanQueryFields(clause, fieldNames);
							}
						}else {
							fieldNames.add(spanQuery.getField());
						}




	}

	protected boolean mustRewriteQuery(SpanQuery spanQuery) {
		if (!(expandMultiTermQuery)) {
			return false;
		}else
			if (spanQuery instanceof FieldMaskingSpanQuery) {
				return mustRewriteQuery(((FieldMaskingSpanQuery) (spanQuery)).getMaskedQuery());
			}else
				if (spanQuery instanceof SpanFirstQuery) {
					return mustRewriteQuery(((SpanFirstQuery) (spanQuery)).getMatch());
				}else
					if (spanQuery instanceof SpanNearQuery) {
						for (final SpanQuery clause : ((SpanNearQuery) (spanQuery)).getClauses()) {
							if (mustRewriteQuery(clause)) {
								return true;
							}
						}
						return false;
					}else
						if (spanQuery instanceof SpanNotQuery) {
							SpanNotQuery spanNotQuery = ((SpanNotQuery) (spanQuery));
							return (mustRewriteQuery(spanNotQuery.getInclude())) || (mustRewriteQuery(spanNotQuery.getExclude()));
						}else
							if (spanQuery instanceof SpanOrQuery) {
								for (final SpanQuery clause : ((SpanOrQuery) (spanQuery)).getClauses()) {
									if (mustRewriteQuery(clause)) {
										return true;
									}
								}
								return false;
							}else
								if (spanQuery instanceof SpanTermQuery) {
									return false;
								}else {
									return true;
								}






	}

	@SuppressWarnings("serial")
	protected static class PositionCheckingMap<K> extends HashMap<K, WeightedSpanTerm> {
		@Override
		public void putAll(Map<? extends K, ? extends WeightedSpanTerm> m) {
			for (Map.Entry<? extends K, ? extends WeightedSpanTerm> entry : m.entrySet())
				this.put(entry.getKey(), entry.getValue());

		}

		@Override
		public WeightedSpanTerm put(K key, WeightedSpanTerm value) {
			WeightedSpanTerm prev = super.put(key, value);
			if (prev == null)
				return prev;

			WeightedSpanTerm prevTerm = prev;
			WeightedSpanTerm newTerm = value;
			return prev;
		}
	}

	public boolean getExpandMultiTermQuery() {
		return expandMultiTermQuery;
	}

	public void setExpandMultiTermQuery(boolean expandMultiTermQuery) {
		this.expandMultiTermQuery = expandMultiTermQuery;
	}

	public boolean isUsePayloads() {
		return usePayloads;
	}

	public void setUsePayloads(boolean usePayloads) {
		this.usePayloads = usePayloads;
	}

	public boolean isCachedTokenStream() {
		return cachedTokenStream;
	}

	public TokenStream getTokenStream() {
		assert (tokenStream) != null;
		return tokenStream;
	}

	public void setWrapIfNotCachingTokenFilter(boolean wrap) {
		this.wrapToCaching = wrap;
	}

	protected final void setMaxDocCharsToAnalyze(int maxDocCharsToAnalyze) {
		this.maxDocCharsToAnalyze = maxDocCharsToAnalyze;
	}
}

