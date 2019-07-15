import org.apache.poi.xdgf.usermodel.section.geometry.GeometryRow;
import org.apache.poi.xdgf.usermodel.section.geometry.*;


import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.util.NotImplemented;
import org.apache.poi.xdgf.usermodel.XDGFCell;
import org.apache.poi.xdgf.usermodel.XDGFShape;

import com.microsoft.schemas.office.visio.x2012.main.CellType;
import com.microsoft.schemas.office.visio.x2012.main.RowType;

public class PolyLineTo implements GeometryRow {

    PolyLineTo _master;

    // The x-coordinate of the ending vertex of a polyline.
    Double x;

    // The y-coordinate of the ending vertex of a polyline.
    Double y;

    // The polyline formula
    String a;

    Boolean deleted;

    // TODO: support formulas

    public PolyLineTo(RowType row) {

        if (row.isSetDel())
            deleted = row.getDel();

        for (CellType cell : row.getCellArray()) {
            String cellName = cell.getN();

            if (cellName.equals("X")) {
                x = XDGFCell.parseDoubleValue(cell);
            } else if (cellName.equals("Y")) {
                y = XDGFCell.parseDoubleValue(cell);
            } else if (cellName.equals("A")) {
                a = cell.getV();
            } else {
                throw new POIXMLException("Invalid cell '" + cellName
                        + "' in ArcTo row");
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

    public String getA() {
        return a == null ? _master.a : a;
    }

    @Override
    public void setupMaster(GeometryRow row) {
        _master = (PolyLineTo) row;
    }

    @Override
    @NotImplemented
    public void addToPath(java.awt.geom.Path2D.Double path, XDGFShape parent) {
        if (getDel())
            return;
        throw new POIXMLException("Polyline support not implemented");
    }
}
