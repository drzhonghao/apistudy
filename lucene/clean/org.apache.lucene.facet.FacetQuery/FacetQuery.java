import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.*;


import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * A term {@link Query} over a {@link FacetField}.
 * <p>
 * <b>NOTE:</b>This helper class is an alternative to {@link DrillDownQuery}
 * especially in cases where you don't intend to use {@link DrillSideways}
 *
 * @lucene.experimental
 */
public class FacetQuery extends TermQuery {

  /**
   * Creates a new {@code FacetQuery} filtering the query on the given dimension.
   */
  public FacetQuery(final FacetsConfig facetsConfig, final String dimension, final String... path) {
    super(toTerm(facetsConfig.getDimConfig(dimension), dimension, path));
  }

  /**
   * Creates a new {@code FacetQuery} filtering the query on the given dimension.
   * <p>
   * <b>NOTE:</b>Uses FacetsConfig.DEFAULT_DIM_CONFIG.
   */
  public FacetQuery(final String dimension, final String... path) {
    super(toTerm(FacetsConfig.DEFAULT_DIM_CONFIG, dimension, path));
  }

  static Term toTerm(final FacetsConfig.DimConfig dimConfig, final String dimension, final String... path) {
    return new Term(dimConfig.indexFieldName, FacetsConfig.pathToString(dimension, path));
  }
}
