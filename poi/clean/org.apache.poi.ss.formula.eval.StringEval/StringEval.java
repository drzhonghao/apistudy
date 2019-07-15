import org.apache.poi.ss.formula.eval.StringValueEval;
import org.apache.poi.ss.formula.eval.*;


//import org.checkerframework.checker.nullness.qual.NonNull;

import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.StringPtg;

/**
 * @author Amol S. Deshmukh &lt; amolweb at ya hoo dot com &gt;
 */
public final class StringEval implements StringValueEval {

	public static final StringEval EMPTY_INSTANCE = new StringEval("");

	//@NotNull
	private final String _value;

	public StringEval(Ptg ptg) {
		this(((StringPtg) ptg).getValue());
	}

	public StringEval(String value) {
		if (value == null) {
			throw new IllegalArgumentException("value must not be null");
		}
		_value = value;
	}

	public String getStringValue() {
		return _value;
	}

	public String toString() {
		return getClass().getName() + " [" +
				_value +
				"]";
	}
}
