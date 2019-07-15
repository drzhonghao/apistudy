import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.*;



import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;

/**
 * A query that matches no documents.
 */

public class MatchNoDocsQuery extends Query {

  private final String reason;

  /** Default constructor */
  public MatchNoDocsQuery() {
    this("");
  }

  /** Provides a reason explaining why this query was used */
  public MatchNoDocsQuery(String reason) {
    this.reason = reason;
  }
  
  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
    return new Weight(this) {
      @Override
      public void extractTerms(Set<Term> terms) {
      }

      @Override
      public Explanation explain(LeafReaderContext context, int doc) throws IOException {
        return Explanation.noMatch(reason);
      }

      @Override
      public Scorer scorer(LeafReaderContext context) throws IOException {
        return null;
      }

      @Override
      public boolean isCacheable(LeafReaderContext ctx) {
        return true;
      }
    };
  }

  @Override
  public String toString(String field) {
    return "MatchNoDocsQuery(\"" + reason + "\")";
  }

  @Override
  public boolean equals(Object o) {
    return sameClassAs(o);
  }

  @Override
  public int hashCode() {
    return classHash();
  }
}
