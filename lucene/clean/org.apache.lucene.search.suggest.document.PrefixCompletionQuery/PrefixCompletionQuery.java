import org.apache.lucene.search.suggest.document.*;


import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.suggest.BitsProducer;

/**
 * A {@link CompletionQuery} which takes an {@link Analyzer}
 * to analyze the prefix of the query term.
 * <p>
 * Example usage of querying an analyzed prefix 'sugg'
 * against a field 'suggest_field' is as follows:
 *
 * <pre class="prettyprint">
 *  CompletionQuery query = new PrefixCompletionQuery(analyzer, new Term("suggest_field", "sugg"));
 * </pre>
 * @lucene.experimental
 */
public class PrefixCompletionQuery extends CompletionQuery {
  /** Used to analyze the term text */
  protected final CompletionAnalyzer analyzer;

  /**
   * Calls {@link PrefixCompletionQuery#PrefixCompletionQuery(Analyzer, Term, BitsProducer)}
   * with no filter
   */
  public PrefixCompletionQuery(Analyzer analyzer, Term term) {
    this(analyzer, term, null);
  }

  /**
   * Constructs an analyzed prefix completion query
   *
   * @param analyzer used to analyze the provided {@link Term#text()}
   * @param term query is run against {@link Term#field()} and {@link Term#text()}
   *             is analyzed with <code>analyzer</code>
   * @param filter used to query on a sub set of documents
   */
  public PrefixCompletionQuery(Analyzer analyzer, Term term, BitsProducer filter) {
    super(term, filter);
    if (!(analyzer instanceof CompletionAnalyzer)) {
      this.analyzer = new CompletionAnalyzer(analyzer);
    } else {
      this.analyzer = (CompletionAnalyzer) analyzer;
    }
  }

  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
    try (CompletionTokenStream stream = (CompletionTokenStream) analyzer.tokenStream(getField(), getTerm().text())) {
      return new CompletionWeight(this, stream.toAutomaton());
    }
  }

  /**
   * Gets the analyzer used to analyze the prefix.
   */
  public Analyzer getAnalyzer() {
    return analyzer;
  }

  @Override
  public boolean equals(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }
}
