import org.apache.lucene.queries.function.*;


import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.FilterScorer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

/**
 * A query that wraps another query, and uses a DoubleValuesSource to
 * replace or modify the wrapped query's score
 *
 * If the DoubleValuesSource doesn't return a value for a particular document,
 * then that document will be given a score of 0.
 */
public final class FunctionScoreQuery extends Query {

  private final Query in;
  private final DoubleValuesSource source;

  /**
   * Create a new FunctionScoreQuery
   * @param in      the query to wrap
   * @param source  a source of scores
   */
  public FunctionScoreQuery(Query in, DoubleValuesSource source) {
    this.in = in;
    this.source = source;
  }

  /**
   * @return the wrapped Query
   */
  public Query getWrappedQuery() {
    return in;
  }

  /**
   * Returns a FunctionScoreQuery where the scores of a wrapped query are multiplied by
   * the value of a DoubleValuesSource.
   *
   * If the source has no value for a particular document, the score for that document
   * is preserved as-is.
   *
   * @param in    the query to boost
   * @param boost a {@link DoubleValuesSource} containing the boost values
   */
  public static FunctionScoreQuery boostByValue(Query in, DoubleValuesSource boost) {
    return new FunctionScoreQuery(in, new MultiplicativeBoostValuesSource(boost));
  }

  /**
   * Returns a FunctionScoreQuery where the scores of a wrapped query are multiplied by
   * a boost factor if the document being scored also matches a separate boosting query.
   *
   * Documents that do not match the boosting query have their scores preserved.
   *
   * This may be used to 'demote' documents that match the boosting query, by passing in
   * a boostValue between 0 and 1.
   *
   * @param in          the query to boost
   * @param boostMatch  the boosting query
   * @param boostValue  the amount to boost documents which match the boosting query
   */
  public static FunctionScoreQuery boostByQuery(Query in, Query boostMatch, float boostValue) {
    return new FunctionScoreQuery(in,
        new MultiplicativeBoostValuesSource(new QueryBoostValuesSource(DoubleValuesSource.fromQuery(boostMatch), boostValue)));
  }

  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
    Weight inner = in.createWeight(searcher, needsScores && source.needsScores(), 1f);
    if (needsScores == false)
      return inner;
    return new FunctionScoreWeight(this, inner, source.rewrite(searcher), boost);
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    Query rewritten = in.rewrite(reader);
    if (rewritten == in)
      return this;
    return new FunctionScoreQuery(rewritten, source);
  }

  @Override
  public String toString(String field) {
    return "FunctionScoreQuery(" + in.toString(field) + ", scored by " + source.toString() + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FunctionScoreQuery that = (FunctionScoreQuery) o;
    return Objects.equals(in, that.in) &&
        Objects.equals(source, that.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(in, source);
  }

  private static class FunctionScoreWeight extends Weight {

    final Weight inner;
    final DoubleValuesSource valueSource;
    final float boost;

    FunctionScoreWeight(Query query, Weight inner, DoubleValuesSource valueSource, float boost) {
      super(query);
      this.inner = inner;
      this.valueSource = valueSource;
      this.boost = boost;
    }

    @Override
    public void extractTerms(Set<Term> terms) {
      this.inner.extractTerms(terms);
    }

    @Override
    public Matches matches(LeafReaderContext context, int doc) throws IOException {
      return inner.matches(context, doc);
    }

    @Override
    public Explanation explain(LeafReaderContext context, int doc) throws IOException {
      Scorer scorer = inner.scorer(context);
      if (scorer.iterator().advance(doc) != doc)
        return Explanation.noMatch("No match");
      Explanation scoreExplanation = inner.explain(context, doc);
      Explanation expl = valueSource.explain(context, doc, scoreExplanation);
      return Explanation.match(expl.getValue() * boost, "product of:",
          Explanation.match(boost, "boost"), expl);
    }

    @Override
    public Scorer scorer(LeafReaderContext context) throws IOException {
      Scorer in = inner.scorer(context);
      if (in == null)
        return null;
      DoubleValues scores = valueSource.getValues(context, DoubleValuesSource.fromScorer(in));
      return new FilterScorer(in) {
        @Override
        public float score() throws IOException {
          if (scores.advanceExact(docID()))
            return (float) (scores.doubleValue() * boost);
          else
            return 0;
        }
      };
    }

    @Override
    public boolean isCacheable(LeafReaderContext ctx) {
      return inner.isCacheable(ctx) && valueSource.isCacheable(ctx);
    }

  }

  private static class MultiplicativeBoostValuesSource extends DoubleValuesSource {

    private final DoubleValuesSource boost;

    private MultiplicativeBoostValuesSource(DoubleValuesSource boost) {
      this.boost = boost;
    }

    @Override
    public DoubleValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
      DoubleValues in = DoubleValues.withDefault(boost.getValues(ctx, scores), 1);
      return new DoubleValues() {
        @Override
        public double doubleValue() throws IOException {
          return scores.doubleValue() * in.doubleValue();
        }

        @Override
        public boolean advanceExact(int doc) throws IOException {
          return in.advanceExact(doc);
        }
      };
    }

    @Override
    public boolean needsScores() {
      return true;
    }

    @Override
    public DoubleValuesSource rewrite(IndexSearcher reader) throws IOException {
      return new MultiplicativeBoostValuesSource(boost.rewrite(reader));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MultiplicativeBoostValuesSource that = (MultiplicativeBoostValuesSource) o;
      return Objects.equals(boost, that.boost);
    }

    @Override
    public Explanation explain(LeafReaderContext ctx, int docId, Explanation scoreExplanation) throws IOException {
      if (scoreExplanation.isMatch() == false) {
        return scoreExplanation;
      }
      Explanation boostExpl = boost.explain(ctx, docId, scoreExplanation);
      if (boostExpl.isMatch() == false) {
        return scoreExplanation;
      }
      return Explanation.match(scoreExplanation.getValue() * boostExpl.getValue(),
          "product of:", scoreExplanation, boostExpl);
    }

    @Override
    public int hashCode() {
      return Objects.hash(boost);
    }

    @Override
    public String toString() {
      return "boost(" + boost.toString() + ")";
    }

    @Override
    public boolean isCacheable(LeafReaderContext ctx) {
      return boost.isCacheable(ctx);
    }
  }

  private static class QueryBoostValuesSource extends DoubleValuesSource {

    private final DoubleValuesSource query;
    private final float boost;

    QueryBoostValuesSource(DoubleValuesSource query, float boost) {
      this.query = query;
      this.boost = boost;
    }

    @Override
    public DoubleValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
      DoubleValues in = query.getValues(ctx, null);
      return DoubleValues.withDefault(new DoubleValues() {
        @Override
        public double doubleValue() {
          return boost;
        }

        @Override
        public boolean advanceExact(int doc) throws IOException {
          return in.advanceExact(doc);
        }
      }, 1);
    }

    @Override
    public boolean needsScores() {
      return false;
    }

    @Override
    public DoubleValuesSource rewrite(IndexSearcher reader) throws IOException {
      return new QueryBoostValuesSource(query.rewrite(reader), boost);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      QueryBoostValuesSource that = (QueryBoostValuesSource) o;
      return Float.compare(that.boost, boost) == 0 &&
          Objects.equals(query, that.query);
    }

    @Override
    public int hashCode() {
      return Objects.hash(query, boost);
    }

    @Override
    public String toString() {
      return "queryboost(" + query + ")^" + boost;
    }

    @Override
    public boolean isCacheable(LeafReaderContext ctx) {
      return query.isCacheable(ctx);
    }

    @Override
    public Explanation explain(LeafReaderContext ctx, int docId, Explanation scoreExplanation) throws IOException {
      Explanation inner = query.explain(ctx, docId, scoreExplanation);
      if (inner.isMatch() == false) {
        return inner;
      }
      return Explanation.match(boost, "Matched boosting query " + query.toString());
    }
  }
}