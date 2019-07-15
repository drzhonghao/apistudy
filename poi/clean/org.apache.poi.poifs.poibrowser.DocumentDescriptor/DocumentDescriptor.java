import org.apache.poi.poifs.filesystem.DocumentDescriptor;
import org.apache.poi.poifs.poibrowser.*;


import java.io.IOException;

import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSDocumentPath;
import org.apache.poi.util.IOUtils;

/**
 * <p>Describes the most important (whatever that is) features of a
 * {@link POIFSDocumentPath}.</p>
 */
class DocumentDescriptor
{

    //arbitrarily selected; may need to increase
    private static final int MAX_RECORD_LENGTH = 100_000;

    String name;
    POIFSDocumentPath path;
    DocumentInputStream stream;

    int size;
    byte[] bytes;


    /**
     * <p>Creates a {@link DocumentDescriptor}.</p>
     *
     * @param name The stream's name.
     *
     * @param path The stream's path in the POI filesystem hierarchy.
     *
     * @param stream The stream.
     *
     * @param nrOfBytes The maximum number of bytes to display in a
     * dump starting at the beginning of the stream.
     */
    public DocumentDescriptor(final String name,
                              final POIFSDocumentPath path,
                              final DocumentInputStream stream,
                              final int nrOfBytes) {
        this.name = name;
        this.path = path;
        this.stream = stream;
        try {
            if (stream.markSupported()) {
                stream.mark(nrOfBytes);
                bytes = IOUtils.toByteArray(stream, nrOfBytes, MAX_RECORD_LENGTH);
                stream.reset();
            } else {
                bytes = new byte[0];
            }
            size = bytes.length + stream.available();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

}
