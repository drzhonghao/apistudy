import org.apache.lucene.benchmark.byTask.tasks.*;


import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;

/**
 * Commits the Taxonomy Index.
 */
public class CommitTaxonomyIndexTask extends PerfTask {
  public CommitTaxonomyIndexTask(PerfRunData runData) {
    super(runData);
  }
  
  @Override
  public int doLogic() throws Exception {
    TaxonomyWriter taxonomyWriter = getRunData().getTaxonomyWriter();
    if (taxonomyWriter != null) {
      taxonomyWriter.commit();
    } else {
      throw new IllegalStateException("TaxonomyWriter is not currently open");
    }
    
    return 1;
  }
}
