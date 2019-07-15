import org.apache.lucene.benchmark.byTask.tasks.*;



import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.index.IndexWriter;

/**
 * Commits the IndexWriter.
 *
 */
public class CommitIndexTask extends PerfTask {
  Map<String,String> commitUserData;

  public CommitIndexTask(PerfRunData runData) {
    super(runData);
  }
  
  @Override
  public boolean supportsParams() {
    return true;
  }
  
  @Override
  public void setParams(String params) {
    super.setParams(params);
    commitUserData = new HashMap<>();
    commitUserData.put(OpenReaderTask.USER_DATA, params);
  }
  
  @Override
  public int doLogic() throws Exception {
    IndexWriter iw = getRunData().getIndexWriter();
    if (iw != null) {
      if (commitUserData != null) {
        iw.setLiveCommitData(commitUserData.entrySet());
      }
      iw.commit();
    }
    
    return 1;
  }
}
