import org.apache.lucene.benchmark.byTask.feeds.QueryMaker;
import org.apache.lucene.benchmark.byTask.feeds.*;



import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.benchmark.byTask.tasks.NewAnalyzerTask;

import java.util.ArrayList;

/**
 * A QueryMaker that makes queries for a collection created 
 * using {@link org.apache.lucene.benchmark.byTask.feeds.SingleDocSource}.
 */
public class SimpleQueryMaker extends AbstractQueryMaker implements QueryMaker {


  /**
   * Prepare the queries for this test.
   * Extending classes can override this method for preparing different queries. 
   * @return prepared queries.
   * @throws Exception if cannot prepare the queries.
   */
  @Override
  protected Query[] prepareQueries() throws Exception {
    // analyzer (default is standard analyzer)
    Analyzer anlzr= NewAnalyzerTask.createAnalyzer(config.get("analyzer",
        "org.apache.lucene.analysis.standard.StandardAnalyzer")); 
    
    QueryParser qp = new QueryParser(DocMaker.BODY_FIELD,anlzr);
    ArrayList<Query> qq = new ArrayList<>();
    Query q1 = new TermQuery(new Term(DocMaker.ID_FIELD,"doc2"));
    qq.add(q1);
    Query q2 = new TermQuery(new Term(DocMaker.BODY_FIELD,"simple"));
    qq.add(q2);
    BooleanQuery.Builder bq = new BooleanQuery.Builder();
    bq.add(q1,Occur.MUST);
    bq.add(q2,Occur.MUST);
    qq.add(bq.build());
    qq.add(qp.parse("synthetic body"));
    qq.add(qp.parse("\"synthetic body\""));
    qq.add(qp.parse("synthetic text"));
    qq.add(qp.parse("\"synthetic text\""));
    qq.add(qp.parse("\"synthetic text\"~3"));
    qq.add(qp.parse("zoom*"));
    qq.add(qp.parse("synth*"));
    return  qq.toArray(new Query[0]);
  }

}