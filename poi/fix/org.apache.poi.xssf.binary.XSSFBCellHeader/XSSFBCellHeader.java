

import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;


@Internal
class XSSFBCellHeader {
	public static int length = 8;

	public static void parse(byte[] data, int offset, int currentRow, XSSFBCellHeader cell) {
		offset += LittleEndian.INT_SIZE;
		offset += 3;
		boolean showPhonetic = false;
	}

	private int rowNum;

	private int colNum;

	private int styleIdx;

	private boolean showPhonetic;

	public void reset(int rowNum, int colNum, int styleIdx, boolean showPhonetic) {
		this.rowNum = rowNum;
		this.colNum = colNum;
		this.styleIdx = styleIdx;
		this.showPhonetic = showPhonetic;
	}

	int getColNum() {
		return colNum;
	}

	int getStyleIdx() {
		return styleIdx;
	}
}

