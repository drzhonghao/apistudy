

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.ComposedQuery;
import org.apache.lucene.queryparser.surround.query.DistanceSubQuery;
import org.apache.lucene.queryparser.surround.query.SpanNearClauseFactory;
import org.apache.lucene.queryparser.surround.query.SrndQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;


public class DistanceQuery extends ComposedQuery implements DistanceSubQuery {
	public DistanceQuery(List<SrndQuery> queries, boolean infix, int opDistance, String opName, boolean ordered) {
		super(queries, infix, opName);
		this.opDistance = opDistance;
		this.ordered = ordered;
	}

	private int opDistance;

	public int getOpDistance() {
		return opDistance;
	}

	private boolean ordered;

	public boolean subQueriesOrdered() {
		return ordered;
	}

	@Override
	public String distanceSubQueryNotAllowed() {
		Iterator<?> sqi = getSubQueriesIterator();
		while (sqi.hasNext()) {
			Object leq = sqi.next();
			if (leq instanceof DistanceSubQuery) {
				DistanceSubQuery dsq = ((DistanceSubQuery) (leq));
				String m = dsq.distanceSubQueryNotAllowed();
				if (m != null) {
					return m;
				}
			}else {
				return (("Operator " + (getOperatorName())) + " does not allow subquery ") + (leq.toString());
			}
		} 
		return null;
	}

	@Override
	public void addSpanQueries(SpanNearClauseFactory sncf) throws IOException {
		Query snq = getSpanNearQuery(sncf.getIndexReader(), sncf.getFieldName(), sncf.getBasicQueryFactory());
		sncf.addSpanQuery(snq);
	}

	public Query getSpanNearQuery(IndexReader reader, String fieldName, BasicQueryFactory qf) throws IOException {
		SpanQuery[] spanClauses = new SpanQuery[getNrSubQueries()];
		Iterator<?> sqi = getSubQueriesIterator();
		int qi = 0;
		while (sqi.hasNext()) {
			SpanNearClauseFactory sncf = new SpanNearClauseFactory(reader, fieldName, qf);
			((DistanceSubQuery) (sqi.next())).addSpanQueries(sncf);
			if ((sncf.size()) == 0) {
				while (sqi.hasNext()) {
					((DistanceSubQuery) (sqi.next())).addSpanQueries(sncf);
					sncf.clear();
				} 
				return new MatchNoDocsQuery();
			}
			spanClauses[qi] = sncf.makeSpanClause();
			qi++;
		} 
		return new SpanNearQuery(spanClauses, ((getOpDistance()) - 1), subQueriesOrdered());
	}

	@Override
	public Query makeLuceneQueryFieldNoBoost(final String fieldName, final BasicQueryFactory qf) {
		return null;
	}
}

