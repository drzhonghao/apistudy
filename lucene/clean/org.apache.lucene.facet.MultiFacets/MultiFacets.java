import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Maps specified dims to provided Facets impls; else, uses
 *  the default Facets impl. */
public class MultiFacets extends Facets {
  private final Map<String,Facets> dimToFacets;
  private final Facets defaultFacets;

  /** Create this, with no default {@link Facets}. */
  public MultiFacets(Map<String,Facets> dimToFacets) {
    this(dimToFacets, null);
  }

  /** Create this, with the specified default {@link Facets}
   *  for fields not included in {@code dimToFacets}. */
  public MultiFacets(Map<String,Facets> dimToFacets, Facets defaultFacets) {
    this.dimToFacets = dimToFacets;
    this.defaultFacets = defaultFacets;
  }

  @Override
  public FacetResult getTopChildren(int topN, String dim, String... path) throws IOException {
    Facets facets = dimToFacets.get(dim);
    if (facets == null) {
      if (defaultFacets == null) {
        throw new IllegalArgumentException("invalid dim \"" + dim + "\"");
      }
      facets = defaultFacets;
    }
    return facets.getTopChildren(topN, dim, path);
  }

  @Override
  public Number getSpecificValue(String dim, String... path) throws IOException {
    Facets facets = dimToFacets.get(dim);
    if (facets == null) {
      if (defaultFacets == null) {
        throw new IllegalArgumentException("invalid dim \"" + dim + "\"");
      }
      facets = defaultFacets;
    }
    return facets.getSpecificValue(dim, path);
  }

  @Override
  public List<FacetResult> getAllDims(int topN) throws IOException {

    List<FacetResult> results = new ArrayList<FacetResult>();

    // First add the specific dim's facets:
    for(Map.Entry<String,Facets> ent : dimToFacets.entrySet()) {
      results.add(ent.getValue().getTopChildren(topN, ent.getKey()));
    }

    if (defaultFacets != null) {

      // Then add all default facets as long as we didn't
      // already add that dim:
      for(FacetResult result : defaultFacets.getAllDims(topN)) {
        if (dimToFacets.containsKey(result.dim) == false) {
          results.add(result);
        }
      }
    }

    return results;
  }
}
