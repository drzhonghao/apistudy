import org.apache.lucene.replicator.nrt.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.StandardDirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;

/** A SearcherManager that refreshes via an externally provided (NRT) SegmentInfos, either from {@link IndexWriter} or via
 *  nrt replication to another index.
 *
 * @lucene.experimental */
class SegmentInfosSearcherManager extends ReferenceManager<IndexSearcher> {
  private volatile SegmentInfos currentInfos;
  private final Directory dir;
  private final Node node;
  private final AtomicInteger openReaderCount = new AtomicInteger();
  private final SearcherFactory searcherFactory;

  public SegmentInfosSearcherManager(Directory dir, Node node, SegmentInfos infosIn, SearcherFactory searcherFactory) throws IOException {
    this.dir = dir;
    this.node = node;
    if (searcherFactory == null) {
      searcherFactory = new SearcherFactory();
    }
    this.searcherFactory = searcherFactory;
    currentInfos = infosIn;
    node.message("SegmentInfosSearcherManager.init: use incoming infos=" + infosIn.toString());
    current = SearcherManager.getSearcher(searcherFactory, StandardDirectoryReader.open(dir, currentInfos, null), null);
    addReaderClosedListener(current.getIndexReader());
  }

  @Override
  protected int getRefCount(IndexSearcher s) {
    return s.getIndexReader().getRefCount();
  }

  @Override
  protected boolean tryIncRef(IndexSearcher s) {
    return s.getIndexReader().tryIncRef();
  }

  @Override
  protected void decRef(IndexSearcher s) throws IOException {
    s.getIndexReader().decRef();
  }

  public SegmentInfos getCurrentInfos() {
    return currentInfos;
  }

  /** Switch to new segments, refreshing if necessary.  Note that it's the caller job to ensure there's a held refCount for the
   *  incoming infos, so all files exist. */
  public void setCurrentInfos(SegmentInfos infos) throws IOException {
    if (currentInfos != null) {
      // So that if we commit, we will go to the next
      // (unwritten so far) generation:
      infos.updateGeneration(currentInfos);
      node.message("mgr.setCurrentInfos: carry over infos gen=" + infos.getSegmentsFileName());
    }
    currentInfos = infos;
    maybeRefresh();
  }

  @Override
  protected IndexSearcher refreshIfNeeded(IndexSearcher old) throws IOException {
    List<LeafReader> subs;
    if (old == null) {
      subs = null;
    } else {
      subs = new ArrayList<>();
      for(LeafReaderContext ctx : old.getIndexReader().leaves()) {
        subs.add(ctx.reader());
      }
    }

    // Open a new reader, sharing any common segment readers with the old one:
    DirectoryReader r = StandardDirectoryReader.open(dir, currentInfos, subs);
    addReaderClosedListener(r);
    node.message("refreshed to version=" + currentInfos.getVersion() + " r=" + r);
    return SearcherManager.getSearcher(searcherFactory, r, (DirectoryReader) old.getIndexReader());
  }

  private void addReaderClosedListener(IndexReader r) {
    IndexReader.CacheHelper cacheHelper = r.getReaderCacheHelper();
    if (cacheHelper == null) {
      throw new IllegalStateException("StandardDirectoryReader must support caching");
    }
    openReaderCount.incrementAndGet();
    cacheHelper.addClosedListener(new IndexReader.ClosedListener() {
        @Override
        public void onClose(IndexReader.CacheKey cacheKey) {
          onReaderClosed();
        }
      });
  }

  /** Tracks how many readers are still open, so that when we are closed,
   *  we can additionally wait until all in-flight searchers are
   *  closed. */
  synchronized void onReaderClosed() {
    if (openReaderCount.decrementAndGet() == 0) {
      notifyAll();
    }
  }
}
