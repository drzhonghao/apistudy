import org.apache.accumulo.core.client.mapred.*;


import java.io.IOException;
import java.util.Map.Entry;

import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.RowIterator;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.PeekingIterator;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

/**
 * This class allows MapReduce jobs to use Accumulo as the source of data. This {@link InputFormat}
 * provides row names as {@link Text} as keys, and a corresponding {@link PeekingIterator} as a
 * value, which in turn makes the {@link Key}/{@link Value} pairs for that row available to the Map
 * function.
 *
 * The user must specify the following via static configurator methods:
 *
 * <ul>
 * <li>{@link AccumuloRowInputFormat#setConnectorInfo(JobConf, String, AuthenticationToken)}
 * <li>{@link AccumuloRowInputFormat#setInputTableName(JobConf, String)}
 * <li>{@link AccumuloRowInputFormat#setScanAuthorizations(JobConf, Authorizations)}
 * <li>{@link AccumuloRowInputFormat#setZooKeeperInstance(JobConf, ClientConfiguration)}
 * </ul>
 *
 * Other static methods are optional.
 */
public class AccumuloRowInputFormat
    extends InputFormatBase<Text,PeekingIterator<Entry<Key,Value>>> {
  @Override
  public RecordReader<Text,PeekingIterator<Entry<Key,Value>>> getRecordReader(InputSplit split,
      JobConf job, Reporter reporter) throws IOException {
    log.setLevel(getLogLevel(job));
    // @formatter:off
    RecordReaderBase<Text,PeekingIterator<Entry<Key,Value>>> recordReader =
      new RecordReaderBase<Text,PeekingIterator<Entry<Key,Value>>>() {
    // @formatter:on
          RowIterator rowIterator;

          @Override
          public void initialize(InputSplit inSplit, JobConf job) throws IOException {
            super.initialize(inSplit, job);
            rowIterator = new RowIterator(scannerIterator);
          }

          @Override
          public boolean next(Text key, PeekingIterator<Entry<Key,Value>> value)
              throws IOException {
            if (!rowIterator.hasNext())
              return false;
            value.initialize(rowIterator.next());
            numKeysRead = rowIterator.getKVCount();
            key.set((currentKey = value.peek().getKey()).getRow());
            return true;
          }

          @Override
          public Text createKey() {
            return new Text();
          }

          @Override
          public PeekingIterator<Entry<Key,Value>> createValue() {
            return new PeekingIterator<>();
          }
        };
    recordReader.initialize(split, job);
    return recordReader;
  }
}
