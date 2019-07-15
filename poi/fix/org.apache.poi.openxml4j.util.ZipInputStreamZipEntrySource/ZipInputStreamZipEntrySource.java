

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.poi.openxml4j.util.ZipArchiveThresholdInputStream;
import org.apache.poi.openxml4j.util.ZipEntrySource;


public class ZipInputStreamZipEntrySource implements ZipEntrySource {
	public ZipInputStreamZipEntrySource(ZipArchiveThresholdInputStream inp) throws IOException {
		for (; ;) {
		}
	}

	@Override
	public Enumeration<? extends ZipArchiveEntry> getEntries() {
		return null;
	}

	@Override
	public InputStream getInputStream(ZipArchiveEntry zipEntry) {
		return null;
	}

	@Override
	public void close() {
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public ZipArchiveEntry getEntry(final String path) {
		final String normalizedPath = path.replace('\\', '/');
		return null;
	}
}

