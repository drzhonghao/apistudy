import org.apache.poi.hemf.record.HemfRecord;
import org.apache.poi.hemf.record.HemfRecordType;
import org.apache.poi.hemf.record.*;



import java.io.IOException;

import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianInputStream;

@Internal
public class UnimplementedHemfRecord implements HemfRecord {

    private long recordId;
    public UnimplementedHemfRecord() {

    }

    @Override
    public HemfRecordType getRecordType() {
        return HemfRecordType.getById(recordId);
    }

    @Override
    public long init(LittleEndianInputStream leis, long recordId, long recordSize) throws IOException {
        this.recordId = recordId;
        long skipped = IOUtils.skipFully(leis, recordSize);
        if (skipped < recordSize) {
            throw new IOException("End of stream reached before record read");
        }
        return skipped;
    }
}
