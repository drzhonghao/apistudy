import org.apache.lucene.benchmark.byTask.tasks.*;



import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.index.IndexWriter;

/**
 * Runs forceMerge on the index.
 * <br>Other side effects: none.
 */
public class ForceMergeTask extends PerfTask {

  public ForceMergeTask(PerfRunData runData) {
    super(runData);
  }

  int maxNumSegments = -1;

  @Override
  public int doLogic() throws Exception {
    if (maxNumSegments == -1) {
      throw new IllegalStateException("required argument (maxNumSegments) was not specified");
    }
    IndexWriter iw = getRunData().getIndexWriter();
    iw.forceMerge(maxNumSegments);
    //System.out.println("forceMerge called");
    return 1;
  }

  @Override
  public void setParams(String params) {
    super.setParams(params);
    maxNumSegments = (int)Double.parseDouble(params);
  }

  @Override
  public boolean supportsParams() {
    return true;
  }
}
