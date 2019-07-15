import org.apache.lucene.benchmark.byTask.tasks.*;



import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.feeds.ContentSource;
import org.apache.lucene.benchmark.byTask.feeds.DocData;

/** Consumes a {@link org.apache.lucene.benchmark.byTask.feeds.ContentSource}. */
public class ConsumeContentSourceTask extends PerfTask {

  private final ContentSource source;
  private ThreadLocal<DocData> dd = new ThreadLocal<>();
  
  public ConsumeContentSourceTask(PerfRunData runData) {
    super(runData);
    source = runData.getContentSource();
  }

  @Override
  protected String getLogMessage(int recsCount) {
    return "read " + recsCount + " documents from the content source";
  }
  
  @Override
  public int doLogic() throws Exception {
    dd.set(source.getNextDocData(dd.get()));
    return 1;
  }

}
