import org.apache.accumulo.core.util.format.FormatterConfig;
import org.apache.accumulo.core.util.format.*;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.google.common.base.Supplier;

/**
 * DateFormatSupplier is a {@code ThreadLocal<DateFormat>} that will set the correct TimeZone when
 * the object is retrieved by {@link #get()}.
 *
 * This exists as a way to get around thread safety issues in {@link DateFormat}. This class also
 * contains helper methods that create some useful DateFormatSuppliers.
 *
 * Instances of DateFormatSuppliers can be shared, but note that a DateFormat generated from it will
 * be shared by all classes within a Thread.
 *
 * In general, the state of a retrieved DateFormat should not be changed, unless it makes sense to
 * only perform a state change within that Thread.
 */
public abstract class DateFormatSupplier extends ThreadLocal<DateFormat>
    implements Supplier<DateFormat> {
  private TimeZone timeZone;

  public DateFormatSupplier() {
    timeZone = TimeZone.getDefault();
  }

  public DateFormatSupplier(TimeZone timeZone) {
    this.timeZone = timeZone;
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
  }

  /** Always sets the TimeZone, which is a fast operation */
  @Override
  public DateFormat get() {
    final DateFormat df = super.get();
    df.setTimeZone(timeZone);
    return df;
  }

  public static final String HUMAN_READABLE_FORMAT = "yyyy/MM/dd HH:mm:ss.SSS";

  /**
   * Create a Supplier for {@link FormatterConfig.DefaultDateFormat}s
   */
  public static DateFormatSupplier createDefaultFormatSupplier() {
    return new DateFormatSupplier() {
      @Override
      protected DateFormat initialValue() {
        return new FormatterConfig.DefaultDateFormat();
      }
    };
  }

  /** Create a generator for SimpleDateFormats accepting a dateFormat */
  public static DateFormatSupplier createSimpleFormatSupplier(final String dateFormat) {
    return new DateFormatSupplier() {
      @Override
      protected SimpleDateFormat initialValue() {
        return new SimpleDateFormat(dateFormat);
      }
    };
  }

  /** Create a generator for SimpleDateFormats accepting a dateFormat */
  public static DateFormatSupplier createSimpleFormatSupplier(final String dateFormat,
      final TimeZone timeZone) {
    return new DateFormatSupplier(timeZone) {
      @Override
      protected SimpleDateFormat initialValue() {
        return new SimpleDateFormat(dateFormat);
      }
    };
  }
}
