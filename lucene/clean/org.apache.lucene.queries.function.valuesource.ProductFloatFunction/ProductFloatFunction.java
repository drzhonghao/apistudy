import org.apache.lucene.queries.function.valuesource.*;


import java.io.IOException;

import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;

/**
 * <code>ProductFloatFunction</code> returns the product of its components.
 */
public class ProductFloatFunction extends MultiFloatFunction {
  public ProductFloatFunction(ValueSource[] sources) {
    super(sources);
  }

  @Override
  protected String name() {
    return "product";
  }

  @Override
  protected float func(int doc, FunctionValues[] valsArr) throws IOException {
    float val = 1.0f;
    for (FunctionValues vals : valsArr) {
      val *= vals.floatVal(doc);
    }
    return val;
  }
}
