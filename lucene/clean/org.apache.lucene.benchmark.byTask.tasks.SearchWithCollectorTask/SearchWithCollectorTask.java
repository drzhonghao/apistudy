import org.apache.lucene.benchmark.byTask.tasks.*;


import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.feeds.QueryMaker;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.TopScoreDocCollector;

/**
 * Does search w/ a custom collector
 */
public class SearchWithCollectorTask extends SearchTask {

  protected String clnName;

  public SearchWithCollectorTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public void setup() throws Exception {
    super.setup();
    //check to make sure either the doc is being stored
    PerfRunData runData = getRunData();
    Config config = runData.getConfig();
    clnName = config.get("collector.class", "");
  }

  

  @Override
  public boolean withCollector() {
    return true;
  }

  @Override
  protected Collector createCollector() throws Exception {
    Collector collector = null;
    if (clnName.equalsIgnoreCase("topScoreDoc") == true) {
      collector = TopScoreDocCollector.create(numHits());
    } else if (clnName.length() > 0){
      collector = Class.forName(clnName).asSubclass(Collector.class).newInstance();

    } else {
      collector = super.createCollector();
    }
    return collector;
  }

  @Override
  public QueryMaker getQueryMaker() {
    return getRunData().getQueryMaker(this);
  }

  @Override
  public boolean withRetrieve() {
    return false;
  }

  @Override
  public boolean withSearch() {
    return true;
  }

  @Override
  public boolean withTraverse() {
    return false;
  }

  @Override
  public boolean withWarm() {
    return false;
  }

}
