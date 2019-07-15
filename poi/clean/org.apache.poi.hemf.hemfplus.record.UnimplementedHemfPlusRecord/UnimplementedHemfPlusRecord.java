import org.apache.poi.hemf.hemfplus.record.HemfPlusRecord;
import org.apache.poi.hemf.hemfplus.record.HemfPlusRecordType;
import org.apache.poi.hemf.hemfplus.record.*;



import java.io.IOException;

import org.apache.poi.util.Internal;

@Internal
public class UnimplementedHemfPlusRecord implements HemfPlusRecord {

    private int recordId;
    private int flags;
    private byte[] recordBytes;

    @Override
    public HemfPlusRecordType getRecordType() {
        return HemfPlusRecordType.getById(recordId);
    }

    @Override
    public int getFlags() {
        return flags;
    }

    @Override
    public void init(byte[] recordBytes, int recordId, int flags) throws IOException {
        this.recordId = recordId;
        this.flags = flags;
        this.recordBytes = recordBytes;
    }

    public byte[] getRecordBytes() {
        //should probably defensively return a copy.
        return recordBytes;
    }
}
