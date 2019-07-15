

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.hslf.usermodel.HSLFGroupShape;
import org.apache.poi.hslf.usermodel.HSLFLine;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSimpleShape;
import org.apache.poi.hslf.usermodel.HSLFTable;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.sl.draw.DrawPaint;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.StrokeStyle;
import org.apache.poi.sl.usermodel.TableCell;

import static org.apache.poi.sl.usermodel.TableCell.BorderEdge.bottom;
import static org.apache.poi.sl.usermodel.TableCell.BorderEdge.left;
import static org.apache.poi.sl.usermodel.TableCell.BorderEdge.right;
import static org.apache.poi.sl.usermodel.TableCell.BorderEdge.top;


public final class HSLFTableCell extends HSLFTextBox implements TableCell<HSLFShape, HSLFTextParagraph> {
	protected static final int DEFAULT_WIDTH = 100;

	protected static final int DEFAULT_HEIGHT = 40;

	HSLFLine borderLeft;

	HSLFLine borderRight;

	HSLFLine borderTop;

	HSLFLine borderBottom;

	private int gridSpan = 1;

	private int rowSpan = 1;

	protected HSLFTableCell(EscherContainerRecord escherRecord, HSLFTable parent) {
		super(escherRecord, parent);
	}

	public HSLFTableCell(HSLFTable parent) {
		super(parent);
		setShapeType(ShapeType.RECT);
	}

	@Override
	protected EscherContainerRecord createSpContainer(boolean isChild) {
		EscherContainerRecord ecr = super.createSpContainer(isChild);
		AbstractEscherOptRecord opt = getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.TEXT__TEXTID, 0);
		HSLFShape.setEscherProperty(opt, EscherProperties.TEXT__SIZE_TEXT_TO_FIT_SHAPE, 131072);
		HSLFShape.setEscherProperty(opt, EscherProperties.FILL__NOFILLHITTEST, 1376257);
		HSLFShape.setEscherProperty(opt, EscherProperties.SHADOWSTYLE__SHADOWOBSURED, 131072);
		HSLFShape.setEscherProperty(opt, EscherProperties.PROTECTION__LOCKAGAINSTGROUPING, 262144);
		return ecr;
	}

	private void anchorBorder(TableCell.BorderEdge edge, final HSLFLine line) {
		if (line == null) {
			return;
		}
		Rectangle2D cellAnchor = getAnchor();
		double x;
		double y;
		double w;
		double h;
		switch (edge) {
			case top :
				x = cellAnchor.getX();
				y = cellAnchor.getY();
				w = cellAnchor.getWidth();
				h = 0;
				break;
			case right :
				x = (cellAnchor.getX()) + (cellAnchor.getWidth());
				y = cellAnchor.getY();
				w = 0;
				h = cellAnchor.getHeight();
				break;
			case bottom :
				x = cellAnchor.getX();
				y = (cellAnchor.getY()) + (cellAnchor.getHeight());
				w = cellAnchor.getWidth();
				h = 0;
				break;
			case left :
				x = cellAnchor.getX();
				y = cellAnchor.getY();
				w = 0;
				h = cellAnchor.getHeight();
				break;
			default :
				throw new IllegalArgumentException();
		}
		line.setAnchor(new Rectangle2D.Double(x, y, w, h));
	}

	@Override
	public void setAnchor(Rectangle2D anchor) {
		super.setAnchor(anchor);
		anchorBorder(top, borderTop);
		anchorBorder(right, borderRight);
		anchorBorder(bottom, borderBottom);
		anchorBorder(left, borderLeft);
	}

	@Override
	public StrokeStyle getBorderStyle(final TableCell.BorderEdge edge) {
		final Double width = getBorderWidth(edge);
		return width == null ? null : new StrokeStyle() {
			@Override
			public PaintStyle getPaint() {
				return DrawPaint.createSolidPaint(getBorderColor(edge));
			}

			@Override
			public StrokeStyle.LineCap getLineCap() {
				return null;
			}

			@Override
			public StrokeStyle.LineDash getLineDash() {
				return getBorderDash(edge);
			}

			@Override
			public StrokeStyle.LineCompound getLineCompound() {
				return getBorderCompound(edge);
			}

			@Override
			public double getLineWidth() {
				return width;
			}
		};
	}

	@Override
	public void setBorderStyle(TableCell.BorderEdge edge, StrokeStyle style) {
		if (style == null) {
			throw new IllegalArgumentException("StrokeStyle needs to be specified.");
		}
		StrokeStyle.LineCompound compound = style.getLineCompound();
		if (compound != null) {
			setBorderCompound(edge, compound);
		}
		StrokeStyle.LineDash dash = style.getLineDash();
		if (dash != null) {
			setBorderDash(edge, dash);
		}
		double width = style.getLineWidth();
		setBorderWidth(edge, width);
	}

	public Double getBorderWidth(TableCell.BorderEdge edge) {
		HSLFLine l;
		switch (edge) {
			case bottom :
				l = borderBottom;
				break;
			case top :
				l = borderTop;
				break;
			case right :
				l = borderRight;
				break;
			case left :
				l = borderLeft;
				break;
			default :
				throw new IllegalArgumentException();
		}
		return l == null ? null : l.getLineWidth();
	}

	@Override
	public void setBorderWidth(TableCell.BorderEdge edge, double width) {
		HSLFLine l = addLine(edge);
		l.setLineWidth(width);
	}

	public Color getBorderColor(TableCell.BorderEdge edge) {
		HSLFLine l;
		switch (edge) {
			case bottom :
				l = borderBottom;
				break;
			case top :
				l = borderTop;
				break;
			case right :
				l = borderRight;
				break;
			case left :
				l = borderLeft;
				break;
			default :
				throw new IllegalArgumentException();
		}
		return l == null ? null : l.getLineColor();
	}

	@Override
	public void setBorderColor(TableCell.BorderEdge edge, Color color) {
		if ((edge == null) || (color == null)) {
			throw new IllegalArgumentException("BorderEdge and/or Color need to be specified.");
		}
		HSLFLine l = addLine(edge);
		l.setLineColor(color);
	}

	public StrokeStyle.LineDash getBorderDash(TableCell.BorderEdge edge) {
		HSLFLine l;
		switch (edge) {
			case bottom :
				l = borderBottom;
				break;
			case top :
				l = borderTop;
				break;
			case right :
				l = borderRight;
				break;
			case left :
				l = borderLeft;
				break;
			default :
				throw new IllegalArgumentException();
		}
		return l == null ? null : l.getLineDash();
	}

	@Override
	public void setBorderDash(TableCell.BorderEdge edge, StrokeStyle.LineDash dash) {
		if ((edge == null) || (dash == null)) {
			throw new IllegalArgumentException("BorderEdge and/or LineDash need to be specified.");
		}
		HSLFLine l = addLine(edge);
		l.setLineDash(dash);
	}

	public StrokeStyle.LineCompound getBorderCompound(TableCell.BorderEdge edge) {
		HSLFLine l;
		switch (edge) {
			case bottom :
				l = borderBottom;
				break;
			case top :
				l = borderTop;
				break;
			case right :
				l = borderRight;
				break;
			case left :
				l = borderLeft;
				break;
			default :
				throw new IllegalArgumentException();
		}
		return l == null ? null : l.getLineCompound();
	}

	@Override
	public void setBorderCompound(TableCell.BorderEdge edge, StrokeStyle.LineCompound compound) {
		if ((edge == null) || (compound == null)) {
			throw new IllegalArgumentException("BorderEdge and/or LineCompound need to be specified.");
		}
		HSLFLine l = addLine(edge);
		l.setLineCompound(compound);
	}

	protected HSLFLine addLine(TableCell.BorderEdge edge) {
		switch (edge) {
			case bottom :
				{
					if ((borderBottom) == null) {
						borderBottom = createBorder(edge);
						HSLFTableCell c = getSiblingCell(1, 0);
						if (c != null) {
							assert (c.borderTop) == null;
							c.borderTop = borderBottom;
						}
					}
					return borderBottom;
				}
			case top :
				{
					if ((borderTop) == null) {
						borderTop = createBorder(edge);
						HSLFTableCell c = getSiblingCell((-1), 0);
						if (c != null) {
							assert (c.borderBottom) == null;
							c.borderBottom = borderTop;
						}
					}
					return borderTop;
				}
			case right :
				{
					if ((borderRight) == null) {
						borderRight = createBorder(edge);
						HSLFTableCell c = getSiblingCell(0, 1);
						if (c != null) {
							assert (c.borderLeft) == null;
							c.borderLeft = borderRight;
						}
					}
					return borderRight;
				}
			case left :
				{
					if ((borderLeft) == null) {
						borderLeft = createBorder(edge);
						HSLFTableCell c = getSiblingCell(0, (-1));
						if (c != null) {
							assert (c.borderRight) == null;
							c.borderRight = borderLeft;
						}
					}
					return borderLeft;
				}
			default :
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void removeBorder(TableCell.BorderEdge edge) {
		switch (edge) {
			case bottom :
				{
					if ((borderBottom) == null)
						break;

					getParent().removeShape(borderBottom);
					borderBottom = null;
					HSLFTableCell c = getSiblingCell(1, 0);
					if (c != null) {
						c.borderTop = null;
					}
					break;
				}
			case top :
				{
					if ((borderTop) == null)
						break;

					getParent().removeShape(borderTop);
					borderTop = null;
					HSLFTableCell c = getSiblingCell((-1), 0);
					if (c != null) {
						c.borderBottom = null;
					}
					break;
				}
			case right :
				{
					if ((borderRight) == null)
						break;

					getParent().removeShape(borderRight);
					borderRight = null;
					HSLFTableCell c = getSiblingCell(0, 1);
					if (c != null) {
						c.borderLeft = null;
					}
					break;
				}
			case left :
				{
					if ((borderLeft) == null)
						break;

					getParent().removeShape(borderLeft);
					borderLeft = null;
					HSLFTableCell c = getSiblingCell(0, (-1));
					if (c != null) {
						c.borderRight = null;
					}
					break;
				}
			default :
				throw new IllegalArgumentException();
		}
	}

	protected HSLFTableCell getSiblingCell(int row, int col) {
		return null;
	}

	private HSLFLine createBorder(TableCell.BorderEdge edge) {
		HSLFTable table = getParent();
		HSLFLine line = new HSLFLine(table);
		table.addShape(line);
		AbstractEscherOptRecord opt = getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.GEOMETRY__SHAPEPATH, (-1));
		HSLFShape.setEscherProperty(opt, EscherProperties.GEOMETRY__FILLOK, (-1));
		HSLFShape.setEscherProperty(opt, EscherProperties.SHADOWSTYLE__SHADOWOBSURED, 131072);
		HSLFShape.setEscherProperty(opt, EscherProperties.THREED__LIGHTFACE, 524288);
		anchorBorder(edge, line);
		return line;
	}

	protected void applyLineProperties(TableCell.BorderEdge edge, HSLFLine other) {
		HSLFLine line = addLine(edge);
		line.setLineWidth(other.getLineWidth());
		line.setLineColor(other.getLineColor());
	}

	@Override
	public HSLFTable getParent() {
		return ((HSLFTable) (super.getParent()));
	}

	protected void setGridSpan(int gridSpan) {
		this.gridSpan = gridSpan;
	}

	protected void setRowSpan(int rowSpan) {
		this.rowSpan = rowSpan;
	}

	@Override
	public int getGridSpan() {
		return gridSpan;
	}

	@Override
	public int getRowSpan() {
		return rowSpan;
	}

	@Override
	public boolean isMerged() {
		return false;
	}
}

