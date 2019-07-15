

import java.util.regex.Pattern;
import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;


final class LookupUtils {
	public interface ValueVector {
		ValueEval getItem(int index);

		int getSize();
	}

	private static final class RowVector implements LookupUtils.ValueVector {
		private final TwoDEval _tableArray;

		private final int _size;

		private final int _rowIndex;

		public RowVector(TwoDEval tableArray, int rowIndex) {
			_rowIndex = rowIndex;
			int lastRowIx = (tableArray.getHeight()) - 1;
			if ((rowIndex < 0) || (rowIndex > lastRowIx)) {
				throw new IllegalArgumentException((((("Specified row index (" + rowIndex) + ") is outside the allowed range (0..") + lastRowIx) + ")"));
			}
			_tableArray = tableArray;
			_size = tableArray.getWidth();
		}

		public ValueEval getItem(int index) {
			if (index > (_size)) {
				throw new ArrayIndexOutOfBoundsException((((("Specified index (" + index) + ") is outside the allowed range (0..") + ((_size) - 1)) + ")"));
			}
			return _tableArray.getValue(_rowIndex, index);
		}

		public int getSize() {
			return _size;
		}
	}

	private static final class ColumnVector implements LookupUtils.ValueVector {
		private final TwoDEval _tableArray;

		private final int _size;

		private final int _columnIndex;

		public ColumnVector(TwoDEval tableArray, int columnIndex) {
			_columnIndex = columnIndex;
			int lastColIx = (tableArray.getWidth()) - 1;
			if ((columnIndex < 0) || (columnIndex > lastColIx)) {
				throw new IllegalArgumentException((((("Specified column index (" + columnIndex) + ") is outside the allowed range (0..") + lastColIx) + ")"));
			}
			_tableArray = tableArray;
			_size = _tableArray.getHeight();
		}

		public ValueEval getItem(int index) {
			if (index > (_size)) {
				throw new ArrayIndexOutOfBoundsException((((("Specified index (" + index) + ") is outside the allowed range (0..") + ((_size) - 1)) + ")"));
			}
			return _tableArray.getValue(index, _columnIndex);
		}

		public int getSize() {
			return _size;
		}
	}

	private static final class SheetVector implements LookupUtils.ValueVector {
		private final RefEval _re;

		private final int _size;

		public SheetVector(RefEval re) {
			_size = re.getNumberOfSheets();
			_re = re;
		}

		public ValueEval getItem(int index) {
			if (index >= (_size)) {
				throw new ArrayIndexOutOfBoundsException((((("Specified index (" + index) + ") is outside the allowed range (0..") + ((_size) - 1)) + ")"));
			}
			int sheetIndex = (_re.getFirstSheetIndex()) + index;
			return _re.getInnerValueEval(sheetIndex);
		}

		public int getSize() {
			return _size;
		}
	}

	public static LookupUtils.ValueVector createRowVector(TwoDEval tableArray, int relativeRowIndex) {
		return new LookupUtils.RowVector(tableArray, relativeRowIndex);
	}

	public static LookupUtils.ValueVector createColumnVector(TwoDEval tableArray, int relativeColumnIndex) {
		return new LookupUtils.ColumnVector(tableArray, relativeColumnIndex);
	}

	public static LookupUtils.ValueVector createVector(TwoDEval ae) {
		if (ae.isColumn()) {
			return LookupUtils.createColumnVector(ae, 0);
		}
		if (ae.isRow()) {
			return LookupUtils.createRowVector(ae, 0);
		}
		return null;
	}

	public static LookupUtils.ValueVector createVector(RefEval re) {
		return new LookupUtils.SheetVector(re);
	}

	public static final class CompareResult {
		private final boolean _isTypeMismatch;

		private final boolean _isLessThan;

		private final boolean _isEqual;

		private final boolean _isGreaterThan;

		private CompareResult(boolean isTypeMismatch, int simpleCompareResult) {
			if (isTypeMismatch) {
				_isTypeMismatch = true;
				_isLessThan = false;
				_isEqual = false;
				_isGreaterThan = false;
			}else {
				_isTypeMismatch = false;
				_isLessThan = simpleCompareResult < 0;
				_isEqual = simpleCompareResult == 0;
				_isGreaterThan = simpleCompareResult > 0;
			}
		}

		public static final LookupUtils.CompareResult TYPE_MISMATCH = new LookupUtils.CompareResult(true, 0);

		public static final LookupUtils.CompareResult LESS_THAN = new LookupUtils.CompareResult(false, (-1));

		public static final LookupUtils.CompareResult EQUAL = new LookupUtils.CompareResult(false, 0);

		public static final LookupUtils.CompareResult GREATER_THAN = new LookupUtils.CompareResult(false, (+1));

		public static LookupUtils.CompareResult valueOf(int simpleCompareResult) {
			if (simpleCompareResult < 0) {
				return LookupUtils.CompareResult.LESS_THAN;
			}
			if (simpleCompareResult > 0) {
				return LookupUtils.CompareResult.GREATER_THAN;
			}
			return LookupUtils.CompareResult.EQUAL;
		}

		public static LookupUtils.CompareResult valueOf(boolean matches) {
			if (matches) {
				return LookupUtils.CompareResult.EQUAL;
			}
			return LookupUtils.CompareResult.LESS_THAN;
		}

		public boolean isTypeMismatch() {
			return _isTypeMismatch;
		}

		public boolean isLessThan() {
			return _isLessThan;
		}

		public boolean isEqual() {
			return _isEqual;
		}

		public boolean isGreaterThan() {
			return _isGreaterThan;
		}

		public String toString() {
			return (((getClass().getName()) + " [") + (formatAsString())) + "]";
		}

		private String formatAsString() {
			if (_isTypeMismatch) {
				return "TYPE_MISMATCH";
			}
			if (_isLessThan) {
				return "LESS_THAN";
			}
			if (_isEqual) {
				return "EQUAL";
			}
			if (_isGreaterThan) {
				return "GREATER_THAN";
			}
			return "??error??";
		}
	}

	public interface LookupValueComparer {
		LookupUtils.CompareResult compareTo(ValueEval other);
	}

	private static abstract class LookupValueComparerBase implements LookupUtils.LookupValueComparer {
		private final Class<? extends ValueEval> _targetClass;

		protected LookupValueComparerBase(ValueEval targetValue) {
			if (targetValue == null) {
				throw new RuntimeException("targetValue cannot be null");
			}
			_targetClass = targetValue.getClass();
		}

		public final LookupUtils.CompareResult compareTo(ValueEval other) {
			if (other == null) {
				throw new RuntimeException("compare to value cannot be null");
			}
			if ((_targetClass) != (other.getClass())) {
				return LookupUtils.CompareResult.TYPE_MISMATCH;
			}
			return compareSameType(other);
		}

		public String toString() {
			return (((getClass().getName()) + " [") + (getValueAsString())) + "]";
		}

		protected abstract LookupUtils.CompareResult compareSameType(ValueEval other);

		protected abstract String getValueAsString();
	}

	private static final class StringLookupComparer extends LookupUtils.LookupValueComparerBase {
		private String _value;

		private final Pattern _wildCardPattern;

		private boolean _matchExact;

		private boolean _isMatchFunction;

		protected StringLookupComparer(StringEval se, boolean matchExact, boolean isMatchFunction) {
			super(se);
			_value = se.getStringValue();
			_wildCardPattern = org.apache.poi.ss.formula.functions.Countif.StringMatcher.getWildCardPattern(_value);
			_matchExact = matchExact;
			_isMatchFunction = isMatchFunction;
		}

		protected LookupUtils.CompareResult compareSameType(ValueEval other) {
			StringEval se = ((StringEval) (other));
			String stringValue = se.getStringValue();
			if ((_wildCardPattern) != null) {
				java.util.regex.Matcher matcher = _wildCardPattern.matcher(stringValue);
				boolean matches = matcher.matches();
				if ((_isMatchFunction) || (!(_matchExact))) {
					return LookupUtils.CompareResult.valueOf(matches);
				}
			}
			return LookupUtils.CompareResult.valueOf(_value.compareToIgnoreCase(stringValue));
		}

		protected String getValueAsString() {
			return _value;
		}
	}

	private static final class NumberLookupComparer extends LookupUtils.LookupValueComparerBase {
		private double _value;

		protected NumberLookupComparer(org.apache.poi.ss.formula.eval.NumberEval ne) {
			super(ne);
			_value = ne.getNumberValue();
		}

		protected LookupUtils.CompareResult compareSameType(ValueEval other) {
			org.apache.poi.ss.formula.eval.NumberEval ne = ((org.apache.poi.ss.formula.eval.NumberEval) (other));
			return LookupUtils.CompareResult.valueOf(Double.compare(_value, ne.getNumberValue()));
		}

		protected String getValueAsString() {
			return String.valueOf(_value);
		}
	}

	private static final class BooleanLookupComparer extends LookupUtils.LookupValueComparerBase {
		private boolean _value;

		protected BooleanLookupComparer(org.apache.poi.ss.formula.eval.BoolEval be) {
			super(be);
			_value = be.getBooleanValue();
		}

		protected LookupUtils.CompareResult compareSameType(ValueEval other) {
			org.apache.poi.ss.formula.eval.BoolEval be = ((org.apache.poi.ss.formula.eval.BoolEval) (other));
			boolean otherVal = be.getBooleanValue();
			if ((_value) == otherVal) {
				return LookupUtils.CompareResult.EQUAL;
			}
			if (_value) {
				return LookupUtils.CompareResult.GREATER_THAN;
			}
			return LookupUtils.CompareResult.LESS_THAN;
		}

		protected String getValueAsString() {
			return String.valueOf(_value);
		}
	}

	public static int resolveRowOrColIndexArg(ValueEval rowColIndexArg, int srcCellRow, int srcCellCol) throws org.apache.poi.ss.formula.eval.EvaluationException {
		if (rowColIndexArg == null) {
			throw new IllegalArgumentException("argument must not be null");
		}
		ValueEval veRowColIndexArg;
		try {
			veRowColIndexArg = org.apache.poi.ss.formula.eval.OperandResolver.getSingleValue(rowColIndexArg, srcCellRow, ((short) (srcCellCol)));
		} catch (org.apache.poi.ss.formula.eval.EvaluationException e) {
			throw org.apache.poi.ss.formula.eval.EvaluationException.invalidRef();
		}
		int oneBasedIndex;
		if (veRowColIndexArg instanceof StringEval) {
			StringEval se = ((StringEval) (veRowColIndexArg));
			String strVal = se.getStringValue();
			Double dVal = org.apache.poi.ss.formula.eval.OperandResolver.parseDouble(strVal);
			if (dVal == null) {
				throw org.apache.poi.ss.formula.eval.EvaluationException.invalidRef();
			}
		}
		oneBasedIndex = org.apache.poi.ss.formula.eval.OperandResolver.coerceValueToInt(veRowColIndexArg);
		if (oneBasedIndex < 1) {
			throw org.apache.poi.ss.formula.eval.EvaluationException.invalidValue();
		}
		return oneBasedIndex - 1;
	}

	public static TwoDEval resolveTableArrayArg(ValueEval eval) throws org.apache.poi.ss.formula.eval.EvaluationException {
		if (eval instanceof TwoDEval) {
			return ((TwoDEval) (eval));
		}
		if (eval instanceof RefEval) {
			RefEval refEval = ((RefEval) (eval));
			return refEval.offset(0, 0, 0, 0);
		}
		throw org.apache.poi.ss.formula.eval.EvaluationException.invalidValue();
	}

	public static boolean resolveRangeLookupArg(ValueEval rangeLookupArg, int srcCellRow, int srcCellCol) throws org.apache.poi.ss.formula.eval.EvaluationException {
		ValueEval valEval = org.apache.poi.ss.formula.eval.OperandResolver.getSingleValue(rangeLookupArg, srcCellRow, srcCellCol);
		if (valEval instanceof org.apache.poi.ss.formula.eval.BlankEval) {
			return false;
		}
		if (valEval instanceof org.apache.poi.ss.formula.eval.BoolEval) {
			org.apache.poi.ss.formula.eval.BoolEval boolEval = ((org.apache.poi.ss.formula.eval.BoolEval) (valEval));
			return boolEval.getBooleanValue();
		}
		if (valEval instanceof StringEval) {
			String stringValue = ((StringEval) (valEval)).getStringValue();
			if ((stringValue.length()) < 1) {
				throw org.apache.poi.ss.formula.eval.EvaluationException.invalidValue();
			}
			throw org.apache.poi.ss.formula.eval.EvaluationException.invalidValue();
		}
		if (valEval instanceof org.apache.poi.ss.formula.eval.NumericValueEval) {
			org.apache.poi.ss.formula.eval.NumericValueEval nve = ((org.apache.poi.ss.formula.eval.NumericValueEval) (valEval));
			return 0.0 != (nve.getNumberValue());
		}
		throw new RuntimeException((("Unexpected eval type (" + valEval) + ")"));
	}

	public static int lookupIndexOfValue(ValueEval lookupValue, LookupUtils.ValueVector vector, boolean isRangeLookup) throws org.apache.poi.ss.formula.eval.EvaluationException {
		LookupUtils.LookupValueComparer lookupComparer = LookupUtils.createLookupComparer(lookupValue, isRangeLookup, false);
		int result;
		if (isRangeLookup) {
			result = LookupUtils.performBinarySearch(vector, lookupComparer);
		}else {
			result = LookupUtils.lookupIndexOfExactValue(lookupComparer, vector);
		}
		if (result < 0) {
			throw new org.apache.poi.ss.formula.eval.EvaluationException(org.apache.poi.ss.formula.eval.ErrorEval.NA);
		}
		return result;
	}

	private static int lookupIndexOfExactValue(LookupUtils.LookupValueComparer lookupComparer, LookupUtils.ValueVector vector) {
		int size = vector.getSize();
		for (int i = 0; i < size; i++) {
			if (lookupComparer.compareTo(vector.getItem(i)).isEqual()) {
				return i;
			}
		}
		return -1;
	}

	private static final class BinarySearchIndexes {
		private int _lowIx;

		private int _highIx;

		public BinarySearchIndexes(int highIx) {
			_lowIx = -1;
			_highIx = highIx;
		}

		public int getMidIx() {
			int ixDiff = (_highIx) - (_lowIx);
			if (ixDiff < 2) {
				return -1;
			}
			return (_lowIx) + (ixDiff / 2);
		}

		public int getLowIx() {
			return _lowIx;
		}

		public int getHighIx() {
			return _highIx;
		}

		public void narrowSearch(int midIx, boolean isLessThan) {
			if (isLessThan) {
				_highIx = midIx;
			}else {
				_lowIx = midIx;
			}
		}
	}

	private static int performBinarySearch(LookupUtils.ValueVector vector, LookupUtils.LookupValueComparer lookupComparer) {
		LookupUtils.BinarySearchIndexes bsi = new LookupUtils.BinarySearchIndexes(vector.getSize());
		while (true) {
			int midIx = bsi.getMidIx();
			if (midIx < 0) {
				return bsi.getLowIx();
			}
			LookupUtils.CompareResult cr = lookupComparer.compareTo(vector.getItem(midIx));
			if (cr.isTypeMismatch()) {
				int newMidIx = LookupUtils.handleMidValueTypeMismatch(lookupComparer, vector, bsi, midIx);
				if (newMidIx < 0) {
					continue;
				}
				midIx = newMidIx;
				cr = lookupComparer.compareTo(vector.getItem(midIx));
			}
			if (cr.isEqual()) {
				return LookupUtils.findLastIndexInRunOfEqualValues(lookupComparer, vector, midIx, bsi.getHighIx());
			}
			bsi.narrowSearch(midIx, cr.isLessThan());
		} 
	}

	private static int handleMidValueTypeMismatch(LookupUtils.LookupValueComparer lookupComparer, LookupUtils.ValueVector vector, LookupUtils.BinarySearchIndexes bsi, int midIx) {
		int newMid = midIx;
		int highIx = bsi.getHighIx();
		while (true) {
			newMid++;
			if (newMid == highIx) {
				bsi.narrowSearch(midIx, true);
				return -1;
			}
			LookupUtils.CompareResult cr = lookupComparer.compareTo(vector.getItem(newMid));
			if ((cr.isLessThan()) && (newMid == (highIx - 1))) {
				bsi.narrowSearch(midIx, true);
				return -1;
			}
			if (cr.isTypeMismatch()) {
				continue;
			}
			if (cr.isEqual()) {
				return newMid;
			}
			bsi.narrowSearch(newMid, cr.isLessThan());
			return -1;
		} 
	}

	private static int findLastIndexInRunOfEqualValues(LookupUtils.LookupValueComparer lookupComparer, LookupUtils.ValueVector vector, int firstFoundIndex, int maxIx) {
		for (int i = firstFoundIndex + 1; i < maxIx; i++) {
			if (!(lookupComparer.compareTo(vector.getItem(i)).isEqual())) {
				return i - 1;
			}
		}
		return maxIx - 1;
	}

	public static LookupUtils.LookupValueComparer createLookupComparer(ValueEval lookupValue, boolean matchExact, boolean isMatchFunction) {
		if (lookupValue == (org.apache.poi.ss.formula.eval.BlankEval.instance)) {
			return new LookupUtils.NumberLookupComparer(org.apache.poi.ss.formula.eval.NumberEval.ZERO);
		}
		if (lookupValue instanceof StringEval) {
			return new LookupUtils.StringLookupComparer(((StringEval) (lookupValue)), matchExact, isMatchFunction);
		}
		if (lookupValue instanceof org.apache.poi.ss.formula.eval.NumberEval) {
			return new LookupUtils.NumberLookupComparer(((org.apache.poi.ss.formula.eval.NumberEval) (lookupValue)));
		}
		if (lookupValue instanceof org.apache.poi.ss.formula.eval.BoolEval) {
			return new LookupUtils.BooleanLookupComparer(((org.apache.poi.ss.formula.eval.BoolEval) (lookupValue)));
		}
		throw new IllegalArgumentException((("Bad lookup value type (" + (lookupValue.getClass().getName())) + ")"));
	}
}

