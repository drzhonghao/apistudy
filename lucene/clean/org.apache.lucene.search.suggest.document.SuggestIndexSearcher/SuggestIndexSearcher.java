import org.apache.lucene.search.suggest.document.*;


import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Weight;

/**
 * Adds document suggest capabilities to IndexSearcher.
 * Any {@link CompletionQuery} can be used to suggest documents.
 *
 * Use {@link PrefixCompletionQuery} for analyzed prefix queries,
 * {@link RegexCompletionQuery} for regular expression prefix queries,
 * {@link FuzzyCompletionQuery} for analyzed prefix with typo tolerance
 * and {@link ContextQuery} to boost and/or filter suggestions by contexts
 *
 * @lucene.experimental
 */
public class SuggestIndexSearcher extends IndexSearcher {

  // NOTE: we do not accept an ExecutorService here, because at least the dedup
  // logic in TopSuggestDocsCollector/NRTSuggester would not be thread safe (and maybe other things)

  /**
   * Creates a searcher with document suggest capabilities
   * for <code>reader</code>.
   */
  public SuggestIndexSearcher(IndexReader reader) {
    super(reader);
  }

  /**
   * Returns top <code>n</code> completion hits for
   * <code>query</code>
   */
  public TopSuggestDocs suggest(CompletionQuery query, int n, boolean skipDuplicates) throws IOException {
    TopSuggestDocsCollector collector = new TopSuggestDocsCollector(n, skipDuplicates);
    suggest(query, collector);
    return collector.get();
  }

  /**
   * Lower-level suggest API.
   * Collects completion hits through <code>collector</code> for <code>query</code>.
   *
   * <p>{@link TopSuggestDocsCollector#collect(int, CharSequence, CharSequence, float)}
   * is called for every matching completion hit.
   */
  public void suggest(CompletionQuery query, TopSuggestDocsCollector collector) throws IOException {
    // TODO use IndexSearcher.rewrite instead
    // have to implement equals() and hashCode() in CompletionQuerys and co
    query = (CompletionQuery) query.rewrite(getIndexReader());
    Weight weight = query.createWeight(this, collector.needsScores(), 1f);
    for (LeafReaderContext context : getIndexReader().leaves()) {
      BulkScorer scorer = weight.bulkScorer(context);
      if (scorer != null) {
        try {
          scorer.score(collector.getLeafCollector(context), context.reader().getLiveDocs());
        } catch (CollectionTerminatedException e) {
          // collection was terminated prematurely
          // continue with the following leaf
        }
      }
    }
  }
}
