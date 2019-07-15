

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.lucene.analysis.compound.hyphenation.Hyphen;
import org.apache.lucene.analysis.compound.hyphenation.PatternConsumer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;


public class PatternParser extends DefaultHandler {
	XMLReader parser;

	int currElement;

	PatternConsumer consumer;

	StringBuilder token;

	ArrayList<Object> exception;

	char hyphenChar;

	String errMsg;

	static final int ELEM_CLASSES = 1;

	static final int ELEM_EXCEPTIONS = 2;

	static final int ELEM_PATTERNS = 3;

	static final int ELEM_HYPHEN = 4;

	public PatternParser() {
		token = new StringBuilder();
		parser = PatternParser.createParser();
		parser.setContentHandler(this);
		parser.setErrorHandler(this);
		parser.setEntityResolver(this);
		hyphenChar = '-';
	}

	public PatternParser(PatternConsumer consumer) {
		this();
		this.consumer = consumer;
	}

	public void setConsumer(PatternConsumer consumer) {
		this.consumer = consumer;
	}

	public void parse(String filename) throws IOException {
		parse(new InputSource(filename));
	}

	public void parse(InputSource source) throws IOException {
		try {
			parser.parse(source);
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}

	static XMLReader createParser() {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
			return factory.newSAXParser().getXMLReader();
		} catch (Exception e) {
			throw new RuntimeException(("Couldn't create XMLReader: " + (e.getMessage())));
		}
	}

	protected String readToken(StringBuilder chars) {
		String word;
		boolean space = false;
		int i;
		for (i = 0; i < (chars.length()); i++) {
			if (Character.isWhitespace(chars.charAt(i))) {
				space = true;
			}else {
				break;
			}
		}
		if (space) {
			for (int countr = i; countr < (chars.length()); countr++) {
				chars.setCharAt((countr - i), chars.charAt(countr));
			}
			chars.setLength(((chars.length()) - i));
			if ((token.length()) > 0) {
				word = token.toString();
				token.setLength(0);
				return word;
			}
		}
		space = false;
		for (i = 0; i < (chars.length()); i++) {
			if (Character.isWhitespace(chars.charAt(i))) {
				space = true;
				break;
			}
		}
		token.append(chars.toString().substring(0, i));
		for (int countr = i; countr < (chars.length()); countr++) {
			chars.setCharAt((countr - i), chars.charAt(countr));
		}
		chars.setLength(((chars.length()) - i));
		if (space) {
			word = token.toString();
			token.setLength(0);
			return word;
		}
		token.append(chars);
		return null;
	}

	protected static String getPattern(String word) {
		StringBuilder pat = new StringBuilder();
		int len = word.length();
		for (int i = 0; i < len; i++) {
			if (!(Character.isDigit(word.charAt(i)))) {
				pat.append(word.charAt(i));
			}
		}
		return pat.toString();
	}

	protected ArrayList<Object> normalizeException(ArrayList<?> ex) {
		ArrayList<Object> res = new ArrayList<>();
		for (int i = 0; i < (ex.size()); i++) {
			Object item = ex.get(i);
			if (item instanceof String) {
				String str = ((String) (item));
				StringBuilder buf = new StringBuilder();
				for (int j = 0; j < (str.length()); j++) {
					char c = str.charAt(j);
					if (c != (hyphenChar)) {
						buf.append(c);
					}else {
						res.add(buf.toString());
						buf.setLength(0);
						char[] h = new char[1];
						h[0] = hyphenChar;
					}
				}
				if ((buf.length()) > 0) {
					res.add(buf.toString());
				}
			}else {
				res.add(item);
			}
		}
		return res;
	}

	protected String getExceptionWord(ArrayList<?> ex) {
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < (ex.size()); i++) {
			Object item = ex.get(i);
			if (item instanceof String) {
				res.append(((String) (item)));
			}else {
				if ((((Hyphen) (item)).noBreak) != null) {
					res.append(((Hyphen) (item)).noBreak);
				}
			}
		}
		return res.toString();
	}

	protected static String getInterletterValues(String pat) {
		StringBuilder il = new StringBuilder();
		String word = pat + "a";
		int len = word.length();
		for (int i = 0; i < len; i++) {
			char c = word.charAt(i);
			if (Character.isDigit(c)) {
				il.append(c);
				i++;
			}else {
				il.append('0');
			}
		}
		return il.toString();
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) {
		if (((systemId != null) && (systemId.matches("(?i).*\\bhyphenation.dtd\\b.*"))) || ("hyphenation-info".equals(publicId))) {
			return new InputSource(this.getClass().getResource("hyphenation.dtd").toExternalForm());
		}
		return null;
	}

	@Override
	public void startElement(String uri, String local, String raw, Attributes attrs) {
		if (local.equals("hyphen-char")) {
			String h = attrs.getValue("value");
			if ((h != null) && ((h.length()) == 1)) {
				hyphenChar = h.charAt(0);
			}
		}else
			if (local.equals("classes")) {
				currElement = PatternParser.ELEM_CLASSES;
			}else
				if (local.equals("patterns")) {
					currElement = PatternParser.ELEM_PATTERNS;
				}else
					if (local.equals("exceptions")) {
						currElement = PatternParser.ELEM_EXCEPTIONS;
						exception = new ArrayList<>();
					}else
						if (local.equals("hyphen")) {
							if ((token.length()) > 0) {
								exception.add(token.toString());
							}
							currElement = PatternParser.ELEM_HYPHEN;
						}




		token.setLength(0);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void endElement(String uri, String local, String raw) {
		if ((token.length()) > 0) {
			String word = token.toString();
			switch (currElement) {
				case PatternParser.ELEM_CLASSES :
					consumer.addClass(word);
					break;
				case PatternParser.ELEM_EXCEPTIONS :
					exception.add(word);
					exception = normalizeException(exception);
					consumer.addException(getExceptionWord(exception), ((ArrayList) (exception.clone())));
					break;
				case PatternParser.ELEM_PATTERNS :
					consumer.addPattern(PatternParser.getPattern(word), PatternParser.getInterletterValues(word));
					break;
				case PatternParser.ELEM_HYPHEN :
					break;
			}
			if ((currElement) != (PatternParser.ELEM_HYPHEN)) {
				token.setLength(0);
			}
		}
		if ((currElement) == (PatternParser.ELEM_HYPHEN)) {
			currElement = PatternParser.ELEM_EXCEPTIONS;
		}else {
			currElement = 0;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void characters(char[] ch, int start, int length) {
		StringBuilder chars = new StringBuilder(length);
		chars.append(ch, start, length);
		String word = readToken(chars);
		while (word != null) {
			switch (currElement) {
				case PatternParser.ELEM_CLASSES :
					consumer.addClass(word);
					break;
				case PatternParser.ELEM_EXCEPTIONS :
					exception.add(word);
					exception = normalizeException(exception);
					consumer.addException(getExceptionWord(exception), ((ArrayList) (exception.clone())));
					exception.clear();
					break;
				case PatternParser.ELEM_PATTERNS :
					consumer.addPattern(PatternParser.getPattern(word), PatternParser.getInterletterValues(word));
					break;
			}
			word = readToken(chars);
		} 
	}

	private String getLocationString(SAXParseException ex) {
		StringBuilder str = new StringBuilder();
		String systemId = ex.getSystemId();
		if (systemId != null) {
			int index = systemId.lastIndexOf('/');
			if (index != (-1)) {
				systemId = systemId.substring((index + 1));
			}
			str.append(systemId);
		}
		str.append(':');
		str.append(ex.getLineNumber());
		str.append(':');
		str.append(ex.getColumnNumber());
		return str.toString();
	}
}

