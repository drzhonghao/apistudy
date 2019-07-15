

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ISDTContent;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFSDT;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHdrFtr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;

import static org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHdrFtr.Factory.newInstance;


public abstract class XWPFHeaderFooter extends POIXMLDocumentPart implements IBody {
	List<XWPFParagraph> paragraphs = new ArrayList<>();

	List<XWPFTable> tables = new ArrayList<>();

	List<XWPFPictureData> pictures = new ArrayList<>();

	List<IBodyElement> bodyElements = new ArrayList<>();

	CTHdrFtr headerFooter;

	XWPFDocument document;

	XWPFHeaderFooter(XWPFDocument doc, CTHdrFtr hdrFtr) {
		if (doc == null) {
			throw new NullPointerException();
		}
		document = doc;
		headerFooter = hdrFtr;
		readHdrFtr();
	}

	protected XWPFHeaderFooter() {
		headerFooter = newInstance();
		readHdrFtr();
	}

	public XWPFHeaderFooter(POIXMLDocumentPart parent, PackagePart part) throws IOException {
		super(parent, part);
		this.document = ((XWPFDocument) (getParent()));
		if ((this.document) == null) {
			throw new NullPointerException();
		}
	}

	@Override
	protected void onDocumentRead() throws IOException {
		for (POIXMLDocumentPart poixmlDocumentPart : getRelations()) {
			if (poixmlDocumentPart instanceof XWPFPictureData) {
				XWPFPictureData xwpfPicData = ((XWPFPictureData) (poixmlDocumentPart));
				pictures.add(xwpfPicData);
			}
		}
	}

	@org.apache.poi.util.Internal
	public CTHdrFtr _getHdrFtr() {
		return headerFooter;
	}

	public List<IBodyElement> getBodyElements() {
		return Collections.unmodifiableList(bodyElements);
	}

	public List<XWPFParagraph> getParagraphs() {
		return Collections.unmodifiableList(paragraphs);
	}

	public List<XWPFTable> getTables() throws ArrayIndexOutOfBoundsException {
		return Collections.unmodifiableList(tables);
	}

	public String getText() {
		StringBuilder t = new StringBuilder(64);
		for (int i = 0; i < (paragraphs.size()); i++) {
			if (!(paragraphs.get(i).isEmpty())) {
				String text = paragraphs.get(i).getText();
				if ((text != null) && ((text.length()) > 0)) {
					t.append(text);
					t.append('\n');
				}
			}
		}
		for (int i = 0; i < (tables.size()); i++) {
			String text = tables.get(i).getText();
			if ((text != null) && ((text.length()) > 0)) {
				t.append(text);
				t.append('\n');
			}
		}
		for (IBodyElement bodyElement : getBodyElements()) {
			if (bodyElement instanceof XWPFSDT) {
				t.append(((((XWPFSDT) (bodyElement)).getContent().getText()) + '\n'));
			}
		}
		return t.toString();
	}

	public void setHeaderFooter(CTHdrFtr headerFooter) {
		this.headerFooter = headerFooter;
		readHdrFtr();
	}

	public XWPFTable getTable(CTTbl ctTable) {
		for (XWPFTable table : tables) {
			if (table == null)
				return null;

			if (table.getCTTbl().equals(ctTable))
				return table;

		}
		return null;
	}

	public XWPFParagraph getParagraph(CTP p) {
		for (XWPFParagraph paragraph : paragraphs) {
			if (paragraph.getCTP().equals(p))
				return paragraph;

		}
		return null;
	}

	public XWPFParagraph getParagraphArray(int pos) {
		if ((pos >= 0) && (pos < (paragraphs.size()))) {
			return paragraphs.get(pos);
		}
		return null;
	}

	public List<XWPFParagraph> getListParagraph() {
		return paragraphs;
	}

	public List<XWPFPictureData> getAllPictures() {
		return Collections.unmodifiableList(pictures);
	}

	public List<XWPFPictureData> getAllPackagePictures() {
		return document.getAllPackagePictures();
	}

	public String addPictureData(byte[] pictureData, int format) throws InvalidFormatException {
		return null;
	}

	public String addPictureData(InputStream is, int format) throws IOException, InvalidFormatException {
		byte[] data = IOUtils.toByteArray(is);
		return addPictureData(data, format);
	}

	public XWPFPictureData getPictureDataByID(String blipID) {
		POIXMLDocumentPart relatedPart = getRelationById(blipID);
		if ((relatedPart != null) && (relatedPart instanceof XWPFPictureData)) {
			return ((XWPFPictureData) (relatedPart));
		}
		return null;
	}

	public XWPFParagraph createParagraph() {
		XWPFParagraph paragraph = new XWPFParagraph(headerFooter.addNewP(), this);
		paragraphs.add(paragraph);
		bodyElements.add(paragraph);
		return paragraph;
	}

	public XWPFTable createTable(int rows, int cols) {
		XWPFTable table = new XWPFTable(headerFooter.addNewTbl(), this, rows, cols);
		tables.add(table);
		bodyElements.add(table);
		return table;
	}

	public void removeParagraph(XWPFParagraph paragraph) {
		if (paragraphs.contains(paragraph)) {
			CTP ctP = paragraph.getCTP();
			XmlCursor c = ctP.newCursor();
			c.removeXml();
			c.dispose();
			paragraphs.remove(paragraph);
			bodyElements.remove(paragraph);
		}
	}

	public void removeTable(XWPFTable table) {
		if (tables.contains(table)) {
			CTTbl ctTbl = table.getCTTbl();
			XmlCursor c = ctTbl.newCursor();
			c.removeXml();
			c.dispose();
			tables.remove(table);
			bodyElements.remove(table);
		}
	}

	public void clearHeaderFooter() {
		XmlCursor c = headerFooter.newCursor();
		c.removeXmlContents();
		c.dispose();
		paragraphs.clear();
		tables.clear();
		bodyElements.clear();
	}

	public XWPFParagraph insertNewParagraph(XmlCursor cursor) {
		if (isCursorInHdrF(cursor)) {
			String uri = CTP.type.getName().getNamespaceURI();
			String localPart = "p";
			cursor.beginElement(localPart, uri);
			cursor.toParent();
			CTP p = ((CTP) (cursor.getObject()));
			XWPFParagraph newP = new XWPFParagraph(p, this);
			XmlObject o = null;
			while ((!(o instanceof CTP)) && (cursor.toPrevSibling())) {
				o = cursor.getObject();
			} 
			if ((!(o instanceof CTP)) || (o == p)) {
				paragraphs.add(0, newP);
			}else {
				int pos = (paragraphs.indexOf(getParagraph(((CTP) (o))))) + 1;
				paragraphs.add(pos, newP);
			}
			int i = 0;
			XmlCursor p2 = p.newCursor();
			cursor.toCursor(p2);
			p2.dispose();
			while (cursor.toPrevSibling()) {
				o = cursor.getObject();
				if ((o instanceof CTP) || (o instanceof CTTbl))
					i++;

			} 
			bodyElements.add(i, newP);
			p2 = p.newCursor();
			cursor.toCursor(p2);
			cursor.toEndToken();
			p2.dispose();
			return newP;
		}
		return null;
	}

	public XWPFTable insertNewTbl(final XmlCursor cursor) {
		if (isCursorInHdrF(cursor)) {
			String uri = CTTbl.type.getName().getNamespaceURI();
			String localPart = "tbl";
			cursor.beginElement(localPart, uri);
			cursor.toParent();
			CTTbl t = ((CTTbl) (cursor.getObject()));
			XWPFTable newT = new XWPFTable(t, this);
			cursor.removeXmlContents();
			XmlObject o = null;
			while ((!(o instanceof CTTbl)) && (cursor.toPrevSibling())) {
				o = cursor.getObject();
			} 
			if (!(o instanceof CTTbl)) {
				tables.add(0, newT);
			}else {
				int pos = (tables.indexOf(getTable(((CTTbl) (o))))) + 1;
				tables.add(pos, newT);
			}
			int i = 0;
			XmlCursor cursor2 = t.newCursor();
			while (cursor2.toPrevSibling()) {
				o = cursor2.getObject();
				if ((o instanceof CTP) || (o instanceof CTTbl)) {
					i++;
				}
			} 
			cursor2.dispose();
			bodyElements.add(i, newT);
			cursor2 = t.newCursor();
			cursor.toCursor(cursor2);
			cursor.toEndToken();
			cursor2.dispose();
			return newT;
		}
		return null;
	}

	private boolean isCursorInHdrF(XmlCursor cursor) {
		XmlCursor verify = cursor.newCursor();
		verify.toParent();
		boolean result = (verify.getObject()) == (this.headerFooter);
		verify.dispose();
		return result;
	}

	public POIXMLDocumentPart getOwner() {
		return this;
	}

	public XWPFTable getTableArray(int pos) {
		if ((pos >= 0) && (pos < (tables.size()))) {
			return tables.get(pos);
		}
		return null;
	}

	public void insertTable(int pos, XWPFTable table) {
		bodyElements.add(pos, table);
		int i = 0;
		for (CTTbl tbl : headerFooter.getTblArray()) {
			if (tbl == (table.getCTTbl())) {
				break;
			}
			i++;
		}
		tables.add(i, table);
	}

	public void readHdrFtr() {
		bodyElements = new ArrayList<>();
		paragraphs = new ArrayList<>();
		tables = new ArrayList<>();
		XmlCursor cursor = headerFooter.newCursor();
		cursor.selectPath("./*");
		while (cursor.toNextSelection()) {
			XmlObject o = cursor.getObject();
			if (o instanceof CTP) {
				XWPFParagraph p = new XWPFParagraph(((CTP) (o)), this);
				paragraphs.add(p);
				bodyElements.add(p);
			}
			if (o instanceof CTTbl) {
				XWPFTable t = new XWPFTable(((CTTbl) (o)), this);
				tables.add(t);
				bodyElements.add(t);
			}
		} 
		cursor.dispose();
	}

	public XWPFTableCell getTableCell(CTTc cell) {
		XmlCursor cursor = cell.newCursor();
		cursor.toParent();
		XmlObject o = cursor.getObject();
		if (!(o instanceof CTRow)) {
			cursor.dispose();
			return null;
		}
		CTRow row = ((CTRow) (o));
		cursor.toParent();
		o = cursor.getObject();
		cursor.dispose();
		if (!(o instanceof CTTbl)) {
			return null;
		}
		CTTbl tbl = ((CTTbl) (o));
		XWPFTable table = getTable(tbl);
		if (table == null) {
			return null;
		}
		XWPFTableRow tableRow = table.getRow(row);
		return tableRow.getTableCell(cell);
	}

	public XWPFDocument getXWPFDocument() {
		if ((document) != null) {
			return document;
		}else {
			return ((XWPFDocument) (getParent()));
		}
	}

	public void setXWPFDocument(XWPFDocument doc) {
		document = doc;
	}

	public POIXMLDocumentPart getPart() {
		return this;
	}

	@Override
	protected void prepareForCommit() {
		if ((bodyElements.size()) == 0) {
			createParagraph();
		}
		for (XWPFTable tbl : tables) {
		}
		super.prepareForCommit();
	}
}

