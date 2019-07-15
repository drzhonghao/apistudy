

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.EscherBSERecord;
import org.apache.poi.ddf.EscherBoolProperty;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherDgRecord;
import org.apache.poi.ddf.EscherDggRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherRGBProperty;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.ddf.EscherSplitMenuColorsRecord;
import org.apache.poi.hssf.model.DrawingManager2;
import org.apache.poi.hssf.model.InternalSheet;
import org.apache.poi.hssf.model.WorkbookRecordList;
import org.apache.poi.hssf.record.AbstractEscherHolderRecord;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BackupRecord;
import org.apache.poi.hssf.record.BookBoolRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.CodepageRecord;
import org.apache.poi.hssf.record.CountryRecord;
import org.apache.poi.hssf.record.DSFRecord;
import org.apache.poi.hssf.record.DateWindow1904Record;
import org.apache.poi.hssf.record.DrawingGroupRecord;
import org.apache.poi.hssf.record.EOFRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.ExtSSTRecord;
import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.FileSharingRecord;
import org.apache.poi.hssf.record.FnGroupCountRecord;
import org.apache.poi.hssf.record.FontRecord;
import org.apache.poi.hssf.record.FormatRecord;
import org.apache.poi.hssf.record.HideObjRecord;
import org.apache.poi.hssf.record.HyperlinkRecord;
import org.apache.poi.hssf.record.InterfaceEndRecord;
import org.apache.poi.hssf.record.InterfaceHdrRecord;
import org.apache.poi.hssf.record.MMSRecord;
import org.apache.poi.hssf.record.NameCommentRecord;
import org.apache.poi.hssf.record.NameRecord;
import org.apache.poi.hssf.record.PaletteRecord;
import org.apache.poi.hssf.record.PasswordRecord;
import org.apache.poi.hssf.record.PasswordRev4Record;
import org.apache.poi.hssf.record.PrecisionRecord;
import org.apache.poi.hssf.record.ProtectRecord;
import org.apache.poi.hssf.record.ProtectionRev4Record;
import org.apache.poi.hssf.record.RecalcIdRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordBase;
import org.apache.poi.hssf.record.RefreshAllRecord;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.StyleRecord;
import org.apache.poi.hssf.record.TabIdRecord;
import org.apache.poi.hssf.record.UseSelFSRecord;
import org.apache.poi.hssf.record.WindowOneRecord;
import org.apache.poi.hssf.record.WindowProtectRecord;
import org.apache.poi.hssf.record.WriteAccessRecord;
import org.apache.poi.hssf.record.WriteProtectRecord;
import org.apache.poi.hssf.record.common.UnicodeString;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.formula.ptg.Area3DPtg;
import org.apache.poi.ss.formula.ptg.NameXPtg;
import org.apache.poi.ss.formula.ptg.OperandPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.Ref3DPtg;
import org.apache.poi.ss.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.RecordFormatException;

import static org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined.BLACK;


@Internal
public final class InternalWorkbook {
	private static final int MAX_SENSITIVE_SHEET_NAME_LEN = 31;

	public static final String[] WORKBOOK_DIR_ENTRY_NAMES = new String[]{ "Workbook", "WORKBOOK", "BOOK", "WorkBook" };

	public static final String OLD_WORKBOOK_DIR_ENTRY_NAME = "Book";

	private static final POILogger LOG = POILogFactory.getLogger(InternalWorkbook.class);

	private static final short CODEPAGE = 1200;

	private final WorkbookRecordList records;

	protected SSTRecord sst;

	private final List<BoundSheetRecord> boundsheets;

	private final List<FormatRecord> formats;

	private final List<HyperlinkRecord> hyperlinks;

	private int numxfs;

	private int numfonts;

	private int maxformatid;

	private boolean uses1904datewindowing;

	private DrawingManager2 drawingManager;

	private List<EscherBSERecord> escherBSERecords;

	private WindowOneRecord windowOne;

	private FileSharingRecord fileShare;

	private WriteAccessRecord writeAccess;

	private WriteProtectRecord writeProtect;

	private final Map<String, NameCommentRecord> commentRecords;

	private InternalWorkbook() {
		records = new WorkbookRecordList();
		boundsheets = new ArrayList<>();
		formats = new ArrayList<>();
		hyperlinks = new ArrayList<>();
		numxfs = 0;
		numfonts = 0;
		maxformatid = -1;
		uses1904datewindowing = false;
		escherBSERecords = new ArrayList<>();
		commentRecords = new LinkedHashMap<>();
	}

	public static InternalWorkbook createWorkbook(List<Record> recs) {
		InternalWorkbook.LOG.log(POILogger.DEBUG, "Workbook (readfile) created with reclen=", recs.size());
		InternalWorkbook retval = new InternalWorkbook();
		List<Record> records = new ArrayList<>(((recs.size()) / 3));
		retval.records.setRecords(records);
		boolean eofPassed = false;
		for (int k = 0; k < (recs.size()); k++) {
			Record rec = recs.get(k);
			String logObj;
			if (!eofPassed) {
				records.add(rec);
			}
			logObj = null;
			InternalWorkbook.LOG.log(POILogger.DEBUG, ((("found " + logObj) + " record at ") + k));
			if ((rec.getSid()) == (EOFRecord.sid)) {
				eofPassed = true;
			}
		}
		if ((retval.windowOne) == null) {
			retval.windowOne = InternalWorkbook.createWindowOne();
		}
		InternalWorkbook.LOG.log(POILogger.DEBUG, "exit create workbook from existing file function");
		return retval;
	}

	public static InternalWorkbook createWorkbook() {
		InternalWorkbook.LOG.log(POILogger.DEBUG, "creating new workbook from scratch");
		InternalWorkbook retval = new InternalWorkbook();
		List<Record> records = new ArrayList<>(30);
		retval.records.setRecords(records);
		List<FormatRecord> formats = retval.formats;
		records.add(InternalWorkbook.createBOF());
		records.add(new InterfaceHdrRecord(InternalWorkbook.CODEPAGE));
		records.add(InternalWorkbook.createMMS());
		records.add(InterfaceEndRecord.instance);
		records.add(InternalWorkbook.createWriteAccess());
		records.add(InternalWorkbook.createCodepage());
		records.add(InternalWorkbook.createDSF());
		records.add(InternalWorkbook.createTabId());
		retval.records.setTabpos(((records.size()) - 1));
		records.add(InternalWorkbook.createFnGroupCount());
		records.add(InternalWorkbook.createWindowProtect());
		records.add(InternalWorkbook.createProtect());
		retval.records.setProtpos(((records.size()) - 1));
		records.add(InternalWorkbook.createPassword());
		records.add(InternalWorkbook.createProtectionRev4());
		records.add(InternalWorkbook.createPasswordRev4());
		retval.windowOne = InternalWorkbook.createWindowOne();
		records.add(retval.windowOne);
		records.add(InternalWorkbook.createBackup());
		retval.records.setBackuppos(((records.size()) - 1));
		records.add(InternalWorkbook.createHideObj());
		records.add(InternalWorkbook.createDateWindow1904());
		records.add(InternalWorkbook.createPrecision());
		records.add(InternalWorkbook.createRefreshAll());
		records.add(InternalWorkbook.createBookBool());
		records.add(InternalWorkbook.createFont());
		records.add(InternalWorkbook.createFont());
		records.add(InternalWorkbook.createFont());
		records.add(InternalWorkbook.createFont());
		retval.records.setFontpos(((records.size()) - 1));
		retval.numfonts = 4;
		for (int i = 0; i <= 7; i++) {
			FormatRecord rec = InternalWorkbook.createFormat(i);
			retval.maxformatid = ((retval.maxformatid) >= (rec.getIndexCode())) ? retval.maxformatid : rec.getIndexCode();
			formats.add(rec);
			records.add(rec);
		}
		for (int k = 0; k < 21; k++) {
			records.add(InternalWorkbook.createExtendedFormat(k));
			(retval.numxfs)++;
		}
		retval.records.setXfpos(((records.size()) - 1));
		for (int k = 0; k < 6; k++) {
			records.add(InternalWorkbook.createStyle(k));
		}
		records.add(InternalWorkbook.createUseSelFS());
		int nBoundSheets = 1;
		for (int k = 0; k < nBoundSheets; k++) {
			BoundSheetRecord bsr = InternalWorkbook.createBoundSheet(k);
			records.add(bsr);
			retval.boundsheets.add(bsr);
			retval.records.setBspos(((records.size()) - 1));
		}
		records.add(InternalWorkbook.createCountry());
		for (int k = 0; k < nBoundSheets; k++) {
		}
		retval.sst = new SSTRecord();
		records.add(retval.sst);
		records.add(InternalWorkbook.createExtendedSST());
		records.add(EOFRecord.instance);
		InternalWorkbook.LOG.log(POILogger.DEBUG, "exit create new workbook from scratch");
		return retval;
	}

	public NameRecord getSpecificBuiltinRecord(byte name, int sheetNumber) {
		return null;
	}

	public void removeBuiltinRecord(byte name, int sheetIndex) {
	}

	public int getNumRecords() {
		return records.size();
	}

	public FontRecord getFontRecordAt(int idx) {
		int index = idx;
		if (index > 4) {
			index -= 1;
		}
		if (index > ((numfonts) - 1)) {
			throw new ArrayIndexOutOfBoundsException(((("There are only " + (numfonts)) + " font records, you asked for ") + idx));
		}
		return ((FontRecord) (records.get((((records.getFontpos()) - ((numfonts) - 1)) + index))));
	}

	public int getFontIndex(FontRecord font) {
		for (int i = 0; i <= (numfonts); i++) {
			FontRecord thisFont = ((FontRecord) (records.get((((records.getFontpos()) - ((numfonts) - 1)) + i))));
			if (thisFont == font) {
				return i > 3 ? i + 1 : i;
			}
		}
		throw new IllegalArgumentException("Could not find that font!");
	}

	public FontRecord createNewFont() {
		FontRecord rec = InternalWorkbook.createFont();
		records.add(((records.getFontpos()) + 1), rec);
		records.setFontpos(((records.getFontpos()) + 1));
		(numfonts)++;
		return rec;
	}

	public void removeFontRecord(FontRecord rec) {
		records.remove(rec);
		(numfonts)--;
	}

	public int getNumberOfFontRecords() {
		return numfonts;
	}

	public void setSheetBof(int sheetIndex, int pos) {
		InternalWorkbook.LOG.log(POILogger.DEBUG, "setting bof for sheetnum =", sheetIndex, " at pos=", pos);
		checkSheets(sheetIndex);
		getBoundSheetRec(sheetIndex).setPositionOfBof(pos);
	}

	private BoundSheetRecord getBoundSheetRec(int sheetIndex) {
		return boundsheets.get(sheetIndex);
	}

	public BackupRecord getBackupRecord() {
		return ((BackupRecord) (records.get(records.getBackuppos())));
	}

	public void setSheetName(int sheetnum, final String sheetname) {
		checkSheets(sheetnum);
		String sn = ((sheetname.length()) > 31) ? sheetname.substring(0, 31) : sheetname;
		BoundSheetRecord sheet = boundsheets.get(sheetnum);
		sheet.setSheetname(sn);
	}

	public boolean doesContainsSheetName(String name, int excludeSheetIdx) {
		String aName = name;
		if ((aName.length()) > (InternalWorkbook.MAX_SENSITIVE_SHEET_NAME_LEN)) {
			aName = aName.substring(0, InternalWorkbook.MAX_SENSITIVE_SHEET_NAME_LEN);
		}
		int i = 0;
		for (BoundSheetRecord boundSheetRecord : boundsheets) {
			if (excludeSheetIdx == (i++)) {
				continue;
			}
			String bName = boundSheetRecord.getSheetname();
			if ((bName.length()) > (InternalWorkbook.MAX_SENSITIVE_SHEET_NAME_LEN)) {
				bName = bName.substring(0, InternalWorkbook.MAX_SENSITIVE_SHEET_NAME_LEN);
			}
			if (aName.equalsIgnoreCase(bName)) {
				return true;
			}
		}
		return false;
	}

	public void setSheetOrder(String sheetname, int pos) {
		int sheetNumber = getSheetIndex(sheetname);
		boundsheets.add(pos, boundsheets.remove(sheetNumber));
		int initialBspos = records.getBspos();
		int pos0 = initialBspos - ((boundsheets.size()) - 1);
		Record removed = records.get((pos0 + sheetNumber));
		records.remove((pos0 + sheetNumber));
		records.add((pos0 + pos), removed);
		records.setBspos(initialBspos);
	}

	public String getSheetName(int sheetIndex) {
		return getBoundSheetRec(sheetIndex).getSheetname();
	}

	public boolean isSheetHidden(int sheetnum) {
		return getBoundSheetRec(sheetnum).isHidden();
	}

	public boolean isSheetVeryHidden(int sheetnum) {
		return getBoundSheetRec(sheetnum).isVeryHidden();
	}

	public SheetVisibility getSheetVisibility(int sheetnum) {
		final BoundSheetRecord bsr = getBoundSheetRec(sheetnum);
		if (bsr.isVeryHidden()) {
			return SheetVisibility.VERY_HIDDEN;
		}
		if (bsr.isHidden()) {
			return SheetVisibility.HIDDEN;
		}
		return SheetVisibility.VISIBLE;
	}

	public void setSheetHidden(int sheetnum, boolean hidden) {
		setSheetHidden(sheetnum, (hidden ? SheetVisibility.HIDDEN : SheetVisibility.VISIBLE));
	}

	public void setSheetHidden(int sheetnum, SheetVisibility visibility) {
		BoundSheetRecord bsr = getBoundSheetRec(sheetnum);
		bsr.setHidden((visibility == (SheetVisibility.HIDDEN)));
		bsr.setVeryHidden((visibility == (SheetVisibility.VERY_HIDDEN)));
	}

	public int getSheetIndex(String name) {
		int retval = -1;
		final int size = boundsheets.size();
		for (int k = 0; k < size; k++) {
			String sheet = getSheetName(k);
			if (sheet.equalsIgnoreCase(name)) {
				retval = k;
				break;
			}
		}
		return retval;
	}

	private void checkSheets(int sheetnum) {
		if ((boundsheets.size()) <= sheetnum) {
			if (((boundsheets.size()) + 1) <= sheetnum) {
				throw new RuntimeException("Sheet number out of bounds!");
			}
			BoundSheetRecord bsr = InternalWorkbook.createBoundSheet(sheetnum);
			records.add(((records.getBspos()) + 1), bsr);
			records.setBspos(((records.getBspos()) + 1));
			boundsheets.add(bsr);
			fixTabIdRecord();
		}
	}

	public void removeSheet(int sheetIndex) {
		if ((boundsheets.size()) > sheetIndex) {
			records.remove((((records.getBspos()) - ((boundsheets.size()) - 1)) + sheetIndex));
			boundsheets.remove(sheetIndex);
			fixTabIdRecord();
		}
		int sheetNum1Based = sheetIndex + 1;
		for (int i = 0; i < (getNumNames()); i++) {
			NameRecord nr = getNameRecord(i);
			if ((nr.getSheetNumber()) == sheetNum1Based) {
				nr.setSheetNumber(0);
			}else
				if ((nr.getSheetNumber()) > sheetNum1Based) {
					nr.setSheetNumber(((nr.getSheetNumber()) - 1));
				}

		}
	}

	private void fixTabIdRecord() {
		Record rec = records.get(records.getTabpos());
		if ((records.getTabpos()) <= 0) {
			return;
		}
		TabIdRecord tir = ((TabIdRecord) (rec));
		short[] tia = new short[boundsheets.size()];
		for (short k = 0; k < (tia.length); k++) {
			tia[k] = k;
		}
		tir.setTabIdArray(tia);
	}

	public int getNumSheets() {
		InternalWorkbook.LOG.log(POILogger.DEBUG, "getNumSheets=", boundsheets.size());
		return boundsheets.size();
	}

	public int getNumExFormats() {
		InternalWorkbook.LOG.log(POILogger.DEBUG, "getXF=", numxfs);
		return numxfs;
	}

	public ExtendedFormatRecord getExFormatAt(int index) {
		int xfptr = (records.getXfpos()) - ((numxfs) - 1);
		xfptr += index;
		return ((ExtendedFormatRecord) (records.get(xfptr)));
	}

	public void removeExFormatRecord(ExtendedFormatRecord rec) {
		records.remove(rec);
		(numxfs)--;
	}

	public void removeExFormatRecord(int index) {
		int xfptr = ((records.getXfpos()) - ((numxfs) - 1)) + index;
		records.remove(xfptr);
		(numxfs)--;
	}

	public ExtendedFormatRecord createCellXF() {
		ExtendedFormatRecord xf = InternalWorkbook.createExtendedFormat();
		records.add(((records.getXfpos()) + 1), xf);
		records.setXfpos(((records.getXfpos()) + 1));
		(numxfs)++;
		return xf;
	}

	public StyleRecord getStyleRecord(int xfIndex) {
		for (int i = records.getXfpos(); i < (records.size()); i++) {
			Record r = records.get(i);
			if (r instanceof StyleRecord) {
				StyleRecord sr = ((StyleRecord) (r));
				if ((sr.getXFIndex()) == xfIndex) {
					return sr;
				}
			}
		}
		return null;
	}

	public void updateStyleRecord(int oldXf, int newXf) {
		for (int i = records.getXfpos(); i < (records.size()); i++) {
			Record r = records.get(i);
			if (r instanceof StyleRecord) {
				StyleRecord sr = ((StyleRecord) (r));
				if ((sr.getXFIndex()) == oldXf) {
					sr.setXFIndex(newXf);
				}
			}
		}
	}

	public StyleRecord createStyleRecord(int xfIndex) {
		StyleRecord newSR = new StyleRecord();
		newSR.setXFIndex(xfIndex);
		int addAt = -1;
		for (int i = records.getXfpos(); (i < (records.size())) && (addAt == (-1)); i++) {
			Record r = records.get(i);
			if ((r instanceof ExtendedFormatRecord) || (r instanceof StyleRecord)) {
			}else {
				addAt = i;
			}
		}
		if (addAt == (-1)) {
			throw new IllegalStateException("No XF Records found!");
		}
		records.add(addAt, newSR);
		return newSR;
	}

	public int addSSTString(UnicodeString string) {
		InternalWorkbook.LOG.log(POILogger.DEBUG, "insert to sst string='", string);
		if ((sst) == null) {
			insertSST();
		}
		return sst.addString(string);
	}

	public UnicodeString getSSTString(int str) {
		if ((sst) == null) {
			insertSST();
		}
		UnicodeString retval = sst.getString(str);
		InternalWorkbook.LOG.log(POILogger.DEBUG, "Returning SST for index=", str, " String= ", retval);
		return retval;
	}

	public void insertSST() {
		InternalWorkbook.LOG.log(POILogger.DEBUG, "creating new SST via insertSST!");
		sst = new SSTRecord();
		records.add(((records.size()) - 1), InternalWorkbook.createExtendedSST());
		records.add(((records.size()) - 2), sst);
	}

	public int serialize(int offset, byte[] data) {
		InternalWorkbook.LOG.log(POILogger.DEBUG, "Serializing Workbook with offsets");
		int pos = 0;
		SSTRecord lSST = null;
		int sstPos = 0;
		boolean wroteBoundSheets = false;
		for (Record record : records.getRecords()) {
			int len = 0;
			if (record instanceof SSTRecord) {
				lSST = ((SSTRecord) (record));
				sstPos = pos;
			}
			if (((record.getSid()) == (ExtSSTRecord.sid)) && (lSST != null)) {
				record = lSST.createExtSSTRecord((sstPos + offset));
			}
			if (record instanceof BoundSheetRecord) {
				if (!wroteBoundSheets) {
					for (BoundSheetRecord bsr : boundsheets) {
						len += bsr.serialize(((pos + offset) + len), data);
					}
					wroteBoundSheets = true;
				}
			}else {
				len = record.serialize((pos + offset), data);
			}
			pos += len;
		}
		InternalWorkbook.LOG.log(POILogger.DEBUG, "Exiting serialize workbook");
		return pos;
	}

	public void preSerialize() {
		if ((records.getTabpos()) > 0) {
			TabIdRecord tir = ((TabIdRecord) (records.get(records.getTabpos())));
			if ((tir._tabids.length) < (boundsheets.size())) {
				fixTabIdRecord();
			}
		}
	}

	public int getSize() {
		int retval = 0;
		SSTRecord lSST = null;
		for (Record record : records.getRecords()) {
			if (record instanceof SSTRecord) {
				lSST = ((SSTRecord) (record));
			}
			if (((record.getSid()) == (ExtSSTRecord.sid)) && (lSST != null)) {
				retval += lSST.calcExtSSTRecordSize();
			}else {
				retval += record.getRecordSize();
			}
		}
		return retval;
	}

	private static BOFRecord createBOF() {
		BOFRecord retval = new BOFRecord();
		retval.setVersion(((short) (1536)));
		retval.setType(BOFRecord.TYPE_WORKBOOK);
		retval.setBuild(((short) (4307)));
		retval.setBuildYear(((short) (1996)));
		retval.setHistoryBitMask(65);
		retval.setRequiredVersion(6);
		return retval;
	}

	private static MMSRecord createMMS() {
		MMSRecord retval = new MMSRecord();
		retval.setAddMenuCount(((byte) (0)));
		retval.setDelMenuCount(((byte) (0)));
		return retval;
	}

	private static WriteAccessRecord createWriteAccess() {
		WriteAccessRecord retval = new WriteAccessRecord();
		String defaultUserName = "POI";
		try {
			String username = System.getProperty("user.name");
			if (username == null) {
				username = defaultUserName;
			}
			retval.setUsername(username);
		} catch (AccessControlException e) {
			InternalWorkbook.LOG.log(POILogger.WARN, "can't determine user.name", e);
			retval.setUsername(defaultUserName);
		}
		return retval;
	}

	private static CodepageRecord createCodepage() {
		CodepageRecord retval = new CodepageRecord();
		retval.setCodepage(InternalWorkbook.CODEPAGE);
		return retval;
	}

	private static DSFRecord createDSF() {
		return new DSFRecord(false);
	}

	private static TabIdRecord createTabId() {
		return new TabIdRecord();
	}

	private static FnGroupCountRecord createFnGroupCount() {
		FnGroupCountRecord retval = new FnGroupCountRecord();
		retval.setCount(((short) (14)));
		return retval;
	}

	private static WindowProtectRecord createWindowProtect() {
		return new WindowProtectRecord(false);
	}

	private static ProtectRecord createProtect() {
		return new ProtectRecord(false);
	}

	private static PasswordRecord createPassword() {
		return new PasswordRecord(0);
	}

	private static ProtectionRev4Record createProtectionRev4() {
		return new ProtectionRev4Record(false);
	}

	private static PasswordRev4Record createPasswordRev4() {
		return new PasswordRev4Record(0);
	}

	private static WindowOneRecord createWindowOne() {
		WindowOneRecord retval = new WindowOneRecord();
		retval.setHorizontalHold(((short) (360)));
		retval.setVerticalHold(((short) (270)));
		retval.setWidth(((short) (14940)));
		retval.setHeight(((short) (9150)));
		retval.setOptions(((short) (56)));
		retval.setActiveSheetIndex(0);
		retval.setFirstVisibleTab(0);
		retval.setNumSelectedTabs(((short) (1)));
		retval.setTabWidthRatio(((short) (600)));
		return retval;
	}

	private static BackupRecord createBackup() {
		BackupRecord retval = new BackupRecord();
		retval.setBackup(((short) (0)));
		return retval;
	}

	private static HideObjRecord createHideObj() {
		HideObjRecord retval = new HideObjRecord();
		retval.setHideObj(((short) (0)));
		return retval;
	}

	private static DateWindow1904Record createDateWindow1904() {
		DateWindow1904Record retval = new DateWindow1904Record();
		retval.setWindowing(((short) (0)));
		return retval;
	}

	private static PrecisionRecord createPrecision() {
		PrecisionRecord retval = new PrecisionRecord();
		retval.setFullPrecision(true);
		return retval;
	}

	private static RefreshAllRecord createRefreshAll() {
		return new RefreshAllRecord(false);
	}

	private static BookBoolRecord createBookBool() {
		BookBoolRecord retval = new BookBoolRecord();
		retval.setSaveLinkValues(((short) (0)));
		return retval;
	}

	private static FontRecord createFont() {
		FontRecord retval = new FontRecord();
		retval.setFontHeight(((short) (200)));
		retval.setAttributes(((short) (0)));
		retval.setColorPaletteIndex(((short) (32767)));
		retval.setBoldWeight(((short) (400)));
		retval.setFontName("Arial");
		return retval;
	}

	private static FormatRecord createFormat(int id) {
		final int[] mappings = new int[]{ 5, 6, 7, 8, 42, 41, 44, 43 };
		if ((id < 0) || (id >= (mappings.length))) {
			throw new IllegalArgumentException(("Unexpected id " + id));
		}
		return new FormatRecord(mappings[id], BuiltinFormats.getBuiltinFormat(mappings[id]));
	}

	private static ExtendedFormatRecord createExtendedFormat(int id) {
		switch (id) {
			case 0 :
				return InternalWorkbook.createExtendedFormat(0, 0, (-11), 0);
			case 1 :
			case 2 :
				return InternalWorkbook.createExtendedFormat(1, 0, (-11), (-3072));
			case 3 :
			case 4 :
				return InternalWorkbook.createExtendedFormat(2, 0, (-11), (-3072));
			case 5 :
			case 6 :
			case 7 :
			case 8 :
			case 9 :
			case 10 :
			case 11 :
			case 12 :
			case 13 :
			case 14 :
				return InternalWorkbook.createExtendedFormat(0, 0, (-11), (-3072));
			case 15 :
				return InternalWorkbook.createExtendedFormat(0, 0, 1, 0);
			case 16 :
				return InternalWorkbook.createExtendedFormat(1, 43, (-11), (-2048));
			case 17 :
				return InternalWorkbook.createExtendedFormat(1, 41, (-11), (-2048));
			case 18 :
				return InternalWorkbook.createExtendedFormat(1, 44, (-11), (-2048));
			case 19 :
				return InternalWorkbook.createExtendedFormat(1, 42, (-11), (-2048));
			case 20 :
				return InternalWorkbook.createExtendedFormat(1, 9, (-11), (-2048));
			case 21 :
				return InternalWorkbook.createExtendedFormat(5, 0, 1, 2048);
			case 22 :
				return InternalWorkbook.createExtendedFormat(6, 0, 1, 23552);
			case 23 :
				return InternalWorkbook.createExtendedFormat(0, 49, 1, 23552);
			case 24 :
				return InternalWorkbook.createExtendedFormat(0, 8, 1, 23552);
			case 25 :
				return InternalWorkbook.createExtendedFormat(6, 8, 1, 23552);
			default :
				throw new IllegalStateException(("Unrecognized format id: " + id));
		}
	}

	private static ExtendedFormatRecord createExtendedFormat(int fontIndex, int formatIndex, int cellOptions, int indentionOptions) {
		ExtendedFormatRecord retval = new ExtendedFormatRecord();
		retval.setFontIndex(((short) (fontIndex)));
		retval.setFormatIndex(((short) (formatIndex)));
		retval.setCellOptions(((short) (cellOptions)));
		retval.setAlignmentOptions(((short) (32)));
		retval.setIndentionOptions(((short) (indentionOptions)));
		retval.setBorderOptions(((short) (0)));
		retval.setPaletteOptions(((short) (0)));
		retval.setAdtlPaletteOptions(((short) (0)));
		retval.setFillPaletteOptions(((short) (8384)));
		return retval;
	}

	private static ExtendedFormatRecord createExtendedFormat() {
		ExtendedFormatRecord retval = new ExtendedFormatRecord();
		retval.setFontIndex(((short) (0)));
		retval.setFormatIndex(((short) (0)));
		retval.setCellOptions(((short) (1)));
		retval.setAlignmentOptions(((short) (32)));
		retval.setIndentionOptions(((short) (0)));
		retval.setBorderOptions(((short) (0)));
		retval.setPaletteOptions(((short) (0)));
		retval.setAdtlPaletteOptions(((short) (0)));
		retval.setFillPaletteOptions(((short) (8384)));
		retval.setTopBorderPaletteIdx(BLACK.getIndex());
		retval.setBottomBorderPaletteIdx(BLACK.getIndex());
		retval.setLeftBorderPaletteIdx(BLACK.getIndex());
		retval.setRightBorderPaletteIdx(BLACK.getIndex());
		return retval;
	}

	private static StyleRecord createStyle(int id) {
		final int[][] mappings = new int[][]{ new int[]{ 16, 3 }, new int[]{ 17, 6 }, new int[]{ 18, 4 }, new int[]{ 19, 7 }, new int[]{ 0, 0 }, new int[]{ 20, 5 } };
		if ((id < 0) || (id >= (mappings.length))) {
			throw new IllegalArgumentException(("Unexpected style id " + id));
		}
		StyleRecord retval = new StyleRecord();
		retval.setOutlineStyleLevel(((byte) (-1)));
		retval.setXFIndex(mappings[id][0]);
		retval.setBuiltinStyle(mappings[id][1]);
		return retval;
	}

	private static PaletteRecord createPalette() {
		return new PaletteRecord();
	}

	private static UseSelFSRecord createUseSelFS() {
		return new UseSelFSRecord(false);
	}

	private static BoundSheetRecord createBoundSheet(int id) {
		return new BoundSheetRecord(("Sheet" + (id + 1)));
	}

	private static CountryRecord createCountry() {
		CountryRecord retval = new CountryRecord();
		retval.setDefaultCountry(((short) (1)));
		if ("ru_RU".equals(LocaleUtil.getUserLocale().toString())) {
			retval.setCurrentCountry(((short) (7)));
		}else {
			retval.setCurrentCountry(((short) (1)));
		}
		return retval;
	}

	private static ExtSSTRecord createExtendedSST() {
		ExtSSTRecord retval = new ExtSSTRecord();
		retval.setNumStringsPerBucket(((short) (8)));
		return retval;
	}

	public int linkExternalWorkbook(String name, Workbook externalWorkbook) {
		return 0;
	}

	public String findSheetFirstNameFromExternSheet(int externSheetIndex) {
		return null;
	}

	public String findSheetLastNameFromExternSheet(int externSheetIndex) {
		return null;
	}

	private String findSheetNameFromIndex(int internalSheetIndex) {
		if (internalSheetIndex < 0) {
			return "";
		}
		if (internalSheetIndex >= (boundsheets.size())) {
			return "";
		}
		return getSheetName(internalSheetIndex);
	}

	public EvaluationWorkbook.ExternalSheet getExternalSheet(int externSheetIndex) {
		return null;
	}

	public EvaluationWorkbook.ExternalName getExternalName(int externSheetIndex, int externNameIndex) {
		return null;
	}

	public int getFirstSheetIndexFromExternSheetIndex(int externSheetNumber) {
		return 0;
	}

	public int getLastSheetIndexFromExternSheetIndex(int externSheetNumber) {
		return 0;
	}

	public short checkExternSheet(int sheetNumber) {
		return 0;
	}

	public short checkExternSheet(int firstSheetNumber, int lastSheetNumber) {
		return 0;
	}

	public int getExternalSheetIndex(String workbookName, String sheetName) {
		return 0;
	}

	public int getExternalSheetIndex(String workbookName, String firstSheetName, String lastSheetName) {
		return 0;
	}

	public int getNumNames() {
		return 0;
	}

	public NameRecord getNameRecord(int index) {
		return null;
	}

	public NameCommentRecord getNameCommentRecord(final NameRecord nameRecord) {
		return commentRecords.get(nameRecord.getNameText());
	}

	public NameRecord createName() {
		return addName(new NameRecord());
	}

	public NameRecord addName(NameRecord name) {
		return name;
	}

	public NameRecord createBuiltInName(byte builtInName, int sheetNumber) {
		if ((sheetNumber < 0) || ((sheetNumber + 1) > (Short.MAX_VALUE))) {
			throw new IllegalArgumentException((("Sheet number [" + sheetNumber) + "]is not valid "));
		}
		NameRecord name = new NameRecord(builtInName, sheetNumber);
		addName(name);
		return name;
	}

	public void removeName(int nameIndex) {
	}

	public void updateNameCommentRecordCache(final NameCommentRecord commentRecord) {
		if (commentRecords.containsValue(commentRecord)) {
			for (Map.Entry<String, NameCommentRecord> entry : commentRecords.entrySet()) {
				if (entry.getValue().equals(commentRecord)) {
					commentRecords.remove(entry.getKey());
					break;
				}
			}
		}
		commentRecords.put(commentRecord.getNameText(), commentRecord);
	}

	public short getFormat(String format, boolean createIfNotFound) {
		for (FormatRecord r : formats) {
			if (r.getFormatString().equals(format)) {
				return ((short) (r.getIndexCode()));
			}
		}
		if (createIfNotFound) {
			return ((short) (createFormat(format)));
		}
		return -1;
	}

	public List<FormatRecord> getFormats() {
		return formats;
	}

	public int createFormat(String formatString) {
		maxformatid = ((maxformatid) >= 164) ? (maxformatid) + 1 : 164;
		FormatRecord rec = new FormatRecord(maxformatid, formatString);
		int pos = 0;
		while ((pos < (records.size())) && ((records.get(pos).getSid()) != (FormatRecord.sid))) {
			pos++;
		} 
		pos += formats.size();
		formats.add(rec);
		records.add(pos, rec);
		return maxformatid;
	}

	public Record findFirstRecordBySid(short sid) {
		for (Record record : records.getRecords()) {
			if ((record.getSid()) == sid) {
				return record;
			}
		}
		return null;
	}

	public int findFirstRecordLocBySid(short sid) {
		int index = 0;
		for (Record record : records.getRecords()) {
			if ((record.getSid()) == sid) {
				return index;
			}
			index++;
		}
		return -1;
	}

	public Record findNextRecordBySid(short sid, int pos) {
		int matches = 0;
		for (Record record : records.getRecords()) {
			if (((record.getSid()) == sid) && ((matches++) == pos)) {
				return record;
			}
		}
		return null;
	}

	public List<HyperlinkRecord> getHyperlinks() {
		return hyperlinks;
	}

	public List<Record> getRecords() {
		return records.getRecords();
	}

	public boolean isUsing1904DateWindowing() {
		return uses1904datewindowing;
	}

	public PaletteRecord getCustomPalette() {
		PaletteRecord palette;
		int palettePos = records.getPalettepos();
		if (palettePos != (-1)) {
			Record rec = records.get(palettePos);
			if (rec instanceof PaletteRecord) {
				palette = ((PaletteRecord) (rec));
			}else {
				throw new RuntimeException((("InternalError: Expected PaletteRecord but got a '" + rec) + "'"));
			}
		}else {
			palette = InternalWorkbook.createPalette();
			records.add(1, palette);
			records.setPalettepos(1);
		}
		return palette;
	}

	public DrawingManager2 findDrawingGroup() {
		if ((drawingManager) != null) {
			return drawingManager;
		}
		for (Record r : records.getRecords()) {
			if (!(r instanceof DrawingGroupRecord)) {
				continue;
			}
			DrawingGroupRecord dg = ((DrawingGroupRecord) (r));
			dg.processChildRecords();
			drawingManager = InternalWorkbook.findDrawingManager(dg, escherBSERecords);
			if ((drawingManager) != null) {
				return drawingManager;
			}
		}
		DrawingGroupRecord dg = ((DrawingGroupRecord) (findFirstRecordBySid(DrawingGroupRecord.sid)));
		drawingManager = InternalWorkbook.findDrawingManager(dg, escherBSERecords);
		return drawingManager;
	}

	private static DrawingManager2 findDrawingManager(DrawingGroupRecord dg, List<EscherBSERecord> escherBSERecords) {
		if (dg == null) {
			return null;
		}
		EscherContainerRecord cr = dg.getEscherContainer();
		if (cr == null) {
			return null;
		}
		EscherDggRecord dgg = null;
		EscherContainerRecord bStore = null;
		for (EscherRecord er : cr) {
			if (er instanceof EscherDggRecord) {
				dgg = ((EscherDggRecord) (er));
			}else
				if ((er.getRecordId()) == (EscherContainerRecord.BSTORE_CONTAINER)) {
					bStore = ((EscherContainerRecord) (er));
				}

		}
		if (dgg == null) {
			return null;
		}
		DrawingManager2 dm = new DrawingManager2(dgg);
		if (bStore != null) {
			for (EscherRecord bs : bStore.getChildRecords()) {
				if (bs instanceof EscherBSERecord) {
					escherBSERecords.add(((EscherBSERecord) (bs)));
				}
			}
		}
		return dm;
	}

	public void createDrawingGroup() {
		if ((drawingManager) == null) {
			EscherContainerRecord dggContainer = new EscherContainerRecord();
			EscherDggRecord dgg = new EscherDggRecord();
			EscherOptRecord opt = new EscherOptRecord();
			EscherSplitMenuColorsRecord splitMenuColors = new EscherSplitMenuColorsRecord();
			dggContainer.setRecordId(((short) (61440)));
			dggContainer.setOptions(((short) (15)));
			dgg.setRecordId(EscherDggRecord.RECORD_ID);
			dgg.setOptions(((short) (0)));
			dgg.setShapeIdMax(1024);
			dgg.setNumShapesSaved(0);
			dgg.setDrawingsSaved(0);
			dgg.setFileIdClusters(new EscherDggRecord.FileIdCluster[]{  });
			drawingManager = new DrawingManager2(dgg);
			EscherContainerRecord bstoreContainer = null;
			if (!(escherBSERecords.isEmpty())) {
				bstoreContainer = new EscherContainerRecord();
				bstoreContainer.setRecordId(EscherContainerRecord.BSTORE_CONTAINER);
				bstoreContainer.setOptions(((short) (((escherBSERecords.size()) << 4) | 15)));
				for (EscherRecord escherRecord : escherBSERecords) {
					bstoreContainer.addChildRecord(escherRecord);
				}
			}
			opt.setRecordId(((short) (61451)));
			opt.setOptions(((short) (51)));
			opt.addEscherProperty(new EscherBoolProperty(EscherProperties.TEXT__SIZE_TEXT_TO_FIT_SHAPE, 524296));
			opt.addEscherProperty(new EscherRGBProperty(EscherProperties.FILL__FILLCOLOR, 134217793));
			opt.addEscherProperty(new EscherRGBProperty(EscherProperties.LINESTYLE__COLOR, 134217792));
			splitMenuColors.setRecordId(((short) (61726)));
			splitMenuColors.setOptions(((short) (64)));
			splitMenuColors.setColor1(134217741);
			splitMenuColors.setColor2(134217740);
			splitMenuColors.setColor3(134217751);
			splitMenuColors.setColor4(268435703);
			dggContainer.addChildRecord(dgg);
			if (bstoreContainer != null) {
				dggContainer.addChildRecord(bstoreContainer);
			}
			dggContainer.addChildRecord(opt);
			dggContainer.addChildRecord(splitMenuColors);
			int dgLoc = findFirstRecordLocBySid(DrawingGroupRecord.sid);
			if (dgLoc == (-1)) {
				DrawingGroupRecord drawingGroup = new DrawingGroupRecord();
				drawingGroup.addEscherRecord(dggContainer);
				int loc = findFirstRecordLocBySid(CountryRecord.sid);
				getRecords().add((loc + 1), drawingGroup);
			}else {
				DrawingGroupRecord drawingGroup = new DrawingGroupRecord();
				drawingGroup.addEscherRecord(dggContainer);
				getRecords().set(dgLoc, drawingGroup);
			}
		}
	}

	public WindowOneRecord getWindowOne() {
		return windowOne;
	}

	public EscherBSERecord getBSERecord(int pictureIndex) {
		return escherBSERecords.get((pictureIndex - 1));
	}

	public int addBSERecord(EscherBSERecord e) {
		createDrawingGroup();
		escherBSERecords.add(e);
		int dgLoc = findFirstRecordLocBySid(DrawingGroupRecord.sid);
		DrawingGroupRecord drawingGroup = ((DrawingGroupRecord) (getRecords().get(dgLoc)));
		EscherContainerRecord dggContainer = ((EscherContainerRecord) (drawingGroup.getEscherRecord(0)));
		EscherContainerRecord bstoreContainer;
		if ((dggContainer.getChild(1).getRecordId()) == (EscherContainerRecord.BSTORE_CONTAINER)) {
			bstoreContainer = ((EscherContainerRecord) (dggContainer.getChild(1)));
		}else {
			bstoreContainer = new EscherContainerRecord();
			bstoreContainer.setRecordId(EscherContainerRecord.BSTORE_CONTAINER);
			List<EscherRecord> childRecords = dggContainer.getChildRecords();
			childRecords.add(1, bstoreContainer);
			dggContainer.setChildRecords(childRecords);
		}
		bstoreContainer.setOptions(((short) (((escherBSERecords.size()) << 4) | 15)));
		bstoreContainer.addChildRecord(e);
		return escherBSERecords.size();
	}

	public DrawingManager2 getDrawingManager() {
		return drawingManager;
	}

	public WriteProtectRecord getWriteProtect() {
		if ((writeProtect) == null) {
			writeProtect = new WriteProtectRecord();
			int i = findFirstRecordLocBySid(BOFRecord.sid);
			records.add((i + 1), writeProtect);
		}
		return this.writeProtect;
	}

	public WriteAccessRecord getWriteAccess() {
		if ((writeAccess) == null) {
			writeAccess = InternalWorkbook.createWriteAccess();
			int i = findFirstRecordLocBySid(InterfaceEndRecord.sid);
			records.add((i + 1), writeAccess);
		}
		return writeAccess;
	}

	public FileSharingRecord getFileSharing() {
		if ((fileShare) == null) {
			fileShare = new FileSharingRecord();
			int i = findFirstRecordLocBySid(WriteAccessRecord.sid);
			records.add((i + 1), fileShare);
		}
		return fileShare;
	}

	public boolean isWriteProtected() {
		if ((fileShare) == null) {
			return false;
		}
		FileSharingRecord frec = getFileSharing();
		return (frec.getReadOnly()) == 1;
	}

	public void writeProtectWorkbook(String password, String username) {
		FileSharingRecord frec = getFileSharing();
		WriteAccessRecord waccess = getWriteAccess();
		getWriteProtect();
		frec.setReadOnly(((short) (1)));
		frec.setPassword(((short) (CryptoFunctions.createXorVerifier1(password))));
		frec.setUsername(username);
		waccess.setUsername(username);
	}

	public void unwriteProtectWorkbook() {
		records.remove(fileShare);
		records.remove(writeProtect);
		fileShare = null;
		writeProtect = null;
	}

	public String resolveNameXText(int refIndex, int definedNameIndex) {
		return null;
	}

	public NameXPtg getNameXPtg(String name, int sheetRefIndex, UDFFinder udf) {
		return null;
	}

	public NameXPtg getNameXPtg(String name, UDFFinder udf) {
		return getNameXPtg(name, (-1), udf);
	}

	public void cloneDrawings(InternalSheet sheet) {
		findDrawingGroup();
		if ((drawingManager) == null) {
			return;
		}
		int aggLoc = sheet.aggregateDrawingRecords(drawingManager, false);
		if (aggLoc == (-1)) {
			return;
		}
		EscherAggregate agg = ((EscherAggregate) (sheet.findFirstRecordBySid(EscherAggregate.sid)));
		EscherContainerRecord escherContainer = agg.getEscherContainer();
		if (escherContainer == null) {
			return;
		}
		EscherDggRecord dgg = drawingManager.getDgg();
		int dgId = drawingManager.findNewDrawingGroupId();
		dgg.addCluster(dgId, 0);
		dgg.setDrawingsSaved(((dgg.getDrawingsSaved()) + 1));
		EscherDgRecord dg = null;
		for (EscherRecord er : escherContainer) {
			if (er instanceof EscherDgRecord) {
				dg = ((EscherDgRecord) (er));
				dg.setOptions(((short) (dgId << 4)));
			}else
				if (er instanceof EscherContainerRecord) {
					for (EscherRecord er2 : ((EscherContainerRecord) (er))) {
						for (EscherRecord shapeChildRecord : ((EscherContainerRecord) (er2))) {
							int recordId = shapeChildRecord.getRecordId();
							if (recordId == (EscherSpRecord.RECORD_ID)) {
								if (dg == null) {
									throw new RecordFormatException("EscherDgRecord wasn't set/processed before.");
								}
								EscherSpRecord sp = ((EscherSpRecord) (shapeChildRecord));
								int shapeId = drawingManager.allocateShapeId(dg);
								dg.setNumShapes(((dg.getNumShapes()) - 1));
								sp.setShapeId(shapeId);
							}else
								if (recordId == (EscherOptRecord.RECORD_ID)) {
									EscherOptRecord opt = ((EscherOptRecord) (shapeChildRecord));
									EscherSimpleProperty prop = opt.lookup(EscherProperties.BLIP__BLIPTODISPLAY);
									if (prop != null) {
										int pictureIndex = prop.getPropertyValue();
										EscherBSERecord bse = getBSERecord(pictureIndex);
										bse.setRef(((bse.getRef()) + 1));
									}
								}

						}
					}
				}

		}
	}

	public NameRecord cloneFilter(int filterDbNameIndex, int newSheetIndex) {
		NameRecord origNameRecord = getNameRecord(filterDbNameIndex);
		int newExtSheetIx = checkExternSheet(newSheetIndex);
		Ptg[] ptgs = origNameRecord.getNameDefinition();
		for (int i = 0; i < (ptgs.length); i++) {
			Ptg ptg = ptgs[i];
			if (ptg instanceof Area3DPtg) {
				Area3DPtg a3p = ((Area3DPtg) (((OperandPtg) (ptg)).copy()));
				a3p.setExternSheetIndex(newExtSheetIx);
				ptgs[i] = a3p;
			}else
				if (ptg instanceof Ref3DPtg) {
					Ref3DPtg r3p = ((Ref3DPtg) (((OperandPtg) (ptg)).copy()));
					r3p.setExternSheetIndex(newExtSheetIx);
					ptgs[i] = r3p;
				}

		}
		NameRecord newNameRecord = createBuiltInName(NameRecord.BUILTIN_FILTER_DB, (newSheetIndex + 1));
		newNameRecord.setNameDefinition(ptgs);
		newNameRecord.setHidden(true);
		return newNameRecord;
	}

	public void updateNamesAfterCellShift(FormulaShifter shifter) {
		for (int i = 0; i < (getNumNames()); ++i) {
			NameRecord nr = getNameRecord(i);
			Ptg[] ptgs = nr.getNameDefinition();
			if (shifter.adjustFormula(ptgs, nr.getSheetNumber())) {
				nr.setNameDefinition(ptgs);
			}
		}
	}

	public RecalcIdRecord getRecalcId() {
		RecalcIdRecord record = ((RecalcIdRecord) (findFirstRecordBySid(RecalcIdRecord.sid)));
		if (record == null) {
			record = new RecalcIdRecord();
			int pos = findFirstRecordLocBySid(CountryRecord.sid);
			records.add((pos + 1), record);
		}
		return record;
	}

	public boolean changeExternalReference(String oldUrl, String newUrl) {
		return false;
	}

	@Internal
	public WorkbookRecordList getWorkbookRecordList() {
		return records;
	}
}

