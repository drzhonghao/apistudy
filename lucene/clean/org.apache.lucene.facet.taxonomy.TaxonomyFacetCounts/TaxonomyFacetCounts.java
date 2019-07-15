import org.apache.lucene.facet.taxonomy.*;


import java.io.IOException;
import java.util.List;

import org.apache.lucene.facet.FacetsCollector.MatchingDocs;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.IntsRef;

/** Reads from any {@link OrdinalsReader}; use {@link
 *  FastTaxonomyFacetCounts} if you are using the
 *  default encoding from {@link BinaryDocValues}.
 * 
 * @lucene.experimental */
public class TaxonomyFacetCounts extends IntTaxonomyFacets {
  private final OrdinalsReader ordinalsReader;

  /** Create {@code TaxonomyFacetCounts}, which also
   *  counts all facet labels.  Use this for a non-default
   *  {@link OrdinalsReader}; otherwise use {@link
   *  FastTaxonomyFacetCounts}. */
  public TaxonomyFacetCounts(OrdinalsReader ordinalsReader, TaxonomyReader taxoReader, FacetsConfig config, FacetsCollector fc) throws IOException {
    super(ordinalsReader.getIndexFieldName(), taxoReader, config, fc);
    this.ordinalsReader = ordinalsReader;
    count(fc.getMatchingDocs());
  }

  private final void count(List<MatchingDocs> matchingDocs) throws IOException {
    IntsRef scratch  = new IntsRef();
    for(MatchingDocs hits : matchingDocs) {
      OrdinalsReader.OrdinalsSegmentReader ords = ordinalsReader.getReader(hits.context);
      DocIdSetIterator docs = hits.bits.iterator();
      
      int doc;
      while ((doc = docs.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
        ords.get(doc, scratch);
        for(int i=0;i<scratch.length;i++) {
          increment(scratch.ints[scratch.offset+i]);
        }
      }
    }

    rollup();
  }
}
