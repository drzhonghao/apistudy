import org.apache.accumulo.tserver.log.RecoveryLogReader;
import org.apache.accumulo.tserver.log.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.tserver.logger.LogFileKey;
import org.apache.accumulo.tserver.logger.LogFileValue;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

/**
 * Iterates over multiple sorted recovery logs merging them into a single sorted stream.
 */
public class RecoveryLogsIterator implements CloseableIterator<Entry<LogFileKey,LogFileValue>> {

  private static final Logger LOG = LoggerFactory.getLogger(RecoveryLogsIterator.class);

  List<CloseableIterator<Entry<LogFileKey,LogFileValue>>> iterators;
  private UnmodifiableIterator<Entry<LogFileKey,LogFileValue>> iter;

  /**
   * Iterates only over keys in the range [start,end].
   */
  RecoveryLogsIterator(VolumeManager fs, List<Path> recoveryLogPaths, LogFileKey start,
      LogFileKey end) throws IOException {

    iterators = new ArrayList<>(recoveryLogPaths.size());

    try {
      for (Path log : recoveryLogPaths) {
        iterators.add(new RecoveryLogReader(fs, log, start, end));
      }

      iter = Iterators.mergeSorted(iterators, new Comparator<Entry<LogFileKey,LogFileValue>>() {
        @Override
        public int compare(Entry<LogFileKey,LogFileValue> o1, Entry<LogFileKey,LogFileValue> o2) {
          return o1.getKey().compareTo(o2.getKey());
        }
      });

    } catch (RuntimeException | IOException e) {
      try {
        close();
      } catch (Exception e2) {
        e.addSuppressed(e2);
      }
      throw e;
    }
  }

  @Override
  public boolean hasNext() {
    return iter.hasNext();
  }

  @Override
  public Entry<LogFileKey,LogFileValue> next() {
    return iter.next();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove");
  }

  @Override
  public void close() {
    for (CloseableIterator<?> reader : iterators) {
      try {
        reader.close();
      } catch (IOException e) {
        LOG.debug("Failed to close reader", e);
      }
    }
  }
}
