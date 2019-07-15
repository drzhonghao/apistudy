

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.valuesource.DocFreqValueSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.PerFieldSimilarityWrapper;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.util.BytesRef;


public class IDFValueSource extends DocFreqValueSource {
	public IDFValueSource(String field, String val, String indexedField, BytesRef indexedBytes) {
		super(field, val, indexedField, indexedBytes);
	}

	@Override
	public String name() {
		return "idf";
	}

	@Override
	public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
		IndexSearcher searcher = ((IndexSearcher) (context.get("searcher")));
		TFIDFSimilarity sim = IDFValueSource.asTFIDF(searcher.getSimilarity(true), field);
		if (sim == null) {
			throw new UnsupportedOperationException("requires a TFIDFSimilarity (such as ClassicSimilarity)");
		}
		int docfreq = searcher.getIndexReader().docFreq(new Term(indexedField, indexedBytes));
		float idf = sim.idf(docfreq, searcher.getIndexReader().maxDoc());
		return null;
	}

	static TFIDFSimilarity asTFIDF(Similarity sim, String field) {
		while (sim instanceof PerFieldSimilarityWrapper) {
			sim = ((PerFieldSimilarityWrapper) (sim)).get(field);
		} 
		if (sim instanceof TFIDFSimilarity) {
			return ((TFIDFSimilarity) (sim));
		}else {
			return null;
		}
	}
}

