

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;
import org.apache.poi.common.usermodel.fonts.FontCharset;
import org.apache.poi.hwpf.HWPFDocumentCore;
import org.apache.poi.hwpf.model.CHPBinTable;
import org.apache.poi.hwpf.model.ComplexFileTable;
import org.apache.poi.hwpf.model.FibBase;
import org.apache.poi.hwpf.model.FileInformationBlock;
import org.apache.poi.hwpf.model.FontTable;
import org.apache.poi.hwpf.model.OldCHPBinTable;
import org.apache.poi.hwpf.model.OldComplexFileTable;
import org.apache.poi.hwpf.model.OldFfn;
import org.apache.poi.hwpf.model.OldFontTable;
import org.apache.poi.hwpf.model.OldPAPBinTable;
import org.apache.poi.hwpf.model.OldSectionTable;
import org.apache.poi.hwpf.model.OldTextPieceTable;
import org.apache.poi.hwpf.model.PAPBinTable;
import org.apache.poi.hwpf.model.PieceDescriptor;
import org.apache.poi.hwpf.model.SectionTable;
import org.apache.poi.hwpf.model.TextPiece;
import org.apache.poi.hwpf.model.TextPieceTable;
import org.apache.poi.hwpf.model.types.FibBaseAbstractType;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.CodePageUtil;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StringUtil;


public class HWPFOldDocument extends HWPFDocumentCore {
	private static final POILogger logger = POILogFactory.getLogger(HWPFOldDocument.class);

	private static final int MAX_RECORD_LENGTH = 10000000;

	private static final Charset DEFAULT_CHARSET = StringUtil.WIN_1252;

	private OldTextPieceTable tpt;

	private StringBuilder _text;

	private final OldFontTable fontTable;

	private final Charset guessedCharset;

	public HWPFOldDocument(POIFSFileSystem fs) throws IOException {
		this(fs.getRoot());
	}

	public HWPFOldDocument(DirectoryNode directory) throws IOException {
		super(directory);
		int sedTableOffset = LittleEndian.getInt(_mainStream, 136);
		int sedTableSize = LittleEndian.getInt(_mainStream, 140);
		int chpTableOffset = LittleEndian.getInt(_mainStream, 184);
		int chpTableSize = LittleEndian.getInt(_mainStream, 188);
		int papTableOffset = LittleEndian.getInt(_mainStream, 192);
		int papTableSize = LittleEndian.getInt(_mainStream, 196);
		int fontTableOffset = LittleEndian.getInt(_mainStream, 208);
		int fontTableSize = LittleEndian.getInt(_mainStream, 212);
		fontTable = new OldFontTable(_mainStream, fontTableOffset, fontTableSize);
		guessedCharset = guessCodePage(fontTable);
		int complexTableOffset = LittleEndian.getInt(_mainStream, 352);
		ComplexFileTable cft = null;
		if (_fib.getFibBase().isFComplex()) {
			cft = new OldComplexFileTable(_mainStream, _mainStream, complexTableOffset, _fib.getFibBase().getFcMin(), guessedCharset);
			tpt = ((OldTextPieceTable) (cft.getTextPieceTable()));
		}else {
			TextPiece tp = null;
			try {
				tp = buildTextPiece(guessedCharset);
			} catch (IllegalStateException e) {
				tp = buildTextPiece(StringUtil.WIN_1252);
				HWPFOldDocument.logger.log(POILogger.WARN, (("Error with " + (guessedCharset)) + ". Backing off to Windows-1252"));
			}
			tpt.add(tp);
		}
		_text = tpt.getText();
		_cbt = new OldCHPBinTable(_mainStream, chpTableOffset, chpTableSize, _fib.getFibBase().getFcMin(), tpt);
		_pbt = new OldPAPBinTable(_mainStream, papTableOffset, papTableSize, _fib.getFibBase().getFcMin(), tpt);
		_st = new OldSectionTable(_mainStream, sedTableOffset, sedTableSize, _fib.getFibBase().getFcMin(), tpt);
		boolean preserveBinTables = false;
		try {
		} catch (Exception exc) {
		}
		if (!preserveBinTables) {
			_cbt.rebuild(cft);
			_pbt.rebuild(_text, cft);
		}
	}

	private TextPiece buildTextPiece(Charset guessedCharset) throws IllegalStateException {
		PieceDescriptor pd = new PieceDescriptor(new byte[]{ 0, 0, 0, 0, 0, 127, 0, 0 }, 0, guessedCharset);
		pd.setFilePosition(_fib.getFibBase().getFcMin());
		tpt = new OldTextPieceTable();
		byte[] textData = IOUtils.safelyAllocate(((_fib.getFibBase().getFcMac()) - (_fib.getFibBase().getFcMin())), HWPFOldDocument.MAX_RECORD_LENGTH);
		System.arraycopy(_mainStream, _fib.getFibBase().getFcMin(), textData, 0, textData.length);
		int numChars = textData.length;
		if (CodePageUtil.DOUBLE_BYTE_CHARSETS.contains(guessedCharset)) {
			numChars /= 2;
		}
		return new TextPiece(0, numChars, textData, pd);
	}

	private Charset guessCodePage(OldFontTable fontTable) {
		for (OldFfn oldFfn : fontTable.getFontNames()) {
			FontCharset wmfCharset = FontCharset.valueOf(((oldFfn.getChs()) & 255));
			if ((((wmfCharset != null) && (wmfCharset != (FontCharset.ANSI))) && (wmfCharset != (FontCharset.DEFAULT))) && (wmfCharset != (FontCharset.SYMBOL))) {
				return wmfCharset.getCharset();
			}
		}
		HWPFOldDocument.logger.log(POILogger.WARN, "Couldn't find a defined charset; backing off to cp1252");
		return HWPFOldDocument.DEFAULT_CHARSET;
	}

	public Range getOverallRange() {
		return new Range(0, ((_fib.getFibBase().getFcMac()) - (_fib.getFibBase().getFcMin())), this);
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public FontTable getFontTable() {
		throw new UnsupportedOperationException("Use getOldFontTable instead.");
	}

	public OldFontTable getOldFontTable() {
		return fontTable;
	}

	public Range getRange() {
		return getOverallRange();
	}

	public TextPieceTable getTextTable() {
		return tpt;
	}

	@Override
	public StringBuilder getText() {
		return _text;
	}

	@Override
	public void write() throws IOException {
		throw new IllegalStateException("Writing is not available for the older file formats");
	}

	@Override
	public void write(File out) throws IOException {
		throw new IllegalStateException("Writing is not available for the older file formats");
	}

	@Override
	public void write(OutputStream out) throws IOException {
		throw new IllegalStateException("Writing is not available for the older file formats");
	}

	public Charset getGuessedCharset() {
		return guessedCharset;
	}
}

