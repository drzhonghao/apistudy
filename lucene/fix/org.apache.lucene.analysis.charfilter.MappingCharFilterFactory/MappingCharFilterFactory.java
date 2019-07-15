

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;


public class MappingCharFilterFactory extends CharFilterFactory implements MultiTermAwareComponent , ResourceLoaderAware {
	protected NormalizeCharMap normMap;

	private final String mapping;

	public MappingCharFilterFactory(Map<String, String> args) {
		super(args);
		mapping = get(args, "mapping");
		if (!(args.isEmpty())) {
			throw new IllegalArgumentException(("Unknown parameters: " + args));
		}
	}

	@Override
	public void inform(ResourceLoader loader) throws IOException {
		if ((mapping) != null) {
			List<String> wlist = null;
			List<String> files = splitFileNames(mapping);
			wlist = new ArrayList<>();
			for (String file : files) {
				List<String> lines = getLines(loader, file.trim());
				wlist.addAll(lines);
			}
			final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
			parseRules(wlist, builder);
			normMap = builder.build();
		}
	}

	@Override
	public Reader create(Reader input) {
		return (normMap) == null ? input : new MappingCharFilter(normMap, input);
	}

	static Pattern p = Pattern.compile("\"(.*)\"\\s*=>\\s*\"(.*)\"\\s*$");

	protected void parseRules(List<String> rules, NormalizeCharMap.Builder builder) {
		for (String rule : rules) {
			Matcher m = MappingCharFilterFactory.p.matcher(rule);
			if (!(m.find()))
				throw new IllegalArgumentException(((("Invalid Mapping Rule : [" + rule) + "], file = ") + (mapping)));

			builder.add(parseString(m.group(1)), parseString(m.group(2)));
		}
	}

	char[] out = new char[256];

	protected String parseString(String s) {
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
					case '"' :
						c = '"';
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

	@Override
	public AbstractAnalysisFactory getMultiTermComponent() {
		return this;
	}
}

