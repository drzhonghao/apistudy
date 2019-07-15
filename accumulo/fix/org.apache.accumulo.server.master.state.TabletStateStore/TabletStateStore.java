

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.server.AccumuloServerContext;
import org.apache.accumulo.server.master.state.Assignment;
import org.apache.accumulo.server.master.state.ClosableIterator;
import org.apache.accumulo.server.master.state.DistributedStoreException;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.master.state.TabletLocationState;
import org.apache.hadoop.fs.Path;


public abstract class TabletStateStore implements Iterable<TabletLocationState> {
	public abstract String name();

	@Override
	public abstract ClosableIterator<TabletLocationState> iterator();

	public abstract void setFutureLocations(Collection<Assignment> assignments) throws DistributedStoreException;

	public abstract void setLocations(Collection<Assignment> assignments) throws DistributedStoreException;

	public abstract void unassign(Collection<TabletLocationState> tablets, Map<TServerInstance, List<Path>> logsForDeadServers) throws DistributedStoreException;

	public abstract void suspend(Collection<TabletLocationState> tablets, Map<TServerInstance, List<Path>> logsForDeadServers, long suspensionTimestamp) throws DistributedStoreException;

	public abstract void unsuspend(Collection<TabletLocationState> tablets) throws DistributedStoreException;

	public static void unassign(AccumuloServerContext context, TabletLocationState tls, Map<TServerInstance, List<Path>> logsForDeadServers) throws DistributedStoreException {
		TabletStateStore.getStoreForTablet(tls.extent, context).unassign(Collections.singletonList(tls), logsForDeadServers);
	}

	public static void suspend(AccumuloServerContext context, TabletLocationState tls, Map<TServerInstance, List<Path>> logsForDeadServers, long suspensionTimestamp) throws DistributedStoreException {
		TabletStateStore.getStoreForTablet(tls.extent, context).suspend(Collections.singletonList(tls), logsForDeadServers, suspensionTimestamp);
	}

	public static void setLocation(AccumuloServerContext context, Assignment assignment) throws DistributedStoreException {
		TabletStateStore.getStoreForTablet(assignment.tablet, context).setLocations(Collections.singletonList(assignment));
	}

	protected static TabletStateStore getStoreForTablet(KeyExtent extent, AccumuloServerContext context) throws DistributedStoreException {
		if (extent.isRootTablet()) {
		}else
			if (extent.isMeta()) {
			}else {
			}

		return null;
	}
}

