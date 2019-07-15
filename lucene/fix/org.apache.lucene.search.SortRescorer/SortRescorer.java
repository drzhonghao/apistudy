

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Rescorer;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;


public class SortRescorer extends Rescorer {
	private final Sort sort;

	public SortRescorer(Sort sort) {
		this.sort = sort;
	}

	@Override
	public TopDocs rescore(IndexSearcher searcher, TopDocs firstPassTopDocs, int topN) throws IOException {
		ScoreDoc[] hits = firstPassTopDocs.scoreDocs.clone();
		Arrays.sort(hits, new Comparator<ScoreDoc>() {
			@Override
			public int compare(ScoreDoc a, ScoreDoc b) {
				return (a.doc) - (b.doc);
			}
		});
		List<LeafReaderContext> leaves = searcher.getIndexReader().leaves();
		TopFieldCollector collector = TopFieldCollector.create(sort, topN, true, true, true, true);
		int hitUpto = 0;
		int readerUpto = -1;
		int endDoc = 0;
		int docBase = 0;
		LeafCollector leafCollector = null;
		while (hitUpto < (hits.length)) {
			ScoreDoc hit = hits[hitUpto];
			int docID = hit.doc;
			LeafReaderContext readerContext = null;
			while (docID >= endDoc) {
				readerUpto++;
				readerContext = leaves.get(readerUpto);
				endDoc = (readerContext.docBase) + (readerContext.reader().maxDoc());
			} 
			if (readerContext != null) {
				leafCollector = collector.getLeafCollector(readerContext);
				docBase = readerContext.docBase;
			}
			hitUpto++;
		} 
		return collector.topDocs();
	}

	@Override
	public Explanation explain(IndexSearcher searcher, Explanation firstPassExplanation, int docID) throws IOException {
		List<Explanation> subs = new ArrayList<>();
		Explanation first = Explanation.match(firstPassExplanation.getValue(), "first pass score", firstPassExplanation);
		subs.add(first);
		SortField[] sortFields = sort.getSort();
		for (int i = 0; i < (sortFields.length); i++) {
		}
		return Explanation.match(0.0F, ("sort field values for sort=" + (sort.toString())), subs);
	}
}

