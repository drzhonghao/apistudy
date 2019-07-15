import org.apache.poi.xslf.usermodel.*;


import org.apache.poi.sl.usermodel.TabStop;
import org.apache.poi.util.Units;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextTabStop;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextTabAlignType;

public class XSLFTabStop implements TabStop {

    final CTTextTabStop tabStop;
    
    XSLFTabStop(CTTextTabStop tabStop) {
        this.tabStop = tabStop;
    }
    
    /** position in EMUs */
    public int getPosition() {
        return tabStop.getPos();
    }

    /** position in EMUs */
    public void setPosition(final int position) {
        tabStop.setPos(position);
    }

    @Override
    public double getPositionInPoints() {
        return Units.toPoints(getPosition());
    }

    @Override
    public void setPositionInPoints(final double points) {
        setPosition(Units.toEMU(points));
    }
    
    public TabStopType getType() {
        return TabStopType.fromOoxmlId(tabStop.getAlgn().intValue());
    }

    public void setType(final TabStopType tabStopType) {
        tabStop.setAlgn(STTextTabAlignType.Enum.forInt(tabStopType.ooxmlId) );
    }
}
