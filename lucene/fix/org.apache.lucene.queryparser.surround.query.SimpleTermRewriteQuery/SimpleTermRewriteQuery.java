

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.SimpleTerm;
import org.apache.lucene.search.Query;


class SimpleTermRewriteQuery {
	SimpleTermRewriteQuery(SimpleTerm srndQuery, String fieldName, BasicQueryFactory qf) {
	}

	public Query rewrite(IndexReader reader) throws IOException {
		final List<Query> luceneSubQueries = new ArrayList<>();
		return null;
	}
}

