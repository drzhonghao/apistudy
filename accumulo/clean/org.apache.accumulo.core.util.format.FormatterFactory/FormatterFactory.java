import org.apache.accumulo.core.util.format.FormatterConfig;
import org.apache.accumulo.core.util.format.DefaultFormatter;
import org.apache.accumulo.core.util.format.*;


import java.util.Map.Entry;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormatterFactory {
  private static final Logger log = LoggerFactory.getLogger(FormatterFactory.class);

  public static Formatter getFormatter(Class<? extends Formatter> formatterClass,
      Iterable<Entry<Key,Value>> scanner, FormatterConfig config) {
    Formatter formatter = null;
    try {
      formatter = formatterClass.newInstance();
    } catch (Exception e) {
      log.warn("Unable to instantiate formatter. Using default formatter.", e);
      formatter = new DefaultFormatter();
    }
    formatter.initialize(scanner, config);
    return formatter;
  }

  public static Formatter getDefaultFormatter(Iterable<Entry<Key,Value>> scanner,
      FormatterConfig config) {
    return getFormatter(DefaultFormatter.class, scanner, config);
  }

  private FormatterFactory() {
    // prevent instantiation
  }
}
