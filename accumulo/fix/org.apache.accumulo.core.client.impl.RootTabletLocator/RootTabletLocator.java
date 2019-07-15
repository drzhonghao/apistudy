

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.TabletLocator;
import org.apache.accumulo.core.client.impl.TabletLocator.TabletServerMutations;
import org.apache.accumulo.core.client.impl.TabletLocatorImpl;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.metadata.RootTable;
import org.apache.accumulo.core.util.OpTimer;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.accumulo.fate.zookeeper.ZooCache;
import org.apache.accumulo.fate.zookeeper.ZooCacheFactory;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RootTabletLocator extends TabletLocator {
	private final TabletLocatorImpl.TabletServerLockChecker lockChecker;

	private final ZooCacheFactory zcf;

	RootTabletLocator(TabletLocatorImpl.TabletServerLockChecker lockChecker) {
		this(lockChecker, new ZooCacheFactory());
	}

	RootTabletLocator(TabletLocatorImpl.TabletServerLockChecker lockChecker, ZooCacheFactory zcf) {
		this.lockChecker = lockChecker;
		this.zcf = zcf;
	}

	@Override
	public <T extends Mutation> void binMutations(ClientContext context, List<T> mutations, Map<String, TabletLocator.TabletServerMutations<T>> binnedMutations, List<T> failures) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		TabletLocator.TabletLocation rootTabletLocation = getRootTabletLocation(context);
		if (rootTabletLocation != null) {
			TabletLocator.TabletServerMutations<T> tsm = new TabletLocator.TabletServerMutations<>(rootTabletLocation.tablet_session);
			for (T mutation : mutations) {
				tsm.addMutation(RootTable.EXTENT, mutation);
			}
			binnedMutations.put(rootTabletLocation.tablet_location, tsm);
		}else {
			failures.addAll(mutations);
		}
	}

	@Override
	public List<Range> binRanges(ClientContext context, List<Range> ranges, Map<String, Map<KeyExtent, List<Range>>> binnedRanges) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		TabletLocator.TabletLocation rootTabletLocation = getRootTabletLocation(context);
		if (rootTabletLocation != null) {
			for (Range range : ranges) {
			}
			return Collections.emptyList();
		}
		return ranges;
	}

	@Override
	public void invalidateCache(KeyExtent failedExtent) {
	}

	@Override
	public void invalidateCache(Collection<KeyExtent> keySet) {
	}

	@Override
	public void invalidateCache(Instance instance, String server) {
		ZooCache zooCache = zcf.getZooCache(instance.getZooKeepers(), instance.getZooKeepersSessionTimeOut());
		String root = (ZooUtil.getRoot(instance)) + (Constants.ZTSERVERS);
		zooCache.clear(((root + "/") + server));
	}

	@Override
	public void invalidateCache() {
	}

	protected TabletLocator.TabletLocation getRootTabletLocation(ClientContext context) {
		Instance instance = context.getInstance();
		String zRootLocPath = (ZooUtil.getRoot(instance)) + (RootTable.ZROOT_TABLET_LOCATION);
		ZooCache zooCache = zcf.getZooCache(instance.getZooKeepers(), instance.getZooKeepersSessionTimeOut());
		Logger log = LoggerFactory.getLogger(this.getClass());
		OpTimer timer = null;
		if (log.isTraceEnabled()) {
			log.trace("tid={} Looking up root tablet location in zookeeper.", Thread.currentThread().getId());
			timer = new OpTimer().start();
		}
		byte[] loc = zooCache.get(zRootLocPath);
		if (timer != null) {
			timer.stop();
			log.trace("tid={} Found root tablet at {} in {}", Thread.currentThread().getId(), (loc == null ? "null" : new String(loc)));
		}
		if (loc == null) {
			return null;
		}
		String[] tokens = new String(loc).split("\\|");
		if (lockChecker.isLockHeld(tokens[0], tokens[1]))
			return new TabletLocator.TabletLocation(RootTable.EXTENT, tokens[0], tokens[1]);
		else
			return null;

	}

	@Override
	public TabletLocator.TabletLocation locateTablet(ClientContext context, Text row, boolean skipRow, boolean retry) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		TabletLocator.TabletLocation location = getRootTabletLocation(context);
		while (retry && (location == null)) {
			UtilWaitThread.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
			location = getRootTabletLocation(context);
		} 
		return location;
	}
}

