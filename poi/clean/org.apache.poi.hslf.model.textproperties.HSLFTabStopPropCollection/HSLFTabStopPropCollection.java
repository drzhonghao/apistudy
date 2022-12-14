import org.apache.poi.hslf.model.textproperties.TextProp;
import org.apache.poi.hslf.model.textproperties.HSLFTabStop;
import org.apache.poi.hslf.model.textproperties.*;


import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.sl.usermodel.TabStop.TabStopType;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianByteArrayInputStream;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.LittleEndianOutputStream;

/**
 * Container for tabstop lists
 */
@Internal
public class HSLFTabStopPropCollection extends TextProp {
    public static final String NAME = "tabStops";
    
    private final List<HSLFTabStop> tabStops = new ArrayList<>();
    
    public HSLFTabStopPropCollection() {
        super(0, 0x100000, NAME);
    }
    
    public HSLFTabStopPropCollection(final HSLFTabStopPropCollection copy) {
        super(0, copy.getMask(), copy.getName());
        for (HSLFTabStop ts : copy.tabStops) {
            tabStops.add(ts.clone());
        }
    }
    
    /**
     * Parses the tabstops from TxMasterStyle record
     *
     * @param data the data stream
     * @param offset the offset within the data
     */
    public void parseProperty(byte data[], int offset) {
        tabStops.addAll(readTabStops(new LittleEndianByteArrayInputStream(data, offset)));
    }
    
    public static List<HSLFTabStop> readTabStops(final LittleEndianInput lei) {
        final int count = lei.readUShort();
        final List<HSLFTabStop> tabs = new ArrayList<>(count);
        for (int i=0; i<count; i++) {
            final int position = lei.readShort();
            final TabStopType type = TabStopType.fromNativeId(lei.readShort());
            tabs.add(new HSLFTabStop(position, type));
        }
        return tabs;
    }
    

    public void writeProperty(OutputStream out) {
        writeTabStops(new LittleEndianOutputStream(out), tabStops);
    }

    public static void writeTabStops(final LittleEndianOutput leo, List<HSLFTabStop> tabStops) {
        final int count = tabStops.size();
        leo.writeShort(count);
        for (HSLFTabStop ts : tabStops) {
            leo.writeShort(ts.getPosition());
            leo.writeShort(ts.getType().nativeId);
        }
        
    }
    
    @Override
    public int getValue() { return tabStops.size(); }

    
    @Override
    public int getSize() {
        return LittleEndianConsts.SHORT_SIZE + tabStops.size()*LittleEndianConsts.INT_SIZE;
    }
    
    public List<HSLFTabStop> getTabStops() {
        return tabStops;
    }

    public void clearTabs() {
        tabStops.clear();
    }
    
    public void addTabStop(HSLFTabStop ts) {
        tabStops.add(ts);
    }
    
    @Override
    public HSLFTabStopPropCollection clone() {
        return new HSLFTabStopPropCollection(this);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + tabStops.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HSLFTabStopPropCollection)) {
            return false;
        }
        HSLFTabStopPropCollection other = (HSLFTabStopPropCollection) obj;
        if (!super.equals(other)) {
            return false;
        }

        return tabStops.equals(other.tabStops);
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" [ ");
        boolean isFirst = true;
        for (HSLFTabStop tabStop : tabStops) {
            if (!isFirst) {
                sb.append(", ");
            }
            sb.append(tabStop.getType());
            sb.append(" @ ");
            sb.append(tabStop.getPosition());
            isFirst = false;
        }
        sb.append(" ]");
        
        return sb.toString();
    }
    
}
