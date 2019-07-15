

import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.EscherArrayProperty;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.hslf.usermodel.HSLFGroupShape;
import org.apache.poi.hslf.usermodel.HSLFLine;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFShapeContainer;
import org.apache.poi.hslf.usermodel.HSLFSheet;
import org.apache.poi.hslf.usermodel.HSLFTableCell;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.TableShape;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.Units;


public final class HSLFTable extends HSLFGroupShape implements HSLFShapeContainer , TableShape<HSLFShape, HSLFTextParagraph> {
	protected static final int BORDERS_ALL = 5;

	protected static final int BORDERS_OUTSIDE = 6;

	protected static final int BORDERS_INSIDE = 7;

	protected static final int BORDERS_NONE = 8;

	protected HSLFTableCell[][] cells;

	private int columnCount = -1;

	protected HSLFTable(int numRows, int numCols) {
		this(numRows, numCols, null);
	}

	protected HSLFTable(int numRows, int numCols, ShapeContainer<HSLFShape, HSLFTextParagraph> parent) {
		super(parent);
		if (numRows < 1) {
			throw new IllegalArgumentException("The number of rows must be greater than 1");
		}
		if (numCols < 1) {
			throw new IllegalArgumentException("The number of columns must be greater than 1");
		}
		double x = 0;
		double y = 0;
		double tblWidth = 0;
		double tblHeight = 0;
		cells = new HSLFTableCell[numRows][numCols];
		for (int i = 0; i < (cells.length); i++) {
			x = 0;
			for (int j = 0; j < (cells[i].length); j++) {
			}
		}
		tblWidth = x;
		tblHeight = y;
		setExteriorAnchor(new Rectangle2D.Double(0, 0, tblWidth, tblHeight));
		EscherContainerRecord spCont = ((EscherContainerRecord) (getSpContainer().getChild(0)));
		AbstractEscherOptRecord opt = new EscherOptRecord();
		opt.setRecordId(RecordTypes.EscherUserDefined.typeID);
		opt.addEscherProperty(new EscherSimpleProperty(EscherProperties.GROUPSHAPE__TABLEPROPERTIES, 1));
		EscherArrayProperty p = new EscherArrayProperty(((short) (16384 | (EscherProperties.GROUPSHAPE__TABLEROWPROPERTIES))), false, null);
		p.setSizeOfElements(4);
		p.setNumberOfElementsInArray(numRows);
		p.setNumberOfElementsInMemory(numRows);
		opt.addEscherProperty(p);
		spCont.addChildBefore(opt, RecordTypes.EscherClientAnchor.typeID);
	}

	protected HSLFTable(EscherContainerRecord escherRecord, ShapeContainer<HSLFShape, HSLFTextParagraph> parent) {
		super(escherRecord, parent);
	}

	@Override
	public HSLFTableCell getCell(int row, int col) {
		if ((row < 0) || ((cells.length) <= row)) {
			return null;
		}
		HSLFTableCell[] r = cells[row];
		if (((r == null) || (col < 0)) || ((r.length) <= col)) {
			return null;
		}
		return r[col];
	}

	@Override
	public int getNumberOfColumns() {
		if ((columnCount) == (-1)) {
			for (HSLFTableCell[] hc : cells) {
				if (hc != null) {
					columnCount = Math.max(columnCount, hc.length);
				}
			}
		}
		return columnCount;
	}

	@Override
	public int getNumberOfRows() {
		return cells.length;
	}

	@Override
	protected void afterInsert(HSLFSheet sh) {
		super.afterInsert(sh);
		Set<HSLFLine> lineSet = new HashSet<>();
		for (HSLFTableCell[] row : cells) {
			for (HSLFTableCell c : row) {
				addShape(c);
			}
		}
		for (HSLFLine l : lineSet) {
			addShape(l);
		}
		updateRowHeightsProperty();
	}

	private void cellListToArray() {
		List<HSLFTableCell> htc = new ArrayList<>();
		for (HSLFShape h : getShapes()) {
			if (h instanceof HSLFTableCell) {
				htc.add(((HSLFTableCell) (h)));
			}
		}
		if (htc.isEmpty()) {
			throw new IllegalStateException("HSLFTable without HSLFTableCells");
		}
		SortedSet<Double> colSet = new TreeSet<>();
		SortedSet<Double> rowSet = new TreeSet<>();
		for (HSLFTableCell sh : htc) {
			Rectangle2D anchor = sh.getAnchor();
			colSet.add(anchor.getX());
			rowSet.add(anchor.getY());
		}
		cells = new HSLFTableCell[rowSet.size()][colSet.size()];
		List<Double> colLst = new ArrayList<>(colSet);
		List<Double> rowLst = new ArrayList<>(rowSet);
		for (HSLFTableCell sh : htc) {
			Rectangle2D anchor = sh.getAnchor();
			int row = rowLst.indexOf(anchor.getY());
			int col = colLst.indexOf(anchor.getX());
			assert (row != (-1)) && (col != (-1));
			cells[row][col] = sh;
			int gridSpan = calcSpan(colLst, anchor.getWidth(), col);
			int rowSpan = calcSpan(rowLst, anchor.getHeight(), row);
		}
	}

	private int calcSpan(List<Double> spaces, double totalSpace, int idx) {
		int span = 1;
		ListIterator<Double> li = spaces.listIterator(idx);
		double start = li.next();
		while ((li.hasNext()) && (((li.next()) - start) < totalSpace)) {
			span++;
		} 
		return span;
	}

	static class LineRect {
		final HSLFLine l;

		final double lx1;

		final double lx2;

		final double ly1;

		final double ly2;

		LineRect(HSLFLine l) {
			this.l = l;
			Rectangle2D r = l.getAnchor();
			lx1 = r.getMinX();
			lx2 = r.getMaxX();
			ly1 = r.getMinY();
			ly2 = r.getMaxY();
		}

		int leftFit(double x1, double x2, double y1, double y2) {
			return ((int) ((((Math.abs((x1 - (lx1)))) + (Math.abs((y1 - (ly1))))) + (Math.abs((x1 - (lx2))))) + (Math.abs((y2 - (ly2))))));
		}

		int topFit(double x1, double x2, double y1, double y2) {
			return ((int) ((((Math.abs((x1 - (lx1)))) + (Math.abs((y1 - (ly1))))) + (Math.abs((x2 - (lx2))))) + (Math.abs((y1 - (ly2))))));
		}

		int rightFit(double x1, double x2, double y1, double y2) {
			return ((int) ((((Math.abs((x2 - (lx1)))) + (Math.abs((y1 - (ly1))))) + (Math.abs((x2 - (lx2))))) + (Math.abs((y2 - (ly2))))));
		}

		int bottomFit(double x1, double x2, double y1, double y2) {
			return ((int) ((((Math.abs((x1 - (lx1)))) + (Math.abs((y2 - (ly1))))) + (Math.abs((x2 - (lx2))))) + (Math.abs((y2 - (ly2))))));
		}
	}

	private void fitLinesToCells() {
		List<HSLFTable.LineRect> lines = new ArrayList<>();
		for (HSLFShape h : getShapes()) {
			if (h instanceof HSLFLine) {
				lines.add(new HSLFTable.LineRect(((HSLFLine) (h))));
			}
		}
		final int threshold = 5;
		for (HSLFTableCell[] tca : cells) {
			for (HSLFTableCell tc : tca) {
				if (tc == null) {
					continue;
				}
				final Rectangle2D cellAnchor = tc.getAnchor();
				final double x1 = cellAnchor.getMinX();
				final double x2 = cellAnchor.getMaxX();
				final double y1 = cellAnchor.getMinY();
				final double y2 = cellAnchor.getMaxY();
				HSLFTable.LineRect lline = null;
				HSLFTable.LineRect tline = null;
				HSLFTable.LineRect rline = null;
				HSLFTable.LineRect bline = null;
				int lfit = Integer.MAX_VALUE;
				int tfit = Integer.MAX_VALUE;
				int rfit = Integer.MAX_VALUE;
				int bfit = Integer.MAX_VALUE;
				for (HSLFTable.LineRect lr : lines) {
					int lfitx = lr.leftFit(x1, x2, y1, y2);
					if (lfitx < lfit) {
						lfit = lfitx;
						lline = lr;
					}
					int tfitx = lr.topFit(x1, x2, y1, y2);
					if (tfitx < tfit) {
						tfit = tfitx;
						tline = lr;
					}
					int rfitx = lr.rightFit(x1, x2, y1, y2);
					if (rfitx < rfit) {
						rfit = rfitx;
						rline = lr;
					}
					int bfitx = lr.bottomFit(x1, x2, y1, y2);
					if (bfitx < bfit) {
						bfit = bfitx;
						bline = lr;
					}
				}
				if ((lfit < threshold) && (lline != null)) {
				}
				if ((tfit < threshold) && (tline != null)) {
				}
				if ((rfit < threshold) && (rline != null)) {
				}
				if ((bfit < threshold) && (bline != null)) {
				}
			}
		}
	}

	protected void initTable() {
		cellListToArray();
		fitLinesToCells();
	}

	@Override
	public void setSheet(HSLFSheet sheet) {
		super.setSheet(sheet);
		if ((cells) == null) {
			initTable();
		}else {
			for (HSLFTableCell[] cols : cells) {
				for (HSLFTableCell col : cols) {
					col.setSheet(sheet);
				}
			}
		}
	}

	@Override
	public double getRowHeight(int row) {
		if ((row < 0) || (row >= (cells.length))) {
			throw new IllegalArgumentException((((("Row index '" + row) + "' is not within range [0-") + ((cells.length) - 1)) + "]"));
		}
		return cells[row][0].getAnchor().getHeight();
	}

	@Override
	public void setRowHeight(int row, final double height) {
		if ((row < 0) || (row >= (cells.length))) {
			throw new IllegalArgumentException((((("Row index '" + row) + "' is not within range [0-") + ((cells.length) - 1)) + "]"));
		}
		AbstractEscherOptRecord opt = getEscherChild(RecordTypes.EscherUserDefined.typeID);
		EscherArrayProperty p = opt.lookup(EscherProperties.GROUPSHAPE__TABLEROWPROPERTIES);
		byte[] masterBytes = p.getElement(row);
		double currentHeight = Units.masterToPoints(LittleEndian.getInt(masterBytes, 0));
		LittleEndian.putInt(masterBytes, 0, Units.pointsToMaster(height));
		p.setElement(row, masterBytes);
		double dy = height - currentHeight;
		for (int i = row; i < (cells.length); i++) {
			for (HSLFTableCell c : cells[i]) {
				if (c == null) {
					continue;
				}
				Rectangle2D anchor = c.getAnchor();
				if (i == row) {
					anchor.setRect(anchor.getX(), anchor.getY(), anchor.getWidth(), height);
				}else {
					anchor.setRect(anchor.getX(), ((anchor.getY()) + dy), anchor.getWidth(), anchor.getHeight());
				}
				c.setAnchor(anchor);
			}
		}
		Rectangle2D tblanchor = getAnchor();
		tblanchor.setRect(tblanchor.getX(), tblanchor.getY(), tblanchor.getWidth(), ((tblanchor.getHeight()) + dy));
		setExteriorAnchor(tblanchor);
	}

	@Override
	public double getColumnWidth(int col) {
		if ((col < 0) || (col >= (cells[0].length))) {
			throw new IllegalArgumentException((((("Column index '" + col) + "' is not within range [0-") + ((cells[0].length) - 1)) + "]"));
		}
		return cells[0][col].getAnchor().getWidth();
	}

	@Override
	public void setColumnWidth(int col, final double width) {
		if ((col < 0) || (col >= (cells[0].length))) {
			throw new IllegalArgumentException((((("Column index '" + col) + "' is not within range [0-") + ((cells[0].length) - 1)) + "]"));
		}
		double currentWidth = cells[0][col].getAnchor().getWidth();
		double dx = width - currentWidth;
		for (HSLFTableCell[] cols : cells) {
			Rectangle2D anchor = cols[col].getAnchor();
			anchor.setRect(anchor.getX(), anchor.getY(), width, anchor.getHeight());
			cols[col].setAnchor(anchor);
			if (col < ((cols.length) - 1)) {
				for (int j = col + 1; j < (cols.length); j++) {
					anchor = cols[j].getAnchor();
					anchor.setRect(((anchor.getX()) + dx), anchor.getY(), anchor.getWidth(), anchor.getHeight());
					cols[j].setAnchor(anchor);
				}
			}
		}
		Rectangle2D tblanchor = getAnchor();
		tblanchor.setRect(tblanchor.getX(), tblanchor.getY(), ((tblanchor.getWidth()) + dx), tblanchor.getHeight());
		setExteriorAnchor(tblanchor);
	}

	protected HSLFTableCell getRelativeCell(HSLFTableCell origin, int row, int col) {
		int thisRow = 0;
		int thisCol = 0;
		boolean found = false;
		outer : for (HSLFTableCell[] tca : cells) {
			thisCol = 0;
			for (HSLFTableCell tc : tca) {
				if (tc == origin) {
					found = true;
					break outer;
				}
				thisCol++;
			}
			thisRow++;
		}
		int otherRow = thisRow + row;
		int otherCol = thisCol + col;
		return (((found && (0 <= otherRow)) && (otherRow < (cells.length))) && (0 <= otherCol)) && (otherCol < (cells[otherRow].length)) ? cells[otherRow][otherCol] : null;
	}

	@Override
	protected void moveAndScale(Rectangle2D anchorDest) {
		super.moveAndScale(anchorDest);
		updateRowHeightsProperty();
	}

	private void updateRowHeightsProperty() {
		AbstractEscherOptRecord opt = getEscherChild(RecordTypes.EscherUserDefined.typeID);
		EscherArrayProperty p = opt.lookup(EscherProperties.GROUPSHAPE__TABLEROWPROPERTIES);
		byte[] val = new byte[4];
		for (int rowIdx = 0; rowIdx < (cells.length); rowIdx++) {
			int rowHeight = Units.pointsToMaster(cells[rowIdx][0].getAnchor().getHeight());
			LittleEndian.putInt(val, 0, rowHeight);
			p.setElement(rowIdx, val);
		}
	}
}

