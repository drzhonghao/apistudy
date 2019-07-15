import org.apache.lucene.benchmark.byTask.tasks.*;



import java.io.IOException;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.index.DirectoryReader;

/**
* Reopens IndexReader and closes old IndexReader.
*
*/
public class ReopenReaderTask extends PerfTask {
  public ReopenReaderTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws IOException {
    DirectoryReader r = getRunData().getIndexReader();
    DirectoryReader nr = DirectoryReader.openIfChanged(r);
    if (nr != null) {
      getRunData().setIndexReader(nr);
      nr.decRef();
    }
    r.decRef();
    return 1;
  }
}
