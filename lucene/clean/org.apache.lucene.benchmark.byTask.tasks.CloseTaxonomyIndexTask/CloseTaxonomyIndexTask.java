import org.apache.lucene.benchmark.byTask.tasks.*;



import java.io.IOException;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.util.IOUtils;

/**
 * Close taxonomy index.
 * <br>Other side effects: taxonomy writer object in perfRunData is nullified.
 */
public class CloseTaxonomyIndexTask extends PerfTask {

  public CloseTaxonomyIndexTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws IOException {
    IOUtils.close(getRunData().getTaxonomyWriter());
    getRunData().setTaxonomyWriter(null);

    return 1;
  }

}
