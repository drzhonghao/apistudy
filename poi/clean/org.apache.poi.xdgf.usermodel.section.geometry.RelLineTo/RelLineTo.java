import org.apache.poi.xdgf.usermodel.section.geometry.GeometryRow;
import org.apache.poi.xdgf.usermodel.section.geometry.*;


import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xdgf.usermodel.XDGFCell;
import org.apache.poi.xdgf.usermodel.XDGFShape;

import com.microsoft.schemas.office.visio.x2012.main.CellType;
import com.microsoft.schemas.office.visio.x2012.main.RowType;

/**
 * Contains x-and y-coordinates of the ending vertex of a straight line segment
 * relative to a shape's width and height.
 */
public class RelLineTo implements GeometryRow {

    RelLineTo _master;

    Double x;
    Double y;

    Boolean deleted;

    // TODO: support formulas

    public RelLineTo(RowType row) {

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
                        + "' in RelLineTo row");
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

    @Override
    public void setupMaster(GeometryRow row) {
        _master = (RelLineTo) row;
    }

    @Override
    public void addToPath(java.awt.geom.Path2D.Double path, XDGFShape parent) {

        if (getDel())
            return;

        path.lineTo(getX() * parent.getWidth(), getY() * parent.getHeight());
    }
}
