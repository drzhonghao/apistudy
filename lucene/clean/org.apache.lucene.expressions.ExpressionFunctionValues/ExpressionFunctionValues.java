import org.apache.lucene.expressions.Expression;
import org.apache.lucene.expressions.*;


import java.io.IOException;

import org.apache.lucene.search.DoubleValues;

/** A {@link DoubleValues} which evaluates an expression */
class ExpressionFunctionValues extends DoubleValues {
  final Expression expression;
  final DoubleValues[] functionValues;
  
  ExpressionFunctionValues(Expression expression, DoubleValues[] functionValues) {
    if (expression == null) {
      throw new NullPointerException();
    }
    if (functionValues == null) {
      throw new NullPointerException();
    }
    this.expression = expression;
    this.functionValues = functionValues;
  }

  @Override
  public boolean advanceExact(int doc) throws IOException {
    for (DoubleValues v : functionValues) {
      v.advanceExact(doc);
    }
    return true;
  }
  
  @Override
  public double doubleValue() {
    return expression.evaluate(functionValues);
  }
}
