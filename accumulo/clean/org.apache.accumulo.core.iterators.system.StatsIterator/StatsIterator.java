import org.apache.accumulo.core.iterators.system.*;


import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.WrappingIterator;

/**
 *
 */
public class StatsIterator extends WrappingIterator {

  private int numRead = 0;
  private AtomicLong seekCounter;
  private AtomicLong readCounter;

  public StatsIterator(SortedKeyValueIterator<Key,Value> source, AtomicLong seekCounter,
      AtomicLong readCounter) {
    super.setSource(source);
    this.seekCounter = seekCounter;
    this.readCounter = readCounter;
  }

  @Override
  public void next() throws IOException {
    super.next();
    numRead++;

    if (numRead % 23 == 0) {
      readCounter.addAndGet(numRead);
      numRead = 0;
    }
  }

  @Override
  public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment env) {
    return new StatsIterator(getSource().deepCopy(env), seekCounter, readCounter);
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive)
      throws IOException {
    super.seek(range, columnFamilies, inclusive);
    seekCounter.incrementAndGet();
    readCounter.addAndGet(numRead);
    numRead = 0;
  }

  public void report() {
    readCounter.addAndGet(numRead);
    numRead = 0;
  }
}
