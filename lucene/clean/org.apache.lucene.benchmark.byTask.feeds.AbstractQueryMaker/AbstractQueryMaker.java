import org.apache.lucene.benchmark.byTask.feeds.*;


import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.search.Query;

/**
 * Abstract base query maker. 
 * Each query maker should just implement the {@link #prepareQueries()} method.
 **/
public abstract class AbstractQueryMaker implements QueryMaker {

  protected int qnum = 0;
  protected Query[] queries;
  protected Config config;

  @Override
  public void resetInputs() throws Exception {
    qnum = 0;
    // re-initialize since properties by round may have changed.
    setConfig(config);
  }

  protected abstract Query[] prepareQueries() throws Exception;

  @Override
  public void setConfig(Config config) throws Exception {
    this.config = config;
    queries = prepareQueries();
  }

  @Override
  public String printQueries() {
    String newline = System.getProperty("line.separator");
    StringBuilder sb = new StringBuilder();
    if (queries != null) {
      for (int i = 0; i < queries.length; i++) {
        sb.append(i+". "+ queries[i].getClass().getSimpleName()+" - "+queries[i].toString());
        sb.append(newline);
      }
    }
    return sb.toString();
  }

  @Override
  public Query makeQuery() throws Exception {
    return queries[nextQnum()];
  }
  
  // return next qnum
  protected synchronized int nextQnum() {
    int res = qnum;
    qnum = (qnum+1) % queries.length;
    return res;
  }

  /*
  *  (non-Javadoc)
  * @see org.apache.lucene.benchmark.byTask.feeds.QueryMaker#makeQuery(int)
  */
  @Override
  public Query makeQuery(int size) throws Exception {
    throw new Exception(this+".makeQuery(int size) is not supported!");
  }
}
