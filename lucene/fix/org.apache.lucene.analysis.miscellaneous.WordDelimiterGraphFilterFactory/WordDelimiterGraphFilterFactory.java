

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterIterator;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;


public class WordDelimiterGraphFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
	public static final String PROTECTED_TOKENS = "protected";

	public static final String TYPES = "types";

	private final String wordFiles;

	private final String types;

	private final int flags;

	byte[] typeTable = null;

	private CharArraySet protectedWords = null;

	public WordDelimiterGraphFilterFactory(Map<String, String> args) {
		super(args);
		int flags = 0;
		if ((getInt(args, "generateWordParts", 1)) != 0) {
			flags |= WordDelimiterGraphFilter.GENERATE_WORD_PARTS;
		}
		if ((getInt(args, "generateNumberParts", 1)) != 0) {
			flags |= WordDelimiterGraphFilter.GENERATE_NUMBER_PARTS;
		}
		if ((getInt(args, "catenateWords", 0)) != 0) {
			flags |= WordDelimiterGraphFilter.CATENATE_WORDS;
		}
		if ((getInt(args, "catenateNumbers", 0)) != 0) {
			flags |= WordDelimiterGraphFilter.CATENATE_NUMBERS;
		}
		if ((getInt(args, "catenateAll", 0)) != 0) {
			flags |= WordDelimiterGraphFilter.CATENATE_ALL;
		}
		if ((getInt(args, "splitOnCaseChange", 1)) != 0) {
			flags |= WordDelimiterGraphFilter.SPLIT_ON_CASE_CHANGE;
		}
		if ((getInt(args, "splitOnNumerics", 1)) != 0) {
			flags |= WordDelimiterGraphFilter.SPLIT_ON_NUMERICS;
		}
		if ((getInt(args, "preserveOriginal", 0)) != 0) {
			flags |= WordDelimiterGraphFilter.PRESERVE_ORIGINAL;
		}
		if ((getInt(args, "stemEnglishPossessive", 1)) != 0) {
			flags |= WordDelimiterGraphFilter.STEM_ENGLISH_POSSESSIVE;
		}
		wordFiles = get(args, WordDelimiterGraphFilterFactory.PROTECTED_TOKENS);
		types = get(args, WordDelimiterGraphFilterFactory.TYPES);
		this.flags = flags;
		if (!(args.isEmpty())) {
			throw new IllegalArgumentException(("Unknown parameters: " + args));
		}
	}

	@Override
	public void inform(ResourceLoader loader) throws IOException {
		if ((wordFiles) != null) {
			protectedWords = getWordSet(loader, wordFiles, false);
		}
		if ((types) != null) {
			List<String> files = splitFileNames(types);
			List<String> wlist = new ArrayList<>();
			for (String file : files) {
				List<String> lines = getLines(loader, file.trim());
				wlist.addAll(lines);
			}
			typeTable = parseTypes(wlist);
		}
	}

	@Override
	public TokenFilter create(TokenStream input) {
		return new WordDelimiterGraphFilter(input, ((typeTable) == null ? WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE : typeTable), flags, protectedWords);
	}

	private static Pattern typePattern = Pattern.compile("(.*)\\s*=>\\s*(.*)\\s*$");

	private byte[] parseTypes(List<String> rules) {
		SortedMap<Character, Byte> typeMap = new TreeMap<>();
		for (String rule : rules) {
			Matcher m = WordDelimiterGraphFilterFactory.typePattern.matcher(rule);
			if (!(m.find()))
				throw new IllegalArgumentException((("Invalid Mapping Rule : [" + rule) + "]"));

			String lhs = parseString(m.group(1).trim());
			Byte rhs = parseType(m.group(2).trim());
			if ((lhs.length()) != 1)
				throw new IllegalArgumentException((("Invalid Mapping Rule : [" + rule) + "]. Only a single character is allowed."));

			if (rhs == null)
				throw new IllegalArgumentException((("Invalid Mapping Rule : [" + rule) + "]. Illegal type."));

			typeMap.put(lhs.charAt(0), rhs);
		}
		byte[] types = new byte[Math.max(((typeMap.lastKey()) + 1), WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE.length)];
		for (int i = 0; i < (types.length); i++)
			types[i] = WordDelimiterIterator.getType(i);

		for (Map.Entry<Character, Byte> mapping : typeMap.entrySet())
			types[mapping.getKey()] = mapping.getValue();

		return types;
	}

	private Byte parseType(String s) {
		if (s.equals("LOWER")) {
		}else
			if (s.equals("UPPER")) {
			}else
				if (s.equals("ALPHA"))
					return WordDelimiterIterator.ALPHA;
				else
					if (s.equals("DIGIT")) {
					}else
						if (s.equals("ALPHANUM"))
							return WordDelimiterIterator.ALPHANUM;
						else
							if (s.equals("SUBWORD_DELIM")) {
							}else
								return null;






		return 0;
	}

	char[] out = new char[256];

	private String parseString(String s) {
		int readPos = 0;
		int len = s.length();
		int writePos = 0;
		while (readPos < len) {
			char c = s.charAt((readPos++));
			if (c == '\\') {
				if (readPos >= len)
					throw new IllegalArgumentException((("Invalid escaped char in [" + s) + "]"));

				c = s.charAt((readPos++));
				switch (c) {
					case '\\' :
						c = '\\';
						break;
					case 'n' :
						c = '\n';
						break;
					case 't' :
						c = '\t';
						break;
					case 'r' :
						c = '\r';
						break;
					case 'b' :
						c = '\b';
						break;
					case 'f' :
						c = '\f';
						break;
					case 'u' :
						if ((readPos + 3) >= len)
							throw new IllegalArgumentException((("Invalid escaped char in [" + s) + "]"));

						c = ((char) (Integer.parseInt(s.substring(readPos, (readPos + 4)), 16)));
						readPos += 4;
						break;
				}
			}
			out[(writePos++)] = c;
		} 
		return new String(out, 0, writePos);
	}
}

