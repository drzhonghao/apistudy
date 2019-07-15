

import java.util.List;
import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.ComposedQuery;
import org.apache.lucene.queryparser.surround.query.SrndQuery;
import org.apache.lucene.search.Query;


public class AndQuery extends ComposedQuery {
	public AndQuery(List<SrndQuery> queries, boolean inf, String opName) {
		super(queries, inf, opName);
	}

	@Override
	public Query makeLuceneQueryFieldNoBoost(String fieldName, BasicQueryFactory qf) {
		return null;
	}
}

