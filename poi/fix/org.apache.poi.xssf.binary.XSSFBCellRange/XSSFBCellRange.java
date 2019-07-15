

import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;


@Internal
class XSSFBCellRange {
	public static final int length = 4 * (LittleEndian.INT_SIZE);

	public static XSSFBCellRange parse(byte[] data, int offset, XSSFBCellRange cellRange) {
		if (cellRange == null) {
			cellRange = new XSSFBCellRange();
		}
		offset += LittleEndian.INT_SIZE;
		offset += LittleEndian.INT_SIZE;
		offset += LittleEndian.INT_SIZE;
		return cellRange;
	}

	int firstRow;

	int lastRow;

	int firstCol;

	int lastCol;
}

