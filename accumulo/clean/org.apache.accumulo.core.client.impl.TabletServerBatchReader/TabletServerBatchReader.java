import org.apache.accumulo.core.client.impl.ScannerOptions;
import org.apache.accumulo.core.client.impl.TabletServerBatchReaderIterator;
import org.apache.accumulo.core.client.impl.*;


import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;

import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.SimpleThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabletServerBatchReader extends ScannerOptions implements BatchScanner {
  private static final Logger log = LoggerFactory.getLogger(TabletServerBatchReader.class);

  private String tableId;
  private int numThreads;
  private ExecutorService queryThreadPool;

  private final ClientContext context;
  private ArrayList<Range> ranges;

  private Authorizations authorizations = Authorizations.EMPTY;
  private Throwable ex = null;

  private static int nextBatchReaderInstance = 1;

  private static synchronized int getNextBatchReaderInstance() {
    return nextBatchReaderInstance++;
  }

  private final int batchReaderInstance = getNextBatchReaderInstance();

  public TabletServerBatchReader(ClientContext context, String tableId,
      Authorizations authorizations, int numQueryThreads) {
    checkArgument(context != null, "context is null");
    checkArgument(tableId != null, "tableId is null");
    checkArgument(authorizations != null, "authorizations is null");
    this.context = context;
    this.authorizations = authorizations;
    this.tableId = tableId;
    this.numThreads = numQueryThreads;

    queryThreadPool = new SimpleThreadPool(numQueryThreads,
        "batch scanner " + batchReaderInstance + "-");

    ranges = null;
    ex = new Throwable();
  }

  @Override
  public void close() {
    queryThreadPool.shutdownNow();
  }

  @Override
  public Authorizations getAuthorizations() {
    return authorizations;
  }

  // WARNING: do not rely upon finalize to close this class. Finalize is not guaranteed to be
  // called.
  @Override
  protected void finalize() {
    if (!queryThreadPool.isShutdown()) {
      log.warn(TabletServerBatchReader.class.getSimpleName()
          + " not shutdown; did you forget to call close()?", ex);
      close();
    }
  }

  @Override
  public void setRanges(Collection<Range> ranges) {
    if (ranges == null || ranges.size() == 0) {
      throw new IllegalArgumentException("ranges must be non null and contain at least 1 range");
    }

    if (queryThreadPool.isShutdown()) {
      throw new IllegalStateException("batch reader closed");
    }

    this.ranges = new ArrayList<>(ranges);

  }

  @Override
  public Iterator<Entry<Key,Value>> iterator() {
    if (ranges == null) {
      throw new IllegalStateException("ranges not set");
    }

    if (queryThreadPool.isShutdown()) {
      throw new IllegalStateException("batch reader closed");
    }

    return new TabletServerBatchReaderIterator(context, tableId, authorizations, ranges, numThreads,
        queryThreadPool, this, timeOut);
  }
}
