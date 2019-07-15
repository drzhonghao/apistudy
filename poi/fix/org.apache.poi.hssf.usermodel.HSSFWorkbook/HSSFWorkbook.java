

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.POIDocument;
import org.apache.poi.ddf.EscherBSERecord;
import org.apache.poi.ddf.EscherBitmapBlip;
import org.apache.poi.ddf.EscherBlipRecord;
import org.apache.poi.ddf.EscherMetafileBlip;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.hpsf.ClassID;
import org.apache.poi.hpsf.ClassIDPredefined;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.OldExcelFormatException;
import org.apache.poi.hssf.model.DrawingManager2;
import org.apache.poi.hssf.model.InternalSheet;
import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.model.RecordStream;
import org.apache.poi.hssf.model.WorkbookRecordList;
import org.apache.poi.hssf.record.AbstractEscherHolderRecord;
import org.apache.poi.hssf.record.BackupRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.CellRecord;
import org.apache.poi.hssf.record.DrawingGroupRecord;
import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.FilePassRecord;
import org.apache.poi.hssf.record.FontRecord;
import org.apache.poi.hssf.record.LabelRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NameRecord;
import org.apache.poi.hssf.record.RecalcIdRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordBase;
import org.apache.poi.hssf.record.RecordFactory;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.UnknownRecord;
import org.apache.poi.hssf.record.WindowOneRecord;
import org.apache.poi.hssf.record.aggregates.RecordAggregate;
import org.apache.poi.hssf.record.common.UnicodeString;
import org.apache.poi.hssf.record.crypto.Biff8DecryptingStream;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFCreationHelper;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFName;
import org.apache.poi.hssf.usermodel.HSSFObjectData;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFPictureData;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFShapeContainer;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.poifs.crypt.ChunkedCipherOutputStream;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.EncryptionVerifier;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.DocumentNode;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.EntryUtils;
import org.apache.poi.poifs.filesystem.FilteringDirectoryNode;
import org.apache.poi.poifs.filesystem.Ole10Native;
import org.apache.poi.poifs.filesystem.POIFSDocument;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.formula.SheetNameFormatter;
import org.apache.poi.ss.formula.udf.AggregatingUDFFinder;
import org.apache.poi.ss.formula.udf.IndexedUDFFinder;
import org.apache.poi.ss.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Configurator;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianByteArrayInputStream;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_NULL_AND_BLANK;


@SuppressWarnings("WeakerAccess")
public final class HSSFWorkbook extends POIDocument implements Workbook {
	private static final int MAX_RECORD_LENGTH = 100000;

	private static final Pattern COMMA_PATTERN = Pattern.compile(",");

	private static final int MAX_STYLES = 4030;

	private static final int DEBUG = POILogger.DEBUG;

	public static final int INITIAL_CAPACITY = Configurator.getIntValue("HSSFWorkbook.SheetInitialCapacity", 3);

	private InternalWorkbook workbook;

	protected List<HSSFSheet> _sheets;

	private ArrayList<HSSFName> names;

	private Map<Integer, HSSFFont> fonts;

	private boolean preserveNodes;

	private HSSFDataFormat formatter;

	private Row.MissingCellPolicy missingCellPolicy = RETURN_NULL_AND_BLANK;

	private static final POILogger log = POILogFactory.getLogger(HSSFWorkbook.class);

	private UDFFinder _udfFinder = new IndexedUDFFinder(AggregatingUDFFinder.DEFAULT);

	public static HSSFWorkbook create(InternalWorkbook book) {
		return new HSSFWorkbook(book);
	}

	public HSSFWorkbook() {
		this(InternalWorkbook.createWorkbook());
	}

	private HSSFWorkbook(InternalWorkbook book) {
		super(((DirectoryNode) (null)));
		workbook = book;
		_sheets = new ArrayList<>(HSSFWorkbook.INITIAL_CAPACITY);
		names = new ArrayList<>(HSSFWorkbook.INITIAL_CAPACITY);
	}

	public HSSFWorkbook(POIFSFileSystem fs) throws IOException {
		this(fs, true);
	}

	public HSSFWorkbook(POIFSFileSystem fs, boolean preserveNodes) throws IOException {
		this(fs.getRoot(), fs, preserveNodes);
	}

	public static String getWorkbookDirEntryName(DirectoryNode directory) {
		for (String wbName : InternalWorkbook.WORKBOOK_DIR_ENTRY_NAMES) {
			if (directory.hasEntry(wbName)) {
				return wbName;
			}
		}
		try {
			directory.getEntry(Decryptor.DEFAULT_POIFS_ENTRY);
			throw new EncryptedDocumentException(("The supplied spreadsheet seems to be an Encrypted .xlsx file. " + "It must be decrypted before use by XSSF, it cannot be used by HSSF"));
		} catch (FileNotFoundException e) {
		}
		try {
			directory.getEntry(InternalWorkbook.OLD_WORKBOOK_DIR_ENTRY_NAME);
			throw new OldExcelFormatException(("The supplied spreadsheet seems to be Excel 5.0/7.0 (BIFF5) format. " + "POI only supports BIFF8 format (from Excel versions 97/2000/XP/2003)"));
		} catch (FileNotFoundException e) {
		}
		throw new IllegalArgumentException((("The supplied POIFSFileSystem does not contain a BIFF8 'Workbook' entry. " + "Is it really an excel file? Had: ") + (directory.getEntryNames())));
	}

	public HSSFWorkbook(DirectoryNode directory, POIFSFileSystem fs, boolean preserveNodes) throws IOException {
		this(directory, preserveNodes);
	}

	public HSSFWorkbook(DirectoryNode directory, boolean preserveNodes) throws IOException {
		super(directory);
		String workbookName = HSSFWorkbook.getWorkbookDirEntryName(directory);
		this.preserveNodes = preserveNodes;
		if (!preserveNodes) {
			clearDirectory();
		}
		_sheets = new ArrayList<>(HSSFWorkbook.INITIAL_CAPACITY);
		names = new ArrayList<>(HSSFWorkbook.INITIAL_CAPACITY);
		InputStream stream = directory.createDocumentInputStream(workbookName);
		List<Record> records = RecordFactory.createRecords(stream);
		workbook = InternalWorkbook.createWorkbook(records);
		setPropertiesFromWorkbook(workbook);
		int recOffset = workbook.getNumRecords();
		convertLabelRecords(records, recOffset);
		RecordStream rs = new RecordStream(records, recOffset);
		while (rs.hasNext()) {
			try {
				InternalSheet sheet = InternalSheet.createSheet(rs);
			} catch (InternalSheet.UnsupportedBOFType eb) {
				HSSFWorkbook.log.log(POILogger.WARN, ("Unsupported BOF found of type " + (eb.getType())));
			}
		} 
		for (int i = 0; i < (workbook.getNumNames()); ++i) {
			NameRecord nameRecord = workbook.getNameRecord(i);
		}
	}

	public HSSFWorkbook(InputStream s) throws IOException {
		this(s, true);
	}

	@SuppressWarnings("resource")
	public HSSFWorkbook(InputStream s, boolean preserveNodes) throws IOException {
		this(new POIFSFileSystem(s).getRoot(), preserveNodes);
	}

	private void setPropertiesFromWorkbook(InternalWorkbook book) {
		this.workbook = book;
	}

	private void convertLabelRecords(List<Record> records, int offset) {
		if (HSSFWorkbook.log.check(POILogger.DEBUG)) {
			HSSFWorkbook.log.log(POILogger.DEBUG, "convertLabelRecords called");
		}
		for (int k = offset; k < (records.size()); k++) {
			Record rec = records.get(k);
			if ((rec.getSid()) == (LabelRecord.sid)) {
				LabelRecord oldrec = ((LabelRecord) (rec));
				records.remove(k);
				LabelSSTRecord newrec = new LabelSSTRecord();
				int stringid = workbook.addSSTString(new UnicodeString(oldrec.getValue()));
				newrec.setRow(oldrec.getRow());
				newrec.setColumn(oldrec.getColumn());
				newrec.setXFIndex(oldrec.getXFIndex());
				newrec.setSSTIndex(stringid);
				records.add(k, newrec);
			}
		}
		if (HSSFWorkbook.log.check(POILogger.DEBUG)) {
			HSSFWorkbook.log.log(POILogger.DEBUG, "convertLabelRecords exit");
		}
	}

	@Override
	public Row.MissingCellPolicy getMissingCellPolicy() {
		return missingCellPolicy;
	}

	@Override
	public void setMissingCellPolicy(Row.MissingCellPolicy missingCellPolicy) {
		this.missingCellPolicy = missingCellPolicy;
	}

	@Override
	public void setSheetOrder(String sheetname, int pos) {
		int oldSheetIndex = getSheetIndex(sheetname);
		_sheets.add(pos, _sheets.remove(oldSheetIndex));
		workbook.setSheetOrder(sheetname, pos);
		FormulaShifter shifter = FormulaShifter.createForSheetShift(oldSheetIndex, pos);
		for (HSSFSheet sheet : _sheets) {
		}
		workbook.updateNamesAfterCellShift(shifter);
		updateNamedRangesAfterSheetReorder(oldSheetIndex, pos);
		updateActiveSheetAfterSheetReorder(oldSheetIndex, pos);
	}

	private void updateNamedRangesAfterSheetReorder(int oldIndex, int newIndex) {
		for (final HSSFName name : names) {
			final int i = name.getSheetIndex();
			if (i != (-1)) {
				if (i == oldIndex) {
					name.setSheetIndex(newIndex);
				}else
					if ((newIndex <= i) && (i < oldIndex)) {
						name.setSheetIndex((i + 1));
					}else
						if ((oldIndex < i) && (i <= newIndex)) {
							name.setSheetIndex((i - 1));
						}


			}
		}
	}

	private void updateActiveSheetAfterSheetReorder(int oldIndex, int newIndex) {
		int active = getActiveSheetIndex();
		if (active == oldIndex) {
			setActiveSheet(newIndex);
		}else
			if (((active < oldIndex) && (active < newIndex)) || ((active > oldIndex) && (active > newIndex))) {
			}else
				if (newIndex > oldIndex) {
					setActiveSheet((active - 1));
				}else {
					setActiveSheet((active + 1));
				}


	}

	private void validateSheetIndex(int index) {
		int lastSheetIx = (_sheets.size()) - 1;
		if ((index < 0) || (index > lastSheetIx)) {
			String range = ("(0.." + lastSheetIx) + ")";
			if (lastSheetIx == (-1)) {
				range = "(no sheets)";
			}
			throw new IllegalArgumentException(((("Sheet index (" + index) + ") is out of range ") + range));
		}
	}

	@Override
	public void setSelectedTab(int index) {
		validateSheetIndex(index);
		int nSheets = _sheets.size();
		for (int i = 0; i < nSheets; i++) {
			getSheetAt(i).setSelected((i == index));
		}
		workbook.getWindowOne().setNumSelectedTabs(((short) (1)));
	}

	public void setSelectedTabs(int[] indexes) {
		Collection<Integer> list = new ArrayList<>(indexes.length);
		for (int index : indexes) {
			list.add(index);
		}
		setSelectedTabs(list);
	}

	public void setSelectedTabs(Collection<Integer> indexes) {
		for (int index : indexes) {
			validateSheetIndex(index);
		}
		Set<Integer> set = new HashSet<>(indexes);
		int nSheets = _sheets.size();
		for (int i = 0; i < nSheets; i++) {
			boolean bSelect = set.contains(i);
			getSheetAt(i).setSelected(bSelect);
		}
		short nSelected = ((short) (set.size()));
		workbook.getWindowOne().setNumSelectedTabs(nSelected);
	}

	public Collection<Integer> getSelectedTabs() {
		Collection<Integer> indexes = new ArrayList<>();
		int nSheets = _sheets.size();
		for (int i = 0; i < nSheets; i++) {
			HSSFSheet sheet = getSheetAt(i);
			if (sheet.isSelected()) {
				indexes.add(i);
			}
		}
		return Collections.unmodifiableCollection(indexes);
	}

	@Override
	public void setActiveSheet(int index) {
		validateSheetIndex(index);
		int nSheets = _sheets.size();
		for (int i = 0; i < nSheets; i++) {
			getSheetAt(i).setActive((i == index));
		}
		workbook.getWindowOne().setActiveSheetIndex(index);
	}

	@Override
	public int getActiveSheetIndex() {
		return workbook.getWindowOne().getActiveSheetIndex();
	}

	@Override
	public void setFirstVisibleTab(int index) {
		workbook.getWindowOne().setFirstVisibleTab(index);
	}

	@Override
	public int getFirstVisibleTab() {
		return workbook.getWindowOne().getFirstVisibleTab();
	}

	@Override
	public void setSheetName(int sheetIx, String name) {
		if (name == null) {
			throw new IllegalArgumentException("sheetName must not be null");
		}
		if (workbook.doesContainsSheetName(name, sheetIx)) {
			throw new IllegalArgumentException((("The workbook already contains a sheet named '" + name) + "'"));
		}
		validateSheetIndex(sheetIx);
		workbook.setSheetName(sheetIx, name);
	}

	@Override
	public String getSheetName(int sheetIndex) {
		validateSheetIndex(sheetIndex);
		return workbook.getSheetName(sheetIndex);
	}

	@Override
	public boolean isHidden() {
		return workbook.getWindowOne().getHidden();
	}

	@Override
	public void setHidden(boolean hiddenFlag) {
		workbook.getWindowOne().setHidden(hiddenFlag);
	}

	@Override
	public boolean isSheetHidden(int sheetIx) {
		validateSheetIndex(sheetIx);
		return workbook.isSheetHidden(sheetIx);
	}

	@Override
	public boolean isSheetVeryHidden(int sheetIx) {
		validateSheetIndex(sheetIx);
		return workbook.isSheetVeryHidden(sheetIx);
	}

	@Override
	public SheetVisibility getSheetVisibility(int sheetIx) {
		return workbook.getSheetVisibility(sheetIx);
	}

	@Override
	public void setSheetHidden(int sheetIx, boolean hidden) {
		setSheetVisibility(sheetIx, (hidden ? SheetVisibility.HIDDEN : SheetVisibility.VISIBLE));
	}

	@Override
	public void setSheetVisibility(int sheetIx, SheetVisibility visibility) {
		validateSheetIndex(sheetIx);
		workbook.setSheetHidden(sheetIx, visibility);
	}

	@Override
	public int getSheetIndex(String name) {
		return workbook.getSheetIndex(name);
	}

	@Override
	public int getSheetIndex(Sheet sheet) {
		return _sheets.indexOf(sheet);
	}

	@Override
	public HSSFSheet createSheet() {
		workbook.setSheetName(((_sheets.size()) - 1), ("Sheet" + ((_sheets.size()) - 1)));
		boolean isOnlySheet = (_sheets.size()) == 1;
		return null;
	}

	@Override
	public HSSFSheet cloneSheet(int sheetIndex) {
		validateSheetIndex(sheetIndex);
		HSSFSheet srcSheet = _sheets.get(sheetIndex);
		String srcName = workbook.getSheetName(sheetIndex);
		String name = getUniqueSheetName(srcName);
		int newSheetIndex = _sheets.size();
		workbook.setSheetName(newSheetIndex, name);
		int filterDbNameIndex = findExistingBuiltinNameRecordIdx(sheetIndex, NameRecord.BUILTIN_FILTER_DB);
		if (filterDbNameIndex != (-1)) {
			NameRecord newNameRecord = workbook.cloneFilter(filterDbNameIndex, newSheetIndex);
		}
		return null;
	}

	private String getUniqueSheetName(String srcName) {
		int uniqueIndex = 2;
		String baseName = srcName;
		int bracketPos = srcName.lastIndexOf('(');
		if ((bracketPos > 0) && (srcName.endsWith(")"))) {
			String suffix = srcName.substring((bracketPos + 1), ((srcName.length()) - (")".length())));
			try {
				uniqueIndex = Integer.parseInt(suffix.trim());
				uniqueIndex++;
				baseName = srcName.substring(0, bracketPos).trim();
			} catch (NumberFormatException e) {
			}
		}
		while (true) {
			String index = Integer.toString((uniqueIndex++));
			String name;
			if ((((baseName.length()) + (index.length())) + 2) < 31) {
				name = ((baseName + " (") + index) + ")";
			}else {
				name = (((baseName.substring(0, ((31 - (index.length())) - 2))) + "(") + index) + ")";
			}
			if ((workbook.getSheetIndex(name)) == (-1)) {
				return name;
			}
		} 
	}

	@Override
	public HSSFSheet createSheet(String sheetname) {
		if (sheetname == null) {
			throw new IllegalArgumentException("sheetName must not be null");
		}
		if (workbook.doesContainsSheetName(sheetname, _sheets.size())) {
			throw new IllegalArgumentException((("The workbook already contains a sheet named '" + sheetname) + "'"));
		}
		workbook.setSheetName(_sheets.size(), sheetname);
		boolean isOnlySheet = (_sheets.size()) == 1;
		return null;
	}

	@Override
	public Iterator<Sheet> sheetIterator() {
		return new HSSFWorkbook.SheetIterator<>();
	}

	@Override
	public Iterator<Sheet> iterator() {
		return sheetIterator();
	}

	private final class SheetIterator<T extends Sheet> implements Iterator<T> {
		private final Iterator<T> it;

		private T cursor;

		@SuppressWarnings("unchecked")
		public SheetIterator() {
			it = ((Iterator<T>) (_sheets.iterator()));
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public T next() throws NoSuchElementException {
			cursor = it.next();
			return cursor;
		}

		@Override
		public void remove() throws IllegalStateException {
			throw new UnsupportedOperationException(("remove method not supported on HSSFWorkbook.iterator(). " + "Use Sheet.removeSheetAt(int) instead."));
		}
	}

	@Override
	public int getNumberOfSheets() {
		return _sheets.size();
	}

	private HSSFSheet[] getSheets() {
		HSSFSheet[] result = new HSSFSheet[_sheets.size()];
		_sheets.toArray(result);
		return result;
	}

	@Override
	public HSSFSheet getSheetAt(int index) {
		validateSheetIndex(index);
		return _sheets.get(index);
	}

	@Override
	public HSSFSheet getSheet(String name) {
		HSSFSheet retval = null;
		for (int k = 0; k < (_sheets.size()); k++) {
			String sheetname = workbook.getSheetName(k);
			if (sheetname.equalsIgnoreCase(name)) {
				retval = _sheets.get(k);
			}
		}
		return retval;
	}

	@Override
	public void removeSheetAt(int index) {
		validateSheetIndex(index);
		boolean wasSelected = getSheetAt(index).isSelected();
		_sheets.remove(index);
		workbook.removeSheet(index);
		int nSheets = _sheets.size();
		if (nSheets < 1) {
			return;
		}
		int newSheetIndex = index;
		if (newSheetIndex >= nSheets) {
			newSheetIndex = nSheets - 1;
		}
		if (wasSelected) {
			boolean someOtherSheetIsStillSelected = false;
			for (int i = 0; i < nSheets; i++) {
				if (getSheetAt(i).isSelected()) {
					someOtherSheetIsStillSelected = true;
					break;
				}
			}
			if (!someOtherSheetIsStillSelected) {
				setSelectedTab(newSheetIndex);
			}
		}
		int active = getActiveSheetIndex();
		if (active == index) {
			setActiveSheet(newSheetIndex);
		}else
			if (active > index) {
				setActiveSheet((active - 1));
			}

	}

	public void setBackupFlag(boolean backupValue) {
		BackupRecord backupRecord = workbook.getBackupRecord();
		backupRecord.setBackup((backupValue ? ((short) (1)) : ((short) (0))));
	}

	public boolean getBackupFlag() {
		BackupRecord backupRecord = workbook.getBackupRecord();
		return (backupRecord.getBackup()) != 0;
	}

	int findExistingBuiltinNameRecordIdx(int sheetIndex, byte builtinCode) {
		for (int defNameIndex = 0; defNameIndex < (names.size()); defNameIndex++) {
			NameRecord r = workbook.getNameRecord(defNameIndex);
			if (r == null) {
				throw new RuntimeException("Unable to find all defined names to iterate over");
			}
			if ((!(r.isBuiltInName())) || ((r.getBuiltInName()) != builtinCode)) {
				continue;
			}
			if (((r.getSheetNumber()) - 1) == sheetIndex) {
				return defNameIndex;
			}
		}
		return -1;
	}

	HSSFName createBuiltInName(byte builtinCode, int sheetIndex) {
		NameRecord nameRecord = workbook.createBuiltInName(builtinCode, (sheetIndex + 1));
		return null;
	}

	HSSFName getBuiltInName(byte builtinCode, int sheetIndex) {
		int index = findExistingBuiltinNameRecordIdx(sheetIndex, builtinCode);
		return index < 0 ? null : names.get(index);
	}

	@Override
	public HSSFFont createFont() {
		workbook.createNewFont();
		int fontindex = (getNumberOfFontsAsInt()) - 1;
		if (fontindex > 3) {
			fontindex++;
		}
		if (fontindex >= (Short.MAX_VALUE)) {
			throw new IllegalArgumentException("Maximum number of fonts was exceeded");
		}
		return getFontAt(fontindex);
	}

	@Override
	public HSSFFont findFont(boolean bold, short color, short fontHeight, String name, boolean italic, boolean strikeout, short typeOffset, byte underline) {
		int numberOfFonts = getNumberOfFontsAsInt();
		for (int i = 0; i <= numberOfFonts; i++) {
			if (i == 4) {
				continue;
			}
			HSSFFont hssfFont = getFontAt(i);
			if (((((((((hssfFont.getBold()) == bold) && ((hssfFont.getColor()) == color)) && ((hssfFont.getFontHeight()) == fontHeight)) && (hssfFont.getFontName().equals(name))) && ((hssfFont.getItalic()) == italic)) && ((hssfFont.getStrikeout()) == strikeout)) && ((hssfFont.getTypeOffset()) == typeOffset)) && ((hssfFont.getUnderline()) == underline)) {
				return hssfFont;
			}
		}
		return null;
	}

	@Override
	@Deprecated
	public short getNumberOfFonts() {
		return ((short) (getNumberOfFontsAsInt()));
	}

	@Override
	public int getNumberOfFontsAsInt() {
		return workbook.getNumberOfFontRecords();
	}

	@Override
	@Deprecated
	public HSSFFont getFontAt(short idx) {
		return getFontAt(((int) (idx)));
	}

	@Override
	public HSSFFont getFontAt(int idx) {
		if ((fonts) == null) {
			fonts = new HashMap<>();
		}
		Integer sIdx = idx;
		if (fonts.containsKey(sIdx)) {
			return fonts.get(sIdx);
		}
		FontRecord font = workbook.getFontRecordAt(idx);
		return null;
	}

	void resetFontCache() {
		fonts = new HashMap<>();
	}

	@Override
	public HSSFCellStyle createCellStyle() {
		if ((workbook.getNumExFormats()) == (HSSFWorkbook.MAX_STYLES)) {
			throw new IllegalStateException(("The maximum number of cell styles was exceeded. " + "You can define up to 4000 styles in a .xls workbook"));
		}
		ExtendedFormatRecord xfr = workbook.createCellXF();
		short index = ((short) ((getNumCellStyles()) - 1));
		return null;
	}

	@Override
	public int getNumCellStyles() {
		return workbook.getNumExFormats();
	}

	@Override
	public HSSFCellStyle getCellStyleAt(int idx) {
		ExtendedFormatRecord xfr = workbook.getExFormatAt(idx);
		return null;
	}

	@Override
	public void close() throws IOException {
		super.close();
	}

	@Override
	public void write() throws IOException {
		validateInPlaceWritePossible();
		final DirectoryNode dir = getDirectory();
		DocumentNode workbookNode = ((DocumentNode) (dir.getEntry(HSSFWorkbook.getWorkbookDirEntryName(dir))));
		POIFSDocument workbookDoc = new POIFSDocument(workbookNode);
		workbookDoc.replaceContents(new ByteArrayInputStream(getBytes()));
		writeProperties();
		dir.getFileSystem().writeFilesystem();
	}

	@Override
	public void write(File newFile) throws IOException {
		try (POIFSFileSystem fs = POIFSFileSystem.create(newFile)) {
			write(fs);
			fs.writeFilesystem();
		}
	}

	@Override
	public void write(OutputStream stream) throws IOException {
		try (POIFSFileSystem fs = new POIFSFileSystem()) {
			write(fs);
			fs.writeFilesystem(stream);
		}
	}

	private void write(POIFSFileSystem fs) throws IOException {
		List<String> excepts = new ArrayList<>(1);
		fs.createDocument(new ByteArrayInputStream(getBytes()), "Workbook");
		writeProperties(fs, excepts);
		if (preserveNodes) {
			excepts.addAll(Arrays.asList(InternalWorkbook.WORKBOOK_DIR_ENTRY_NAMES));
			excepts.addAll(Arrays.asList(DocumentSummaryInformation.DEFAULT_STREAM_NAME, SummaryInformation.DEFAULT_STREAM_NAME, getEncryptedPropertyStreamName()));
			EntryUtils.copyNodes(new FilteringDirectoryNode(getDirectory(), excepts), new FilteringDirectoryNode(fs.getRoot(), excepts));
			fs.getRoot().setStorageClsid(getDirectory().getStorageClsid());
		}
	}

	private static final class SheetRecordCollector implements RecordAggregate.RecordVisitor {
		private List<Record> _list;

		private int _totalSize;

		public SheetRecordCollector() {
			_totalSize = 0;
			_list = new ArrayList<>(128);
		}

		public int getTotalSize() {
			return _totalSize;
		}

		@Override
		public void visitRecord(Record r) {
			_list.add(r);
			_totalSize += r.getRecordSize();
		}

		public int serialize(int offset, byte[] data) {
			int result = 0;
			for (Record rec : _list) {
				result += rec.serialize((offset + result), data);
			}
			return result;
		}
	}

	public byte[] getBytes() {
		if (HSSFWorkbook.log.check(POILogger.DEBUG)) {
			HSSFWorkbook.log.log(HSSFWorkbook.DEBUG, "HSSFWorkbook.getBytes()");
		}
		HSSFSheet[] sheets = getSheets();
		int nSheets = sheets.length;
		updateEncryptionInfo();
		workbook.preSerialize();
		for (HSSFSheet sheet : sheets) {
		}
		int totalsize = workbook.getSize();
		HSSFWorkbook.SheetRecordCollector[] srCollectors = new HSSFWorkbook.SheetRecordCollector[nSheets];
		for (int k = 0; k < nSheets; k++) {
			workbook.setSheetBof(k, totalsize);
			HSSFWorkbook.SheetRecordCollector src = new HSSFWorkbook.SheetRecordCollector();
			totalsize += src.getTotalSize();
			srCollectors[k] = src;
		}
		byte[] retval = new byte[totalsize];
		int pos = workbook.serialize(0, retval);
		for (int k = 0; k < nSheets; k++) {
			HSSFWorkbook.SheetRecordCollector src = srCollectors[k];
			int serializedSize = src.serialize(pos, retval);
			if (serializedSize != (src.getTotalSize())) {
				throw new IllegalStateException((((((("Actual serialized sheet size (" + serializedSize) + ") differs from pre-calculated size (") + (src.getTotalSize())) + ") for sheet (") + k) + ")"));
			}
			pos += serializedSize;
		}
		encryptBytes(retval);
		return retval;
	}

	@SuppressWarnings("resource")
	void encryptBytes(byte[] buf) {
		EncryptionInfo ei = getEncryptionInfo();
		if (ei == null) {
			return;
		}
		Encryptor enc = ei.getEncryptor();
		int initialOffset = 0;
		LittleEndianByteArrayInputStream plain = new LittleEndianByteArrayInputStream(buf, 0);
		LittleEndianByteArrayOutputStream leos = new LittleEndianByteArrayOutputStream(buf, 0);
		enc.setChunkSize(Biff8DecryptingStream.RC4_REKEYING_INTERVAL);
		byte[] tmp = new byte[1024];
		try {
			ChunkedCipherOutputStream os = enc.getDataStream(leos, initialOffset);
			int totalBytes = 0;
			while (totalBytes < (buf.length)) {
				IOUtils.readFully(plain, tmp, 0, 4);
				final int sid = LittleEndian.getUShort(tmp, 0);
				final int len = LittleEndian.getUShort(tmp, 2);
				boolean isPlain = Biff8DecryptingStream.isNeverEncryptedRecord(sid);
				os.setNextRecordSize(len, isPlain);
				os.writePlain(tmp, 0, 4);
				if (sid == (BoundSheetRecord.sid)) {
					byte[] bsrBuf = IOUtils.safelyAllocate(len, HSSFWorkbook.MAX_RECORD_LENGTH);
					plain.readFully(bsrBuf);
					os.writePlain(bsrBuf, 0, 4);
					os.write(bsrBuf, 4, (len - 4));
				}else {
					int todo = len;
					while (todo > 0) {
						int nextLen = Math.min(todo, tmp.length);
						plain.readFully(tmp, 0, nextLen);
						if (isPlain) {
							os.writePlain(tmp, 0, nextLen);
						}else {
							os.write(tmp, 0, nextLen);
						}
						todo -= nextLen;
					} 
				}
				totalBytes += 4 + len;
			} 
			os.close();
		} catch (Exception e) {
			throw new EncryptedDocumentException(e);
		}
	}

	InternalWorkbook getWorkbook() {
		return workbook;
	}

	@Override
	public int getNumberOfNames() {
		return names.size();
	}

	@Override
	public HSSFName getName(String name) {
		int nameIndex = getNameIndex(name);
		if (nameIndex < 0) {
			return null;
		}
		return names.get(nameIndex);
	}

	@Override
	public List<HSSFName> getNames(String name) {
		List<HSSFName> nameList = new ArrayList<>();
		for (HSSFName nr : names) {
			if (nr.getNameName().equals(name)) {
				nameList.add(nr);
			}
		}
		return Collections.unmodifiableList(nameList);
	}

	@Override
	public HSSFName getNameAt(int nameIndex) {
		int nNames = names.size();
		if (nNames < 1) {
			throw new IllegalStateException("There are no defined names in this workbook");
		}
		if ((nameIndex < 0) || (nameIndex > nNames)) {
			throw new IllegalArgumentException((((("Specified name index " + nameIndex) + " is outside the allowable range (0..") + (nNames - 1)) + ")."));
		}
		return names.get(nameIndex);
	}

	@Override
	public List<HSSFName> getAllNames() {
		return Collections.unmodifiableList(names);
	}

	public NameRecord getNameRecord(int nameIndex) {
		return getWorkbook().getNameRecord(nameIndex);
	}

	public String getNameName(int index) {
		return getNameAt(index).getNameName();
	}

	@Override
	public void setPrintArea(int sheetIndex, String reference) {
		NameRecord name = workbook.getSpecificBuiltinRecord(NameRecord.BUILTIN_PRINT_AREA, (sheetIndex + 1));
		if (name == null) {
			name = workbook.createBuiltInName(NameRecord.BUILTIN_PRINT_AREA, (sheetIndex + 1));
		}
		String[] parts = HSSFWorkbook.COMMA_PATTERN.split(reference);
		StringBuilder sb = new StringBuilder(32);
		for (int i = 0; i < (parts.length); i++) {
			if (i > 0) {
				sb.append(",");
			}
			SheetNameFormatter.appendFormat(sb, getSheetName(sheetIndex));
			sb.append("!");
			sb.append(parts[i]);
		}
	}

	@Override
	public void setPrintArea(int sheetIndex, int startColumn, int endColumn, int startRow, int endRow) {
		CellReference cell = new CellReference(startRow, startColumn, true, true);
		String reference = cell.formatAsString();
		cell = new CellReference(endRow, endColumn, true, true);
		reference = (reference + ":") + (cell.formatAsString());
		setPrintArea(sheetIndex, reference);
	}

	@Override
	public String getPrintArea(int sheetIndex) {
		NameRecord name = workbook.getSpecificBuiltinRecord(NameRecord.BUILTIN_PRINT_AREA, (sheetIndex + 1));
		if (name == null) {
			return null;
		}
		return null;
	}

	@Override
	public void removePrintArea(int sheetIndex) {
		getWorkbook().removeBuiltinRecord(NameRecord.BUILTIN_PRINT_AREA, (sheetIndex + 1));
	}

	@Override
	public HSSFName createName() {
		NameRecord nameRecord = workbook.createName();
		return null;
	}

	@Override
	public int getNameIndex(String name) {
		for (int k = 0; k < (names.size()); k++) {
			String nameName = getNameName(k);
			if (nameName.equalsIgnoreCase(name)) {
				return k;
			}
		}
		return -1;
	}

	int getNameIndex(HSSFName name) {
		for (int k = 0; k < (names.size()); k++) {
			if (name == (names.get(k))) {
				return k;
			}
		}
		return -1;
	}

	@Override
	public void removeName(int index) {
		names.remove(index);
		workbook.removeName(index);
	}

	@Override
	public HSSFDataFormat createDataFormat() {
		if ((formatter) == null) {
		}
		return formatter;
	}

	@Override
	public void removeName(String name) {
		int index = getNameIndex(name);
		removeName(index);
	}

	@Override
	public void removeName(Name name) {
		int index = getNameIndex(((HSSFName) (name)));
		removeName(index);
	}

	public HSSFPalette getCustomPalette() {
		return null;
	}

	public void insertChartRecord() {
		int loc = workbook.findFirstRecordLocBySid(SSTRecord.sid);
		byte[] data = new byte[]{ ((byte) (15)), ((byte) (0)), ((byte) (0)), ((byte) (240)), ((byte) (82)), ((byte) (0)), ((byte) (0)), ((byte) (0)), ((byte) (0)), ((byte) (0)), ((byte) (6)), ((byte) (240)), ((byte) (24)), ((byte) (0)), ((byte) (0)), ((byte) (0)), ((byte) (1)), ((byte) (8)), ((byte) (0)), ((byte) (0)), ((byte) (2)), ((byte) (0)), ((byte) (0)), ((byte) (0)), ((byte) (2)), ((byte) (0)), ((byte) (0)), ((byte) (0)), ((byte) (1)), ((byte) (0)), ((byte) (0)), ((byte) (0)), ((byte) (1)), ((byte) (0)), ((byte) (0)), ((byte) (0)), ((byte) (3)), ((byte) (0)), ((byte) (0)), ((byte) (0)), ((byte) (51)), ((byte) (0)), ((byte) (11)), ((byte) (240)), ((byte) (18)), ((byte) (0)), ((byte) (0)), ((byte) (0)), ((byte) (191)), ((byte) (0)), ((byte) (8)), ((byte) (0)), ((byte) (8)), ((byte) (0)), ((byte) (129)), ((byte) (1)), ((byte) (9)), ((byte) (0)), ((byte) (0)), ((byte) (8)), ((byte) (192)), ((byte) (1)), ((byte) (64)), ((byte) (0)), ((byte) (0)), ((byte) (8)), ((byte) (64)), ((byte) (0)), ((byte) (30)), ((byte) (241)), ((byte) (16)), ((byte) (0)), ((byte) (0)), ((byte) (0)), ((byte) (13)), ((byte) (0)), ((byte) (0)), ((byte) (8)), ((byte) (12)), ((byte) (0)), ((byte) (0)), ((byte) (8)), ((byte) (23)), ((byte) (0)), ((byte) (0)), ((byte) (8)), ((byte) (247)), ((byte) (0)), ((byte) (0)), ((byte) (16)) };
		UnknownRecord r = new UnknownRecord(((short) (235)), data);
		workbook.getRecords().add(loc, r);
	}

	public void dumpDrawingGroupRecords(boolean fat) {
		DrawingGroupRecord r = ((DrawingGroupRecord) (workbook.findFirstRecordBySid(DrawingGroupRecord.sid)));
		if (r == null) {
			return;
		}
		r.decode();
		List<EscherRecord> escherRecords = r.getEscherRecords();
		PrintWriter w = new PrintWriter(new OutputStreamWriter(System.out, Charset.defaultCharset()));
		for (EscherRecord escherRecord : escherRecords) {
			if (fat) {
				System.out.println(escherRecord);
			}else {
				escherRecord.display(w, 0);
			}
		}
		w.flush();
	}

	void initDrawings() {
		DrawingManager2 mgr = workbook.findDrawingGroup();
		if (mgr != null) {
			for (HSSFSheet sh : _sheets) {
				sh.getDrawingPatriarch();
			}
		}else {
			workbook.createDrawingGroup();
		}
	}

	@SuppressWarnings("fallthrough")
	@Override
	public int addPicture(byte[] pictureData, int format) {
		initDrawings();
		byte[] uid = DigestUtils.md5(pictureData);
		EscherBlipRecord blipRecord;
		int blipSize;
		short escherTag;
		switch (format) {
			case Workbook.PICTURE_TYPE_WMF :
				if ((LittleEndian.getInt(pictureData)) == (-1698247209)) {
					byte[] picDataNoHeader = new byte[(pictureData.length) - 22];
					System.arraycopy(pictureData, 22, picDataNoHeader, 0, ((pictureData.length) - 22));
					pictureData = picDataNoHeader;
				}
			case Workbook.PICTURE_TYPE_EMF :
				EscherMetafileBlip blipRecordMeta = new EscherMetafileBlip();
				blipRecord = blipRecordMeta;
				blipRecordMeta.setUID(uid);
				blipRecordMeta.setPictureData(pictureData);
				blipRecordMeta.setFilter(((byte) (-2)));
				blipSize = (blipRecordMeta.getCompressedSize()) + 58;
				escherTag = 0;
				break;
			default :
				EscherBitmapBlip blipRecordBitmap = new EscherBitmapBlip();
				blipRecord = blipRecordBitmap;
				blipRecordBitmap.setUID(uid);
				blipRecordBitmap.setMarker(((byte) (255)));
				blipRecordBitmap.setPictureData(pictureData);
				blipSize = (pictureData.length) + 25;
				escherTag = ((short) (255));
				break;
		}
		blipRecord.setRecordId(((short) ((EscherBlipRecord.RECORD_ID_START) + format)));
		switch (format) {
			case Workbook.PICTURE_TYPE_EMF :
				blipRecord.setOptions(HSSFPictureData.MSOBI_EMF);
				break;
			case Workbook.PICTURE_TYPE_WMF :
				blipRecord.setOptions(HSSFPictureData.MSOBI_WMF);
				break;
			case Workbook.PICTURE_TYPE_PICT :
				blipRecord.setOptions(HSSFPictureData.MSOBI_PICT);
				break;
			case Workbook.PICTURE_TYPE_PNG :
				blipRecord.setOptions(HSSFPictureData.MSOBI_PNG);
				break;
			case Workbook.PICTURE_TYPE_JPEG :
				blipRecord.setOptions(HSSFPictureData.MSOBI_JPEG);
				break;
			case Workbook.PICTURE_TYPE_DIB :
				blipRecord.setOptions(HSSFPictureData.MSOBI_DIB);
				break;
			default :
				throw new IllegalStateException(("Unexpected picture format: " + format));
		}
		EscherBSERecord r = new EscherBSERecord();
		r.setRecordId(EscherBSERecord.RECORD_ID);
		r.setOptions(((short) (2 | (format << 4))));
		r.setBlipTypeMacOS(((byte) (format)));
		r.setBlipTypeWin32(((byte) (format)));
		r.setUid(uid);
		r.setTag(escherTag);
		r.setSize(blipSize);
		r.setRef(0);
		r.setOffset(0);
		r.setBlipRecord(blipRecord);
		return workbook.addBSERecord(r);
	}

	@Override
	public List<HSSFPictureData> getAllPictures() {
		List<HSSFPictureData> pictures = new ArrayList<>();
		for (Record r : workbook.getRecords()) {
			if (r instanceof AbstractEscherHolderRecord) {
				((AbstractEscherHolderRecord) (r)).decode();
				List<EscherRecord> escherRecords = ((AbstractEscherHolderRecord) (r)).getEscherRecords();
				searchForPictures(escherRecords, pictures);
			}
		}
		return Collections.unmodifiableList(pictures);
	}

	private void searchForPictures(List<EscherRecord> escherRecords, List<HSSFPictureData> pictures) {
		for (EscherRecord escherRecord : escherRecords) {
			if (escherRecord instanceof EscherBSERecord) {
				EscherBlipRecord blip = ((EscherBSERecord) (escherRecord)).getBlipRecord();
				if (blip != null) {
					HSSFPictureData picture = new HSSFPictureData(blip);
					pictures.add(picture);
				}
			}
			searchForPictures(escherRecord.getChildRecords(), pictures);
		}
	}

	static Map<String, ClassID> getOleMap() {
		Map<String, ClassID> olemap = new HashMap<>();
		olemap.put("PowerPoint Document", ClassIDPredefined.POWERPOINT_V8.getClassID());
		for (String str : InternalWorkbook.WORKBOOK_DIR_ENTRY_NAMES) {
			olemap.put(str, ClassIDPredefined.EXCEL_V7_WORKBOOK.getClassID());
		}
		return olemap;
	}

	public int addOlePackage(POIFSFileSystem poiData, String label, String fileName, String command) throws IOException {
		DirectoryNode root = poiData.getRoot();
		Map<String, ClassID> olemap = HSSFWorkbook.getOleMap();
		for (Map.Entry<String, ClassID> entry : olemap.entrySet()) {
			if (root.hasEntry(entry.getKey())) {
				root.setStorageClsid(entry.getValue());
				break;
			}
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		poiData.writeFilesystem(bos);
		return addOlePackage(bos.toByteArray(), label, fileName, command);
	}

	@Override
	public int addOlePackage(byte[] oleData, String label, String fileName, String command) throws IOException {
		if (initDirectory()) {
			preserveNodes = true;
		}
		int storageId = 0;
		DirectoryEntry oleDir = null;
		do {
			String storageStr = "MBD" + (HexDump.toHex((++storageId)));
			if (!(getDirectory().hasEntry(storageStr))) {
				oleDir = getDirectory().createDirectory(storageStr);
				oleDir.setStorageClsid(ClassIDPredefined.OLE_V1_PACKAGE.getClassID());
			}
		} while (oleDir == null );
		Ole10Native.createOleMarkerEntry(oleDir);
		Ole10Native oleNative = new Ole10Native(label, fileName, command, oleData);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		oleNative.writeOut(bos);
		oleDir.createDocument(Ole10Native.OLE10_NATIVE, new ByteArrayInputStream(bos.toByteArray()));
		return storageId;
	}

	@Override
	public int linkExternalWorkbook(String name, Workbook workbook) {
		return this.workbook.linkExternalWorkbook(name, workbook);
	}

	public boolean isWriteProtected() {
		return this.workbook.isWriteProtected();
	}

	public void writeProtectWorkbook(String password, String username) {
		this.workbook.writeProtectWorkbook(password, username);
	}

	public void unwriteProtectWorkbook() {
		this.workbook.unwriteProtectWorkbook();
	}

	public List<HSSFObjectData> getAllEmbeddedObjects() {
		List<HSSFObjectData> objects = new ArrayList<>();
		for (HSSFSheet sheet : _sheets) {
			getAllEmbeddedObjects(sheet, objects);
		}
		return Collections.unmodifiableList(objects);
	}

	private void getAllEmbeddedObjects(HSSFSheet sheet, List<HSSFObjectData> objects) {
		HSSFPatriarch patriarch = sheet.getDrawingPatriarch();
		if (null == patriarch) {
			return;
		}
		getAllEmbeddedObjects(patriarch, objects);
	}

	private void getAllEmbeddedObjects(HSSFShapeContainer parent, List<HSSFObjectData> objects) {
		for (HSSFShape shape : parent.getChildren()) {
			if (shape instanceof HSSFObjectData) {
				objects.add(((HSSFObjectData) (shape)));
			}else
				if (shape instanceof HSSFShapeContainer) {
					getAllEmbeddedObjects(((HSSFShapeContainer) (shape)), objects);
				}

		}
	}

	@Override
	public HSSFCreationHelper getCreationHelper() {
		return null;
	}

	UDFFinder getUDFFinder() {
		return _udfFinder;
	}

	@Override
	public void addToolPack(UDFFinder toopack) {
		AggregatingUDFFinder udfs = ((AggregatingUDFFinder) (_udfFinder));
		udfs.add(toopack);
	}

	@Override
	public void setForceFormulaRecalculation(boolean value) {
		InternalWorkbook iwb = getWorkbook();
		RecalcIdRecord recalc = iwb.getRecalcId();
		recalc.setEngineId(0);
	}

	@Override
	public boolean getForceFormulaRecalculation() {
		InternalWorkbook iwb = getWorkbook();
		RecalcIdRecord recalc = ((RecalcIdRecord) (iwb.findFirstRecordBySid(RecalcIdRecord.sid)));
		return (recalc != null) && ((recalc.getEngineId()) != 0);
	}

	public boolean changeExternalReference(String oldUrl, String newUrl) {
		return workbook.changeExternalReference(oldUrl, newUrl);
	}

	@org.apache.poi.util.Internal
	public InternalWorkbook getInternalWorkbook() {
		return workbook;
	}

	@Override
	public SpreadsheetVersion getSpreadsheetVersion() {
		return SpreadsheetVersion.EXCEL97;
	}

	@Override
	public EncryptionInfo getEncryptionInfo() {
		FilePassRecord fpr = ((FilePassRecord) (workbook.findFirstRecordBySid(FilePassRecord.sid)));
		return fpr != null ? fpr.getEncryptionInfo() : null;
	}

	private void updateEncryptionInfo() {
		readProperties();
		FilePassRecord fpr = ((FilePassRecord) (workbook.findFirstRecordBySid(FilePassRecord.sid)));
		String password = Biff8EncryptionKey.getCurrentUserPassword();
		WorkbookRecordList wrl = workbook.getWorkbookRecordList();
		if (password == null) {
			if (fpr != null) {
				wrl.remove(fpr);
			}
		}else {
			if (fpr == null) {
				fpr = new FilePassRecord(EncryptionMode.cryptoAPI);
				wrl.add(1, fpr);
			}
			EncryptionInfo ei = fpr.getEncryptionInfo();
			EncryptionVerifier ver = ei.getVerifier();
			byte[] encVer = ver.getEncryptedVerifier();
			Decryptor dec = ei.getDecryptor();
			Encryptor enc = ei.getEncryptor();
			try {
				if ((encVer == null) || (!(dec.verifyPassword(password)))) {
					enc.confirmPassword(password);
				}else {
					byte[] verifier = dec.getVerifier();
					byte[] salt = ver.getSalt();
					enc.confirmPassword(password, null, null, verifier, salt, null);
				}
			} catch (GeneralSecurityException e) {
				throw new EncryptedDocumentException("can't validate/update encryption setting", e);
			}
		}
	}
}

