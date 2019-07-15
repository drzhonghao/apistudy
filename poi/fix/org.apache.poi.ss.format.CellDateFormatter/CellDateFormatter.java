

import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.Format;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import org.apache.poi.ss.format.CellFormatType;
import org.apache.poi.ss.format.CellFormatter;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.StringUtil;

import static java.text.DateFormat.Field.AM_PM;
import static java.text.DateFormat.Field.MILLISECOND;


public class CellDateFormatter extends CellFormatter {
	private boolean amPmUpper;

	private boolean showM;

	private boolean showAmPm;

	private DateFormat dateFmt = null;

	private String sFmt;

	private final Calendar EXCEL_EPOCH_CAL = LocaleUtil.getLocaleCalendar(1904, 0, 1);

	private static CellDateFormatter SIMPLE_DATE;

	private class DatePartHandler {
		private int mStart = -1;

		private int mLen;

		private int hStart = -1;

		private int hLen;

		public String handlePart(Matcher m, String part, CellFormatType type, StringBuffer desc) {
			int pos = desc.length();
			char firstCh = part.charAt(0);
			switch (firstCh) {
				case 's' :
				case 'S' :
					if ((mStart) >= 0) {
						for (int i = 0; i < (mLen); i++)
							desc.setCharAt(((mStart) + i), 'm');

						mStart = -1;
					}
					return part.toLowerCase(Locale.ROOT);
				case 'h' :
				case 'H' :
					mStart = -1;
					hStart = pos;
					hLen = part.length();
					return part.toLowerCase(Locale.ROOT);
				case 'd' :
				case 'D' :
					mStart = -1;
					if ((part.length()) <= 2)
						return part.toLowerCase(Locale.ROOT);
					else
						return part.toLowerCase(Locale.ROOT).replace('d', 'E');

				case 'm' :
				case 'M' :
					mStart = pos;
					mLen = part.length();
					if ((hStart) >= 0)
						return part.toLowerCase(Locale.ROOT);
					else
						return part.toUpperCase(Locale.ROOT);

				case 'y' :
				case 'Y' :
					mStart = -1;
					if ((part.length()) == 3)
						part = "yyyy";

					return part.toLowerCase(Locale.ROOT);
				case '0' :
					mStart = -1;
					int sLen = part.length();
					sFmt = ((("%0" + (sLen + 2)) + ".") + sLen) + "f";
					return part.replace('0', 'S');
				case 'a' :
				case 'A' :
				case 'p' :
				case 'P' :
					if ((part.length()) > 1) {
						mStart = -1;
						showAmPm = true;
						showM = StringUtil.toLowerCase(part.charAt(1)).equals("m");
						amPmUpper = (showM) || (StringUtil.isUpperCase(part.charAt(0)));
						return "a";
					}
				default :
					return null;
			}
		}

		public void finish(StringBuffer toAppendTo) {
			if (((hStart) >= 0) && (!(showAmPm))) {
				for (int i = 0; i < (hLen); i++) {
					toAppendTo.setCharAt(((hStart) + i), 'H');
				}
			}
		}
	}

	public CellDateFormatter(String format) {
		this(LocaleUtil.getUserLocale(), format);
	}

	public CellDateFormatter(Locale locale, String format) {
		super(format);
		CellDateFormatter.DatePartHandler partHandler = new CellDateFormatter.DatePartHandler();
		dateFmt.setTimeZone(LocaleUtil.getUserTimeZone());
		dateFmt = null;
	}

	public void formatValue(StringBuffer toAppendTo, Object value) {
		if (value == null)
			value = 0.0;

		if (value instanceof Number) {
			Number num = ((Number) (value));
			long v = num.longValue();
			if (v == 0L) {
				value = EXCEL_EPOCH_CAL.getTime();
			}else {
				Calendar c = ((Calendar) (EXCEL_EPOCH_CAL.clone()));
				c.add(Calendar.SECOND, ((int) (v / 1000)));
				c.add(Calendar.MILLISECOND, ((int) (v % 1000)));
				value = c.getTime();
			}
		}
		AttributedCharacterIterator it = dateFmt.formatToCharacterIterator(value);
		boolean doneAm = false;
		boolean doneMillis = false;
		for (char ch = it.first(); ch != (CharacterIterator.DONE); ch = it.next()) {
			if ((it.getAttribute(MILLISECOND)) != null) {
				if (!doneMillis) {
					Date dateObj = ((Date) (value));
					int pos = toAppendTo.length();
					try (Formatter formatter = new Formatter(toAppendTo, Locale.ROOT)) {
						long msecs = (dateObj.getTime()) % 1000;
						formatter.format(locale, sFmt, (msecs / 1000.0));
					}
					toAppendTo.delete(pos, (pos + 2));
					doneMillis = true;
				}
			}else
				if ((it.getAttribute(AM_PM)) != null) {
					if (!doneAm) {
						if (showAmPm) {
							if (amPmUpper) {
								toAppendTo.append(StringUtil.toUpperCase(ch));
								if (showM)
									toAppendTo.append('M');

							}else {
								toAppendTo.append(StringUtil.toLowerCase(ch));
								if (showM)
									toAppendTo.append('m');

							}
						}
						doneAm = true;
					}
				}else {
					toAppendTo.append(ch);
				}

		}
	}

	public void simpleValue(StringBuffer toAppendTo, Object value) {
		synchronized(CellDateFormatter.class) {
			if (((CellDateFormatter.SIMPLE_DATE) == null) || (!(CellDateFormatter.SIMPLE_DATE.EXCEL_EPOCH_CAL.equals(EXCEL_EPOCH_CAL)))) {
				CellDateFormatter.SIMPLE_DATE = new CellDateFormatter("mm/d/y");
			}
		}
		CellDateFormatter.SIMPLE_DATE.formatValue(toAppendTo, value);
	}
}

