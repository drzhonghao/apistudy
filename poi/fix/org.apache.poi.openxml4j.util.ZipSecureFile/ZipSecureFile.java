

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.poi.openxml4j.util.ZipArchiveThresholdInputStream;


public class ZipSecureFile extends ZipFile {
	static double MIN_INFLATE_RATIO = 0.01;

	static long MAX_ENTRY_SIZE = 4294967295L;

	private static long MAX_TEXT_SIZE = (10 * 1024) * 1024L;

	private final String fileName;

	public static void setMinInflateRatio(double ratio) {
		ZipSecureFile.MIN_INFLATE_RATIO = ratio;
	}

	public static double getMinInflateRatio() {
		return ZipSecureFile.MIN_INFLATE_RATIO;
	}

	public static void setMaxEntrySize(long maxEntrySize) {
		if ((maxEntrySize < 0) || (maxEntrySize > 4294967295L)) {
			throw new IllegalArgumentException(("Max entry size is bounded [0-4GB], but had " + maxEntrySize));
		}
		ZipSecureFile.MAX_ENTRY_SIZE = maxEntrySize;
	}

	public static long getMaxEntrySize() {
		return ZipSecureFile.MAX_ENTRY_SIZE;
	}

	public static void setMaxTextSize(long maxTextSize) {
		if ((maxTextSize < 0) || (maxTextSize > 4294967295L)) {
			throw new IllegalArgumentException(("Max text size is bounded [0-4GB], but had " + maxTextSize));
		}
		ZipSecureFile.MAX_TEXT_SIZE = maxTextSize;
	}

	public static long getMaxTextSize() {
		return ZipSecureFile.MAX_TEXT_SIZE;
	}

	public ZipSecureFile(File file) throws IOException {
		super(file);
		this.fileName = file.getAbsolutePath();
	}

	public ZipSecureFile(String name) throws IOException {
		super(name);
		this.fileName = new File(name).getAbsolutePath();
	}

	@Override
	@SuppressWarnings("resource")
	public ZipArchiveThresholdInputStream getInputStream(ZipArchiveEntry entry) throws IOException {
		ZipArchiveThresholdInputStream zatis = new ZipArchiveThresholdInputStream(super.getInputStream(entry));
		return zatis;
	}

	public String getName() {
		return fileName;
	}
}

