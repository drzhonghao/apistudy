

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.poi.POIDocument;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hslf.exceptions.CorruptPowerPointFileException;
import org.apache.poi.hslf.exceptions.HSLFException;
import org.apache.poi.hslf.exceptions.OldPowerPointFormatException;
import org.apache.poi.hslf.record.CurrentUserAtom;
import org.apache.poi.hslf.record.DocumentEncryptionAtom;
import org.apache.poi.hslf.record.ExOleObjStg;
import org.apache.poi.hslf.record.PersistPtrHolder;
import org.apache.poi.hslf.record.PersistRecord;
import org.apache.poi.hslf.record.PositionDependentRecord;
import org.apache.poi.hslf.record.PositionDependentRecordAtom;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.hslf.record.UserEditAtom;
import org.apache.poi.hslf.usermodel.HSLFObjectData;
import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.EntryNode;
import org.apache.poi.poifs.filesystem.EntryUtils;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public final class HSLFSlideShowImpl extends POIDocument implements Closeable {
	static final int UNSET_OFFSET = -1;

	private static final int MAX_RECORD_LENGTH = 200000000;

	private POILogger logger = POILogFactory.getLogger(this.getClass());

	private CurrentUserAtom currentUser;

	private byte[] _docstream;

	private Record[] _records;

	private List<HSLFPictureData> _pictures;

	private HSLFObjectData[] _objects;

	@SuppressWarnings("resource")
	public HSLFSlideShowImpl(String fileName) throws IOException {
		this(new POIFSFileSystem(new File(fileName)));
	}

	@SuppressWarnings("resource")
	public HSLFSlideShowImpl(InputStream inputStream) throws IOException {
		this(new POIFSFileSystem(inputStream));
	}

	public HSLFSlideShowImpl(POIFSFileSystem filesystem) throws IOException {
		this(filesystem.getRoot());
	}

	public HSLFSlideShowImpl(DirectoryNode dir) throws IOException {
		super(HSLFSlideShowImpl.handleDualStorage(dir));
		readCurrentUserStream();
		readPowerPointStream();
		buildRecords();
		readOtherStreams();
	}

	private static DirectoryNode handleDualStorage(DirectoryNode dir) throws IOException {
		String dualName = "PP97_DUALSTORAGE";
		if (!(dir.hasEntry(dualName))) {
			return dir;
		}
		dir = ((DirectoryNode) (dir.getEntry(dualName)));
		return dir;
	}

	public static HSLFSlideShowImpl create() {
		InputStream is = HSLFSlideShowImpl.class.getResourceAsStream("/org/apache/poi/hslf/data/empty.ppt");
		if (is == null) {
			throw new HSLFException("Missing resource 'empty.ppt'");
		}
		try {
			try {
				return new HSLFSlideShowImpl(is);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			throw new HSLFException(e);
		}
	}

	private void readPowerPointStream() throws IOException {
		final DirectoryNode dir = getDirectory();
		if ((!(dir.hasEntry(HSLFSlideShow.POWERPOINT_DOCUMENT))) && (dir.hasEntry(HSLFSlideShow.PP95_DOCUMENT))) {
			throw new OldPowerPointFormatException("You seem to have supplied a PowerPoint95 file, which isn't supported");
		}
		DocumentEntry docProps = ((DocumentEntry) (dir.getEntry(HSLFSlideShow.POWERPOINT_DOCUMENT)));
		int len = docProps.getSize();
		try (InputStream is = dir.createDocumentInputStream(docProps)) {
			_docstream = IOUtils.toByteArray(is, len);
		}
	}

	private void buildRecords() throws IOException {
		_records = read(_docstream, ((int) (currentUser.getCurrentEditOffset())));
	}

	private Record[] read(byte[] docstream, int usrOffset) throws IOException {
		NavigableMap<Integer, Record> records = new TreeMap<>();
		Map<Integer, Integer> persistIds = new HashMap<>();
		initRecordOffsets(docstream, usrOffset, records, persistIds);
		for (Map.Entry<Integer, Record> entry : records.entrySet()) {
			Integer offset = entry.getKey();
			Record record = entry.getValue();
			Integer persistId = persistIds.get(offset);
			if (record == null) {
				record = Record.buildRecordAtOffset(docstream, offset);
				entry.setValue(record);
			}
			if (record instanceof PersistRecord) {
				((PersistRecord) (record)).setPersistId(persistId);
			}
		}
		return records.values().toArray(new Record[0]);
	}

	private void initRecordOffsets(byte[] docstream, int usrOffset, NavigableMap<Integer, Record> recordMap, Map<Integer, Integer> offset2id) {
		while (usrOffset != 0) {
			UserEditAtom usr = ((UserEditAtom) (Record.buildRecordAtOffset(docstream, usrOffset)));
			recordMap.put(usrOffset, usr);
			int psrOffset = usr.getPersistPointersOffset();
			PersistPtrHolder ptr = ((PersistPtrHolder) (Record.buildRecordAtOffset(docstream, psrOffset)));
			recordMap.put(psrOffset, ptr);
			for (Map.Entry<Integer, Integer> entry : ptr.getSlideLocationsLookup().entrySet()) {
				Integer offset = entry.getValue();
				Integer id = entry.getKey();
				recordMap.put(offset, null);
				offset2id.put(offset, id);
			}
			usrOffset = usr.getLastUserEditAtomOffset();
			if ((usrOffset > 0) && (recordMap.containsKey(usrOffset))) {
				usrOffset = (recordMap.firstKey()) - 36;
				int ver_inst = LittleEndian.getUShort(docstream, usrOffset);
				int type = LittleEndian.getUShort(docstream, (usrOffset + 2));
				int len = LittleEndian.getInt(docstream, (usrOffset + 4));
				if (((ver_inst == 0) && (type == 4085)) && ((len == 28) || (len == 32))) {
					logger.log(POILogger.WARN, "Repairing invalid user edit atom");
					usr.setLastUserEditAtomOffset(usrOffset);
				}else {
					throw new CorruptPowerPointFileException("Powerpoint document contains invalid user edit atom");
				}
			}
		} 
	}

	public DocumentEncryptionAtom getDocumentEncryptionAtom() {
		for (Record r : _records) {
			if (r instanceof DocumentEncryptionAtom) {
				return ((DocumentEncryptionAtom) (r));
			}
		}
		return null;
	}

	private void readCurrentUserStream() {
		try {
			currentUser = new CurrentUserAtom(getDirectory());
		} catch (IOException ie) {
			logger.log(POILogger.ERROR, ("Error finding Current User Atom:\n" + ie));
			currentUser = new CurrentUserAtom();
		}
	}

	private void readOtherStreams() {
	}

	private void readPictures() throws IOException {
		_pictures = new ArrayList<>();
		if (!(getDirectory().hasEntry("Pictures"))) {
			return;
		}
		DocumentEntry entry = ((DocumentEntry) (getDirectory().getEntry("Pictures")));
		DocumentInputStream is = getDirectory().createDocumentInputStream(entry);
		byte[] pictstream = IOUtils.toByteArray(is, entry.getSize());
		is.close();
	}

	public void normalizeRecords() {
		try {
			updateAndWriteDependantRecords(null, null);
		} catch (IOException e) {
			throw new CorruptPowerPointFileException(e);
		}
	}

	@SuppressWarnings("WeakerAccess")
	public void updateAndWriteDependantRecords(OutputStream os, Map<RecordTypes, PositionDependentRecord> interestingRecords) throws IOException {
		Map<Integer, Integer> oldToNewPositions = new HashMap<>();
		UserEditAtom usr = null;
		PersistPtrHolder ptr = null;
		HSLFSlideShowImpl.CountingOS cos = new HSLFSlideShowImpl.CountingOS();
		for (Record record : _records) {
			assert record instanceof PositionDependentRecord;
			PositionDependentRecord pdr = ((PositionDependentRecord) (record));
			int oldPos = pdr.getLastOnDiskOffset();
			int newPos = cos.size();
			pdr.setLastOnDiskOffset(newPos);
			if (oldPos != (HSLFSlideShowImpl.UNSET_OFFSET)) {
				oldToNewPositions.put(oldPos, newPos);
			}
			RecordTypes saveme = null;
			int recordType = ((int) (record.getRecordType()));
			if (recordType == (RecordTypes.PersistPtrIncrementalBlock.typeID)) {
				saveme = RecordTypes.PersistPtrIncrementalBlock;
				ptr = ((PersistPtrHolder) (pdr));
			}else
				if (recordType == (RecordTypes.UserEditAtom.typeID)) {
					saveme = RecordTypes.UserEditAtom;
					usr = ((UserEditAtom) (pdr));
				}

			if ((interestingRecords != null) && (saveme != null)) {
				interestingRecords.put(saveme, pdr);
			}
			record.writeOut(cos);
		}
		cos.close();
		if ((usr == null) || (ptr == null)) {
			throw new HSLFException("UserEditAtom or PersistPtr can't be determined.");
		}
		Map<Integer, Integer> persistIds = new HashMap<>();
		for (Map.Entry<Integer, Integer> entry : ptr.getSlideLocationsLookup().entrySet()) {
			persistIds.put(oldToNewPositions.get(entry.getValue()), entry.getKey());
		}
		for (Record record : _records) {
			assert record instanceof PositionDependentRecord;
			PositionDependentRecord pdr = ((PositionDependentRecord) (record));
			Integer persistId = persistIds.get(pdr.getLastOnDiskOffset());
			if (persistId == null) {
				persistId = 0;
			}
			pdr.updateOtherRecordReferences(oldToNewPositions);
			if (os != null) {
			}
		}
		int oldLastUserEditAtomPos = ((int) (currentUser.getCurrentEditOffset()));
		Integer newLastUserEditAtomPos = oldToNewPositions.get(oldLastUserEditAtomPos);
		if ((newLastUserEditAtomPos == null) || ((usr.getLastOnDiskOffset()) != newLastUserEditAtomPos)) {
			throw new HSLFException(("Couldn't find the new location of the last UserEditAtom that used to be at " + oldLastUserEditAtomPos));
		}
		currentUser.setCurrentEditOffset(usr.getLastOnDiskOffset());
	}

	@Override
	public void write() throws IOException {
		validateInPlaceWritePossible();
		write(getDirectory().getFileSystem(), false);
		getDirectory().getFileSystem().writeFilesystem();
	}

	@Override
	public void write(File newFile) throws IOException {
		write(newFile, false);
	}

	public void write(File newFile, boolean preserveNodes) throws IOException {
		try (POIFSFileSystem outFS = POIFSFileSystem.create(newFile)) {
			write(outFS, preserveNodes);
			outFS.writeFilesystem();
		}
	}

	@Override
	public void write(OutputStream out) throws IOException {
		write(out, false);
	}

	public void write(OutputStream out, boolean preserveNodes) throws IOException {
		try (POIFSFileSystem outFS = new POIFSFileSystem()) {
			write(outFS, preserveNodes);
			outFS.writeFilesystem(out);
		}
	}

	private void write(POIFSFileSystem outFS, boolean copyAllOtherNodes) throws IOException {
		if ((_pictures) == null) {
			readPictures();
		}
		getDocumentSummaryInformation();
		List<String> writtenEntries = new ArrayList<>(1);
		writeProperties(outFS, writtenEntries);
		HSLFSlideShowImpl.BufAccessBAOS baos = new HSLFSlideShowImpl.BufAccessBAOS();
		updateAndWriteDependantRecords(baos, null);
		_docstream = new byte[baos.size()];
		System.arraycopy(baos.getBuf(), 0, _docstream, 0, baos.size());
		baos.close();
		ByteArrayInputStream bais = new ByteArrayInputStream(_docstream);
		outFS.createOrUpdateDocument(bais, HSLFSlideShow.POWERPOINT_DOCUMENT);
		writtenEntries.add(HSLFSlideShow.POWERPOINT_DOCUMENT);
		currentUser.writeToFS(outFS);
		writtenEntries.add("Current User");
		if ((_pictures.size()) > 0) {
			HSLFSlideShowImpl.BufAccessBAOS pict = new HSLFSlideShowImpl.BufAccessBAOS();
			for (HSLFPictureData p : _pictures) {
				int offset = pict.size();
				p.write(pict);
			}
			outFS.createOrUpdateDocument(new ByteArrayInputStream(pict.getBuf(), 0, pict.size()), "Pictures");
			writtenEntries.add("Pictures");
			pict.close();
		}
		if (copyAllOtherNodes) {
			EntryUtils.copyNodes(getDirectory().getFileSystem(), outFS, writtenEntries);
		}
	}

	@Override
	public EncryptionInfo getEncryptionInfo() {
		DocumentEncryptionAtom dea = getDocumentEncryptionAtom();
		return dea != null ? dea.getEncryptionInfo() : null;
	}

	@SuppressWarnings({ "UnusedReturnValue", "WeakerAccess" })
	public synchronized int appendRootLevelRecord(Record newRecord) {
		int addedAt = -1;
		Record[] r = new Record[(_records.length) + 1];
		boolean added = false;
		for (int i = (_records.length) - 1; i >= 0; i--) {
			if (added) {
				r[i] = _records[i];
			}else {
				r[(i + 1)] = _records[i];
				if ((_records[i]) instanceof PersistPtrHolder) {
					r[i] = newRecord;
					added = true;
					addedAt = i;
				}
			}
		}
		_records = r;
		return addedAt;
	}

	public int addPicture(HSLFPictureData img) {
		if ((_pictures) == null) {
			try {
				readPictures();
			} catch (IOException e) {
				throw new CorruptPowerPointFileException(e.getMessage());
			}
		}
		int offset = 0;
		if ((_pictures.size()) > 0) {
			HSLFPictureData prev = _pictures.get(((_pictures.size()) - 1));
			offset = ((prev.getOffset()) + (prev.getRawData().length)) + 8;
		}
		img.setOffset(offset);
		img.setIndex(((_pictures.size()) + 1));
		_pictures.add(img);
		return offset;
	}

	public Record[] getRecords() {
		return _records;
	}

	public byte[] getUnderlyingBytes() {
		return _docstream;
	}

	public CurrentUserAtom getCurrentUserAtom() {
		return currentUser;
	}

	public List<HSLFPictureData> getPictureData() {
		if ((_pictures) == null) {
			try {
				readPictures();
			} catch (IOException e) {
				throw new CorruptPowerPointFileException(e.getMessage());
			}
		}
		return Collections.unmodifiableList(_pictures);
	}

	public HSLFObjectData[] getEmbeddedObjects() {
		if ((_objects) == null) {
			List<HSLFObjectData> objects = new ArrayList<>();
			for (Record r : _records) {
				if (r instanceof ExOleObjStg) {
					objects.add(new HSLFObjectData(((ExOleObjStg) (r))));
				}
			}
			_objects = objects.toArray(new HSLFObjectData[0]);
		}
		return _objects;
	}

	@Override
	public void close() throws IOException {
		if ((getDirectory().getParent()) == null) {
			POIFSFileSystem fs = getDirectory().getFileSystem();
			if (fs != null) {
				fs.close();
			}
		}
	}

	@Override
	protected String getEncryptedPropertyStreamName() {
		return "EncryptedSummary";
	}

	private static class BufAccessBAOS extends ByteArrayOutputStream {
		public byte[] getBuf() {
			return buf;
		}
	}

	private static class CountingOS extends OutputStream {
		int count;

		@Override
		public void write(int b) throws IOException {
			(count)++;
		}

		@Override
		public void write(byte[] b) throws IOException {
			count += b.length;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			count += len;
		}

		public int size() {
			return count;
		}
	}
}

