import org.apache.lucene.queryparser.flexible.standard.config.*;


import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Date;

/**
 * This {@link Format} parses {@link Long} into date strings and vice-versa. It
 * uses the given {@link DateFormat} to parse and format dates, but before, it
 * converts {@link Long} to {@link Date} objects or vice-versa.
 */
public class NumberDateFormat extends NumberFormat {
  
  private static final long serialVersionUID = 964823936071308283L;
  
  final private DateFormat dateFormat;
  
  /**
   * Constructs a {@link NumberDateFormat} object using the given {@link DateFormat}.
   * 
   * @param dateFormat {@link DateFormat} used to parse and format dates
   */
  public NumberDateFormat(DateFormat dateFormat) {
    this.dateFormat = dateFormat;
  }
  
  @Override
  public StringBuffer format(double number, StringBuffer toAppendTo,
      FieldPosition pos) {
    return dateFormat.format(new Date((long) number), toAppendTo, pos);
  }
  
  @Override
  public StringBuffer format(long number, StringBuffer toAppendTo,
      FieldPosition pos) {
    return dateFormat.format(new Date(number), toAppendTo, pos);
  }
  
  @Override
  public Number parse(String source, ParsePosition parsePosition) {
    final Date date = dateFormat.parse(source, parsePosition);
    return (date == null) ? null : date.getTime();
  }
  
  @Override
  public StringBuffer format(Object number, StringBuffer toAppendTo,
      FieldPosition pos) {
    return dateFormat.format(number, toAppendTo, pos);
  }
  
}
