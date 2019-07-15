import org.apache.lucene.benchmark.byTask.tasks.*;


import org.apache.lucene.benchmark.byTask.PerfRunData;




/**
 * Reset all index and input data and call gc, erase index and dir, does NOT clear statistics.
 * <br>This contains ResetInputs.
 * <br>Other side effects: writers/readers nullified, deleted, closed.
 * Index is erased.
 * Directory is erased.
 */
public class ResetSystemEraseTask extends ResetSystemSoftTask {

  public ResetSystemEraseTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws Exception {
    getRunData().reinit(true);
    return 0;
  }
  
}
