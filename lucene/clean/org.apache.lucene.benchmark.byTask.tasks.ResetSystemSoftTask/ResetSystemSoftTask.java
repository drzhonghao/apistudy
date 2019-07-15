import org.apache.lucene.benchmark.byTask.tasks.*;


import org.apache.lucene.benchmark.byTask.PerfRunData;




/**
 * Reset all index and input data and call gc, does NOT erase index/dir, does NOT clear statistics.
 * This contains ResetInputs.
 * <br>Other side effects: writers/readers nullified, closed.
 * Index is NOT erased.
 * Directory is NOT erased.
 */
public class ResetSystemSoftTask extends ResetInputsTask {

  public ResetSystemSoftTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws Exception {
    getRunData().reinit(false);
    return 0;
  }

}
