import org.apache.lucene.benchmark.byTask.tasks.*;



import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import java.io.IOException;

/**
 * Open an index writer.
 * <br>Other side effects: index writer object in perfRunData is set.
 * <br>Relevant properties: <code>merge.factor, max.buffered,
 * max.field.length, ram.flush.mb [default 0]</code>.
 *
 * <p> Accepts a param specifying the commit point as
 * previously saved with CommitIndexTask.  If you specify
 * this, it rolls the index back to that commit on opening
 * the IndexWriter.
 */
public class OpenIndexTask extends PerfTask {

  public static final int DEFAULT_MAX_BUFFERED = IndexWriterConfig.DEFAULT_MAX_BUFFERED_DOCS;
  public static final int DEFAULT_MERGE_PFACTOR = LogMergePolicy.DEFAULT_MERGE_FACTOR;
  public static final double DEFAULT_RAM_FLUSH_MB = (int) IndexWriterConfig.DEFAULT_RAM_BUFFER_SIZE_MB;
  private String commitUserData;

  public OpenIndexTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws IOException {
    PerfRunData runData = getRunData();
    Config config = runData.getConfig();
    final IndexCommit ic;
    if (commitUserData != null) {
      ic = OpenReaderTask.findIndexCommit(runData.getDirectory(), commitUserData);
    } else {
      ic = null;
    }
    
    final IndexWriter writer = CreateIndexTask.configureWriter(config, runData, OpenMode.APPEND, ic);
    runData.setIndexWriter(writer);
    return 1;
  }

  @Override
  public void setParams(String params) {
    super.setParams(params);
    if (params != null) {
      // specifies which commit point to open
      commitUserData = params;
    }
  }

  @Override
  public boolean supportsParams() {
    return true;
  }
}
