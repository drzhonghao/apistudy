import org.apache.accumulo.core.client.mapred.*;


import java.io.IOException;
import java.util.Map.Entry;

import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.format.DefaultFormatter;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.log4j.Level;

/**
 * This class allows MapReduce jobs to use Accumulo as the source of data. This {@link InputFormat}
 * provides keys and values of type {@link Key} and {@link Value} to the Map function.
 *
 * The user must specify the following via static configurator methods:
 *
 * <ul>
 * <li>{@link AccumuloInputFormat#setConnectorInfo(JobConf, String, AuthenticationToken)}
 * <li>{@link AccumuloInputFormat#setConnectorInfo(JobConf, String, String)}
 * <li>{@link AccumuloInputFormat#setScanAuthorizations(JobConf, Authorizations)}
 * <li>{@link AccumuloInputFormat#setZooKeeperInstance(JobConf, ClientConfiguration)}
 * </ul>
 *
 * Other static methods are optional.
 */
public class AccumuloInputFormat extends InputFormatBase<Key,Value> {

  @Override
  public RecordReader<Key,Value> getRecordReader(InputSplit split, JobConf job, Reporter reporter)
      throws IOException {
    log.setLevel(getLogLevel(job));

    // Override the log level from the configuration as if the RangeInputSplit has one it's the more
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

    RecordReaderBase<Key,Value> recordReader = new RecordReaderBase<Key,Value>() {

      @Override
      public boolean next(Key key, Value value) throws IOException {
        if (scannerIterator.hasNext()) {
          ++numKeysRead;
          Entry<Key,Value> entry = scannerIterator.next();
          key.set(currentKey = entry.getKey());
          value.set(entry.getValue().get());
          if (log.isTraceEnabled())
            log.trace("Processing key/value pair: " + DefaultFormatter.formatEntry(entry, true));
          return true;
        }
        return false;
      }

      @Override
      public Key createKey() {
        return new Key();
      }

      @Override
      public Value createValue() {
        return new Value();
      }

    };
    recordReader.initialize(split, job);
    return recordReader;
  }
}
