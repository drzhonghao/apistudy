import org.apache.poi.xdgf.usermodel.section.geometry.*;


import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xdgf.usermodel.XDGFCell;
import org.apache.poi.xdgf.usermodel.XDGFShape;

import com.microsoft.schemas.office.visio.x2012.main.CellType;
import com.microsoft.schemas.office.visio.x2012.main.RowType;

/**
 * Contains the x- and y-coordinates of the first vertex of a shape or the x-
 * and y-coordinates of the first vertex after a break in a path, relative to
 * the height and width of the shape.
 */
public class MoveTo implements GeometryRow {

    MoveTo _master;

    Double x;
    Double y;

    Boolean deleted;

    // TODO: support formulas

    public MoveTo(RowType row) {

        if (row.isSetDel())
            deleted = row.getDel();

        for (CellType cell : row.getCellArray()) {
            String cellName = cell.getN();

            if (cellName.equals("X")) {
                x = XDGFCell.parseDoubleValue(cell);
            } else if (cellName.equals("Y")) {
                y = XDGFCell.parseDoubleValue(cell);
            } else {
                throw new POIXMLException("Invalid cell '" + cellName
                        + "' in MoveTo row");
            }
        }
    }

    @Override
    public String toString() {
        return "MoveTo: x=" + getX() + "; y=" + getY();
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

    @Override
    public void setupMaster(GeometryRow row) {
        _master = (MoveTo) row;
    }

    @Override
    public void addToPath(java.awt.geom.Path2D.Double path, XDGFShape parent) {
        if (getDel())
            return;
        path.moveTo(getX(), getY());
    }
}
