import org.apache.lucene.benchmark.byTask.tasks.*;



import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;

/**
 * Opens a reader and prints basic statistics.
 */
public class PrintReaderTask extends PerfTask {
  private String userData = null;
  
  public PrintReaderTask(PerfRunData runData) {
    super(runData);
  }
  
  @Override
  public void setParams(String params) {
    super.setParams(params);
    userData = params;
  }
  
  @Override
  public boolean supportsParams() {
    return true;
  }
  
  @Override
  public int doLogic() throws Exception {
    Directory dir = getRunData().getDirectory();
    IndexReader r = null;
    if (userData == null) 
      r = DirectoryReader.open(dir);
    else
      r = DirectoryReader.open(OpenReaderTask.findIndexCommit(dir, userData));
    System.out.println("--> numDocs:"+r.numDocs()+" dels:"+r.numDeletedDocs());
    r.close();
    return 1;
  }
}
