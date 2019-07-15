

import com.google.common.base.Preconditions;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.metadata.MetadataLocationObtainer;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.RootTable;
import org.apache.hadoop.io.Text;


public abstract class TabletLocator {
	private volatile boolean isValid = true;

	boolean isValid() {
		return isValid;
	}

	public abstract TabletLocator.TabletLocation locateTablet(ClientContext context, Text row, boolean skipRow, boolean retry) throws AccumuloException, AccumuloSecurityException, TableNotFoundException;

	public abstract <T extends Mutation> void binMutations(ClientContext context, List<T> mutations, Map<String, TabletLocator.TabletServerMutations<T>> binnedMutations, List<T> failures) throws AccumuloException, AccumuloSecurityException, TableNotFoundException;

	public abstract List<Range> binRanges(ClientContext context, List<Range> ranges, Map<String, Map<KeyExtent, List<Range>>> binnedRanges) throws AccumuloException, AccumuloSecurityException, TableNotFoundException;

	public abstract void invalidateCache(KeyExtent failedExtent);

	public abstract void invalidateCache(Collection<KeyExtent> keySet);

	public abstract void invalidateCache();

	public abstract void invalidateCache(Instance instance, String server);

	private static class LocatorKey {
		String instanceId;

		String tableName;

		LocatorKey(String instanceId, String table) {
			this.instanceId = instanceId;
			this.tableName = table;
		}

		@Override
		public int hashCode() {
			return (instanceId.hashCode()) + (tableName.hashCode());
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof TabletLocator.LocatorKey)
				return equals(((TabletLocator.LocatorKey) (o)));

			return false;
		}

		public boolean equals(TabletLocator.LocatorKey lk) {
			return (instanceId.equals(lk.instanceId)) && (tableName.equals(lk.tableName));
		}
	}

	private static HashMap<TabletLocator.LocatorKey, TabletLocator> locators = new HashMap<>();

	public static synchronized void clearLocators() {
		for (TabletLocator locator : TabletLocator.locators.values()) {
			locator.isValid = false;
		}
		TabletLocator.locators.clear();
	}

	public static synchronized TabletLocator getLocator(ClientContext context, String tableId) {
		Instance instance = context.getInstance();
		TabletLocator.LocatorKey key = new TabletLocator.LocatorKey(instance.getInstanceID(), tableId);
		TabletLocator tl = TabletLocator.locators.get(key);
		if (tl == null) {
			MetadataLocationObtainer mlo = new MetadataLocationObtainer();
			if (RootTable.ID.equals(tableId)) {
			}else
				if (MetadataTable.ID.equals(tableId)) {
				}else {
				}

			TabletLocator.locators.put(key, tl);
		}
		return tl;
	}

	public static class TabletLocations {
		private final List<TabletLocator.TabletLocation> locations;

		private final List<KeyExtent> locationless;

		public TabletLocations(List<TabletLocator.TabletLocation> locations, List<KeyExtent> locationless) {
			this.locations = locations;
			this.locationless = locationless;
		}

		public List<TabletLocator.TabletLocation> getLocations() {
			return locations;
		}

		public List<KeyExtent> getLocationless() {
			return locationless;
		}
	}

	public static class TabletLocation implements Comparable<TabletLocator.TabletLocation> {
		private static final WeakHashMap<String, WeakReference<String>> tabletLocs = new WeakHashMap<>();

		private static String dedupeLocation(String tabletLoc) {
			synchronized(TabletLocator.TabletLocation.tabletLocs) {
				WeakReference<String> lref = TabletLocator.TabletLocation.tabletLocs.get(tabletLoc);
				if (lref != null) {
					String loc = lref.get();
					if (loc != null) {
						return loc;
					}
				}
				TabletLocator.TabletLocation.tabletLocs.put(tabletLoc, new WeakReference<>(tabletLoc));
				return tabletLoc;
			}
		}

		public final KeyExtent tablet_extent;

		public final String tablet_location;

		public final String tablet_session;

		public TabletLocation(KeyExtent tablet_extent, String tablet_location, String session) {
			Preconditions.checkArgument((tablet_extent != null), "tablet_extent is null");
			Preconditions.checkArgument((tablet_location != null), "tablet_location is null");
			Preconditions.checkArgument((session != null), "session is null");
			this.tablet_extent = tablet_extent;
			this.tablet_location = TabletLocator.TabletLocation.dedupeLocation(tablet_location);
			this.tablet_session = TabletLocator.TabletLocation.dedupeLocation(session);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof TabletLocator.TabletLocation) {
				TabletLocator.TabletLocation otl = ((TabletLocator.TabletLocation) (o));
				return ((tablet_extent.equals(otl.tablet_extent)) && (tablet_location.equals(otl.tablet_location))) && (tablet_session.equals(otl.tablet_session));
			}
			return false;
		}

		@Override
		public int hashCode() {
			throw new UnsupportedOperationException(("hashcode is not implemented for class " + (this.getClass().toString())));
		}

		@Override
		public String toString() {
			return ((((("(" + (tablet_extent)) + ",") + (tablet_location)) + ",") + (tablet_session)) + ")";
		}

		@Override
		public int compareTo(TabletLocator.TabletLocation o) {
			int result = tablet_extent.compareTo(o.tablet_extent);
			if (result == 0) {
				result = tablet_location.compareTo(o.tablet_location);
				if (result == 0)
					result = tablet_session.compareTo(o.tablet_session);

			}
			return result;
		}
	}

	public static class TabletServerMutations<T extends Mutation> {
		private Map<KeyExtent, List<T>> mutations;

		private String tserverSession;

		public TabletServerMutations(String tserverSession) {
			this.tserverSession = tserverSession;
			this.mutations = new HashMap<>();
		}

		public void addMutation(KeyExtent ke, T m) {
			List<T> mutList = mutations.get(ke);
			if (mutList == null) {
				mutList = new ArrayList<>();
				mutations.put(ke, mutList);
			}
			mutList.add(m);
		}

		public Map<KeyExtent, List<T>> getMutations() {
			return mutations;
		}

		final String getSession() {
			return tserverSession;
		}
	}
}

