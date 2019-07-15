

import org.apache.poi.hssf.record.ArrayRecord;
import org.apache.poi.hssf.record.CellRecord;
import org.apache.poi.hssf.record.CellValueRecordInterface;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.SharedFormulaRecord;
import org.apache.poi.hssf.record.SharedValueRecordBase;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.hssf.record.aggregates.RecordAggregate;
import org.apache.poi.hssf.record.aggregates.SharedValueManager;
import org.apache.poi.hssf.util.CellRangeAddress8Bit;
import org.apache.poi.ss.formula.Formula;
import org.apache.poi.ss.formula.ptg.ExpPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressBase;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.RecordFormatException;


public final class FormulaRecordAggregate extends RecordAggregate implements CellValueRecordInterface {
	private final FormulaRecord _formulaRecord;

	private SharedValueManager _sharedValueManager;

	private StringRecord _stringRecord;

	private SharedFormulaRecord _sharedFormulaRecord;

	public FormulaRecordAggregate(FormulaRecord formulaRec, StringRecord stringRec, SharedValueManager svm) {
		if (svm == null) {
			throw new IllegalArgumentException("sfm must not be null");
		}
		if (formulaRec.hasCachedResultString()) {
			if (stringRec == null) {
				throw new RecordFormatException("Formula record flag is set but String record was not found");
			}
			_stringRecord = stringRec;
		}else {
			_stringRecord = null;
		}
		_formulaRecord = formulaRec;
		_sharedValueManager = svm;
		if (formulaRec.isSharedFormula()) {
			CellReference firstCell = formulaRec.getFormula().getExpReference();
			if (firstCell == null) {
				FormulaRecordAggregate.handleMissingSharedFormulaRecord(formulaRec);
			}else {
			}
		}
	}

	private static void handleMissingSharedFormulaRecord(FormulaRecord formula) {
		Ptg firstToken = formula.getParsedExpression()[0];
		if (firstToken instanceof ExpPtg) {
			throw new RecordFormatException("SharedFormulaRecord not found for FormulaRecord with (isSharedFormula=true)");
		}
		formula.setSharedFormula(false);
	}

	public FormulaRecord getFormulaRecord() {
		return _formulaRecord;
	}

	public StringRecord getStringRecord() {
		return _stringRecord;
	}

	public short getXFIndex() {
		return _formulaRecord.getXFIndex();
	}

	public void setXFIndex(short xf) {
		_formulaRecord.setXFIndex(xf);
	}

	public void setColumn(short col) {
		_formulaRecord.setColumn(col);
	}

	public void setRow(int row) {
		_formulaRecord.setRow(row);
	}

	public short getColumn() {
		return _formulaRecord.getColumn();
	}

	public int getRow() {
		return _formulaRecord.getRow();
	}

	public String toString() {
		return _formulaRecord.toString();
	}

	public void visitContainedRecords(RecordAggregate.RecordVisitor rv) {
		rv.visitRecord(_formulaRecord);
		if ((_formulaRecord.hasCachedResultString()) && ((_stringRecord) != null)) {
			rv.visitRecord(_stringRecord);
		}
	}

	public String getStringValue() {
		if ((_stringRecord) == null) {
			return null;
		}
		return _stringRecord.getString();
	}

	public void setCachedStringResult(String value) {
		if ((_stringRecord) == null) {
			_stringRecord = new StringRecord();
		}
		_stringRecord.setString(value);
		if ((value.length()) < 1) {
			_formulaRecord.setCachedResultTypeEmptyString();
		}else {
			_formulaRecord.setCachedResultTypeString();
		}
	}

	public void setCachedBooleanResult(boolean value) {
		_stringRecord = null;
		_formulaRecord.setCachedResultBoolean(value);
	}

	public void setCachedErrorResult(int errorCode) {
		_stringRecord = null;
		_formulaRecord.setCachedResultErrorCode(errorCode);
	}

	public void setCachedErrorResult(FormulaError error) {
		setCachedErrorResult(error.getCode());
	}

	public void setCachedDoubleResult(double value) {
		_stringRecord = null;
		_formulaRecord.setValue(value);
	}

	public Ptg[] getFormulaTokens() {
		if ((_sharedFormulaRecord) != null) {
			return _sharedFormulaRecord.getFormulaTokens(_formulaRecord);
		}
		CellReference expRef = _formulaRecord.getFormula().getExpReference();
		if (expRef != null) {
			ArrayRecord arec = _sharedValueManager.getArrayRecord(expRef.getRow(), expRef.getCol());
			return arec.getFormulaTokens();
		}
		return _formulaRecord.getParsedExpression();
	}

	public void setParsedExpression(Ptg[] ptgs) {
		notifyFormulaChanging();
		_formulaRecord.setParsedExpression(ptgs);
	}

	public void unlinkSharedFormula() {
		SharedFormulaRecord sfr = _sharedFormulaRecord;
		if (sfr == null) {
			throw new IllegalStateException("Formula not linked to shared formula");
		}
		Ptg[] ptgs = sfr.getFormulaTokens(_formulaRecord);
		_formulaRecord.setParsedExpression(ptgs);
		_formulaRecord.setSharedFormula(false);
		_sharedFormulaRecord = null;
	}

	public void notifyFormulaChanging() {
		if ((_sharedFormulaRecord) != null) {
			_sharedValueManager.unlink(_sharedFormulaRecord);
		}
	}

	public boolean isPartOfArrayFormula() {
		if ((_sharedFormulaRecord) != null) {
			return false;
		}
		CellReference expRef = _formulaRecord.getFormula().getExpReference();
		ArrayRecord arec = (expRef == null) ? null : _sharedValueManager.getArrayRecord(expRef.getRow(), expRef.getCol());
		return arec != null;
	}

	public CellRangeAddress getArrayFormulaRange() {
		if ((_sharedFormulaRecord) != null) {
			throw new IllegalStateException("not an array formula cell.");
		}
		CellReference expRef = _formulaRecord.getFormula().getExpReference();
		if (expRef == null) {
			throw new IllegalStateException("not an array formula cell.");
		}
		ArrayRecord arec = _sharedValueManager.getArrayRecord(expRef.getRow(), expRef.getCol());
		if (arec == null) {
			throw new IllegalStateException(("ArrayRecord was not found for the locator " + (expRef.formatAsString())));
		}
		CellRangeAddress8Bit a = arec.getRange();
		return new CellRangeAddress(a.getFirstRow(), a.getLastRow(), a.getFirstColumn(), a.getLastColumn());
	}

	public void setArrayFormula(CellRangeAddress r, Ptg[] ptgs) {
		ArrayRecord arr = new ArrayRecord(Formula.create(ptgs), new CellRangeAddress8Bit(r.getFirstRow(), r.getLastRow(), r.getFirstColumn(), r.getLastColumn()));
		_sharedValueManager.addArrayRecord(arr);
	}

	public CellRangeAddress removeArrayFormula(int rowIndex, int columnIndex) {
		CellRangeAddress8Bit a = _sharedValueManager.removeArrayFormula(rowIndex, columnIndex);
		_formulaRecord.setParsedExpression(null);
		return new CellRangeAddress(a.getFirstRow(), a.getLastRow(), a.getFirstColumn(), a.getLastColumn());
	}
}

