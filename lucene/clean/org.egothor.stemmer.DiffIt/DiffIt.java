import org.egothor.stemmer.Diff;
import org.egothor.stemmer.*;


import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.lucene.util.SuppressForbidden;

/**
 * The DiffIt class is a means generate patch commands from an already prepared
 * stemmer table.
 */
public class DiffIt {
  
  /** no instantiation */
  private DiffIt() {}
  
  static int get(int i, String s) {
    try {
      return Integer.parseInt(s.substring(i, i + 1));
    } catch (Throwable x) {
      return 1;
    }
  }
  
  /**
   * Entry point to the DiffIt application.
   * <p>
   * This application takes one argument, the path to a file containing a
   * stemmer table. The program reads the file and generates the patch commands
   * for the stems.
   * 
   * @param args the path to a file containing a stemmer table
   */
  @SuppressForbidden(reason = "System.out required: command line tool")
  public static void main(java.lang.String[] args) throws Exception {
    
    int ins = get(0, args[0]);
    int del = get(1, args[0]);
    int rep = get(2, args[0]);
    int nop = get(3, args[0]);
    
    for (int i = 1; i < args.length; i++) {
      // System.out.println("[" + args[i] + "]");
      Diff diff = new Diff(ins, del, rep, nop);
      String charset = System.getProperty("egothor.stemmer.charset", "UTF-8");
      try (LineNumberReader in = new LineNumberReader(Files.newBufferedReader(Paths.get(args[i]), Charset.forName(charset)))) {
        for (String line = in.readLine(); line != null; line = in.readLine()) {
          try {
            line = line.toLowerCase(Locale.ROOT);
            StringTokenizer st = new StringTokenizer(line);
            String stem = st.nextToken();
            System.out.println(stem + " -a");
            while (st.hasMoreTokens()) {
              String token = st.nextToken();
              if (token.equals(stem) == false) {
                System.out.println(stem + " " + diff.exec(token, stem));
              }
            }
          } catch (java.util.NoSuchElementException x) {
            // no base token (stem) on a line
          }
        }
      }
    }
  }
}
