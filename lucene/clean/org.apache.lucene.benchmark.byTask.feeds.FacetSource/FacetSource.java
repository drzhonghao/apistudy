import org.apache.lucene.benchmark.byTask.feeds.ContentItemsSource;
import org.apache.lucene.benchmark.byTask.feeds.*;



import java.io.IOException;
import java.util.List;

import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetsConfig;

/**
 * Source items for facets.
 * <p>
 * For supported configuration parameters see {@link ContentItemsSource}.
 */
public abstract class FacetSource extends ContentItemsSource {

  /**
   * Fills the next facets content items in the given list. Implementations must
   * account for multi-threading, as multiple threads can call this method
   * simultaneously.
   */
  public abstract void getNextFacets(List<FacetField> facets) throws NoMoreDataException, IOException;

  public abstract void configure(FacetsConfig config);

  @Override
  public void resetInputs() throws IOException {
    printStatistics("facets");
    // re-initiate since properties by round may have changed.
    setConfig(getConfig());
    super.resetInputs();
  }
  
}
