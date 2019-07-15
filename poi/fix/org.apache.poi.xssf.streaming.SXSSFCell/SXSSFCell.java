

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;


public class SXSSFCell implements Cell {
	private static final POILogger logger = POILogFactory.getLogger(SXSSFCell.class);

	private final SXSSFRow _row;

	private SXSSFCell.Value _value;

	private CellStyle _style;

	private SXSSFCell.Property _firstProperty;

	public SXSSFCell(SXSSFRow row, CellType cellType) {
		_row = row;
		setType(cellType);
	}

	@Override
	public int getColumnIndex() {
		return 0;
	}

	@Override
	public int getRowIndex() {
		return _row.getRowNum();
	}

	@Override
	public CellAddress getAddress() {
		return new CellAddress(this);
	}

	@Override
	public SXSSFSheet getSheet() {
		return _row.getSheet();
	}

	@Override
	public Row getRow() {
		return _row;
	}

	@Override
	public void setCellType(CellType cellType) {
		ensureType(cellType);
	}

	private boolean isFormulaCell() {
		return (_value) instanceof SXSSFCell.FormulaValue;
	}

	@Override
	public CellType getCellType() {
		if (isFormulaCell()) {
			return CellType.FORMULA;
		}
		return _value.getType();
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
		return ((SXSSFCell.FormulaValue) (_value)).getFormulaType();
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2")
	@Override
	public CellType getCachedFormulaResultTypeEnum() {
		return getCachedFormulaResultType();
	}

	@Override
	public void setCellValue(double value) {
		if (Double.isInfinite(value)) {
			setCellErrorValue(FormulaError.DIV0.getCode());
		}else
			if (Double.isNaN(value)) {
				setCellErrorValue(FormulaError.NUM.getCode());
			}else {
				ensureTypeOrFormulaType(CellType.NUMERIC);
				if ((_value.getType()) == (CellType.FORMULA))
					((SXSSFCell.NumericFormulaValue) (_value)).setPreEvaluatedValue(value);
				else
					((SXSSFCell.NumericValue) (_value)).setValue(value);

			}

	}

	@Override
	public void setCellValue(Date value) {
		if (value == null) {
			setCellType(CellType.BLANK);
			return;
		}
	}

	@Override
	public void setCellValue(Calendar value) {
		if (value == null) {
			setCellType(CellType.BLANK);
			return;
		}
	}

	@Override
	public void setCellValue(RichTextString value) {
		XSSFRichTextString xvalue = ((XSSFRichTextString) (value));
		if ((xvalue != null) && ((xvalue.getString()) != null)) {
			ensureRichTextStringType();
			if ((xvalue.length()) > (SpreadsheetVersion.EXCEL2007.getMaxTextLength())) {
				throw new IllegalArgumentException("The maximum length of cell contents (text) is 32,767 characters");
			}
			((SXSSFCell.RichTextValue) (_value)).setValue(xvalue);
		}else {
			setCellType(CellType.BLANK);
		}
	}

	@Override
	public void setCellValue(String value) {
		if (value != null) {
			ensureTypeOrFormulaType(CellType.STRING);
			if ((value.length()) > (SpreadsheetVersion.EXCEL2007.getMaxTextLength())) {
				throw new IllegalArgumentException("The maximum length of cell contents (text) is 32,767 characters");
			}
			if ((_value.getType()) == (CellType.FORMULA))
				if ((_value) instanceof SXSSFCell.NumericFormulaValue) {
					((SXSSFCell.NumericFormulaValue) (_value)).setPreEvaluatedValue(Double.parseDouble(value));
				}else {
					((SXSSFCell.StringFormulaValue) (_value)).setPreEvaluatedValue(value);
				}
			else
				((SXSSFCell.PlainStringValue) (_value)).setValue(value);

		}else {
			setCellType(CellType.BLANK);
		}
	}

	@Override
	public void setCellFormula(String formula) throws FormulaParseException {
		if (formula == null) {
			setType(CellType.BLANK);
			return;
		}
		ensureFormulaType(computeTypeFromFormula(formula));
		((SXSSFCell.FormulaValue) (_value)).setValue(formula);
	}

	@Override
	public String getCellFormula() {
		if ((_value.getType()) != (CellType.FORMULA))
			throw SXSSFCell.typeMismatch(CellType.FORMULA, _value.getType(), false);

		return ((SXSSFCell.FormulaValue) (_value)).getValue();
	}

	@Override
	public double getNumericCellValue() {
		CellType cellType = getCellType();
		switch (cellType) {
			case BLANK :
				return 0.0;
			case FORMULA :
				{
					SXSSFCell.FormulaValue fv = ((SXSSFCell.FormulaValue) (_value));
					if ((fv.getFormulaType()) != (CellType.NUMERIC))
						throw SXSSFCell.typeMismatch(CellType.NUMERIC, CellType.FORMULA, false);

					return ((SXSSFCell.NumericFormulaValue) (_value)).getPreEvaluatedValue();
				}
			case NUMERIC :
				return ((SXSSFCell.NumericValue) (_value)).getValue();
			default :
				throw SXSSFCell.typeMismatch(CellType.NUMERIC, cellType, false);
		}
	}

	@Override
	public Date getDateCellValue() {
		CellType cellType = getCellType();
		if (cellType == (CellType.BLANK)) {
			return null;
		}
		double value = getNumericCellValue();
		return null;
	}

	@Override
	public RichTextString getRichStringCellValue() {
		CellType cellType = getCellType();
		if ((getCellType()) != (CellType.STRING))
			throw SXSSFCell.typeMismatch(CellType.STRING, cellType, false);

		SXSSFCell.StringValue sval = ((SXSSFCell.StringValue) (_value));
		if (sval.isRichText())
			return ((SXSSFCell.RichTextValue) (_value)).getValue();
		else {
			String plainText = getStringCellValue();
			return getSheet().getWorkbook().getCreationHelper().createRichTextString(plainText);
		}
	}

	@Override
	public String getStringCellValue() {
		CellType cellType = getCellType();
		switch (cellType) {
			case BLANK :
				return "";
			case FORMULA :
				{
					SXSSFCell.FormulaValue fv = ((SXSSFCell.FormulaValue) (_value));
					if ((fv.getFormulaType()) != (CellType.STRING))
						throw SXSSFCell.typeMismatch(CellType.STRING, CellType.FORMULA, false);

					return ((SXSSFCell.StringFormulaValue) (_value)).getPreEvaluatedValue();
				}
			case STRING :
				{
					if (((SXSSFCell.StringValue) (_value)).isRichText())
						return ((SXSSFCell.RichTextValue) (_value)).getValue().getString();
					else
						return ((SXSSFCell.PlainStringValue) (_value)).getValue();

				}
			default :
				throw SXSSFCell.typeMismatch(CellType.STRING, cellType, false);
		}
	}

	@Override
	public void setCellValue(boolean value) {
		ensureTypeOrFormulaType(CellType.BOOLEAN);
		if ((_value.getType()) == (CellType.FORMULA))
			((SXSSFCell.BooleanFormulaValue) (_value)).setPreEvaluatedValue(value);
		else
			((SXSSFCell.BooleanValue) (_value)).setValue(value);

	}

	@Override
	public void setCellErrorValue(byte value) {
		if ((_value.getType()) == (CellType.FORMULA)) {
			setFormulaType(CellType.ERROR);
			((SXSSFCell.ErrorFormulaValue) (_value)).setPreEvaluatedValue(value);
		}else {
			ensureTypeOrFormulaType(CellType.ERROR);
			((SXSSFCell.ErrorValue) (_value)).setValue(value);
		}
	}

	@Override
	public boolean getBooleanCellValue() {
		CellType cellType = getCellType();
		switch (cellType) {
			case BLANK :
				return false;
			case FORMULA :
				{
					SXSSFCell.FormulaValue fv = ((SXSSFCell.FormulaValue) (_value));
					if ((fv.getFormulaType()) != (CellType.BOOLEAN))
						throw SXSSFCell.typeMismatch(CellType.BOOLEAN, CellType.FORMULA, false);

					return ((SXSSFCell.BooleanFormulaValue) (_value)).getPreEvaluatedValue();
				}
			case BOOLEAN :
				{
					return ((SXSSFCell.BooleanValue) (_value)).getValue();
				}
			default :
				throw SXSSFCell.typeMismatch(CellType.BOOLEAN, cellType, false);
		}
	}

	@Override
	public byte getErrorCellValue() {
		CellType cellType = getCellType();
		switch (cellType) {
			case BLANK :
				return 0;
			case FORMULA :
				{
					SXSSFCell.FormulaValue fv = ((SXSSFCell.FormulaValue) (_value));
					if ((fv.getFormulaType()) != (CellType.ERROR))
						throw SXSSFCell.typeMismatch(CellType.ERROR, CellType.FORMULA, false);

					return ((SXSSFCell.ErrorFormulaValue) (_value)).getPreEvaluatedValue();
				}
			case ERROR :
				{
					return ((SXSSFCell.ErrorValue) (_value)).getValue();
				}
			default :
				throw SXSSFCell.typeMismatch(CellType.ERROR, cellType, false);
		}
	}

	@Override
	public void setCellStyle(CellStyle style) {
		_style = style;
	}

	@Override
	public CellStyle getCellStyle() {
		if ((_style) == null) {
			SXSSFWorkbook wb = ((SXSSFWorkbook) (getRow().getSheet().getWorkbook()));
			return wb.getCellStyleAt(0);
		}else {
			return _style;
		}
	}

	@Override
	public void setAsActiveCell() {
		getSheet().setActiveCell(getAddress());
	}

	@Override
	public void setCellComment(Comment comment) {
		setProperty(SXSSFCell.Property.COMMENT, comment);
	}

	@Override
	public Comment getCellComment() {
		return ((Comment) (getPropertyValue(SXSSFCell.Property.COMMENT)));
	}

	@Override
	public void removeCellComment() {
		removeProperty(SXSSFCell.Property.COMMENT);
	}

	@Override
	public Hyperlink getHyperlink() {
		return ((Hyperlink) (getPropertyValue(SXSSFCell.Property.HYPERLINK)));
	}

	@Override
	public void setHyperlink(Hyperlink link) {
		if (link == null) {
			removeHyperlink();
			return;
		}
		setProperty(SXSSFCell.Property.HYPERLINK, link);
		XSSFHyperlink xssfobj = ((XSSFHyperlink) (link));
		CellReference ref = new CellReference(getRowIndex(), getColumnIndex());
		xssfobj.setCellReference(ref);
	}

	@Override
	public void removeHyperlink() {
		removeProperty(SXSSFCell.Property.HYPERLINK);
	}

	@org.apache.poi.util.NotImplemented
	public CellRangeAddress getArrayFormulaRange() {
		return null;
	}

	@org.apache.poi.util.NotImplemented
	public boolean isPartOfArrayFormulaGroup() {
		return false;
	}

	@Override
	public String toString() {
		switch (getCellType()) {
			case BLANK :
				return "";
			case BOOLEAN :
				return getBooleanCellValue() ? "TRUE" : "FALSE";
			case ERROR :
				return ErrorEval.getText(getErrorCellValue());
			case FORMULA :
				return getCellFormula();
			case NUMERIC :
				if (DateUtil.isCellDateFormatted(this)) {
					DateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", LocaleUtil.getUserLocale());
					sdf.setTimeZone(LocaleUtil.getUserTimeZone());
					return sdf.format(getDateCellValue());
				}
				return (getNumericCellValue()) + "";
			case STRING :
				return getRichStringCellValue().toString();
			default :
				return "Unknown Cell Type: " + (getCellType());
		}
	}

	void removeProperty(int type) {
		SXSSFCell.Property current = _firstProperty;
		SXSSFCell.Property previous = null;
		while ((current != null) && ((current.getType()) != type)) {
			previous = current;
			current = current._next;
		} 
		if (current != null) {
			if (previous != null) {
				previous._next = current._next;
			}else {
				_firstProperty = current._next;
			}
		}
	}

	void setProperty(int type, Object value) {
		SXSSFCell.Property current = _firstProperty;
		SXSSFCell.Property previous = null;
		while ((current != null) && ((current.getType()) != type)) {
			previous = current;
			current = current._next;
		} 
		if (current != null) {
			current.setValue(value);
		}else {
			switch (type) {
				case SXSSFCell.Property.COMMENT :
					{
						current = new SXSSFCell.CommentProperty(value);
						break;
					}
				case SXSSFCell.Property.HYPERLINK :
					{
						current = new SXSSFCell.HyperlinkProperty(value);
						break;
					}
				default :
					{
						throw new IllegalArgumentException(("Invalid type: " + type));
					}
			}
			if (previous != null) {
				previous._next = current;
			}else {
				_firstProperty = current;
			}
		}
	}

	Object getPropertyValue(int type) {
		return getPropertyValue(type, null);
	}

	Object getPropertyValue(int type, String defaultValue) {
		SXSSFCell.Property current = _firstProperty;
		while ((current != null) && ((current.getType()) != type))
			current = current._next;

		return current == null ? defaultValue : current.getValue();
	}

	void ensurePlainStringType() {
		if (((_value.getType()) != (CellType.STRING)) || (((SXSSFCell.StringValue) (_value)).isRichText()))
			_value = new SXSSFCell.PlainStringValue();

	}

	void ensureRichTextStringType() {
		if (((_value.getType()) != (CellType.STRING)) || (!(((SXSSFCell.StringValue) (_value)).isRichText())))
			_value = new SXSSFCell.RichTextValue();

	}

	void ensureType(CellType type) {
		if ((_value.getType()) != type)
			setType(type);

	}

	void ensureFormulaType(CellType type) {
		if (((_value.getType()) != (CellType.FORMULA)) || ((((SXSSFCell.FormulaValue) (_value)).getFormulaType()) != type))
			setFormulaType(type);

	}

	void ensureTypeOrFormulaType(CellType type) {
		if ((_value.getType()) == type) {
			if ((type == (CellType.STRING)) && (((SXSSFCell.StringValue) (_value)).isRichText()))
				setType(CellType.STRING);

			return;
		}
		if ((_value.getType()) == (CellType.FORMULA)) {
			if ((((SXSSFCell.FormulaValue) (_value)).getFormulaType()) == type)
				return;

			setFormulaType(type);
			return;
		}
		setType(type);
	}

	void setType(CellType type) {
		switch (type) {
			case NUMERIC :
				{
					_value = new SXSSFCell.NumericValue();
					break;
				}
			case STRING :
				{
					SXSSFCell.PlainStringValue sval = new SXSSFCell.PlainStringValue();
					if ((_value) != null) {
						String str = convertCellValueToString();
						sval.setValue(str);
					}
					_value = sval;
					break;
				}
			case FORMULA :
				{
					_value = new SXSSFCell.NumericFormulaValue();
					break;
				}
			case BLANK :
				{
					_value = new SXSSFCell.BlankValue();
					break;
				}
			case BOOLEAN :
				{
					SXSSFCell.BooleanValue bval = new SXSSFCell.BooleanValue();
					if ((_value) != null) {
						boolean val = convertCellValueToBoolean();
						bval.setValue(val);
					}
					_value = bval;
					break;
				}
			case ERROR :
				{
					_value = new SXSSFCell.ErrorValue();
					break;
				}
			default :
				{
					throw new IllegalArgumentException(("Illegal type " + type));
				}
		}
	}

	void setFormulaType(CellType type) {
		SXSSFCell.Value prevValue = _value;
		switch (type) {
			case NUMERIC :
				{
					_value = new SXSSFCell.NumericFormulaValue();
					break;
				}
			case STRING :
				{
					_value = new SXSSFCell.StringFormulaValue();
					break;
				}
			case BOOLEAN :
				{
					_value = new SXSSFCell.BooleanFormulaValue();
					break;
				}
			case ERROR :
				{
					_value = new SXSSFCell.ErrorFormulaValue();
					break;
				}
			default :
				{
					throw new IllegalArgumentException(("Illegal type " + type));
				}
		}
		if (prevValue instanceof SXSSFCell.FormulaValue) {
			((SXSSFCell.FormulaValue) (_value))._value = ((SXSSFCell.FormulaValue) (prevValue))._value;
		}
	}

	@org.apache.poi.util.NotImplemented
	CellType computeTypeFromFormula(String formula) {
		return CellType.NUMERIC;
	}

	private static RuntimeException typeMismatch(CellType expectedTypeCode, CellType actualTypeCode, boolean isFormulaCell) {
		String msg = ((((("Cannot get a " + expectedTypeCode) + " value from a ") + actualTypeCode) + " ") + (isFormulaCell ? "formula " : "")) + "cell";
		return new IllegalStateException(msg);
	}

	private boolean convertCellValueToBoolean() {
		CellType cellType = getCellType();
		if (cellType == (CellType.FORMULA)) {
			cellType = getCachedFormulaResultType();
		}
		switch (cellType) {
			case BOOLEAN :
				return getBooleanCellValue();
			case STRING :
				String text = getStringCellValue();
				return Boolean.parseBoolean(text);
			case NUMERIC :
				return (getNumericCellValue()) != 0;
			case ERROR :
			case BLANK :
				return false;
			default :
				throw new RuntimeException((("Unexpected cell type (" + cellType) + ")"));
		}
	}

	private String convertCellValueToString() {
		CellType cellType = getCellType();
		return convertCellValueToString(cellType);
	}

	private String convertCellValueToString(CellType cellType) {
		switch (cellType) {
			case BLANK :
				return "";
			case BOOLEAN :
				return getBooleanCellValue() ? "TRUE" : "FALSE";
			case STRING :
				return getStringCellValue();
			case NUMERIC :
				return Double.toString(getNumericCellValue());
			case ERROR :
				byte errVal = getErrorCellValue();
				return FormulaError.forInt(errVal).getString();
			case FORMULA :
				if ((_value) != null) {
					SXSSFCell.FormulaValue fv = ((SXSSFCell.FormulaValue) (_value));
					if ((fv.getFormulaType()) != (CellType.FORMULA)) {
						return convertCellValueToString(fv.getFormulaType());
					}
				}
				return "";
			default :
				throw new IllegalStateException((("Unexpected cell type (" + cellType) + ")"));
		}
	}

	static abstract class Property {
		static final int COMMENT = 1;

		static final int HYPERLINK = 2;

		Object _value;

		SXSSFCell.Property _next;

		public Property(Object value) {
			_value = value;
		}

		abstract int getType();

		void setValue(Object value) {
			_value = value;
		}

		Object getValue() {
			return _value;
		}
	}

	static class CommentProperty extends SXSSFCell.Property {
		public CommentProperty(Object value) {
			super(value);
		}

		@Override
		public int getType() {
			return SXSSFCell.Property.COMMENT;
		}
	}

	static class HyperlinkProperty extends SXSSFCell.Property {
		public HyperlinkProperty(Object value) {
			super(value);
		}

		@Override
		public int getType() {
			return SXSSFCell.Property.HYPERLINK;
		}
	}

	interface Value {
		CellType getType();
	}

	static class NumericValue implements SXSSFCell.Value {
		double _value;

		public CellType getType() {
			return CellType.NUMERIC;
		}

		void setValue(double value) {
			_value = value;
		}

		double getValue() {
			return _value;
		}
	}

	static abstract class StringValue implements SXSSFCell.Value {
		public CellType getType() {
			return CellType.STRING;
		}

		abstract boolean isRichText();
	}

	static class PlainStringValue extends SXSSFCell.StringValue {
		String _value;

		void setValue(String value) {
			_value = value;
		}

		String getValue() {
			return _value;
		}

		@Override
		boolean isRichText() {
			return false;
		}
	}

	static class RichTextValue extends SXSSFCell.StringValue {
		RichTextString _value;

		@Override
		public CellType getType() {
			return CellType.STRING;
		}

		void setValue(RichTextString value) {
			_value = value;
		}

		RichTextString getValue() {
			return _value;
		}

		@Override
		boolean isRichText() {
			return true;
		}
	}

	static abstract class FormulaValue implements SXSSFCell.Value {
		String _value;

		public CellType getType() {
			return CellType.FORMULA;
		}

		void setValue(String value) {
			_value = value;
		}

		String getValue() {
			return _value;
		}

		abstract CellType getFormulaType();
	}

	static class NumericFormulaValue extends SXSSFCell.FormulaValue {
		double _preEvaluatedValue;

		@Override
		CellType getFormulaType() {
			return CellType.NUMERIC;
		}

		void setPreEvaluatedValue(double value) {
			_preEvaluatedValue = value;
		}

		double getPreEvaluatedValue() {
			return _preEvaluatedValue;
		}
	}

	static class StringFormulaValue extends SXSSFCell.FormulaValue {
		String _preEvaluatedValue;

		@Override
		CellType getFormulaType() {
			return CellType.STRING;
		}

		void setPreEvaluatedValue(String value) {
			_preEvaluatedValue = value;
		}

		String getPreEvaluatedValue() {
			return _preEvaluatedValue;
		}
	}

	static class BooleanFormulaValue extends SXSSFCell.FormulaValue {
		boolean _preEvaluatedValue;

		@Override
		CellType getFormulaType() {
			return CellType.BOOLEAN;
		}

		void setPreEvaluatedValue(boolean value) {
			_preEvaluatedValue = value;
		}

		boolean getPreEvaluatedValue() {
			return _preEvaluatedValue;
		}
	}

	static class ErrorFormulaValue extends SXSSFCell.FormulaValue {
		byte _preEvaluatedValue;

		@Override
		CellType getFormulaType() {
			return CellType.ERROR;
		}

		void setPreEvaluatedValue(byte value) {
			_preEvaluatedValue = value;
		}

		byte getPreEvaluatedValue() {
			return _preEvaluatedValue;
		}
	}

	static class BlankValue implements SXSSFCell.Value {
		public CellType getType() {
			return CellType.BLANK;
		}
	}

	static class BooleanValue implements SXSSFCell.Value {
		boolean _value;

		public CellType getType() {
			return CellType.BOOLEAN;
		}

		void setValue(boolean value) {
			_value = value;
		}

		boolean getValue() {
			return _value;
		}
	}

	static class ErrorValue implements SXSSFCell.Value {
		byte _value;

		public CellType getType() {
			return CellType.ERROR;
		}

		void setValue(byte value) {
			_value = value;
		}

		byte getValue() {
			return _value;
		}
	}
}

