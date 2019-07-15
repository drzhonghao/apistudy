

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.karaf.log.core.internal.layout.AbsoluteTimeDateFormat;
import org.apache.karaf.log.core.internal.layout.DateTimeDateFormat;
import org.apache.karaf.log.core.internal.layout.FormattingInfo;
import org.apache.karaf.log.core.internal.layout.ISO8601DateFormat;
import org.apache.karaf.log.core.internal.layout.PatternConverter;
import org.ops4j.pax.logging.PaxLogger;
import org.ops4j.pax.logging.spi.PaxLevel;
import org.ops4j.pax.logging.spi.PaxLocationInfo;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;


public class PatternParser {
	private static final String LINE_SEP = System.getProperty("line.separator");

	private static final char ESCAPE_CHAR = '%';

	private static final int LITERAL_STATE = 0;

	private static final int CONVERTER_STATE = 1;

	private static final int MINUS_STATE = 2;

	private static final int DOT_STATE = 3;

	private static final int MIN_STATE = 4;

	private static final int MAX_STATE = 5;

	static final int FULL_LOCATION_CONVERTER = 1000;

	static final int METHOD_LOCATION_CONVERTER = 1001;

	static final int CLASS_LOCATION_CONVERTER = 1002;

	static final int LINE_LOCATION_CONVERTER = 1003;

	static final int FILE_LOCATION_CONVERTER = 1004;

	static final int RELATIVE_TIME_CONVERTER = 2000;

	static final int THREAD_CONVERTER = 2001;

	static final int LEVEL_CONVERTER = 2002;

	static final int NDC_CONVERTER = 2003;

	static final int MESSAGE_CONVERTER = 2004;

	int state;

	protected StringBuffer currentLiteral = new StringBuffer(32);

	protected int patternLength;

	protected int i;

	PatternConverter head;

	PatternConverter tail;

	protected FormattingInfo formattingInfo = new FormattingInfo();

	protected String pattern;

	public PatternParser(String pattern) {
		this.pattern = pattern;
		patternLength = pattern.length();
		state = PatternParser.LITERAL_STATE;
	}

	private void addToList(PatternConverter pc) {
		if ((head) == null) {
			head = tail = pc;
		}else {
			tail.next = pc;
			tail = pc;
		}
	}

	protected String extractOption() {
		if (((i) < (patternLength)) && ((pattern.charAt(i)) == '{')) {
			int end = i;
			int nb = 1;
			while ((++end) < (patternLength)) {
				switch (pattern.charAt(end)) {
					case '{' :
						nb++;
						break;
					case '}' :
						if ((--nb) == 0) {
							String r = pattern.substring(((i) + 1), end);
							i = end + 1;
							return r;
						}
						break;
				}
			} 
		}
		return null;
	}

	protected int extractPrecisionOption() {
		String opt = extractOption();
		int r = 0;
		if (opt != null) {
			try {
				r = Integer.parseInt(opt);
				if (r <= 0) {
					r = 0;
				}
			} catch (NumberFormatException e) {
			}
		}
		return r;
	}

	public PatternConverter parse() {
		char c;
		i = 0;
		while ((i) < (patternLength)) {
			c = pattern.charAt(((i)++));
		} 
		if ((currentLiteral.length()) != 0) {
			addToList(new PatternParser.LiteralPatternConverter(currentLiteral.toString()));
		}
		return head;
	}

	protected void finalizeConverter(char c) {
		PatternConverter pc = null;
		switch (c) {
			case 'c' :
				pc = new PatternParser.CategoryPatternConverter(formattingInfo, extractPrecisionOption());
				currentLiteral.setLength(0);
				break;
			case 'C' :
				pc = new PatternParser.ClassNamePatternConverter(formattingInfo, extractPrecisionOption());
				currentLiteral.setLength(0);
				break;
			case 'd' :
				String dateFormatStr = AbsoluteTimeDateFormat.ISO8601_DATE_FORMAT;
				DateFormat df;
				String dOpt = extractOption();
				if (dOpt != null)
					dateFormatStr = dOpt;

				if (dateFormatStr.equalsIgnoreCase(AbsoluteTimeDateFormat.ISO8601_DATE_FORMAT))
					df = new ISO8601DateFormat();
				else
					if (dateFormatStr.equalsIgnoreCase(AbsoluteTimeDateFormat.ABS_TIME_DATE_FORMAT))
						df = new AbsoluteTimeDateFormat();
					else
						if (dateFormatStr.equalsIgnoreCase(AbsoluteTimeDateFormat.DATE_AND_TIME_DATE_FORMAT))
							df = new DateTimeDateFormat();
						else {
							try {
								df = new SimpleDateFormat(dateFormatStr);
							} catch (IllegalArgumentException e) {
								df = new ISO8601DateFormat();
							}
						}


				pc = new PatternParser.DatePatternConverter(formattingInfo, df);
				currentLiteral.setLength(0);
				break;
			case 'F' :
				pc = new PatternParser.LocationPatternConverter(formattingInfo, PatternParser.FILE_LOCATION_CONVERTER);
				currentLiteral.setLength(0);
				break;
			case 'h' :
				String pat = extractOption();
				String style = extractOption();
				pc = new PatternParser.HighlightPatternConverter(formattingInfo, pat, style);
				currentLiteral.setLength(0);
				break;
			case 'L' :
				pc = new PatternParser.LocationPatternConverter(formattingInfo, PatternParser.LINE_LOCATION_CONVERTER);
				currentLiteral.setLength(0);
				break;
			case 'm' :
				pc = new PatternParser.BasicPatternConverter(formattingInfo, PatternParser.MESSAGE_CONVERTER);
				currentLiteral.setLength(0);
				break;
			case 'M' :
				pc = new PatternParser.LocationPatternConverter(formattingInfo, PatternParser.METHOD_LOCATION_CONVERTER);
				currentLiteral.setLength(0);
				break;
			case 'p' :
				pc = new PatternParser.BasicPatternConverter(formattingInfo, PatternParser.LEVEL_CONVERTER);
				currentLiteral.setLength(0);
				break;
			case 'r' :
				pc = new PatternParser.BasicPatternConverter(formattingInfo, PatternParser.RELATIVE_TIME_CONVERTER);
				currentLiteral.setLength(0);
				break;
			case 't' :
				pc = new PatternParser.BasicPatternConverter(formattingInfo, PatternParser.THREAD_CONVERTER);
				currentLiteral.setLength(0);
				break;
			case 'X' :
				String xOpt = extractOption();
				pc = new PatternParser.MDCPatternConverter(formattingInfo, xOpt);
				currentLiteral.setLength(0);
				break;
			default :
				pc = new PatternParser.LiteralPatternConverter(currentLiteral.toString());
				currentLiteral.setLength(0);
		}
		addConverter(pc);
	}

	protected void addConverter(PatternConverter pc) {
		currentLiteral.setLength(0);
		addToList(pc);
		state = PatternParser.LITERAL_STATE;
	}

	private static class BasicPatternConverter extends PatternConverter {
		int type;

		BasicPatternConverter(FormattingInfo formattingInfo, int type) {
			super(formattingInfo);
			this.type = type;
		}

		public String convert(PaxLoggingEvent event) {
			switch (type) {
				case PatternParser.RELATIVE_TIME_CONVERTER :
					return Long.toString(((event.getTimeStamp()) - (PatternParser.getStartTime())));
				case PatternParser.THREAD_CONVERTER :
					return event.getThreadName();
				case PatternParser.LEVEL_CONVERTER :
					return event.getLevel().toString();
				case PatternParser.MESSAGE_CONVERTER :
					{
						return event.getRenderedMessage();
					}
				default :
					return null;
			}
		}
	}

	private static class LiteralPatternConverter extends PatternConverter {
		private String literal;

		LiteralPatternConverter(String value) {
			literal = value;
		}

		public final void format(StringBuffer sbuf, PaxLoggingEvent event) {
			sbuf.append(literal);
		}

		public String convert(PaxLoggingEvent event) {
			return literal;
		}
	}

	private static class DatePatternConverter extends PatternConverter {
		private DateFormat df;

		private Date date;

		DatePatternConverter(FormattingInfo formattingInfo, DateFormat df) {
			super(formattingInfo);
			date = new Date();
			this.df = df;
		}

		public String convert(PaxLoggingEvent event) {
			date.setTime(event.getTimeStamp());
			String converted = null;
			try {
				converted = df.format(date);
			} catch (Exception ex) {
			}
			return converted;
		}
	}

	private static class HighlightPatternConverter extends PatternConverter {
		static Map<String, String> SEQUENCES;

		static {
			PatternParser.HighlightPatternConverter.SEQUENCES = new HashMap<>();
			PatternParser.HighlightPatternConverter.SEQUENCES.put("csi", "\u001b[");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("suffix", "m");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("separator", ";");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("normal", "0");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("bold", "1");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("bright", "1");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("dim", "2");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("underline", "3");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("blink", "5");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("reverse", "7");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("hidden", "8");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("black", "30");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("fg_black", "30");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("red", "31");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("fg_red", "31");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("green", "32");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("fg_green", "32");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("yellow", "33");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("fg_yellow", "33");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("blue", "34");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("fg_blue", "34");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("magenta", "35");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("fg_magenta", "35");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("cyan", "36");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("fg_cyan", "36");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("white", "37");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("fg_white", "37");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("default", "39");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("fg_default", "39");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("bg_black", "40");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("bg_red", "41");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("bg_green", "42");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("bg_yellow", "43");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("bg_blue", "44");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("bg_magenta", "45");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("bg_cyan", "46");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("bg_white", "47");
			PatternParser.HighlightPatternConverter.SEQUENCES.put("bg_default", "49");
		}

		private PatternConverter pattern;

		private Map<String, String> style;

		HighlightPatternConverter(FormattingInfo formattingInfo, String pattern, String style) {
			super(formattingInfo);
			this.pattern = new PatternParser(pattern).parse();
			Map<String, String> unparsed = new HashMap<>();
			unparsed.put("trace", "cyan");
			unparsed.put("debug", "cyan");
			unparsed.put("info", "bright green");
			unparsed.put("warn", "bright yellow");
			unparsed.put("error", "bright red");
			unparsed.put("fatal", "bright red");
			if (style != null) {
				style = style.toLowerCase(Locale.ENGLISH);
				if (((style.indexOf(',')) < 0) && ((style.indexOf('=')) < 0)) {
					unparsed.put("trace", style.trim());
					unparsed.put("debug", style.trim());
					unparsed.put("info", style.trim());
					unparsed.put("warn", style.trim());
					unparsed.put("error", style.trim());
					unparsed.put("fatal", style.trim());
				}else {
					String[] keys = style.split("\\s*,\\s*");
					for (String key : keys) {
						String[] val = key.split("\\s*=\\s*");
						if ((val.length) > 1) {
							unparsed.put(val[0].trim(), val[1].trim());
						}
					}
				}
			}
			this.style = new HashMap<>();
			for (Map.Entry<String, String> e : unparsed.entrySet()) {
				this.style.put(e.getKey(), createSequence(e.getValue().split("\\s")));
			}
		}

		private String createSequence(String... names) {
			StringBuilder sb = new StringBuilder(PatternParser.HighlightPatternConverter.SEQUENCES.get("csi"));
			boolean first = true;
			for (String name : names) {
				name = name.trim();
				if (!first) {
					sb.append(PatternParser.HighlightPatternConverter.SEQUENCES.get("separator"));
				}
				first = false;
				sb.append(PatternParser.HighlightPatternConverter.SEQUENCES.getOrDefault(name, name));
			}
			sb.append(PatternParser.HighlightPatternConverter.SEQUENCES.get("suffix"));
			return sb.toString();
		}

		public String convert(PaxLoggingEvent event) {
			String s;
			switch (event.getLevel().toInt()) {
				case PaxLogger.LEVEL_TRACE :
					s = "trace";
					break;
				case PaxLogger.LEVEL_DEBUG :
					s = "debug";
					break;
				case PaxLogger.LEVEL_INFO :
					s = "info";
					break;
				case PaxLogger.LEVEL_WARNING :
					s = "warn";
					break;
				default :
					s = "error";
					break;
			}
			String str = style.get(s);
			if (str != null) {
			}else {
			}
			return null;
		}
	}

	private class LocationPatternConverter extends PatternConverter {
		int type;

		LocationPatternConverter(FormattingInfo formattingInfo, int type) {
			super(formattingInfo);
			this.type = type;
		}

		public String convert(PaxLoggingEvent event) {
			if (!(event.locationInformationExists())) {
				return "?";
			}
			PaxLocationInfo locationInfo = event.getLocationInformation();
			switch (type) {
				case PatternParser.METHOD_LOCATION_CONVERTER :
					return locationInfo.getMethodName();
				case PatternParser.LINE_LOCATION_CONVERTER :
					return locationInfo.getLineNumber();
				case PatternParser.FILE_LOCATION_CONVERTER :
					return locationInfo.getFileName();
				default :
					return null;
			}
		}
	}

	private abstract static class NamedPatternConverter extends PatternConverter {
		int precision;

		NamedPatternConverter(FormattingInfo formattingInfo, int precision) {
			super(formattingInfo);
			this.precision = precision;
		}

		abstract String getFullyQualifiedName(PaxLoggingEvent event);

		public String convert(PaxLoggingEvent event) {
			String n = getFullyQualifiedName(event);
			if (n == null)
				return null;

			if ((precision) <= 0)
				return n;
			else {
				int len = n.length();
				int end = len - 1;
				for (int i = precision; i > 0; i--) {
					end = n.lastIndexOf('.', (end - 1));
					if (end == (-1))
						return n;

				}
				return n.substring((end + 1), len);
			}
		}
	}

	private class ClassNamePatternConverter extends PatternParser.NamedPatternConverter {
		ClassNamePatternConverter(FormattingInfo formattingInfo, int precision) {
			super(formattingInfo, precision);
		}

		String getFullyQualifiedName(PaxLoggingEvent event) {
			if (!(event.locationInformationExists())) {
				return "?";
			}
			return event.getLocationInformation().getClassName();
		}
	}

	private class CategoryPatternConverter extends PatternParser.NamedPatternConverter {
		CategoryPatternConverter(FormattingInfo formattingInfo, int precision) {
			super(formattingInfo, precision);
		}

		String getFullyQualifiedName(PaxLoggingEvent event) {
			return event.getLoggerName();
		}
	}

	private class MDCPatternConverter extends PatternConverter {
		String key;

		MDCPatternConverter(FormattingInfo formattingInfo, String key) {
			super(formattingInfo);
			this.key = key;
		}

		public String convert(PaxLoggingEvent event) {
			Map properties = event.getProperties();
			if (properties == null) {
				return null;
			}else
				if ((key) == null) {
					StringBuffer buf = new StringBuffer("{");
					if ((properties.size()) > 0) {
						Object[] keys = properties.keySet().toArray();
						Arrays.sort(keys);
						for (Object key : keys) {
							buf.append('{');
							buf.append(key);
							buf.append(',');
							buf.append(properties.get(key));
							buf.append('}');
						}
					}
					buf.append('}');
					return buf.toString();
				}else {
					Object val = properties.get(key);
					if (val == null) {
						return null;
					}else {
						return val.toString();
					}
				}

		}
	}

	private static long startTime = 0;

	private static long getStartTime() {
		if ((PatternParser.startTime) == 0) {
			synchronized(PatternParser.class) {
				try {
					PatternParser.startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
				} catch (Throwable t) {
					PatternParser.startTime = System.currentTimeMillis();
				}
			}
		}
		return PatternParser.startTime;
	}
}

