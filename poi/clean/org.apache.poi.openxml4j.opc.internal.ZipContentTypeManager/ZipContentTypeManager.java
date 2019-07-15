import org.apache.poi.openxml4j.opc.internal.ContentTypeManager;
import org.apache.poi.openxml4j.opc.internal.*;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.StreamHelper;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.w3c.dom.Document;

/**
 * Zip implementation of the ContentTypeManager.
 *
 * @author Julien Chable
 * @version 1.0
 * @see ContentTypeManager
 */
public class ZipContentTypeManager extends ContentTypeManager {
    private final static POILogger logger = POILogFactory.getLogger(ZipContentTypeManager.class);

	/**
	 * Delegate constructor to the super constructor.
	 *
	 * @param in
	 *            The input stream to parse to fill internal content type
	 *            collections.
	 * @throws InvalidFormatException
	 *             If the content types part content is not valid.
	 */
	public ZipContentTypeManager(InputStream in, OPCPackage pkg)
			throws InvalidFormatException {
		super(in, pkg);
	}

	@SuppressWarnings("resource")
    @Override
	public boolean saveImpl(Document content, OutputStream out) {
		final ZipArchiveOutputStream zos = (out instanceof ZipArchiveOutputStream)
				? (ZipArchiveOutputStream) out : new ZipArchiveOutputStream(out);

		ZipArchiveEntry partEntry = new ZipArchiveEntry(CONTENT_TYPES_PART_NAME);
		try {
			// Referenced in ZIP
			zos.putArchiveEntry(partEntry);
			try {
				// Saving data in the ZIP file
				return StreamHelper.saveXmlInStream(content, zos);
			} finally {
				zos.closeArchiveEntry();
			}
		} catch (IOException ioe) {
			logger.log(POILogger.ERROR, "Cannot write: " + CONTENT_TYPES_PART_NAME
					+ " in Zip !", ioe);
			return false;
		}
	}
}
