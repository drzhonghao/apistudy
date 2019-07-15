

import java.io.IOException;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.suggest.BitsProducer;
import org.apache.lucene.search.suggest.document.CompletionTerms;


public abstract class CompletionQuery extends Query {
	private final Term term;

	private final BitsProducer filter;

	protected CompletionQuery(Term term, BitsProducer filter) {
		validate(term.text());
		this.term = term;
		this.filter = filter;
	}

	public BitsProducer getFilter() {
		return filter;
	}

	public String getField() {
		return term.field();
	}

	public Term getTerm() {
		return term;
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		byte type = 0;
		boolean first = true;
		Terms terms;
		for (LeafReaderContext context : reader.leaves()) {
			LeafReader leafReader = context.reader();
			try {
				if ((terms = leafReader.terms(getField())) == null) {
					continue;
				}
			} catch (IOException e) {
				continue;
			}
			if (terms instanceof CompletionTerms) {
				CompletionTerms completionTerms = ((CompletionTerms) (terms));
				byte t = completionTerms.getType();
				if (first) {
					type = t;
					first = false;
				}else
					if (type != t) {
						throw new IllegalStateException(((getField()) + " has values of multiple types"));
					}

			}
		}
		if (first == false) {
		}
		return super.rewrite(reader);
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		if (!(term.field().equals(field))) {
			buffer.append(term.field());
			buffer.append(":");
		}
		buffer.append(term.text());
		buffer.append('*');
		if ((filter) != null) {
			buffer.append(",");
			buffer.append("filter");
			buffer.append(":");
			buffer.append(filter.toString());
		}
		return buffer.toString();
	}

	private void validate(String termText) {
		for (int i = 0; i < (termText.length()); i++) {
		}
	}
}

