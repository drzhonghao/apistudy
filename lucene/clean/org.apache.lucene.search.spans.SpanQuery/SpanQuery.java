import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.*;



import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

/** Base class for span-based queries. */
public abstract class SpanQuery extends Query {

  /**
   * Returns the name of the field matched by this query.
   */
  public abstract String getField();

  @Override
  public abstract SpanWeight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException;

  /**
   * Build a map of terms to termcontexts, for use in constructing SpanWeights
   * @lucene.internal
   */
  public static Map<Term, TermContext> getTermContexts(SpanWeight... weights) {
    Map<Term, TermContext> terms = new TreeMap<>();
    for (SpanWeight w : weights) {
      w.extractTermContexts(terms);
    }
    return terms;
  }

  /**
   * Build a map of terms to termcontexts, for use in constructing SpanWeights
   * @lucene.internal
   */
  public static Map<Term, TermContext> getTermContexts(Collection<SpanWeight> weights) {
    Map<Term, TermContext> terms = new TreeMap<>();
    for (SpanWeight w : weights) {
      w.extractTermContexts(terms);
    }
    return terms;
  }
}
