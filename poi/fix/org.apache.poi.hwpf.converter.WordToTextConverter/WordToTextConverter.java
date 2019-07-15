

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.HWPFDocumentCore;
import org.apache.poi.hwpf.converter.AbstractWordConverter;
import org.apache.poi.hwpf.converter.AbstractWordUtils;
import org.apache.poi.hwpf.converter.TextDocumentFacade;
import org.apache.poi.hwpf.usermodel.Bookmark;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.OfficeDrawing;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Section;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.util.Beta;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;


@Beta
public class WordToTextConverter extends AbstractWordConverter {
	private static final POILogger logger = POILogFactory.getLogger(WordToTextConverter.class);

	public static String getText(DirectoryNode root) throws Exception {
		final HWPFDocumentCore wordDocument = AbstractWordUtils.loadDoc(root);
		return WordToTextConverter.getText(wordDocument);
	}

	public static String getText(File docFile) throws Exception {
		final HWPFDocumentCore wordDocument = AbstractWordUtils.loadDoc(docFile);
		return WordToTextConverter.getText(wordDocument);
	}

	public static String getText(final HWPFDocumentCore wordDocument) throws Exception {
		WordToTextConverter wordToTextConverter = new WordToTextConverter(XMLHelper.getDocumentBuilderFactory().newDocumentBuilder().newDocument());
		wordToTextConverter.processDocument(wordDocument);
		return wordToTextConverter.getText();
	}

	public static void main(String[] args) throws Exception {
		if ((args.length) < 2) {
			System.err.println("Usage: WordToTextConverter <inputFile.doc> <saveTo.txt>");
			return;
		}
		System.out.println(("Converting " + (args[0])));
		System.out.println(("Saving output to " + (args[1])));
		Document doc = WordToTextConverter.process(new File(args[0]));
		DOMSource domSource = new DOMSource(doc);
		StreamResult streamResult = new StreamResult(new File(args[1]));
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer serializer = tf.newTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT, "no");
		serializer.setOutputProperty(OutputKeys.METHOD, "text");
		serializer.transform(domSource, streamResult);
	}

	private static Document process(File docFile) throws IOException, ParserConfigurationException {
		try (final HWPFDocumentCore wordDocument = AbstractWordUtils.loadDoc(docFile)) {
			WordToTextConverter wordToTextConverter = new WordToTextConverter(XMLHelper.getDocumentBuilderFactory().newDocumentBuilder().newDocument());
			wordToTextConverter.processDocument(wordDocument);
			return wordToTextConverter.getDocument();
		}
	}

	private AtomicInteger noteCounters = new AtomicInteger(1);

	private Element notes;

	private boolean outputSummaryInformation;

	private final TextDocumentFacade textDocumentFacade;

	public WordToTextConverter() throws ParserConfigurationException {
		this.textDocumentFacade = new TextDocumentFacade(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
	}

	@SuppressWarnings("WeakerAccess")
	public WordToTextConverter(Document document) {
		this.textDocumentFacade = new TextDocumentFacade(document);
	}

	@SuppressWarnings("unused")
	public WordToTextConverter(TextDocumentFacade textDocumentFacade) {
		this.textDocumentFacade = textDocumentFacade;
	}

	@Override
	protected void afterProcess() {
		if ((notes) != null)
			textDocumentFacade.getBody().appendChild(notes);

	}

	public Document getDocument() {
		return textDocumentFacade.getDocument();
	}

	public String getText() throws Exception {
		StringWriter stringWriter = new StringWriter();
		DOMSource domSource = new DOMSource(getDocument());
		StreamResult streamResult = new StreamResult(stringWriter);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer serializer = tf.newTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT, "no");
		serializer.setOutputProperty(OutputKeys.METHOD, "text");
		serializer.transform(domSource, streamResult);
		return stringWriter.toString();
	}

	@SuppressWarnings("WeakerAccess")
	public boolean isOutputSummaryInformation() {
		return outputSummaryInformation;
	}

	@Override
	protected void outputCharacters(Element block, CharacterRun characterRun, String text) {
		block.appendChild(textDocumentFacade.createText(text));
	}

	@Override
	protected void processBookmarks(HWPFDocumentCore wordDocument, Element currentBlock, Range range, int currentTableLevel, List<Bookmark> rangeBookmarks) {
		processCharacters(wordDocument, currentTableLevel, range, currentBlock);
	}

	@Override
	protected void processDocumentInformation(SummaryInformation summaryInformation) {
		if (isOutputSummaryInformation()) {
		}
	}

	@Override
	public void processDocumentPart(HWPFDocumentCore wordDocument, Range range) {
		super.processDocumentPart(wordDocument, range);
		afterProcess();
	}

	@Override
	protected void processDrawnObject(HWPFDocument doc, CharacterRun characterRun, OfficeDrawing officeDrawing, String path, Element block) {
	}

	@Override
	protected void processEndnoteAutonumbered(HWPFDocument wordDocument, int noteIndex, Element block, Range endnoteTextRange) {
		processNote(wordDocument, block, endnoteTextRange);
	}

	@Override
	protected void processFootnoteAutonumbered(HWPFDocument wordDocument, int noteIndex, Element block, Range footnoteTextRange) {
		processNote(wordDocument, block, footnoteTextRange);
	}

	@Override
	protected void processHyperlink(HWPFDocumentCore wordDocument, Element currentBlock, Range textRange, int currentTableLevel, String hyperlink) {
		processCharacters(wordDocument, currentTableLevel, textRange, currentBlock);
		currentBlock.appendChild(textDocumentFacade.createText(((((" (" + (AbstractWordConverter.UNICODECHAR_ZERO_WIDTH_SPACE)) + (hyperlink.replaceAll("/", (((AbstractWordConverter.UNICODECHAR_ZERO_WIDTH_SPACE) + "\\/") + (AbstractWordConverter.UNICODECHAR_ZERO_WIDTH_SPACE))))) + (AbstractWordConverter.UNICODECHAR_ZERO_WIDTH_SPACE)) + ")")));
	}

	@Override
	protected void processImage(Element currentBlock, boolean inlined, Picture picture) {
	}

	@Override
	protected void processImage(Element currentBlock, boolean inlined, Picture picture, String url) {
	}

	@Override
	protected void processImageWithoutPicturesManager(Element currentBlock, boolean inlined, Picture picture) {
	}

	@Override
	protected void processLineBreak(Element block, CharacterRun characterRun) {
		block.appendChild(textDocumentFacade.createText("\n"));
	}

	private void processNote(HWPFDocument wordDocument, Element block, Range noteTextRange) {
		final int noteIndex = noteCounters.getAndIncrement();
		block.appendChild(textDocumentFacade.createText((((((AbstractWordConverter.UNICODECHAR_ZERO_WIDTH_SPACE) + "[") + noteIndex) + "]") + (AbstractWordConverter.UNICODECHAR_ZERO_WIDTH_SPACE))));
		if ((notes) == null)
			notes = textDocumentFacade.createBlock();

		Element note = textDocumentFacade.createBlock();
		notes.appendChild(note);
		note.appendChild(textDocumentFacade.createText((("^" + noteIndex) + "\t ")));
		processCharacters(wordDocument, Integer.MIN_VALUE, noteTextRange, note);
		note.appendChild(textDocumentFacade.createText("\n"));
	}

	@Override
	protected boolean processOle2(HWPFDocument wordDocument, Element block, Entry entry) throws Exception {
		if (!(entry instanceof DirectoryNode))
			return false;

		DirectoryNode directoryNode = ((DirectoryNode) (entry));
		if (directoryNode.hasEntry("WordDocument")) {
			String text = WordToTextConverter.getText(((DirectoryNode) (entry)));
			block.appendChild(textDocumentFacade.createText((((AbstractWordConverter.UNICODECHAR_ZERO_WIDTH_SPACE) + text) + (AbstractWordConverter.UNICODECHAR_ZERO_WIDTH_SPACE))));
			return true;
		}
		Object extractor;
		try {
			Class<?> cls = Class.forName("org.apache.poi.extractor.ExtractorFactory");
			Method createExtractor = cls.getMethod("createExtractor", DirectoryNode.class);
			extractor = createExtractor.invoke(null, directoryNode);
		} catch (Exception exc) {
			WordToTextConverter.logger.log(POILogger.WARN, "There is an OLE object entry '", entry.getName(), "', but there is no text extractor for this object type ", "or text extractor factory is not available: ", ("" + exc));
			return false;
		}
		try {
			Method getText = extractor.getClass().getMethod("getText");
			String text = ((String) (getText.invoke(extractor)));
			block.appendChild(textDocumentFacade.createText((((AbstractWordConverter.UNICODECHAR_ZERO_WIDTH_SPACE) + text) + (AbstractWordConverter.UNICODECHAR_ZERO_WIDTH_SPACE))));
			return true;
		} catch (Exception exc) {
			WordToTextConverter.logger.log(POILogger.ERROR, "Unable to extract text from OLE entry '", entry.getName(), "': ", exc, exc);
			return false;
		}
	}

	@Override
	protected void processPageBreak(HWPFDocumentCore wordDocument, Element flow) {
		Element block = textDocumentFacade.createBlock();
		block.appendChild(textDocumentFacade.createText("\n"));
		flow.appendChild(block);
	}

	@Override
	protected void processPageref(HWPFDocumentCore wordDocument, Element currentBlock, Range textRange, int currentTableLevel, String pageref) {
		processCharacters(wordDocument, currentTableLevel, textRange, currentBlock);
	}

	@Override
	protected void processParagraph(HWPFDocumentCore wordDocument, Element parentElement, int currentTableLevel, Paragraph paragraph, String bulletText) {
		Element pElement = textDocumentFacade.createParagraph();
		pElement.appendChild(textDocumentFacade.createText(bulletText));
		processCharacters(wordDocument, currentTableLevel, paragraph, pElement);
		pElement.appendChild(textDocumentFacade.createText("\n"));
		parentElement.appendChild(pElement);
	}

	@Override
	protected void processSection(HWPFDocumentCore wordDocument, Section section, int s) {
		Element sectionElement = textDocumentFacade.createBlock();
		processParagraphes(wordDocument, sectionElement, section, Integer.MIN_VALUE);
		sectionElement.appendChild(textDocumentFacade.createText("\n"));
	}

	protected void processTable(HWPFDocumentCore wordDocument, Element flow, Table table) {
		final int tableRows = table.numRows();
		for (int r = 0; r < tableRows; r++) {
			TableRow tableRow = table.getRow(r);
			Element tableRowElement = textDocumentFacade.createTableRow();
			final int rowCells = tableRow.numCells();
			for (int c = 0; c < rowCells; c++) {
				TableCell tableCell = tableRow.getCell(c);
				Element tableCellElement = textDocumentFacade.createTableCell();
				if (c != 0)
					tableCellElement.appendChild(textDocumentFacade.createText("\t"));

				processCharacters(wordDocument, table.getTableLevel(), tableCell, tableCellElement);
				tableRowElement.appendChild(tableCellElement);
			}
			tableRowElement.appendChild(textDocumentFacade.createText("\n"));
			flow.appendChild(tableRowElement);
		}
	}

	@SuppressWarnings("unused")
	public void setOutputSummaryInformation(boolean outputDocumentInformation) {
		this.outputSummaryInformation = outputDocumentInformation;
	}
}

