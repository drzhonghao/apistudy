

import java.io.IOException;
import java.util.List;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.FrequencyTrackingRingBuffer;


public class UsageTrackingQueryCachingPolicy implements QueryCachingPolicy {
	private static final int SENTINEL = Integer.MIN_VALUE;

	private static boolean isPointQuery(Query query) {
		for (Class<?> clazz = query.getClass(); clazz != (Query.class); clazz = clazz.getSuperclass()) {
			final String simpleName = clazz.getSimpleName();
			if ((simpleName.startsWith("Point")) && (simpleName.endsWith("Query"))) {
				return true;
			}
		}
		return false;
	}

	static boolean isCostly(Query query) {
		return false;
	}

	private static boolean shouldNeverCache(Query query) {
		if (query instanceof TermQuery) {
			return true;
		}
		if (query instanceof MatchAllDocsQuery) {
			return true;
		}
		if (query instanceof MatchNoDocsQuery) {
			return true;
		}
		if (query instanceof BooleanQuery) {
			BooleanQuery bq = ((BooleanQuery) (query));
			if (bq.clauses().isEmpty()) {
				return true;
			}
		}
		if (query instanceof DisjunctionMaxQuery) {
			DisjunctionMaxQuery dmq = ((DisjunctionMaxQuery) (query));
			if (dmq.getDisjuncts().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private final FrequencyTrackingRingBuffer recentlyUsedFilters;

	public UsageTrackingQueryCachingPolicy(int historySize) {
		this.recentlyUsedFilters = new FrequencyTrackingRingBuffer(historySize, UsageTrackingQueryCachingPolicy.SENTINEL);
	}

	public UsageTrackingQueryCachingPolicy() {
		this(256);
	}

	protected int minFrequencyToCache(Query query) {
		if (UsageTrackingQueryCachingPolicy.isCostly(query)) {
			return 2;
		}else {
			int minFrequency = 5;
			if ((query instanceof BooleanQuery) || (query instanceof DisjunctionMaxQuery)) {
				minFrequency--;
			}
			return minFrequency;
		}
	}

	@Override
	public void onUse(Query query) {
		assert (query instanceof BoostQuery) == false;
		assert (query instanceof ConstantScoreQuery) == false;
		if (UsageTrackingQueryCachingPolicy.shouldNeverCache(query)) {
			return;
		}
		int hashCode = query.hashCode();
		synchronized(this) {
			recentlyUsedFilters.add(hashCode);
		}
	}

	int frequency(Query query) {
		assert (query instanceof BoostQuery) == false;
		assert (query instanceof ConstantScoreQuery) == false;
		int hashCode = query.hashCode();
		synchronized(this) {
			return recentlyUsedFilters.frequency(hashCode);
		}
	}

	@Override
	public boolean shouldCache(Query query) throws IOException {
		if (UsageTrackingQueryCachingPolicy.shouldNeverCache(query)) {
			return false;
		}
		final int frequency = frequency(query);
		final int minFrequency = minFrequencyToCache(query);
		return frequency >= minFrequency;
	}
}

