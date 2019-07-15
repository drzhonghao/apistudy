

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.accumulo.core.client.BatchDeleter;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.mock.MockAccumulo;
import org.apache.accumulo.core.client.mock.MockBatchScanner;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.hadoop.io.Text;


@Deprecated
public class MockBatchDeleter extends MockBatchScanner implements BatchDeleter {
	private final MockAccumulo acc;

	private final String tableName;

	@Override
	public void delete() throws MutationsRejectedException, TableNotFoundException {
		try {
			Iterator<Map.Entry<Key, Value>> iter = super.iterator();
			while (iter.hasNext()) {
				Map.Entry<Key, Value> next = iter.next();
				Key k = next.getKey();
				Mutation m = new Mutation(k.getRow());
				m.putDelete(k.getColumnFamily(), k.getColumnQualifier(), new ColumnVisibility(k.getColumnVisibility()), k.getTimestamp());
			} 
		} finally {
		}
	}
}

