

import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Fixed2ArgFunction;


public abstract class XYNumericFunction extends Fixed2ArgFunction {
	private static abstract class ValueArray {
		private final int _size;

		protected ValueArray(int size) {
			_size = size;
		}

		public ValueEval getItem(int index) {
			if ((index < 0) || (index > (_size))) {
				throw new IllegalArgumentException((((("Specified index " + index) + " is outside range (0..") + ((_size) - 1)) + ")"));
			}
			return getItemInternal(index);
		}

		protected abstract ValueEval getItemInternal(int index);

		public final int getSize() {
			return _size;
		}
	}

	private static final class SingleCellValueArray extends XYNumericFunction.ValueArray {
		private final ValueEval _value;

		public SingleCellValueArray(ValueEval value) {
			super(1);
			_value = value;
		}

		protected ValueEval getItemInternal(int index) {
			return _value;
		}
	}

	private static final class RefValueArray extends XYNumericFunction.ValueArray {
		private final RefEval _ref;

		private final int _width;

		public RefValueArray(RefEval ref) {
			super(ref.getNumberOfSheets());
			_ref = ref;
			_width = ref.getNumberOfSheets();
		}

		protected ValueEval getItemInternal(int index) {
			int sIx = (index % (_width)) + (_ref.getFirstSheetIndex());
			return _ref.getInnerValueEval(sIx);
		}
	}

	private static final class AreaValueArray extends XYNumericFunction.ValueArray {
		private final TwoDEval _ae;

		private final int _width;

		public AreaValueArray(TwoDEval ae) {
			super(((ae.getWidth()) * (ae.getHeight())));
			_ae = ae;
			_width = ae.getWidth();
		}

		protected ValueEval getItemInternal(int index) {
			int rowIx = index / (_width);
			int colIx = index % (_width);
			return _ae.getValue(rowIx, colIx);
		}
	}

	protected static interface Accumulator {
		public abstract double accumulate(double x, double y);
	}

	protected abstract XYNumericFunction.Accumulator createAccumulator();

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
		double result;
		result = 0.0;
		if ((Double.isNaN(result)) || (Double.isInfinite(result))) {
			return ErrorEval.NUM_ERROR;
		}
		return new NumberEval(result);
	}
}

