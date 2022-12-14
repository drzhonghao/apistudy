import org.apache.poi.extractor.ole2.*;


import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.extractor.POIOLE2TextExtractor;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.extractor.OLE2ExtractorFactory;
import org.apache.poi.hdgf.extractor.VisioTextExtractor;
import org.apache.poi.hpbf.extractor.PublisherTextExtractor;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.datatypes.AttachmentChunks;
import org.apache.poi.hsmf.extractor.OutlookTextExtactor;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hwpf.OldWordFileFormatException;
import org.apache.poi.hwpf.extractor.Word6Extractor;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

/**
 * Scratchpad-specific logic for {@link OLE2ExtractorFactory} and
 *  {@link org.apache.poi.extractor.ExtractorFactory}, which permit the other two to run with
 *  no Scratchpad jar (though without functionality!)
 * <p>Note - should not be used standalone, always use via the other
 *  two classes</p>
 */
@SuppressWarnings("WeakerAccess")
public class OLE2ScratchpadExtractorFactory {
    private static final POILogger logger = POILogFactory.getLogger(OLE2ScratchpadExtractorFactory.class);

    /**
     * Look for certain entries in the stream, to figure it
     * out what format is desired
     * Note - doesn't check for core-supported formats!
     * Note - doesn't check for OOXML-supported formats
     *
     * @param poifsDir the directory node to be inspected
     * @return the format specific text extractor
     *
     * @throws IOException when the format specific extraction fails because of invalid entires
     */
    public static POITextExtractor createExtractor(DirectoryNode poifsDir) throws IOException {
        if (poifsDir.hasEntry("WordDocument")) {
            // Old or new style word document?
            try {
                return new WordExtractor(poifsDir);
            } catch (OldWordFileFormatException e) {
                return new Word6Extractor(poifsDir);
            }
        }

        if (poifsDir.hasEntry(HSLFSlideShow.POWERPOINT_DOCUMENT)) {
            return new SlideShowExtractor(SlideShowFactory.create(poifsDir));
        }

        if (poifsDir.hasEntry("VisioDocument")) {
            return new VisioTextExtractor(poifsDir);
        }

        if (poifsDir.hasEntry("Quill")) {
            return new PublisherTextExtractor(poifsDir);
        }

        final String[] outlookEntryNames = new String[] {
                // message bodies, saved as plain text (PtypString)
                // The first short (0x1000, 0x0047, 0x0037) refer to the Property ID (see [MS-OXPROPS].pdf)
                // the second short (0x001e, 0x001f, 0x0102) refer to the type of data stored in this entry
                // https://msdn.microsoft.com/endatatypes.Ex-us/library/cc433490(v=exchg.80).aspx
                // @see org.apache.poi.hsmf.Types.MAPIType
                "__substg1.0_1000001E", //PidTagBody ASCII
                "__substg1.0_1000001F", //PidTagBody Unicode
                "__substg1.0_0047001E", //PidTagMessageSubmissionId ASCII
                "__substg1.0_0047001F", //PidTagMessageSubmissionId Unicode
                "__substg1.0_0037001E", //PidTagSubject ASCII
                "__substg1.0_0037001F", //PidTagSubject Unicode
        };
        for (String entryName : outlookEntryNames) {
            if (poifsDir.hasEntry(entryName)) {
                return new OutlookTextExtactor(poifsDir);
            }
        }

        throw new IllegalArgumentException("No supported documents found in the OLE2 stream");
    }

    /**
     * Returns an array of text extractors, one for each of
     *  the embedded documents in the file (if there are any).
     * If there are no embedded documents, you'll get back an
     *  empty array. Otherwise, you'll get one open
     *  {@link POITextExtractor} for each embedded file.
     *
     * @param ext the extractor holding the directory to start parsing
     * @param dirs a list to be filled with directory references holding embedded
     * @param nonPOIFS a list to be filled with streams which aren't based on POIFS entries
     *
     * @throws IOException when the format specific extraction fails because of invalid entires
     */
    public static void identifyEmbeddedResources(POIOLE2TextExtractor ext, List<Entry> dirs, List<InputStream> nonPOIFS) throws IOException {
        // Find all the embedded directories
        DirectoryEntry root = ext.getRoot();
        if (root == null) {
            throw new IllegalStateException("The extractor didn't know which POIFS it came from!");
        }

        if (ext instanceof ExcelExtractor) {
            // These are in MBD... under the root
            Iterator<Entry> it = root.getEntries();
            while (it.hasNext()) {
                Entry entry = it.next();
                if (entry.getName().startsWith("MBD")) {
                    dirs.add(entry);
                }
            }
        } else if (ext instanceof WordExtractor) {
            // These are in ObjectPool -> _... under the root
            try {
                DirectoryEntry op = (DirectoryEntry)
                        root.getEntry("ObjectPool");
                Iterator<Entry> it = op.getEntries();
                while(it.hasNext()) {
                    Entry entry = it.next();
                    if(entry.getName().startsWith("_")) {
                        dirs.add(entry);
                    }
                }
            } catch(FileNotFoundException e) {
                logger.log(POILogger.INFO, "Ignoring FileNotFoundException while extracting Word document", e.getLocalizedMessage());
                // ignored here
            }
            //} else if(ext instanceof PowerPointExtractor) {
            // Tricky, not stored directly in poifs
            // TODO
        } else if (ext instanceof OutlookTextExtactor) {
            // Stored in the Attachment blocks
            MAPIMessage msg = ((OutlookTextExtactor)ext).getMAPIMessage();
            for (AttachmentChunks attachment : msg.getAttachmentFiles()) {
                if (attachment.getAttachData() != null) {
                    byte[] data = attachment.getAttachData().getValue();
                    nonPOIFS.add( new ByteArrayInputStream(data) );
                } else if (attachment.getAttachmentDirectory() != null) {
                    dirs.add(attachment.getAttachmentDirectory().getDirectory());
                }
            }
        }
    }
}
