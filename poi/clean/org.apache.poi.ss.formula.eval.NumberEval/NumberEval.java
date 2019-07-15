import org.apache.poi.ss.formula.eval.*;


import org.apache.poi.ss.formula.ptg.IntPtg;
import org.apache.poi.ss.formula.ptg.NumberPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.util.NumberToTextConverter;

/**
 * @author Amol S. Deshmukh &lt; amolweb at ya hoo dot com &gt;
 *  
 */
public final class NumberEval implements NumericValueEval, StringValueEval {
    
    public static final NumberEval ZERO = new NumberEval(0);

    private final double _value;
    private String _stringValue;

    public NumberEval(Ptg ptg) {
        if (ptg == null) {
            throw new IllegalArgumentException("ptg must not be null");
        }
        if (ptg instanceof IntPtg) {
            _value = ((IntPtg) ptg).getValue();
        } else if (ptg instanceof NumberPtg) {
            _value = ((NumberPtg) ptg).getValue();
        } else {
            throw new IllegalArgumentException("bad argument type (" + ptg.getClass().getName() + ")");
        }
    }

    public NumberEval(double value) {
        _value = value;
    }

    public double getNumberValue() {
        return _value;
    }

    public String getStringValue() {
        if (_stringValue == null) {
            _stringValue = NumberToTextConverter.toText(_value);
        }
        return _stringValue;
    }
    public final String toString() {
        return getClass().getName() + " [" +
                getStringValue() +
                "]";
    }
}
