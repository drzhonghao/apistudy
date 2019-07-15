import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.*;



import java.io.IOException;
import java.util.Date;

/**
 * Parser for the FT docs in trec disks 4+5 collection format
 */
public class TrecFTParser extends TrecDocParser {

  private static final String DATE = "<DATE>";
  private static final String DATE_END = "</DATE>";
  
  private static final String HEADLINE = "<HEADLINE>";
  private static final String HEADLINE_END = "</HEADLINE>";

  @Override
  public DocData parse(DocData docData, String name, TrecContentSource trecSrc, 
      StringBuilder docBuf, ParsePathType pathType) throws IOException {
    int mark = 0; // that much is skipped

    // date...
    Date date = null;
    String dateStr = extract(docBuf, DATE, DATE_END, -1, null);
    if (dateStr != null) {
      date = trecSrc.parseDate(dateStr);
    }
     
    // title...
    String title = extract(docBuf, HEADLINE, HEADLINE_END, -1, null);

    docData.clear();
    docData.setName(name);
    docData.setDate(date);
    docData.setTitle(title);
    docData.setBody(stripTags(docBuf, mark).toString());
    return docData;
  }

}
