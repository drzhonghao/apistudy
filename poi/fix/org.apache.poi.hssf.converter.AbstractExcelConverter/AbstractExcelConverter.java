

import org.apache.poi.hssf.converter.AbstractExcelUtils;
import org.apache.poi.hssf.converter.ExcelToHtmlUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hwpf.converter.DefaultFontReplacer;
import org.apache.poi.hwpf.converter.FontReplacer;
import org.apache.poi.hwpf.converter.NumberFormatter;
import org.apache.poi.util.Beta;
import org.w3c.dom.Document;


@Beta
public abstract class AbstractExcelConverter {
	protected static int getColumnWidth(HSSFSheet sheet, int columnIndex) {
		return ExcelToHtmlUtils.getColumnWidthInPx(sheet.getColumnWidth(columnIndex));
	}

	protected static int getDefaultColumnWidth(HSSFSheet sheet) {
		return ExcelToHtmlUtils.getColumnWidthInPx(sheet.getDefaultColumnWidth());
	}

	protected final HSSFDataFormatter _formatter = new HSSFDataFormatter();

	private FontReplacer fontReplacer = new DefaultFontReplacer();

	private boolean outputColumnHeaders = true;

	private boolean outputHiddenColumns;

	private boolean outputHiddenRows;

	private boolean outputLeadingSpacesAsNonBreaking = true;

	private boolean outputRowNumbers = true;

	protected String getColumnName(int columnIndex) {
		return NumberFormatter.getNumber((columnIndex + 1), 3);
	}

	protected abstract Document getDocument();

	public FontReplacer getFontReplacer() {
		return fontReplacer;
	}

	protected String getRowName(HSSFRow row) {
		return String.valueOf(((row.getRowNum()) + 1));
	}

	public boolean isOutputColumnHeaders() {
		return outputColumnHeaders;
	}

	public boolean isOutputHiddenColumns() {
		return outputHiddenColumns;
	}

	public boolean isOutputHiddenRows() {
		return outputHiddenRows;
	}

	public boolean isOutputLeadingSpacesAsNonBreaking() {
		return outputLeadingSpacesAsNonBreaking;
	}

	public boolean isOutputRowNumbers() {
		return outputRowNumbers;
	}

	protected boolean isTextEmpty(HSSFCell cell) {
		final String value;
		return false;
	}

	public void setFontReplacer(FontReplacer fontReplacer) {
		this.fontReplacer = fontReplacer;
	}

	public void setOutputColumnHeaders(boolean outputColumnHeaders) {
		this.outputColumnHeaders = outputColumnHeaders;
	}

	public void setOutputHiddenColumns(boolean outputZeroWidthColumns) {
		this.outputHiddenColumns = outputZeroWidthColumns;
	}

	public void setOutputHiddenRows(boolean outputZeroHeightRows) {
		this.outputHiddenRows = outputZeroHeightRows;
	}

	public void setOutputLeadingSpacesAsNonBreaking(boolean outputPrePostSpacesAsNonBreaking) {
		this.outputLeadingSpacesAsNonBreaking = outputPrePostSpacesAsNonBreaking;
	}

	public void setOutputRowNumbers(boolean outputRowNumbers) {
		this.outputRowNumbers = outputRowNumbers;
	}
}

