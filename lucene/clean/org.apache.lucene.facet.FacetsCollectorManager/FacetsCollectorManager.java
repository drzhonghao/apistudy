import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.*;


import org.apache.lucene.search.CollectorManager;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * A {@link CollectorManager} implementation which produces FacetsCollector and produces a merged FacetsCollector.
 * This is used for concurrent FacetsCollection.
 */
public class FacetsCollectorManager implements CollectorManager<FacetsCollector, FacetsCollector> {

  /** Sole constructor. */
  public FacetsCollectorManager() {
  }

  @Override
  public FacetsCollector newCollector() throws IOException {
    return new FacetsCollector();
  }

  @Override
  public FacetsCollector reduce(Collection<FacetsCollector> collectors) throws IOException {
    if (collectors == null || collectors.size() == 0) {
      return new FacetsCollector();
    } if (collectors.size() == 1) {
      return collectors.iterator().next();
    }
    return new ReducedFacetsCollector(collectors);
  }

  private static class ReducedFacetsCollector extends FacetsCollector {

    public ReducedFacetsCollector(final Collection<FacetsCollector> facetsCollectors) {
      final List<MatchingDocs> matchingDocs = this.getMatchingDocs();
      facetsCollectors.forEach(facetsCollector -> matchingDocs.addAll(facetsCollector.getMatchingDocs()));
    }
  }
}
