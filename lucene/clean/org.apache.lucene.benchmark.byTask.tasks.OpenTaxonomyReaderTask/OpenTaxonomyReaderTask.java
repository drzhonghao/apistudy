import org.apache.lucene.benchmark.byTask.tasks.*;



import java.io.IOException;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;

/**
 * Open a taxonomy index reader.
 * <br>Other side effects: taxonomy reader object in perfRunData is set.
 */
public class OpenTaxonomyReaderTask extends PerfTask {

  public OpenTaxonomyReaderTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws IOException {
    PerfRunData runData = getRunData();
    DirectoryTaxonomyReader taxoReader = new DirectoryTaxonomyReader(runData.getTaxonomyDir());
    runData.setTaxonomyReader(taxoReader);
    // We transfer reference to the run data
    taxoReader.decRef();
    return 1;
  }
 
}
