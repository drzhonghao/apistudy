import org.apache.poi.xdgf.usermodel.section.geometry.GeometryRow;
import org.apache.poi.xdgf.usermodel.section.geometry.*;


import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xdgf.usermodel.XDGFCell;
import org.apache.poi.xdgf.usermodel.XDGFShape;

import com.microsoft.schemas.office.visio.x2012.main.CellType;
import com.microsoft.schemas.office.visio.x2012.main.RowType;

public class RelCubBezTo implements GeometryRow {

    RelCubBezTo _master;

    // The x-coordinate of the ending vertex of a cubic Bezier curve relative to
    // the width of the shape.
    Double x;

    // The y-coordinate of the ending vertex of a cubic Bezier curve relative to
    // the height of the shape.
    Double y;

    // The x-coordinate of the curve's beginning control point relative to the
    // shape's width; a point on the arc. The control point is best located
    // between the beginning and ending vertices of the arc.
    Double a;

    // The y-coordinate of a curve's beginning control point relative to the
    // shape's height.
    Double b;

    // The x-coordinate of the curve's ending control point relative to the
    // shape's width; a point on the arc. The control point is best located
    // between the beginning control point and ending vertices of the arc.
    Double c;

    // The y-coordinate of a curve's ending control point relative to the
    // shape's height.
    Double d;

    Boolean deleted;

    // TODO: support formulas

    public RelCubBezTo(RowType row) {

        if (row.isSetDel())
            deleted = row.getDel();

        for (CellType cell : row.getCellArray()) {
            String cellName = cell.getN();

            if (cellName.equals("X")) {
                x = XDGFCell.parseDoubleValue(cell);
            } else if (cellName.equals("Y")) {
                y = XDGFCell.parseDoubleValue(cell);
            } else if (cellName.equals("A")) {
                a = XDGFCell.parseDoubleValue(cell);
            } else if (cellName.equals("B")) {
                b = XDGFCell.parseDoubleValue(cell);
            } else if (cellName.equals("C")) {
                c = XDGFCell.parseDoubleValue(cell);
            } else if (cellName.equals("D")) {
                d = XDGFCell.parseDoubleValue(cell);
            } else {
                throw new POIXMLException("Invalid cell '" + cellName
                        + "' in RelCubBezTo row");
            }
        }
    }

    public boolean getDel() {
        if (deleted != null)
            return deleted;

        return _master != null && _master.getDel();
    }

    public Double getX() {
        return x == null ? _master.x : x;
    }

    public Double getY() {
        return y == null ? _master.y : y;
    }

    public Double getA() {
        return a == null ? _master.a : a;
    }

    public Double getB() {
        return b == null ? _master.b : b;
    }

    public Double getC() {
        return c == null ? _master.c : c;
    }

    public Double getD() {
        return d == null ? _master.d : d;
    }

    @Override
    public void setupMaster(GeometryRow row) {
        _master = (RelCubBezTo) row;
    }

    @Override
    public void addToPath(java.awt.geom.Path2D.Double path, XDGFShape parent) {

        if (getDel())
            return;

        double w = parent.getWidth();
        double h = parent.getHeight();

        path.curveTo(getA() * w, getB() * h, getC() * w, getD() * h,
                getX() * w, getY() * h);
    }
}
