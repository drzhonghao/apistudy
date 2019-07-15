import org.apache.lucene.search.spans.*;



import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

/**
 * Counterpart of {@link BoostQuery} for spans.
 */
public final class SpanBoostQuery extends SpanQuery {

  private final SpanQuery query;
  private final float boost;

  /** Sole constructor: wrap {@code query} in such a way that the produced
   *  scores will be boosted by {@code boost}. */
  public SpanBoostQuery(SpanQuery query, float boost) {
    this.query = Objects.requireNonNull(query);
    this.boost = boost;
  }

  /**
   * Return the wrapped {@link SpanQuery}.
   */
  public SpanQuery getQuery() {
    return query;
  }

  /**
   * Return the applied boost.
   */
  public float getBoost() {
    return boost;
  }

  @Override
  public boolean equals(Object other) {
    return sameClassAs(other) &&
           equalsTo(getClass().cast(other));
  }
  
  private boolean equalsTo(SpanBoostQuery other) {
    return query.equals(other.query) && 
           Float.floatToIntBits(boost) == Float.floatToIntBits(other.boost);
  }

  @Override
  public int hashCode() {
    int h = classHash();
    h = 31 * h + query.hashCode();
    h = 31 * h + Float.floatToIntBits(boost);
    return h;
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    if (boost == 1f) {
      return query;
    }

    final SpanQuery rewritten = (SpanQuery) query.rewrite(reader);
    if (query != rewritten) {
      return new SpanBoostQuery(rewritten, boost);
    }

    if (query.getClass() == SpanBoostQuery.class) {
      SpanBoostQuery in = (SpanBoostQuery) query;
      return new SpanBoostQuery(in.query, boost * in.boost);
    }

    return super.rewrite(reader);
  }

  @Override
  public String toString(String field) {
    StringBuilder builder = new StringBuilder();
    builder.append("(");
    builder.append(query.toString(field));
    builder.append(")^");
    builder.append(boost);
    return builder.toString();
  }

  @Override
  public String getField() {
    return query.getField();
  }

  @Override
  public SpanWeight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
    return query.createWeight(searcher, needsScores, SpanBoostQuery.this.boost * boost);
  }

}
