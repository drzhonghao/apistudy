

import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.record.NameRecord;
import org.apache.poi.hssf.usermodel.HSSFName;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationName;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaRenderingWorkbook;
import org.apache.poi.ss.formula.NameIdentifier;
import org.apache.poi.ss.formula.SheetIdentifier;
import org.apache.poi.ss.formula.SheetRangeIdentifier;
import org.apache.poi.ss.formula.ptg.Area3DPtg;
import org.apache.poi.ss.formula.ptg.NamePtg;
import org.apache.poi.ss.formula.ptg.NameXPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.Ref3DPtg;
import org.apache.poi.ss.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.Table;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Internal;


@Internal
public final class HSSFEvaluationWorkbook implements EvaluationWorkbook , FormulaParsingWorkbook , FormulaRenderingWorkbook {
	private final HSSFWorkbook _uBook;

	private final InternalWorkbook _iBook;

	public static HSSFEvaluationWorkbook create(HSSFWorkbook book) {
		if (book == null) {
			return null;
		}
		return new HSSFEvaluationWorkbook(book);
	}

	private HSSFEvaluationWorkbook(HSSFWorkbook book) {
		_uBook = book;
		_iBook = null;
	}

	@Override
	public void clearAllCachedResultValues() {
	}

	@Override
	public HSSFName createName() {
		return _uBook.createName();
	}

	@Override
	public int getExternalSheetIndex(String sheetName) {
		int sheetIndex = _uBook.getSheetIndex(sheetName);
		return _iBook.checkExternSheet(sheetIndex);
	}

	@Override
	public int getExternalSheetIndex(String workbookName, String sheetName) {
		return _iBook.getExternalSheetIndex(workbookName, sheetName);
	}

	@Override
	public Ptg get3DReferencePtg(CellReference cr, SheetIdentifier sheet) {
		int extIx = getSheetExtIx(sheet);
		return new Ref3DPtg(cr, extIx);
	}

	@Override
	public Ptg get3DReferencePtg(AreaReference areaRef, SheetIdentifier sheet) {
		int extIx = getSheetExtIx(sheet);
		return new Area3DPtg(areaRef, extIx);
	}

	@Override
	public NameXPtg getNameXPtg(String name, SheetIdentifier sheet) {
		int sheetRefIndex = getSheetExtIx(sheet);
		return null;
	}

	@Override
	public EvaluationName getName(String name, int sheetIndex) {
		for (int i = 0; i < (_iBook.getNumNames()); i++) {
			NameRecord nr = _iBook.getNameRecord(i);
			if (((nr.getSheetNumber()) == (sheetIndex + 1)) && (name.equalsIgnoreCase(nr.getNameText()))) {
				return new HSSFEvaluationWorkbook.Name(nr, i);
			}
		}
		return sheetIndex == (-1) ? null : getName(name, (-1));
	}

	@Override
	public int getSheetIndex(EvaluationSheet evalSheet) {
		return 0;
	}

	@Override
	public int getSheetIndex(String sheetName) {
		return _uBook.getSheetIndex(sheetName);
	}

	@Override
	public String getSheetName(int sheetIndex) {
		return _uBook.getSheetName(sheetIndex);
	}

	@Override
	public EvaluationSheet getSheet(int sheetIndex) {
		return null;
	}

	@Override
	public int convertFromExternSheetIndex(int externSheetIndex) {
		return _iBook.getFirstSheetIndexFromExternSheetIndex(externSheetIndex);
	}

	@Override
	public EvaluationWorkbook.ExternalSheet getExternalSheet(int externSheetIndex) {
		EvaluationWorkbook.ExternalSheet sheet = _iBook.getExternalSheet(externSheetIndex);
		if (sheet == null) {
			int localSheetIndex = convertFromExternSheetIndex(externSheetIndex);
			if (localSheetIndex == (-1)) {
				return null;
			}
			if (localSheetIndex == (-2)) {
				return null;
			}
			String sheetName = getSheetName(localSheetIndex);
			int lastLocalSheetIndex = _iBook.getLastSheetIndexFromExternSheetIndex(externSheetIndex);
			if (lastLocalSheetIndex == localSheetIndex) {
				sheet = new EvaluationWorkbook.ExternalSheet(null, sheetName);
			}else {
				String lastSheetName = getSheetName(lastLocalSheetIndex);
				sheet = new EvaluationWorkbook.ExternalSheetRange(null, sheetName, lastSheetName);
			}
		}
		return sheet;
	}

	@Override
	public EvaluationWorkbook.ExternalSheet getExternalSheet(String firstSheetName, String lastSheetName, int externalWorkbookNumber) {
		throw new IllegalStateException("XSSF-style external references are not supported for HSSF");
	}

	@Override
	public EvaluationWorkbook.ExternalName getExternalName(int externSheetIndex, int externNameIndex) {
		return _iBook.getExternalName(externSheetIndex, externNameIndex);
	}

	@Override
	public EvaluationWorkbook.ExternalName getExternalName(String nameName, String sheetName, int externalWorkbookNumber) {
		throw new IllegalStateException("XSSF-style external names are not supported for HSSF");
	}

	@Override
	public String resolveNameXText(NameXPtg n) {
		return _iBook.resolveNameXText(n.getSheetRefIndex(), n.getNameIndex());
	}

	@Override
	public String getSheetFirstNameByExternSheet(int externSheetIndex) {
		return _iBook.findSheetFirstNameFromExternSheet(externSheetIndex);
	}

	@Override
	public String getSheetLastNameByExternSheet(int externSheetIndex) {
		return _iBook.findSheetLastNameFromExternSheet(externSheetIndex);
	}

	@Override
	public String getNameText(NamePtg namePtg) {
		return _iBook.getNameRecord(namePtg.getIndex()).getNameText();
	}

	@Override
	public EvaluationName getName(NamePtg namePtg) {
		int ix = namePtg.getIndex();
		return new HSSFEvaluationWorkbook.Name(_iBook.getNameRecord(ix), ix);
	}

	@Override
	public Ptg[] getFormulaTokens(EvaluationCell evalCell) {
		return null;
	}

	@Override
	public UDFFinder getUDFFinder() {
		return null;
	}

	private static final class Name implements EvaluationName {
		private final NameRecord _nameRecord;

		private final int _index;

		public Name(NameRecord nameRecord, int index) {
			_nameRecord = nameRecord;
			_index = index;
		}

		@Override
		public Ptg[] getNameDefinition() {
			return _nameRecord.getNameDefinition();
		}

		@Override
		public String getNameText() {
			return _nameRecord.getNameText();
		}

		@Override
		public boolean hasFormula() {
			return _nameRecord.hasFormula();
		}

		@Override
		public boolean isFunctionName() {
			return _nameRecord.isFunctionName();
		}

		@Override
		public boolean isRange() {
			return _nameRecord.hasFormula();
		}

		@Override
		public NamePtg createPtg() {
			return new NamePtg(_index);
		}
	}

	private int getSheetExtIx(SheetIdentifier sheetIden) {
		int extIx;
		if (sheetIden == null) {
			extIx = -1;
		}else {
			String workbookName = sheetIden.getBookName();
			String firstSheetName = sheetIden.getSheetIdentifier().getName();
			String lastSheetName = firstSheetName;
			if (sheetIden instanceof SheetRangeIdentifier) {
				lastSheetName = ((SheetRangeIdentifier) (sheetIden)).getLastSheetIdentifier().getName();
			}
			if (workbookName == null) {
				int firstSheetIndex = _uBook.getSheetIndex(firstSheetName);
				int lastSheetIndex = _uBook.getSheetIndex(lastSheetName);
				extIx = _iBook.checkExternSheet(firstSheetIndex, lastSheetIndex);
			}else {
				extIx = _iBook.getExternalSheetIndex(workbookName, firstSheetName, lastSheetName);
			}
		}
		return extIx;
	}

	@Override
	public SpreadsheetVersion getSpreadsheetVersion() {
		return SpreadsheetVersion.EXCEL97;
	}

	@Override
	public Table getTable(String name) {
		throw new IllegalStateException("XSSF-style tables are not supported for HSSF");
	}
}

