import org.apache.lucene.queryparser.surround.query.*;

import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;

class SrndBooleanQuery {
  public static void addQueriesToBoolean(
          BooleanQuery.Builder bq,
          List<Query> queries,
          BooleanClause.Occur occur) {
    for (int i = 0; i < queries.size(); i++) {
      bq.add( queries.get(i), occur);
    }
  }
  
  public static Query makeBooleanQuery(
          List<Query> queries,
          BooleanClause.Occur occur) {
    if (queries.size() <= 1) {
      throw new AssertionError("Too few subqueries: " + queries.size());
    }
    BooleanQuery.Builder bq = new BooleanQuery.Builder();
    addQueriesToBoolean(bq, queries.subList(0, queries.size()), occur);
    return bq.build();
  }
}
