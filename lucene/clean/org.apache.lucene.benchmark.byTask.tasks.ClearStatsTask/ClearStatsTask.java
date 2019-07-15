import org.apache.lucene.benchmark.byTask.tasks.*;


import org.apache.lucene.benchmark.byTask.PerfRunData;


/**
 * Clear statistics data.
 * <br>Other side effects: None.
 */
public class ClearStatsTask extends PerfTask {

  public ClearStatsTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws Exception {
    getRunData().getPoints().clearData();
    return 0;
  }

  /* (non-Javadoc)
   * @see PerfTask#shouldNotRecordStats()
   */
  @Override
  protected boolean shouldNotRecordStats() {
    return true;
  }

}
