

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.common.usermodel.fonts.FontInfo;
import org.apache.poi.ddf.EscherBSERecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.hpsf.ClassID;
import org.apache.poi.hpsf.ClassIDPredefined;
import org.apache.poi.hpsf.extractor.HPSFPropertiesExtractor;
import org.apache.poi.hslf.exceptions.CorruptPowerPointFileException;
import org.apache.poi.hslf.exceptions.HSLFException;
import org.apache.poi.hslf.model.HeadersFooters;
import org.apache.poi.hslf.model.MovieShape;
import org.apache.poi.hslf.record.CString;
import org.apache.poi.hslf.record.Document;
import org.apache.poi.hslf.record.DocumentAtom;
import org.apache.poi.hslf.record.Environment;
import org.apache.poi.hslf.record.ExAviMovie;
import org.apache.poi.hslf.record.ExControl;
import org.apache.poi.hslf.record.ExEmbed;
import org.apache.poi.hslf.record.ExEmbedAtom;
import org.apache.poi.hslf.record.ExMCIMovie;
import org.apache.poi.hslf.record.ExMediaAtom;
import org.apache.poi.hslf.record.ExObjList;
import org.apache.poi.hslf.record.ExObjListAtom;
import org.apache.poi.hslf.record.ExOleObjAtom;
import org.apache.poi.hslf.record.ExOleObjStg;
import org.apache.poi.hslf.record.ExVideoContainer;
import org.apache.poi.hslf.record.FontCollection;
import org.apache.poi.hslf.record.HeadersFootersContainer;
import org.apache.poi.hslf.record.MainMaster;
import org.apache.poi.hslf.record.Notes;
import org.apache.poi.hslf.record.PPDrawingGroup;
import org.apache.poi.hslf.record.PersistPtrHolder;
import org.apache.poi.hslf.record.PositionDependentRecord;
import org.apache.poi.hslf.record.PositionDependentRecordContainer;
import org.apache.poi.hslf.record.Record;
import org.apache.poi.hslf.record.RecordContainer;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.hslf.record.Slide;
import org.apache.poi.hslf.record.SlideAtom;
import org.apache.poi.hslf.record.SlideListWithText;
import org.apache.poi.hslf.record.SlidePersistAtom;
import org.apache.poi.hslf.record.TxMasterStyleAtom;
import org.apache.poi.hslf.record.UserEditAtom;
import org.apache.poi.hslf.usermodel.HSLFFontInfo;
import org.apache.poi.hslf.usermodel.HSLFNotes;
import org.apache.poi.hslf.usermodel.HSLFObjectData;
import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFShapeContainer;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideMaster;
import org.apache.poi.hslf.usermodel.HSLFSlideShowImpl;
import org.apache.poi.hslf.usermodel.HSLFSoundData;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.hslf.usermodel.HSLFTitleMaster;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.Ole10Native;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.Resources;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;

import static org.apache.poi.sl.usermodel.PictureData.PictureType.EMF;
import static org.apache.poi.sl.usermodel.PictureData.PictureType.PICT;
import static org.apache.poi.sl.usermodel.PictureData.PictureType.WMF;


public final class HSLFSlideShow implements Closeable , SlideShow<HSLFShape, HSLFTextParagraph> {
	private static final int MAX_RECORD_LENGTH = 10000000;

	public static final String POWERPOINT_DOCUMENT = "PowerPoint Document";

	public static final String PP95_DOCUMENT = "PP40";

	enum LoadSavePhase {

		INIT,
		LOADED;}

	private static final ThreadLocal<HSLFSlideShow.LoadSavePhase> loadSavePhase = new ThreadLocal<>();

	private final HSLFSlideShowImpl _hslfSlideShow;

	private Record[] _mostRecentCoreRecords;

	private Map<Integer, Integer> _sheetIdToCoreRecordsLookup;

	private Document _documentRecord;

	private final List<HSLFSlideMaster> _masters = new ArrayList<>();

	private final List<HSLFTitleMaster> _titleMasters = new ArrayList<>();

	private final List<HSLFSlide> _slides = new ArrayList<>();

	private final List<HSLFNotes> _notes = new ArrayList<>();

	private FontCollection _fonts;

	private static final POILogger logger = POILogFactory.getLogger(HSLFSlideShow.class);

	public HSLFSlideShow(HSLFSlideShowImpl hslfSlideShow) {
		HSLFSlideShow.loadSavePhase.set(HSLFSlideShow.LoadSavePhase.INIT);
		_hslfSlideShow = hslfSlideShow;
		for (Record record : _hslfSlideShow.getRecords()) {
			if (record instanceof RecordContainer) {
				RecordContainer.handleParentAwareRecords(((RecordContainer) (record)));
			}
		}
		findMostRecentCoreRecords();
		buildSlidesAndNotes();
		HSLFSlideShow.loadSavePhase.set(HSLFSlideShow.LoadSavePhase.LOADED);
	}

	public HSLFSlideShow() {
		this(HSLFSlideShowImpl.create());
	}

	@SuppressWarnings("resource")
	public HSLFSlideShow(InputStream inputStream) throws IOException {
		this(new HSLFSlideShowImpl(inputStream));
	}

	@SuppressWarnings("resource")
	public HSLFSlideShow(POIFSFileSystem npoifs) throws IOException {
		this(new HSLFSlideShowImpl(npoifs));
	}

	@SuppressWarnings("resource")
	public HSLFSlideShow(DirectoryNode root) throws IOException {
		this(new HSLFSlideShowImpl(root));
	}

	static HSLFSlideShow.LoadSavePhase getLoadSavePhase() {
		return HSLFSlideShow.loadSavePhase.get();
	}

	private void findMostRecentCoreRecords() {
		Map<Integer, Integer> mostRecentByBytes = new HashMap<>();
		for (Record record : _hslfSlideShow.getRecords()) {
			if (record instanceof PersistPtrHolder) {
				PersistPtrHolder pph = ((PersistPtrHolder) (record));
				int[] ids = pph.getKnownSlideIDs();
				for (int id : ids) {
					mostRecentByBytes.remove(id);
				}
				Map<Integer, Integer> thisSetOfLocations = pph.getSlideLocationsLookup();
				for (int id : ids) {
					mostRecentByBytes.put(id, thisSetOfLocations.get(id));
				}
			}
		}
		_mostRecentCoreRecords = new Record[mostRecentByBytes.size()];
		_sheetIdToCoreRecordsLookup = new HashMap<>();
		Integer[] allIDs = mostRecentByBytes.keySet().toArray(new Integer[0]);
		Arrays.sort(allIDs);
		for (int i = 0; i < (allIDs.length); i++) {
			_sheetIdToCoreRecordsLookup.put(allIDs[i], i);
		}
		Map<Integer, Integer> mostRecentByBytesRev = new HashMap<>(mostRecentByBytes.size());
		for (Map.Entry<Integer, Integer> me : mostRecentByBytes.entrySet()) {
			mostRecentByBytesRev.put(me.getValue(), me.getKey());
		}
		for (Record record : _hslfSlideShow.getRecords()) {
			if (!(record instanceof PositionDependentRecord)) {
				continue;
			}
			PositionDependentRecord pdr = ((PositionDependentRecord) (record));
			int recordAt = pdr.getLastOnDiskOffset();
			Integer thisID = mostRecentByBytesRev.get(recordAt);
			if (thisID == null) {
				continue;
			}
			int storeAt = _sheetIdToCoreRecordsLookup.get(thisID);
			if (pdr instanceof PositionDependentRecordContainer) {
				PositionDependentRecordContainer pdrc = ((PositionDependentRecordContainer) (record));
				pdrc.setSheetId(thisID);
			}
			_mostRecentCoreRecords[storeAt] = record;
		}
		for (Record record : _mostRecentCoreRecords) {
			if (record != null) {
				if ((record.getRecordType()) == (RecordTypes.Document.typeID)) {
					_documentRecord = ((Document) (record));
					_fonts = _documentRecord.getEnvironment().getFontCollection();
				}
			}
		}
	}

	private Record getCoreRecordForSAS(SlideListWithText.SlideAtomsSet sas) {
		SlidePersistAtom spa = sas.getSlidePersistAtom();
		int refID = spa.getRefID();
		return getCoreRecordForRefID(refID);
	}

	private Record getCoreRecordForRefID(int refID) {
		Integer coreRecordId = _sheetIdToCoreRecordsLookup.get(refID);
		if (coreRecordId != null) {
			return _mostRecentCoreRecords[coreRecordId];
		}
		HSLFSlideShow.logger.log(POILogger.ERROR, ("We tried to look up a reference to a core record, but there was no core ID for reference ID " + refID));
		return null;
	}

	private void buildSlidesAndNotes() {
		if ((_documentRecord) == null) {
			throw new CorruptPowerPointFileException("The PowerPoint file didn't contain a Document Record in its PersistPtr blocks. It is probably corrupt.");
		}
		findMasterSlides();
		Map<Integer, Integer> slideIdToNotes = new HashMap<>();
		findNotesSlides(slideIdToNotes);
		findSlides(slideIdToNotes);
	}

	private void findMasterSlides() {
		SlideListWithText masterSLWT = _documentRecord.getMasterSlideListWithText();
		if (masterSLWT == null) {
			return;
		}
		for (SlideListWithText.SlideAtomsSet sas : masterSLWT.getSlideAtomsSets()) {
			Record r = getCoreRecordForSAS(sas);
			int sheetNo = sas.getSlidePersistAtom().getSlideIdentifier();
			if (r instanceof Slide) {
				HSLFTitleMaster master = new HSLFTitleMaster(((Slide) (r)), sheetNo);
				_titleMasters.add(master);
			}else
				if (r instanceof MainMaster) {
					HSLFSlideMaster master = new HSLFSlideMaster(((MainMaster) (r)), sheetNo);
					_masters.add(master);
				}

		}
	}

	private void findNotesSlides(Map<Integer, Integer> slideIdToNotes) {
		SlideListWithText notesSLWT = _documentRecord.getNotesSlideListWithText();
		if (notesSLWT == null) {
			return;
		}
		int idx = -1;
		for (SlideListWithText.SlideAtomsSet notesSet : notesSLWT.getSlideAtomsSets()) {
			idx++;
			Record r = getCoreRecordForSAS(notesSet);
			SlidePersistAtom spa = notesSet.getSlidePersistAtom();
			String loggerLoc = (("A Notes SlideAtomSet at " + idx) + " said its record was at refID ") + (spa.getRefID());
			if (r == null) {
				HSLFSlideShow.logger.log(POILogger.WARN, (loggerLoc + ", but that record didn't exist - record ignored."));
				continue;
			}
			if (!(r instanceof Notes)) {
				HSLFSlideShow.logger.log(POILogger.ERROR, ((loggerLoc + ", but that was actually a ") + r));
				continue;
			}
			Notes notesRecord = ((Notes) (r));
			int slideId = spa.getSlideIdentifier();
			slideIdToNotes.put(slideId, idx);
			HSLFNotes hn = new HSLFNotes(notesRecord);
			_notes.add(hn);
		}
	}

	private void findSlides(Map<Integer, Integer> slideIdToNotes) {
		SlideListWithText slidesSLWT = _documentRecord.getSlideSlideListWithText();
		if (slidesSLWT == null) {
			return;
		}
		int idx = -1;
		for (SlideListWithText.SlideAtomsSet sas : slidesSLWT.getSlideAtomsSets()) {
			idx++;
			SlidePersistAtom spa = sas.getSlidePersistAtom();
			Record r = getCoreRecordForSAS(sas);
			if (!(r instanceof Slide)) {
				HSLFSlideShow.logger.log(POILogger.ERROR, ((((("A Slide SlideAtomSet at " + idx) + " said its record was at refID ") + (spa.getRefID())) + ", but that was actually a ") + r));
				continue;
			}
			Slide slide = ((Slide) (r));
			HSLFNotes notes = null;
			int noteId = slide.getSlideAtom().getNotesID();
			if (noteId != 0) {
				Integer notesPos = slideIdToNotes.get(noteId);
				if (((notesPos != null) && (0 <= notesPos)) && (notesPos < (_notes.size()))) {
					notes = _notes.get(notesPos);
				}else {
					HSLFSlideShow.logger.log(POILogger.ERROR, ("Notes not found for noteId=" + noteId));
				}
			}
			int slideIdentifier = spa.getSlideIdentifier();
			HSLFSlide hs = new HSLFSlide(slide, notes, sas, slideIdentifier, (idx + 1));
			_slides.add(hs);
		}
	}

	@Override
	public void write(OutputStream out) throws IOException {
		for (HSLFSlide sl : getSlides()) {
			writeDirtyParagraphs(sl);
		}
		for (HSLFSlideMaster sl : getSlideMasters()) {
			boolean isDirty = false;
			for (List<HSLFTextParagraph> paras : sl.getTextParagraphs()) {
				for (HSLFTextParagraph p : paras) {
					isDirty |= p.isDirty();
				}
			}
			if (isDirty) {
				for (TxMasterStyleAtom sa : sl.getTxMasterStyleAtoms()) {
					if (sa != null) {
						sa.updateStyles();
					}
				}
			}
		}
		_hslfSlideShow.write(out);
	}

	private void writeDirtyParagraphs(HSLFShapeContainer container) {
		for (HSLFShape sh : container.getShapes()) {
			if (sh instanceof HSLFShapeContainer) {
				writeDirtyParagraphs(((HSLFShapeContainer) (sh)));
			}else
				if (sh instanceof HSLFTextShape) {
					HSLFTextShape hts = ((HSLFTextShape) (sh));
					boolean isDirty = false;
					for (HSLFTextParagraph p : hts.getTextParagraphs()) {
						isDirty |= p.isDirty();
					}
					if (isDirty) {
					}
				}

		}
	}

	public Record[] getMostRecentCoreRecords() {
		return _mostRecentCoreRecords;
	}

	@Override
	public List<HSLFSlide> getSlides() {
		return _slides;
	}

	public List<HSLFNotes> getNotes() {
		return _notes;
	}

	@Override
	public List<HSLFSlideMaster> getSlideMasters() {
		return _masters;
	}

	public List<HSLFTitleMaster> getTitleMasters() {
		return _titleMasters;
	}

	@Override
	public List<HSLFPictureData> getPictureData() {
		return _hslfSlideShow.getPictureData();
	}

	@SuppressWarnings("WeakerAccess")
	public HSLFObjectData[] getEmbeddedObjects() {
		return _hslfSlideShow.getEmbeddedObjects();
	}

	public HSLFSoundData[] getSoundData() {
		return HSLFSoundData.find(_documentRecord);
	}

	@Override
	public Dimension getPageSize() {
		DocumentAtom docatom = _documentRecord.getDocumentAtom();
		int pgx = ((int) (Units.masterToPoints(((int) (docatom.getSlideSizeX())))));
		int pgy = ((int) (Units.masterToPoints(((int) (docatom.getSlideSizeY())))));
		return new Dimension(pgx, pgy);
	}

	@Override
	public void setPageSize(Dimension pgsize) {
		DocumentAtom docatom = _documentRecord.getDocumentAtom();
		docatom.setSlideSizeX(Units.pointsToMaster(pgsize.width));
		docatom.setSlideSizeY(Units.pointsToMaster(pgsize.height));
	}

	FontCollection getFontCollection() {
		return _fonts;
	}

	public Document getDocumentRecord() {
		return _documentRecord;
	}

	@SuppressWarnings("WeakerAccess")
	public void reorderSlide(int oldSlideNumber, int newSlideNumber) {
		if ((oldSlideNumber < 1) || (newSlideNumber < 1)) {
			throw new IllegalArgumentException("Old and new slide numbers must be greater than 0");
		}
		if ((oldSlideNumber > (_slides.size())) || (newSlideNumber > (_slides.size()))) {
			throw new IllegalArgumentException((("Old and new slide numbers must not exceed the number of slides (" + (_slides.size())) + ")"));
		}
		SlideListWithText slwt = _documentRecord.getSlideSlideListWithText();
		if (slwt == null) {
			throw new IllegalStateException("Slide record not defined.");
		}
		SlideListWithText.SlideAtomsSet[] sas = slwt.getSlideAtomsSets();
		SlideListWithText.SlideAtomsSet tmp = sas[(oldSlideNumber - 1)];
		sas[(oldSlideNumber - 1)] = sas[(newSlideNumber - 1)];
		sas[(newSlideNumber - 1)] = tmp;
		Collections.swap(_slides, (oldSlideNumber - 1), (newSlideNumber - 1));
		_slides.get((newSlideNumber - 1)).setSlideNumber(newSlideNumber);
		_slides.get((oldSlideNumber - 1)).setSlideNumber(oldSlideNumber);
		ArrayList<Record> lst = new ArrayList<>();
		for (SlideListWithText.SlideAtomsSet s : sas) {
			lst.add(s.getSlidePersistAtom());
			lst.addAll(Arrays.asList(s.getSlideRecords()));
		}
		Record[] r = lst.toArray(new Record[0]);
		slwt.setChildRecord(r);
	}

	@SuppressWarnings("WeakerAccess")
	public HSLFSlide removeSlide(int index) {
		int lastSlideIdx = (_slides.size()) - 1;
		if ((index < 0) || (index > lastSlideIdx)) {
			throw new IllegalArgumentException((((("Slide index (" + index) + ") is out of range (0..") + lastSlideIdx) + ")"));
		}
		SlideListWithText slwt = _documentRecord.getSlideSlideListWithText();
		if (slwt == null) {
			throw new IllegalStateException("Slide record not defined.");
		}
		SlideListWithText.SlideAtomsSet[] sas = slwt.getSlideAtomsSets();
		List<Record> records = new ArrayList<>();
		List<SlideListWithText.SlideAtomsSet> sa = new ArrayList<>(Arrays.asList(sas));
		HSLFSlide removedSlide = _slides.remove(index);
		_notes.remove(removedSlide.getNotes());
		sa.remove(index);
		int i = 0;
		for (HSLFSlide s : _slides) {
			s.setSlideNumber((i++));
		}
		for (SlideListWithText.SlideAtomsSet s : sa) {
			records.add(s.getSlidePersistAtom());
			records.addAll(Arrays.asList(s.getSlideRecords()));
		}
		if (sa.isEmpty()) {
			_documentRecord.removeSlideListWithText(slwt);
		}else {
			slwt.setSlideAtomsSets(sa.toArray(new SlideListWithText.SlideAtomsSet[0]));
			slwt.setChildRecord(records.toArray(new Record[0]));
		}
		int notesId = removedSlide.getSlideRecord().getSlideAtom().getNotesID();
		if (notesId != 0) {
			SlideListWithText nslwt = _documentRecord.getNotesSlideListWithText();
			records = new ArrayList<>();
			ArrayList<SlideListWithText.SlideAtomsSet> na = new ArrayList<>();
			if (nslwt != null) {
				for (SlideListWithText.SlideAtomsSet ns : nslwt.getSlideAtomsSets()) {
					if ((ns.getSlidePersistAtom().getSlideIdentifier()) == notesId) {
						continue;
					}
					na.add(ns);
					records.add(ns.getSlidePersistAtom());
					if ((ns.getSlideRecords()) != null) {
						records.addAll(Arrays.asList(ns.getSlideRecords()));
					}
				}
				if (!(na.isEmpty())) {
					nslwt.setSlideAtomsSets(na.toArray(new SlideListWithText.SlideAtomsSet[0]));
					nslwt.setChildRecord(records.toArray(new Record[0]));
				}
			}
			if (na.isEmpty()) {
				_documentRecord.removeSlideListWithText(nslwt);
			}
		}
		return removedSlide;
	}

	@Override
	public HSLFSlide createSlide() {
		SlideListWithText slist = _documentRecord.getSlideSlideListWithText();
		if (slist == null) {
			slist = new SlideListWithText();
			slist.setInstance(SlideListWithText.SLIDES);
			_documentRecord.addSlideListWithText(slist);
		}
		SlidePersistAtom prev = null;
		for (SlideListWithText.SlideAtomsSet sas : slist.getSlideAtomsSets()) {
			SlidePersistAtom spa = sas.getSlidePersistAtom();
			if ((spa.getSlideIdentifier()) >= 0) {
				if (prev == null) {
					prev = spa;
				}
				if ((prev.getSlideIdentifier()) < (spa.getSlideIdentifier())) {
					prev = spa;
				}
			}
		}
		SlidePersistAtom sp = new SlidePersistAtom();
		sp.setSlideIdentifier((prev == null ? 256 : (prev.getSlideIdentifier()) + 1));
		slist.addSlidePersistAtom(sp);
		HSLFSlide slide = new HSLFSlide(sp.getSlideIdentifier(), sp.getRefID(), ((_slides.size()) + 1));
		slide.onCreate();
		_slides.add(slide);
		HSLFSlideShow.logger.log(POILogger.INFO, ((((("Added slide " + (_slides.size())) + " with ref ") + (sp.getRefID())) + " and identifier ") + (sp.getSlideIdentifier())));
		Slide slideRecord = slide.getSlideRecord();
		int psrId = addPersistentObject(slideRecord);
		sp.setRefID(psrId);
		slideRecord.setSheetId(psrId);
		slide.setMasterSheet(_masters.get(0));
		return slide;
	}

	@Override
	public HSLFPictureData addPicture(byte[] data, PictureData.PictureType format) throws IOException {
		if ((format == null) || ((format.nativeId) == (-1))) {
			throw new IllegalArgumentException(("Unsupported picture format: " + format));
		}
		HSLFPictureData pd = findPictureData(data);
		if (pd != null) {
			return pd;
		}
		EscherContainerRecord bstore;
		EscherContainerRecord dggContainer = _documentRecord.getPPDrawingGroup().getDggContainer();
		bstore = HSLFShape.getEscherChild(dggContainer, EscherContainerRecord.BSTORE_CONTAINER);
		if (bstore == null) {
			bstore = new EscherContainerRecord();
			bstore.setRecordId(EscherContainerRecord.BSTORE_CONTAINER);
			dggContainer.addChildBefore(bstore, EscherOptRecord.RECORD_ID);
		}
		HSLFPictureData pict = HSLFPictureData.create(format);
		pict.setData(data);
		int offset = _hslfSlideShow.addPicture(pict);
		EscherBSERecord bse = new EscherBSERecord();
		bse.setRecordId(EscherBSERecord.RECORD_ID);
		bse.setOptions(((short) (2 | ((format.nativeId) << 4))));
		bse.setSize(((pict.getRawData().length) + 8));
		byte[] uid = HSLFPictureData.getChecksum(data);
		bse.setUid(uid);
		bse.setBlipTypeMacOS(((byte) (format.nativeId)));
		bse.setBlipTypeWin32(((byte) (format.nativeId)));
		if (format == (EMF)) {
			bse.setBlipTypeMacOS(((byte) (PICT.nativeId)));
		}else
			if (format == (WMF)) {
				bse.setBlipTypeMacOS(((byte) (PICT.nativeId)));
			}else
				if (format == (PICT)) {
					bse.setBlipTypeWin32(((byte) (WMF.nativeId)));
				}


		bse.setRef(0);
		bse.setOffset(offset);
		bse.setRemainingData(new byte[0]);
		bstore.addChildRecord(bse);
		int count = bstore.getChildRecords().size();
		bstore.setOptions(((short) ((count << 4) | 15)));
		return pict;
	}

	@Override
	public HSLFPictureData addPicture(InputStream is, PictureData.PictureType format) throws IOException {
		if ((format == null) || ((format.nativeId) == (-1))) {
			throw new IllegalArgumentException(("Unsupported picture format: " + format));
		}
		return addPicture(IOUtils.toByteArray(is), format);
	}

	@Override
	public HSLFPictureData addPicture(File pict, PictureData.PictureType format) throws IOException {
		if ((format == null) || ((format.nativeId) == (-1))) {
			throw new IllegalArgumentException(("Unsupported picture format: " + format));
		}
		byte[] data = IOUtils.safelyAllocate(pict.length(), HSLFSlideShow.MAX_RECORD_LENGTH);
		try (FileInputStream is = new FileInputStream(pict)) {
			IOUtils.readFully(is, data);
		}
		return addPicture(data, format);
	}

	@Override
	public HSLFPictureData findPictureData(byte[] pictureData) {
		byte[] uid = HSLFPictureData.getChecksum(pictureData);
		for (HSLFPictureData pic : getPictureData()) {
			if (Arrays.equals(pic.getUID(), uid)) {
				return pic;
			}
		}
		return null;
	}

	public HSLFFontInfo addFont(FontInfo fontInfo) {
		return getDocumentRecord().getEnvironment().getFontCollection().addFont(fontInfo);
	}

	public HSLFFontInfo getFont(int idx) {
		return getDocumentRecord().getEnvironment().getFontCollection().getFontInfo(idx);
	}

	public int getNumberOfFonts() {
		return getDocumentRecord().getEnvironment().getFontCollection().getNumberOfFonts();
	}

	public HeadersFooters getSlideHeadersFooters() {
		return null;
	}

	public HeadersFooters getNotesHeadersFooters() {
		if (_notes.isEmpty()) {
		}else {
			return new HeadersFooters(_notes.get(0), HeadersFootersContainer.NotesHeadersFootersContainer);
		}
		return null;
	}

	public int addMovie(String path, int type) {
		ExMCIMovie mci;
		switch (type) {
			case MovieShape.MOVIE_MPEG :
				mci = new ExMCIMovie();
				break;
			case MovieShape.MOVIE_AVI :
				mci = new ExAviMovie();
				break;
			default :
				throw new IllegalArgumentException(("Unsupported Movie: " + type));
		}
		ExVideoContainer exVideo = mci.getExVideo();
		exVideo.getExMediaAtom().setMask(15204352);
		exVideo.getPathAtom().setText(path);
		int objectId = addToObjListAtom(mci);
		exVideo.getExMediaAtom().setObjectId(objectId);
		return objectId;
	}

	@SuppressWarnings("unused")
	public int addControl(String name, String progId) {
		ExControl ctrl = new ExControl();
		ctrl.setProgId(progId);
		ctrl.setMenuName(name);
		ctrl.setClipboardName(name);
		ExOleObjAtom oleObj = ctrl.getExOleObjAtom();
		oleObj.setDrawAspect(ExOleObjAtom.DRAW_ASPECT_VISIBLE);
		oleObj.setType(ExOleObjAtom.TYPE_CONTROL);
		oleObj.setSubType(ExOleObjAtom.SUBTYPE_DEFAULT);
		int objectId = addToObjListAtom(ctrl);
		oleObj.setObjID(objectId);
		return objectId;
	}

	public int addEmbed(POIFSFileSystem poiData) {
		DirectoryNode root = poiData.getRoot();
		if (new ClassID().equals(root.getStorageClsid())) {
			Map<String, ClassID> olemap = HSLFSlideShow.getOleMap();
			ClassID classID = null;
			for (Map.Entry<String, ClassID> entry : olemap.entrySet()) {
				if (root.hasEntry(entry.getKey())) {
					classID = entry.getValue();
					break;
				}
			}
			if (classID == null) {
				throw new IllegalArgumentException("Unsupported embedded document");
			}
			root.setStorageClsid(classID);
		}
		ExEmbed exEmbed = new ExEmbed();
		Record[] children = exEmbed.getChildRecords();
		exEmbed.removeChild(children[2]);
		exEmbed.removeChild(children[3]);
		exEmbed.removeChild(children[4]);
		ExEmbedAtom eeEmbed = exEmbed.getExEmbedAtom();
		eeEmbed.setCantLockServerB(true);
		ExOleObjAtom eeAtom = exEmbed.getExOleObjAtom();
		eeAtom.setDrawAspect(ExOleObjAtom.DRAW_ASPECT_VISIBLE);
		eeAtom.setType(ExOleObjAtom.TYPE_EMBEDDED);
		eeAtom.setOptions(1226240);
		ExOleObjStg exOleObjStg = new ExOleObjStg();
		try {
			Ole10Native.createOleMarkerEntry(poiData);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			poiData.writeFilesystem(bos);
			exOleObjStg.setData(bos.toByteArray());
		} catch (IOException e) {
			throw new HSLFException(e);
		}
		int psrId = addPersistentObject(exOleObjStg);
		exOleObjStg.setPersistId(psrId);
		eeAtom.setObjStgDataRef(psrId);
		int objectId = addToObjListAtom(exEmbed);
		eeAtom.setObjID(objectId);
		return objectId;
	}

	@Override
	public HPSFPropertiesExtractor getMetadataTextExtractor() {
		return new HPSFPropertiesExtractor(getSlideShowImpl());
	}

	int addToObjListAtom(RecordContainer exObj) {
		ExObjList lst = getDocumentRecord().getExObjList(true);
		ExObjListAtom objAtom = lst.getExObjListAtom();
		int objectId = ((int) (objAtom.getObjectIDSeed())) + 1;
		objAtom.setObjectIDSeed(objectId);
		lst.addChildAfter(exObj, objAtom);
		return objectId;
	}

	private static Map<String, ClassID> getOleMap() {
		Map<String, ClassID> olemap = new HashMap<>();
		olemap.put(HSLFSlideShow.POWERPOINT_DOCUMENT, ClassIDPredefined.POWERPOINT_V8.getClassID());
		olemap.put("Workbook", ClassIDPredefined.EXCEL_V8.getClassID());
		olemap.put("WORKBOOK", ClassIDPredefined.EXCEL_V8.getClassID());
		olemap.put("BOOK", ClassIDPredefined.EXCEL_V8.getClassID());
		return olemap;
	}

	private int addPersistentObject(PositionDependentRecord slideRecord) {
		_hslfSlideShow.appendRootLevelRecord(((Record) (slideRecord)));
		Map<RecordTypes, PositionDependentRecord> interestingRecords = new HashMap<>();
		try {
			_hslfSlideShow.updateAndWriteDependantRecords(null, interestingRecords);
		} catch (IOException e) {
			throw new HSLFException(e);
		}
		PersistPtrHolder ptr = ((PersistPtrHolder) (interestingRecords.get(RecordTypes.PersistPtrIncrementalBlock)));
		UserEditAtom usr = ((UserEditAtom) (interestingRecords.get(RecordTypes.UserEditAtom)));
		int psrId = (usr.getMaxPersistWritten()) + 1;
		usr.setLastViewType(((short) (UserEditAtom.LAST_VIEW_SLIDE_VIEW)));
		usr.setMaxPersistWritten(psrId);
		int slideOffset = slideRecord.getLastOnDiskOffset();
		slideRecord.setLastOnDiskOffset(slideOffset);
		ptr.addSlideLookup(psrId, slideOffset);
		HSLFSlideShow.logger.log(POILogger.INFO, ("New slide/object ended up at " + slideOffset));
		return psrId;
	}

	@Override
	public MasterSheet<HSLFShape, HSLFTextParagraph> createMasterSheet() {
		return null;
	}

	@Override
	public Resources getResources() {
		return null;
	}

	@org.apache.poi.util.Internal
	public HSLFSlideShowImpl getSlideShowImpl() {
		return _hslfSlideShow;
	}

	@Override
	public void close() throws IOException {
		_hslfSlideShow.close();
	}

	@Override
	public Object getPersistDocument() {
		return getSlideShowImpl();
	}
}

