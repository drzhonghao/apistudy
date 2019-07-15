

import java.util.Comparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.suggest.Lookup;


public class TopSuggestDocs extends TopDocs {
	public static final TopSuggestDocs EMPTY = new TopSuggestDocs(0, new TopSuggestDocs.SuggestScoreDoc[0], 0);

	public static class SuggestScoreDoc extends ScoreDoc implements Comparable<TopSuggestDocs.SuggestScoreDoc> {
		public final CharSequence key;

		public final CharSequence context;

		public SuggestScoreDoc(int doc, CharSequence key, CharSequence context, float score) {
			super(doc, score);
			this.key = key;
			this.context = context;
		}

		@Override
		public int compareTo(TopSuggestDocs.SuggestScoreDoc o) {
			return Lookup.CHARSEQUENCE_COMPARATOR.compare(key, o.key);
		}

		@Override
		public boolean equals(Object other) {
			if ((other instanceof TopSuggestDocs.SuggestScoreDoc) == false) {
				return false;
			}else {
				return key.equals(((TopSuggestDocs.SuggestScoreDoc) (other)).key);
			}
		}

		@Override
		public int hashCode() {
			return key.hashCode();
		}

		@Override
		public String toString() {
			return (((((("key=" + (key)) + " doc=") + (doc)) + " score=") + (score)) + " shardIndex=") + (shardIndex);
		}
	}

	public TopSuggestDocs(int totalHits, TopSuggestDocs.SuggestScoreDoc[] scoreDocs, float maxScore) {
		super(totalHits, scoreDocs, maxScore);
	}

	public TopSuggestDocs.SuggestScoreDoc[] scoreLookupDocs() {
		return ((TopSuggestDocs.SuggestScoreDoc[]) (scoreDocs));
	}

	public static TopSuggestDocs merge(int topN, TopSuggestDocs[] shardHits) {
		for (TopSuggestDocs shardHit : shardHits) {
			for (TopSuggestDocs.SuggestScoreDoc scoreDoc : shardHit.scoreLookupDocs()) {
			}
		}
		return null;
	}
}

