import org.apache.accumulo.fate.TStore;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.fate.ReadOnlyRepo;
import org.apache.accumulo.fate.*;


import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This store removes Repos, in the store it wraps, that are in a finished or new state for more
 * than a configurable time period.
 *
 * No external time source is used. It starts tracking idle time when its created.
 *
 */
public class AgeOffStore<T> implements TStore<T> {

  public interface TimeSource {
    long currentTimeMillis();
  }

  final private static Logger log = LoggerFactory.getLogger(AgeOffStore.class);

  private TStore<T> store;
  private Map<Long,Long> candidates;
  private long ageOffTime;
  private long minTime;
  private TimeSource timeSource;

  private synchronized void updateMinTime() {
    minTime = Long.MAX_VALUE;

    for (Long time : candidates.values()) {
      if (time < minTime)
        minTime = time;
    }
  }

  private synchronized void addCandidate(long txid) {
    long time = timeSource.currentTimeMillis();
    candidates.put(txid, time);
    if (time < minTime)
      minTime = time;
  }

  private synchronized void removeCandidate(long txid) {
    Long time = candidates.remove(txid);
    if (time != null && time <= minTime)
      updateMinTime();
  }

  public void ageOff() {
    HashSet<Long> oldTxs = new HashSet<>();

    synchronized (this) {
      long time = timeSource.currentTimeMillis();
      if (minTime < time && time - minTime >= ageOffTime) {
        for (Entry<Long,Long> entry : candidates.entrySet()) {
          if (time - entry.getValue() >= ageOffTime) {
            oldTxs.add(entry.getKey());
          }
        }

        candidates.keySet().removeAll(oldTxs);
        updateMinTime();
      }
    }

    for (Long txid : oldTxs) {
      try {
        store.reserve(txid);
        try {
          switch (store.getStatus(txid)) {
            case NEW:
            case FAILED:
            case SUCCESSFUL:
              store.delete(txid);
              log.debug("Aged off FATE tx " + String.format("%016x", txid));
              break;
            default:
              break;
          }

        } finally {
          store.unreserve(txid, 0);
        }
      } catch (Exception e) {
        log.warn("Failed to age off FATE tx " + String.format("%016x", txid), e);
      }
    }
  }

  public AgeOffStore(TStore<T> store, long ageOffTime, TimeSource timeSource) {
    this.store = store;
    this.ageOffTime = ageOffTime;
    this.timeSource = timeSource;
    candidates = new HashMap<>();

    minTime = Long.MAX_VALUE;

    List<Long> txids = store.list();
    for (Long txid : txids) {
      store.reserve(txid);
      try {
        switch (store.getStatus(txid)) {
          case NEW:
          case FAILED:
          case SUCCESSFUL:
            addCandidate(txid);
            break;
          default:
            break;
        }
      } finally {
        store.unreserve(txid, 0);
      }
    }
  }

  public AgeOffStore(TStore<T> store, long ageOffTime) {
    this(store, ageOffTime, new TimeSource() {
      @Override
      public long currentTimeMillis() {
        return System.currentTimeMillis();
      }
    });
  }

  @Override
  public long create() {
    long txid = store.create();
    addCandidate(txid);
    return txid;
  }

  @Override
  public long reserve() {
    return store.reserve();
  }

  @Override
  public void reserve(long tid) {
    store.reserve(tid);
  }

  @Override
  public void unreserve(long tid, long deferTime) {
    store.unreserve(tid, deferTime);
  }

  @Override
  public Repo<T> top(long tid) {
    return store.top(tid);
  }

  @Override
  public void push(long tid, Repo<T> repo) throws StackOverflowException {
    store.push(tid, repo);
  }

  @Override
  public void pop(long tid) {
    store.pop(tid);
  }

  @Override
  public org.apache.accumulo.fate.TStore.TStatus getStatus(long tid) {
    return store.getStatus(tid);
  }

  @Override
  public void setStatus(long tid, org.apache.accumulo.fate.TStore.TStatus status) {
    store.setStatus(tid, status);

    switch (status) {
      case IN_PROGRESS:
      case FAILED_IN_PROGRESS:
        removeCandidate(tid);
        break;
      case FAILED:
      case SUCCESSFUL:
        addCandidate(tid);
        break;
      default:
        break;
    }
  }

  @Override
  public org.apache.accumulo.fate.TStore.TStatus waitForStatusChange(long tid,
      EnumSet<org.apache.accumulo.fate.TStore.TStatus> expected) {
    return store.waitForStatusChange(tid, expected);
  }

  @Override
  public void setProperty(long tid, String prop, Serializable val) {
    store.setProperty(tid, prop, val);
  }

  @Override
  public Serializable getProperty(long tid, String prop) {
    return store.getProperty(tid, prop);
  }

  @Override
  public void delete(long tid) {
    store.delete(tid);
    removeCandidate(tid);
  }

  @Override
  public List<Long> list() {
    return store.list();
  }

  @Override
  public List<ReadOnlyRepo<T>> getStack(long tid) {
    return store.getStack(tid);
  }
}
