import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.*;



import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.document.StringField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;

/**
 * A {@link Query} that matches documents that have a value for a given field
 * as reported by field norms.  This will not work for fields that omit norms,
 * e.g. {@link StringField}.
 */
public final class NormsFieldExistsQuery extends Query {

  private final String field;

  /** Create a query that will match that have a value for the given
   *  {@code field}. */
  public NormsFieldExistsQuery(String field) {
    this.field = Objects.requireNonNull(field);
  }

  public String getField() {
    return field;
  }

  @Override
  public boolean equals(Object other) {
    return sameClassAs(other) &&
           field.equals(((NormsFieldExistsQuery) other).field);
  }

  @Override
  public int hashCode() {
    return 31 * classHash() + field.hashCode();
  }

  @Override
  public String toString(String field) {
    return "NormsFieldExistsQuery [field=" + this.field + "]";
  }

  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
    return new ConstantScoreWeight(this, boost) {
      @Override
      public Scorer scorer(LeafReaderContext context) throws IOException {
        FieldInfos fieldInfos = context.reader().getFieldInfos();
        FieldInfo fieldInfo = fieldInfos.fieldInfo(field);
        if (fieldInfo == null || fieldInfo.hasNorms() == false) {
          return null;
        }
        LeafReader reader = context.reader();
        DocIdSetIterator iterator = reader.getNormValues(field);
        return new ConstantScoreScorer(this, score(), iterator);
      }

      @Override
      public boolean isCacheable(LeafReaderContext ctx) {
        return true;
      }
    };
  }
}
