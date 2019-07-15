import org.apache.lucene.benchmark.byTask.tasks.*;



import java.io.IOException;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.util.InfoStream;

/**
 * Close index writer.
 * <br>Other side effects: index writer object in perfRunData is nullified.
 * <br>Takes optional param "doWait": if false, then close(false) is called.
 */
public class CloseIndexTask extends PerfTask {

  public CloseIndexTask(PerfRunData runData) {
    super(runData);
  }

  boolean doWait = true;

  @Override
  public int doLogic() throws IOException {
    IndexWriter iw = getRunData().getIndexWriter();
    if (iw != null) {
      // If infoStream was set to output to a file, close it.
      InfoStream infoStream = iw.getConfig().getInfoStream();
      if (infoStream != null) {
        infoStream.close();
      }
      if (doWait == false) {
        iw.commit();
        iw.rollback();
      } else {
        iw.close();
      }
      getRunData().setIndexWriter(null);
    }
    return 1;
  }

  @Override
  public void setParams(String params) {
    super.setParams(params);
    doWait = Boolean.valueOf(params).booleanValue();
  }

  @Override
  public boolean supportsParams() {
    return true;
  }
}
