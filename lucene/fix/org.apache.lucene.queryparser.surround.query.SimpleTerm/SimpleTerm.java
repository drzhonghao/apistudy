

import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.DistanceSubQuery;
import org.apache.lucene.queryparser.surround.query.SpanNearClauseFactory;
import org.apache.lucene.queryparser.surround.query.SrndQuery;
import org.apache.lucene.search.Query;


public abstract class SimpleTerm extends SrndQuery implements Comparable<SimpleTerm> , DistanceSubQuery {
	public SimpleTerm(boolean q) {
		quoted = q;
	}

	private boolean quoted;

	boolean isQuoted() {
		return quoted;
	}

	public String getQuote() {
		return "\"";
	}

	public String getFieldOperator() {
		return "/";
	}

	public abstract String toStringUnquoted();

	@Override
	@Deprecated
	public int compareTo(SimpleTerm ost) {
		return this.toStringUnquoted().compareTo(ost.toStringUnquoted());
	}

	protected void suffixToString(StringBuilder r) {
	}

	@Override
	public String toString() {
		StringBuilder r = new StringBuilder();
		if (isQuoted()) {
			r.append(getQuote());
		}
		r.append(toStringUnquoted());
		if (isQuoted()) {
			r.append(getQuote());
		}
		suffixToString(r);
		weightToString(r);
		return r.toString();
	}

	public abstract void visitMatchingTerms(IndexReader reader, String fieldName, SimpleTerm.MatchingTermVisitor mtv) throws IOException;

	public interface MatchingTermVisitor {
		void visitMatchingTerm(Term t) throws IOException;
	}

	@Override
	public String distanceSubQueryNotAllowed() {
		return null;
	}

	@Override
	public void addSpanQueries(final SpanNearClauseFactory sncf) throws IOException {
		visitMatchingTerms(sncf.getIndexReader(), sncf.getFieldName(), new SimpleTerm.MatchingTermVisitor() {
			@Override
			public void visitMatchingTerm(Term term) throws IOException {
				sncf.addTermWeighted(term, getWeight());
			}
		});
	}

	@Override
	public Query makeLuceneQueryFieldNoBoost(final String fieldName, final BasicQueryFactory qf) {
		return null;
	}
}

