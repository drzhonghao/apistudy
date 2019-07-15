

import java.util.HashMap;
import java.util.Map;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.util.Internal;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;


@Internal
final class XSSFEvaluationSheet implements EvaluationSheet {
	private final XSSFSheet _xs;

	private Map<XSSFEvaluationSheet.CellKey, EvaluationCell> _cellCache;

	private int _lastDefinedRow = -1;

	public XSSFEvaluationSheet(XSSFSheet sheet) {
		_xs = sheet;
		_lastDefinedRow = _xs.getLastRowNum();
	}

	public XSSFSheet getXSSFSheet() {
		return _xs;
	}

	@Override
	public int getLastRowNum() {
		return _lastDefinedRow;
	}

	@Override
	public void clearAllCachedResultValues() {
		_cellCache = null;
		_lastDefinedRow = _xs.getLastRowNum();
	}

	@Override
	public EvaluationCell getCell(int rowIndex, int columnIndex) {
		if (rowIndex > (_lastDefinedRow))
			return null;

		if ((_cellCache) == null) {
			_cellCache = new HashMap<>(((_xs.getLastRowNum()) * 3));
			for (final Row row : _xs) {
				final int rowNum = row.getRowNum();
				for (final Cell cell : row) {
					final XSSFEvaluationSheet.CellKey key = new XSSFEvaluationSheet.CellKey(rowNum, cell.getColumnIndex());
				}
			}
		}
		final XSSFEvaluationSheet.CellKey key = new XSSFEvaluationSheet.CellKey(rowIndex, columnIndex);
		EvaluationCell evalcell = _cellCache.get(key);
		if (evalcell == null) {
			XSSFRow row = _xs.getRow(rowIndex);
			if (row == null) {
				return null;
			}
			XSSFCell cell = row.getCell(columnIndex);
			if (cell == null) {
				return null;
			}
			_cellCache.put(key, evalcell);
		}
		return evalcell;
	}

	private static class CellKey {
		private final int _row;

		private final int _col;

		private int _hash = -1;

		protected CellKey(int row, int col) {
			_row = row;
			_col = col;
		}

		@Override
		public int hashCode() {
			if ((_hash) == (-1)) {
				_hash = (((17 * 37) + (_row)) * 37) + (_col);
			}
			return _hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof XSSFEvaluationSheet.CellKey)) {
				return false;
			}
			final XSSFEvaluationSheet.CellKey oKey = ((XSSFEvaluationSheet.CellKey) (obj));
			return ((_row) == (oKey._row)) && ((_col) == (oKey._col));
		}
	}
}

