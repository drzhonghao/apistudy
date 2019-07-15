import org.apache.poi.xdgf.usermodel.section.geometry.*;


import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xdgf.usermodel.XDGFCell;
import org.apache.poi.xdgf.usermodel.XDGFShape;

import com.microsoft.schemas.office.visio.x2012.main.CellType;
import com.microsoft.schemas.office.visio.x2012.main.RowType;

public class LineTo implements GeometryRow {

    LineTo _master;

    Double x;
    Double y;

    Boolean deleted;

    // TODO: support formulas

    public LineTo(RowType row) {

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
                        + "' in LineTo row");
            }
        }
    }

    @Override
    public String toString() {
        return "LineTo: x=" + getX() + "; y=" + getY();
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
        _master = (LineTo) row;
    }

    @Override
    public void addToPath(java.awt.geom.Path2D.Double path, XDGFShape parent) {
        if (getDel())
            return;
        path.lineTo(getX(), getY());
    }
}
