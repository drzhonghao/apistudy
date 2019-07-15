import org.apache.lucene.benchmark.byTask.tasks.*;



import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.feeds.FacetSource;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetsConfig;

/**
 * Add a faceted document.
 * <p>
 * Config properties:
 * <ul>
 * <li><b>with.facets</b>=&lt;tells whether to actually add any facets to the
 * document| Default: true&gt; <br>
 * This config property allows to easily compare the performance of adding docs
 * with and without facets. Note that facets are created even when this is
 * false, just that they are not added to the document (nor to the taxonomy).
 * </ul>
 * <p>
 * See {@link AddDocTask} for general document parameters and configuration.
 * <p>
 * Makes use of the {@link FacetSource} in effect - see {@link PerfRunData} for
 * facet source settings.
 */
public class AddFacetedDocTask extends AddDocTask {

  private FacetsConfig config;
  
  public AddFacetedDocTask(PerfRunData runData) {
    super(runData);
  }

  @Override
  public void setup() throws Exception {
    super.setup();
    if (config == null) {
      boolean withFacets = getRunData().getConfig().get("with.facets", true);
      if (withFacets) {
        FacetSource facetsSource = getRunData().getFacetSource();
        config = new FacetsConfig();
        facetsSource.configure(config);
      }
    }
  }

  @Override
  protected String getLogMessage(int recsCount) {
    if (config == null) {
      return super.getLogMessage(recsCount);
    }
    return super.getLogMessage(recsCount)+ " with facets";
  }
  
  @Override
  public int doLogic() throws Exception {
    if (config != null) {
      List<FacetField> facets = new ArrayList<>();
      getRunData().getFacetSource().getNextFacets(facets);
      for(FacetField ff : facets) {
        doc.add(ff);
      }
      doc = config.build(getRunData().getTaxonomyWriter(), doc);
    }
    return super.doLogic();
  }
}
