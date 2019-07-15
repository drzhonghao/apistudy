import org.apache.poi.hemf.hemfplus.record.*;



import java.io.IOException;

import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;

@Internal
public class HemfPlusHeader implements HemfPlusRecord {

    private int flags;
    private long version; //hack for now; replace with EmfPlusGraphicsVersion object
    private long emfPlusFlags;
    private long logicalDpiX;
    private long logicalDpiY;

    @Override
    public HemfPlusRecordType getRecordType() {
        return HemfPlusRecordType.header;
    }

    public int getFlags() {
        return flags;
    }

    @Override
    public void init(byte[] dataBytes, int recordId, int flags) throws IOException {
        //assert record id == header
        this.flags = flags;
        int offset = 0;
        this.version = LittleEndian.getUInt(dataBytes, offset); offset += LittleEndian.INT_SIZE;
        this.emfPlusFlags = LittleEndian.getUInt(dataBytes, offset); offset += LittleEndian.INT_SIZE;
        this.logicalDpiX = LittleEndian.getUInt(dataBytes, offset); offset += LittleEndian.INT_SIZE;
        this.logicalDpiY = LittleEndian.getUInt(dataBytes, offset);

    }

    public long getVersion() {
        return version;
    }

    public long getEmfPlusFlags() {
        return emfPlusFlags;
    }

    public long getLogicalDpiX() {
        return logicalDpiX;
    }

    public long getLogicalDpiY() {
        return logicalDpiY;
    }

    @Override
    public String toString() {
        return "HemfPlusHeader{" +
                "flags=" + flags +
                ", version=" + version +
                ", emfPlusFlags=" + emfPlusFlags +
                ", logicalDpiX=" + logicalDpiX +
                ", logicalDpiY=" + logicalDpiY +
                '}';
    }
}
