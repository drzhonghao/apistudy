import org.apache.lucene.benchmark.byTask.tasks.*;


import org.apache.lucene.benchmark.byTask.PerfRunData;



/**
 * Increment the counter for properties maintained by Round Number.
 * <br>Other side effects: if there are props by round number, log value change.
 */
public class NewRoundTask extends PerfTask {

  public NewRoundTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws Exception {
    getRunData().getConfig().newRound();
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
