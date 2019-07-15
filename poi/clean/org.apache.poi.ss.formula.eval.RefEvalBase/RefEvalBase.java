import org.apache.poi.ss.formula.eval.*;


import org.apache.poi.ss.formula.SheetRange;

/**
 * Common base class for implementors of {@link RefEval}
 */
public abstract class RefEvalBase implements RefEval {
    private final int _firstSheetIndex;
    private final int _lastSheetIndex;
	private final int _rowIndex;
	private final int _columnIndex;

    protected RefEvalBase(SheetRange sheetRange, int rowIndex, int columnIndex) {
        if (sheetRange == null) {
            throw new IllegalArgumentException("sheetRange must not be null");
        }
        _firstSheetIndex = sheetRange.getFirstSheetIndex();
        _lastSheetIndex = sheetRange.getLastSheetIndex();
        _rowIndex = rowIndex;
        _columnIndex = columnIndex;
    }
	protected RefEvalBase(int firstSheetIndex, int lastSheetIndex, int rowIndex, int columnIndex) {
	    _firstSheetIndex = firstSheetIndex;
	    _lastSheetIndex = lastSheetIndex;
		_rowIndex = rowIndex;
		_columnIndex = columnIndex;
	}
    protected RefEvalBase(int onlySheetIndex, int rowIndex, int columnIndex) {
        this(onlySheetIndex, onlySheetIndex, rowIndex, columnIndex);
    }
    
	public int getNumberOfSheets() {
	    return _lastSheetIndex-_firstSheetIndex+1;
    }
    public int getFirstSheetIndex() {
        return _firstSheetIndex;
    }
    public int getLastSheetIndex() {
        return _lastSheetIndex;
    }
    public final int getRow() {
		return _rowIndex;
	}
	public final int getColumn() {
		return _columnIndex;
	}
}
