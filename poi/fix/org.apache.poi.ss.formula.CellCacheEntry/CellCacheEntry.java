

import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;


abstract class CellCacheEntry {
	public static final CellCacheEntry[] EMPTY_ARRAY = new CellCacheEntry[]{  };

	private ValueEval _value;

	protected CellCacheEntry() {
	}

	protected final void clearValue() {
		_value = null;
	}

	public final boolean updateValue(ValueEval value) {
		if (value == null) {
			throw new IllegalArgumentException("Did not expect to update to null");
		}
		boolean result = !(CellCacheEntry.areValuesEqual(_value, value));
		_value = value;
		return result;
	}

	public final ValueEval getValue() {
		return _value;
	}

	private static boolean areValuesEqual(ValueEval a, ValueEval b) {
		if (a == null) {
			return false;
		}
		Class<? extends ValueEval> cls = a.getClass();
		if (cls != (b.getClass())) {
			return false;
		}
		if (a == (BlankEval.instance)) {
			return b == a;
		}
		if (cls == (NumberEval.class)) {
			return (((NumberEval) (a)).getNumberValue()) == (((NumberEval) (b)).getNumberValue());
		}
		if (cls == (StringEval.class)) {
			return ((StringEval) (a)).getStringValue().equals(((StringEval) (b)).getStringValue());
		}
		if (cls == (BoolEval.class)) {
			return (((BoolEval) (a)).getBooleanValue()) == (((BoolEval) (b)).getBooleanValue());
		}
		if (cls == (ErrorEval.class)) {
			return (((ErrorEval) (a)).getErrorCode()) == (((ErrorEval) (b)).getErrorCode());
		}
		throw new IllegalStateException((("Unexpected value class (" + (cls.getName())) + ")"));
	}

	protected final void recurseClearCachedFormulaResults() {
	}
}

