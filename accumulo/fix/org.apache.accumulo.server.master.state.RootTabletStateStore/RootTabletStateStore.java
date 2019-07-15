

import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.metadata.RootTable;
import org.apache.accumulo.server.AccumuloServerContext;
import org.apache.accumulo.server.master.state.ClosableIterator;
import org.apache.accumulo.server.master.state.CurrentState;
import org.apache.accumulo.server.master.state.MetaDataStateStore;
import org.apache.accumulo.server.master.state.TabletLocationState;


public class RootTabletStateStore extends MetaDataStateStore {
	public RootTabletStateStore(ClientContext context, CurrentState state) {
		super(context, state, RootTable.NAME);
	}

	public RootTabletStateStore(AccumuloServerContext context) {
		super(context, RootTable.NAME);
	}

	@Override
	public ClosableIterator<TabletLocationState> iterator() {
		return null;
	}

	@Override
	public String name() {
		return "Metadata Tablets";
	}
}

