import org.apache.poi.xdgf.usermodel.section.geometry.GeometryRow;
import org.apache.poi.xdgf.usermodel.section.geometry.*;


import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xdgf.usermodel.XDGFCell;
import org.apache.poi.xdgf.usermodel.XDGFShape;

import com.microsoft.schemas.office.visio.x2012.main.CellType;
import com.microsoft.schemas.office.visio.x2012.main.RowType;

public class RelMoveTo implements GeometryRow {

    RelMoveTo _master;

    Double x;
    Double y;

    Boolean deleted;

    // TODO: support formulas

    public RelMoveTo(RowType row) {

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
                        + "' in RelMoveTo row");
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
        _master = (RelMoveTo) row;
    }

    @Override
    public void addToPath(java.awt.geom.Path2D.Double path, XDGFShape parent) {

        if (getDel())
            return;

        path.moveTo(getX() * parent.getWidth(), getY() * parent.getHeight());
    }
}
