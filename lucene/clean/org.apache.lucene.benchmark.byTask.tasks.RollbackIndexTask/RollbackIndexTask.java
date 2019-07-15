import org.apache.lucene.benchmark.byTask.tasks.*;



import java.io.IOException;
import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.util.InfoStream;

/**
 * Rollback the index writer.
 */
public class RollbackIndexTask extends PerfTask {

  public RollbackIndexTask(PerfRunData runData) {
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
      iw.rollback();
      getRunData().setIndexWriter(null);
    }
    return 1;
  }
}
