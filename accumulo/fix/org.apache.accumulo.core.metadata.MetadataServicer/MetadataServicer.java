

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.SortedMap;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.RootTable;


public abstract class MetadataServicer {
	public static MetadataServicer forTableName(ClientContext context, String tableName) throws AccumuloException, AccumuloSecurityException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		return MetadataServicer.forTableId(context, context.getConnector().tableOperations().tableIdMap().get(tableName));
	}

	public static MetadataServicer forTableId(ClientContext context, String tableId) {
		Preconditions.checkArgument((tableId != null), "tableId is null");
		if (RootTable.ID.equals(tableId)) {
		}else
			if (MetadataTable.ID.equals(tableId)) {
			}else {
			}

		return null;
	}

	public abstract String getServicedTableId();

	public abstract void getTabletLocations(SortedMap<KeyExtent, String> tablets) throws AccumuloException, AccumuloSecurityException, TableNotFoundException;
}

