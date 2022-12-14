import org.apache.poi.hslf.model.textproperties.*;


import org.apache.poi.sl.usermodel.TabStop;
import org.apache.poi.util.Internal;
import org.apache.poi.util.Units;

@Internal
public class HSLFTabStop implements TabStop, Cloneable {
    /**
     * A signed integer that specifies an offset, in master units, of the tab stop.
     * 
     * If the TextPFException record that contains this TabStop structure also contains a
     * leftMargin, then the value of position is relative to the left margin of the paragraph;
     * otherwise, the value is relative to the left side of the paragraph.
     * 
     * If a TextRuler record contains this TabStop structure, the value is relative to the
     * left side of the text ruler.
     */
    private int position;

    /**
     * A enumeration that specifies how text aligns at the tab stop.
     */
    private TabStopType type;

    public HSLFTabStop(int position, TabStopType type) {
        this.position = position;
        this.type = type;
    }

    public int getPosition() {
        return position;
    }
    
    public void setPosition(final int position) {
        this.position = position;
    }
    
    @Override
    public double getPositionInPoints() {
        return Units.masterToPoints(getPosition());
    }

    @Override
    public void setPositionInPoints(final double points) {
        setPosition(Units.pointsToMaster(points));
    }

    @Override
    public TabStopType getType() {
        return type;
    }

    @Override
    public void setType(TabStopType type) {
        this.type = type;
    }

    @Override
    public HSLFTabStop clone() {
        try {
            return (HSLFTabStop)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + position;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HSLFTabStop)) {
            return false;
        }
        HSLFTabStop other = (HSLFTabStop) obj;
        if (position != other.position) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return type + " @ " + position;
    }
}
