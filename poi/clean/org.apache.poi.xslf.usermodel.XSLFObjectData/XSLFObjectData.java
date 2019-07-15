import org.apache.poi.xslf.usermodel.*;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.sl.usermodel.ObjectData;
import org.apache.poi.util.Beta;

/**
 * An XSLFOleData instance holds the ole binary stream/object  
 */
@Beta
public final class XSLFObjectData extends POIXMLDocumentPart implements ObjectData {

    /**
     * Create a new XSLFOleData node
     */
    protected XSLFObjectData() {
        super();
    }

    /**
     * Construct XSLFOleData from a package part
     *
     * @param part the package part holding the ole data
     * 
     * @since POI 3.14-Beta1
     */
    public XSLFObjectData(final PackagePart part) {
        super(part);
    }    

    @Override
    public InputStream getInputStream() throws IOException {
        return getPackagePart().getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        final PackagePart pp = getPackagePart();
        pp.clear();
        return pp.getOutputStream();
    }
    
    /**
     * *PictureData objects store the actual content in the part directly without keeping a
     * copy like all others therefore we need to handle them differently.
     */
    @Override
    protected void prepareForCommit() {
        // do not clear the part here
    }


    public void setData(final byte[] data) throws IOException {
        try (final OutputStream os = getPackagePart().getOutputStream()) {
            os.write(data);
        }
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
