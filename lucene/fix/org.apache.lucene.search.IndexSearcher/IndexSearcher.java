

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.LRUQueryCache;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryCache;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.UsageTrackingQueryCachingPolicy;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ThreadInterruptedException;


public class IndexSearcher {
	private static final Similarity NON_SCORING_SIMILARITY = new Similarity() {
		@Override
		public long computeNorm(FieldInvertState state) {
			throw new UnsupportedOperationException("This Similarity may only be used for searching, not indexing");
		}

		@Override
		public Similarity.SimWeight computeWeight(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
			return new Similarity.SimWeight() {};
		}

		@Override
		public Similarity.SimScorer simScorer(Similarity.SimWeight weight, LeafReaderContext context) throws IOException {
			return new Similarity.SimScorer() {
				@Override
				public float score(int doc, float freq) {
					return 0.0F;
				}

				@Override
				public float computeSlopFactor(int distance) {
					return 1.0F;
				}

				@Override
				public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
					return 1.0F;
				}
			};
		}
	};

	private static QueryCache DEFAULT_QUERY_CACHE;

	private static QueryCachingPolicy DEFAULT_CACHING_POLICY = new UsageTrackingQueryCachingPolicy();

	static {
		final int maxCachedQueries = 1000;
		final long maxRamBytesUsed = Math.min((1L << 25), ((Runtime.getRuntime().maxMemory()) / 20));
		IndexSearcher.DEFAULT_QUERY_CACHE = new LRUQueryCache(maxCachedQueries, maxRamBytesUsed);
	}

	final IndexReader reader;

	protected final IndexReaderContext readerContext;

	protected final List<LeafReaderContext> leafContexts;

	protected final IndexSearcher.LeafSlice[] leafSlices;

	private final ExecutorService executor;

	private static final Similarity defaultSimilarity = new BM25Similarity();

	private QueryCache queryCache = IndexSearcher.DEFAULT_QUERY_CACHE;

	private QueryCachingPolicy queryCachingPolicy = IndexSearcher.DEFAULT_CACHING_POLICY;

	public static Similarity getDefaultSimilarity() {
		return IndexSearcher.defaultSimilarity;
	}

	public static QueryCache getDefaultQueryCache() {
		return IndexSearcher.DEFAULT_QUERY_CACHE;
	}

	public static void setDefaultQueryCache(QueryCache defaultQueryCache) {
		IndexSearcher.DEFAULT_QUERY_CACHE = defaultQueryCache;
	}

	public static QueryCachingPolicy getDefaultQueryCachingPolicy() {
		return IndexSearcher.DEFAULT_CACHING_POLICY;
	}

	public static void setDefaultQueryCachingPolicy(QueryCachingPolicy defaultQueryCachingPolicy) {
		IndexSearcher.DEFAULT_CACHING_POLICY = defaultQueryCachingPolicy;
	}

	private Similarity similarity = IndexSearcher.defaultSimilarity;

	public IndexSearcher(IndexReader r) {
		this(r, null);
	}

	public IndexSearcher(IndexReader r, ExecutorService executor) {
		this(r.getContext(), executor);
	}

	public IndexSearcher(IndexReaderContext context, ExecutorService executor) {
		assert context.isTopLevel : "IndexSearcher's ReaderContext must be topLevel for reader" + (context.reader());
		reader = context.reader();
		this.executor = executor;
		this.readerContext = context;
		leafContexts = context.leaves();
		this.leafSlices = (executor == null) ? null : slices(leafContexts);
	}

	public IndexSearcher(IndexReaderContext context) {
		this(context, null);
	}

	public void setQueryCache(QueryCache queryCache) {
		this.queryCache = queryCache;
	}

	public QueryCache getQueryCache() {
		return queryCache;
	}

	public void setQueryCachingPolicy(QueryCachingPolicy queryCachingPolicy) {
		this.queryCachingPolicy = Objects.requireNonNull(queryCachingPolicy);
	}

	public QueryCachingPolicy getQueryCachingPolicy() {
		return queryCachingPolicy;
	}

	protected IndexSearcher.LeafSlice[] slices(List<LeafReaderContext> leaves) {
		IndexSearcher.LeafSlice[] slices = new IndexSearcher.LeafSlice[leaves.size()];
		for (int i = 0; i < (slices.length); i++) {
			slices[i] = new IndexSearcher.LeafSlice(leaves.get(i));
		}
		return slices;
	}

	public IndexReader getIndexReader() {
		return reader;
	}

	public Document doc(int docID) throws IOException {
		return reader.document(docID);
	}

	public void doc(int docID, StoredFieldVisitor fieldVisitor) throws IOException {
		reader.document(docID, fieldVisitor);
	}

	public Document doc(int docID, Set<String> fieldsToLoad) throws IOException {
		return reader.document(docID, fieldsToLoad);
	}

	public void setSimilarity(Similarity similarity) {
		this.similarity = similarity;
	}

	public Similarity getSimilarity(boolean needsScores) {
		return needsScores ? similarity : IndexSearcher.NON_SCORING_SIMILARITY;
	}

	public int count(Query query) throws IOException {
		query = rewrite(query);
		while (true) {
			if (query instanceof ConstantScoreQuery) {
				query = ((ConstantScoreQuery) (query)).getQuery();
			}else {
				break;
			}
		} 
		if (query instanceof MatchAllDocsQuery) {
			return reader.numDocs();
		}else
			if ((query instanceof TermQuery) && ((reader.hasDeletions()) == false)) {
				Term term = ((TermQuery) (query)).getTerm();
				int count = 0;
				for (LeafReaderContext leaf : reader.leaves()) {
					count += leaf.reader().docFreq(term);
				}
				return count;
			}

		final CollectorManager<TotalHitCountCollector, Integer> collectorManager = new CollectorManager<TotalHitCountCollector, Integer>() {
			@Override
			public TotalHitCountCollector newCollector() throws IOException {
				return new TotalHitCountCollector();
			}

			@Override
			public Integer reduce(Collection<TotalHitCountCollector> collectors) throws IOException {
				int total = 0;
				for (TotalHitCountCollector collector : collectors) {
					total += collector.getTotalHits();
				}
				return total;
			}
		};
		return search(query, collectorManager);
	}

	public TopDocs searchAfter(ScoreDoc after, Query query, int numHits) throws IOException {
		final int limit = Math.max(1, reader.maxDoc());
		if ((after != null) && ((after.doc) >= limit)) {
			throw new IllegalArgumentException(((("after.doc exceeds the number of documents in the reader: after.doc=" + (after.doc)) + " limit=") + limit));
		}
		final int cappedNumHits = Math.min(numHits, limit);
		final CollectorManager<TopScoreDocCollector, TopDocs> manager = new CollectorManager<TopScoreDocCollector, TopDocs>() {
			@Override
			public TopScoreDocCollector newCollector() throws IOException {
				return TopScoreDocCollector.create(cappedNumHits, after);
			}

			@Override
			public TopDocs reduce(Collection<TopScoreDocCollector> collectors) throws IOException {
				final TopDocs[] topDocs = new TopDocs[collectors.size()];
				int i = 0;
				for (TopScoreDocCollector collector : collectors) {
					topDocs[(i++)] = collector.topDocs();
				}
				return TopDocs.merge(0, cappedNumHits, topDocs, true);
			}
		};
		return search(query, manager);
	}

	public TopDocs search(Query query, int n) throws IOException {
		return searchAfter(null, query, n);
	}

	public void search(Query query, Collector results) throws IOException {
		query = rewrite(query);
		search(leafContexts, createWeight(query, results.needsScores(), 1), results);
	}

	public TopFieldDocs search(Query query, int n, Sort sort, boolean doDocScores, boolean doMaxScore) throws IOException {
		return searchAfter(null, query, n, sort, doDocScores, doMaxScore);
	}

	public TopFieldDocs search(Query query, int n, Sort sort) throws IOException {
		return searchAfter(null, query, n, sort, false, false);
	}

	public TopDocs searchAfter(ScoreDoc after, Query query, int n, Sort sort) throws IOException {
		return searchAfter(after, query, n, sort, false, false);
	}

	public TopFieldDocs searchAfter(ScoreDoc after, Query query, int numHits, Sort sort, boolean doDocScores, boolean doMaxScore) throws IOException {
		if ((after != null) && (!(after instanceof FieldDoc))) {
			throw new IllegalArgumentException(("after must be a FieldDoc; got " + after));
		}
		return searchAfter(((FieldDoc) (after)), query, numHits, sort, doDocScores, doMaxScore);
	}

	private TopFieldDocs searchAfter(FieldDoc after, Query query, int numHits, Sort sort, boolean doDocScores, boolean doMaxScore) throws IOException {
		final int limit = Math.max(1, reader.maxDoc());
		if ((after != null) && ((after.doc) >= limit)) {
			throw new IllegalArgumentException(((("after.doc exceeds the number of documents in the reader: after.doc=" + (after.doc)) + " limit=") + limit));
		}
		final int cappedNumHits = Math.min(numHits, limit);
		final CollectorManager<TopFieldCollector, TopFieldDocs> manager = new CollectorManager<TopFieldCollector, TopFieldDocs>() {
			@Override
			public TopFieldCollector newCollector() throws IOException {
				final boolean fillFields = true;
				return null;
			}

			@Override
			public TopFieldDocs reduce(Collection<TopFieldCollector> collectors) throws IOException {
				final TopFieldDocs[] topDocs = new TopFieldDocs[collectors.size()];
				int i = 0;
				for (TopFieldCollector collector : collectors) {
					topDocs[(i++)] = collector.topDocs();
				}
				return null;
			}
		};
		return search(query, manager);
	}

	public <C extends Collector, T> T search(Query query, CollectorManager<C, T> collectorManager) throws IOException {
		if ((executor) == null) {
			final C collector = collectorManager.newCollector();
			search(query, collector);
			return collectorManager.reduce(Collections.singletonList(collector));
		}else {
			final List<C> collectors = new ArrayList<>(leafSlices.length);
			boolean needsScores = false;
			for (int i = 0; i < (leafSlices.length); ++i) {
				final C collector = collectorManager.newCollector();
				collectors.add(collector);
				needsScores |= collector.needsScores();
			}
			query = rewrite(query);
			final Weight weight = createWeight(query, needsScores, 1);
			final List<Future<C>> topDocsFutures = new ArrayList<>(leafSlices.length);
			for (int i = 0; i < (leafSlices.length); ++i) {
				final LeafReaderContext[] leaves = leafSlices[i].leaves;
				final C collector = collectors.get(i);
				topDocsFutures.add(executor.submit(new Callable<C>() {
					@Override
					public C call() throws Exception {
						search(Arrays.asList(leaves), weight, collector);
						return collector;
					}
				}));
			}
			final List<C> collectedCollectors = new ArrayList<>();
			for (Future<C> future : topDocsFutures) {
				try {
					collectedCollectors.add(future.get());
				} catch (InterruptedException e) {
					throw new ThreadInterruptedException(e);
				} catch (ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
			return collectorManager.reduce(collectors);
		}
	}

	protected void search(List<LeafReaderContext> leaves, Weight weight, Collector collector) throws IOException {
		for (LeafReaderContext ctx : leaves) {
			final LeafCollector leafCollector;
			try {
				leafCollector = collector.getLeafCollector(ctx);
			} catch (CollectionTerminatedException e) {
				continue;
			}
			BulkScorer scorer = weight.bulkScorer(ctx);
			if (scorer != null) {
				try {
					scorer.score(leafCollector, ctx.reader().getLiveDocs());
				} catch (CollectionTerminatedException e) {
				}
			}
		}
	}

	public Query rewrite(Query original) throws IOException {
		Query query = original;
		for (Query rewrittenQuery = query.rewrite(reader); rewrittenQuery != query; rewrittenQuery = query.rewrite(reader)) {
			query = rewrittenQuery;
		}
		return query;
	}

	public Explanation explain(Query query, int doc) throws IOException {
		query = rewrite(query);
		return explain(createWeight(query, true, 1), doc);
	}

	protected Explanation explain(Weight weight, int doc) throws IOException {
		int n = ReaderUtil.subIndex(doc, leafContexts);
		final LeafReaderContext ctx = leafContexts.get(n);
		int deBasedDoc = doc - (ctx.docBase);
		final Bits liveDocs = ctx.reader().getLiveDocs();
		if ((liveDocs != null) && ((liveDocs.get(deBasedDoc)) == false)) {
			return Explanation.noMatch((("Document " + doc) + " is deleted"));
		}
		return weight.explain(ctx, deBasedDoc);
	}

	@Deprecated
	public Weight createNormalizedWeight(Query query, boolean needsScores) throws IOException {
		query = rewrite(query);
		return createWeight(query, needsScores, 1.0F);
	}

	public Weight createWeight(Query query, boolean needsScores, float boost) throws IOException {
		final QueryCache queryCache = this.queryCache;
		if ((needsScores == false) && (queryCache != null)) {
		}
		return null;
	}

	public IndexReaderContext getTopReaderContext() {
		return readerContext;
	}

	public static class LeafSlice {
		final LeafReaderContext[] leaves;

		public LeafSlice(LeafReaderContext... leaves) {
			this.leaves = leaves;
		}
	}

	@Override
	public String toString() {
		return ((("IndexSearcher(" + (reader)) + "; executor=") + (executor)) + ")";
	}

	public TermStatistics termStatistics(Term term, TermContext context) throws IOException {
		return new TermStatistics(term.bytes(), context.docFreq(), context.totalTermFreq());
	}

	public CollectionStatistics collectionStatistics(String field) throws IOException {
		final int docCount;
		final long sumTotalTermFreq;
		final long sumDocFreq;
		assert field != null;
		Terms terms = MultiFields.getTerms(reader, field);
		if (terms == null) {
			docCount = 0;
			sumTotalTermFreq = 0;
			sumDocFreq = 0;
		}else {
			docCount = terms.getDocCount();
			sumTotalTermFreq = terms.getSumTotalTermFreq();
			sumDocFreq = terms.getSumDocFreq();
		}
		return new CollectionStatistics(field, reader.maxDoc(), docCount, sumTotalTermFreq, sumDocFreq);
	}
}

