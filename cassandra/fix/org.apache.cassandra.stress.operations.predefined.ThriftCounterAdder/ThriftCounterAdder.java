

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.cassandra.stress.Operation;
import org.apache.cassandra.stress.generate.Distribution;
import org.apache.cassandra.stress.generate.DistributionFactory;
import org.apache.cassandra.stress.generate.PartitionGenerator;
import org.apache.cassandra.stress.generate.SeedManager;
import org.apache.cassandra.stress.operations.predefined.PredefinedOperation;
import org.apache.cassandra.stress.report.Timer;
import org.apache.cassandra.stress.settings.Command;
import org.apache.cassandra.stress.settings.SettingsCommand;
import org.apache.cassandra.stress.settings.StressSettings;
import org.apache.cassandra.stress.util.ThriftClient;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.CounterColumn;
import org.apache.cassandra.thrift.Mutation;


public class ThriftCounterAdder extends PredefinedOperation {
	final Distribution counteradd;

	public ThriftCounterAdder(DistributionFactory counteradd, Timer timer, PartitionGenerator generator, SeedManager seedManager, StressSettings settings) {
		super(Command.COUNTER_WRITE, timer, generator, seedManager, settings);
		this.counteradd = counteradd.get();
	}

	public boolean isWrite() {
		return true;
	}

	public void run(final ThriftClient client) throws IOException {
		List<CounterColumn> columns = new ArrayList<>();
		List<Mutation> mutations = new ArrayList<>(columns.size());
		for (CounterColumn c : columns) {
			ColumnOrSuperColumn cosc = new ColumnOrSuperColumn().setCounter_column(c);
			mutations.add(new Mutation().setColumn_or_supercolumn(cosc));
		}
		Map<String, List<Mutation>> row = Collections.singletonMap(type.table, mutations);
		final ByteBuffer key = getKey();
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
}

