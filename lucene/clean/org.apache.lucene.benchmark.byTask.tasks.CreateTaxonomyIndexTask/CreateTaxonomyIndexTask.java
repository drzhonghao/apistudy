import org.apache.lucene.benchmark.byTask.tasks.*;



import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;

import java.io.IOException;


/**
 * Create a taxonomy index.
 * <br>Other side effects: taxonomy writer object in perfRunData is set.
 */
public class CreateTaxonomyIndexTask extends PerfTask {

  public CreateTaxonomyIndexTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws IOException {
    PerfRunData runData = getRunData();
    runData.setTaxonomyWriter(new DirectoryTaxonomyWriter(runData.getTaxonomyDir(), OpenMode.CREATE));
    return 1;
  }

}
