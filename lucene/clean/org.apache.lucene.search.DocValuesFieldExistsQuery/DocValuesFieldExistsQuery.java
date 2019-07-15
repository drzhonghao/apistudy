import org.apache.lucene.search.*;



import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;

/**
 * A {@link Query} that matches documents that have a value for a given field
 * as reported by doc values iterators.
 */
public final class DocValuesFieldExistsQuery extends Query {

  private final String field;

  /** Create a query that will match that have a value for the given
   *  {@code field}. */
  public DocValuesFieldExistsQuery(String field) {
    this.field = Objects.requireNonNull(field);
  }

  public String getField() {
    return field;
  }

  @Override
  public boolean equals(Object other) {
    return sameClassAs(other) &&
           field.equals(((DocValuesFieldExistsQuery) other).field);
  }

  @Override
  public int hashCode() {
    return 31 * classHash() + field.hashCode();
  }

  @Override
  public String toString(String field) {
    return "DocValuesFieldExistsQuery [field=" + this.field + "]";
  }

  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) {
    return new ConstantScoreWeight(this, boost) {
      @Override
      public Scorer scorer(LeafReaderContext context) throws IOException {
        DocIdSetIterator iterator = getDocValuesDocIdSetIterator(field, context.reader());
        if (iterator == null) {
          return null;
        }
        return new ConstantScoreScorer(this, score(), iterator);
      }

      @Override
      public boolean isCacheable(LeafReaderContext ctx) {
        return DocValues.isCacheable(ctx, field);
      }

    };
  }

  /**
   * Returns a {@link DocIdSetIterator} from the given field or null if the field doesn't exist
   * in the reader or if the reader has no doc values for the field.
   */
  public static DocIdSetIterator getDocValuesDocIdSetIterator(String field, LeafReader reader) throws IOException {
    FieldInfo fieldInfo = reader.getFieldInfos().fieldInfo(field);
    final DocIdSetIterator iterator;
    if (fieldInfo != null) {
      switch (fieldInfo.getDocValuesType()) {
        case NONE:
          iterator = null;
          break;
        case NUMERIC:
          iterator = reader.getNumericDocValues(field);
          break;
        case BINARY:
          iterator = reader.getBinaryDocValues(field);
          break;
        case SORTED:
          iterator = reader.getSortedDocValues(field);
          break;
        case SORTED_NUMERIC:
          iterator = reader.getSortedNumericDocValues(field);
          break;
        case SORTED_SET:
          iterator = reader.getSortedSetDocValues(field);
          break;
        default:
          throw new AssertionError();
      }
      return iterator;
    }
    return null;
  }
}
