import org.apache.lucene.benchmark.quality.utils.*;


import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.PriorityQueue;

/**
 * Suggest Quality queries based on an index contents.
 * Utility class, used for making quality test benchmarks.
 */
public class QualityQueriesFinder {

  private static final String newline = System.getProperty("line.separator");
  private Directory dir;
  
  /**
   * Constructor over a directory containing the index.
   * @param dir directory containing the index we search for the quality test. 
   */
  private QualityQueriesFinder(Directory dir) {
    this.dir = dir;
  }

  /**
   * @param args {index-dir}
   * @throws IOException  if cannot access the index.
   */
  public static void main(String[] args) throws IOException {
    if (args.length<1) {
      System.err.println("Usage: java QualityQueriesFinder <index-dir>");
      System.exit(1);
    }
    QualityQueriesFinder qqf = new QualityQueriesFinder(FSDirectory.open(Paths.get(args[0])));
    String q[] = qqf.bestQueries("body",20);
    for (int i=0; i<q.length; i++) {
      System.out.println(newline+formatQueryAsTrecTopic(i,q[i],null,null));
    }
  }

  private String [] bestQueries(String field,int numQueries) throws IOException {
    String words[] = bestTerms("body",4*numQueries);
    int n = words.length;
    int m = n/4;
    String res[] = new String[m];
    for (int i=0; i<res.length; i++) {
      res[i] = words[i] + " " + words[m+i]+ "  " + words[n-1-m-i]  + " " + words[n-1-i];
      //System.out.println("query["+i+"]:  "+res[i]);
    }
    return res;
  }
  
  private static String formatQueryAsTrecTopic (int qnum, String title, String description, String narrative) {
    return 
      "<top>" + newline +
      "<num> Number: " + qnum             + newline + newline + 
      "<title> " + (title==null?"":title) + newline + newline + 
      "<desc> Description:"               + newline +
      (description==null?"":description)  + newline + newline +
      "<narr> Narrative:"                 + newline +
      (narrative==null?"":narrative)      + newline + newline +
      "</top>";
  }
  
  private String [] bestTerms(String field,int numTerms) throws IOException {
    PriorityQueue<TermDf> pq = new TermsDfQueue(numTerms);
    IndexReader ir = DirectoryReader.open(dir);
    try {
      int threshold = ir.maxDoc() / 10; // ignore words too common.
      Terms terms = MultiFields.getTerms(ir, field);
      if (terms != null) {
        TermsEnum termsEnum = terms.iterator();
        while (termsEnum.next() != null) {
          int df = termsEnum.docFreq();
          if (df<threshold) {
            String ttxt = termsEnum.term().utf8ToString();
            pq.insertWithOverflow(new TermDf(ttxt,df));
          }
        }
      }
    } finally {
      ir.close();
    }
    String res[] = new String[pq.size()];
    int i = 0;
    while (pq.size()>0) {
      TermDf tdf = pq.pop(); 
      res[i++] = tdf.word;
      System.out.println(i+".   word:  "+tdf.df+"   "+tdf.word);
    }
    return res;
  }

  private static class TermDf {
    String word;
    int df;
    TermDf (String word, int freq) {
      this.word = word;
      this.df = freq;
    }
  }
  
  private static class TermsDfQueue extends PriorityQueue<TermDf> {
    TermsDfQueue (int maxSize) {
      super(maxSize);
    }
    @Override
    protected boolean lessThan(TermDf tf1, TermDf tf2) {
      return tf1.df < tf2.df;
    }
  }
  
}
