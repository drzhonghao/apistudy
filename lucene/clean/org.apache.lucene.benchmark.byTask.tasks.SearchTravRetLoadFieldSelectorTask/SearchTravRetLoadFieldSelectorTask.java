import org.apache.lucene.benchmark.byTask.tasks.*;


import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.IndexReader;

/**
 * Search and Traverse and Retrieve docs task using a
 * FieldVisitor loading only the requested fields.
 *
 * <p>Note: This task reuses the reader if it is already open.
 * Otherwise a reader is opened at start and closed at the end.
 *
 * <p>Takes optional param: comma separated list of Fields to load.</p>
 * 
 * <p>Other side effects: counts additional 1 (record) for each traversed hit, 
 * and 1 more for each retrieved (non null) document.</p>
 */
public class SearchTravRetLoadFieldSelectorTask extends SearchTravTask {

  protected Set<String> fieldsToLoad;

  public SearchTravRetLoadFieldSelectorTask(PerfRunData runData) {
    super(runData);
    
  }

  @Override
  public boolean withRetrieve() {
    return true;
  }


  @Override
  protected Document retrieveDoc(IndexReader ir, int id) throws IOException {
    if (fieldsToLoad == null) {
      return ir.document(id);
    } else {
      DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor(fieldsToLoad);
      ir.document(id, visitor);
      return visitor.getDocument();
    }
  }

  @Override
  public void setParams(String params) {
    this.params = params; // cannot just call super.setParams(), b/c its params differ.
    fieldsToLoad = new HashSet<>();
    for (StringTokenizer tokenizer = new StringTokenizer(params, ","); tokenizer.hasMoreTokens();) {
      String s = tokenizer.nextToken();
      fieldsToLoad.add(s);
    }
  }


  /* (non-Javadoc)
  * @see org.apache.lucene.benchmark.byTask.tasks.PerfTask#supportsParams()
  */
  @Override
  public boolean supportsParams() {
    return true;
  }
}
