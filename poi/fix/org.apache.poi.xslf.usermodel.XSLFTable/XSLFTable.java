

import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.sl.draw.DrawFactory;
import org.apache.poi.sl.draw.DrawTableShape;
import org.apache.poi.sl.draw.DrawTextShape;
import org.apache.poi.sl.usermodel.TableShape;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFGraphicFrame;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTableStyle;
import org.apache.poi.xslf.usermodel.XSLFTableStyles;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObjectData;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObjectFrameLocking;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualGraphicFrameProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTable;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableCol;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableGrid;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableRow;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;
import org.openxmlformats.schemas.presentationml.x2006.main.CTApplicationNonVisualDrawingProps;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGraphicalObjectFrame;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGraphicalObjectFrameNonVisual;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTTable.Factory.newInstance;


public class XSLFTable extends XSLFGraphicFrame implements Iterable<XSLFTableRow> , TableShape<XSLFShape, XSLFTextParagraph> {
	static final String TABLE_URI = "http://schemas.openxmlformats.org/drawingml/2006/table";

	private CTTable _table;

	private List<XSLFTableRow> _rows;

	@Override
	public XSLFTableCell getCell(int row, int col) {
		List<XSLFTableRow> rows = getRows();
		if ((row < 0) || ((rows.size()) <= row)) {
			return null;
		}
		XSLFTableRow r = rows.get(row);
		if (r == null) {
			return null;
		}
		List<XSLFTableCell> cells = r.getCells();
		if ((col < 0) || ((cells.size()) <= col)) {
			return null;
		}
		return cells.get(col);
	}

	@org.apache.poi.util.Internal
	public CTTable getCTTable() {
		return _table;
	}

	@Override
	public int getNumberOfColumns() {
		return _table.getTblGrid().sizeOfGridColArray();
	}

	@Override
	public int getNumberOfRows() {
		return _table.sizeOfTrArray();
	}

	@Override
	public double getColumnWidth(int idx) {
		return Units.toPoints(_table.getTblGrid().getGridColArray(idx).getW());
	}

	@Override
	public void setColumnWidth(int idx, double width) {
		_table.getTblGrid().getGridColArray(idx).setW(Units.toEMU(width));
	}

	@Override
	public double getRowHeight(int row) {
		return Units.toPoints(_table.getTrArray(row).getH());
	}

	@Override
	public void setRowHeight(int row, double height) {
		_table.getTrArray(row).setH(Units.toEMU(height));
	}

	public Iterator<XSLFTableRow> iterator() {
		return _rows.iterator();
	}

	public List<XSLFTableRow> getRows() {
		return Collections.unmodifiableList(_rows);
	}

	public XSLFTableRow addRow() {
		CTTableRow tr = _table.addNewTr();
		updateRowColIndexes();
		return null;
	}

	static CTGraphicalObjectFrame prototype(int shapeId) {
		CTGraphicalObjectFrame frame = CTGraphicalObjectFrame.Factory.newInstance();
		CTGraphicalObjectFrameNonVisual nvGr = frame.addNewNvGraphicFramePr();
		CTNonVisualDrawingProps cnv = nvGr.addNewCNvPr();
		cnv.setName(("Table " + shapeId));
		cnv.setId(shapeId);
		nvGr.addNewCNvGraphicFramePr().addNewGraphicFrameLocks().setNoGrp(true);
		nvGr.addNewNvPr();
		frame.addNewXfrm();
		CTGraphicalObjectData gr = frame.addNewGraphic().addNewGraphicData();
		XmlCursor grCur = gr.newCursor();
		grCur.toNextToken();
		CTTable tbl = newInstance();
		tbl.addNewTblPr();
		tbl.addNewTblGrid();
		XmlCursor tblCur = tbl.newCursor();
		tblCur.moveXmlContents(grCur);
		tblCur.dispose();
		grCur.dispose();
		gr.setUri(XSLFTable.TABLE_URI);
		return frame;
	}

	@SuppressWarnings("unused")
	public void mergeCells(int firstRow, int lastRow, int firstCol, int lastCol) {
		if (firstRow > lastRow) {
			throw new IllegalArgumentException(((("Cannot merge, first row > last row : " + firstRow) + " > ") + lastRow));
		}
		if (firstCol > lastCol) {
			throw new IllegalArgumentException(((("Cannot merge, first column > last column : " + firstCol) + " > ") + lastCol));
		}
		int rowSpan = (lastRow - firstRow) + 1;
		boolean mergeRowRequired = rowSpan > 1;
		int colSpan = (lastCol - firstCol) + 1;
		boolean mergeColumnRequired = colSpan > 1;
		for (int i = firstRow; i <= lastRow; i++) {
			XSLFTableRow row = _rows.get(i);
			for (int colPos = firstCol; colPos <= lastCol; colPos++) {
				XSLFTableCell cell = row.getCells().get(colPos);
				if (mergeRowRequired) {
					if (i == firstRow) {
					}else {
					}
				}
				if (mergeColumnRequired) {
					if (colPos == firstCol) {
					}else {
					}
				}
			}
		}
	}

	protected XSLFTableStyle getTableStyle() {
		CTTable tab = getCTTable();
		if ((!(tab.isSetTblPr())) || (!(tab.getTblPr().isSetTableStyleId()))) {
			return null;
		}
		String styleId = tab.getTblPr().getTableStyleId();
		XSLFTableStyles styles = getSheet().getSlideShow().getTableStyles();
		for (XSLFTableStyle style : styles.getStyles()) {
			if (style.getStyleId().equals(styleId)) {
				return style;
			}
		}
		return null;
	}

	void updateRowColIndexes() {
		int rowIdx = 0;
		for (XSLFTableRow xr : this) {
			int colIdx = 0;
			for (XSLFTableCell tc : xr) {
				colIdx++;
			}
			rowIdx++;
		}
	}

	void updateCellAnchor() {
		int rows = getNumberOfRows();
		int cols = getNumberOfColumns();
		double[] colWidths = new double[cols];
		double[] rowHeights = new double[rows];
		for (int row = 0; row < rows; row++) {
			rowHeights[row] = getRowHeight(row);
		}
		for (int col = 0; col < cols; col++) {
			colWidths[col] = getColumnWidth(col);
		}
		Rectangle2D tblAnc = getAnchor();
		DrawFactory df = DrawFactory.getInstance(null);
		double newY = tblAnc.getY();
		for (int row = 0; row < rows; row++) {
			double maxHeight = 0;
			for (int col = 0; col < cols; col++) {
				XSLFTableCell tc = getCell(row, col);
				if (((tc == null) || ((tc.getGridSpan()) != 1)) || ((tc.getRowSpan()) != 1)) {
					continue;
				}
				tc.setAnchor(new Rectangle2D.Double(0, 0, colWidths[col], 0));
				DrawTextShape dts = df.getDrawable(tc);
				maxHeight = Math.max(maxHeight, dts.getTextHeight());
			}
			rowHeights[row] = Math.max(rowHeights[row], maxHeight);
		}
		for (int row = 0; row < rows; row++) {
			double newX = tblAnc.getX();
			for (int col = 0; col < cols; col++) {
				Rectangle2D bounds = new Rectangle2D.Double(newX, newY, colWidths[col], rowHeights[row]);
				XSLFTableCell tc = getCell(row, col);
				if (tc != null) {
					tc.setAnchor(bounds);
					newX += (colWidths[col]) + (DrawTableShape.borderSize);
				}
			}
			newY += (rowHeights[row]) + (DrawTableShape.borderSize);
		}
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				XSLFTableCell tc = getCell(row, col);
				if (tc == null) {
					continue;
				}
				Rectangle2D mergedBounds = tc.getAnchor();
				for (int col2 = col + 1; col2 < (col + (tc.getGridSpan())); col2++) {
					assert col2 < cols;
					XSLFTableCell tc2 = getCell(row, col2);
					assert ((tc2.getGridSpan()) == 1) && ((tc2.getRowSpan()) == 1);
					mergedBounds.add(tc2.getAnchor());
				}
				for (int row2 = row + 1; row2 < (row + (tc.getRowSpan())); row2++) {
					assert row2 < rows;
					XSLFTableCell tc2 = getCell(row2, col);
					assert ((tc2.getGridSpan()) == 1) && ((tc2.getRowSpan()) == 1);
					mergedBounds.add(tc2.getAnchor());
				}
				tc.setAnchor(mergedBounds);
			}
		}
	}
}

