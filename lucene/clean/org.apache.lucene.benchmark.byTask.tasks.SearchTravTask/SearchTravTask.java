import org.apache.lucene.benchmark.byTask.tasks.*;



import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.feeds.QueryMaker;

/**
 * Search and Traverse task.
 * 
 * <p>Note: This task reuses the reader if it is already open. 
 * Otherwise a reader is opened at start and closed at the end.
 * 
 * <p>Takes optional param: traversal size (otherwise all results are traversed).</p>
 * 
 * <p>Other side effects: counts additional 1 (record) for each traversed hit.</p>
 */
public class SearchTravTask extends ReadTask {
  protected int traversalSize = Integer.MAX_VALUE;

  public SearchTravTask(PerfRunData runData) {
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
    return true;
  }

  @Override
  public boolean withWarm() {
    return false;
  }

  

  @Override
  public QueryMaker getQueryMaker() {
    return getRunData().getQueryMaker(this);
  }

  @Override
  public int traversalSize() {
    return traversalSize;
  }

  @Override
  public void setParams(String params) {
    super.setParams(params);
    traversalSize = (int)Float.parseFloat(params);
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.benchmark.byTask.tasks.PerfTask#supportsParams()
   */
  @Override
  public boolean supportsParams() {
    return true;
  }
}
