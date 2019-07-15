

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.ComposedQuery;
import org.apache.lucene.queryparser.surround.query.DistanceSubQuery;
import org.apache.lucene.queryparser.surround.query.SpanNearClauseFactory;
import org.apache.lucene.queryparser.surround.query.SrndQuery;
import org.apache.lucene.search.Query;


public class OrQuery extends ComposedQuery implements DistanceSubQuery {
	public OrQuery(List<SrndQuery> queries, boolean infix, String opName) {
		super(queries, infix, opName);
	}

	@Override
	public Query makeLuceneQueryFieldNoBoost(String fieldName, BasicQueryFactory qf) {
		return null;
	}

	@Override
	public String distanceSubQueryNotAllowed() {
		Iterator<SrndQuery> sqi = getSubQueriesIterator();
		while (sqi.hasNext()) {
			SrndQuery leq = sqi.next();
			if (leq instanceof DistanceSubQuery) {
				String m = ((DistanceSubQuery) (leq)).distanceSubQueryNotAllowed();
				if (m != null) {
					return m;
				}
			}else {
				return "subquery not allowed: " + (leq.toString());
			}
		} 
		return null;
	}

	@Override
	public void addSpanQueries(SpanNearClauseFactory sncf) throws IOException {
		Iterator<SrndQuery> sqi = getSubQueriesIterator();
		while (sqi.hasNext()) {
			SrndQuery s = sqi.next();
			((DistanceSubQuery) (s)).addSpanQueries(sncf);
		} 
	}
}

