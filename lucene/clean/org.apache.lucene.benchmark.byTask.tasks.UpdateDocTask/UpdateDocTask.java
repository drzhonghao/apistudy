import org.apache.lucene.benchmark.byTask.tasks.*;



import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.feeds.DocMaker;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter;

/**
 * Update a document, using IndexWriter.updateDocument,
 * optionally with of a certain size.
 * <br>Other side effects: none.
 * <br>Takes optional param: document size. 
 */
public class UpdateDocTask extends PerfTask {

  public UpdateDocTask(PerfRunData runData) {
    super(runData);
  }

  private int docSize = 0;
  
  // volatile data passed between setup(), doLogic(), tearDown().
  private Document doc = null;
  
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
  public int doLogic() throws Exception {
    final String docID = doc.get(DocMaker.ID_FIELD);
    if (docID == null) {
      throw new IllegalStateException("document must define the docid field");
    }
    final IndexWriter iw = getRunData().getIndexWriter();
    iw.updateDocument(new Term(DocMaker.ID_FIELD, docID), doc);
    return 1;
  }

  @Override
  protected String getLogMessage(int recsCount) {
    return "updated " + recsCount + " docs";
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
