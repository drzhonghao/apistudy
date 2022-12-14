

import java.io.IOException;
import java.io.InputStream;
import javax.xml.XMLConstants;
import org.apache.poi.ooxml.util.DocumentHelper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.PackageNamespaces;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageProperties;
import org.apache.poi.openxml4j.opc.internal.PartUnmarshaller;
import org.apache.poi.openxml4j.opc.internal.unmarshallers.UnmarshallContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public final class PackagePropertiesUnmarshaller implements PartUnmarshaller {
	protected static final String KEYWORD_CATEGORY = "category";

	protected static final String KEYWORD_CONTENT_STATUS = "contentStatus";

	protected static final String KEYWORD_CONTENT_TYPE = "contentType";

	protected static final String KEYWORD_CREATED = "created";

	protected static final String KEYWORD_CREATOR = "creator";

	protected static final String KEYWORD_DESCRIPTION = "description";

	protected static final String KEYWORD_IDENTIFIER = "identifier";

	protected static final String KEYWORD_KEYWORDS = "keywords";

	protected static final String KEYWORD_LANGUAGE = "language";

	protected static final String KEYWORD_LAST_MODIFIED_BY = "lastModifiedBy";

	protected static final String KEYWORD_LAST_PRINTED = "lastPrinted";

	protected static final String KEYWORD_MODIFIED = "modified";

	protected static final String KEYWORD_REVISION = "revision";

	protected static final String KEYWORD_SUBJECT = "subject";

	protected static final String KEYWORD_TITLE = "title";

	protected static final String KEYWORD_VERSION = "version";

	public PackagePart unmarshall(UnmarshallContext context, InputStream in) throws IOException, InvalidFormatException {
		if (in == null) {
		}
		Document xmlDoc;
		try {
			xmlDoc = DocumentHelper.readDocument(in);
			checkElementForOPCCompliance(xmlDoc.getDocumentElement());
		} catch (SAXException e) {
			throw new IOException(e.getMessage());
		}
		return null;
	}

	private String readElement(Document xmlDoc, String localName, String namespaceURI) {
		Element el = ((Element) (xmlDoc.getDocumentElement().getElementsByTagNameNS(namespaceURI, localName).item(0)));
		if (el == null) {
			return null;
		}
		return el.getTextContent();
	}

	private String loadCategory(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_CATEGORY, PackageNamespaces.CORE_PROPERTIES);
	}

	private String loadContentStatus(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_CONTENT_STATUS, PackageNamespaces.CORE_PROPERTIES);
	}

	private String loadContentType(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_CONTENT_TYPE, PackageNamespaces.CORE_PROPERTIES);
	}

	private String loadCreated(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_CREATED, PackageProperties.NAMESPACE_DCTERMS);
	}

	private String loadCreator(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_CREATOR, PackageProperties.NAMESPACE_DC);
	}

	private String loadDescription(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_DESCRIPTION, PackageProperties.NAMESPACE_DC);
	}

	private String loadIdentifier(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_IDENTIFIER, PackageProperties.NAMESPACE_DC);
	}

	private String loadKeywords(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_KEYWORDS, PackageNamespaces.CORE_PROPERTIES);
	}

	private String loadLanguage(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_LANGUAGE, PackageProperties.NAMESPACE_DC);
	}

	private String loadLastModifiedBy(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_LAST_MODIFIED_BY, PackageNamespaces.CORE_PROPERTIES);
	}

	private String loadLastPrinted(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_LAST_PRINTED, PackageNamespaces.CORE_PROPERTIES);
	}

	private String loadModified(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_MODIFIED, PackageProperties.NAMESPACE_DCTERMS);
	}

	private String loadRevision(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_REVISION, PackageNamespaces.CORE_PROPERTIES);
	}

	private String loadSubject(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_SUBJECT, PackageProperties.NAMESPACE_DC);
	}

	private String loadTitle(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_TITLE, PackageProperties.NAMESPACE_DC);
	}

	private String loadVersion(Document xmlDoc) {
		return readElement(xmlDoc, PackagePropertiesUnmarshaller.KEYWORD_VERSION, PackageNamespaces.CORE_PROPERTIES);
	}

	public void checkElementForOPCCompliance(Element el) throws InvalidFormatException {
		NamedNodeMap namedNodeMap = el.getAttributes();
		int namedNodeCount = namedNodeMap.getLength();
		for (int i = 0; i < namedNodeCount; i++) {
			Attr attr = ((Attr) (namedNodeMap.item(0)));
			if (attr.getNamespaceURI().equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
				if (attr.getValue().equals(PackageNamespaces.MARKUP_COMPATIBILITY))
					throw new InvalidFormatException("OPC Compliance error [M4.2]: A format consumer shall consider the use of the Markup Compatibility namespace to be an error.");

			}
		}
		String elName = el.getLocalName();
		if (el.getNamespaceURI().equals(PackageProperties.NAMESPACE_DCTERMS))
			if (!((elName.equals(PackagePropertiesUnmarshaller.KEYWORD_CREATED)) || (elName.equals(PackagePropertiesUnmarshaller.KEYWORD_MODIFIED))))
				throw new InvalidFormatException("OPC Compliance error [M4.3]: Producers shall not create a document element that contains refinements to the Dublin Core elements, except for the two specified in the schema: <dcterms:created> and <dcterms:modified> Consumers shall consider a document element that violates this constraint to be an error.");


		if ((el.getAttributeNodeNS(XMLConstants.XML_NS_URI, "lang")) != null)
			throw new InvalidFormatException("OPC Compliance error [M4.4]: Producers shall not create a document element that contains the xml:lang attribute. Consumers shall consider a document element that violates this constraint to be an error.");

		if (el.getNamespaceURI().equals(PackageProperties.NAMESPACE_DCTERMS)) {
			if (!((elName.equals(PackagePropertiesUnmarshaller.KEYWORD_CREATED)) || (elName.equals(PackagePropertiesUnmarshaller.KEYWORD_MODIFIED))))
				throw new InvalidFormatException(((("Namespace error : " + elName) + " shouldn't have the following naemspace -> ") + (PackageProperties.NAMESPACE_DCTERMS)));

			Attr typeAtt = el.getAttributeNodeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type");
			if (typeAtt == null)
				throw new InvalidFormatException((("The element '" + elName) + "' must have the 'xsi:type' attribute present !"));

			if (!(typeAtt.getValue().equals(((el.getPrefix()) + ":W3CDTF"))))
				throw new InvalidFormatException((((((("The element '" + elName) + "' must have the 'xsi:type' attribute with the value '") + (el.getPrefix())) + ":W3CDTF', but had '") + (typeAtt.getValue())) + "' !"));

		}
		NodeList childElements = el.getElementsByTagName("*");
		int childElementCount = childElements.getLength();
		for (int i = 0; i < childElementCount; i++)
			checkElementForOPCCompliance(((Element) (childElements.item(i))));

	}
}

