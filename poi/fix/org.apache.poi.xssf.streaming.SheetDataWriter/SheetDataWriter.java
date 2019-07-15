

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StringCodepointsIterable;
import org.apache.poi.util.TempFile;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;


public class SheetDataWriter implements Closeable {
	private static final POILogger logger = POILogFactory.getLogger(SheetDataWriter.class);

	private final File _fd;

	private final Writer _out;

	private int _rownum;

	private int _numberOfFlushedRows;

	private int _lowestIndexOfFlushedRows;

	private int _numberOfCellsOfLastFlushedRow;

	private int _numberLastFlushedRow = -1;

	private SharedStringsTable _sharedStringSource;

	public SheetDataWriter() throws IOException {
		_fd = createTempFile();
		_out = createWriter(_fd);
	}

	public SheetDataWriter(SharedStringsTable sharedStringsTable) throws IOException {
		this();
		this._sharedStringSource = sharedStringsTable;
	}

	public File createTempFile() throws IOException {
		return TempFile.createTempFile("poi-sxssf-sheet", ".xml");
	}

	public Writer createWriter(File fd) throws IOException {
		FileOutputStream fos = new FileOutputStream(fd);
		OutputStream decorated;
		try {
			decorated = decorateOutputStream(fos);
		} catch (final IOException e) {
			fos.close();
			throw e;
		}
		return new BufferedWriter(new OutputStreamWriter(decorated, "UTF-8"));
	}

	protected OutputStream decorateOutputStream(FileOutputStream fos) throws IOException {
		return fos;
	}

	public void close() throws IOException {
		_out.flush();
		_out.close();
	}

	protected File getTempFile() {
		return _fd;
	}

	public InputStream getWorksheetXMLInputStream() throws IOException {
		File fd = getTempFile();
		FileInputStream fis = new FileInputStream(fd);
		try {
			return decorateInputStream(fis);
		} catch (IOException e) {
			fis.close();
			throw e;
		}
	}

	protected InputStream decorateInputStream(FileInputStream fis) throws IOException {
		return fis;
	}

	public int getNumberOfFlushedRows() {
		return _numberOfFlushedRows;
	}

	public int getNumberOfCellsOfLastFlushedRow() {
		return _numberOfCellsOfLastFlushedRow;
	}

	public int getLowestIndexOfFlushedRows() {
		return _lowestIndexOfFlushedRows;
	}

	public int getLastFlushedRow() {
		return _numberLastFlushedRow;
	}

	@Override
	protected void finalize() throws Throwable {
		if (!(_fd.delete())) {
			SheetDataWriter.logger.log(POILogger.ERROR, ("Can't delete temporary encryption file: " + (_fd)));
		}
		super.finalize();
	}

	public void writeRow(int rownum, SXSSFRow row) throws IOException {
		if ((_numberOfFlushedRows) == 0)
			_lowestIndexOfFlushedRows = rownum;

		_numberLastFlushedRow = Math.max(rownum, _numberLastFlushedRow);
		_numberOfCellsOfLastFlushedRow = row.getLastCellNum();
		(_numberOfFlushedRows)++;
		beginRow(rownum, row);
		Iterator<Cell> cells = row.allCellsIterator();
		int columnIndex = 0;
		while (cells.hasNext()) {
			writeCell((columnIndex++), cells.next());
		} 
		endRow();
	}

	void beginRow(int rownum, SXSSFRow row) throws IOException {
		_out.write("<row");
		writeAttribute("r", Integer.toString((rownum + 1)));
		if (row.hasCustomHeight()) {
			writeAttribute("customHeight", "true");
			writeAttribute("ht", Float.toString(row.getHeightInPoints()));
		}
		if (row.getZeroHeight()) {
			writeAttribute("hidden", "true");
		}
		if (row.isFormatted()) {
			writeAttribute("customFormat", "1");
		}
		if ((row.getOutlineLevel()) != 0) {
			writeAttribute("outlineLevel", Integer.toString(row.getOutlineLevel()));
		}
		if ((row.getHidden()) != null) {
			writeAttribute("hidden", (row.getHidden() ? "1" : "0"));
		}
		if ((row.getCollapsed()) != null) {
			writeAttribute("collapsed", (row.getCollapsed() ? "1" : "0"));
		}
		_out.write(">\n");
		this._rownum = rownum;
	}

	void endRow() throws IOException {
		_out.write("</row>\n");
	}

	public void writeCell(int columnIndex, Cell cell) throws IOException {
		if (cell == null) {
			return;
		}
		String ref = new CellReference(_rownum, columnIndex).formatAsString();
		_out.write("<c");
		writeAttribute("r", ref);
		CellStyle cellStyle = cell.getCellStyle();
		if ((cellStyle.getIndex()) != 0) {
			writeAttribute("s", Integer.toString(((cellStyle.getIndex()) & 65535)));
		}
		CellType cellType = cell.getCellType();
		switch (cellType) {
			case BLANK :
				{
					_out.write('>');
					break;
				}
			case FORMULA :
				{
					_out.write("><f>");
					outputQuotedString(cell.getCellFormula());
					_out.write("</f>");
					switch (cell.getCachedFormulaResultType()) {
						case NUMERIC :
							double nval = cell.getNumericCellValue();
							if (!(Double.isNaN(nval))) {
								_out.write("<v>");
								_out.write(Double.toString(nval));
								_out.write("</v>");
							}
							break;
						default :
							break;
					}
					break;
				}
			case STRING :
				{
					if ((_sharedStringSource) != null) {
						XSSFRichTextString rt = new XSSFRichTextString(cell.getStringCellValue());
						int sRef = _sharedStringSource.addSharedStringItem(rt);
						writeAttribute("t", STCellType.S.toString());
						_out.write("><v>");
						_out.write(String.valueOf(sRef));
						_out.write("</v>");
					}else {
						writeAttribute("t", "inlineStr");
						_out.write("><is><t");
						if (hasLeadingTrailingSpaces(cell.getStringCellValue())) {
							writeAttribute("xml:space", "preserve");
						}
						_out.write(">");
						outputQuotedString(cell.getStringCellValue());
						_out.write("</t></is>");
					}
					break;
				}
			case NUMERIC :
				{
					writeAttribute("t", "n");
					_out.write("><v>");
					_out.write(Double.toString(cell.getNumericCellValue()));
					_out.write("</v>");
					break;
				}
			case BOOLEAN :
				{
					writeAttribute("t", "b");
					_out.write("><v>");
					_out.write((cell.getBooleanCellValue() ? "1" : "0"));
					_out.write("</v>");
					break;
				}
			case ERROR :
				{
					FormulaError error = FormulaError.forInt(cell.getErrorCellValue());
					writeAttribute("t", "e");
					_out.write("><v>");
					_out.write(error.getString());
					_out.write("</v>");
					break;
				}
			default :
				{
					throw new IllegalStateException(("Invalid cell type: " + cellType));
				}
		}
		_out.write("</c>");
	}

	private void writeAttribute(String name, String value) throws IOException {
		_out.write(' ');
		_out.write(name);
		_out.write("=\"");
		_out.write(value);
		_out.write('\"');
	}

	boolean hasLeadingTrailingSpaces(String str) {
		if ((str != null) && ((str.length()) > 0)) {
			char firstChar = str.charAt(0);
			char lastChar = str.charAt(((str.length()) - 1));
			return (Character.isWhitespace(firstChar)) || (Character.isWhitespace(lastChar));
		}
		return false;
	}

	protected void outputQuotedString(String s) throws IOException {
		if ((s == null) || ((s.length()) == 0)) {
			return;
		}
		for (String codepoint : new StringCodepointsIterable(s)) {
			switch (codepoint) {
				case "<" :
					_out.write("&lt;");
					break;
				case ">" :
					_out.write("&gt;");
					break;
				case "&" :
					_out.write("&amp;");
					break;
				case "\"" :
					_out.write("&quot;");
					break;
				case "\n" :
					_out.write("&#xa;");
					break;
				case "\r" :
					_out.write("&#xd;");
					break;
				case "\t" :
					_out.write("&#x9;");
					break;
				case "\u00a0" :
					_out.write("&#xa0;");
					break;
				default :
					if ((codepoint.length()) == 1) {
						char c = codepoint.charAt(0);
						if (SheetDataWriter.replaceWithQuestionMark(c)) {
							_out.write('?');
						}else {
							_out.write(c);
						}
					}else {
						_out.write(codepoint);
					}
					break;
			}
		}
	}

	static boolean replaceWithQuestionMark(char c) {
		return (c < ' ') || (('\ufffe' <= c) && (c <= '\uffff'));
	}

	boolean dispose() throws IOException {
		final boolean ret;
		try {
			_out.close();
		} finally {
			ret = _fd.delete();
		}
		return ret;
	}
}

