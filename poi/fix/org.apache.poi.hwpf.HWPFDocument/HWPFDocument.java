

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import org.apache.poi.POIDocument;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hwpf.HWPFDocumentCore;
import org.apache.poi.hwpf.OldWordFileFormatException;
import org.apache.poi.hwpf.model.BookmarksTables;
import org.apache.poi.hwpf.model.CHPBinTable;
import org.apache.poi.hwpf.model.ComplexFileTable;
import org.apache.poi.hwpf.model.DocumentProperties;
import org.apache.poi.hwpf.model.EscherRecordHolder;
import org.apache.poi.hwpf.model.FSPADocumentPart;
import org.apache.poi.hwpf.model.FSPATable;
import org.apache.poi.hwpf.model.FibBase;
import org.apache.poi.hwpf.model.FieldsTables;
import org.apache.poi.hwpf.model.FileInformationBlock;
import org.apache.poi.hwpf.model.FontTable;
import org.apache.poi.hwpf.model.LFO;
import org.apache.poi.hwpf.model.LFOData;
import org.apache.poi.hwpf.model.ListData;
import org.apache.poi.hwpf.model.ListTables;
import org.apache.poi.hwpf.model.NoteType;
import org.apache.poi.hwpf.model.NotesTables;
import org.apache.poi.hwpf.model.PAPBinTable;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.model.RevisionMarkAuthorTable;
import org.apache.poi.hwpf.model.SavedByTable;
import org.apache.poi.hwpf.model.SectionTable;
import org.apache.poi.hwpf.model.SinglentonTextPiece;
import org.apache.poi.hwpf.model.StyleSheet;
import org.apache.poi.hwpf.model.SubdocumentType;
import org.apache.poi.hwpf.model.TextPiece;
import org.apache.poi.hwpf.model.TextPieceTable;
import org.apache.poi.hwpf.model.io.HWPFFileSystem;
import org.apache.poi.hwpf.model.types.FibBaseAbstractType;
import org.apache.poi.hwpf.usermodel.Bookmarks;
import org.apache.poi.hwpf.usermodel.BookmarksImpl;
import org.apache.poi.hwpf.usermodel.Fields;
import org.apache.poi.hwpf.usermodel.FieldsImpl;
import org.apache.poi.hwpf.usermodel.HWPFList;
import org.apache.poi.hwpf.usermodel.Notes;
import org.apache.poi.hwpf.usermodel.NotesImpl;
import org.apache.poi.hwpf.usermodel.ObjectPoolImpl;
import org.apache.poi.hwpf.usermodel.OfficeDrawings;
import org.apache.poi.hwpf.usermodel.OfficeDrawingsImpl;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.common.POIFSConstants;
import org.apache.poi.poifs.crypt.ChunkedCipherOutputStream;
import org.apache.poi.poifs.crypt.EncryptionHeader;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.crypt.standard.EncryptionRecord;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.EntryUtils;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;


public final class HWPFDocument extends HWPFDocumentCore {
	static final String PROPERTY_PRESERVE_BIN_TABLES = "org.apache.poi.hwpf.preserveBinTables";

	private static final String PROPERTY_PRESERVE_TEXT_TABLE = "org.apache.poi.hwpf.preserveTextTable";

	private static final int MAX_RECORD_LENGTH = 1000000;

	private static final String STREAM_DATA = "Data";

	private byte[] _tableStream;

	private byte[] _dataStream;

	private DocumentProperties _dop;

	private ComplexFileTable _cft;

	private StringBuilder _text;

	private SavedByTable _sbt;

	private RevisionMarkAuthorTable _rmat;

	private FSPATable _fspaHeaders;

	private FSPATable _fspaMain;

	private EscherRecordHolder _escherRecordHolder;

	private PicturesTable _pictures;

	private OfficeDrawingsImpl _officeDrawingsHeaders;

	private OfficeDrawingsImpl _officeDrawingsMain;

	private BookmarksTables _bookmarksTables;

	private Bookmarks _bookmarks;

	private NotesTables _endnotesTables = new NotesTables(NoteType.ENDNOTE);

	private Notes _endnotes = new NotesImpl(_endnotesTables);

	private NotesTables _footnotesTables = new NotesTables(NoteType.FOOTNOTE);

	private Notes _footnotes = new NotesImpl(_footnotesTables);

	private FieldsTables _fieldsTables;

	private Fields _fields;

	public HWPFDocument(InputStream istream) throws IOException {
		this(HWPFDocumentCore.verifyAndBuildPOIFS(istream));
	}

	public HWPFDocument(POIFSFileSystem pfilesystem) throws IOException {
		this(pfilesystem.getRoot());
	}

	public HWPFDocument(DirectoryNode directory) throws IOException {
		super(directory);
		if ((_fib.getFibBase().getNFib()) < 106) {
			throw new OldWordFileFormatException("The document is too old - Word 95 or older. Try HWPFOldDocument instead?");
		}
		String name = (_fib.getFibBase().isFWhichTblStm()) ? HWPFDocumentCore.STREAM_TABLE_1 : HWPFDocumentCore.STREAM_TABLE_0;
		if (!(directory.hasEntry(name))) {
			throw new IllegalStateException((("Table Stream '" + name) + "' wasn't found - Either the document is corrupt, or is Word95 (or earlier)"));
		}
		_tableStream = getDocumentEntryBytes(name, _fib.getFibBase().getLKey(), Integer.MAX_VALUE);
		_fib.fillVariableFields(_mainStream, _tableStream);
		_dataStream = (directory.hasEntry(HWPFDocument.STREAM_DATA)) ? getDocumentEntryBytes(HWPFDocument.STREAM_DATA, 0, Integer.MAX_VALUE) : new byte[0];
		int fcMin = 0;
		_dop = new DocumentProperties(_tableStream, _fib.getFcDop(), _fib.getLcbDop());
		_cft = new ComplexFileTable(_mainStream, _tableStream, _fib.getFcClx(), fcMin);
		TextPieceTable _tpt = _cft.getTextPieceTable();
		_cbt = new CHPBinTable(_mainStream, _tableStream, _fib.getFcPlcfbteChpx(), _fib.getLcbPlcfbteChpx(), _tpt);
		_pbt = new PAPBinTable(_mainStream, _tableStream, _dataStream, _fib.getFcPlcfbtePapx(), _fib.getLcbPlcfbtePapx(), _tpt);
		_text = _tpt.getText();
		boolean preserveBinTables = false;
		try {
			preserveBinTables = Boolean.parseBoolean(System.getProperty(HWPFDocument.PROPERTY_PRESERVE_BIN_TABLES));
		} catch (Exception exc) {
		}
		if (!preserveBinTables) {
			_cbt.rebuild(_cft);
			_pbt.rebuild(_text, _cft);
		}
		boolean preserveTextTable = false;
		try {
			preserveTextTable = Boolean.parseBoolean(System.getProperty(HWPFDocument.PROPERTY_PRESERVE_TEXT_TABLE));
		} catch (Exception exc) {
		}
		if (!preserveTextTable) {
			_cft = new ComplexFileTable();
			_tpt = _cft.getTextPieceTable();
			final TextPiece textPiece = new SinglentonTextPiece(_text);
			_tpt.add(textPiece);
			_text = textPiece.getStringBuilder();
		}
		_fspaHeaders = new FSPATable(_tableStream, _fib, FSPADocumentPart.HEADER);
		_fspaMain = new FSPATable(_tableStream, _fib, FSPADocumentPart.MAIN);
		if ((_fib.getFcDggInfo()) != 0) {
			_escherRecordHolder = new EscherRecordHolder(_tableStream, _fib.getFcDggInfo(), _fib.getLcbDggInfo());
		}else {
			_escherRecordHolder = new EscherRecordHolder();
		}
		_officeDrawingsHeaders = new OfficeDrawingsImpl(_fspaHeaders, _escherRecordHolder, _mainStream);
		_officeDrawingsMain = new OfficeDrawingsImpl(_fspaMain, _escherRecordHolder, _mainStream);
		_st = new SectionTable(_mainStream, _tableStream, _fib.getFcPlcfsed(), _fib.getLcbPlcfsed(), fcMin, _tpt, _fib.getSubdocumentTextStreamLength(SubdocumentType.MAIN));
		_ss = new StyleSheet(_tableStream, _fib.getFcStshf());
		_ft = new FontTable(_tableStream, _fib.getFcSttbfffn(), _fib.getLcbSttbfffn());
		int listOffset = _fib.getFcPlfLst();
		if ((listOffset != 0) && ((_fib.getLcbPlfLst()) != 0)) {
			_lt = new ListTables(_tableStream, listOffset, _fib.getFcPlfLfo(), _fib.getLcbPlfLfo());
		}
		int sbtOffset = _fib.getFcSttbSavedBy();
		int sbtLength = _fib.getLcbSttbSavedBy();
		if ((sbtOffset != 0) && (sbtLength != 0)) {
			_sbt = new SavedByTable(_tableStream, sbtOffset, sbtLength);
		}
		int rmarkOffset = _fib.getFcSttbfRMark();
		int rmarkLength = _fib.getLcbSttbfRMark();
		if ((rmarkOffset != 0) && (rmarkLength != 0)) {
			_rmat = new RevisionMarkAuthorTable(_tableStream, rmarkOffset, rmarkLength);
		}
		_bookmarksTables = new BookmarksTables(_tableStream, _fib);
		_bookmarks = new BookmarksImpl(_bookmarksTables);
		_endnotesTables = new NotesTables(NoteType.ENDNOTE, _tableStream, _fib);
		_endnotes = new NotesImpl(_endnotesTables);
		_footnotesTables = new NotesTables(NoteType.FOOTNOTE, _tableStream, _fib);
		_footnotes = new NotesImpl(_footnotesTables);
		_fieldsTables = new FieldsTables(_tableStream, _fib);
		_fields = new FieldsImpl(_fieldsTables);
	}

	@Override
	@org.apache.poi.util.Internal
	public TextPieceTable getTextTable() {
		return _cft.getTextPieceTable();
	}

	@org.apache.poi.util.Internal
	@Override
	public StringBuilder getText() {
		return _text;
	}

	public DocumentProperties getDocProperties() {
		return _dop;
	}

	@Override
	public Range getOverallRange() {
		return new Range(0, _text.length(), this);
	}

	@Override
	public Range getRange() {
		return getRange(SubdocumentType.MAIN);
	}

	private Range getRange(SubdocumentType subdocument) {
		int startCp = 0;
		for (SubdocumentType previos : SubdocumentType.ORDERED) {
			int length = getFileInformationBlock().getSubdocumentTextStreamLength(previos);
			if (subdocument == previos) {
				return new Range(startCp, (startCp + length), this);
			}
			startCp += length;
		}
		throw new UnsupportedOperationException(("Subdocument type not supported: " + subdocument));
	}

	public Range getFootnoteRange() {
		return getRange(SubdocumentType.FOOTNOTE);
	}

	public Range getEndnoteRange() {
		return getRange(SubdocumentType.ENDNOTE);
	}

	public Range getCommentsRange() {
		return getRange(SubdocumentType.ANNOTATION);
	}

	public Range getMainTextboxRange() {
		return getRange(SubdocumentType.TEXTBOX);
	}

	public Range getHeaderStoryRange() {
		return getRange(SubdocumentType.HEADER);
	}

	public int characterLength() {
		return _text.length();
	}

	@org.apache.poi.util.Internal
	public SavedByTable getSavedByTable() {
		return _sbt;
	}

	@org.apache.poi.util.Internal
	public RevisionMarkAuthorTable getRevisionMarkAuthorTable() {
		return _rmat;
	}

	public PicturesTable getPicturesTable() {
		return _pictures;
	}

	@org.apache.poi.util.Internal
	public EscherRecordHolder getEscherRecordHolder() {
		return _escherRecordHolder;
	}

	public OfficeDrawings getOfficeDrawingsHeaders() {
		return _officeDrawingsHeaders;
	}

	public OfficeDrawings getOfficeDrawingsMain() {
		return _officeDrawingsMain;
	}

	public Bookmarks getBookmarks() {
		return _bookmarks;
	}

	public Notes getEndnotes() {
		return _endnotes;
	}

	public Notes getFootnotes() {
		return _footnotes;
	}

	@Deprecated
	@org.apache.poi.util.Internal
	public FieldsTables getFieldsTables() {
		return _fieldsTables;
	}

	public Fields getFields() {
		return _fields;
	}

	@Override
	public void write() throws IOException {
		validateInPlaceWritePossible();
		write(getDirectory().getFileSystem(), false);
		getDirectory().getFileSystem().writeFilesystem();
	}

	@Override
	public void write(File newFile) throws IOException {
		POIFSFileSystem pfs = POIFSFileSystem.create(newFile);
		write(pfs, true);
		pfs.writeFilesystem();
	}

	@Override
	public void write(OutputStream out) throws IOException {
		POIFSFileSystem pfs = new POIFSFileSystem();
		write(pfs, true);
		pfs.writeFilesystem(out);
	}

	private void write(POIFSFileSystem pfs, boolean copyOtherEntries) throws IOException {
		_fib.clearOffsetsSizes();
		int fibSize = _fib.getSize();
		fibSize += (POIFSConstants.SMALLER_BIG_BLOCK_SIZE) - (fibSize % (POIFSConstants.SMALLER_BIG_BLOCK_SIZE));
		HWPFFileSystem docSys = new HWPFFileSystem();
		ByteArrayOutputStream wordDocumentStream = docSys.getStream(HWPFDocumentCore.STREAM_WORD_DOCUMENT);
		ByteArrayOutputStream tableStream = docSys.getStream(HWPFDocumentCore.STREAM_TABLE_1);
		byte[] placeHolder = IOUtils.safelyAllocate(fibSize, HWPFDocument.MAX_RECORD_LENGTH);
		wordDocumentStream.write(placeHolder);
		int mainOffset = wordDocumentStream.size();
		int tableOffset = 0;
		updateEncryptionInfo();
		EncryptionInfo ei = getEncryptionInfo();
		if (ei != null) {
			byte[] buf = new byte[1000];
			LittleEndianByteArrayOutputStream leos = new LittleEndianByteArrayOutputStream(buf, 0);
			leos.writeShort(ei.getVersionMajor());
			leos.writeShort(ei.getVersionMinor());
			if ((ei.getEncryptionMode()) == (EncryptionMode.cryptoAPI)) {
				leos.writeInt(ei.getEncryptionFlags());
			}
			((EncryptionRecord) (ei.getHeader())).write(leos);
			((EncryptionRecord) (ei.getVerifier())).write(leos);
			tableStream.write(buf, 0, leos.getWriteIndex());
			tableOffset += leos.getWriteIndex();
			_fib.getFibBase().setLKey(tableOffset);
		}
		_fib.setFcStshf(tableOffset);
		_ss.writeTo(tableStream);
		_fib.setLcbStshf(((tableStream.size()) - tableOffset));
		tableOffset = tableStream.size();
		_fib.setFcClx(tableOffset);
		_cft.writeTo(wordDocumentStream, tableStream);
		_fib.setLcbClx(((tableStream.size()) - tableOffset));
		tableOffset = tableStream.size();
		int fcMac = wordDocumentStream.size();
		_fib.setFcDop(tableOffset);
		_dop.writeTo(tableStream);
		_fib.setLcbDop(((tableStream.size()) - tableOffset));
		tableOffset = tableStream.size();
		if ((_bookmarksTables) != null) {
			_bookmarksTables.writePlcfBkmkf(_fib, tableStream);
			tableOffset = tableStream.size();
		}
		if ((_bookmarksTables) != null) {
			_bookmarksTables.writePlcfBkmkl(_fib, tableStream);
			tableOffset = tableStream.size();
		}
		_fib.setFcPlcfbteChpx(tableOffset);
		_cbt.writeTo(wordDocumentStream, tableStream, mainOffset, _cft.getTextPieceTable());
		_fib.setLcbPlcfbteChpx(((tableStream.size()) - tableOffset));
		tableOffset = tableStream.size();
		_fib.setFcPlcfbtePapx(tableOffset);
		_pbt.writeTo(wordDocumentStream, tableStream, _cft.getTextPieceTable());
		_fib.setLcbPlcfbtePapx(((tableStream.size()) - tableOffset));
		tableOffset = tableStream.size();
		_endnotesTables.writeRef(_fib, tableStream);
		_endnotesTables.writeTxt(_fib, tableStream);
		tableOffset = tableStream.size();
		if ((_fieldsTables) != null) {
			_fieldsTables.write(_fib, tableStream);
			tableOffset = tableStream.size();
		}
		_footnotesTables.writeRef(_fib, tableStream);
		_footnotesTables.writeTxt(_fib, tableStream);
		tableOffset = tableStream.size();
		_fib.setFcPlcfsed(tableOffset);
		_st.writeTo(wordDocumentStream, tableStream);
		_fib.setLcbPlcfsed(((tableStream.size()) - tableOffset));
		tableOffset = tableStream.size();
		if ((_lt) != null) {
			_lt.writeListDataTo(_fib, tableStream);
			tableOffset = tableStream.size();
			_lt.writeListOverridesTo(_fib, tableStream);
			tableOffset = tableStream.size();
		}
		if ((_bookmarksTables) != null) {
			_bookmarksTables.writeSttbfBkmk(_fib, tableStream);
			tableOffset = tableStream.size();
		}
		if ((_sbt) != null) {
			_fib.setFcSttbSavedBy(tableOffset);
			_sbt.writeTo(tableStream);
			_fib.setLcbSttbSavedBy(((tableStream.size()) - tableOffset));
			tableOffset = tableStream.size();
		}
		if ((_rmat) != null) {
			_fib.setFcSttbfRMark(tableOffset);
			_rmat.writeTo(tableStream);
			_fib.setLcbSttbfRMark(((tableStream.size()) - tableOffset));
			tableOffset = tableStream.size();
		}
		_fib.setFcSttbfffn(tableOffset);
		_ft.writeTo(tableStream);
		_fib.setLcbSttbfffn(((tableStream.size()) - tableOffset));
		tableOffset = tableStream.size();
		_fib.getFibBase().setFcMin(mainOffset);
		_fib.getFibBase().setFcMac(fcMac);
		_fib.setCbMac(wordDocumentStream.size());
		byte[] mainBuf = HWPFDocument.fillUp4096(wordDocumentStream);
		_fib.getFibBase().setFWhichTblStm(true);
		_fib.writeTo(mainBuf, tableStream);
		byte[] tableBuf = HWPFDocument.fillUp4096(tableStream);
		byte[] dataBuf = HWPFDocument.fillUp4096(_dataStream);
		if (ei == null) {
			HWPFDocument.write(pfs, mainBuf, HWPFDocumentCore.STREAM_WORD_DOCUMENT);
			HWPFDocument.write(pfs, tableBuf, HWPFDocumentCore.STREAM_TABLE_1);
			HWPFDocument.write(pfs, dataBuf, HWPFDocument.STREAM_DATA);
		}else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(100000);
			encryptBytes(mainBuf, HWPFDocumentCore.FIB_BASE_LEN, bos);
			HWPFDocument.write(pfs, bos.toByteArray(), HWPFDocumentCore.STREAM_WORD_DOCUMENT);
			bos.reset();
			encryptBytes(tableBuf, _fib.getFibBase().getLKey(), bos);
			HWPFDocument.write(pfs, bos.toByteArray(), HWPFDocumentCore.STREAM_TABLE_1);
			bos.reset();
			encryptBytes(dataBuf, 0, bos);
			HWPFDocument.write(pfs, bos.toByteArray(), HWPFDocument.STREAM_DATA);
			bos.reset();
		}
		writeProperties(pfs);
		if (copyOtherEntries && (ei == null)) {
			DirectoryNode newRoot = pfs.getRoot();
			_objectPool.writeTo(newRoot);
			for (Entry entry : getDirectory()) {
				String entryName = entry.getName();
				if (!(((((((HWPFDocumentCore.STREAM_WORD_DOCUMENT.equals(entryName)) || (HWPFDocumentCore.STREAM_TABLE_0.equals(entryName))) || (HWPFDocumentCore.STREAM_TABLE_1.equals(entryName))) || (HWPFDocument.STREAM_DATA.equals(entryName))) || (HWPFDocumentCore.STREAM_OBJECT_POOL.equals(entryName))) || (SummaryInformation.DEFAULT_STREAM_NAME.equals(entryName))) || (DocumentSummaryInformation.DEFAULT_STREAM_NAME.equals(entryName)))) {
					EntryUtils.copyNodeRecursively(entry, newRoot);
				}
			}
		}
		replaceDirectory(pfs.getRoot());
		this._tableStream = tableStream.toByteArray();
		this._dataStream = dataBuf;
	}

	private void encryptBytes(byte[] plain, int encryptOffset, OutputStream bos) throws IOException {
		try {
			EncryptionInfo ei = getEncryptionInfo();
			Encryptor enc = ei.getEncryptor();
			enc.setChunkSize(HWPFDocumentCore.RC4_REKEYING_INTERVAL);
			ChunkedCipherOutputStream os = enc.getDataStream(bos, 0);
			if (encryptOffset > 0) {
				os.writePlain(plain, 0, encryptOffset);
			}
			os.write(plain, encryptOffset, ((plain.length) - encryptOffset));
			os.close();
		} catch (GeneralSecurityException e) {
			throw new IOException(e);
		}
	}

	private static byte[] fillUp4096(byte[] buf) {
		if (buf == null) {
			return new byte[4096];
		}else
			if ((buf.length) < 4096) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
				bos.write(buf, 0, buf.length);
				return HWPFDocument.fillUp4096(bos);
			}else {
				return buf;
			}

	}

	private static byte[] fillUp4096(ByteArrayOutputStream bos) {
		int fillSize = 4096 - (bos.size());
		if (fillSize > 0) {
			bos.write(new byte[fillSize], 0, fillSize);
		}
		return bos.toByteArray();
	}

	private static void write(POIFSFileSystem pfs, byte[] data, String name) throws IOException {
		pfs.createOrUpdateDocument(new ByteArrayInputStream(data), name);
	}

	@org.apache.poi.util.Internal
	public byte[] getDataStream() {
		return _dataStream;
	}

	@org.apache.poi.util.Internal
	public byte[] getTableStream() {
		return _tableStream;
	}

	public int registerList(HWPFList list) {
		if ((_lt) == null) {
			_lt = new ListTables();
		}
		return _lt.addList(list.getListData(), list.getLFO(), list.getLFOData());
	}

	public void delete(int start, int length) {
		Range r = new Range(start, (start + length), this);
		r.delete();
	}
}

