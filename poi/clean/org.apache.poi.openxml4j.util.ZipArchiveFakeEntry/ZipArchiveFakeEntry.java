import org.apache.poi.openxml4j.util.*;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.poi.util.IOUtils;


/**
 * So we can close the real zip entry and still
 *  effectively work with it.
 * Holds the (decompressed!) data in memory, so
 *  close this as soon as you can!
 */
/* package */ class ZipArchiveFakeEntry extends ZipArchiveEntry {
    private final byte[] data;

    ZipArchiveFakeEntry(ZipArchiveEntry entry, InputStream inp) throws IOException {
        super(entry.getName());

        final long entrySize = entry.getSize();

        if (entrySize < -1 || entrySize>=Integer.MAX_VALUE) {
            throw new IOException("ZIP entry size is too large or invalid");
        }

        // Grab the de-compressed contents for later
        data = (entrySize == -1) ? IOUtils.toByteArray(inp) : IOUtils.toByteArray(inp, (int)entrySize);
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }
}
