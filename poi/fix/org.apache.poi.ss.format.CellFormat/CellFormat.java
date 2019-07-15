

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.apache.poi.ss.format.CellFormatPart;
import org.apache.poi.ss.format.CellFormatResult;
import org.apache.poi.ss.format.CellFormatter;
import org.apache.poi.ss.format.CellGeneralFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.util.LocaleUtil;


public class CellFormat {
	private final Locale locale;

	private final String format;

	private final CellFormatPart posNumFmt;

	private final CellFormatPart zeroNumFmt;

	private final CellFormatPart negNumFmt;

	private final CellFormatPart textFmt;

	private final int formatPartCount;

	private static final Pattern ONE_PART = Pattern.compile(((CellFormatPart.FORMAT_PAT.pattern()) + "(;|$)"), ((Pattern.COMMENTS) | (Pattern.CASE_INSENSITIVE)));

	private static final String INVALID_VALUE_FOR_FORMAT = "###################################################" + ((("###################################################" + "###################################################") + "###################################################") + "###################################################");

	private static String QUOTE = "\"";

	private static CellFormat createGeneralFormat(final Locale locale) {
		return new CellFormat(locale, "General") {
			@Override
			public CellFormatResult apply(Object value) {
				String text = new CellGeneralFormatter(locale).format(value);
				return new CellFormatResult(true, text, null);
			}
		};
	}

	private static final Map<Locale, Map<String, CellFormat>> formatCache = new WeakHashMap<>();

	public static CellFormat getInstance(String format) {
		return CellFormat.getInstance(LocaleUtil.getUserLocale(), format);
	}

	public static synchronized CellFormat getInstance(Locale locale, String format) {
		Map<String, CellFormat> formatMap = CellFormat.formatCache.get(locale);
		if (formatMap == null) {
			formatMap = new WeakHashMap<>();
			CellFormat.formatCache.put(locale, formatMap);
		}
		CellFormat fmt = formatMap.get(format);
		if (fmt == null) {
			if ((format.equals("General")) || (format.equals("@")))
				fmt = CellFormat.createGeneralFormat(locale);
			else
				fmt = new CellFormat(locale, format);

			formatMap.put(format, fmt);
		}
		return fmt;
	}

	private CellFormat(Locale locale, String format) {
		this.locale = locale;
		this.format = format;
		CellFormatPart defaultTextFormat = new CellFormatPart(locale, "@");
		Matcher m = CellFormat.ONE_PART.matcher(format);
		List<CellFormatPart> parts = new ArrayList<>();
		while (m.find()) {
			try {
				String valueDesc = m.group();
				if (valueDesc.endsWith(";"))
					valueDesc = valueDesc.substring(0, ((valueDesc.length()) - 1));

				parts.add(new CellFormatPart(locale, valueDesc));
			} catch (RuntimeException e) {
				parts.add(null);
			}
		} 
		formatPartCount = parts.size();
		switch (formatPartCount) {
			case 1 :
				posNumFmt = parts.get(0);
				negNumFmt = null;
				zeroNumFmt = null;
				textFmt = defaultTextFormat;
				break;
			case 2 :
				posNumFmt = parts.get(0);
				negNumFmt = parts.get(1);
				zeroNumFmt = null;
				textFmt = defaultTextFormat;
				break;
			case 3 :
				posNumFmt = parts.get(0);
				negNumFmt = parts.get(1);
				zeroNumFmt = parts.get(2);
				textFmt = defaultTextFormat;
				break;
			case 4 :
			default :
				posNumFmt = parts.get(0);
				negNumFmt = parts.get(1);
				zeroNumFmt = parts.get(2);
				textFmt = parts.get(3);
				break;
		}
	}

	public CellFormatResult apply(Object value) {
		if (value instanceof Number) {
			Number num = ((Number) (value));
			double val = num.doubleValue();
		}else
			if (value instanceof Date) {
				Double numericValue = DateUtil.getExcelDate(((Date) (value)));
				if (DateUtil.isValidExcelDate(numericValue)) {
					return getApplicableFormatPart(numericValue).apply(value);
				}else {
					throw new IllegalArgumentException((((("value " + numericValue) + " of date ") + value) + " is not a valid Excel date"));
				}
			}else {
				return textFmt.apply(value);
			}

		return null;
	}

	private CellFormatResult apply(Date date, double numericValue) {
		return getApplicableFormatPart(numericValue).apply(date);
	}

	public CellFormatResult apply(Cell c) {
		return null;
	}

	public CellFormatResult apply(JLabel label, Object value) {
		CellFormatResult result = apply(value);
		label.setText(result.text);
		if ((result.textColor) != null) {
			label.setForeground(result.textColor);
		}
		return result;
	}

	private CellFormatResult apply(JLabel label, Date date, double numericValue) {
		CellFormatResult result = apply(date, numericValue);
		label.setText(result.text);
		if ((result.textColor) != null) {
			label.setForeground(result.textColor);
		}
		return result;
	}

	public CellFormatResult apply(JLabel label, Cell c) {
		return null;
	}

	private CellFormatPart getApplicableFormatPart(Object value) {
		if (value instanceof Number) {
			double val = ((Number) (value)).doubleValue();
			if ((formatPartCount) == 1) {
			}else
				if ((formatPartCount) == 2) {
				}else {
				}

		}else {
			throw new IllegalArgumentException("value must be a Number");
		}
		return null;
	}

	public static CellType ultimateType(Cell cell) {
		CellType type = cell.getCellType();
		if (type == (CellType.FORMULA))
			return cell.getCachedFormulaResultType();
		else
			return type;

	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2")
	public static CellType ultimateTypeEnum(Cell cell) {
		return CellFormat.ultimateType(cell);
	}

	@Override
	public boolean equals(Object obj) {
		if ((this) == obj)
			return true;

		if (obj instanceof CellFormat) {
			CellFormat that = ((CellFormat) (obj));
			return format.equals(that.format);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return format.hashCode();
	}
}

