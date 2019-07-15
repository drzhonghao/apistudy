import org.apache.lucene.benchmark.byTask.tasks.*;



import java.io.IOException;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;

/**
 * Close taxonomy reader.
 * <br>Other side effects: taxonomy reader in perfRunData is nullified.
 */
public class CloseTaxonomyReaderTask extends PerfTask {

  public CloseTaxonomyReaderTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public int doLogic() throws IOException {
    try (TaxonomyReader taxoReader = getRunData().getTaxonomyReader()) {
      getRunData().setTaxonomyReader(null);
      if (taxoReader.getRefCount() != 1) {
        System.out.println("WARNING: CloseTaxonomyReader: reference count is currently " + taxoReader.getRefCount());
      }
    }
    return 1;
  }

}
