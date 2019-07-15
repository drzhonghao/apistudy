

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSDocument;
import org.apache.poi.poifs.filesystem.POIFSDocumentPath;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.property.DirectoryProperty;
import org.apache.poi.poifs.property.Property;
import org.apache.poi.poifs.property.PropertyTable;
import org.apache.poi.poifs.property.RootProperty;
import org.apache.poi.util.IOUtils;


public class POIFSReader {
	private boolean registryClosed = false;

	private boolean notifyEmptyDirectories;

	public void read(final InputStream stream) throws IOException {
		try (final POIFSFileSystem poifs = new POIFSFileSystem(stream)) {
			read(poifs);
		}
	}

	public void read(final File poifsFile) throws IOException {
		try (final POIFSFileSystem poifs = new POIFSFileSystem(poifsFile, true)) {
			read(poifs);
		}
	}

	public void read(final POIFSFileSystem poifs) throws IOException {
		registryClosed = true;
		PropertyTable properties = poifs.getPropertyTable();
		RootProperty root = properties.getRoot();
		processProperties(poifs, root, new POIFSDocumentPath());
	}

	public void registerListener(final POIFSReaderListener listener) {
		if (listener == null) {
			throw new NullPointerException();
		}
		if (registryClosed) {
			throw new IllegalStateException();
		}
	}

	public void registerListener(final POIFSReaderListener listener, final String name) {
		registerListener(listener, null, name);
	}

	public void registerListener(final POIFSReaderListener listener, final POIFSDocumentPath path, final String name) {
		if (((listener == null) || (name == null)) || ((name.length()) == 0)) {
			throw new NullPointerException();
		}
		if (registryClosed) {
			throw new IllegalStateException();
		}
	}

	public void setNotifyEmptyDirectories(boolean notifyEmptyDirectories) {
		this.notifyEmptyDirectories = notifyEmptyDirectories;
	}

	public static void main(String[] args) throws IOException {
		if ((args.length) == 0) {
			System.err.println("at least one argument required: input filename(s)");
			System.exit(1);
		}
		for (String arg : args) {
			POIFSReader reader = new POIFSReader();
			reader.registerListener(POIFSReader::readEntry);
			System.out.println(("reading " + arg));
			reader.read(new File(arg));
		}
	}

	private static void readEntry(POIFSReaderEvent event) {
		POIFSDocumentPath path = event.getPath();
		StringBuilder sb = new StringBuilder();
		try (final DocumentInputStream istream = event.getStream()) {
			sb.setLength(0);
			int pathLength = path.length();
			for (int k = 0; k < pathLength; k++) {
				sb.append("/").append(path.getComponent(k));
			}
			byte[] data = IOUtils.toByteArray(istream);
			sb.append("/").append(event.getName()).append(": ").append(data.length).append(" bytes read");
			System.out.println(sb);
		} catch (IOException ignored) {
		}
	}

	private void processProperties(final POIFSFileSystem poifs, DirectoryProperty dir, final POIFSDocumentPath path) {
		boolean hasChildren = false;
		for (final Property property : dir) {
			hasChildren = true;
			String name = property.getName();
			if (property.isDirectory()) {
				POIFSDocumentPath new_path = new POIFSDocumentPath(path, new String[]{ name });
				processProperties(poifs, ((DirectoryProperty) (property)), new_path);
			}else {
				POIFSDocument document = null;
			}
		}
		if (hasChildren || (!(notifyEmptyDirectories))) {
			return;
		}
	}
}

