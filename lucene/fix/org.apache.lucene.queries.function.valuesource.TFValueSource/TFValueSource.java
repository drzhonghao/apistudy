

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.FloatDocValues;
import org.apache.lucene.queries.function.valuesource.DocFreqValueSource;
import org.apache.lucene.queries.function.valuesource.TermFreqValueSource;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;


public class TFValueSource extends TermFreqValueSource {
	public TFValueSource(String field, String val, String indexedField, BytesRef indexedBytes) {
		super(field, val, indexedField, indexedBytes);
	}

	@Override
	public String name() {
		return "tf";
	}

	@Override
	public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
		final Terms terms = readerContext.reader().terms(indexedField);
		IndexSearcher searcher = ((IndexSearcher) (context.get("searcher")));
		return new FloatDocValues(this) {
			PostingsEnum docs;

			int atDoc;

			int lastDocRequested = -1;

			{
				reset();
			}

			public void reset() throws IOException {
				if (terms != null) {
					final TermsEnum termsEnum = terms.iterator();
					if (termsEnum.seekExact(indexedBytes)) {
						docs = termsEnum.postings(null);
					}else {
						docs = null;
					}
				}else {
					docs = null;
				}
				if ((docs) == null) {
					docs = new PostingsEnum() {
						@Override
						public int freq() {
							return 0;
						}

						@Override
						public int nextPosition() throws IOException {
							return -1;
						}

						@Override
						public int startOffset() throws IOException {
							return -1;
						}

						@Override
						public int endOffset() throws IOException {
							return -1;
						}

						@Override
						public BytesRef getPayload() throws IOException {
							return null;
						}

						@Override
						public int docID() {
							return DocIdSetIterator.NO_MORE_DOCS;
						}

						@Override
						public int nextDoc() {
							return DocIdSetIterator.NO_MORE_DOCS;
						}

						@Override
						public int advance(int target) {
							return DocIdSetIterator.NO_MORE_DOCS;
						}

						@Override
						public long cost() {
							return 0;
						}
					};
				}
				atDoc = -1;
			}

			@Override
			public float floatVal(int doc) {
				try {
					if (doc < (lastDocRequested)) {
						reset();
					}
					lastDocRequested = doc;
					if ((atDoc) < doc) {
						atDoc = docs.advance(doc);
					}
					if ((atDoc) > doc) {
					}
				} catch (IOException e) {
					throw new RuntimeException(((("caught exception in function " + (description())) + " : doc=") + doc), e);
				}
				return 0f;
			}
		};
	}
}

