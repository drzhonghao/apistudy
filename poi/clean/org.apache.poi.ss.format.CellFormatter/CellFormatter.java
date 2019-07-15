import org.apache.poi.ss.format.*;


import java.util.Locale;
import java.util.logging.Logger;

import org.apache.poi.util.LocaleUtil;

/**
 * This is the abstract supertype for the various cell formatters.
 *
 * @author Ken Arnold, Industrious Media LLC
 */
public abstract class CellFormatter {
    /** The original specified format. */
    protected final String format;
    protected final Locale locale;

    /**
     * Creates a new formatter object, storing the format in {@link #format}.
     *
     * @param format The format.
     */
    public CellFormatter(String format) {
        this(LocaleUtil.getUserLocale(), format);
    }

    /**
     * Creates a new formatter object, storing the format in {@link #format}.
     *
     * @param locale The locale.
     * @param format The format.
     */
    public CellFormatter(Locale locale, String format) {
        this.locale = locale;
        this.format = format;
    }

    /** The logger to use in the formatting code. */
    static final Logger logger = Logger.getLogger(
            CellFormatter.class.getName());

    /**
     * Format a value according the format string.
     *
     * @param toAppendTo The buffer to append to.
     * @param value      The value to format.
     */
    public abstract void formatValue(StringBuffer toAppendTo, Object value);

    /**
     * Format a value according to the type, in the most basic way.
     *
     * @param toAppendTo The buffer to append to.
     * @param value      The value to format.
     */
    public abstract void simpleValue(StringBuffer toAppendTo, Object value);

    /**
     * Formats the value, returning the resulting string.
     *
     * @param value The value to format.
     *
     * @return The value, formatted.
     */
    public String format(Object value) {
        StringBuffer sb = new StringBuffer();
        formatValue(sb, value);
        return sb.toString();
    }

    /**
     * Formats the value in the most basic way, returning the resulting string.
     *
     * @param value The value to format.
     *
     * @return The value, formatted.
     */
    public String simpleFormat(Object value) {
        StringBuffer sb = new StringBuffer();
        simpleValue(sb, value);
        return sb.toString();
    }

    /**
     * Returns the input string, surrounded by quotes.
     *
     * @param str The string to quote.
     *
     * @return The input string, surrounded by quotes.
     */
    static String quote(String str) {
        return '"' + str + '"';
    }
}
