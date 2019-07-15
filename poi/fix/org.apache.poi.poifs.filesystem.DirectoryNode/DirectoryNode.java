

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.hpsf.ClassID;
import org.apache.poi.poifs.dev.POIFSViewable;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.DocumentNode;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.EntryNode;
import org.apache.poi.poifs.filesystem.POIFSDocument;
import org.apache.poi.poifs.filesystem.POIFSDocumentPath;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.filesystem.POIFSWriterListener;
import org.apache.poi.poifs.property.DirectoryProperty;
import org.apache.poi.poifs.property.Property;


public class DirectoryNode extends EntryNode implements Iterable<Entry> , POIFSViewable , DirectoryEntry {
	private final Map<String, Entry> _byname = new HashMap<>();

	private final ArrayList<Entry> _entries = new ArrayList<>();

	private final POIFSFileSystem _nfilesystem;

	private final POIFSDocumentPath _path;

	public POIFSDocumentPath getPath() {
		return _path;
	}

	public POIFSFileSystem getFileSystem() {
		return _nfilesystem;
	}

	public POIFSFileSystem getNFileSystem() {
		return _nfilesystem;
	}

	public DocumentInputStream createDocumentInputStream(final String documentName) throws IOException {
		return createDocumentInputStream(getEntry(documentName));
	}

	public DocumentInputStream createDocumentInputStream(final Entry document) throws IOException {
		if (!(document.isDocumentEntry())) {
			throw new IOException((("Entry '" + (document.getName())) + "' is not a DocumentEntry"));
		}
		DocumentEntry entry = ((DocumentEntry) (document));
		return new DocumentInputStream(entry);
	}

	DocumentEntry createDocument(final POIFSDocument document) throws IOException {
		return null;
	}

	boolean changeName(final String oldName, final String newName) {
		boolean rval = false;
		EntryNode child = ((EntryNode) (_byname.get(oldName)));
		if (child != null) {
			if (rval) {
				_byname.remove(oldName);
			}
		}
		return rval;
	}

	boolean deleteEntry(final EntryNode entry) {
		return false;
	}

	public Iterator<Entry> getEntries() {
		return _entries.iterator();
	}

	public Set<String> getEntryNames() {
		return _byname.keySet();
	}

	public boolean isEmpty() {
		return _entries.isEmpty();
	}

	public int getEntryCount() {
		return _entries.size();
	}

	public boolean hasEntry(String name) {
		return (name != null) && (_byname.containsKey(name));
	}

	public Entry getEntry(final String name) throws FileNotFoundException {
		Entry rval = null;
		if (name != null) {
			rval = _byname.get(name);
		}
		if (rval == null) {
			throw new FileNotFoundException(((("no such entry: \"" + name) + "\", had: ") + (_byname.keySet())));
		}
		return rval;
	}

	public DocumentEntry createDocument(final String name, final InputStream stream) throws IOException {
		return createDocument(new POIFSDocument(name, _nfilesystem, stream));
	}

	public DocumentEntry createDocument(final String name, final int size, final POIFSWriterListener writer) throws IOException {
		return createDocument(new POIFSDocument(name, size, _nfilesystem, writer));
	}

	public DirectoryEntry createDirectory(final String name) throws IOException {
		DirectoryProperty property = new DirectoryProperty(name);
		((DirectoryProperty) (getProperty())).addChild(property);
		return null;
	}

	@SuppressWarnings("WeakerAccess")
	public DocumentEntry createOrUpdateDocument(final String name, final InputStream stream) throws IOException {
		if (!(hasEntry(name))) {
			return createDocument(name, stream);
		}else {
			DocumentNode existing = ((DocumentNode) (getEntry(name)));
			POIFSDocument nDoc = new POIFSDocument(existing);
			nDoc.replaceContents(stream);
			return existing;
		}
	}

	public ClassID getStorageClsid() {
		return getProperty().getStorageClsid();
	}

	public void setStorageClsid(ClassID clsidStorage) {
		getProperty().setStorageClsid(clsidStorage);
	}

	@Override
	public boolean isDirectoryEntry() {
		return true;
	}

	@Override
	protected boolean isDeleteOK() {
		return isEmpty();
	}

	public Object[] getViewableArray() {
		return new Object[0];
	}

	public Iterator<Object> getViewableIterator() {
		List<Object> components = new ArrayList<>();
		components.add(getProperty());
		components.addAll(_entries);
		return components.iterator();
	}

	public boolean preferArray() {
		return false;
	}

	public String getShortDescription() {
		return getName();
	}

	public Iterator<Entry> iterator() {
		return getEntries();
	}
}

