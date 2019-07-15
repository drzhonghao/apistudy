

import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;


final class ForkedEvaluationCell implements EvaluationCell {
	private final EvaluationSheet _sheet = null;

	private final EvaluationCell _masterCell = null;

	private boolean _booleanValue;

	private CellType _cellType;

	private int _errorValue;

	private double _numberValue;

	private String _stringValue;

	@Override
	public Object getIdentityKey() {
		return _masterCell.getIdentityKey();
	}

	public void setValue(ValueEval value) {
		Class<? extends ValueEval> cls = value.getClass();
		if (cls == (NumberEval.class)) {
			_cellType = CellType.NUMERIC;
			_numberValue = ((NumberEval) (value)).getNumberValue();
			return;
		}
		if (cls == (StringEval.class)) {
			_cellType = CellType.STRING;
			_stringValue = ((StringEval) (value)).getStringValue();
			return;
		}
		if (cls == (BoolEval.class)) {
			_cellType = CellType.BOOLEAN;
			_booleanValue = ((BoolEval) (value)).getBooleanValue();
			return;
		}
		if (cls == (ErrorEval.class)) {
			_cellType = CellType.ERROR;
			_errorValue = ((ErrorEval) (value)).getErrorCode();
			return;
		}
		if (cls == (BlankEval.class)) {
			_cellType = CellType.BLANK;
			return;
		}
		throw new IllegalArgumentException((("Unexpected value class (" + (cls.getName())) + ")"));
	}

	public void copyValue(Cell destCell) {
		switch (_cellType) {
			case BLANK :
				destCell.setCellType(CellType.BLANK);
				return;
			case NUMERIC :
				destCell.setCellValue(_numberValue);
				return;
			case BOOLEAN :
				destCell.setCellValue(_booleanValue);
				return;
			case STRING :
				destCell.setCellValue(_stringValue);
				return;
			case ERROR :
				destCell.setCellErrorValue(((byte) (_errorValue)));
				return;
			default :
				throw new IllegalStateException((("Unexpected data type (" + (_cellType)) + ")"));
		}
	}

	private void checkCellType(CellType expectedCellType) {
		if ((_cellType) != expectedCellType) {
			throw new RuntimeException((("Wrong data type (" + (_cellType)) + ")"));
		}
	}

	@Override
	public CellType getCellType() {
		return _cellType;
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2")
	@Override
	public CellType getCellTypeEnum() {
		return getCellType();
	}

	@Override
	public boolean getBooleanCellValue() {
		checkCellType(CellType.BOOLEAN);
		return _booleanValue;
	}

	@Override
	public int getErrorCellValue() {
		checkCellType(CellType.ERROR);
		return _errorValue;
	}

	@Override
	public double getNumericCellValue() {
		checkCellType(CellType.NUMERIC);
		return _numberValue;
	}

	@Override
	public String getStringCellValue() {
		checkCellType(CellType.STRING);
		return _stringValue;
	}

	@Override
	public EvaluationSheet getSheet() {
		return _sheet;
	}

	@Override
	public int getRowIndex() {
		return _masterCell.getRowIndex();
	}

	@Override
	public int getColumnIndex() {
		return _masterCell.getColumnIndex();
	}

	@Override
	public CellRangeAddress getArrayFormulaRange() {
		return _masterCell.getArrayFormulaRange();
	}

	@Override
	public boolean isPartOfArrayFormulaGroup() {
		return _masterCell.isPartOfArrayFormulaGroup();
	}

	@Override
	public CellType getCachedFormulaResultType() {
		return _masterCell.getCachedFormulaResultType();
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2")
	@Override
	public CellType getCachedFormulaResultTypeEnum() {
		return getCachedFormulaResultType();
	}
}

