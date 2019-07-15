import org.apache.poi.hssf.usermodel.examples.*;


import java.io.Closeable;
import java.io.FileInputStream;

import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hssf.usermodel.HSSFObjectData;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * Demonstrates how you can extract embedded data from a .xls file
 */
public class EmbeddedObjects {
    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(args[0]));
        try (HSSFWorkbook workbook = new HSSFWorkbook(fs)) {
            for (HSSFObjectData obj : workbook.getAllEmbeddedObjects()) {
                //the OLE2 Class Name of the object
                String oleName = obj.getOLE2ClassName();
                DirectoryNode dn = (obj.hasDirectoryEntry()) ? (DirectoryNode) obj.getDirectory() : null;
                Closeable document = null;
                if (oleName.equals("Worksheet")) {
                    document = new HSSFWorkbook(dn, fs, false);
                } else if (oleName.equals("Document")) {
                    document = new HWPFDocument(dn);
                } else if (oleName.equals("Presentation")) {
                    document = new HSLFSlideShow(dn);
                } else {
                    if (dn != null) {
                        // The DirectoryEntry is a DocumentNode. Examine its entries to find out what it is
                        for (Entry entry : dn) {
                            String name = entry.getName();
                        }
                    } else {
                        // There is no DirectoryEntry
                        // Recover the object's data from the HSSFObjectData instance.
                        byte[] objectData = obj.getObjectData();
                    }
                }
                if (document != null) {
                    document.close();
                }
            }
        }
    }
}
