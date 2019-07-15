

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.apache.poi.hpsf.ClassID;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.MarkUnsupportedException;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.util.LittleEndianInputStream;


public class PropertySetFactory {
	public static PropertySet create(final DirectoryEntry dir, final String name) throws FileNotFoundException, IOException, UnsupportedEncodingException, NoPropertySetStreamException {
		InputStream inp = null;
		try {
			DocumentEntry entry = ((DocumentEntry) (dir.getEntry(name)));
			inp = new DocumentInputStream(entry);
			try {
				return PropertySetFactory.create(inp);
			} catch (MarkUnsupportedException e) {
				return null;
			}
		} finally {
			if (inp != null) {
				inp.close();
			}
		}
	}

	public static PropertySet create(final InputStream stream) throws IOException, UnsupportedEncodingException, MarkUnsupportedException, NoPropertySetStreamException {
		LittleEndianInputStream leis = new LittleEndianInputStream(stream);
		int byteOrder = leis.readUShort();
		int format = leis.readUShort();
		leis.readUInt();
		byte[] clsIdBuf = new byte[ClassID.LENGTH];
		leis.readFully(clsIdBuf);
		int sectionCount = ((int) (leis.readUInt()));
		if (sectionCount > 0) {
			leis.readFully(clsIdBuf);
		}
		stream.reset();
		ClassID clsId = new ClassID(clsIdBuf, 0);
		return null;
	}

	public static SummaryInformation newSummaryInformation() {
		return new SummaryInformation();
	}

	public static DocumentSummaryInformation newDocumentSummaryInformation() {
		return new DocumentSummaryInformation();
	}
}

