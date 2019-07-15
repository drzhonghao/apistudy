import org.apache.lucene.expressions.Expression;
import org.apache.lucene.expressions.Bindings;
import org.apache.lucene.expressions.*;



import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Rescorer;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortRescorer;

/**
 * A {@link Rescorer} that uses an expression to re-score
 * first pass hits.  Functionally this is the same as {@link
 * SortRescorer} (if you build the {@link Sort} using {@link
 * Expression#getSortField}), except for the explain method
 * which gives more detail by showing the value of each
 * variable.
 * 
 * @lucene.experimental
 */

class ExpressionRescorer extends SortRescorer {

  private final Expression expression;
  private final Bindings bindings;

  /** Uses the provided {@link Expression} to assign second
   *  pass scores. */
  public ExpressionRescorer(Expression expression, Bindings bindings) {
    super(new Sort(expression.getSortField(bindings, true)));
    this.expression = expression;
    this.bindings = bindings;
  }

  private static DoubleValues scores(int doc, float score) {
    return new DoubleValues() {
      @Override
      public double doubleValue() throws IOException {
        return score;
      }

      @Override
      public boolean advanceExact(int target) throws IOException {
        assert doc == target;
        return true;
      }
    };
  }

  @Override
  public Explanation explain(IndexSearcher searcher, Explanation firstPassExplanation, int docID) throws IOException {
    Explanation superExpl = super.explain(searcher, firstPassExplanation, docID);

    List<LeafReaderContext> leaves = searcher.getIndexReader().leaves();
    int subReader = ReaderUtil.subIndex(docID, leaves);
    LeafReaderContext readerContext = leaves.get(subReader);
    int docIDInSegment = docID - readerContext.docBase;

    return expression.getDoubleValuesSource(bindings).explain(readerContext, docIDInSegment, superExpl);
  }
}
