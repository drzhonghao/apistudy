import org.apache.lucene.queryparser.surround.query.SrndQuery;
import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.*;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;

abstract class RewriteQuery<SQ extends SrndQuery> extends Query {
  protected final SQ srndQuery;
  protected final String fieldName;
  protected final BasicQueryFactory qf;

  RewriteQuery(
      SQ srndQuery,
      String fieldName,
      BasicQueryFactory qf) {
    this.srndQuery = Objects.requireNonNull(srndQuery);
    this.fieldName = Objects.requireNonNull(fieldName);
    this.qf = Objects.requireNonNull(qf);
  }

  @Override
  abstract public Query rewrite(IndexReader reader) throws IOException;

  @Override
  public String toString(String field) {
    return getClass().getName()
    + (field.isEmpty() ? "" : "(unused: " + field + ")")
    + "(" + fieldName
    + ", " + srndQuery.toString()
    + ", " + qf.toString()
    + ")";
  }

  @Override
  public int hashCode() {
    return classHash()
      ^ fieldName.hashCode()
      ^ qf.hashCode()
      ^ srndQuery.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return sameClassAs(other) &&
           equalsTo(getClass().cast(other));
  }

  private boolean equalsTo(RewriteQuery<?> other) {
    return fieldName.equals(other.fieldName) && 
           qf.equals(other.qf) && 
           srndQuery.equals(other.srndQuery);
  }
}

