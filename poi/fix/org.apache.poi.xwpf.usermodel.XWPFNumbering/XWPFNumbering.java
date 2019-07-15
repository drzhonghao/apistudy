

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;
import org.apache.poi.xwpf.usermodel.XWPFNum;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDecimalNumber;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNumbering;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.NumberingDocument;

import static org.openxmlformats.schemas.wordprocessingml.x2006.main.NumberingDocument.Factory.parse;


public class XWPFNumbering extends POIXMLDocumentPart {
	protected List<XWPFAbstractNum> abstractNums = new ArrayList<>();

	protected List<XWPFNum> nums = new ArrayList<>();

	boolean isNew;

	private CTNumbering ctNumbering;

	public XWPFNumbering(PackagePart part) throws IOException, OpenXML4JException {
		super(part);
		isNew = true;
	}

	public XWPFNumbering() {
		abstractNums = new ArrayList<>();
		nums = new ArrayList<>();
		isNew = true;
	}

	@Override
	protected void onDocumentRead() throws IOException {
		NumberingDocument numberingDoc = null;
		InputStream is;
		is = getPackagePart().getInputStream();
		try {
			numberingDoc = parse(is, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
			ctNumbering = numberingDoc.getNumbering();
			for (CTNum ctNum : ctNumbering.getNumArray()) {
			}
			for (CTAbstractNum ctAbstractNum : ctNumbering.getAbstractNumArray()) {
			}
			isNew = false;
		} catch (XmlException e) {
			throw new POIXMLException();
		} finally {
			is.close();
		}
	}

	@Override
	protected void commit() throws IOException {
		XmlOptions xmlOptions = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		xmlOptions.setSaveSyntheticDocumentElement(new QName(CTNumbering.type.getName().getNamespaceURI(), "numbering"));
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		ctNumbering.save(out, xmlOptions);
		out.close();
	}

	public void setNumbering(CTNumbering numbering) {
		ctNumbering = numbering;
	}

	public boolean numExist(BigInteger numID) {
		for (XWPFNum num : nums) {
			if (num.getCTNum().getNumId().equals(numID))
				return true;

		}
		return false;
	}

	public BigInteger addNum(XWPFNum num) {
		ctNumbering.addNewNum();
		int pos = (ctNumbering.sizeOfNumArray()) - 1;
		ctNumbering.setNumArray(pos, num.getCTNum());
		nums.add(num);
		return num.getCTNum().getNumId();
	}

	public BigInteger addNum(BigInteger abstractNumID) {
		CTNum ctNum = this.ctNumbering.addNewNum();
		ctNum.addNewAbstractNumId();
		ctNum.getAbstractNumId().setVal(abstractNumID);
		ctNum.setNumId(BigInteger.valueOf(((nums.size()) + 1)));
		return ctNum.getNumId();
	}

	public void addNum(BigInteger abstractNumID, BigInteger numID) {
		CTNum ctNum = this.ctNumbering.addNewNum();
		ctNum.addNewAbstractNumId();
		ctNum.getAbstractNumId().setVal(abstractNumID);
		ctNum.setNumId(numID);
	}

	public XWPFNum getNum(BigInteger numID) {
		for (XWPFNum num : nums) {
			if (num.getCTNum().getNumId().equals(numID))
				return num;

		}
		return null;
	}

	public XWPFAbstractNum getAbstractNum(BigInteger abstractNumID) {
		for (XWPFAbstractNum abstractNum : abstractNums) {
			if (abstractNum.getAbstractNum().getAbstractNumId().equals(abstractNumID)) {
				return abstractNum;
			}
		}
		return null;
	}

	public BigInteger getIdOfAbstractNum(XWPFAbstractNum abstractNum) {
		CTAbstractNum copy = ((CTAbstractNum) (abstractNum.getCTAbstractNum().copy()));
		int i;
		for (i = 0; i < (abstractNums.size()); i++) {
		}
		return null;
	}

	public BigInteger addAbstractNum(XWPFAbstractNum abstractNum) {
		int pos = abstractNums.size();
		if ((abstractNum.getAbstractNum()) != null) {
			ctNumbering.addNewAbstractNum().set(abstractNum.getAbstractNum());
		}else {
			ctNumbering.addNewAbstractNum();
			abstractNum.getAbstractNum().setAbstractNumId(BigInteger.valueOf(pos));
			ctNumbering.setAbstractNumArray(pos, abstractNum.getAbstractNum());
		}
		abstractNums.add(abstractNum);
		return abstractNum.getCTAbstractNum().getAbstractNumId();
	}

	public boolean removeAbstractNum(BigInteger abstractNumID) {
		if ((abstractNumID.byteValue()) < (abstractNums.size())) {
			ctNumbering.removeAbstractNum(abstractNumID.byteValue());
			abstractNums.remove(abstractNumID.byteValue());
			return true;
		}
		return false;
	}

	public BigInteger getAbstractNumID(BigInteger numID) {
		XWPFNum num = getNum(numID);
		if (num == null)
			return null;

		if ((num.getCTNum()) == null)
			return null;

		if ((num.getCTNum().getAbstractNumId()) == null)
			return null;

		return num.getCTNum().getAbstractNumId().getVal();
	}
}

