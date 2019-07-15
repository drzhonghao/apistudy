import org.apache.poi.xssf.binary.*;


import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Internal;

/**
 * This is a read only record that maintains information about
 * a hyperlink.  In OOXML land, this information has to be merged
 * from 1) the sheet's .rels to get the url and 2) from after the
 * sheet data in they hyperlink section.
 *
 * The {@link #display} is often empty and should be filled from
 * the contents of the anchor cell.
 *
 * @since 3.16-beta3
 */
@Internal
public class XSSFHyperlinkRecord {

    private final CellRangeAddress cellRangeAddress;
    private final String relId;
    private String location;
    private String toolTip;
    private String display;

    XSSFHyperlinkRecord(CellRangeAddress cellRangeAddress, String relId, String location, String toolTip, String display) {
        this.cellRangeAddress = cellRangeAddress;
        this.relId = relId;
        this.location = location;
        this.toolTip = toolTip;
        this.display = display;
    }

    void setLocation(String location) {
        this.location = location;
    }

    void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    void setDisplay(String display) {
        this.display = display;
    }

    CellRangeAddress getCellRangeAddress() {
        return cellRangeAddress;
    }

    public String getRelId() {
        return relId;
    }

    public String getLocation() {
        return location;
    }

    public String getToolTip() {
        return toolTip;
    }

    public String getDisplay() {
        return display;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XSSFHyperlinkRecord that = (XSSFHyperlinkRecord) o;

        if (cellRangeAddress != null ? !cellRangeAddress.equals(that.cellRangeAddress) : that.cellRangeAddress != null)
            return false;
        if (relId != null ? !relId.equals(that.relId) : that.relId != null) return false;
        if (location != null ? !location.equals(that.location) : that.location != null) return false;
        if (toolTip != null ? !toolTip.equals(that.toolTip) : that.toolTip != null) return false;
        return display != null ? display.equals(that.display) : that.display == null;
    }

    @Override
    public int hashCode() {
        int result = cellRangeAddress != null ? cellRangeAddress.hashCode() : 0;
        result = 31 * result + (relId != null ? relId.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (toolTip != null ? toolTip.hashCode() : 0);
        result = 31 * result + (display != null ? display.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "XSSFHyperlinkRecord{" +
                "cellRangeAddress=" + cellRangeAddress +
                ", relId='" + relId + '\'' +
                ", location='" + location + '\'' +
                ", toolTip='" + toolTip + '\'' +
                ", display='" + display + '\'' +
                '}';
    }
}
