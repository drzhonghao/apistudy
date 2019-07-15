

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.cassandra.stress.Operation;
import org.apache.cassandra.stress.generate.PartitionGenerator;
import org.apache.cassandra.stress.generate.SeedManager;
import org.apache.cassandra.stress.operations.predefined.PredefinedOperation;
import org.apache.cassandra.stress.report.Timer;
import org.apache.cassandra.stress.settings.Command;
import org.apache.cassandra.stress.settings.SettingsCommand;
import org.apache.cassandra.stress.settings.StressSettings;
import org.apache.cassandra.stress.util.ThriftClient;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.Mutation;


public final class ThriftInserter extends PredefinedOperation {
	public ThriftInserter(Timer timer, PartitionGenerator generator, SeedManager seedManager, StressSettings settings) {
		super(Command.WRITE, timer, generator, seedManager, settings);
	}

	public boolean isWrite() {
		return true;
	}

	public void run(final ThriftClient client) throws IOException {
		final ByteBuffer key = getKey();
		final List<Column> columns = getColumns();
		List<Mutation> mutations = new ArrayList<>(columns.size());
		for (Column c : columns) {
			ColumnOrSuperColumn column = new ColumnOrSuperColumn().setColumn(c);
			mutations.add(new Mutation().setColumn_or_supercolumn(column));
		}
		Map<String, List<Mutation>> row = Collections.singletonMap(type.table, mutations);
		final Map<ByteBuffer, Map<String, List<Mutation>>> record = Collections.singletonMap(key, row);
		timeWithRetry(new Operation.RunOp() {
			@Override
			public boolean run() throws Exception {
				client.batch_mutate(record, settings.command.consistencyLevel);
				return true;
			}

			@Override
			public int partitionCount() {
				return 1;
			}

			@Override
			public int rowCount() {
				return 1;
			}
		});
	}

	protected List<Column> getColumns() {
		return null;
	}
}

