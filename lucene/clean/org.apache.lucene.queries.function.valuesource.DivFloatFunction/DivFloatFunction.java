import org.apache.lucene.queries.function.valuesource.*;


import java.io.IOException;

import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;

/** Function to divide "a" by "b"
 */
public class DivFloatFunction extends DualFloatFunction {
 /**
   * @param   a  the numerator.
   * @param   b  the denominator.
   */
  public DivFloatFunction(ValueSource a, ValueSource b) {
    super(a,b);
  }

  @Override
  protected String name() {
    return "div";
  }

  @Override
  protected float func(int doc, FunctionValues aVals, FunctionValues bVals) throws IOException {
    return aVals.floatVal(doc) / bVals.floatVal(doc);
  }
}
