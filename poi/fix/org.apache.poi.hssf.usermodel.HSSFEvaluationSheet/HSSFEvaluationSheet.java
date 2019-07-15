

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.util.Internal;


@Internal
final class HSSFEvaluationSheet implements EvaluationSheet {
	private final HSSFSheet _hs;

	private int _lastDefinedRow = -1;

	public HSSFEvaluationSheet(HSSFSheet hs) {
		_hs = hs;
		_lastDefinedRow = _hs.getLastRowNum();
	}

	public HSSFSheet getHSSFSheet() {
		return _hs;
	}

	@Override
	public int getLastRowNum() {
		return _lastDefinedRow;
	}

	@Override
	public EvaluationCell getCell(int rowIndex, int columnIndex) {
		HSSFRow row = _hs.getRow(rowIndex);
		if (row == null) {
			return null;
		}
		HSSFCell cell = row.getCell(columnIndex);
		if (cell == null) {
			return null;
		}
		return null;
	}

	@Override
	public void clearAllCachedResultValues() {
		_lastDefinedRow = _hs.getLastRowNum();
	}
}

