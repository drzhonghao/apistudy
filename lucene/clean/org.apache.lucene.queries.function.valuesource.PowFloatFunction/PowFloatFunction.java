import org.apache.lucene.queries.function.valuesource.*;


import java.io.IOException;

import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;

/** Function to raise the base "a" to the power "b"
 */
public class PowFloatFunction extends DualFloatFunction {
 /**
   * @param   a  the base.
   * @param   b  the exponent.
   */
  public PowFloatFunction(ValueSource a, ValueSource b) {
    super(a,b);
  }

  @Override
  protected String name() {
    return "pow";
  }

  @Override
  protected float func(int doc, FunctionValues aVals, FunctionValues bVals) throws IOException {
    return (float)Math.pow(aVals.floatVal(doc), bVals.floatVal(doc));
  }
}


