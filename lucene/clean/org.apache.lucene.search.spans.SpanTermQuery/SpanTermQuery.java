import org.apache.lucene.search.spans.*;



import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;

/** Matches spans containing a term.
 * This should not be used for terms that are indexed at position Integer.MAX_VALUE.
 */
public class SpanTermQuery extends SpanQuery {

  protected final Term term;
  protected final TermContext termContext;

  /** Construct a SpanTermQuery matching the named term's spans. */
  public SpanTermQuery(Term term) {
    this.term = Objects.requireNonNull(term);
    this.termContext = null;
  }

  /**
   * Expert: Construct a SpanTermQuery matching the named term's spans, using
   * the provided TermContext
   */
  public SpanTermQuery(Term term, TermContext context) {
    this.term = Objects.requireNonNull(term);
    this.termContext = context;
  }

  /** Return the term whose spans are matched. */
  public Term getTerm() { return term; }

  @Override
  public String getField() { return term.field(); }

  @Override
  public SpanWeight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
    final TermContext context;
    final IndexReaderContext topContext = searcher.getTopReaderContext();
    if (termContext == null || termContext.wasBuiltFor(topContext) == false) {
      context = TermContext.build(topContext, term);
    }
    else {
      context = termContext;
    }
    return new SpanTermWeight(context, searcher, needsScores ? Collections.singletonMap(term, context) : null, boost);
  }

  public class SpanTermWeight extends SpanWeight {

    final TermContext termContext;

    public SpanTermWeight(TermContext termContext, IndexSearcher searcher, Map<Term, TermContext> terms, float boost) throws IOException {
      super(SpanTermQuery.this, searcher, terms, boost);
      this.termContext = termContext;
      assert termContext != null : "TermContext must not be null";
    }

    @Override
    public void extractTerms(Set<Term> terms) {
      terms.add(term);
    }

    @Override
    public boolean isCacheable(LeafReaderContext ctx) {
      return true;
    }

    @Override
    public void extractTermContexts(Map<Term, TermContext> contexts) {
      contexts.put(term, termContext);
    }

    @Override
    public Spans getSpans(final LeafReaderContext context, Postings requiredPostings) throws IOException {

      assert termContext.wasBuiltFor(ReaderUtil.getTopLevelContext(context)) : "The top-reader used to create Weight is not the same as the current reader's top-reader (" + ReaderUtil.getTopLevelContext(context);

      final TermState state = termContext.get(context.ord);
      if (state == null) { // term is not present in that reader
        assert context.reader().docFreq(term) == 0 : "no termstate found but term exists in reader term=" + term;
        return null;
      }

      final Terms terms = context.reader().terms(term.field());
      if (terms == null)
        return null;
      if (terms.hasPositions() == false)
        throw new IllegalStateException("field \"" + term.field() + "\" was indexed without position data; cannot run SpanTermQuery (term=" + term.text() + ")");

      final TermsEnum termsEnum = terms.iterator();
      termsEnum.seekExact(term.bytes(), state);

      final PostingsEnum postings = termsEnum.postings(null, requiredPostings.getRequiredPostings());
      float positionsCost = termPositionsCost(termsEnum) * PHRASE_TO_SPAN_TERM_POSITIONS_COST;
      return new TermSpans(getSimScorer(context), postings, term, positionsCost);
    }
  }

  /** A guess of
   * the relative cost of dealing with the term positions
   * when using a SpanNearQuery instead of a PhraseQuery.
   */
  private static final float PHRASE_TO_SPAN_TERM_POSITIONS_COST = 4.0f;

  private static final int TERM_POSNS_SEEK_OPS_PER_DOC = 128;

  private static final int TERM_OPS_PER_POS = 7;

  /** Returns an expected cost in simple operations
   *  of processing the occurrences of a term
   *  in a document that contains the term.
   *  <br>This may be inaccurate when {@link TermsEnum#totalTermFreq()} is not available.
   *  @param termsEnum The term is the term at which this TermsEnum is positioned.
   *  <p>
   *  This is a copy of org.apache.lucene.search.PhraseQuery.termPositionsCost().
   *  <br>
   *  TODO: keep only a single copy of this method and the constants used in it
   *  when SpanTermQuery moves to the o.a.l.search package.
   */
  static float termPositionsCost(TermsEnum termsEnum) throws IOException {
    int docFreq = termsEnum.docFreq();
    assert docFreq > 0;
    long totalTermFreq = termsEnum.totalTermFreq(); // -1 when not available
    float expOccurrencesInMatchingDoc = (totalTermFreq < docFreq) ? 1 : (totalTermFreq / (float) docFreq);
    return TERM_POSNS_SEEK_OPS_PER_DOC + expOccurrencesInMatchingDoc * TERM_OPS_PER_POS;
  }

  @Override
  public String toString(String field) {
    StringBuilder buffer = new StringBuilder();
    if (term.field().equals(field))
      buffer.append(term.text());
    else
      buffer.append(term.toString());
    return buffer.toString();
  }

  @Override
  public int hashCode() {
    return classHash() ^ term.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return sameClassAs(other) &&
           term.equals(((SpanTermQuery) other).term);
  }

}
