import org.apache.accumulo.core.client.mapreduce.*;


import java.io.IOException;
import java.util.Map.Entry;

import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.format.DefaultFormatter;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.log4j.Level;

/**
 * This class allows MapReduce jobs to use Accumulo as the source of data. This {@link InputFormat}
 * provides keys and values of type {@link Key} and {@link Value} to the Map function.
 *
 * The user must specify the following via static configurator methods:
 *
 * <ul>
 * <li>{@link AccumuloInputFormat#setConnectorInfo(Job, String, AuthenticationToken)}
 * <li>{@link AccumuloInputFormat#setScanAuthorizations(Job, Authorizations)}
 * <li>{@link AccumuloInputFormat#setZooKeeperInstance(Job, ClientConfiguration)}
 * </ul>
 *
 * Other static methods are optional.
 */
public class AccumuloInputFormat extends InputFormatBase<Key,Value> {

  @Override
  public RecordReader<Key,Value> createRecordReader(InputSplit split, TaskAttemptContext context)
      throws IOException, InterruptedException {
    log.setLevel(getLogLevel(context));

    // Override the log level from the configuration as if the InputSplit has one it's the more
    // correct one to use.
    if (split instanceof org.apache.accumulo.core.client.mapreduce.RangeInputSplit) {
      // @formatter:off
      org.apache.accumulo.core.client.mapreduce.RangeInputSplit accSplit =
        (org.apache.accumulo.core.client.mapreduce.RangeInputSplit) split;
      // @formatter:on
      Level level = accSplit.getLogLevel();
      if (null != level) {
        log.setLevel(level);
      }
    } else {
      throw new IllegalArgumentException("No RecordReader for " + split.getClass().toString());
    }

    return new RecordReaderBase<Key,Value>() {
      @Override
      public boolean nextKeyValue() throws IOException, InterruptedException {
        if (scannerIterator.hasNext()) {
          ++numKeysRead;
          Entry<Key,Value> entry = scannerIterator.next();
          currentK = currentKey = entry.getKey();
          currentV = entry.getValue();
          if (log.isTraceEnabled())
            log.trace("Processing key/value pair: " + DefaultFormatter.formatEntry(entry, true));
          return true;
        }
        return false;
      }
    };
  }
}
