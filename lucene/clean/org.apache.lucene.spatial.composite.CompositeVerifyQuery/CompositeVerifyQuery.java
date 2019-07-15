import org.apache.lucene.spatial.composite.*;


import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.spatial.util.ShapeValuesPredicate;

/**
 * A Query that considers an "indexQuery" to have approximate results, and a follow-on
 * ShapeValuesSource is called to verify each hit from {@link TwoPhaseIterator#matches()}.
 *
 * @lucene.experimental
 */
public class CompositeVerifyQuery extends Query {

  private final Query indexQuery;//approximation (matches more than needed)
  private final ShapeValuesPredicate predicateValueSource;

  public CompositeVerifyQuery(Query indexQuery, ShapeValuesPredicate predicateValueSource) {
    this.indexQuery = indexQuery;
    this.predicateValueSource = predicateValueSource;
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    final Query rewritten = indexQuery.rewrite(reader);
    if (rewritten != indexQuery) {
      return new CompositeVerifyQuery(rewritten, predicateValueSource);
    }
    return super.rewrite(reader);
  }

  @Override
  public boolean equals(Object other) {
    return sameClassAs(other) &&
           equalsTo(getClass().cast(other));
  }
  
  private boolean equalsTo(CompositeVerifyQuery other) {
    return indexQuery.equals(other.indexQuery) &&
           predicateValueSource.equals(other.predicateValueSource);
  }

  @Override
  public int hashCode() {
    int result = classHash();
    result = 31 * result + indexQuery.hashCode();
    result = 31 * result + predicateValueSource.hashCode();
    return result;
  }

  @Override
  public String toString(String field) {
    //TODO verify this looks good
    return getClass().getSimpleName() + "(" + indexQuery.toString(field) + ", " + predicateValueSource + ")";
  }

  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
    final Weight indexQueryWeight = indexQuery.createWeight(searcher, false, boost);//scores aren't unsupported

    return new ConstantScoreWeight(this, boost) {

      @Override
      public Scorer scorer(LeafReaderContext context) throws IOException {

        final Scorer indexQueryScorer = indexQueryWeight.scorer(context);
        if (indexQueryScorer == null) {
          return null;
        }

        final TwoPhaseIterator predFuncValues = predicateValueSource.iterator(context, indexQueryScorer.iterator());
        return new ConstantScoreScorer(this, score(), predFuncValues);
      }

      @Override
      public boolean isCacheable(LeafReaderContext ctx) {
        return predicateValueSource.isCacheable(ctx);
      }

    };
  }
}
