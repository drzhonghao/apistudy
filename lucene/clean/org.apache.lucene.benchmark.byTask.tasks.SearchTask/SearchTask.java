import org.apache.lucene.benchmark.byTask.tasks.*;



import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.feeds.QueryMaker;

/**
 * Search task.
 * 
 * <p>Note: This task reuses the reader if it is already open. 
 * Otherwise a reader is opened at start and closed at the end.
 */
public class SearchTask extends ReadTask {

  public SearchTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public boolean withRetrieve() {
    return false;
  }

  @Override
  public boolean withSearch() {
    return true;
  }

  @Override
  public boolean withTraverse() {
    return false;
  }

  @Override
  public boolean withWarm() {
    return false;
  }

  @Override
  public QueryMaker getQueryMaker() {
    return getRunData().getQueryMaker(this);
  }


}
