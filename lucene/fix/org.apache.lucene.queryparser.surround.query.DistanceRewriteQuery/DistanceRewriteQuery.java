

import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.DistanceQuery;
import org.apache.lucene.search.Query;


class DistanceRewriteQuery {
	DistanceRewriteQuery(DistanceQuery srndQuery, String fieldName, BasicQueryFactory qf) {
	}

	public Query rewrite(IndexReader reader) throws IOException {
		return null;
	}
}

