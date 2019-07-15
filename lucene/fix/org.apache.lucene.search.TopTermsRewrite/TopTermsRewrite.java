

import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;


public abstract class TopTermsRewrite<B> {
	private final int size;

	public TopTermsRewrite(int size) {
		this.size = size;
	}

	public int getSize() {
		return size;
	}

	protected abstract int getMaxSize();

	public final Query rewrite(final IndexReader reader, final MultiTermQuery query) throws IOException {
		final int maxSize = Math.min(size, getMaxSize());
		final PriorityQueue<TopTermsRewrite.ScoreTerm> stQueue = new PriorityQueue<>();
		final TopTermsRewrite.ScoreTerm[] scoreTerms = stQueue.toArray(new TopTermsRewrite.ScoreTerm[stQueue.size()]);
		ArrayUtil.timSort(scoreTerms, TopTermsRewrite.scoreTermSortByTermComp);
		for (final TopTermsRewrite.ScoreTerm st : scoreTerms) {
		}
		return null;
	}

	@Override
	public int hashCode() {
		return 31 * (size);
	}

	@Override
	public boolean equals(Object obj) {
		if ((this) == obj)
			return true;

		if (obj == null)
			return false;

		if ((getClass()) != (obj.getClass()))
			return false;

		final TopTermsRewrite<?> other = ((TopTermsRewrite<?>) (obj));
		if ((size) != (other.size))
			return false;

		return true;
	}

	private static final Comparator<TopTermsRewrite.ScoreTerm> scoreTermSortByTermComp = null;

	static final class ScoreTerm implements Comparable<TopTermsRewrite.ScoreTerm> {
		public final BytesRefBuilder bytes = new BytesRefBuilder();

		public float boost;

		public final TermContext termState;

		public ScoreTerm(TermContext termState) {
			this.termState = termState;
		}

		@Override
		public int compareTo(TopTermsRewrite.ScoreTerm other) {
			if ((this.boost) == (other.boost))
				return other.bytes.get().compareTo(this.bytes.get());
			else
				return Float.compare(this.boost, other.boost);

		}
	}
}

