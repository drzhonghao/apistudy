import org.apache.lucene.benchmark.byTask.tasks.*;



import java.util.Locale;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.feeds.DocMaker;
import org.apache.lucene.document.Document;

/**
 * Add a document, optionally of a certain size.
 * <br>Other side effects: none.
 * <br>Takes optional param: document size. 
 */
public class AddDocTask extends PerfTask {

  public AddDocTask(PerfRunData runData) {
    super(runData);
  }

  private int docSize = 0;
  
  /** 
   * volatile data passed between setup(), doLogic(), tearDown().
   * the doc is created at setup() and added at doLogic(). 
   */
  protected Document doc = null;

  @Override
  public void setup() throws Exception {
    super.setup();
    DocMaker docMaker = getRunData().getDocMaker();
    if (docSize > 0) {
      doc = docMaker.makeDocument(docSize);
    } else {
      doc = docMaker.makeDocument();
    }
  }

  @Override
  public void tearDown() throws Exception {
    doc = null;
    super.tearDown();
  }

  @Override
  protected String getLogMessage(int recsCount) {
    return String.format(Locale.ROOT, "added %9d docs",recsCount);
  }
  
  @Override
  public int doLogic() throws Exception {
    getRunData().getIndexWriter().addDocument(doc);
    return 1;
  }

  /**
   * Set the params (docSize only)
   * @param params docSize, or 0 for no limit.
   */
  @Override
  public void setParams(String params) {
    super.setParams(params);
    docSize = (int) Float.parseFloat(params); 
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.benchmark.byTask.tasks.PerfTask#supportsParams()
   */
  @Override
  public boolean supportsParams() {
    return true;
  }
  
}
