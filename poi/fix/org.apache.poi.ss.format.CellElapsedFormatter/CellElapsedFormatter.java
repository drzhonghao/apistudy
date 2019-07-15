

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.format.CellFormatType;
import org.apache.poi.ss.format.CellFormatter;


public class CellElapsedFormatter extends CellFormatter {
	private final List<CellElapsedFormatter.TimeSpec> specs;

	private CellElapsedFormatter.TimeSpec topmost;

	private final String printfFmt;

	private static final Pattern PERCENTS = Pattern.compile("%");

	private static final double HOUR__FACTOR = 1.0 / 24.0;

	private static final double MIN__FACTOR = (CellElapsedFormatter.HOUR__FACTOR) / 60.0;

	private static final double SEC__FACTOR = (CellElapsedFormatter.MIN__FACTOR) / 60.0;

	private static class TimeSpec {
		final char type;

		final int pos;

		final int len;

		final double factor;

		double modBy;

		public TimeSpec(char type, int pos, int len, double factor) {
			this.type = type;
			this.pos = pos;
			this.len = len;
			this.factor = factor;
			modBy = 0;
		}

		public long valueFor(double elapsed) {
			double val;
			if ((modBy) == 0)
				val = elapsed / (factor);
			else
				val = (elapsed / (factor)) % (modBy);

			if ((type) == '0')
				return Math.round(val);
			else
				return ((long) (val));

		}
	}

	private class ElapsedPartHandler {
		public String handlePart(Matcher m, String part, CellFormatType type, StringBuffer desc) {
			int pos = desc.length();
			char firstCh = part.charAt(0);
			switch (firstCh) {
				case '[' :
					if ((part.length()) < 3)
						break;

					if ((topmost) != null)
						throw new IllegalArgumentException("Duplicate '[' times in format");

					part = part.toLowerCase(Locale.ROOT);
					int specLen = (part.length()) - 2;
					topmost = assignSpec(part.charAt(1), pos, specLen);
					return part.substring(1, (1 + specLen));
				case 'h' :
				case 'm' :
				case 's' :
				case '0' :
					part = part.toLowerCase(Locale.ROOT);
					assignSpec(part.charAt(0), pos, part.length());
					return part;
				case '\n' :
					return "%n";
				case '\"' :
					part = part.substring(1, ((part.length()) - 1));
					break;
				case '\\' :
					part = part.substring(1);
					break;
				case '*' :
					if ((part.length()) > 1) {
					}
					break;
				case '_' :
					return null;
			}
			return CellElapsedFormatter.PERCENTS.matcher(part).replaceAll("%%");
		}
	}

	public CellElapsedFormatter(String pattern) {
		super(pattern);
		specs = new ArrayList<>();
		ListIterator<CellElapsedFormatter.TimeSpec> it = specs.listIterator(specs.size());
		while (it.hasPrevious()) {
			CellElapsedFormatter.TimeSpec spec = it.previous();
			if ((spec.type) != (topmost.type)) {
				spec.modBy = CellElapsedFormatter.modFor(spec.type, spec.len);
			}
		} 
		printfFmt = null;
	}

	private CellElapsedFormatter.TimeSpec assignSpec(char type, int pos, int len) {
		CellElapsedFormatter.TimeSpec spec = new CellElapsedFormatter.TimeSpec(type, pos, len, CellElapsedFormatter.factorFor(type, len));
		specs.add(spec);
		return spec;
	}

	private static double factorFor(char type, int len) {
		switch (type) {
			case 'h' :
				return CellElapsedFormatter.HOUR__FACTOR;
			case 'm' :
				return CellElapsedFormatter.MIN__FACTOR;
			case 's' :
				return CellElapsedFormatter.SEC__FACTOR;
			case '0' :
				return (CellElapsedFormatter.SEC__FACTOR) / (Math.pow(10, len));
			default :
				throw new IllegalArgumentException(("Uknown elapsed time spec: " + type));
		}
	}

	private static double modFor(char type, int len) {
		switch (type) {
			case 'h' :
				return 24;
			case 'm' :
				return 60;
			case 's' :
				return 60;
			case '0' :
				return Math.pow(10, len);
			default :
				throw new IllegalArgumentException(("Uknown elapsed time spec: " + type));
		}
	}

	public void formatValue(StringBuffer toAppendTo, Object value) {
		double elapsed = ((Number) (value)).doubleValue();
		if (elapsed < 0) {
			toAppendTo.append('-');
			elapsed = -elapsed;
		}
		Object[] parts = new Long[specs.size()];
		for (int i = 0; i < (specs.size()); i++) {
			parts[i] = specs.get(i).valueFor(elapsed);
		}
		Formatter formatter = new Formatter(toAppendTo, Locale.ROOT);
		try {
			formatter.format(printfFmt, parts);
		} finally {
			formatter.close();
		}
	}

	public void simpleValue(StringBuffer toAppendTo, Object value) {
		formatValue(toAppendTo, value);
	}
}

