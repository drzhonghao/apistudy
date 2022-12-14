import org.apache.poi.hslf.usermodel.*;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.poi.hslf.record.ExOleObjStg;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.sl.usermodel.ObjectData;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

/**
 * A class that represents object data embedded in a slide show.
 */
public class HSLFObjectData implements ObjectData {
    private static final POILogger LOG = POILogFactory.getLogger(HSLFObjectData.class);
    
    /**
     * The record that contains the object data.
     */
    private ExOleObjStg storage;

    /**
     * Creates the object data wrapping the record that contains the object data.
     *
     * @param storage the record that contains the object data.
     */
    public HSLFObjectData(ExOleObjStg storage) {
        this.storage = storage;
    }

    @Override
    public InputStream getInputStream() {
        return storage.getData();
    }
    
    @Override
    public OutputStream getOutputStream() throws IOException {
        return new ByteArrayOutputStream(100000) {
            public void close() throws IOException {
                setData(getBytes());
            }
        };
    }

    /**
     * Sets the embedded data.
     *
     * @param data the embedded data.
     */
     public void setData(byte[] data) throws IOException {
        storage.setData(data);    
    }

    /**
     * Return the record that contains the object data.
     *
     * @return the record that contains the object data.
     */
    public ExOleObjStg getExOleObjStg() {
        return storage;
    }


    @Override
    public String getOLE2ClassName() {
        return null;
    }

    @Override
    public String getFileName() {
        return null;
    }
}
