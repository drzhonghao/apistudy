

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaRenderer;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.SharedFormula;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellRange;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressBase;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.xssf.model.CalculationChain;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.BaseXSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellFormula;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRst;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellFormulaType;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;

import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_NULL_AND_BLANK;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellFormula.Factory.newInstance;


public final class XSSFCell implements Cell {
	private static final String FALSE_AS_STRING = "0";

	private static final String TRUE_AS_STRING = "1";

	private static final String FALSE = "FALSE";

	private static final String TRUE = "TRUE";

	private CTCell _cell;

	private final XSSFRow _row;

	private int _cellNum;

	private SharedStringsTable _sharedStringSource;

	private StylesTable _stylesSource;

	protected XSSFCell(XSSFRow row, CTCell cell) {
		_cell = cell;
		_row = row;
		if ((cell.getR()) != null) {
			_cellNum = new CellReference(cell.getR()).getCol();
		}else {
			int prevNum = row.getLastCellNum();
			if (prevNum != (-1)) {
				_cellNum = (row.getCell((prevNum - 1), RETURN_NULL_AND_BLANK).getColumnIndex()) + 1;
			}
		}
		_sharedStringSource = row.getSheet().getWorkbook().getSharedStringSource();
		_stylesSource = row.getSheet().getWorkbook().getStylesSource();
	}

	@org.apache.poi.util.Beta
	@org.apache.poi.util.Internal
	public void copyCellFrom(Cell srcCell, CellCopyPolicy policy) {
		if (policy.isCopyCellValue()) {
			if (srcCell != null) {
				CellType copyCellType = srcCell.getCellType();
				if ((copyCellType == (CellType.FORMULA)) && (!(policy.isCopyCellFormula()))) {
					copyCellType = srcCell.getCachedFormulaResultType();
				}
				switch (copyCellType) {
					case NUMERIC :
						if (DateUtil.isCellDateFormatted(srcCell)) {
							setCellValue(srcCell.getDateCellValue());
						}else {
							setCellValue(srcCell.getNumericCellValue());
						}
						break;
					case STRING :
						setCellValue(srcCell.getStringCellValue());
						break;
					case FORMULA :
						setCellFormula(srcCell.getCellFormula());
						break;
					case BLANK :
						setBlank();
						break;
					case BOOLEAN :
						setCellValue(srcCell.getBooleanCellValue());
						break;
					case ERROR :
						setCellErrorValue(srcCell.getErrorCellValue());
						break;
					default :
						throw new IllegalArgumentException(("Invalid cell type " + (srcCell.getCellType())));
				}
			}else {
				setBlank();
			}
		}
		if (policy.isCopyCellStyle()) {
			setCellStyle((srcCell == null ? null : srcCell.getCellStyle()));
		}
		final Hyperlink srcHyperlink = (srcCell == null) ? null : srcCell.getHyperlink();
		if (policy.isMergeHyperlink()) {
			if (srcHyperlink != null) {
				setHyperlink(new XSSFHyperlink(srcHyperlink));
			}
		}else
			if (policy.isCopyHyperlink()) {
				setHyperlink((srcHyperlink == null ? null : new XSSFHyperlink(srcHyperlink)));
			}

	}

	protected SharedStringsTable getSharedStringSource() {
		return _sharedStringSource;
	}

	protected StylesTable getStylesSource() {
		return _stylesSource;
	}

	@Override
	public XSSFSheet getSheet() {
		return getRow().getSheet();
	}

	@Override
	public XSSFRow getRow() {
		return _row;
	}

	@Override
	public boolean getBooleanCellValue() {
		CellType cellType = getCellType();
		switch (cellType) {
			case BLANK :
				return false;
			case BOOLEAN :
				return (_cell.isSetV()) && (XSSFCell.TRUE_AS_STRING.equals(_cell.getV()));
			case FORMULA :
				return (_cell.isSetV()) && (XSSFCell.TRUE_AS_STRING.equals(_cell.getV()));
			default :
				throw XSSFCell.typeMismatch(CellType.BOOLEAN, cellType, false);
		}
	}

	@Override
	public void setCellValue(boolean value) {
		_cell.setT(STCellType.B);
		_cell.setV((value ? XSSFCell.TRUE_AS_STRING : XSSFCell.FALSE_AS_STRING));
	}

	@Override
	public double getNumericCellValue() {
		CellType cellType = getCellType();
		switch (cellType) {
			case BLANK :
				return 0.0;
			case FORMULA :
			case NUMERIC :
				if (_cell.isSetV()) {
					String v = _cell.getV();
					if (v.isEmpty()) {
						return 0.0;
					}
					try {
						return Double.parseDouble(v);
					} catch (NumberFormatException e) {
						throw XSSFCell.typeMismatch(CellType.NUMERIC, CellType.STRING, false);
					}
				}else {
					return 0.0;
				}
			default :
				throw XSSFCell.typeMismatch(CellType.NUMERIC, cellType, false);
		}
	}

	@Override
	public void setCellValue(double value) {
		if (Double.isInfinite(value)) {
			_cell.setT(STCellType.E);
			_cell.setV(FormulaError.DIV0.getString());
		}else
			if (Double.isNaN(value)) {
				_cell.setT(STCellType.E);
				_cell.setV(FormulaError.NUM.getString());
			}else {
				_cell.setT(STCellType.N);
				_cell.setV(String.valueOf(value));
			}

	}

	@Override
	public String getStringCellValue() {
		return getRichStringCellValue().getString();
	}

	@Override
	public XSSFRichTextString getRichStringCellValue() {
		CellType cellType = getCellType();
		XSSFRichTextString rt;
		switch (cellType) {
			case BLANK :
				rt = new XSSFRichTextString("");
				break;
			case STRING :
				if ((_cell.getT()) == (STCellType.INLINE_STR)) {
					if (_cell.isSetIs()) {
						rt = new XSSFRichTextString(_cell.getIs());
					}else
						if (_cell.isSetV()) {
							rt = new XSSFRichTextString(_cell.getV());
						}else {
							rt = new XSSFRichTextString("");
						}

				}else
					if ((_cell.getT()) == (STCellType.STR)) {
						rt = new XSSFRichTextString((_cell.isSetV() ? _cell.getV() : ""));
					}else {
						if (_cell.isSetV()) {
							int idx = Integer.parseInt(_cell.getV());
							rt = new XSSFRichTextString(_sharedStringSource.getEntryAt(idx));
						}else {
							rt = new XSSFRichTextString("");
						}
					}

				break;
			case FORMULA :
				XSSFCell.checkFormulaCachedValueType(CellType.STRING, getBaseCellType(false));
				rt = new XSSFRichTextString((_cell.isSetV() ? _cell.getV() : ""));
				break;
			default :
				throw XSSFCell.typeMismatch(CellType.STRING, cellType, false);
		}
		return rt;
	}

	private static void checkFormulaCachedValueType(CellType expectedTypeCode, CellType cachedValueType) {
		if (cachedValueType != expectedTypeCode) {
			throw XSSFCell.typeMismatch(expectedTypeCode, cachedValueType, true);
		}
	}

	@Override
	public void setCellValue(String str) {
		setCellValue((str == null ? null : new XSSFRichTextString(str)));
	}

	@Override
	public void setCellValue(RichTextString str) {
		if ((str == null) || ((str.getString()) == null)) {
			setCellType(CellType.BLANK);
			return;
		}
		if ((str.length()) > (SpreadsheetVersion.EXCEL2007.getMaxTextLength())) {
			throw new IllegalArgumentException("The maximum length of cell contents (text) is 32,767 characters");
		}
		CellType cellType = getCellType();
		switch (cellType) {
			case FORMULA :
				_cell.setV(str.getString());
				_cell.setT(STCellType.STR);
				break;
			default :
				if ((_cell.getT()) == (STCellType.INLINE_STR)) {
					_cell.setV(str.getString());
				}else {
					_cell.setT(STCellType.S);
					XSSFRichTextString rt = ((XSSFRichTextString) (str));
					int sRef = _sharedStringSource.addSharedStringItem(rt);
					_cell.setV(Integer.toString(sRef));
				}
				break;
		}
	}

	@Override
	public String getCellFormula() {
		return getCellFormula(null);
	}

	protected String getCellFormula(BaseXSSFEvaluationWorkbook fpb) {
		CellType cellType = getCellType();
		if (cellType != (CellType.FORMULA)) {
			throw XSSFCell.typeMismatch(CellType.FORMULA, cellType, false);
		}
		CTCellFormula f = _cell.getF();
		if (isPartOfArrayFormulaGroup()) {
			if ((f == null) || (f.getStringValue().isEmpty())) {
			}
		}
		if (f == null) {
			return null;
		}else
			if ((f.getT()) == (STCellFormulaType.SHARED)) {
				return convertSharedFormula(((int) (f.getSi())), (fpb == null ? XSSFEvaluationWorkbook.create(getSheet().getWorkbook()) : fpb));
			}else {
				return f.getStringValue();
			}

	}

	private String convertSharedFormula(int si, BaseXSSFEvaluationWorkbook fpb) {
		XSSFSheet sheet = getSheet();
		CTCellFormula f = sheet.getSharedFormula(si);
		if (f == null) {
			throw new IllegalStateException((("Master cell of a shared formula with sid=" + si) + " was not found"));
		}
		String sharedFormula = f.getStringValue();
		String sharedFormulaRange = f.getRef();
		CellRangeAddress ref = CellRangeAddress.valueOf(sharedFormulaRange);
		int sheetIndex = sheet.getWorkbook().getSheetIndex(sheet);
		SharedFormula sf = new SharedFormula(SpreadsheetVersion.EXCEL2007);
		Ptg[] ptgs = FormulaParser.parse(sharedFormula, fpb, FormulaType.CELL, sheetIndex, getRowIndex());
		Ptg[] fmla = sf.convertSharedFormulas(ptgs, ((getRowIndex()) - (ref.getFirstRow())), ((getColumnIndex()) - (ref.getFirstColumn())));
		return FormulaRenderer.toFormulaString(fpb, fmla);
	}

	@Override
	public void setCellFormula(String formula) {
		if (isPartOfArrayFormulaGroup()) {
			notifyArrayFormulaChanging();
		}
		setFormula(formula, FormulaType.CELL);
	}

	void setCellArrayFormula(String formula, CellRangeAddress range) {
		setFormula(formula, FormulaType.ARRAY);
		CTCellFormula cellFormula = _cell.getF();
		cellFormula.setT(STCellFormulaType.ARRAY);
		cellFormula.setRef(range.formatAsString());
	}

	private void setFormula(String formula, FormulaType formulaType) {
		XSSFWorkbook wb = _row.getSheet().getWorkbook();
		if (formula == null) {
			if (_cell.isSetF()) {
				_cell.unsetF();
			}
			return;
		}
		if (wb.getCellFormulaValidation()) {
			XSSFEvaluationWorkbook fpb = XSSFEvaluationWorkbook.create(wb);
			FormulaParser.parse(formula, fpb, formulaType, wb.getSheetIndex(getSheet()), getRowIndex());
		}
		CTCellFormula f;
		if (_cell.isSetF()) {
			f = _cell.getF();
			f.setStringValue(formula);
			if ((f.getT()) == (STCellFormulaType.SHARED)) {
			}
		}else {
			f = newInstance();
			f.setStringValue(formula);
			_cell.setF(f);
		}
		if (_cell.isSetV()) {
			_cell.unsetV();
		}
	}

	@Override
	public int getColumnIndex() {
		return this._cellNum;
	}

	@Override
	public int getRowIndex() {
		return _row.getRowNum();
	}

	public String getReference() {
		String ref = _cell.getR();
		if (ref == null) {
			return getAddress().formatAsString();
		}
		return ref;
	}

	@Override
	public CellAddress getAddress() {
		return new CellAddress(this);
	}

	@Override
	public XSSFCellStyle getCellStyle() {
		XSSFCellStyle style = null;
		if ((_stylesSource.getNumCellStyles()) > 0) {
			long idx = (_cell.isSetS()) ? _cell.getS() : 0;
			style = _stylesSource.getStyleAt(((int) (idx)));
		}
		return style;
	}

	@Override
	public void setCellStyle(CellStyle style) {
		if (style == null) {
			if (_cell.isSetS()) {
				_cell.unsetS();
			}
		}else {
			XSSFCellStyle xStyle = ((XSSFCellStyle) (style));
			xStyle.verifyBelongsToStylesSource(_stylesSource);
			long idx = _stylesSource.putStyle(xStyle);
			_cell.setS(idx);
		}
	}

	private boolean isFormulaCell() {
		return false;
	}

	@Override
	public CellType getCellType() {
		if (isFormulaCell()) {
			return CellType.FORMULA;
		}
		return getBaseCellType(true);
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2")
	@Override
	public CellType getCellTypeEnum() {
		return getCellType();
	}

	@Override
	public CellType getCachedFormulaResultType() {
		if (!(isFormulaCell())) {
			throw new IllegalStateException("Only formula cells have cached results");
		}
		return getBaseCellType(false);
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2")
	@Override
	public CellType getCachedFormulaResultTypeEnum() {
		return getCachedFormulaResultType();
	}

	private CellType getBaseCellType(boolean blankCells) {
		switch (_cell.getT().intValue()) {
			case STCellType.INT_B :
				return CellType.BOOLEAN;
			case STCellType.INT_N :
				if ((!(_cell.isSetV())) && blankCells) {
					return CellType.BLANK;
				}
				return CellType.NUMERIC;
			case STCellType.INT_E :
				return CellType.ERROR;
			case STCellType.INT_S :
			case STCellType.INT_INLINE_STR :
			case STCellType.INT_STR :
				return CellType.STRING;
			default :
				throw new IllegalStateException(("Illegal cell type: " + (this._cell.getT())));
		}
	}

	@Override
	public Date getDateCellValue() {
		if ((getCellType()) == (CellType.BLANK)) {
			return null;
		}
		double value = getNumericCellValue();
		boolean date1904 = getSheet().getWorkbook().isDate1904();
		return DateUtil.getJavaDate(value, date1904);
	}

	@Override
	public void setCellValue(Date value) {
		if (value == null) {
			setCellType(CellType.BLANK);
			return;
		}
		boolean date1904 = getSheet().getWorkbook().isDate1904();
		setCellValue(DateUtil.getExcelDate(value, date1904));
	}

	@Override
	public void setCellValue(Calendar value) {
		if (value == null) {
			setCellType(CellType.BLANK);
			return;
		}
		boolean date1904 = getSheet().getWorkbook().isDate1904();
		setCellValue(DateUtil.getExcelDate(value, date1904));
	}

	public String getErrorCellString() throws IllegalStateException {
		CellType cellType = getBaseCellType(true);
		if (cellType != (CellType.ERROR)) {
			throw XSSFCell.typeMismatch(CellType.ERROR, cellType, false);
		}
		return _cell.getV();
	}

	@Override
	public byte getErrorCellValue() throws IllegalStateException {
		String code = getErrorCellString();
		if (code == null) {
			return 0;
		}
		try {
			return FormulaError.forString(code).getCode();
		} catch (final IllegalArgumentException e) {
			throw new IllegalStateException("Unexpected error code", e);
		}
	}

	@Override
	public void setCellErrorValue(byte errorCode) {
		FormulaError error = FormulaError.forInt(errorCode);
		setCellErrorValue(error);
	}

	public void setCellErrorValue(FormulaError error) {
		_cell.setT(STCellType.E);
		_cell.setV(error.getString());
	}

	@Override
	public void setAsActiveCell() {
		getSheet().setActiveCell(getAddress());
	}

	private void setBlank() {
		CTCell blank = CTCell.Factory.newInstance();
		blank.setR(_cell.getR());
		if (_cell.isSetS()) {
			blank.setS(_cell.getS());
		}
		_cell.set(blank);
	}

	protected void setCellNum(int num) {
		XSSFCell.checkBounds(num);
		_cellNum = num;
		String ref = new CellReference(getRowIndex(), getColumnIndex()).formatAsString();
		_cell.setR(ref);
	}

	@Override
	public void setCellType(CellType cellType) {
		setCellType(cellType, null);
	}

	protected void setCellType(CellType cellType, BaseXSSFEvaluationWorkbook evalWb) {
		CellType prevType = getCellType();
		if (isPartOfArrayFormulaGroup()) {
			notifyArrayFormulaChanging();
		}
		if ((prevType == (CellType.FORMULA)) && (cellType != (CellType.FORMULA))) {
			if (_cell.isSetF()) {
			}
		}
		switch (cellType) {
			case NUMERIC :
				_cell.setT(STCellType.N);
				break;
			case STRING :
				if (prevType != (CellType.STRING)) {
					String str = convertCellValueToString();
					XSSFRichTextString rt = new XSSFRichTextString(str);
					int sRef = _sharedStringSource.addSharedStringItem(rt);
					_cell.setV(Integer.toString(sRef));
				}
				_cell.setT(STCellType.S);
				break;
			case FORMULA :
				if (!(_cell.isSetF())) {
					CTCellFormula f = newInstance();
					f.setStringValue("0");
					_cell.setF(f);
					if (_cell.isSetT()) {
						_cell.unsetT();
					}
				}
				break;
			case BLANK :
				setBlank();
				break;
			case BOOLEAN :
				String newVal = (convertCellValueToBoolean()) ? XSSFCell.TRUE_AS_STRING : XSSFCell.FALSE_AS_STRING;
				_cell.setT(STCellType.B);
				_cell.setV(newVal);
				break;
			case ERROR :
				_cell.setT(STCellType.E);
				break;
			default :
				throw new IllegalArgumentException(("Illegal cell type: " + cellType));
		}
		if ((cellType != (CellType.FORMULA)) && (_cell.isSetF())) {
			_cell.unsetF();
		}
	}

	@Override
	public String toString() {
		switch (getCellType()) {
			case NUMERIC :
				if (DateUtil.isCellDateFormatted(this)) {
					DateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", LocaleUtil.getUserLocale());
					sdf.setTimeZone(LocaleUtil.getUserTimeZone());
					return sdf.format(getDateCellValue());
				}
				return Double.toString(getNumericCellValue());
			case STRING :
				return getRichStringCellValue().toString();
			case FORMULA :
				return getCellFormula();
			case BLANK :
				return "";
			case BOOLEAN :
				return getBooleanCellValue() ? XSSFCell.TRUE : XSSFCell.FALSE;
			case ERROR :
				return ErrorEval.getText(getErrorCellValue());
			default :
				return "Unknown Cell Type: " + (getCellType());
		}
	}

	public String getRawValue() {
		return _cell.getV();
	}

	private static RuntimeException typeMismatch(CellType expectedType, CellType actualType, boolean isFormulaCell) {
		String msg = ((((("Cannot get a " + expectedType) + " value from a ") + actualType) + " ") + (isFormulaCell ? "formula " : "")) + "cell";
		return new IllegalStateException(msg);
	}

	private static void checkBounds(int cellIndex) {
		SpreadsheetVersion v = SpreadsheetVersion.EXCEL2007;
		int maxcol = SpreadsheetVersion.EXCEL2007.getLastColumnIndex();
		if ((cellIndex < 0) || (cellIndex > maxcol)) {
			throw new IllegalArgumentException((((((((("Invalid column index (" + cellIndex) + ").  Allowable column range for ") + (v.name())) + " is (0..") + maxcol) + ") or ('A'..'") + (v.getLastColumnName())) + "')"));
		}
	}

	@Override
	public XSSFComment getCellComment() {
		return getSheet().getCellComment(new CellAddress(this));
	}

	@Override
	public void setCellComment(Comment comment) {
		if (comment == null) {
			removeCellComment();
			return;
		}
		comment.setAddress(getRowIndex(), getColumnIndex());
	}

	@Override
	public void removeCellComment() {
		XSSFComment comment = getCellComment();
		if (comment != null) {
			CellAddress ref = new CellAddress(getReference());
			XSSFSheet sh = getSheet();
		}
	}

	@Override
	public XSSFHyperlink getHyperlink() {
		return getSheet().getHyperlink(_row.getRowNum(), _cellNum);
	}

	@Override
	public void setHyperlink(Hyperlink hyperlink) {
		if (hyperlink == null) {
			removeHyperlink();
			return;
		}
		XSSFHyperlink link = ((XSSFHyperlink) (hyperlink));
		link.setCellReference(new CellReference(_row.getRowNum(), _cellNum).formatAsString());
		getSheet().addHyperlink(link);
	}

	@Override
	public void removeHyperlink() {
		getSheet().removeHyperlink(_row.getRowNum(), _cellNum);
	}

	@org.apache.poi.util.Internal
	public CTCell getCTCell() {
		return _cell;
	}

	@org.apache.poi.util.Internal
	public void setCTCell(CTCell cell) {
		_cell = cell;
	}

	private boolean convertCellValueToBoolean() {
		CellType cellType = getCellType();
		if (cellType == (CellType.FORMULA)) {
			cellType = getBaseCellType(false);
		}
		switch (cellType) {
			case BOOLEAN :
				return XSSFCell.TRUE_AS_STRING.equals(_cell.getV());
			case STRING :
				int sstIndex = Integer.parseInt(_cell.getV());
				XSSFRichTextString rt = new XSSFRichTextString(_sharedStringSource.getEntryAt(sstIndex));
				String text = rt.getString();
				return Boolean.parseBoolean(text);
			case NUMERIC :
				return (Double.parseDouble(_cell.getV())) != 0;
			case ERROR :
			case BLANK :
				return false;
			default :
				throw new RuntimeException((("Unexpected cell type (" + cellType) + ")"));
		}
	}

	private String convertCellValueToString() {
		CellType cellType = getCellType();
		switch (cellType) {
			case BLANK :
				return "";
			case BOOLEAN :
				return XSSFCell.TRUE_AS_STRING.equals(_cell.getV()) ? XSSFCell.TRUE : XSSFCell.FALSE;
			case STRING :
				int sstIndex = Integer.parseInt(_cell.getV());
				XSSFRichTextString rt = new XSSFRichTextString(_sharedStringSource.getEntryAt(sstIndex));
				return rt.getString();
			case NUMERIC :
			case ERROR :
				return _cell.getV();
			case FORMULA :
				break;
			default :
				throw new IllegalStateException((("Unexpected cell type (" + cellType) + ")"));
		}
		cellType = getBaseCellType(false);
		String textValue = _cell.getV();
		switch (cellType) {
			case BOOLEAN :
				if (XSSFCell.TRUE_AS_STRING.equals(textValue)) {
					return XSSFCell.TRUE;
				}
				if (XSSFCell.FALSE_AS_STRING.equals(textValue)) {
					return XSSFCell.FALSE;
				}
				throw new IllegalStateException((("Unexpected boolean cached formula value '" + textValue) + "'."));
			case STRING :
			case NUMERIC :
			case ERROR :
				return textValue;
			default :
				throw new IllegalStateException((("Unexpected formula result type (" + cellType) + ")"));
		}
	}

	@Override
	public CellRangeAddress getArrayFormulaRange() {
		return null;
	}

	@Override
	public boolean isPartOfArrayFormulaGroup() {
		return false;
	}

	void notifyArrayFormulaChanging(String msg) {
		if (isPartOfArrayFormulaGroup()) {
			CellRangeAddress cra = getArrayFormulaRange();
			if ((cra.getNumberOfCells()) > 1) {
				throw new IllegalStateException(msg);
			}
			getRow().getSheet().removeArrayFormula(this);
		}
	}

	void notifyArrayFormulaChanging() {
		CellReference ref = new CellReference(this);
		String msg = (("Cell " + (ref.formatAsString())) + " is part of a multi-cell array formula. ") + "You cannot change part of an array.";
		notifyArrayFormulaChanging(msg);
	}

	public void updateCellReferencesForShifting(String msg) {
		if (isPartOfArrayFormulaGroup())
			notifyArrayFormulaChanging(msg);

		CalculationChain calcChain = getSheet().getWorkbook().getCalculationChain();
		if (calcChain != null) {
		}
		CTCell ctCell = getCTCell();
		String r = new CellReference(getRowIndex(), getColumnIndex()).formatAsString();
		ctCell.setR(r);
	}
}

