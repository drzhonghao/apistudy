

import java.util.List;
import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.ComposedQuery;
import org.apache.lucene.queryparser.surround.query.SrndQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import static org.apache.lucene.search.BooleanClause.Occur.MUST;


public class NotQuery extends ComposedQuery {
	public NotQuery(List<SrndQuery> queries, String opName) {
		super(queries, true, opName);
	}

	@Override
	public Query makeLuceneQueryFieldNoBoost(String fieldName, BasicQueryFactory qf) {
		List<Query> luceneSubQueries = makeLuceneSubQueriesField(fieldName, qf);
		BooleanQuery.Builder bq = new BooleanQuery.Builder();
		bq.add(luceneSubQueries.get(0), MUST);
		return bq.build();
	}
}

