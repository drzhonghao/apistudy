

import java.io.IOException;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;


abstract class TermCollectingRewrite<B> extends MultiTermQuery.RewriteMethod {
	protected abstract B getTopLevelBuilder() throws IOException;

	protected abstract Query build(B builder);

	protected final void addClause(B topLevel, Term term, int docCount, float boost) throws IOException {
		addClause(topLevel, term, docCount, boost, null);
	}

	protected abstract void addClause(B topLevel, Term term, int docCount, float boost, TermContext states) throws IOException;

	final void collectTerms(IndexReader reader, MultiTermQuery query, TermCollectingRewrite.TermCollector collector) throws IOException {
		IndexReaderContext topReaderContext = reader.getContext();
		for (LeafReaderContext context : topReaderContext.leaves()) {
			collector.setReaderContext(topReaderContext, context);
			BytesRef bytes;
		}
	}

	static abstract class TermCollector {
		protected LeafReaderContext readerContext;

		protected IndexReaderContext topReaderContext;

		public void setReaderContext(IndexReaderContext topReaderContext, LeafReaderContext readerContext) {
			this.readerContext = readerContext;
			this.topReaderContext = topReaderContext;
		}

		public final AttributeSource attributes = new AttributeSource();

		public abstract boolean collect(BytesRef bytes) throws IOException;

		public abstract void setNextEnum(TermsEnum termsEnum) throws IOException;
	}
}

