

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.suggest.document.TopSuggestDocs;
import org.apache.lucene.search.suggest.document.TopSuggestDocs.SuggestScoreDoc;


public class TopSuggestDocsCollector extends SimpleCollector {
	private final int num;

	private final List<TopSuggestDocs.SuggestScoreDoc> pendingResults;

	final CharArraySet seenSurfaceForms;

	protected int docBase;

	public TopSuggestDocsCollector(int num, boolean skipDuplicates) {
		if (num <= 0) {
			throw new IllegalArgumentException("'num' must be > 0");
		}
		this.num = num;
		if (skipDuplicates) {
			seenSurfaceForms = new CharArraySet(num, false);
			pendingResults = new ArrayList<>();
		}else {
			seenSurfaceForms = null;
			pendingResults = null;
		}
	}

	protected boolean doSkipDuplicates() {
		return (seenSurfaceForms) != null;
	}

	public int getCountToCollect() {
		return num;
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		docBase = context.docBase;
		if ((seenSurfaceForms) != null) {
			seenSurfaceForms.clear();
		}
	}

	public void collect(int docID, CharSequence key, CharSequence context, float score) throws IOException {
		TopSuggestDocs.SuggestScoreDoc current = new TopSuggestDocs.SuggestScoreDoc(((docBase) + docID), key, context, score);
	}

	public TopSuggestDocs get() throws IOException {
		TopSuggestDocs.SuggestScoreDoc[] suggestScoreDocs;
		if ((seenSurfaceForms) != null) {
			seenSurfaceForms.clear();
			Collections.sort(pendingResults, new Comparator<TopSuggestDocs.SuggestScoreDoc>() {
				@Override
				public int compare(TopSuggestDocs.SuggestScoreDoc a, TopSuggestDocs.SuggestScoreDoc b) {
					int cmp = Float.compare(b.score, a.score);
					if (cmp == 0) {
						cmp = Integer.compare(a.doc, b.doc);
					}
					return cmp;
				}
			});
			List<TopSuggestDocs.SuggestScoreDoc> hits = new ArrayList<>();
			for (TopSuggestDocs.SuggestScoreDoc hit : pendingResults) {
				if ((seenSurfaceForms.contains(hit.key)) == false) {
					seenSurfaceForms.add(hit.key);
					hits.add(hit);
					if ((hits.size()) == (num)) {
						break;
					}
				}
			}
			suggestScoreDocs = hits.toArray(new TopSuggestDocs.SuggestScoreDoc[0]);
		}else {
		}
		suggestScoreDocs = null;
		if ((suggestScoreDocs.length) > 0) {
			return new TopSuggestDocs(suggestScoreDocs.length, suggestScoreDocs, suggestScoreDocs[0].score);
		}else {
			return TopSuggestDocs.EMPTY;
		}
	}

	@Override
	public void collect(int doc) throws IOException {
	}

	@Override
	public boolean needsScores() {
		return true;
	}
}

