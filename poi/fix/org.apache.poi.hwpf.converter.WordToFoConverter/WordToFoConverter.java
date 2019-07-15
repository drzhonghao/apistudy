

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
import org.apache.poi.hwpf.converter.FoDocumentFacade;
import org.apache.poi.hwpf.converter.FontReplacer;
import org.apache.poi.hwpf.converter.WordToFoUtils;
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
import org.apache.poi.util.Beta;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.XMLHelper;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


@Beta
public class WordToFoConverter extends AbstractWordConverter {
	private static final POILogger logger = POILogFactory.getLogger(WordToFoConverter.class);

	public static void main(String[] args) throws Exception {
		if ((args.length) < 2) {
			System.err.println("Usage: WordToFoConverter <inputFile.doc> <saveTo.fo>");
			return;
		}
		System.out.println(("Converting " + (args[0])));
		System.out.println(("Saving output to " + (args[1])));
		Document doc = WordToFoConverter.process(new File(args[0]));
		DOMSource domSource = new DOMSource(doc);
		StreamResult streamResult = new StreamResult(new File(args[1]));
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer serializer = tf.newTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		serializer.transform(domSource, streamResult);
	}

	static Document process(File docFile) throws Exception {
		final DocumentBuilder docBuild = XMLHelper.getDocumentBuilderFactory().newDocumentBuilder();
		try (final HWPFDocumentCore hwpfDocument = WordToFoUtils.loadDoc(docFile)) {
			WordToFoConverter wordToFoConverter = new WordToFoConverter(docBuild.newDocument());
			wordToFoConverter.processDocument(hwpfDocument);
			return wordToFoConverter.getDocument();
		}
	}

	private List<Element> endnotes = new ArrayList<>(0);

	protected final FoDocumentFacade foDocumentFacade;

	private AtomicInteger internalLinkCounter = new AtomicInteger(0);

	private boolean outputCharactersLanguage;

	private Set<String> usedIds = new LinkedHashSet<>();

	public WordToFoConverter(Document document) {
		this.foDocumentFacade = new FoDocumentFacade(document);
	}

	public WordToFoConverter(FoDocumentFacade foDocumentFacade) {
		this.foDocumentFacade = foDocumentFacade;
	}

	protected Element createNoteInline(String noteIndexText) {
		Element inline = foDocumentFacade.createInline();
		inline.setTextContent(noteIndexText);
		inline.setAttribute("baseline-shift", "super");
		inline.setAttribute("font-size", "smaller");
		return inline;
	}

	protected String createPageMaster(Section section, String type, int sectionIndex) {
		float height = (section.getPageHeight()) / (WordToFoUtils.TWIPS_PER_INCH);
		float width = (section.getPageWidth()) / (WordToFoUtils.TWIPS_PER_INCH);
		float leftMargin = (section.getMarginLeft()) / (WordToFoUtils.TWIPS_PER_INCH);
		float rightMargin = (section.getMarginRight()) / (WordToFoUtils.TWIPS_PER_INCH);
		float topMargin = (section.getMarginTop()) / (WordToFoUtils.TWIPS_PER_INCH);
		float bottomMargin = (section.getMarginBottom()) / (WordToFoUtils.TWIPS_PER_INCH);
		String pageMasterName = (type + "-page") + sectionIndex;
		Element pageMaster = foDocumentFacade.addSimplePageMaster(pageMasterName);
		pageMaster.setAttribute("page-height", (height + "in"));
		pageMaster.setAttribute("page-width", (width + "in"));
		Element regionBody = foDocumentFacade.addRegionBody(pageMaster);
		regionBody.setAttribute("margin", (((((((topMargin + "in ") + rightMargin) + "in ") + bottomMargin) + "in ") + leftMargin) + "in"));
		if ((section.getNumColumns()) > 1) {
			regionBody.setAttribute("column-count", ("" + (section.getNumColumns())));
			if (section.isColumnsEvenlySpaced()) {
				float distance = (section.getDistanceBetweenColumns()) / (WordToFoUtils.TWIPS_PER_INCH);
				regionBody.setAttribute("column-gap", (distance + "in"));
			}else {
				regionBody.setAttribute("column-gap", "0.25in");
			}
		}
		return pageMasterName;
	}

	public Document getDocument() {
		return foDocumentFacade.getDocument();
	}

	public boolean isOutputCharactersLanguage() {
		return outputCharactersLanguage;
	}

	@Override
	protected void outputCharacters(Element block, CharacterRun characterRun, String text) {
		Element inline = foDocumentFacade.createInline();
		FontReplacer.Triplet triplet = getCharacterRunTriplet(characterRun);
		WordToFoUtils.setBold(inline, triplet.bold);
		WordToFoUtils.setItalic(inline, triplet.italic);
		WordToFoUtils.setFontSize(inline, ((characterRun.getFontSize()) / 2));
		WordToFoUtils.setCharactersProperties(characterRun, inline);
		if (isOutputCharactersLanguage())
			WordToFoUtils.setLanguage(characterRun, inline);

		block.appendChild(inline);
		Text textNode = foDocumentFacade.createText(text);
		inline.appendChild(textNode);
	}

	@Override
	protected void processBookmarks(HWPFDocumentCore wordDocument, Element currentBlock, Range range, int currentTableLevel, List<Bookmark> rangeBookmarks) {
		Element parent = currentBlock;
		for (Bookmark bookmark : rangeBookmarks) {
			Element bookmarkElement = foDocumentFacade.createInline();
			final String idName = "bookmark_" + (bookmark.getName());
			if (setId(bookmarkElement, idName)) {
				parent.appendChild(bookmarkElement);
				parent = bookmarkElement;
			}
		}
		if (range != null)
			processCharacters(wordDocument, currentTableLevel, range, parent);

	}

	@Override
	protected void processDocumentInformation(SummaryInformation summaryInformation) {
	}

	@Override
	protected void processDrawnObject(HWPFDocument doc, CharacterRun characterRun, OfficeDrawing officeDrawing, String path, Element block) {
		final Element externalGraphic = foDocumentFacade.createExternalGraphic(path);
		block.appendChild(externalGraphic);
	}

	@Override
	protected void processEndnoteAutonumbered(HWPFDocument wordDocument, int noteIndex, Element block, Range endnoteTextRange) {
		final String textIndex = String.valueOf(internalLinkCounter.incrementAndGet());
		final String forwardLinkName = "endnote_" + textIndex;
		final String backwardLinkName = "endnote_back_" + textIndex;
		Element forwardLink = foDocumentFacade.createBasicLinkInternal(forwardLinkName);
		forwardLink.appendChild(createNoteInline(textIndex));
		setId(forwardLink, backwardLinkName);
		block.appendChild(forwardLink);
		Element endnote = foDocumentFacade.createBlock();
		Element backwardLink = foDocumentFacade.createBasicLinkInternal(backwardLinkName);
		backwardLink.appendChild(createNoteInline((textIndex + " ")));
		setId(backwardLink, forwardLinkName);
		endnote.appendChild(backwardLink);
		processCharacters(wordDocument, Integer.MIN_VALUE, endnoteTextRange, endnote);
		this.endnotes.add(endnote);
	}

	@Override
	protected void processFootnoteAutonumbered(HWPFDocument wordDocument, int noteIndex, Element block, Range footnoteTextRange) {
		final String textIndex = String.valueOf(internalLinkCounter.incrementAndGet());
		final String forwardLinkName = "footnote_" + textIndex;
		final String backwardLinkName = "footnote_back_" + textIndex;
		Element footNote = foDocumentFacade.createFootnote();
		block.appendChild(footNote);
		Element inline = foDocumentFacade.createInline();
		Element forwardLink = foDocumentFacade.createBasicLinkInternal(forwardLinkName);
		forwardLink.appendChild(createNoteInline(textIndex));
		setId(forwardLink, backwardLinkName);
		inline.appendChild(forwardLink);
		footNote.appendChild(inline);
		Element footnoteBody = foDocumentFacade.createFootnoteBody();
		Element footnoteBlock = foDocumentFacade.createBlock();
		Element backwardLink = foDocumentFacade.createBasicLinkInternal(backwardLinkName);
		backwardLink.appendChild(createNoteInline((textIndex + " ")));
		setId(backwardLink, forwardLinkName);
		footnoteBlock.appendChild(backwardLink);
		footnoteBody.appendChild(footnoteBlock);
		footNote.appendChild(footnoteBody);
		processCharacters(wordDocument, Integer.MIN_VALUE, footnoteTextRange, footnoteBlock);
	}

	protected void processHyperlink(HWPFDocumentCore wordDocument, Element currentBlock, Range textRange, int currentTableLevel, String hyperlink) {
		Element basicLink = foDocumentFacade.createBasicLinkExternal(hyperlink);
		currentBlock.appendChild(basicLink);
		if (textRange != null)
			processCharacters(wordDocument, currentTableLevel, textRange, basicLink);

	}

	protected void processImage(Element currentBlock, boolean inlined, Picture picture, String url) {
		final Element externalGraphic = foDocumentFacade.createExternalGraphic(url);
		WordToFoUtils.setPictureProperties(picture, externalGraphic);
		currentBlock.appendChild(externalGraphic);
	}

	@Override
	protected void processImageWithoutPicturesManager(Element currentBlock, boolean inlined, Picture picture) {
		currentBlock.appendChild(foDocumentFacade.getDocument().createComment((("Image link to '" + (picture.suggestFullFileName())) + "' can be here")));
	}

	@Override
	protected void processLineBreak(Element block, CharacterRun characterRun) {
		block.appendChild(foDocumentFacade.createBlock());
	}

	@Override
	protected void processPageBreak(HWPFDocumentCore wordDocument, Element flow) {
		Element block = null;
		NodeList childNodes = flow.getChildNodes();
		if ((childNodes.getLength()) > 0) {
			Node lastChild = childNodes.item(((childNodes.getLength()) - 1));
			if (lastChild instanceof Element) {
				Element lastElement = ((Element) (lastChild));
				if (!(lastElement.hasAttribute("break-after"))) {
					block = lastElement;
				}
			}
		}
		if (block == null) {
			block = foDocumentFacade.createBlock();
			flow.appendChild(block);
		}
		block.setAttribute("break-after", "page");
	}

	protected void processPageref(HWPFDocumentCore hwpfDocument, Element currentBlock, Range textRange, int currentTableLevel, String pageref) {
		Element basicLink = foDocumentFacade.createBasicLinkInternal(("bookmark_" + pageref));
		currentBlock.appendChild(basicLink);
		if (textRange != null)
			processCharacters(hwpfDocument, currentTableLevel, textRange, basicLink);

	}

	protected void processParagraph(HWPFDocumentCore hwpfDocument, Element parentFopElement, int currentTableLevel, Paragraph paragraph, String bulletText) {
		final Element block = foDocumentFacade.createBlock();
		parentFopElement.appendChild(block);
		WordToFoUtils.setParagraphProperties(paragraph, block);
		final int charRuns = paragraph.numCharacterRuns();
		if (charRuns == 0) {
			return;
		}
		boolean haveAnyText = false;
		haveAnyText = processCharacters(hwpfDocument, currentTableLevel, paragraph, block);
		if (!haveAnyText) {
			Element leader = foDocumentFacade.createLeader();
			block.appendChild(leader);
		}
	}

	protected void processSection(HWPFDocumentCore wordDocument, Section section, int sectionCounter) {
		String regularPage = createPageMaster(section, "page", sectionCounter);
		Element pageSequence = foDocumentFacade.addPageSequence(regularPage);
		Element flow = foDocumentFacade.addFlowToPageSequence(pageSequence, "xsl-region-body");
		processParagraphes(wordDocument, flow, section, Integer.MIN_VALUE);
		if (((endnotes) != null) && (!(endnotes.isEmpty()))) {
			for (Element endnote : endnotes)
				flow.appendChild(endnote);

			endnotes.clear();
		}
	}

	protected void processTable(HWPFDocumentCore wordDocument, Element flow, Table table) {
		Element tableHeader = foDocumentFacade.createTableHeader();
		Element tableBody = foDocumentFacade.createTableBody();
		final int tableRows = table.numRows();
		int maxColumns = Integer.MIN_VALUE;
		for (int r = 0; r < tableRows; r++) {
			maxColumns = Math.max(maxColumns, table.getRow(r).numCells());
		}
		for (int r = 0; r < tableRows; r++) {
			TableRow tableRow = table.getRow(r);
			Element tableRowElement = foDocumentFacade.createTableRow();
			WordToFoUtils.setTableRowProperties(tableRow, tableRowElement);
			int currentEdgeIndex = 0;
			final int rowCells = tableRow.numCells();
			for (int c = 0; c < rowCells; c++) {
				TableCell tableCell = tableRow.getCell(c);
				if ((tableCell.isVerticallyMerged()) && (!(tableCell.isFirstVerticallyMerged()))) {
					continue;
				}
				Element tableCellElement = foDocumentFacade.createTableCell();
				WordToFoUtils.setTableCellProperties(tableRow, tableCell, tableCellElement, (r == 0), (r == (tableRows - 1)), (c == 0), (c == (rowCells - 1)));
				processParagraphes(wordDocument, tableCellElement, tableCell, table.getTableLevel());
				if (!(tableCellElement.hasChildNodes())) {
					tableCellElement.appendChild(foDocumentFacade.createBlock());
				}
				tableRowElement.appendChild(tableCellElement);
			}
			if (tableRowElement.hasChildNodes()) {
				if (tableRow.isTableHeader()) {
					tableHeader.appendChild(tableRowElement);
				}else {
					tableBody.appendChild(tableRowElement);
				}
			}
		}
		final Element tableElement = foDocumentFacade.createTable();
		tableElement.setAttribute("table-layout", "fixed");
		if (tableHeader.hasChildNodes()) {
			tableElement.appendChild(tableHeader);
		}
		if (tableBody.hasChildNodes()) {
			tableElement.appendChild(tableBody);
			flow.appendChild(tableElement);
		}else {
			WordToFoConverter.logger.log(POILogger.WARN, ((("Table without body starting on offset " + (table.getStartOffset())) + " -- ") + (table.getEndOffset())));
		}
	}

	protected boolean setId(Element element, final String id) {
		if (usedIds.contains(id)) {
			WordToFoConverter.logger.log(POILogger.WARN, "Tried to create element with same ID '", id, "'. Skipped");
			return false;
		}
		element.setAttribute("id", id);
		usedIds.add(id);
		return true;
	}

	public void setOutputCharactersLanguage(boolean outputCharactersLanguage) {
		this.outputCharactersLanguage = outputCharactersLanguage;
	}
}

