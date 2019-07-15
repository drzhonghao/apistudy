import org.apache.poi.hsmf.datatypes.PropertiesChunk;
import org.apache.poi.hsmf.datatypes.ChunkGroup;
import org.apache.poi.hsmf.datatypes.*;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.poi.util.LittleEndian;

/**
 * A {@link PropertiesChunk} for a Storage Properties, such as Attachments and
 * Recipients. This only has a 8 byte header
 */
public class StoragePropertiesChunk extends PropertiesChunk {
    public StoragePropertiesChunk(ChunkGroup parentGroup) {
        super(parentGroup);
    }

    @Override
    public void readValue(InputStream stream) throws IOException {
        // 8 bytes of reserved zeros
        LittleEndian.readLong(stream);

        // Now properties
        readProperties(stream);
    }

    @Override
    public void writeValue(OutputStream out) throws IOException {
        // 8 bytes of reserved zeros
        out.write(new byte[8]);

        // Now properties
        writeProperties(out);
    }
}
