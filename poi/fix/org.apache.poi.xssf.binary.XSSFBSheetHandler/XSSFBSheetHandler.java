

import java.io.InputStream;
import java.util.Collection;
import java.util.Queue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.xssf.binary.XSSFBCommentsTable;
import org.apache.poi.xssf.binary.XSSFBParseException;
import org.apache.poi.xssf.binary.XSSFBParser;
import org.apache.poi.xssf.binary.XSSFBRecordType;
import org.apache.poi.xssf.binary.XSSFBStylesTable;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.usermodel.XSSFComment;


@Internal
public class XSSFBSheetHandler extends XSSFBParser {
	private static final int CHECK_ALL_ROWS = -1;

	private final SharedStrings stringsTable;

	private final XSSFSheetXMLHandler.SheetContentsHandler handler;

	private final XSSFBStylesTable styles;

	private final XSSFBCommentsTable comments;

	private final DataFormatter dataFormatter;

	private final boolean formulasNotResults;

	private int lastEndedRow = -1;

	private int lastStartedRow = -1;

	private int currentRow;

	private byte[] rkBuffer = new byte[8];

	private StringBuilder xlWideStringBuffer = new StringBuilder();

	public XSSFBSheetHandler(InputStream is, XSSFBStylesTable styles, XSSFBCommentsTable comments, SharedStrings strings, XSSFSheetXMLHandler.SheetContentsHandler sheetContentsHandler, DataFormatter dataFormatter, boolean formulasNotResults) {
		super(is);
		this.styles = styles;
		this.comments = comments;
		this.stringsTable = strings;
		this.handler = sheetContentsHandler;
		this.dataFormatter = dataFormatter;
		this.formulasNotResults = formulasNotResults;
	}

	@Override
	public void handleRecord(int id, byte[] data) throws XSSFBParseException {
		XSSFBRecordType type = XSSFBRecordType.lookup(id);
	}

	private void beforeCellValue(byte[] data) {
	}

	private void handleCellValue(String formattedValue) {
		if ((comments) != null) {
		}
	}

	private void handleFmlaNum(byte[] data) {
		beforeCellValue(data);
	}

	private void handleCellSt(byte[] data) {
		beforeCellValue(data);
		xlWideStringBuffer.setLength(0);
		handleCellValue(xlWideStringBuffer.toString());
	}

	private void handleFmlaString(byte[] data) {
		beforeCellValue(data);
		xlWideStringBuffer.setLength(0);
		handleCellValue(xlWideStringBuffer.toString());
	}

	private void handleCellError(byte[] data) {
		beforeCellValue(data);
		handleCellValue("ERROR");
	}

	private void handleFmlaError(byte[] data) {
		beforeCellValue(data);
		handleCellValue("ERROR");
	}

	private void handleBoolean(byte[] data) {
		beforeCellValue(data);
	}

	private void handleCellReal(byte[] data) {
		beforeCellValue(data);
	}

	private void handleCellRk(byte[] data) {
		beforeCellValue(data);
	}

	private String formatVal(double val, int styleIdx) {
		return null;
	}

	private void handleBrtCellIsst(byte[] data) {
		beforeCellValue(data);
	}

	private void handleHeaderFooter(byte[] data) {
	}

	private void checkMissedComments(int currentRow, int colNum) {
		if ((comments) == null) {
			return;
		}
		Queue<CellAddress> queue = comments.getAddresses();
		while ((queue.size()) > 0) {
			CellAddress cellAddress = queue.peek();
			if (((cellAddress.getRow()) == currentRow) && ((cellAddress.getColumn()) < colNum)) {
				cellAddress = queue.remove();
			}else
				if (((cellAddress.getRow()) == currentRow) && ((cellAddress.getColumn()) == colNum)) {
					queue.remove();
					return;
				}else
					if (((cellAddress.getRow()) == currentRow) && ((cellAddress.getColumn()) > colNum)) {
						return;
					}else
						if ((cellAddress.getRow()) > currentRow) {
							return;
						}



		} 
	}

	private void checkMissedComments(int currentRow) {
		if ((comments) == null) {
			return;
		}
		Queue<CellAddress> queue = comments.getAddresses();
		int lastInterpolatedRow = -1;
		while ((queue.size()) > 0) {
			CellAddress cellAddress = queue.peek();
			if ((currentRow == (XSSFBSheetHandler.CHECK_ALL_ROWS)) || ((cellAddress.getRow()) < currentRow)) {
				cellAddress = queue.remove();
				if ((cellAddress.getRow()) != lastInterpolatedRow) {
					startRow(cellAddress.getRow());
				}
				lastInterpolatedRow = cellAddress.getRow();
			}else {
				break;
			}
		} 
	}

	private void startRow(int row) {
		if (row == (lastStartedRow)) {
			return;
		}
		if ((lastStartedRow) != (lastEndedRow)) {
			endRow(lastStartedRow);
		}
		handler.startRow(row);
		lastStartedRow = row;
	}

	private void endRow(int row) {
		if ((lastEndedRow) == row) {
			return;
		}
		handler.endRow(row);
		lastEndedRow = row;
	}

	private double rkNumber(byte[] data, int offset) {
		byte b0 = data[offset];
		boolean numDivBy100 = (b0 & 1) == 1;
		boolean floatingPoint = ((b0 >> 1) & 1) == 0;
		b0 &= ~1;
		b0 &= ~(1 << 1);
		rkBuffer[4] = b0;
		for (int i = 1; i < 4; i++) {
			rkBuffer[(i + 4)] = data[(offset + i)];
		}
		double d = 0.0;
		if (floatingPoint) {
			d = LittleEndian.getDouble(rkBuffer);
		}else {
			int rawInt = LittleEndian.getInt(rkBuffer, 4);
			d = rawInt >> 2;
		}
		d = (numDivBy100) ? d / 100 : d;
		return d;
	}

	public interface SheetContentsHandler extends XSSFSheetXMLHandler.SheetContentsHandler {
		public abstract void hyperlinkCell(String cellReference, String formattedValue, String url, String toolTip, XSSFComment comment);
	}
}

