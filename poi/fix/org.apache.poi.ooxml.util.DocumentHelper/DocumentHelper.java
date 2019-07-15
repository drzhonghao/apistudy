

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.events.Namespace;
import org.apache.poi.ooxml.util.POIXMLConstants;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


public final class DocumentHelper {
	private static POILogger logger = POILogFactory.getLogger(DocumentHelper.class);

	private static long lastLog;

	private DocumentHelper() {
	}

	private static class DocHelperErrorHandler implements ErrorHandler {
		public void warning(SAXParseException exception) throws SAXException {
			printError(POILogger.WARN, exception);
		}

		public void error(SAXParseException exception) throws SAXException {
			printError(POILogger.ERROR, exception);
		}

		public void fatalError(SAXParseException exception) throws SAXException {
			printError(POILogger.FATAL, exception);
			throw exception;
		}

		private void printError(int type, SAXParseException ex) {
			StringBuilder sb = new StringBuilder();
			String systemId = ex.getSystemId();
			if (systemId != null) {
				int index = systemId.lastIndexOf('/');
				if (index != (-1))
					systemId = systemId.substring((index + 1));

				sb.append(systemId);
			}
			sb.append(':');
			sb.append(ex.getLineNumber());
			sb.append(':');
			sb.append(ex.getColumnNumber());
			sb.append(": ");
			sb.append(ex.getMessage());
			DocumentHelper.logger.log(type, sb.toString(), ex);
		}
	}

	public static synchronized DocumentBuilder newDocumentBuilder() {
		try {
			DocumentBuilder documentBuilder = DocumentHelper.documentBuilderFactory.newDocumentBuilder();
			documentBuilder.setErrorHandler(new DocumentHelper.DocHelperErrorHandler());
			return documentBuilder;
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException("cannot create a DocumentBuilder", e);
		}
	}

	static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

	static {
		DocumentHelper.documentBuilderFactory.setNamespaceAware(true);
		DocumentHelper.documentBuilderFactory.setValidating(false);
		DocumentHelper.documentBuilderFactory.setExpandEntityReferences(false);
		DocumentHelper.trySetFeature(DocumentHelper.documentBuilderFactory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
		DocumentHelper.trySetFeature(DocumentHelper.documentBuilderFactory, POIXMLConstants.FEATURE_LOAD_DTD_GRAMMAR, false);
		DocumentHelper.trySetFeature(DocumentHelper.documentBuilderFactory, POIXMLConstants.FEATURE_LOAD_EXTERNAL_DTD, false);
		DocumentHelper.trySetXercesSecurityManager(DocumentHelper.documentBuilderFactory);
	}

	private static void trySetFeature(DocumentBuilderFactory dbf, String feature, boolean enabled) {
		try {
			dbf.setFeature(feature, enabled);
		} catch (Exception e) {
			DocumentHelper.logger.log(POILogger.WARN, "DocumentBuilderFactory Feature unsupported", feature, e);
		} catch (AbstractMethodError ame) {
			DocumentHelper.logger.log(POILogger.WARN, "Cannot set DocumentBuilderFactory feature because outdated XML parser in classpath", feature, ame);
		}
	}

	private static void trySetXercesSecurityManager(DocumentBuilderFactory dbf) {
		for (String securityManagerClassName : new String[]{ "org.apache.xerces.util.SecurityManager" }) {
			try {
				Object mgr = Class.forName(securityManagerClassName).newInstance();
				Method setLimit = mgr.getClass().getMethod("setEntityExpansionLimit", Integer.TYPE);
				setLimit.invoke(mgr, 1);
				dbf.setAttribute(POIXMLConstants.PROPERTY_SECURITY_MANAGER, mgr);
				return;
			} catch (ClassNotFoundException e) {
			} catch (Throwable e) {
				if ((System.currentTimeMillis()) > ((DocumentHelper.lastLog) + (TimeUnit.MINUTES.toMillis(5)))) {
					DocumentHelper.logger.log(POILogger.WARN, "DocumentBuilderFactory Security Manager could not be setup [log suppressed for 5 minutes]", e);
					DocumentHelper.lastLog = System.currentTimeMillis();
				}
			}
		}
		try {
			dbf.setAttribute(POIXMLConstants.PROPERTY_ENTITY_EXPANSION_LIMIT, 1);
		} catch (Throwable e) {
			if ((System.currentTimeMillis()) > ((DocumentHelper.lastLog) + (TimeUnit.MINUTES.toMillis(5)))) {
				DocumentHelper.logger.log(POILogger.WARN, "DocumentBuilderFactory Entity Expansion Limit could not be setup [log suppressed for 5 minutes]", e);
				DocumentHelper.lastLog = System.currentTimeMillis();
			}
		}
	}

	public static Document readDocument(InputStream inp) throws IOException, SAXException {
		return DocumentHelper.newDocumentBuilder().parse(inp);
	}

	public static Document readDocument(InputSource inp) throws IOException, SAXException {
		return DocumentHelper.newDocumentBuilder().parse(inp);
	}

	private static final DocumentBuilder documentBuilderSingleton = DocumentHelper.newDocumentBuilder();

	public static synchronized Document createDocument() {
		return DocumentHelper.documentBuilderSingleton.newDocument();
	}

	public static void addNamespaceDeclaration(Element element, String namespacePrefix, String namespaceURI) {
		element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, (((XMLConstants.XMLNS_ATTRIBUTE) + ':') + namespacePrefix), namespaceURI);
	}

	public static void addNamespaceDeclaration(Element element, Namespace namespace) {
		DocumentHelper.addNamespaceDeclaration(element, namespace.getPrefix(), namespace.getNamespaceURI());
	}
}

