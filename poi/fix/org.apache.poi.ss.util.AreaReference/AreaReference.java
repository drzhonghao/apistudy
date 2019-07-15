

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.util.CellReference;


public class AreaReference {
	private static final char SHEET_NAME_DELIMITER = '!';

	private static final char CELL_DELIMITER = ':';

	private static final char SPECIAL_NAME_DELIMITER = '\'';

	private static final SpreadsheetVersion DEFAULT_SPREADSHEET_VERSION = SpreadsheetVersion.EXCEL97;

	private final CellReference _firstCell;

	private final CellReference _lastCell;

	private final boolean _isSingleCell;

	private final SpreadsheetVersion _version;

	public AreaReference(String reference, SpreadsheetVersion version) {
		_version = (null != version) ? version : AreaReference.DEFAULT_SPREADSHEET_VERSION;
		if (!(AreaReference.isContiguous(reference))) {
			throw new IllegalArgumentException(("References passed to the AreaReference must be contiguous, " + "use generateContiguous(ref) if you have non-contiguous references"));
		}
		String[] parts = AreaReference.separateAreaRefs(reference);
		String part0 = parts[0];
		if ((parts.length) == 1) {
			_firstCell = new CellReference(part0);
			_lastCell = _firstCell;
			_isSingleCell = true;
			return;
		}
		if ((parts.length) != 2) {
			throw new IllegalArgumentException((("Bad area ref '" + reference) + "'"));
		}
		String part1 = parts[1];
		if (AreaReference.isPlainColumn(part0)) {
			if (!(AreaReference.isPlainColumn(part1))) {
				throw new RuntimeException((("Bad area ref '" + reference) + "'"));
			}
			boolean firstIsAbs = CellReference.isPartAbsolute(part0);
			boolean lastIsAbs = CellReference.isPartAbsolute(part1);
			int col0 = CellReference.convertColStringToIndex(part0);
			int col1 = CellReference.convertColStringToIndex(part1);
			_firstCell = new CellReference(0, col0, true, firstIsAbs);
			_lastCell = new CellReference(65535, col1, true, lastIsAbs);
			_isSingleCell = false;
		}else {
			_firstCell = new CellReference(part0);
			_lastCell = new CellReference(part1);
			_isSingleCell = part0.equals(part1);
		}
	}

	private static boolean isPlainColumn(String refPart) {
		for (int i = (refPart.length()) - 1; i >= 0; i--) {
			int ch = refPart.charAt(i);
			if ((ch == '$') && (i == 0)) {
				continue;
			}
			if ((ch < 'A') || (ch > 'Z')) {
				return false;
			}
		}
		return true;
	}

	public AreaReference(CellReference topLeft, CellReference botRight, SpreadsheetVersion version) {
		_version = (null != version) ? version : AreaReference.DEFAULT_SPREADSHEET_VERSION;
		boolean swapRows = (topLeft.getRow()) > (botRight.getRow());
		boolean swapCols = (topLeft.getCol()) > (botRight.getCol());
		if (swapRows || swapCols) {
			int firstRow;
			int lastRow;
			int firstColumn;
			int lastColumn;
			boolean firstRowAbs;
			boolean lastRowAbs;
			boolean firstColAbs;
			boolean lastColAbs;
			if (swapRows) {
				firstRow = botRight.getRow();
				firstRowAbs = botRight.isRowAbsolute();
				lastRow = topLeft.getRow();
				lastRowAbs = topLeft.isRowAbsolute();
			}else {
				firstRow = topLeft.getRow();
				firstRowAbs = topLeft.isRowAbsolute();
				lastRow = botRight.getRow();
				lastRowAbs = botRight.isRowAbsolute();
			}
			if (swapCols) {
				firstColumn = botRight.getCol();
				firstColAbs = botRight.isColAbsolute();
				lastColumn = topLeft.getCol();
				lastColAbs = topLeft.isColAbsolute();
			}else {
				firstColumn = topLeft.getCol();
				firstColAbs = topLeft.isColAbsolute();
				lastColumn = botRight.getCol();
				lastColAbs = botRight.isColAbsolute();
			}
			_firstCell = new CellReference(firstRow, firstColumn, firstRowAbs, firstColAbs);
			_lastCell = new CellReference(lastRow, lastColumn, lastRowAbs, lastColAbs);
		}else {
			_firstCell = topLeft;
			_lastCell = botRight;
		}
		_isSingleCell = false;
	}

	public static boolean isContiguous(String reference) {
		int sheetRefEnd = reference.indexOf('!');
		if (sheetRefEnd != (-1)) {
			reference = reference.substring(sheetRefEnd);
		}
		return !(reference.contains(","));
	}

	public static AreaReference getWholeRow(SpreadsheetVersion version, String start, String end) {
		if (null == version) {
			version = AreaReference.DEFAULT_SPREADSHEET_VERSION;
		}
		return new AreaReference((((("$A" + start) + ":$") + (version.getLastColumnName())) + end), version);
	}

	public static AreaReference getWholeColumn(SpreadsheetVersion version, String start, String end) {
		if (null == version) {
			version = AreaReference.DEFAULT_SPREADSHEET_VERSION;
		}
		return new AreaReference(((((start + "$1:") + end) + "$") + (version.getMaxRows())), version);
	}

	public static boolean isWholeColumnReference(SpreadsheetVersion version, CellReference topLeft, CellReference botRight) {
		if (null == version) {
			version = AreaReference.DEFAULT_SPREADSHEET_VERSION;
		}
		return ((((topLeft.getRow()) == 0) && (topLeft.isRowAbsolute())) && ((botRight.getRow()) == (version.getLastRowIndex()))) && (botRight.isRowAbsolute());
	}

	public boolean isWholeColumnReference() {
		return AreaReference.isWholeColumnReference(_version, _firstCell, _lastCell);
	}

	public static AreaReference[] generateContiguous(SpreadsheetVersion version, String reference) {
		if (null == version) {
			version = AreaReference.DEFAULT_SPREADSHEET_VERSION;
		}
		List<AreaReference> refs = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(reference, ",");
		while (st.hasMoreTokens()) {
			refs.add(new AreaReference(st.nextToken(), version));
		} 
		return refs.toArray(new AreaReference[refs.size()]);
	}

	public boolean isSingleCell() {
		return _isSingleCell;
	}

	public CellReference getFirstCell() {
		return _firstCell;
	}

	public CellReference getLastCell() {
		return _lastCell;
	}

	public CellReference[] getAllReferencedCells() {
		if (_isSingleCell) {
			return new CellReference[]{ _firstCell };
		}
		int minRow = Math.min(_firstCell.getRow(), _lastCell.getRow());
		int maxRow = Math.max(_firstCell.getRow(), _lastCell.getRow());
		int minCol = Math.min(_firstCell.getCol(), _lastCell.getCol());
		int maxCol = Math.max(_firstCell.getCol(), _lastCell.getCol());
		String sheetName = _firstCell.getSheetName();
		List<CellReference> refs = new ArrayList<>();
		for (int row = minRow; row <= maxRow; row++) {
			for (int col = minCol; col <= maxCol; col++) {
				CellReference ref = new CellReference(sheetName, row, col, _firstCell.isRowAbsolute(), _firstCell.isColAbsolute());
				refs.add(ref);
			}
		}
		return refs.toArray(new CellReference[refs.size()]);
	}

	public String formatAsString() {
		if (isWholeColumnReference()) {
			return ((CellReference.convertNumToColString(_firstCell.getCol())) + ":") + (CellReference.convertNumToColString(_lastCell.getCol()));
		}
		StringBuilder sb = new StringBuilder(32);
		sb.append(_firstCell.formatAsString());
		if (!(_isSingleCell)) {
			sb.append(AreaReference.CELL_DELIMITER);
			if ((_lastCell.getSheetName()) == null) {
				sb.append(_lastCell.formatAsString());
			}else {
			}
		}
		return sb.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(64);
		sb.append(getClass().getName()).append(" [");
		try {
			sb.append(formatAsString());
		} catch (Exception e) {
			sb.append(e.toString());
		}
		sb.append(']');
		return sb.toString();
	}

	private static String[] separateAreaRefs(String reference) {
		int len = reference.length();
		int delimiterPos = -1;
		boolean insideDelimitedName = false;
		for (int i = 0; i < len; i++) {
			switch (reference.charAt(i)) {
				case AreaReference.CELL_DELIMITER :
					if (!insideDelimitedName) {
						if (delimiterPos >= 0) {
							throw new IllegalArgumentException((((("More than one cell delimiter '" + (AreaReference.CELL_DELIMITER)) + "' appears in area reference '") + reference) + "'"));
						}
						delimiterPos = i;
					}
					continue;
				case AreaReference.SPECIAL_NAME_DELIMITER :
					break;
				default :
					continue;
			}
			if (!insideDelimitedName) {
				insideDelimitedName = true;
				continue;
			}
			if (i >= (len - 1)) {
				throw new IllegalArgumentException((((("Area reference '" + reference) + "' ends with special name delimiter '") + (AreaReference.SPECIAL_NAME_DELIMITER)) + "'"));
			}
			if ((reference.charAt((i + 1))) == (AreaReference.SPECIAL_NAME_DELIMITER)) {
				i++;
			}else {
				insideDelimitedName = false;
			}
		}
		if (delimiterPos < 0) {
			return new String[]{ reference };
		}
		String partA = reference.substring(0, delimiterPos);
		String partB = reference.substring((delimiterPos + 1));
		if ((partB.indexOf(AreaReference.SHEET_NAME_DELIMITER)) >= 0) {
			throw new RuntimeException((((("Unexpected " + (AreaReference.SHEET_NAME_DELIMITER)) + " in second cell reference of '") + reference) + "'"));
		}
		int plingPos = partA.lastIndexOf(AreaReference.SHEET_NAME_DELIMITER);
		if (plingPos < 0) {
			return new String[]{ partA, partB };
		}
		String sheetName = partA.substring(0, (plingPos + 1));
		return new String[]{ partA, sheetName + partB };
	}
}

