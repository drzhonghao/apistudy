import org.apache.poi.poifs.crypt.temp.EncryptedTempData;
import org.apache.poi.poifs.crypt.temp.AesZipFileZipEntrySource;
import org.apache.poi.poifs.crypt.temp.SheetDataWriterWithDecorator;
import org.apache.poi.poifs.crypt.temp.*;


import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.openxml4j.util.ZipEntrySource;
import org.apache.poi.util.Beta;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.streaming.SheetDataWriter;

@Beta
public class SXSSFWorkbookWithCustomZipEntrySource extends SXSSFWorkbook {
    private static final POILogger LOG = POILogFactory.getLogger(SXSSFWorkbookWithCustomZipEntrySource.class);

    public SXSSFWorkbookWithCustomZipEntrySource() {
        super(20);
        setCompressTempFiles(true);
    }
    
    @Override
    public void write(OutputStream stream) throws IOException {
        flushSheets();
        EncryptedTempData tempData = new EncryptedTempData();
        ZipEntrySource source = null;
        try {
            try (OutputStream os = tempData.getOutputStream()) {
                getXSSFWorkbook().write(os);
            }
            // provide ZipEntrySource to poi which decrypts on the fly
            source = AesZipFileZipEntrySource.createZipEntrySource(tempData.getInputStream());
            injectData(source, stream);
        } finally {
            tempData.dispose();
            IOUtils.closeQuietly(source);
        }
    }
    
    @Override
    protected SheetDataWriter createSheetDataWriter() throws IOException {
        //log values to ensure these values are accessible to subclasses
        LOG.log(POILogger.INFO, "isCompressTempFiles: " + isCompressTempFiles());
        LOG.log(POILogger.INFO, "SharedStringSource: " + getSharedStringSource());
        return new SheetDataWriterWithDecorator();
    }
}
