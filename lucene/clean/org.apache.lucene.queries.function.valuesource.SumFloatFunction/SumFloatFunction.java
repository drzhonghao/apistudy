import org.apache.lucene.queries.function.valuesource.*;


import java.io.IOException;

import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;

/**
 * <code>SumFloatFunction</code> returns the sum of its components.
 */
public class SumFloatFunction extends MultiFloatFunction {
  public SumFloatFunction(ValueSource[] sources) {
    super(sources);
  }

  @Override  
  protected String name() {
    return "sum";
  }

  @Override
  protected float func(int doc, FunctionValues[] valsArr) throws IOException {
    float val = 0.0f;
    for (FunctionValues vals : valsArr) {
      val += vals.floatVal(doc);
    }
    return val;
  }
}
