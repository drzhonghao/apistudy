import org.apache.lucene.queries.function.valuesource.*;


import java.io.IOException;

import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;

/**
 * <code>MinFloatFunction</code> returns the min of its components.
 */
public class MinFloatFunction extends MultiFloatFunction {
  public MinFloatFunction(ValueSource[] sources) {
    super(sources);
  }

  @Override  
  protected String name() {
    return "min";
  }

  @Override
  protected float func(int doc, FunctionValues[] valsArr) throws IOException {
    if ( ! exists(doc, valsArr) ) return 0.0f;

    float val = Float.POSITIVE_INFINITY;
    for (FunctionValues vals : valsArr) {
      if (vals.exists(doc)) {
        val = Math.min(vals.floatVal(doc), val);
      }
    }
    return val;
  }
  
  /** 
   * True if <em>any</em> of the specified <code>values</code> 
   * {@link FunctionValues#exists} for the specified doc, else false.
   *
   * @see MultiFunction#anyExists
   */
  @Override
  protected boolean exists(int doc, FunctionValues[] valsArr) throws IOException {
    return MultiFunction.anyExists(doc, valsArr);
  }

}
