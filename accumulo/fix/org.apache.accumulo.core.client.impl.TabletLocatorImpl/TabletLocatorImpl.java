

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.TabletLocator;
import org.apache.accumulo.core.client.impl.TabletLocator.TabletLocation;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.PartialKey;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.util.OpTimer;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.core.util.TextUtil;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.hadoop.io.BinaryComparable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TabletLocatorImpl extends TabletLocator {
	private static final Logger log = LoggerFactory.getLogger(TabletLocatorImpl.class);

	static final Text MAX_TEXT = new Text();

	private static class EndRowComparator implements Serializable , Comparator<Text> {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(Text o1, Text o2) {
			int ret;
			if (o1 == (TabletLocatorImpl.MAX_TEXT))
				if (o2 == (TabletLocatorImpl.MAX_TEXT))
					ret = 0;
				else
					ret = 1;

			else
				if (o2 == (TabletLocatorImpl.MAX_TEXT))
					ret = -1;
				else
					ret = o1.compareTo(o2);


			return ret;
		}
	}

	static final TabletLocatorImpl.EndRowComparator endRowComparator = new TabletLocatorImpl.EndRowComparator();

	protected String tableId;

	protected TabletLocator parent;

	protected TreeMap<Text, TabletLocator.TabletLocation> metaCache = new TreeMap<>(TabletLocatorImpl.endRowComparator);

	protected TabletLocatorImpl.TabletLocationObtainer locationObtainer;

	private TabletLocatorImpl.TabletServerLockChecker lockChecker;

	protected Text lastTabletRow;

	private TreeSet<KeyExtent> badExtents = new TreeSet<>();

	private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

	private final Lock rLock = rwLock.readLock();

	private final Lock wLock = rwLock.writeLock();

	public interface TabletLocationObtainer {
		TabletLocator.TabletLocations lookupTablet(ClientContext context, TabletLocator.TabletLocation src, Text row, Text stopRow, TabletLocator parent) throws AccumuloException, AccumuloSecurityException;

		List<TabletLocator.TabletLocation> lookupTablets(ClientContext context, String tserver, Map<KeyExtent, List<Range>> map, TabletLocator parent) throws AccumuloException, AccumuloSecurityException;
	}

	public static interface TabletServerLockChecker {
		boolean isLockHeld(String tserver, String session);

		void invalidateCache(String server);
	}

	private class LockCheckerSession {
		private HashSet<Pair<String, String>> okLocks = new HashSet<>();

		private HashSet<Pair<String, String>> invalidLocks = new HashSet<>();

		private TabletLocator.TabletLocation checkLock(TabletLocator.TabletLocation tl) {
			if (tl == null)
				return null;

			Pair<String, String> lock = new Pair<>(tl.tablet_location, tl.tablet_session);
			if (okLocks.contains(lock))
				return tl;

			if (invalidLocks.contains(lock))
				return null;

			if (lockChecker.isLockHeld(tl.tablet_location, tl.tablet_session)) {
				okLocks.add(lock);
				return tl;
			}
			if (TabletLocatorImpl.log.isTraceEnabled())
				TabletLocatorImpl.log.trace("Tablet server {} {} no longer holds its lock", tl.tablet_location, tl.tablet_session);

			invalidLocks.add(lock);
			return null;
		}
	}

	public TabletLocatorImpl(String tableId, TabletLocator parent, TabletLocatorImpl.TabletLocationObtainer tlo, TabletLocatorImpl.TabletServerLockChecker tslc) {
		this.tableId = tableId;
		this.parent = parent;
		this.locationObtainer = tlo;
		this.lockChecker = tslc;
		this.lastTabletRow = new Text(tableId);
		lastTabletRow.append(new byte[]{ '<' }, 0, 1);
	}

	@Override
	public <T extends Mutation> void binMutations(ClientContext context, List<T> mutations, Map<String, TabletLocator.TabletServerMutations<T>> binnedMutations, List<T> failures) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		OpTimer timer = null;
		if (TabletLocatorImpl.log.isTraceEnabled()) {
			TabletLocatorImpl.log.trace("tid={} Binning {} mutations for table {}", Thread.currentThread().getId(), mutations.size());
			timer = new OpTimer().start();
		}
		ArrayList<T> notInCache = new ArrayList<>();
		Text row = new Text();
		TabletLocatorImpl.LockCheckerSession lcSession = new TabletLocatorImpl.LockCheckerSession();
		rLock.lock();
		try {
			processInvalidated(context, lcSession);
			for (T mutation : mutations) {
				row.set(mutation.getRow());
				TabletLocator.TabletLocation tl = locateTabletInCache(row);
				if ((tl == null) || (!(addMutation(binnedMutations, mutation, tl, lcSession))))
					notInCache.add(mutation);

			}
		} finally {
			rLock.unlock();
		}
		if ((notInCache.size()) > 0) {
			Collections.sort(notInCache, new Comparator<Mutation>() {
				@Override
				public int compare(Mutation o1, Mutation o2) {
					return WritableComparator.compareBytes(o1.getRow(), 0, o1.getRow().length, o2.getRow(), 0, o2.getRow().length);
				}
			});
			wLock.lock();
			try {
				boolean failed = false;
				for (T mutation : notInCache) {
					if (failed) {
						failures.add(mutation);
						continue;
					}
					row.set(mutation.getRow());
					TabletLocator.TabletLocation tl = _locateTablet(context, row, false, false, false, lcSession);
					if ((tl == null) || (!(addMutation(binnedMutations, mutation, tl, lcSession)))) {
						failures.add(mutation);
						failed = true;
					}
				}
			} finally {
				wLock.unlock();
			}
		}
		if (timer != null) {
			timer.stop();
			TabletLocatorImpl.log.trace("tid={} Binned {} mutations for table {} to {} tservers in {}");
		}
	}

	private <T extends Mutation> boolean addMutation(Map<String, TabletLocator.TabletServerMutations<T>> binnedMutations, T mutation, TabletLocator.TabletLocation tl, TabletLocatorImpl.LockCheckerSession lcSession) {
		TabletLocator.TabletServerMutations<T> tsm = binnedMutations.get(tl.tablet_location);
		if (tsm == null) {
			boolean lockHeld = (lcSession.checkLock(tl)) != null;
			if (lockHeld) {
				tsm = new TabletLocator.TabletServerMutations<>(tl.tablet_session);
				binnedMutations.put(tl.tablet_location, tsm);
			}else {
				return false;
			}
		}
		return false;
	}

	private List<Range> binRanges(ClientContext context, List<Range> ranges, Map<String, Map<KeyExtent, List<Range>>> binnedRanges, boolean useCache, TabletLocatorImpl.LockCheckerSession lcSession) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		List<Range> failures = new ArrayList<>();
		List<TabletLocator.TabletLocation> tabletLocations = new ArrayList<>();
		boolean lookupFailed = false;
		l1 : for (Range range : ranges) {
			tabletLocations.clear();
			Text startRow;
			if ((range.getStartKey()) != null) {
				startRow = range.getStartKey().getRow();
			}else
				startRow = new Text();

			TabletLocator.TabletLocation tl = null;
			if (useCache)
				tl = lcSession.checkLock(locateTabletInCache(startRow));
			else
				if (!lookupFailed)
					tl = _locateTablet(context, startRow, false, false, false, lcSession);


			if (tl == null) {
				failures.add(range);
				if (!useCache)
					lookupFailed = true;

				continue;
			}
			tabletLocations.add(tl);
			while (((tl.tablet_extent.getEndRow()) != null) && (!(range.afterEndKey(new Key(tl.tablet_extent.getEndRow()).followingKey(PartialKey.ROW))))) {
				if (useCache) {
					Text row = new Text(tl.tablet_extent.getEndRow());
					row.append(new byte[]{ 0 }, 0, 1);
					tl = lcSession.checkLock(locateTabletInCache(row));
				}else {
					tl = _locateTablet(context, tl.tablet_extent.getEndRow(), true, false, false, lcSession);
				}
				if (tl == null) {
					failures.add(range);
					if (!useCache)
						lookupFailed = true;

					continue l1;
				}
				tabletLocations.add(tl);
			} 
			for (TabletLocator.TabletLocation tl2 : tabletLocations) {
				TabletLocatorImpl.addRange(binnedRanges, tl2.tablet_location, tl2.tablet_extent, range);
			}
		}
		return failures;
	}

	@Override
	public List<Range> binRanges(ClientContext context, List<Range> ranges, Map<String, Map<KeyExtent, List<Range>>> binnedRanges) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		OpTimer timer = null;
		if (TabletLocatorImpl.log.isTraceEnabled()) {
			TabletLocatorImpl.log.trace("tid={} Binning {} ranges for table {}", Thread.currentThread().getId(), ranges.size());
			timer = new OpTimer().start();
		}
		TabletLocatorImpl.LockCheckerSession lcSession = new TabletLocatorImpl.LockCheckerSession();
		List<Range> failures;
		rLock.lock();
		try {
			processInvalidated(context, lcSession);
			failures = binRanges(context, ranges, binnedRanges, true, lcSession);
		} finally {
			rLock.unlock();
		}
		if ((failures.size()) > 0) {
			Collections.sort(failures);
			wLock.lock();
			try {
				failures = binRanges(context, failures, binnedRanges, false, lcSession);
			} finally {
				wLock.unlock();
			}
		}
		if (timer != null) {
			timer.stop();
			TabletLocatorImpl.log.trace("tid={} Binned {} ranges for table {} to {} tservers in {}");
		}
		return failures;
	}

	@Override
	public void invalidateCache(KeyExtent failedExtent) {
		wLock.lock();
		try {
			badExtents.add(failedExtent);
		} finally {
			wLock.unlock();
		}
		if (TabletLocatorImpl.log.isTraceEnabled())
			TabletLocatorImpl.log.trace("Invalidated extent={}", failedExtent);

	}

	@Override
	public void invalidateCache(Collection<KeyExtent> keySet) {
		wLock.lock();
		try {
			badExtents.addAll(keySet);
		} finally {
			wLock.unlock();
		}
		if (TabletLocatorImpl.log.isTraceEnabled())
			TabletLocatorImpl.log.trace("Invalidated {} cache entries for table {}", keySet.size(), tableId);

	}

	@Override
	public void invalidateCache(Instance instance, String server) {
		int invalidatedCount = 0;
		wLock.lock();
		try {
			for (TabletLocator.TabletLocation cacheEntry : metaCache.values())
				if (cacheEntry.tablet_location.equals(server)) {
					badExtents.add(cacheEntry.tablet_extent);
					invalidatedCount++;
				}

		} finally {
			wLock.unlock();
		}
		lockChecker.invalidateCache(server);
		if (TabletLocatorImpl.log.isTraceEnabled())
			TabletLocatorImpl.log.trace("invalidated {} cache entries  table={} server={}", invalidatedCount, tableId);

	}

	@Override
	public void invalidateCache() {
		int invalidatedCount;
		wLock.lock();
		try {
			invalidatedCount = metaCache.size();
			metaCache.clear();
		} finally {
			wLock.unlock();
		}
		if (TabletLocatorImpl.log.isTraceEnabled())
			TabletLocatorImpl.log.trace("invalidated all {} cache entries for table={}", invalidatedCount, tableId);

	}

	@Override
	public TabletLocator.TabletLocation locateTablet(ClientContext context, Text row, boolean skipRow, boolean retry) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		OpTimer timer = null;
		if (TabletLocatorImpl.log.isTraceEnabled()) {
			TabletLocatorImpl.log.trace("tid={} Locating tablet  table={} row={} skipRow={} retry={}");
			timer = new OpTimer().start();
		}
		while (true) {
			TabletLocatorImpl.LockCheckerSession lcSession = new TabletLocatorImpl.LockCheckerSession();
			TabletLocator.TabletLocation tl = _locateTablet(context, row, skipRow, retry, true, lcSession);
			if (retry && (tl == null)) {
				UtilWaitThread.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
				if (TabletLocatorImpl.log.isTraceEnabled())
					TabletLocatorImpl.log.trace("Failed to locate tablet containing row {} in table {}, will retry...", TextUtil.truncate(row), tableId);

				continue;
			}
			if (timer != null) {
				timer.stop();
			}
			return tl;
		} 
	}

	private void lookupTabletLocation(ClientContext context, Text row, boolean retry, TabletLocatorImpl.LockCheckerSession lcSession) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Text metadataRow = new Text(tableId);
		metadataRow.append(new byte[]{ ';' }, 0, 1);
		metadataRow.append(row.getBytes(), 0, row.getLength());
		TabletLocator.TabletLocation ptl = parent.locateTablet(context, metadataRow, false, retry);
		if (ptl != null) {
			TabletLocator.TabletLocations locations = locationObtainer.lookupTablet(context, ptl, metadataRow, lastTabletRow, parent);
			while (((locations != null) && (locations.getLocations().isEmpty())) && (locations.getLocationless().isEmpty())) {
				Text er = ptl.tablet_extent.getEndRow();
				if ((er != null) && ((er.compareTo(lastTabletRow)) < 0)) {
					ptl = parent.locateTablet(context, er, true, retry);
					if (ptl != null)
						locations = locationObtainer.lookupTablet(context, ptl, metadataRow, lastTabletRow, parent);
					else
						break;

				}else {
					break;
				}
			} 
			if (locations == null)
				return;

			Text lastEndRow = null;
			for (TabletLocator.TabletLocation tabletLocation : locations.getLocations()) {
				KeyExtent ke = tabletLocation.tablet_extent;
				TabletLocator.TabletLocation locToCache;
				if (((lastEndRow != null) && ((ke.getPrevEndRow()) != null)) && (ke.getPrevEndRow().equals(lastEndRow))) {
					locToCache = new TabletLocator.TabletLocation(new KeyExtent(ke.getTableId(), ke.getEndRow(), lastEndRow), tabletLocation.tablet_location, tabletLocation.tablet_session);
				}else {
					locToCache = tabletLocation;
				}
				lastEndRow = locToCache.tablet_extent.getEndRow();
				updateCache(locToCache, lcSession);
			}
		}
	}

	private void updateCache(TabletLocator.TabletLocation tabletLocation, TabletLocatorImpl.LockCheckerSession lcSession) {
		if (!(tabletLocation.tablet_extent.getTableId().equals(tableId))) {
			throw new IllegalStateException(((("Unexpected extent returned " + (tableId)) + "  ") + (tabletLocation.tablet_extent)));
		}
		if ((tabletLocation.tablet_location) == null) {
			throw new IllegalStateException(((("Cannot add null locations to cache " + (tableId)) + "  ") + (tabletLocation.tablet_extent)));
		}
		if (!(tabletLocation.tablet_extent.getTableId().equals(tableId))) {
			throw new IllegalStateException(((("Cannot add other table ids to locations cache " + (tableId)) + "  ") + (tabletLocation.tablet_extent)));
		}
		TabletLocatorImpl.removeOverlapping(metaCache, tabletLocation.tablet_extent);
		if ((lcSession.checkLock(tabletLocation)) == null)
			return;

		Text er = tabletLocation.tablet_extent.getEndRow();
		if (er == null)
			er = TabletLocatorImpl.MAX_TEXT;

		metaCache.put(er, tabletLocation);
		if ((badExtents.size()) > 0)
			TabletLocatorImpl.removeOverlapping(badExtents, tabletLocation.tablet_extent);

	}

	static void removeOverlapping(TreeMap<Text, TabletLocator.TabletLocation> metaCache, KeyExtent nke) {
		Iterator<Map.Entry<Text, TabletLocator.TabletLocation>> iter = null;
		if ((nke.getPrevEndRow()) == null) {
			iter = metaCache.entrySet().iterator();
		}else {
			Text row = TabletLocatorImpl.rowAfterPrevRow(nke);
			SortedMap<Text, TabletLocator.TabletLocation> tailMap = metaCache.tailMap(row);
			iter = tailMap.entrySet().iterator();
		}
		while (iter.hasNext()) {
			Map.Entry<Text, TabletLocator.TabletLocation> entry = iter.next();
			KeyExtent ke = entry.getValue().tablet_extent;
			if (TabletLocatorImpl.stopRemoving(nke, ke)) {
				break;
			}
			iter.remove();
		} 
	}

	private static boolean stopRemoving(KeyExtent nke, KeyExtent ke) {
		return (((ke.getPrevEndRow()) != null) && ((nke.getEndRow()) != null)) && ((ke.getPrevEndRow().compareTo(nke.getEndRow())) >= 0);
	}

	private static Text rowAfterPrevRow(KeyExtent nke) {
		Text row = new Text(nke.getPrevEndRow());
		row.append(new byte[]{ 0 }, 0, 1);
		return row;
	}

	static void removeOverlapping(TreeSet<KeyExtent> extents, KeyExtent nke) {
		for (KeyExtent overlapping : KeyExtent.findOverlapping(nke, extents)) {
			extents.remove(overlapping);
		}
	}

	private TabletLocator.TabletLocation locateTabletInCache(Text row) {
		Map.Entry<Text, TabletLocator.TabletLocation> entry = metaCache.ceilingEntry(row);
		if (entry != null) {
			KeyExtent ke = entry.getValue().tablet_extent;
			if (((ke.getPrevEndRow()) == null) || ((ke.getPrevEndRow().compareTo(row)) < 0)) {
				return entry.getValue();
			}
		}
		return null;
	}

	protected TabletLocator.TabletLocation _locateTablet(ClientContext context, Text row, boolean skipRow, boolean retry, boolean lock, TabletLocatorImpl.LockCheckerSession lcSession) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (skipRow) {
			row = new Text(row);
			row.append(new byte[]{ 0 }, 0, 1);
		}
		TabletLocator.TabletLocation tl;
		if (lock) {
			rLock.lock();
			try {
				tl = processInvalidatedAndCheckLock(context, lcSession, row);
			} finally {
				rLock.unlock();
			}
		}else {
			tl = processInvalidatedAndCheckLock(context, lcSession, row);
		}
		if (tl == null) {
			if (lock) {
				wLock.lock();
				try {
					tl = lookupTabletLocationAndCheckLock(context, row, retry, lcSession);
				} finally {
					wLock.unlock();
				}
			}else {
				tl = lookupTabletLocationAndCheckLock(context, row, retry, lcSession);
			}
		}
		return tl;
	}

	private TabletLocator.TabletLocation lookupTabletLocationAndCheckLock(ClientContext context, Text row, boolean retry, TabletLocatorImpl.LockCheckerSession lcSession) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		lookupTabletLocation(context, row, retry, lcSession);
		return lcSession.checkLock(locateTabletInCache(row));
	}

	private TabletLocator.TabletLocation processInvalidatedAndCheckLock(ClientContext context, TabletLocatorImpl.LockCheckerSession lcSession, Text row) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		processInvalidated(context, lcSession);
		return lcSession.checkLock(locateTabletInCache(row));
	}

	private void processInvalidated(ClientContext context, TabletLocatorImpl.LockCheckerSession lcSession) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if ((badExtents.size()) == 0)
			return;

		final boolean writeLockHeld = rwLock.isWriteLockedByCurrentThread();
		try {
			if (!writeLockHeld) {
				rLock.unlock();
				wLock.lock();
				if ((badExtents.size()) == 0)
					return;

			}
			List<Range> lookups = new ArrayList<>(badExtents.size());
			for (KeyExtent be : badExtents) {
				lookups.add(be.toMetadataRange());
				TabletLocatorImpl.removeOverlapping(metaCache, be);
			}
			lookups = Range.mergeOverlapping(lookups);
			Map<String, Map<KeyExtent, List<Range>>> binnedRanges = new HashMap<>();
			parent.binRanges(context, lookups, binnedRanges);
			ArrayList<String> tabletServers = new ArrayList<>(binnedRanges.keySet());
			Collections.shuffle(tabletServers);
			for (String tserver : tabletServers) {
				List<TabletLocator.TabletLocation> locations = locationObtainer.lookupTablets(context, tserver, binnedRanges.get(tserver), parent);
				for (TabletLocator.TabletLocation tabletLocation : locations) {
					updateCache(tabletLocation, lcSession);
				}
			}
		} finally {
			if (!writeLockHeld) {
				rLock.lock();
				wLock.unlock();
			}
		}
	}

	protected static void addRange(Map<String, Map<KeyExtent, List<Range>>> binnedRanges, String location, KeyExtent ke, Range range) {
		Map<KeyExtent, List<Range>> tablets = binnedRanges.get(location);
		if (tablets == null) {
			tablets = new HashMap<>();
			binnedRanges.put(location, tablets);
		}
		List<Range> tabletsRanges = tablets.get(ke);
		if (tabletsRanges == null) {
			tabletsRanges = new ArrayList<>();
			tablets.put(ke, tabletsRanges);
		}
		tabletsRanges.add(range);
	}
}

