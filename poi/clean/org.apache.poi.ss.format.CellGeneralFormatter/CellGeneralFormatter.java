import org.apache.poi.ss.format.*;


import java.util.Formatter;
import java.util.Locale;

import org.apache.poi.util.LocaleUtil;

/**
 * A formatter for the default "General" cell format.
 *
 * @author Ken Arnold, Industrious Media LLC
 */
public class CellGeneralFormatter extends CellFormatter {
    /** Creates a new general formatter. */
    public CellGeneralFormatter() {
        this(LocaleUtil.getUserLocale());
    }
    /** Creates a new general formatter. */
    public CellGeneralFormatter(Locale locale) {
        super(locale, "General");
    }

    /**
     * The general style is not quite the same as any other, or any combination
     * of others.
     *
     * @param toAppendTo The buffer to append to.
     * @param value      The value to format.
     */
    public void formatValue(StringBuffer toAppendTo, Object value) {
        if (value instanceof Number) {
            double val = ((Number) value).doubleValue();
            if (val == 0) {
                toAppendTo.append('0');
                return;
            }

            String fmt;
            double exp = Math.log10(Math.abs(val));
            boolean stripZeros = true;
            if (exp > 10 || exp < -9)
                fmt = "%1.5E";
            else if ((long) val != val)
                fmt = "%1.9f";
            else {
                fmt = "%1.0f";
                stripZeros = false;
            }

            Formatter formatter = new Formatter(toAppendTo, locale);
            try {
                formatter.format(locale, fmt, value);
            } finally {
                formatter.close();
            }
            if (stripZeros) {
                // strip off trailing zeros
                int removeFrom;
                if (fmt.endsWith("E"))
                    removeFrom = toAppendTo.lastIndexOf("E") - 1;
                else
                    removeFrom = toAppendTo.length() - 1;
                while (toAppendTo.charAt(removeFrom) == '0') {
                    toAppendTo.deleteCharAt(removeFrom--);
                }
                if (toAppendTo.charAt(removeFrom) == '.') {
                    toAppendTo.deleteCharAt(removeFrom--);
                }
            }
        } else if (value instanceof Boolean) {
            toAppendTo.append(value.toString().toUpperCase(Locale.ROOT));
        } else {
            toAppendTo.append(value);
        }
    }

    /** Equivalent to {@link #formatValue(StringBuffer,Object)}. {@inheritDoc}. */
    public void simpleValue(StringBuffer toAppendTo, Object value) {
        formatValue(toAppendTo, value);
    }
}
