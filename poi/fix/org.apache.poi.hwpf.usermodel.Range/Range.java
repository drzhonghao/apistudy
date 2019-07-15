

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.HWPFDocumentCore;
import org.apache.poi.hwpf.model.CHPBinTable;
import org.apache.poi.hwpf.model.CHPX;
import org.apache.poi.hwpf.model.FileInformationBlock;
import org.apache.poi.hwpf.model.PAPBinTable;
import org.apache.poi.hwpf.model.PAPX;
import org.apache.poi.hwpf.model.PropertyNode;
import org.apache.poi.hwpf.model.SEPX;
import org.apache.poi.hwpf.model.SectionTable;
import org.apache.poi.hwpf.model.StyleSheet;
import org.apache.poi.hwpf.model.SubdocumentType;
import org.apache.poi.hwpf.model.types.PAPAbstractType;
import org.apache.poi.hwpf.sprm.CharacterSprmCompressor;
import org.apache.poi.hwpf.sprm.ParagraphSprmCompressor;
import org.apache.poi.hwpf.sprm.SprmBuffer;
import org.apache.poi.hwpf.usermodel.CharacterProperties;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.ParagraphProperties;
import org.apache.poi.hwpf.usermodel.Section;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.util.DocumentFormatException;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public class Range {
	private POILogger logger = POILogFactory.getLogger(Range.class);

	@Deprecated
	public static final int TYPE_PARAGRAPH = 0;

	@Deprecated
	public static final int TYPE_CHARACTER = 1;

	@Deprecated
	public static final int TYPE_SECTION = 2;

	@Deprecated
	public static final int TYPE_TEXT = 3;

	@Deprecated
	public static final int TYPE_LISTENTRY = 4;

	@Deprecated
	public static final int TYPE_TABLE = 5;

	@Deprecated
	public static final int TYPE_UNDEFINED = 6;

	private final WeakReference<Range> _parent;

	protected final int _start;

	protected int _end;

	protected final HWPFDocumentCore _doc;

	boolean _sectionRangeFound;

	protected final List<SEPX> _sections;

	protected int _sectionStart;

	protected int _sectionEnd;

	protected boolean _parRangeFound;

	protected final List<PAPX> _paragraphs;

	protected int _parStart;

	protected int _parEnd;

	protected boolean _charRangeFound;

	protected List<CHPX> _characters;

	protected int _charStart;

	protected int _charEnd;

	protected StringBuilder _text;

	public Range(int start, int end, HWPFDocumentCore doc) {
		_start = start;
		_end = end;
		_doc = doc;
		_sections = _doc.getSectionTable().getSections();
		_paragraphs = _doc.getParagraphTable().getParagraphs();
		_characters = _doc.getCharacterTable().getTextRuns();
		_text = _doc.getText();
		_parent = new WeakReference<>(null);
		sanityCheckStartEnd();
	}

	protected Range(int start, int end, Range parent) {
		_start = start;
		_end = end;
		_doc = parent._doc;
		_sections = parent._sections;
		_paragraphs = parent._paragraphs;
		_characters = parent._characters;
		_text = parent._text;
		_parent = new WeakReference<>(parent);
		sanityCheckStartEnd();
		sanityCheck();
	}

	private void sanityCheckStartEnd() {
		if ((_start) < 0) {
			throw new IllegalArgumentException(("Range start must not be negative. Given " + (_start)));
		}
		if ((_end) < (_start)) {
			throw new IllegalArgumentException((((("The end (" + (_end)) + ") must not be before the start (") + (_start)) + ")"));
		}
	}

	public String text() {
		return _text.substring(_start, _end);
	}

	public static String stripFields(String text) {
		if ((text.indexOf('\u0013')) == (-1))
			return text;

		while (((text.indexOf('\u0013')) > (-1)) && ((text.indexOf('\u0015')) > (-1))) {
			int first13 = text.indexOf('\u0013');
			int next13 = text.indexOf('\u0013', (first13 + 1));
			int first14 = text.indexOf('\u0014', (first13 + 1));
			int last15 = text.lastIndexOf('\u0015');
			if (last15 < first13) {
				break;
			}
			if ((next13 == (-1)) && (first14 == (-1))) {
				text = (text.substring(0, first13)) + (text.substring((last15 + 1)));
				break;
			}
			if ((first14 != (-1)) && ((first14 < next13) || (next13 == (-1)))) {
				text = ((text.substring(0, first13)) + (text.substring((first14 + 1), last15))) + (text.substring((last15 + 1)));
				continue;
			}
			text = (text.substring(0, first13)) + (text.substring((last15 + 1)));
		} 
		return text;
	}

	public int numSections() {
		initSections();
		return (_sectionEnd) - (_sectionStart);
	}

	public int numParagraphs() {
		initParagraphs();
		return (_parEnd) - (_parStart);
	}

	public int numCharacterRuns() {
		initCharacterRuns();
		return (_charEnd) - (_charStart);
	}

	public CharacterRun insertBefore(String text) {
		initAll();
		_text.insert(_start, text);
		_doc.getCharacterTable().adjustForInsert(_charStart, text.length());
		_doc.getParagraphTable().adjustForInsert(_parStart, text.length());
		_doc.getSectionTable().adjustForInsert(_sectionStart, text.length());
		if ((_doc) instanceof HWPFDocument) {
		}
		adjustForInsert(text.length());
		adjustFIB(text.length());
		sanityCheck();
		return getCharacterRun(0);
	}

	public CharacterRun insertAfter(String text) {
		initAll();
		_text.insert(_end, text);
		_doc.getCharacterTable().adjustForInsert(((_charEnd) - 1), text.length());
		_doc.getParagraphTable().adjustForInsert(((_parEnd) - 1), text.length());
		_doc.getSectionTable().adjustForInsert(((_sectionEnd) - 1), text.length());
		if ((_doc) instanceof HWPFDocument) {
		}
		adjustForInsert(text.length());
		sanityCheck();
		return getCharacterRun(((numCharacterRuns()) - 1));
	}

	@Deprecated
	private CharacterRun insertBefore(String text, CharacterProperties props) {
		initAll();
		PAPX papx = _paragraphs.get(_parStart);
		short istd = papx.getIstd();
		StyleSheet ss = _doc.getStyleSheet();
		CharacterProperties baseStyle = ss.getCharacterStyle(istd);
		byte[] grpprl = CharacterSprmCompressor.compressCharacterProperty(props, baseStyle);
		SprmBuffer buf = new SprmBuffer(grpprl, 0);
		_doc.getCharacterTable().insert(_charStart, _start, buf);
		return insertBefore(text);
	}

	@Deprecated
	private CharacterRun insertAfter(String text, CharacterProperties props) {
		initAll();
		PAPX papx = _paragraphs.get(((_parEnd) - 1));
		short istd = papx.getIstd();
		StyleSheet ss = _doc.getStyleSheet();
		CharacterProperties baseStyle = ss.getCharacterStyle(istd);
		byte[] grpprl = CharacterSprmCompressor.compressCharacterProperty(props, baseStyle);
		SprmBuffer buf = new SprmBuffer(grpprl, 0);
		_doc.getCharacterTable().insert(_charEnd, _end, buf);
		(_charEnd)++;
		return insertAfter(text);
	}

	@Deprecated
	private Paragraph insertBefore(ParagraphProperties props, int styleIndex) {
		return this.insertBefore(props, styleIndex, "\r");
	}

	@Deprecated
	private Paragraph insertBefore(ParagraphProperties props, int styleIndex, String text) {
		initAll();
		StyleSheet ss = _doc.getStyleSheet();
		ParagraphProperties baseStyle = ss.getParagraphStyle(styleIndex);
		CharacterProperties baseChp = ss.getCharacterStyle(styleIndex);
		byte[] grpprl = ParagraphSprmCompressor.compressParagraphProperty(props, baseStyle);
		byte[] withIndex = new byte[(grpprl.length) + (LittleEndian.SHORT_SIZE)];
		LittleEndian.putShort(withIndex, 0, ((short) (styleIndex)));
		System.arraycopy(grpprl, 0, withIndex, LittleEndian.SHORT_SIZE, grpprl.length);
		SprmBuffer buf = new SprmBuffer(withIndex, 2);
		_doc.getParagraphTable().insert(_parStart, _start, buf);
		insertBefore(text, baseChp);
		return getParagraph(0);
	}

	@Deprecated
	Paragraph insertAfter(ParagraphProperties props, int styleIndex) {
		return this.insertAfter(props, styleIndex, "\r");
	}

	@Deprecated
	Paragraph insertAfter(ParagraphProperties props, int styleIndex, String text) {
		initAll();
		StyleSheet ss = _doc.getStyleSheet();
		ParagraphProperties baseStyle = ss.getParagraphStyle(styleIndex);
		CharacterProperties baseChp = ss.getCharacterStyle(styleIndex);
		byte[] grpprl = ParagraphSprmCompressor.compressParagraphProperty(props, baseStyle);
		byte[] withIndex = new byte[(grpprl.length) + (LittleEndian.SHORT_SIZE)];
		LittleEndian.putShort(withIndex, 0, ((short) (styleIndex)));
		System.arraycopy(grpprl, 0, withIndex, LittleEndian.SHORT_SIZE, grpprl.length);
		SprmBuffer buf = new SprmBuffer(withIndex, 2);
		_doc.getParagraphTable().insert(_parEnd, _end, buf);
		(_parEnd)++;
		insertAfter(text, baseChp);
		return getParagraph(((numParagraphs()) - 1));
	}

	public void delete() {
		initAll();
		int numSections = _sections.size();
		int numRuns = _characters.size();
		int numParagraphs = _paragraphs.size();
		for (int x = _charStart; x < numRuns; x++) {
			CHPX chpx = _characters.get(x);
			chpx.adjustForDelete(_start, ((_end) - (_start)));
		}
		for (int x = _parStart; x < numParagraphs; x++) {
			PAPX papx = _paragraphs.get(x);
			papx.adjustForDelete(_start, ((_end) - (_start)));
		}
		for (int x = _sectionStart; x < numSections; x++) {
			SEPX sepx = _sections.get(x);
			sepx.adjustForDelete(_start, ((_end) - (_start)));
		}
		if ((_doc) instanceof HWPFDocument) {
		}
		_text.delete(_start, _end);
		Range parent = _parent.get();
		if (parent != null) {
			parent.adjustForInsert((-((_end) - (_start))));
		}
		adjustFIB((-((_end) - (_start))));
	}

	public Table insertTableBefore(short columns, int rows) {
		ParagraphProperties parProps = new ParagraphProperties();
		parProps.setFInTable(true);
		parProps.setItap(1);
		final int oldEnd = this._end;
		for (int x = 0; x < rows; x++) {
			Paragraph cell = this.insertBefore(parProps, StyleSheet.NIL_STYLE);
			cell.insertAfter(String.valueOf('\u0007'));
			for (int y = 1; y < columns; y++) {
				cell.insertAfter(String.valueOf('\u0007'));
			}
		}
		final int newEnd = this._end;
		final int diff = newEnd - oldEnd;
		return null;
	}

	public void replaceText(String newText, boolean addAfter) {
		if (addAfter) {
			int originalEnd = getEndOffset();
			insertAfter(newText);
			new Range(getStartOffset(), originalEnd, this).delete();
		}else {
			int originalStart = getStartOffset();
			int originalEnd = getEndOffset();
			insertBefore(newText);
			new Range((originalStart + (newText.length())), (originalEnd + (newText.length())), this).delete();
		}
	}

	@org.apache.poi.util.Internal
	public void replaceText(String pPlaceHolder, String pValue, int pOffset) {
		int absPlaceHolderIndex = (getStartOffset()) + pOffset;
		Range subRange = new Range(absPlaceHolderIndex, (absPlaceHolderIndex + (pPlaceHolder.length())), this);
		subRange.insertBefore(pValue);
		subRange = new Range((absPlaceHolderIndex + (pValue.length())), ((absPlaceHolderIndex + (pPlaceHolder.length())) + (pValue.length())), this);
		subRange.delete();
	}

	public void replaceText(String pPlaceHolder, String pValue) {
		while (true) {
			String text = text();
			int offset = text.indexOf(pPlaceHolder);
			if (offset >= 0) {
				replaceText(pPlaceHolder, pValue, offset);
			}else {
				break;
			}
		} 
	}

	public CharacterRun getCharacterRun(int index) {
		initCharacterRuns();
		if ((index + (_charStart)) >= (_charEnd))
			throw new IndexOutOfBoundsException((((((((("CHPX #" + index) + " (") + (index + (_charStart))) + ") not in range [") + (_charStart)) + "; ") + (_charEnd)) + ")"));

		CHPX chpx = _characters.get((index + (_charStart)));
		if (chpx == null) {
			return null;
		}
		short istd;
		return null;
	}

	public Section getSection(int index) {
		initSections();
		SEPX sepx = _sections.get((index + (_sectionStart)));
		return null;
	}

	public Paragraph getParagraph(int index) {
		initParagraphs();
		if ((index + (_parStart)) >= (_parEnd))
			throw new IndexOutOfBoundsException((((((((("Paragraph #" + index) + " (") + (index + (_parStart))) + ") not in range [") + (_parStart)) + "; ") + (_parEnd)) + ")"));

		PAPX papx = _paragraphs.get((index + (_parStart)));
		return null;
	}

	public Table getTable(Paragraph paragraph) {
		if (!(paragraph.isInTable())) {
			throw new IllegalArgumentException("This paragraph doesn't belong to a table");
		}
		int tableLevel = paragraph.getTableLevel();
		int limit = _paragraphs.size();
		initAll();
		return null;
	}

	protected void initAll() {
		initCharacterRuns();
		initParagraphs();
		initSections();
	}

	private void initParagraphs() {
		if (!(_parRangeFound)) {
			int[] point = findRange(_paragraphs, _start, _end);
			_parStart = point[0];
			_parEnd = point[1];
			_parRangeFound = true;
		}
	}

	private void initCharacterRuns() {
		if (!(_charRangeFound)) {
			int[] point = findRange(_characters, _start, _end);
			_charStart = point[0];
			_charEnd = point[1];
			_charRangeFound = true;
		}
	}

	private void initSections() {
		if (!(_sectionRangeFound)) {
			int[] point = findRange(_sections, _sectionStart, _start, _end);
			_sectionStart = point[0];
			_sectionEnd = point[1];
			_sectionRangeFound = true;
		}
	}

	private static int binarySearchStart(List<? extends PropertyNode<?>> rpl, int start) {
		if ((rpl.size()) == 0)
			return -1;

		if ((rpl.get(0).getStart()) >= start)
			return 0;

		int low = 0;
		int high = (rpl.size()) - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			PropertyNode<?> node = rpl.get(mid);
			if ((node.getStart()) < start) {
				low = mid + 1;
			}else
				if ((node.getStart()) > start) {
					high = mid - 1;
				}else {
					assert (node.getStart()) == start;
					return mid;
				}

		} 
		assert low != 0;
		return low - 1;
	}

	private static int binarySearchEnd(List<? extends PropertyNode<?>> rpl, int foundStart, int end) {
		if ((rpl.get(((rpl.size()) - 1)).getEnd()) <= end)
			return (rpl.size()) - 1;

		int low = foundStart;
		int high = (rpl.size()) - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			PropertyNode<?> node = rpl.get(mid);
			if ((node.getEnd()) < end) {
				low = mid + 1;
			}else
				if ((node.getEnd()) > end) {
					high = mid - 1;
				}else {
					assert (node.getEnd()) == end;
					return mid;
				}

		} 
		assert (0 <= low) && (low < (rpl.size()));
		return low;
	}

	private int[] findRange(List<? extends PropertyNode<?>> rpl, int start, int end) {
		int startIndex = Range.binarySearchStart(rpl, start);
		while ((startIndex > 0) && ((rpl.get((startIndex - 1)).getStart()) >= start))
			startIndex--;

		int endIndex = Range.binarySearchEnd(rpl, startIndex, end);
		while ((endIndex < ((rpl.size()) - 1)) && ((rpl.get((endIndex + 1)).getEnd()) <= end))
			endIndex++;

		if (((((startIndex < 0) || (startIndex >= (rpl.size()))) || (startIndex > endIndex)) || (endIndex < 0)) || (endIndex >= (rpl.size()))) {
			throw new DocumentFormatException("problem finding range");
		}
		return new int[]{ startIndex, endIndex + 1 };
	}

	private int[] findRange(List<? extends PropertyNode<?>> rpl, int min, int start, int end) {
		int x = min;
		if ((rpl.size()) == min)
			return new int[]{ min, min };

		PropertyNode<?> node = rpl.get(x);
		while ((node == null) || (((node.getEnd()) <= start) && (x < ((rpl.size()) - 1)))) {
			x++;
			if (x >= (rpl.size())) {
				return new int[]{ 0, 0 };
			}
			node = rpl.get(x);
		} 
		if ((node.getStart()) > end) {
			return new int[]{ 0, 0 };
		}
		if ((node.getEnd()) <= start) {
			return new int[]{ rpl.size(), rpl.size() };
		}
		for (int y = x; y < (rpl.size()); y++) {
			node = rpl.get(y);
			if (node == null)
				continue;

			if (((node.getStart()) < end) && ((node.getEnd()) <= end))
				continue;

			if ((node.getStart()) < end)
				return new int[]{ x, y + 1 };

			return new int[]{ x, y };
		}
		return new int[]{ x, rpl.size() };
	}

	protected void reset() {
		_charRangeFound = false;
		_parRangeFound = false;
		_sectionRangeFound = false;
	}

	protected void adjustFIB(int adjustment) {
		if (!((_doc) instanceof HWPFDocument)) {
			throw new IllegalArgumentException("doc must be instance of HWPFDocument");
		}
		FileInformationBlock fib = _doc.getFileInformationBlock();
		int currentEnd = 0;
		for (SubdocumentType type : SubdocumentType.ORDERED) {
			int currentLength = fib.getSubdocumentTextStreamLength(type);
			currentEnd += currentLength;
			if ((_start) > currentEnd)
				continue;

			fib.setSubdocumentTextStreamLength(type, (currentLength + adjustment));
			break;
		}
	}

	private void adjustForInsert(int length) {
		_end += length;
		reset();
		Range parent = _parent.get();
		if (parent != null) {
			parent.adjustForInsert(length);
		}
	}

	public int getStartOffset() {
		return _start;
	}

	public int getEndOffset() {
		return _end;
	}

	protected HWPFDocumentCore getDocument() {
		return _doc;
	}

	@Override
	public String toString() {
		return ((("Range from " + (getStartOffset())) + " to ") + (getEndOffset())) + " (chars)";
	}

	public boolean sanityCheck() {
		DocumentFormatException.check(((_start) >= 0), "start can't be < 0");
		DocumentFormatException.check(((_start) <= (_text.length())), "start can't be > text length");
		DocumentFormatException.check(((_end) >= 0), "end can't be < 0");
		DocumentFormatException.check(((_end) <= (_text.length())), "end can't be > text length");
		DocumentFormatException.check(((_start) <= (_end)), "start can't be > end");
		if (_charRangeFound) {
			for (int c = _charStart; c < (_charEnd); c++) {
				CHPX chpx = _characters.get(c);
				int left = Math.max(this._start, chpx.getStart());
				int right = Math.min(this._end, chpx.getEnd());
				DocumentFormatException.check((left < right), "left must be < right");
			}
		}
		if (_parRangeFound) {
			for (int p = _parStart; p < (_parEnd); p++) {
				PAPX papx = _paragraphs.get(p);
				int left = Math.max(this._start, papx.getStart());
				int right = Math.min(this._end, papx.getEnd());
				DocumentFormatException.check((left < right), "left must be < right");
			}
		}
		return true;
	}
}

