import org.apache.lucene.benchmark.byTask.tasks.*;


import org.apache.lucene.benchmark.byTask.PerfRunData;




/**
 * Reset inputs so that the test run would behave, input wise, 
 * as if it just started. This affects e.g. the generation of docs and queries.
 */
public class ResetInputsTask extends PerfTask {

  public ResetInputsTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws Exception {
    getRunData().resetInputs();
    return 0;
  }
  
  /*
   * (non-Javadoc)
   * @see org.apache.lucene.benchmark.byTask.tasks.PerfTask#shouldNotRecordStats()
   */
  @Override
  protected boolean shouldNotRecordStats() {
    return true;
  }


}
