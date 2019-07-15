import org.apache.lucene.benchmark.byTask.tasks.WriteLineDocTask;
import org.apache.lucene.benchmark.byTask.tasks.*;


import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.feeds.DocMaker;
import org.apache.lucene.benchmark.byTask.utils.StreamUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;


/**
 * A {@link WriteLineDocTask} which for Wikipedia input, will write category pages 
 * to  another file, while remaining pages will be written to the original file.
 * The categories file is derived from the original file, by adding a prefix "categories-". 
 */
public class WriteEnwikiLineDocTask extends WriteLineDocTask {

  private final PrintWriter categoryLineFileOut;

  public WriteEnwikiLineDocTask(PerfRunData runData) throws Exception {
    super(runData);
    OutputStream out = StreamUtils.outputStream(categoriesLineFile(Paths.get(fname)));
    categoryLineFileOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), StreamUtils.BUFFER_SIZE));
    writeHeader(categoryLineFileOut);
  }

  /** Compose categories line file out of original line file */
  public static Path categoriesLineFile(Path f) {
    Path dir = f.toAbsolutePath().getParent();
    String categoriesName = "categories-"+f.getFileName();
    return dir.resolve(categoriesName);
  }
  
  @Override
  public void close() throws Exception {
    categoryLineFileOut.close();
    super.close();
  }
  
  @Override
  protected PrintWriter lineFileOut(Document doc) {
    IndexableField titleField = doc.getField(DocMaker.TITLE_FIELD);
    if (titleField!=null && titleField.stringValue().startsWith("Category:")) {
      return categoryLineFileOut;
    }
    return super.lineFileOut(doc);
  }
  
}
