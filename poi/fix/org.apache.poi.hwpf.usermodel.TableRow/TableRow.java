

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hwpf.model.types.TAPAbstractType;
import org.apache.poi.hwpf.sprm.SprmBuffer;
import org.apache.poi.hwpf.sprm.TableSprmUncompressor;
import org.apache.poi.hwpf.usermodel.BorderCode;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableCellDescriptor;
import org.apache.poi.hwpf.usermodel.TableProperties;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public final class TableRow extends Range {
	private static final POILogger logger = POILogFactory.getLogger(TableRow.class);

	private static final short SPRM_DXAGAPHALF = ((short) (38402));

	private static final short SPRM_DYAROWHEIGHT = ((short) (37895));

	private static final short SPRM_FCANTSPLIT = 13315;

	private static final short SPRM_FTABLEHEADER = 13316;

	private static final short SPRM_TJC = 21504;

	private static final char TABLE_CELL_MARK = '\u0007';

	private TableCell[] _cells;

	private boolean _cellsFound;

	int _levelNum;

	private SprmBuffer _papx;

	private TableProperties _tprops;

	public TableRow(int startIdxInclusive, int endIdxExclusive, Table parent, int levelNum) {
		super(startIdxInclusive, endIdxExclusive, parent);
		Paragraph last = getParagraph(((numParagraphs()) - 1));
		_tprops = TableSprmUncompressor.uncompressTAP(_papx);
		_levelNum = levelNum;
		initCells();
	}

	public boolean cantSplit() {
		return _tprops.getFCantSplit();
	}

	public BorderCode getBarBorder() {
		throw new UnsupportedOperationException("not applicable for TableRow");
	}

	public BorderCode getBottomBorder() {
		return _tprops.getBrcBottom();
	}

	public TableCell getCell(int index) {
		initCells();
		return _cells[index];
	}

	public int getGapHalf() {
		return _tprops.getDxaGapHalf();
	}

	public BorderCode getHorizontalBorder() {
		return _tprops.getBrcHorizontal();
	}

	public BorderCode getLeftBorder() {
		return _tprops.getBrcLeft();
	}

	public BorderCode getRightBorder() {
		return _tprops.getBrcRight();
	}

	public int getRowHeight() {
		return _tprops.getDyaRowHeight();
	}

	public int getRowJustification() {
		return _tprops.getJc();
	}

	public BorderCode getTopBorder() {
		return _tprops.getBrcTop();
	}

	public BorderCode getVerticalBorder() {
		return _tprops.getBrcVertical();
	}

	private void initCells() {
		if (_cellsFound)
			return;

		final short expectedCellsCount = _tprops.getItcMac();
		int lastCellStart = 0;
		List<TableCell> cells = new ArrayList<>((expectedCellsCount + 1));
		for (int p = 0; p < (numParagraphs()); p++) {
			Paragraph paragraph = getParagraph(p);
			String s = paragraph.text();
			if (((((s.length()) > 0) && ((s.charAt(((s.length()) - 1))) == (TableRow.TABLE_CELL_MARK))) || (paragraph.isEmbeddedCellMark())) && ((paragraph.getTableLevel()) == (_levelNum))) {
				TableCellDescriptor tableCellDescriptor = (((_tprops.getRgtc()) != null) && ((_tprops.getRgtc().length) > (cells.size()))) ? _tprops.getRgtc()[cells.size()] : new TableCellDescriptor();
				final short leftEdge = (((_tprops.getRgdxaCenter()) != null) && ((_tprops.getRgdxaCenter().length) > (cells.size()))) ? _tprops.getRgdxaCenter()[cells.size()] : 0;
				final short rightEdge = (((_tprops.getRgdxaCenter()) != null) && ((_tprops.getRgdxaCenter().length) > ((cells.size()) + 1))) ? _tprops.getRgdxaCenter()[((cells.size()) + 1)] : 0;
				lastCellStart = p + 1;
			}
		}
		if (lastCellStart < ((numParagraphs()) - 1)) {
			TableCellDescriptor tableCellDescriptor = (((_tprops.getRgtc()) != null) && ((_tprops.getRgtc().length) > (cells.size()))) ? _tprops.getRgtc()[cells.size()] : new TableCellDescriptor();
			final short leftEdge = (((_tprops.getRgdxaCenter()) != null) && ((_tprops.getRgdxaCenter().length) > (cells.size()))) ? _tprops.getRgdxaCenter()[cells.size()] : 0;
			final short rightEdge = (((_tprops.getRgdxaCenter()) != null) && ((_tprops.getRgdxaCenter().length) > ((cells.size()) + 1))) ? _tprops.getRgdxaCenter()[((cells.size()) + 1)] : 0;
		}
		if (!(cells.isEmpty())) {
			TableCell lastCell = cells.get(((cells.size()) - 1));
			if (((lastCell.numParagraphs()) == 1) && (lastCell.getParagraph(0).isTableRowEnd())) {
				cells.remove(((cells.size()) - 1));
			}
		}
		if ((cells.size()) != expectedCellsCount) {
			TableRow.logger.log(POILogger.WARN, ((((((("Number of found table cells (" + (cells.size())) + ") for table row [") + (getStartOffset())) + "c; ") + (getEndOffset())) + "c] not equals to stored property value ") + expectedCellsCount));
			_tprops.setItcMac(((short) (cells.size())));
		}
		_cells = cells.toArray(new TableCell[cells.size()]);
		_cellsFound = true;
	}

	public boolean isTableHeader() {
		return _tprops.getFTableHeader();
	}

	public int numCells() {
		initCells();
		return _cells.length;
	}

	@Override
	protected void reset() {
		_cellsFound = false;
	}

	public void setCantSplit(boolean cantSplit) {
		_tprops.setFCantSplit(cantSplit);
		_papx.updateSprm(TableRow.SPRM_FCANTSPLIT, ((byte) (cantSplit ? 1 : 0)));
	}

	public void setGapHalf(int dxaGapHalf) {
		_tprops.setDxaGapHalf(dxaGapHalf);
		_papx.updateSprm(TableRow.SPRM_DXAGAPHALF, ((short) (dxaGapHalf)));
	}

	public void setRowHeight(int dyaRowHeight) {
		_tprops.setDyaRowHeight(dyaRowHeight);
		_papx.updateSprm(TableRow.SPRM_DYAROWHEIGHT, ((short) (dyaRowHeight)));
	}

	public void setRowJustification(int jc) {
		_tprops.setJc(((short) (jc)));
		_papx.updateSprm(TableRow.SPRM_TJC, ((short) (jc)));
	}

	public void setTableHeader(boolean tableHeader) {
		_tprops.setFTableHeader(tableHeader);
		_papx.updateSprm(TableRow.SPRM_FTABLEHEADER, ((byte) (tableHeader ? 1 : 0)));
	}
}

