

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.MultiFacets;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.FilterCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.TopScoreDocCollector;


public class DrillSideways {
	protected final IndexSearcher searcher;

	protected final TaxonomyReader taxoReader;

	protected final SortedSetDocValuesReaderState state;

	protected final FacetsConfig config;

	private final ExecutorService executor;

	public DrillSideways(IndexSearcher searcher, FacetsConfig config, TaxonomyReader taxoReader) {
		this(searcher, config, taxoReader, null);
	}

	public DrillSideways(IndexSearcher searcher, FacetsConfig config, SortedSetDocValuesReaderState state) {
		this(searcher, config, null, state);
	}

	public DrillSideways(IndexSearcher searcher, FacetsConfig config, TaxonomyReader taxoReader, SortedSetDocValuesReaderState state) {
		this(searcher, config, taxoReader, state, null);
	}

	public DrillSideways(IndexSearcher searcher, FacetsConfig config, TaxonomyReader taxoReader, SortedSetDocValuesReaderState state, ExecutorService executor) {
		this.searcher = searcher;
		this.config = config;
		this.taxoReader = taxoReader;
		this.state = state;
		this.executor = executor;
	}

	protected Facets buildFacetsResult(FacetsCollector drillDowns, FacetsCollector[] drillSideways, String[] drillSidewaysDims) throws IOException {
		Facets drillDownFacets;
		Map<String, Facets> drillSidewaysFacets = new HashMap<>();
		if ((taxoReader) != null) {
			drillDownFacets = new FastTaxonomyFacetCounts(taxoReader, config, drillDowns);
			if (drillSideways != null) {
				for (int i = 0; i < (drillSideways.length); i++) {
					drillSidewaysFacets.put(drillSidewaysDims[i], new FastTaxonomyFacetCounts(taxoReader, config, drillSideways[i]));
				}
			}
		}else {
			drillDownFacets = new SortedSetDocValuesFacetCounts(state, drillDowns);
			if (drillSideways != null) {
				for (int i = 0; i < (drillSideways.length); i++) {
					drillSidewaysFacets.put(drillSidewaysDims[i], new SortedSetDocValuesFacetCounts(state, drillSideways[i]));
				}
			}
		}
		if (drillSidewaysFacets.isEmpty()) {
			return drillDownFacets;
		}else {
			return new MultiFacets(drillSidewaysFacets, drillDownFacets);
		}
	}

	public DrillSideways.DrillSidewaysResult search(DrillDownQuery query, Collector hitCollector) throws IOException {
		FacetsCollector drillDownCollector = new FacetsCollector();
		if ((hitCollector.needsScores()) == false) {
			hitCollector = new FilterCollector(hitCollector) {
				@Override
				public boolean needsScores() {
					return true;
				}
			};
		}
		return null;
	}

	public DrillSideways.DrillSidewaysResult search(DrillDownQuery query, Query filter, FieldDoc after, int topN, Sort sort, boolean doDocScores, boolean doMaxScore) throws IOException {
		if (filter != null) {
		}
		if (sort != null) {
			int limit = searcher.getIndexReader().maxDoc();
			if (limit == 0) {
				limit = 1;
			}
			final int fTopN = Math.min(topN, limit);
			if ((executor) != null) {
				final CollectorManager<TopFieldCollector, TopFieldDocs> collectorManager = new CollectorManager<TopFieldCollector, TopFieldDocs>() {
					@Override
					public TopFieldCollector newCollector() throws IOException {
						return TopFieldCollector.create(sort, fTopN, after, true, doDocScores, doMaxScore, true);
					}

					@Override
					public TopFieldDocs reduce(Collection<TopFieldCollector> collectors) throws IOException {
						final TopFieldDocs[] topFieldDocs = new TopFieldDocs[collectors.size()];
						int pos = 0;
						for (TopFieldCollector collector : collectors)
							topFieldDocs[(pos++)] = collector.topDocs();

						return TopDocs.merge(sort, topN, topFieldDocs);
					}
				};
				DrillSideways.ConcurrentDrillSidewaysResult<TopFieldDocs> r = search(query, collectorManager);
				return new DrillSideways.DrillSidewaysResult(r.facets, r.collectorResult);
			}else {
				final TopFieldCollector hitCollector = TopFieldCollector.create(sort, fTopN, after, true, doDocScores, doMaxScore, true);
				DrillSideways.DrillSidewaysResult r = search(query, hitCollector);
				return new DrillSideways.DrillSidewaysResult(r.facets, hitCollector.topDocs());
			}
		}else {
			return search(after, query, topN);
		}
	}

	public DrillSideways.DrillSidewaysResult search(DrillDownQuery query, int topN) throws IOException {
		return search(null, query, topN);
	}

	public DrillSideways.DrillSidewaysResult search(ScoreDoc after, DrillDownQuery query, int topN) throws IOException {
		int limit = searcher.getIndexReader().maxDoc();
		if (limit == 0) {
			limit = 1;
		}
		final int fTopN = Math.min(topN, limit);
		if ((executor) != null) {
			final CollectorManager<TopScoreDocCollector, TopDocs> collectorManager = new CollectorManager<TopScoreDocCollector, TopDocs>() {
				@Override
				public TopScoreDocCollector newCollector() throws IOException {
					return TopScoreDocCollector.create(fTopN, after);
				}

				@Override
				public TopDocs reduce(Collection<TopScoreDocCollector> collectors) throws IOException {
					final TopDocs[] topDocs = new TopDocs[collectors.size()];
					int pos = 0;
					for (TopScoreDocCollector collector : collectors)
						topDocs[(pos++)] = collector.topDocs();

					return TopDocs.merge(topN, topDocs);
				}
			};
			DrillSideways.ConcurrentDrillSidewaysResult<TopDocs> r = search(query, collectorManager);
			return new DrillSideways.DrillSidewaysResult(r.facets, r.collectorResult);
		}else {
			TopScoreDocCollector hitCollector = TopScoreDocCollector.create(topN, after);
			DrillSideways.DrillSidewaysResult r = search(query, hitCollector);
			return new DrillSideways.DrillSidewaysResult(r.facets, hitCollector.topDocs());
		}
	}

	protected boolean scoreSubDocsAtOnce() {
		return false;
	}

	public static class DrillSidewaysResult {
		public final Facets facets;

		public final TopDocs hits;

		public DrillSidewaysResult(Facets facets, TopDocs hits) {
			this.facets = facets;
			this.hits = hits;
		}
	}

	private static class CallableCollector implements Callable<DrillSideways.CallableResult> {
		private final int pos;

		private final IndexSearcher searcher;

		private final Query query;

		private final CollectorManager<?, ?> collectorManager;

		private CallableCollector(int pos, IndexSearcher searcher, Query query, CollectorManager<?, ?> collectorManager) {
			this.pos = pos;
			this.searcher = searcher;
			this.query = query;
			this.collectorManager = collectorManager;
		}

		@Override
		public DrillSideways.CallableResult call() throws Exception {
			return new DrillSideways.CallableResult(pos, searcher.search(query, collectorManager));
		}
	}

	private static class CallableResult {
		private final int pos;

		private final Object result;

		private CallableResult(int pos, Object result) {
			this.pos = pos;
			this.result = result;
		}
	}

	private DrillDownQuery getDrillDownQuery(final DrillDownQuery query, Query[] queries, final String excludedDimension) {
		return null;
	}

	public <R> DrillSideways.ConcurrentDrillSidewaysResult<R> search(final DrillDownQuery query, final CollectorManager<?, R> hitCollectorManager) throws IOException {
		int i = 0;
		final FacetsCollector mainFacetsCollector;
		final R collectorResult;
		return null;
	}

	public static class ConcurrentDrillSidewaysResult<R> extends DrillSideways.DrillSidewaysResult {
		public final R collectorResult;

		ConcurrentDrillSidewaysResult(Facets facets, TopDocs hits, R collectorResult) {
			super(facets, hits);
			this.collectorResult = collectorResult;
		}
	}
}

