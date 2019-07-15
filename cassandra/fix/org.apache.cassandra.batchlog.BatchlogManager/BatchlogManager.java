

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.RateLimiter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.apache.cassandra.batchlog.Batch;
import org.apache.cassandra.batchlog.BatchlogManagerMBean;
import org.apache.cassandra.concurrent.DebuggableScheduledThreadPoolExecutor;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.cql3.QueryProcessor;
import org.apache.cassandra.cql3.UntypedResultSet;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.WriteType;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.UUIDType;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.exceptions.WriteFailureException;
import org.apache.cassandra.exceptions.WriteTimeoutException;
import org.apache.cassandra.gms.FailureDetector;
import org.apache.cassandra.gms.IFailureDetector;
import org.apache.cassandra.hints.Hint;
import org.apache.cassandra.hints.HintsService;
import org.apache.cassandra.io.util.DataInputBuffer;
import org.apache.cassandra.locator.TokenMetadata;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.service.AbstractWriteResponseHandler;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.service.WriteResponseHandler;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.UUIDGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BatchlogManager implements BatchlogManagerMBean {
	public static final String MBEAN_NAME = "org.apache.cassandra.db:type=BatchlogManager";

	private static final long REPLAY_INTERVAL = 10 * 1000;

	static final int DEFAULT_PAGE_SIZE = 128;

	private static final Logger logger = LoggerFactory.getLogger(BatchlogManager.class);

	public static final BatchlogManager instance = new BatchlogManager();

	public static final long BATCHLOG_REPLAY_TIMEOUT = Long.getLong("cassandra.batchlog.replay_timeout_in_ms", ((DatabaseDescriptor.getWriteRpcTimeout()) * 2));

	private volatile long totalBatchesReplayed = 0;

	private volatile UUID lastReplayedUuid = UUIDGen.minTimeUUID(0);

	private final ScheduledExecutorService batchlogTasks;

	public BatchlogManager() {
		ScheduledThreadPoolExecutor executor = new DebuggableScheduledThreadPoolExecutor("BatchlogTasks");
		executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		batchlogTasks = executor;
	}

	public void start() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			mbs.registerMBean(this, new ObjectName(BatchlogManager.MBEAN_NAME));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		batchlogTasks.scheduleWithFixedDelay(this::replayFailedBatches, StorageService.RING_DELAY, BatchlogManager.REPLAY_INTERVAL, TimeUnit.MILLISECONDS);
	}

	public void shutdown() throws InterruptedException {
		batchlogTasks.shutdown();
		batchlogTasks.awaitTermination(60, TimeUnit.SECONDS);
	}

	public static void remove(UUID id) {
		new Mutation(PartitionUpdate.fullPartitionDelete(SystemKeyspace.Batches, UUIDType.instance.decompose(id), FBUtilities.timestampMicros(), FBUtilities.nowInSeconds())).apply();
	}

	public static void store(Batch batch) {
		BatchlogManager.store(batch, true);
	}

	public static void store(Batch batch, boolean durableWrites) {
		PartitionUpdate.SimpleBuilder builder = PartitionUpdate.simpleBuilder(SystemKeyspace.Batches, batch.id);
		builder.buildAsMutation().apply(durableWrites);
	}

	@com.google.common.annotations.VisibleForTesting
	public int countAllBatches() {
		String query = String.format("SELECT count(*) FROM %s.%s", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.BATCHES);
		UntypedResultSet results = QueryProcessor.executeInternal(query);
		if ((results == null) || (results.isEmpty()))
			return 0;

		return ((int) (results.one().getLong("count")));
	}

	public long getTotalBatchesReplayed() {
		return totalBatchesReplayed;
	}

	public void forceBatchlogReplay() throws Exception {
		startBatchlogReplay().get();
	}

	public Future<?> startBatchlogReplay() {
		return batchlogTasks.submit(this::replayFailedBatches);
	}

	void performInitialReplay() throws InterruptedException, ExecutionException {
		batchlogTasks.submit(this::replayFailedBatches).get();
	}

	private void replayFailedBatches() {
		BatchlogManager.logger.trace("Started replayFailedBatches");
		int endpointsCount = StorageService.instance.getTokenMetadata().getAllEndpoints().size();
		if (endpointsCount <= 0) {
			BatchlogManager.logger.trace("Replay cancelled as there are no peers in the ring.");
			return;
		}
		int throttleInKB = (DatabaseDescriptor.getBatchlogReplayThrottleInKB()) / endpointsCount;
		RateLimiter rateLimiter = RateLimiter.create((throttleInKB == 0 ? Double.MAX_VALUE : throttleInKB * 1024));
		UUID limitUuid = UUIDGen.maxTimeUUID(((System.currentTimeMillis()) - (BatchlogManager.getBatchlogTimeout())));
		ColumnFamilyStore store = Keyspace.open(SchemaConstants.SYSTEM_KEYSPACE_NAME).getColumnFamilyStore(SystemKeyspace.BATCHES);
		int pageSize = BatchlogManager.calculatePageSize(store);
		String query = String.format("SELECT id, mutations, version FROM %s.%s WHERE token(id) > token(?) AND token(id) <= token(?)", SchemaConstants.SYSTEM_KEYSPACE_NAME, SystemKeyspace.BATCHES);
		UntypedResultSet batches = QueryProcessor.executeInternalWithPaging(query, pageSize, lastReplayedUuid, limitUuid);
		processBatchlogEntries(batches, pageSize, rateLimiter);
		lastReplayedUuid = limitUuid;
		BatchlogManager.logger.trace("Finished replayFailedBatches");
	}

	static int calculatePageSize(ColumnFamilyStore store) {
		double averageRowSize = store.getMeanPartitionSize();
		if (averageRowSize <= 0)
			return BatchlogManager.DEFAULT_PAGE_SIZE;

		return ((int) (Math.max(1, Math.min(BatchlogManager.DEFAULT_PAGE_SIZE, (((4 * 1024) * 1024) / averageRowSize)))));
	}

	private void processBatchlogEntries(UntypedResultSet batches, int pageSize, RateLimiter rateLimiter) {
		int positionInPage = 0;
		ArrayList<BatchlogManager.ReplayingBatch> unfinishedBatches = new ArrayList<>(pageSize);
		Set<InetAddress> hintedNodes = new HashSet<>();
		Set<UUID> replayedBatches = new HashSet<>();
		for (UntypedResultSet.Row row : batches) {
			UUID id = row.getUUID("id");
			int version = row.getInt("version");
			try {
				BatchlogManager.ReplayingBatch batch = new BatchlogManager.ReplayingBatch(id, version, row.getList("mutations", BytesType.instance));
				if ((batch.replay(rateLimiter, hintedNodes)) > 0) {
					unfinishedBatches.add(batch);
				}else {
					BatchlogManager.remove(id);
					++(totalBatchesReplayed);
				}
			} catch (IOException e) {
				BatchlogManager.logger.warn("Skipped batch replay of {} due to {}", id, e);
				BatchlogManager.remove(id);
			}
			if ((++positionInPage) == pageSize) {
				finishAndClearBatches(unfinishedBatches, hintedNodes, replayedBatches);
				positionInPage = 0;
			}
		}
		finishAndClearBatches(unfinishedBatches, hintedNodes, replayedBatches);
		HintsService.instance.flushAndFsyncBlockingly(Iterables.transform(hintedNodes, StorageService.instance::getHostIdForEndpoint));
		replayedBatches.forEach(BatchlogManager::remove);
	}

	private void finishAndClearBatches(ArrayList<BatchlogManager.ReplayingBatch> batches, Set<InetAddress> hintedNodes, Set<UUID> replayedBatches) {
		for (BatchlogManager.ReplayingBatch batch : batches) {
			batch.finish(hintedNodes);
			replayedBatches.add(batch.id);
		}
		totalBatchesReplayed += batches.size();
		batches.clear();
	}

	public static long getBatchlogTimeout() {
		return BatchlogManager.BATCHLOG_REPLAY_TIMEOUT;
	}

	private static class ReplayingBatch {
		private final UUID id;

		private final long writtenAt;

		private final List<Mutation> mutations;

		private final int replayedBytes;

		private List<BatchlogManager.ReplayingBatch.ReplayWriteResponseHandler<Mutation>> replayHandlers;

		ReplayingBatch(UUID id, int version, List<ByteBuffer> serializedMutations) throws IOException {
			this.id = id;
			this.writtenAt = UUIDGen.unixTimestamp(id);
			this.mutations = new ArrayList<>(serializedMutations.size());
			this.replayedBytes = addMutations(version, serializedMutations);
		}

		public int replay(RateLimiter rateLimiter, Set<InetAddress> hintedNodes) throws IOException {
			BatchlogManager.logger.trace("Replaying batch {}", id);
			if (mutations.isEmpty())
				return 0;

			int gcgs = BatchlogManager.ReplayingBatch.gcgs(mutations);
			if (((TimeUnit.MILLISECONDS.toSeconds(writtenAt)) + gcgs) <= (FBUtilities.nowInSeconds()))
				return 0;

			replayHandlers = BatchlogManager.ReplayingBatch.sendReplays(mutations, writtenAt, hintedNodes);
			rateLimiter.acquire(replayedBytes);
			return replayHandlers.size();
		}

		public void finish(Set<InetAddress> hintedNodes) {
			for (int i = 0; i < (replayHandlers.size()); i++) {
				BatchlogManager.ReplayingBatch.ReplayWriteResponseHandler<Mutation> handler = replayHandlers.get(i);
				try {
					handler.get();
				} catch (WriteTimeoutException | WriteFailureException e) {
					BatchlogManager.logger.trace("Failed replaying a batched mutation to a node, will write a hint");
					BatchlogManager.logger.trace("Failure was : {}", e.getMessage());
					writeHintsForUndeliveredEndpoints(i, hintedNodes);
					return;
				}
			}
		}

		private int addMutations(int version, List<ByteBuffer> serializedMutations) throws IOException {
			int ret = 0;
			for (ByteBuffer serializedMutation : serializedMutations) {
				ret += serializedMutation.remaining();
				try (DataInputBuffer in = new DataInputBuffer(serializedMutation, true)) {
					addMutation(Mutation.serializer.deserialize(in, version));
				}
			}
			return ret;
		}

		private void addMutation(Mutation mutation) {
			for (UUID cfId : mutation.getColumnFamilyIds())
				if ((writtenAt) <= (SystemKeyspace.getTruncatedAt(cfId)))
					mutation = mutation.without(cfId);


			if (!(mutation.isEmpty()))
				mutations.add(mutation);

		}

		private void writeHintsForUndeliveredEndpoints(int startFrom, Set<InetAddress> hintedNodes) {
			int gcgs = BatchlogManager.ReplayingBatch.gcgs(mutations);
			if (((TimeUnit.MILLISECONDS.toSeconds(writtenAt)) + gcgs) <= (FBUtilities.nowInSeconds()))
				return;

			for (int i = startFrom; i < (replayHandlers.size()); i++) {
				BatchlogManager.ReplayingBatch.ReplayWriteResponseHandler<Mutation> handler = replayHandlers.get(i);
				Mutation undeliveredMutation = mutations.get(i);
				if (handler != null) {
					hintedNodes.addAll(handler.undelivered);
					HintsService.instance.write(Iterables.transform(handler.undelivered, StorageService.instance::getHostIdForEndpoint), Hint.create(undeliveredMutation, writtenAt));
				}
			}
		}

		private static List<BatchlogManager.ReplayingBatch.ReplayWriteResponseHandler<Mutation>> sendReplays(List<Mutation> mutations, long writtenAt, Set<InetAddress> hintedNodes) {
			List<BatchlogManager.ReplayingBatch.ReplayWriteResponseHandler<Mutation>> handlers = new ArrayList<>(mutations.size());
			for (Mutation mutation : mutations) {
				BatchlogManager.ReplayingBatch.ReplayWriteResponseHandler<Mutation> handler = BatchlogManager.ReplayingBatch.sendSingleReplayMutation(mutation, writtenAt, hintedNodes);
				if (handler != null)
					handlers.add(handler);

			}
			return handlers;
		}

		private static BatchlogManager.ReplayingBatch.ReplayWriteResponseHandler<Mutation> sendSingleReplayMutation(final Mutation mutation, long writtenAt, Set<InetAddress> hintedNodes) {
			Set<InetAddress> liveEndpoints = new HashSet<>();
			String ks = mutation.getKeyspaceName();
			Token tk = mutation.key().getToken();
			for (InetAddress endpoint : StorageService.instance.getNaturalAndPendingEndpoints(ks, tk)) {
				if (endpoint.equals(FBUtilities.getBroadcastAddress())) {
					mutation.apply();
				}else
					if (FailureDetector.instance.isAlive(endpoint)) {
						liveEndpoints.add(endpoint);
					}else {
						hintedNodes.add(endpoint);
						HintsService.instance.write(StorageService.instance.getHostIdForEndpoint(endpoint), Hint.create(mutation, writtenAt));
					}

			}
			if (liveEndpoints.isEmpty())
				return null;

			BatchlogManager.ReplayingBatch.ReplayWriteResponseHandler<Mutation> handler = new BatchlogManager.ReplayingBatch.ReplayWriteResponseHandler<>(liveEndpoints, System.nanoTime());
			MessageOut<Mutation> message = mutation.createMessage();
			for (InetAddress endpoint : liveEndpoints)
				MessagingService.instance().sendRR(message, endpoint, handler, false);

			return handler;
		}

		private static int gcgs(Collection<Mutation> mutations) {
			int gcgs = Integer.MAX_VALUE;
			for (Mutation mutation : mutations)
				gcgs = Math.min(gcgs, mutation.smallestGCGS());

			return gcgs;
		}

		private static class ReplayWriteResponseHandler<T> extends WriteResponseHandler<T> {
			private final Set<InetAddress> undelivered = Collections.newSetFromMap(new ConcurrentHashMap<>());

			ReplayWriteResponseHandler(Collection<InetAddress> writeEndpoints, long queryStartNanoTime) {
				super(writeEndpoints, Collections.<InetAddress>emptySet(), null, null, null, WriteType.UNLOGGED_BATCH, queryStartNanoTime);
				undelivered.addAll(writeEndpoints);
			}

			@Override
			protected int totalBlockFor() {
				return this.naturalEndpoints.size();
			}

			@Override
			public void response(MessageIn<T> m) {
				boolean removed = undelivered.remove((m == null ? FBUtilities.getBroadcastAddress() : m.from));
				assert removed;
				super.response(m);
			}
		}
	}

	public static class EndpointFilter {
		private final String localRack;

		private final Multimap<String, InetAddress> endpoints;

		public EndpointFilter(String localRack, Multimap<String, InetAddress> endpoints) {
			this.localRack = localRack;
			this.endpoints = endpoints;
		}

		public Collection<InetAddress> filter() {
			if ((endpoints.values().size()) == 1)
				return endpoints.values();

			ListMultimap<String, InetAddress> validated = ArrayListMultimap.create();
			for (Map.Entry<String, InetAddress> entry : endpoints.entries())
				if (isValid(entry.getValue()))
					validated.put(entry.getKey(), entry.getValue());


			if ((validated.size()) <= 2)
				return validated.values();

			if (((validated.size()) - (validated.get(localRack).size())) >= 2) {
				validated.removeAll(localRack);
			}
			if ((validated.keySet().size()) == 1) {
				List<InetAddress> otherRack = Lists.newArrayList(validated.values());
				shuffle(otherRack);
				return otherRack.subList(0, 2);
			}
			Collection<String> racks;
			if ((validated.keySet().size()) == 2) {
				racks = validated.keySet();
			}else {
				racks = Lists.newArrayList(validated.keySet());
				shuffle(((List<String>) (racks)));
			}
			List<InetAddress> result = new ArrayList<>(2);
			for (String rack : Iterables.limit(racks, 2)) {
				List<InetAddress> rackMembers = validated.get(rack);
				result.add(rackMembers.get(getRandomInt(rackMembers.size())));
			}
			return result;
		}

		@com.google.common.annotations.VisibleForTesting
		protected boolean isValid(InetAddress input) {
			return (!(input.equals(FBUtilities.getBroadcastAddress()))) && (FailureDetector.instance.isAlive(input));
		}

		@com.google.common.annotations.VisibleForTesting
		protected int getRandomInt(int bound) {
			return ThreadLocalRandom.current().nextInt(bound);
		}

		@com.google.common.annotations.VisibleForTesting
		protected void shuffle(List<?> list) {
			Collections.shuffle(list);
		}
	}
}

