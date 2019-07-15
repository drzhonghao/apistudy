

import com.google.common.collect.MapMaker;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.compaction.AbstractCompactionStrategy;
import org.apache.cassandra.db.compaction.CompactionStrategyManager;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.utils.NoSpamLogger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.utils.NoSpamLogger.Level.ERROR;


public class CompactionLogger {
	public interface Strategy {
		JsonNode sstable(SSTableReader sstable);

		JsonNode options();

		static CompactionLogger.Strategy none = new CompactionLogger.Strategy() {
			public JsonNode sstable(SSTableReader sstable) {
				return null;
			}

			public JsonNode options() {
				return null;
			}
		};
	}

	public interface StrategySummary {
		JsonNode getSummary();
	}

	public interface Writer {
		void writeStart(JsonNode statement, Object tag);

		void write(JsonNode statement, CompactionLogger.StrategySummary summary, Object tag);
	}

	private interface CompactionStrategyAndTableFunction {
		JsonNode apply(AbstractCompactionStrategy strategy, SSTableReader sstable);
	}

	private static final JsonNodeFactory json = JsonNodeFactory.instance;

	private static final Logger logger = LoggerFactory.getLogger(CompactionLogger.class);

	private static final CompactionLogger.Writer serializer = new CompactionLogger.CompactionLogSerializer();

	private final WeakReference<ColumnFamilyStore> cfsRef;

	private final WeakReference<CompactionStrategyManager> csmRef;

	private final AtomicInteger identifier = new AtomicInteger(0);

	private final Map<AbstractCompactionStrategy, String> compactionStrategyMapping = new MapMaker().weakKeys().makeMap();

	private final AtomicBoolean enabled = new AtomicBoolean(false);

	public CompactionLogger(ColumnFamilyStore cfs, CompactionStrategyManager csm) {
		csmRef = new WeakReference<>(csm);
		cfsRef = new WeakReference<>(cfs);
	}

	private void forEach(Consumer<AbstractCompactionStrategy> consumer) {
		CompactionStrategyManager csm = csmRef.get();
		if (csm == null)
			return;

		csm.getStrategies().forEach(( l) -> l.forEach(consumer));
	}

	private ArrayNode compactionStrategyMap(Function<AbstractCompactionStrategy, JsonNode> select) {
		ArrayNode node = CompactionLogger.json.arrayNode();
		forEach(( acs) -> node.add(select.apply(acs)));
		return node;
	}

	private ArrayNode sstableMap(Collection<SSTableReader> sstables, CompactionLogger.CompactionStrategyAndTableFunction csatf) {
		CompactionStrategyManager csm = csmRef.get();
		ArrayNode node = CompactionLogger.json.arrayNode();
		if (csm == null)
			return node;

		return node;
	}

	private String getId(AbstractCompactionStrategy strategy) {
		return compactionStrategyMapping.computeIfAbsent(strategy, ( s) -> String.valueOf(identifier.getAndIncrement()));
	}

	private JsonNode formatSSTables(AbstractCompactionStrategy strategy) {
		ArrayNode node = CompactionLogger.json.arrayNode();
		CompactionStrategyManager csm = csmRef.get();
		ColumnFamilyStore cfs = cfsRef.get();
		if ((csm == null) || (cfs == null))
			return node;

		for (SSTableReader sstable : cfs.getLiveSSTables()) {
		}
		return node;
	}

	private JsonNode formatSSTable(AbstractCompactionStrategy strategy, SSTableReader sstable) {
		ObjectNode node = CompactionLogger.json.objectNode();
		node.put("generation", sstable.descriptor.generation);
		node.put("version", sstable.descriptor.version.getVersion());
		node.put("size", sstable.onDiskLength());
		JsonNode logResult = strategy.strategyLogger().sstable(sstable);
		if (logResult != null)
			node.put("details", logResult);

		return node;
	}

	private JsonNode startStrategy(AbstractCompactionStrategy strategy) {
		ObjectNode node = CompactionLogger.json.objectNode();
		CompactionStrategyManager csm = csmRef.get();
		if (csm == null)
			return node;

		node.put("strategyId", getId(strategy));
		node.put("type", strategy.getName());
		node.put("tables", formatSSTables(strategy));
		node.put("repaired", csm.isRepaired(strategy));
		List<String> folders = csm.getStrategyFolders(strategy);
		ArrayNode folderNode = CompactionLogger.json.arrayNode();
		for (String folder : folders) {
			folderNode.add(folder);
		}
		node.put("folders", folderNode);
		JsonNode logResult = strategy.strategyLogger().options();
		if (logResult != null)
			node.put("options", logResult);

		return node;
	}

	private JsonNode shutdownStrategy(AbstractCompactionStrategy strategy) {
		ObjectNode node = CompactionLogger.json.objectNode();
		node.put("strategyId", getId(strategy));
		return node;
	}

	private JsonNode describeSSTable(AbstractCompactionStrategy strategy, SSTableReader sstable) {
		ObjectNode node = CompactionLogger.json.objectNode();
		node.put("strategyId", getId(strategy));
		node.put("table", formatSSTable(strategy, sstable));
		return node;
	}

	private void describeStrategy(ObjectNode node) {
		ColumnFamilyStore cfs = cfsRef.get();
		if (cfs == null)
			return;

		node.put("keyspace", cfs.keyspace.getName());
		node.put("table", cfs.getTableName());
		node.put("time", System.currentTimeMillis());
	}

	private JsonNode startStrategies() {
		ObjectNode node = CompactionLogger.json.objectNode();
		node.put("type", "enable");
		describeStrategy(node);
		node.put("strategies", compactionStrategyMap(this::startStrategy));
		return node;
	}

	public void enable() {
		if (enabled.compareAndSet(false, true)) {
			CompactionLogger.serializer.writeStart(startStrategies(), this);
		}
	}

	public void disable() {
		if (enabled.compareAndSet(true, false)) {
			ObjectNode node = CompactionLogger.json.objectNode();
			node.put("type", "disable");
			describeStrategy(node);
			node.put("strategies", compactionStrategyMap(this::shutdownStrategy));
			CompactionLogger.serializer.write(node, this::startStrategies, this);
		}
	}

	public void flush(Collection<SSTableReader> sstables) {
		if (enabled.get()) {
			ObjectNode node = CompactionLogger.json.objectNode();
			node.put("type", "flush");
			describeStrategy(node);
			node.put("tables", sstableMap(sstables, this::describeSSTable));
			CompactionLogger.serializer.write(node, this::startStrategies, this);
		}
	}

	public void compaction(long startTime, Collection<SSTableReader> input, long endTime, Collection<SSTableReader> output) {
		if (enabled.get()) {
			ObjectNode node = CompactionLogger.json.objectNode();
			node.put("type", "compaction");
			describeStrategy(node);
			node.put("start", String.valueOf(startTime));
			node.put("end", String.valueOf(endTime));
			node.put("input", sstableMap(input, this::describeSSTable));
			node.put("output", sstableMap(output, this::describeSSTable));
			CompactionLogger.serializer.write(node, this::startStrategies, this);
		}
	}

	public void pending(AbstractCompactionStrategy strategy, int remaining) {
		if ((remaining != 0) && (enabled.get())) {
			ObjectNode node = CompactionLogger.json.objectNode();
			node.put("type", "pending");
			describeStrategy(node);
			node.put("strategyId", getId(strategy));
			node.put("pending", remaining);
			CompactionLogger.serializer.write(node, this::startStrategies, this);
		}
	}

	private static class CompactionLogSerializer implements CompactionLogger.Writer {
		private static final String logDirectory = System.getProperty("cassandra.logdir", ".");

		private final ExecutorService loggerService = Executors.newFixedThreadPool(1);

		private final Set<Object> rolled = new HashSet<>();

		private OutputStreamWriter stream;

		private static OutputStreamWriter createStream() throws IOException {
			int count = 0;
			Path compactionLog = Paths.get(CompactionLogger.CompactionLogSerializer.logDirectory, "compaction.log");
			if (Files.exists(compactionLog)) {
				Path tryPath = compactionLog;
				while (Files.exists(tryPath)) {
					tryPath = Paths.get(CompactionLogger.CompactionLogSerializer.logDirectory, String.format("compaction-%d.log", (count++)));
				} 
				Files.move(compactionLog, tryPath);
			}
			return new OutputStreamWriter(Files.newOutputStream(compactionLog, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE));
		}

		private void writeLocal(String toWrite) {
			try {
				if ((stream) == null)
					stream = CompactionLogger.CompactionLogSerializer.createStream();

				stream.write(toWrite);
				stream.flush();
			} catch (IOException ioe) {
				NoSpamLogger.log(CompactionLogger.logger, ERROR, 1, TimeUnit.MINUTES, "Could not write to the log file: {}", ioe);
			}
		}

		public void writeStart(JsonNode statement, Object tag) {
			final String toWrite = (statement.toString()) + (System.lineSeparator());
			loggerService.execute(() -> {
				rolled.add(tag);
				writeLocal(toWrite);
			});
		}

		public void write(JsonNode statement, CompactionLogger.StrategySummary summary, Object tag) {
			final String toWrite = (statement.toString()) + (System.lineSeparator());
			loggerService.execute(() -> {
				if (!(rolled.contains(tag))) {
					writeLocal(((summary.getSummary().toString()) + (System.lineSeparator())));
					rolled.add(tag);
				}
				writeLocal(toWrite);
			});
		}
	}
}

