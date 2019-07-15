import org.apache.lucene.benchmark.quality.utils.DocNameExtractor;
import org.apache.lucene.benchmark.quality.utils.*;


import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;

/**
 * Create a log ready for submission.
 * Extend this class and override
 * {@link #report(QualityQuery, TopDocs, String, IndexSearcher)}
 * to create different reports. 
 */
public class SubmissionReport {

  private NumberFormat nf;
  private PrintWriter logger;
  private String name;
  
  /**
   * Constructor for SubmissionReport.
   * @param logger if null, no submission data is created. 
   * @param name name of this run.
   */
  public SubmissionReport (PrintWriter logger, String name) {
    this.logger = logger;
    this.name = name;
    nf = NumberFormat.getInstance(Locale.ROOT);
    nf.setMaximumFractionDigits(4);
    nf.setMinimumFractionDigits(4);
  }
  
  /**
   * Report a search result for a certain quality query.
   * @param qq quality query for which the results are reported.
   * @param td search results for the query.
   * @param docNameField stored field used for fetching the result doc name.  
   * @param searcher index access for fetching doc name.
   * @throws IOException in case of a problem.
   */
  public void report(QualityQuery qq, TopDocs td, String docNameField, IndexSearcher searcher) throws IOException {
    if (logger==null) {
      return;
    }
    ScoreDoc sd[] = td.scoreDocs;
    String sep = " \t ";
    DocNameExtractor xt = new DocNameExtractor(docNameField);
    for (int i=0; i<sd.length; i++) {
      String docName = xt.docName(searcher,sd[i].doc);
      logger.println(
          qq.getQueryID()       + sep +
          "Q0"                   + sep +
          format(docName,20)    + sep +
          format(""+i,7)        + sep +
          nf.format(sd[i].score) + sep +
          name
          );
    }
  }

  public void flush() {
    if (logger!=null) {
      logger.flush();
    }
  }
  
  private static String padd = "                                    ";
  private String format(String s, int minLen) {
    s = (s==null ? "" : s);
    int n = Math.max(minLen,s.length());
    return (s+padd).substring(0,n);
  }
}
