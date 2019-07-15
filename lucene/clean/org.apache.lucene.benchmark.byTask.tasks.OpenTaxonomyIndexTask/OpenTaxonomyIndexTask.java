import org.apache.lucene.benchmark.byTask.tasks.*;



import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;

import java.io.IOException;


/**
 * Open a taxonomy index.
 * <br>Other side effects: taxonomy writer object in perfRunData is set.
 */
public class OpenTaxonomyIndexTask extends PerfTask {

  public OpenTaxonomyIndexTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws IOException {
    PerfRunData runData = getRunData();
    runData.setTaxonomyWriter(new DirectoryTaxonomyWriter(runData.getTaxonomyDir()));
    return 1;
  }

}
