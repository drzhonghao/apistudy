import org.apache.accumulo.core.util.format.DefaultFormatter;
import org.apache.accumulo.core.util.format.DateFormatSupplier;
import org.apache.accumulo.core.util.format.FormatterConfig;
import org.apache.accumulo.core.util.format.*;


import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

/**
 * This class is <strong>not</strong> recommended because
 * {@link #initialize(Iterable, FormatterConfig)} replaces parameters in {@link FormatterConfig},
 * which could surprise users.
 *
 * This class can be replaced by {@link DefaultFormatter} where FormatterConfig is initialized with
 * a DateFormat set to {@link #DATE_FORMAT}. See
 * {@link DateFormatSupplier#createSimpleFormatSupplier(String, java.util.TimeZone)}.
 *
 * <pre>
 * final DateFormatSupplier dfSupplier = DateFormatSupplier.createSimpleFormatSupplier(
 *     DateFormatSupplier.HUMAN_READABLE_FORMAT, TimeZone.getTimeZone(&quot;UTC&quot;));
 * final FormatterConfig config = new FormatterConfig().setPrintTimestamps(true)
 *     .setDateFormatSupplier(dfSupplier);
 * </pre>
 */
@Deprecated
public class DateStringFormatter implements Formatter {

  private DefaultFormatter defaultFormatter;
  private TimeZone timeZone;

  public static final String DATE_FORMAT = DateFormatSupplier.HUMAN_READABLE_FORMAT;

  public DateStringFormatter() {
    this(TimeZone.getDefault());
  }

  public DateStringFormatter(TimeZone timeZone) {
    this.defaultFormatter = new DefaultFormatter();
    this.timeZone = timeZone;
  }

  @Override
  public void initialize(Iterable<Entry<Key,Value>> scanner, FormatterConfig config) {
    FormatterConfig newConfig = new FormatterConfig(config);
    newConfig.setDateFormatSupplier(
        DateFormatSupplier.createSimpleFormatSupplier(DATE_FORMAT, timeZone));
    defaultFormatter.initialize(scanner, newConfig);
  }

  @Override
  public boolean hasNext() {
    return defaultFormatter.hasNext();
  }

  @Override
  public String next() {
    return defaultFormatter.next();
  }

  @Override
  public void remove() {
    defaultFormatter.remove();
  }

}
