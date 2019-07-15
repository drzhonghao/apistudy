

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.TabletLocator;
import org.apache.accumulo.core.client.impl.TabletLocator.TabletServerMutations;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;


public class SyncingTabletLocator extends TabletLocator {
	private static final Logger log = Logger.getLogger(SyncingTabletLocator.class);

	private volatile TabletLocator locator;

	private final Callable<TabletLocator> getLocatorFunction;

	public SyncingTabletLocator(Callable<TabletLocator> getLocatorFunction) {
		this.getLocatorFunction = getLocatorFunction;
		try {
			this.locator = getLocatorFunction.call();
		} catch (Exception e) {
			SyncingTabletLocator.log.error("Problem obtaining TabletLocator", e);
			throw new RuntimeException(e);
		}
	}

	public SyncingTabletLocator(final ClientContext context, final String tableId) {
		this(new Callable<TabletLocator>() {
			@Override
			public TabletLocator call() throws Exception {
				return TabletLocator.getLocator(context, tableId);
			}
		});
	}

	private TabletLocator syncLocator() {
		TabletLocator loc = this.locator;
		return loc;
	}

	@Override
	public TabletLocator.TabletLocation locateTablet(ClientContext context, Text row, boolean skipRow, boolean retry) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		return syncLocator().locateTablet(context, row, skipRow, retry);
	}

	@Override
	public <T extends Mutation> void binMutations(ClientContext context, List<T> mutations, Map<String, TabletLocator.TabletServerMutations<T>> binnedMutations, List<T> failures) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		syncLocator().binMutations(context, mutations, binnedMutations, failures);
	}

	@Override
	public List<Range> binRanges(ClientContext context, List<Range> ranges, Map<String, Map<KeyExtent, List<Range>>> binnedRanges) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		return syncLocator().binRanges(context, ranges, binnedRanges);
	}

	@Override
	public void invalidateCache(KeyExtent failedExtent) {
		syncLocator().invalidateCache(failedExtent);
	}

	@Override
	public void invalidateCache(Collection<KeyExtent> keySet) {
		syncLocator().invalidateCache(keySet);
	}

	@Override
	public void invalidateCache() {
		syncLocator().invalidateCache();
	}

	@Override
	public void invalidateCache(Instance instance, String server) {
		syncLocator().invalidateCache(instance, server);
	}
}

