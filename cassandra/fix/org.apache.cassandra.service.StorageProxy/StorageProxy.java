

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.apache.cassandra.batchlog.Batch;
import org.apache.cassandra.batchlog.BatchlogManager;
import org.apache.cassandra.batchlog.LegacyBatchlogMigrator;
import org.apache.cassandra.concurrent.LocalAwareExecutorService;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.CounterMutation;
import org.apache.cassandra.db.DataRange;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.HintedHandOffManager;
import org.apache.cassandra.db.IMutation;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.PartitionRangeReadCommand;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.ReadResponse;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.Truncation;
import org.apache.cassandra.db.WriteType;
import org.apache.cassandra.db.filter.DataLimits;
import org.apache.cassandra.db.filter.TombstoneOverwhelmingException;
import org.apache.cassandra.db.monitoring.MonitorableImpl;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.BasePartitionIterator;
import org.apache.cassandra.db.partitions.FilteredPartition;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.partitions.PartitionIterators;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.db.view.ViewManager;
import org.apache.cassandra.db.view.ViewUtils;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.dht.Bounds;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.RingPosition;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.IsBootstrappingException;
import org.apache.cassandra.exceptions.OverloadedException;
import org.apache.cassandra.exceptions.ReadFailureException;
import org.apache.cassandra.exceptions.ReadTimeoutException;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.exceptions.RequestFailureException;
import org.apache.cassandra.exceptions.RequestFailureReason;
import org.apache.cassandra.exceptions.RequestTimeoutException;
import org.apache.cassandra.exceptions.UnavailableException;
import org.apache.cassandra.exceptions.WriteFailureException;
import org.apache.cassandra.exceptions.WriteTimeoutException;
import org.apache.cassandra.gms.FailureDetector;
import org.apache.cassandra.gms.Gossiper;
import org.apache.cassandra.gms.IFailureDetector;
import org.apache.cassandra.hints.Hint;
import org.apache.cassandra.hints.HintsService;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.io.util.BufferedDataOutputStreamPlus;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.locator.AbstractReplicationStrategy;
import org.apache.cassandra.locator.IEndpointSnitch;
import org.apache.cassandra.locator.LocalStrategy;
import org.apache.cassandra.locator.TokenMetadata;
import org.apache.cassandra.metrics.CASClientRequestMetrics;
import org.apache.cassandra.metrics.ClientRequestMetrics;
import org.apache.cassandra.metrics.HintedHandoffMetrics;
import org.apache.cassandra.metrics.LatencyMetrics;
import org.apache.cassandra.metrics.ReadRepairMetrics;
import org.apache.cassandra.metrics.StorageMetrics;
import org.apache.cassandra.metrics.TableMetrics;
import org.apache.cassandra.metrics.ViewWriteMetrics;
import org.apache.cassandra.net.CompactEndpointSerializationHelper;
import org.apache.cassandra.net.IAsyncCallback;
import org.apache.cassandra.net.IAsyncCallbackWithFailure;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.net.MessagingService.Verb;
import org.apache.cassandra.service.AbstractReadExecutor;
import org.apache.cassandra.service.AbstractWriteResponseHandler;
import org.apache.cassandra.service.BatchlogResponseHandler;
import org.apache.cassandra.service.CASRequest;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.DigestMismatchException;
import org.apache.cassandra.service.ReadCallback;
import org.apache.cassandra.service.StorageProxyMBean;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.service.TruncateResponseHandler;
import org.apache.cassandra.service.WriteResponseHandler;
import org.apache.cassandra.service.paxos.AbstractPaxosCallback;
import org.apache.cassandra.service.paxos.Commit;
import org.apache.cassandra.service.paxos.PaxosState;
import org.apache.cassandra.service.paxos.PrepareCallback;
import org.apache.cassandra.service.paxos.ProposeCallback;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.triggers.TriggerExecutor;
import org.apache.cassandra.utils.AbstractIterator;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.UUIDGen;
import org.apache.cassandra.utils.UUIDSerializer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.db.SinglePartitionReadCommand.Group.one;
import static org.apache.cassandra.net.MessagingService.Verb.BATCH_REMOVE;
import static org.apache.cassandra.net.MessagingService.Verb.BATCH_STORE;
import static org.apache.cassandra.net.MessagingService.Verb.COUNTER_MUTATION;
import static org.apache.cassandra.net.MessagingService.Verb.MUTATION;
import static org.apache.cassandra.net.MessagingService.Verb.PAXOS_COMMIT;
import static org.apache.cassandra.net.MessagingService.Verb.PAXOS_PREPARE;
import static org.apache.cassandra.net.MessagingService.Verb.PAXOS_PROPOSE;
import static org.apache.cassandra.net.MessagingService.Verb.READ;
import static org.apache.cassandra.net.MessagingService.Verb.SCHEMA_CHECK;


public class StorageProxy implements StorageProxyMBean {
	public static final String MBEAN_NAME = "org.apache.cassandra.db:type=StorageProxy";

	private static final Logger logger = LoggerFactory.getLogger(StorageProxy.class);

	public static final String UNREACHABLE = "UNREACHABLE";

	private static final StorageProxy.WritePerformer standardWritePerformer;

	private static final StorageProxy.WritePerformer counterWritePerformer;

	private static final StorageProxy.WritePerformer counterWriteOnCoordinatorPerformer;

	public static final StorageProxy instance = new StorageProxy();

	private static volatile int maxHintsInProgress = 128 * (FBUtilities.getAvailableProcessors());

	private static final CacheLoader<InetAddress, AtomicInteger> hintsInProgress = new CacheLoader<InetAddress, AtomicInteger>() {
		public AtomicInteger load(InetAddress inetAddress) {
			return new AtomicInteger(0);
		}
	};

	private static final ClientRequestMetrics readMetrics = new ClientRequestMetrics("Read");

	private static final ClientRequestMetrics rangeMetrics = new ClientRequestMetrics("RangeSlice");

	private static final ClientRequestMetrics writeMetrics = new ClientRequestMetrics("Write");

	private static final CASClientRequestMetrics casWriteMetrics = new CASClientRequestMetrics("CASWrite");

	private static final CASClientRequestMetrics casReadMetrics = new CASClientRequestMetrics("CASRead");

	private static final ViewWriteMetrics viewWriteMetrics = new ViewWriteMetrics("ViewWrite");

	private static final Map<ConsistencyLevel, ClientRequestMetrics> readMetricsMap = new EnumMap<>(ConsistencyLevel.class);

	private static final Map<ConsistencyLevel, ClientRequestMetrics> writeMetricsMap = new EnumMap<>(ConsistencyLevel.class);

	private static final double CONCURRENT_SUBREQUESTS_MARGIN = 0.1;

	private StorageProxy() {
	}

	static {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			mbs.registerMBean(StorageProxy.instance, new ObjectName(StorageProxy.MBEAN_NAME));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		HintsService.instance.registerMBean();
		HintedHandOffManager.instance.registerMBean();
		standardWritePerformer = new StorageProxy.WritePerformer() {
			public void apply(IMutation mutation, Iterable<InetAddress> targets, AbstractWriteResponseHandler<IMutation> responseHandler, String localDataCenter, ConsistencyLevel consistency_level) throws OverloadedException {
				assert mutation instanceof Mutation;
				StorageProxy.sendToHintedEndpoints(((Mutation) (mutation)), targets, responseHandler, localDataCenter, Stage.MUTATION);
			}
		};
		counterWritePerformer = new StorageProxy.WritePerformer() {
			public void apply(IMutation mutation, Iterable<InetAddress> targets, AbstractWriteResponseHandler<IMutation> responseHandler, String localDataCenter, ConsistencyLevel consistencyLevel) {
				StorageProxy.counterWriteTask(mutation, targets, responseHandler, localDataCenter).run();
			}
		};
		counterWriteOnCoordinatorPerformer = new StorageProxy.WritePerformer() {
			public void apply(IMutation mutation, Iterable<InetAddress> targets, AbstractWriteResponseHandler<IMutation> responseHandler, String localDataCenter, ConsistencyLevel consistencyLevel) {
				StageManager.getStage(Stage.COUNTER_MUTATION).execute(StorageProxy.counterWriteTask(mutation, targets, responseHandler, localDataCenter));
			}
		};
		for (ConsistencyLevel level : ConsistencyLevel.values()) {
			StorageProxy.readMetricsMap.put(level, new ClientRequestMetrics(("Read-" + (level.name()))));
			StorageProxy.writeMetricsMap.put(level, new ClientRequestMetrics(("Write-" + (level.name()))));
		}
	}

	public static RowIterator cas(String keyspaceName, String cfName, DecoratedKey key, CASRequest request, ConsistencyLevel consistencyForPaxos, ConsistencyLevel consistencyForCommit, ClientState state, long queryStartNanoTime) throws InvalidRequestException, IsBootstrappingException, RequestFailureException, RequestTimeoutException, UnavailableException {
		final long startTimeForMetrics = System.nanoTime();
		int contentions = 0;
		try {
			consistencyForPaxos.validateForCas();
			consistencyForCommit.validateForCasCommit(keyspaceName);
			CFMetaData metadata = Schema.instance.getCFMetaData(keyspaceName, cfName);
			long timeout = TimeUnit.MILLISECONDS.toNanos(DatabaseDescriptor.getCasContentionTimeout());
			while (((System.nanoTime()) - queryStartNanoTime) < timeout) {
				Pair<List<InetAddress>, Integer> p = StorageProxy.getPaxosParticipants(metadata, key, consistencyForPaxos);
				List<InetAddress> liveEndpoints = p.left;
				int requiredParticipants = p.right;
				final Pair<UUID, Integer> pair = StorageProxy.beginAndRepairPaxos(queryStartNanoTime, key, metadata, liveEndpoints, requiredParticipants, consistencyForPaxos, consistencyForCommit, true, state);
				final UUID ballot = pair.left;
				contentions += pair.right;
				Tracing.trace("Reading existing values for CAS precondition");
				SinglePartitionReadCommand readCommand = request.readCommand(FBUtilities.nowInSeconds());
				ConsistencyLevel readConsistency = (consistencyForPaxos == (ConsistencyLevel.LOCAL_SERIAL)) ? ConsistencyLevel.LOCAL_QUORUM : ConsistencyLevel.QUORUM;
				FilteredPartition current;
				try (RowIterator rowIter = StorageProxy.readOne(readCommand, readConsistency, queryStartNanoTime)) {
					current = FilteredPartition.create(rowIter);
				}
				if (!(request.appliesTo(current))) {
					Tracing.trace("CAS precondition does not match current values {}", current);
					StorageProxy.casWriteMetrics.conditionNotMet.inc();
					return current.rowIterator();
				}
				PartitionUpdate updates = request.makeUpdates(current);
				updates = TriggerExecutor.instance.execute(updates);
				Commit proposal = Commit.newProposal(ballot, updates);
				Tracing.trace("CAS precondition is met; proposing client-requested updates for {}", ballot);
				if (StorageProxy.proposePaxos(proposal, liveEndpoints, requiredParticipants, true, consistencyForPaxos, queryStartNanoTime)) {
					StorageProxy.commitPaxos(proposal, consistencyForCommit, true, queryStartNanoTime);
					Tracing.trace("CAS successful");
					return null;
				}
				Tracing.trace("Paxos proposal not accepted (pre-empted by a higher ballot)");
				contentions++;
				Uninterruptibles.sleepUninterruptibly(ThreadLocalRandom.current().nextInt(100), TimeUnit.MILLISECONDS);
			} 
			throw new WriteTimeoutException(WriteType.CAS, consistencyForPaxos, 0, consistencyForPaxos.blockFor(Keyspace.open(keyspaceName)));
		} catch (WriteTimeoutException | ReadTimeoutException e) {
			StorageProxy.casWriteMetrics.timeouts.mark();
			StorageProxy.writeMetricsMap.get(consistencyForPaxos).timeouts.mark();
			throw e;
		} catch (WriteFailureException | ReadFailureException e) {
			StorageProxy.casWriteMetrics.failures.mark();
			StorageProxy.writeMetricsMap.get(consistencyForPaxos).failures.mark();
			throw e;
		} catch (UnavailableException e) {
			StorageProxy.casWriteMetrics.unavailables.mark();
			StorageProxy.writeMetricsMap.get(consistencyForPaxos).unavailables.mark();
			throw e;
		} finally {
			StorageProxy.recordCasContention(contentions);
			final long latency = (System.nanoTime()) - startTimeForMetrics;
			StorageProxy.casWriteMetrics.addNano(latency);
			StorageProxy.writeMetricsMap.get(consistencyForPaxos).addNano(latency);
		}
	}

	private static void recordCasContention(int contentions) {
		if (contentions > 0)
			StorageProxy.casWriteMetrics.contention.update(contentions);

	}

	private static Predicate<InetAddress> sameDCPredicateFor(final String dc) {
		final IEndpointSnitch snitch = DatabaseDescriptor.getEndpointSnitch();
		return new Predicate<InetAddress>() {
			public boolean apply(InetAddress host) {
				return dc.equals(snitch.getDatacenter(host));
			}
		};
	}

	private static Pair<List<InetAddress>, Integer> getPaxosParticipants(CFMetaData cfm, DecoratedKey key, ConsistencyLevel consistencyForPaxos) throws UnavailableException {
		Token tk = key.getToken();
		List<InetAddress> naturalEndpoints = StorageService.instance.getNaturalEndpoints(cfm.ksName, tk);
		Collection<InetAddress> pendingEndpoints = StorageService.instance.getTokenMetadata().pendingEndpointsFor(tk, cfm.ksName);
		if (consistencyForPaxos == (ConsistencyLevel.LOCAL_SERIAL)) {
			String localDc = DatabaseDescriptor.getEndpointSnitch().getDatacenter(FBUtilities.getBroadcastAddress());
			Predicate<InetAddress> isLocalDc = StorageProxy.sameDCPredicateFor(localDc);
			naturalEndpoints = ImmutableList.copyOf(Iterables.filter(naturalEndpoints, isLocalDc));
			pendingEndpoints = ImmutableList.copyOf(Iterables.filter(pendingEndpoints, isLocalDc));
		}
		int participants = (pendingEndpoints.size()) + (naturalEndpoints.size());
		int requiredParticipants = (participants / 2) + 1;
		List<InetAddress> liveEndpoints = ImmutableList.copyOf(Iterables.filter(Iterables.concat(naturalEndpoints, pendingEndpoints), IAsyncCallback.isAlive));
		if ((liveEndpoints.size()) < requiredParticipants)
			throw new UnavailableException(consistencyForPaxos, requiredParticipants, liveEndpoints.size());

		if ((pendingEndpoints.size()) > 1)
			throw new UnavailableException(String.format("Cannot perform LWT operation as there is more than one (%d) pending range movement", pendingEndpoints.size()), consistencyForPaxos, (participants + 1), liveEndpoints.size());

		return Pair.create(liveEndpoints, requiredParticipants);
	}

	private static Pair<UUID, Integer> beginAndRepairPaxos(long queryStartNanoTime, DecoratedKey key, CFMetaData metadata, List<InetAddress> liveEndpoints, int requiredParticipants, ConsistencyLevel consistencyForPaxos, ConsistencyLevel consistencyForCommit, final boolean isWrite, ClientState state) throws WriteFailureException, WriteTimeoutException {
		long timeout = TimeUnit.MILLISECONDS.toNanos(DatabaseDescriptor.getCasContentionTimeout());
		PrepareCallback summary = null;
		int contentions = 0;
		while (((System.nanoTime()) - queryStartNanoTime) < timeout) {
			long minTimestampMicrosToUse = (summary == null) ? Long.MIN_VALUE : 1 + (UUIDGen.microsTimestamp(summary.mostRecentInProgressCommit.ballot));
			long ballotMicros = state.getTimestampForPaxos(minTimestampMicrosToUse);
			UUID ballot = UUIDGen.getRandomTimeUUIDFromMicros(ballotMicros);
			Tracing.trace("Preparing {}", ballot);
			Commit toPrepare = Commit.newPrepare(key, metadata, ballot);
			summary = StorageProxy.preparePaxos(toPrepare, liveEndpoints, requiredParticipants, consistencyForPaxos, queryStartNanoTime);
			if (!(summary.promised)) {
				Tracing.trace("Some replicas have already promised a higher ballot than ours; aborting");
				contentions++;
				Uninterruptibles.sleepUninterruptibly(ThreadLocalRandom.current().nextInt(100), TimeUnit.MILLISECONDS);
				continue;
			}
			Commit inProgress = summary.mostRecentInProgressCommitWithUpdate;
			Commit mostRecent = summary.mostRecentCommit;
			if ((!(inProgress.update.isEmpty())) && (inProgress.isAfter(mostRecent))) {
				Tracing.trace("Finishing incomplete paxos round {}", inProgress);
				if (isWrite)
					StorageProxy.casWriteMetrics.unfinishedCommit.inc();
				else
					StorageProxy.casReadMetrics.unfinishedCommit.inc();

				Commit refreshedInProgress = Commit.newProposal(ballot, inProgress.update);
				if (StorageProxy.proposePaxos(refreshedInProgress, liveEndpoints, requiredParticipants, false, consistencyForPaxos, queryStartNanoTime)) {
					try {
						StorageProxy.commitPaxos(refreshedInProgress, consistencyForCommit, false, queryStartNanoTime);
					} catch (WriteTimeoutException e) {
						StorageProxy.recordCasContention(contentions);
						throw new WriteTimeoutException(WriteType.CAS, e.consistency, e.received, e.blockFor);
					}
				}else {
					Tracing.trace("Some replicas have already promised a higher ballot than ours; aborting");
					contentions++;
					Uninterruptibles.sleepUninterruptibly(ThreadLocalRandom.current().nextInt(100), TimeUnit.MILLISECONDS);
				}
				continue;
			}
			int nowInSec = Ints.checkedCast(TimeUnit.MICROSECONDS.toSeconds(ballotMicros));
			Iterable<InetAddress> missingMRC = summary.replicasMissingMostRecentCommit(metadata, nowInSec);
			if ((Iterables.size(missingMRC)) > 0) {
				Tracing.trace("Repairing replicas that missed the most recent commit");
				StorageProxy.sendCommit(mostRecent, missingMRC);
				continue;
			}
			return Pair.create(ballot, contentions);
		} 
		StorageProxy.recordCasContention(contentions);
		throw new WriteTimeoutException(WriteType.CAS, consistencyForPaxos, 0, consistencyForPaxos.blockFor(Keyspace.open(metadata.ksName)));
	}

	private static void sendCommit(Commit commit, Iterable<InetAddress> replicas) {
		MessageOut<Commit> message = new MessageOut<Commit>(PAXOS_COMMIT, commit, Commit.serializer);
		for (InetAddress target : replicas)
			MessagingService.instance().sendOneWay(message, target);

	}

	private static PrepareCallback preparePaxos(Commit toPrepare, List<InetAddress> endpoints, int requiredParticipants, ConsistencyLevel consistencyForPaxos, long queryStartNanoTime) throws WriteTimeoutException {
		PrepareCallback callback = new PrepareCallback(toPrepare.update.partitionKey(), toPrepare.update.metadata(), requiredParticipants, consistencyForPaxos, queryStartNanoTime);
		MessageOut<Commit> message = new MessageOut<Commit>(PAXOS_PREPARE, toPrepare, Commit.serializer);
		for (InetAddress target : endpoints)
			MessagingService.instance().sendRR(message, target, callback);

		callback.await();
		return callback;
	}

	private static boolean proposePaxos(Commit proposal, List<InetAddress> endpoints, int requiredParticipants, boolean timeoutIfPartial, ConsistencyLevel consistencyLevel, long queryStartNanoTime) throws WriteTimeoutException {
		ProposeCallback callback = new ProposeCallback(endpoints.size(), requiredParticipants, (!timeoutIfPartial), consistencyLevel, queryStartNanoTime);
		MessageOut<Commit> message = new MessageOut<Commit>(PAXOS_PROPOSE, proposal, Commit.serializer);
		for (InetAddress target : endpoints)
			MessagingService.instance().sendRR(message, target, callback);

		callback.await();
		if (callback.isSuccessful())
			return true;

		if (timeoutIfPartial && (!(callback.isFullyRefused())))
			throw new WriteTimeoutException(WriteType.CAS, consistencyLevel, callback.getAcceptCount(), requiredParticipants);

		return false;
	}

	private static void commitPaxos(Commit proposal, ConsistencyLevel consistencyLevel, boolean shouldHint, long queryStartNanoTime) throws WriteTimeoutException {
		boolean shouldBlock = consistencyLevel != (ConsistencyLevel.ANY);
		Keyspace keyspace = Keyspace.open(proposal.update.metadata().ksName);
		Token tk = proposal.update.partitionKey().getToken();
		List<InetAddress> naturalEndpoints = StorageService.instance.getNaturalEndpoints(keyspace.getName(), tk);
		Collection<InetAddress> pendingEndpoints = StorageService.instance.getTokenMetadata().pendingEndpointsFor(tk, keyspace.getName());
		AbstractWriteResponseHandler<Commit> responseHandler = null;
		if (shouldBlock) {
			AbstractReplicationStrategy rs = keyspace.getReplicationStrategy();
			responseHandler = rs.getWriteResponseHandler(naturalEndpoints, pendingEndpoints, consistencyLevel, null, WriteType.SIMPLE, queryStartNanoTime);
			responseHandler.setSupportsBackPressure(false);
		}
		MessageOut<Commit> message = new MessageOut<Commit>(PAXOS_COMMIT, proposal, Commit.serializer);
		for (InetAddress destination : Iterables.concat(naturalEndpoints, pendingEndpoints)) {
			StorageProxy.checkHintOverload(destination);
			if (FailureDetector.instance.isAlive(destination)) {
				if (shouldBlock) {
					if (StorageProxy.canDoLocalRequest(destination))
						StorageProxy.commitPaxosLocal(message, responseHandler);
					else
						MessagingService.instance().sendRR(message, destination, responseHandler, shouldHint);

				}else {
					MessagingService.instance().sendOneWay(message, destination);
				}
			}else
				if (shouldHint) {
					StorageProxy.submitHint(proposal.makeMutation(), destination, null);
				}

		}
		if (shouldBlock)
			responseHandler.get();

	}

	private static void commitPaxosLocal(final MessageOut<Commit> message, final AbstractWriteResponseHandler<?> responseHandler) {
		StageManager.getStage(MessagingService.verbStages.get(PAXOS_COMMIT)).maybeExecuteImmediately(new StorageProxy.LocalMutationRunnable() {
			public void runMayThrow() {
				try {
					PaxosState.commit(message.payload);
					if (responseHandler != null)
						responseHandler.response(null);

				} catch (Exception ex) {
					if (!(ex instanceof WriteTimeoutException))
						StorageProxy.logger.error("Failed to apply paxos commit locally : {}", ex);

					responseHandler.onFailure(FBUtilities.getBroadcastAddress(), RequestFailureReason.UNKNOWN);
				}
			}

			@Override
			protected MessagingService.Verb verb() {
				return PAXOS_COMMIT;
			}
		});
	}

	public static void mutate(Collection<? extends IMutation> mutations, ConsistencyLevel consistency_level, long queryStartNanoTime) throws OverloadedException, UnavailableException, WriteFailureException, WriteTimeoutException {
		Tracing.trace("Determining replicas for mutation");
		final String localDataCenter = DatabaseDescriptor.getEndpointSnitch().getDatacenter(FBUtilities.getBroadcastAddress());
		long startTime = System.nanoTime();
		List<AbstractWriteResponseHandler<IMutation>> responseHandlers = new ArrayList<>(mutations.size());
		try {
			for (IMutation mutation : mutations) {
				if (mutation instanceof CounterMutation) {
					responseHandlers.add(StorageProxy.mutateCounter(((CounterMutation) (mutation)), localDataCenter, queryStartNanoTime));
				}else {
					WriteType wt = ((mutations.size()) <= 1) ? WriteType.SIMPLE : WriteType.UNLOGGED_BATCH;
					responseHandlers.add(StorageProxy.performWrite(mutation, consistency_level, localDataCenter, StorageProxy.standardWritePerformer, null, wt, queryStartNanoTime));
				}
			}
			for (AbstractWriteResponseHandler<IMutation> responseHandler : responseHandlers) {
				responseHandler.get();
			}
		} catch (WriteTimeoutException | WriteFailureException ex) {
			if (consistency_level == (ConsistencyLevel.ANY)) {
				StorageProxy.hintMutations(mutations);
			}else {
				if (ex instanceof WriteFailureException) {
					StorageProxy.writeMetrics.failures.mark();
					StorageProxy.writeMetricsMap.get(consistency_level).failures.mark();
					WriteFailureException fe = ((WriteFailureException) (ex));
					Tracing.trace("Write failure; received {} of {} required replies, failed {} requests", fe.received, fe.blockFor, fe.failureReasonByEndpoint.size());
				}else {
					StorageProxy.writeMetrics.timeouts.mark();
					StorageProxy.writeMetricsMap.get(consistency_level).timeouts.mark();
					WriteTimeoutException te = ((WriteTimeoutException) (ex));
					Tracing.trace("Write timeout; received {} of {} required replies", te.received, te.blockFor);
				}
				throw ex;
			}
		} catch (UnavailableException e) {
			StorageProxy.writeMetrics.unavailables.mark();
			StorageProxy.writeMetricsMap.get(consistency_level).unavailables.mark();
			Tracing.trace("Unavailable");
			throw e;
		} catch (OverloadedException e) {
			StorageProxy.writeMetrics.unavailables.mark();
			StorageProxy.writeMetricsMap.get(consistency_level).unavailables.mark();
			Tracing.trace("Overloaded");
			throw e;
		} finally {
			long latency = (System.nanoTime()) - startTime;
			StorageProxy.writeMetrics.addNano(latency);
			StorageProxy.writeMetricsMap.get(consistency_level).addNano(latency);
		}
	}

	private static void hintMutations(Collection<? extends IMutation> mutations) {
		for (IMutation mutation : mutations)
			if (!(mutation instanceof CounterMutation))
				StorageProxy.hintMutation(((Mutation) (mutation)));


		Tracing.trace("Wrote hints to satisfy CL.ANY after no replicas acknowledged the write");
	}

	private static void hintMutation(Mutation mutation) {
		String keyspaceName = mutation.getKeyspaceName();
		Token token = mutation.key().getToken();
		Iterable<InetAddress> endpoints = StorageService.instance.getNaturalAndPendingEndpoints(keyspaceName, token);
		ArrayList<InetAddress> endpointsToHint = new ArrayList<>(Iterables.size(endpoints));
		for (InetAddress target : endpoints)
			if ((!(target.equals(FBUtilities.getBroadcastAddress()))) && (StorageProxy.shouldHint(target)))
				endpointsToHint.add(target);


		StorageProxy.submitHint(mutation, endpointsToHint, null);
	}

	public boolean appliesLocally(Mutation mutation) {
		String keyspaceName = mutation.getKeyspaceName();
		Token token = mutation.key().getToken();
		InetAddress local = FBUtilities.getBroadcastAddress();
		return (StorageService.instance.getNaturalEndpoints(keyspaceName, token).contains(local)) || (StorageService.instance.getTokenMetadata().pendingEndpointsFor(token, keyspaceName).contains(local));
	}

	public static void mutateMV(ByteBuffer dataKey, Collection<Mutation> mutations, boolean writeCommitLog, AtomicLong baseComplete, long queryStartNanoTime) throws OverloadedException, UnavailableException, WriteTimeoutException {
		Tracing.trace("Determining replicas for mutation");
		final String localDataCenter = DatabaseDescriptor.getEndpointSnitch().getDatacenter(FBUtilities.getBroadcastAddress());
		long startTime = System.nanoTime();
		try {
			final UUID batchUUID = UUIDGen.getTimeUUID();
			if (((StorageService.instance.isStarting()) || (StorageService.instance.isJoining())) || (StorageService.instance.isMoving())) {
				BatchlogManager.store(Batch.createLocal(batchUUID, FBUtilities.timestampMicros(), mutations), writeCommitLog);
			}else {
				List<StorageProxy.WriteResponseHandlerWrapper> wrappers = new ArrayList<>(mutations.size());
				Set<Mutation> nonLocalMutations = new HashSet<>(mutations);
				Token baseToken = StorageService.instance.getTokenMetadata().partitioner.getToken(dataKey);
				ConsistencyLevel consistencyLevel = ConsistencyLevel.ONE;
				final Collection<InetAddress> batchlogEndpoints = Collections.singleton(FBUtilities.getBroadcastAddress());
				BatchlogResponseHandler.BatchlogCleanup cleanup = new BatchlogResponseHandler.BatchlogCleanup(mutations.size(), () -> StorageProxy.asyncRemoveFromBatchlog(batchlogEndpoints, batchUUID));
				for (Mutation mutation : mutations) {
					String keyspaceName = mutation.getKeyspaceName();
					Token tk = mutation.key().getToken();
					Optional<InetAddress> pairedEndpoint = ViewUtils.getViewNaturalEndpoint(keyspaceName, baseToken, tk);
					Collection<InetAddress> pendingEndpoints = StorageService.instance.getTokenMetadata().pendingEndpointsFor(tk, keyspaceName);
					if (!(pairedEndpoint.isPresent())) {
						if (pendingEndpoints.isEmpty())
							StorageProxy.logger.warn(("Received base materialized view mutation for key {} that does not belong " + (("to this node. There is probably a range movement happening (move or decommission)," + "but this node hasn't updated its ring metadata yet. Adding mutation to ") + "local batchlog to be replayed later.")), mutation.key());

						continue;
					}
					if ((pairedEndpoint.get().equals(FBUtilities.getBroadcastAddress())) && (StorageService.instance.isJoined())) {
						try {
							mutation.apply(writeCommitLog);
							nonLocalMutations.remove(mutation);
							cleanup.ackMutation();
						} catch (Exception exc) {
							StorageProxy.logger.error("Error applying local view update to keyspace {}: {}", mutation.getKeyspaceName(), mutation);
							throw exc;
						}
					}else {
						wrappers.add(StorageProxy.wrapViewBatchResponseHandler(mutation, consistencyLevel, consistencyLevel, Collections.singletonList(pairedEndpoint.get()), baseComplete, WriteType.BATCH, cleanup, queryStartNanoTime));
					}
				}
				if (!(nonLocalMutations.isEmpty()))
					BatchlogManager.store(Batch.createLocal(batchUUID, FBUtilities.timestampMicros(), nonLocalMutations), writeCommitLog);

				if (!(wrappers.isEmpty()))
					StorageProxy.asyncWriteBatchedMutations(wrappers, localDataCenter, Stage.VIEW_MUTATION);

			}
		} finally {
			StorageProxy.viewWriteMetrics.addNano(((System.nanoTime()) - startTime));
		}
	}

	@SuppressWarnings("unchecked")
	public static void mutateWithTriggers(Collection<? extends IMutation> mutations, ConsistencyLevel consistencyLevel, boolean mutateAtomically, long queryStartNanoTime) throws InvalidRequestException, OverloadedException, UnavailableException, WriteFailureException, WriteTimeoutException {
		Collection<Mutation> augmented = TriggerExecutor.instance.execute(mutations);
		boolean updatesView = Keyspace.open(mutations.iterator().next().getKeyspaceName()).viewManager.updatesAffectView(mutations, true);
		if (augmented != null)
			StorageProxy.mutateAtomically(augmented, consistencyLevel, updatesView, queryStartNanoTime);
		else {
			if (mutateAtomically || updatesView)
				StorageProxy.mutateAtomically(((Collection<Mutation>) (mutations)), consistencyLevel, updatesView, queryStartNanoTime);
			else
				StorageProxy.mutate(mutations, consistencyLevel, queryStartNanoTime);

		}
	}

	public static void mutateAtomically(Collection<Mutation> mutations, ConsistencyLevel consistency_level, boolean requireQuorumForRemove, long queryStartNanoTime) throws OverloadedException, UnavailableException, WriteTimeoutException {
		Tracing.trace("Determining replicas for atomic batch");
		long startTime = System.nanoTime();
		List<StorageProxy.WriteResponseHandlerWrapper> wrappers = new ArrayList<StorageProxy.WriteResponseHandlerWrapper>(mutations.size());
		String localDataCenter = DatabaseDescriptor.getEndpointSnitch().getDatacenter(FBUtilities.getBroadcastAddress());
		try {
			ConsistencyLevel batchConsistencyLevel = (requireQuorumForRemove) ? ConsistencyLevel.QUORUM : consistency_level;
			switch (consistency_level) {
				case ALL :
				case EACH_QUORUM :
					batchConsistencyLevel = consistency_level;
			}
			final StorageProxy.BatchlogEndpoints batchlogEndpoints = StorageProxy.getBatchlogEndpoints(localDataCenter, batchConsistencyLevel);
			final UUID batchUUID = UUIDGen.getTimeUUID();
			BatchlogResponseHandler.BatchlogCleanup cleanup = new BatchlogResponseHandler.BatchlogCleanup(mutations.size(), () -> StorageProxy.asyncRemoveFromBatchlog(batchlogEndpoints, batchUUID, queryStartNanoTime));
			for (Mutation mutation : mutations) {
				StorageProxy.WriteResponseHandlerWrapper wrapper = StorageProxy.wrapBatchResponseHandler(mutation, consistency_level, batchConsistencyLevel, WriteType.BATCH, cleanup, queryStartNanoTime);
				wrapper.handler.assureSufficientLiveNodes();
				wrappers.add(wrapper);
			}
			StorageProxy.syncWriteToBatchlog(mutations, batchlogEndpoints, batchUUID, queryStartNanoTime);
			StorageProxy.syncWriteBatchedMutations(wrappers, localDataCenter, Stage.MUTATION);
		} catch (UnavailableException e) {
			StorageProxy.writeMetrics.unavailables.mark();
			StorageProxy.writeMetricsMap.get(consistency_level).unavailables.mark();
			Tracing.trace("Unavailable");
			throw e;
		} catch (WriteTimeoutException e) {
			StorageProxy.writeMetrics.timeouts.mark();
			StorageProxy.writeMetricsMap.get(consistency_level).timeouts.mark();
			Tracing.trace("Write timeout; received {} of {} required replies", e.received, e.blockFor);
			throw e;
		} catch (WriteFailureException e) {
			StorageProxy.writeMetrics.failures.mark();
			StorageProxy.writeMetricsMap.get(consistency_level).failures.mark();
			Tracing.trace("Write failure; received {} of {} required replies", e.received, e.blockFor);
			throw e;
		} finally {
			long latency = (System.nanoTime()) - startTime;
			StorageProxy.writeMetrics.addNano(latency);
			StorageProxy.writeMetricsMap.get(consistency_level).addNano(latency);
		}
	}

	public static boolean canDoLocalRequest(InetAddress replica) {
		return replica.equals(FBUtilities.getBroadcastAddress());
	}

	private static void syncWriteToBatchlog(Collection<Mutation> mutations, StorageProxy.BatchlogEndpoints endpoints, UUID uuid, long queryStartNanoTime) throws WriteFailureException, WriteTimeoutException {
		WriteResponseHandler<?> handler = new WriteResponseHandler<>(endpoints.all, Collections.<InetAddress>emptyList(), ((endpoints.all.size()) == 1 ? ConsistencyLevel.ONE : ConsistencyLevel.TWO), Keyspace.open(SchemaConstants.SYSTEM_KEYSPACE_NAME), null, WriteType.BATCH_LOG, queryStartNanoTime);
		Batch batch = Batch.createLocal(uuid, FBUtilities.timestampMicros(), mutations);
		if (!(endpoints.current.isEmpty()))
			StorageProxy.syncWriteToBatchlog(handler, batch, endpoints.current);

		if (!(endpoints.legacy.isEmpty()))
			LegacyBatchlogMigrator.syncWriteToBatchlog(handler, batch, endpoints.legacy);

		handler.get();
	}

	private static void syncWriteToBatchlog(WriteResponseHandler<?> handler, Batch batch, Collection<InetAddress> endpoints) throws WriteFailureException, WriteTimeoutException {
		MessageOut<Batch> message = new MessageOut<>(BATCH_STORE, batch, Batch.serializer);
		for (InetAddress target : endpoints) {
			StorageProxy.logger.trace("Sending batchlog store request {} to {} for {} mutations", batch.id, target, batch.size());
			if (StorageProxy.canDoLocalRequest(target))
				StorageProxy.performLocally(Stage.MUTATION, Optional.empty(), () -> BatchlogManager.store(batch), handler);
			else
				MessagingService.instance().sendRR(message, target, handler);

		}
	}

	private static void asyncRemoveFromBatchlog(StorageProxy.BatchlogEndpoints endpoints, UUID uuid, long queryStartNanoTime) {
		if (!(endpoints.current.isEmpty()))
			StorageProxy.asyncRemoveFromBatchlog(endpoints.current, uuid);

		if (!(endpoints.legacy.isEmpty()))
			LegacyBatchlogMigrator.asyncRemoveFromBatchlog(endpoints.legacy, uuid, queryStartNanoTime);

	}

	private static void asyncRemoveFromBatchlog(Collection<InetAddress> endpoints, UUID uuid) {
		MessageOut<UUID> message = new MessageOut<>(BATCH_REMOVE, uuid, UUIDSerializer.serializer);
		for (InetAddress target : endpoints) {
			if (StorageProxy.logger.isTraceEnabled())
				StorageProxy.logger.trace("Sending batchlog remove request {} to {}", uuid, target);

			if (StorageProxy.canDoLocalRequest(target))
				StorageProxy.performLocally(Stage.MUTATION, () -> BatchlogManager.remove(uuid));
			else
				MessagingService.instance().sendOneWay(message, target);

		}
	}

	private static void asyncWriteBatchedMutations(List<StorageProxy.WriteResponseHandlerWrapper> wrappers, String localDataCenter, Stage stage) {
		for (StorageProxy.WriteResponseHandlerWrapper wrapper : wrappers) {
			try {
			} catch (OverloadedException | WriteTimeoutException e) {
				wrapper.handler.onFailure(FBUtilities.getBroadcastAddress(), RequestFailureReason.UNKNOWN);
			}
		}
	}

	private static void syncWriteBatchedMutations(List<StorageProxy.WriteResponseHandlerWrapper> wrappers, String localDataCenter, Stage stage) throws OverloadedException, WriteTimeoutException {
		for (StorageProxy.WriteResponseHandlerWrapper wrapper : wrappers) {
		}
		for (StorageProxy.WriteResponseHandlerWrapper wrapper : wrappers)
			wrapper.handler.get();

	}

	public static AbstractWriteResponseHandler<IMutation> performWrite(IMutation mutation, ConsistencyLevel consistency_level, String localDataCenter, StorageProxy.WritePerformer performer, Runnable callback, WriteType writeType, long queryStartNanoTime) throws OverloadedException, UnavailableException {
		String keyspaceName = mutation.getKeyspaceName();
		AbstractReplicationStrategy rs = Keyspace.open(keyspaceName).getReplicationStrategy();
		Token tk = mutation.key().getToken();
		List<InetAddress> naturalEndpoints = StorageService.instance.getNaturalEndpoints(keyspaceName, tk);
		Collection<InetAddress> pendingEndpoints = StorageService.instance.getTokenMetadata().pendingEndpointsFor(tk, keyspaceName);
		AbstractWriteResponseHandler<IMutation> responseHandler = rs.getWriteResponseHandler(naturalEndpoints, pendingEndpoints, consistency_level, callback, writeType, queryStartNanoTime);
		responseHandler.assureSufficientLiveNodes();
		performer.apply(mutation, Iterables.concat(naturalEndpoints, pendingEndpoints), responseHandler, localDataCenter, consistency_level);
		return responseHandler;
	}

	private static StorageProxy.WriteResponseHandlerWrapper wrapBatchResponseHandler(Mutation mutation, ConsistencyLevel consistency_level, ConsistencyLevel batchConsistencyLevel, WriteType writeType, BatchlogResponseHandler.BatchlogCleanup cleanup, long queryStartNanoTime) {
		Keyspace keyspace = Keyspace.open(mutation.getKeyspaceName());
		AbstractReplicationStrategy rs = keyspace.getReplicationStrategy();
		String keyspaceName = mutation.getKeyspaceName();
		Token tk = mutation.key().getToken();
		List<InetAddress> naturalEndpoints = StorageService.instance.getNaturalEndpoints(keyspaceName, tk);
		Collection<InetAddress> pendingEndpoints = StorageService.instance.getTokenMetadata().pendingEndpointsFor(tk, keyspaceName);
		AbstractWriteResponseHandler<IMutation> writeHandler = rs.getWriteResponseHandler(naturalEndpoints, pendingEndpoints, consistency_level, null, writeType, queryStartNanoTime);
		BatchlogResponseHandler<IMutation> batchHandler = new BatchlogResponseHandler<>(writeHandler, batchConsistencyLevel.blockFor(keyspace), cleanup, queryStartNanoTime);
		return new StorageProxy.WriteResponseHandlerWrapper(batchHandler, mutation);
	}

	private static StorageProxy.WriteResponseHandlerWrapper wrapViewBatchResponseHandler(Mutation mutation, ConsistencyLevel consistency_level, ConsistencyLevel batchConsistencyLevel, List<InetAddress> naturalEndpoints, AtomicLong baseComplete, WriteType writeType, BatchlogResponseHandler.BatchlogCleanup cleanup, long queryStartNanoTime) {
		Keyspace keyspace = Keyspace.open(mutation.getKeyspaceName());
		AbstractReplicationStrategy rs = keyspace.getReplicationStrategy();
		String keyspaceName = mutation.getKeyspaceName();
		Token tk = mutation.key().getToken();
		Collection<InetAddress> pendingEndpoints = StorageService.instance.getTokenMetadata().pendingEndpointsFor(tk, keyspaceName);
		AbstractWriteResponseHandler<IMutation> writeHandler = rs.getWriteResponseHandler(naturalEndpoints, pendingEndpoints, consistency_level, () -> {
			long delay = Math.max(0, ((System.currentTimeMillis()) - (baseComplete.get())));
			StorageProxy.viewWriteMetrics.viewWriteLatency.update(delay, TimeUnit.MILLISECONDS);
		}, writeType, queryStartNanoTime);
		BatchlogResponseHandler<IMutation> batchHandler = new StorageProxy.ViewWriteMetricsWrapped(writeHandler, batchConsistencyLevel.blockFor(keyspace), cleanup, queryStartNanoTime);
		return new StorageProxy.WriteResponseHandlerWrapper(batchHandler, mutation);
	}

	private static class WriteResponseHandlerWrapper {
		final BatchlogResponseHandler<IMutation> handler;

		final Mutation mutation;

		WriteResponseHandlerWrapper(BatchlogResponseHandler<IMutation> handler, Mutation mutation) {
			this.handler = handler;
			this.mutation = mutation;
		}
	}

	private static final class BatchlogEndpoints {
		public final Collection<InetAddress> all;

		public final Collection<InetAddress> current;

		public final Collection<InetAddress> legacy;

		BatchlogEndpoints(Collection<InetAddress> endpoints) {
			all = endpoints;
			current = new ArrayList<>(2);
			legacy = new ArrayList<>(2);
			for (InetAddress ep : endpoints) {
				if ((MessagingService.instance().getVersion(ep)) >= (MessagingService.VERSION_30))
					current.add(ep);
				else
					legacy.add(ep);

			}
		}
	}

	private static StorageProxy.BatchlogEndpoints getBatchlogEndpoints(String localDataCenter, ConsistencyLevel consistencyLevel) throws UnavailableException {
		TokenMetadata.Topology topology = StorageService.instance.getTokenMetadata().cachedOnlyTokenMap().getTopology();
		Multimap<String, InetAddress> localEndpoints = HashMultimap.create(topology.getDatacenterRacks().get(localDataCenter));
		String localRack = DatabaseDescriptor.getEndpointSnitch().getRack(FBUtilities.getBroadcastAddress());
		Collection<InetAddress> chosenEndpoints = new BatchlogManager.EndpointFilter(localRack, localEndpoints).filter();
		if (chosenEndpoints.isEmpty()) {
			if (consistencyLevel == (ConsistencyLevel.ANY))
				return new StorageProxy.BatchlogEndpoints(Collections.singleton(FBUtilities.getBroadcastAddress()));

			throw new UnavailableException(ConsistencyLevel.ONE, 1, 0);
		}
		return new StorageProxy.BatchlogEndpoints(chosenEndpoints);
	}

	public static void sendToHintedEndpoints(final Mutation mutation, Iterable<InetAddress> targets, AbstractWriteResponseHandler<IMutation> responseHandler, String localDataCenter, Stage stage) throws OverloadedException {
		int targetsSize = Iterables.size(targets);
		Collection<InetAddress> localDc = null;
		Map<String, Collection<InetAddress>> dcGroups = null;
		MessageOut<Mutation> message = null;
		boolean insertLocal = false;
		ArrayList<InetAddress> endpointsToHint = null;
		List<InetAddress> backPressureHosts = null;
		for (InetAddress destination : targets) {
			StorageProxy.checkHintOverload(destination);
			if (FailureDetector.instance.isAlive(destination)) {
				if (StorageProxy.canDoLocalRequest(destination)) {
					insertLocal = true;
				}else {
					if (message == null)
						message = mutation.createMessage();

					String dc = DatabaseDescriptor.getEndpointSnitch().getDatacenter(destination);
					if (localDataCenter.equals(dc)) {
						if (localDc == null)
							localDc = new ArrayList<>(targetsSize);

						localDc.add(destination);
					}else {
						Collection<InetAddress> messages = (dcGroups != null) ? dcGroups.get(dc) : null;
						if (messages == null) {
							messages = new ArrayList<>(3);
							if (dcGroups == null)
								dcGroups = new HashMap<>();

							dcGroups.put(dc, messages);
						}
						messages.add(destination);
					}
					if (backPressureHosts == null)
						backPressureHosts = new ArrayList<>(targetsSize);

					backPressureHosts.add(destination);
				}
			}else {
				if (StorageProxy.shouldHint(destination)) {
					if (endpointsToHint == null)
						endpointsToHint = new ArrayList<>(targetsSize);

					endpointsToHint.add(destination);
				}
			}
		}
		if (backPressureHosts != null)
			MessagingService.instance().applyBackPressure(backPressureHosts, responseHandler.currentTimeout());

		if (endpointsToHint != null)
			StorageProxy.submitHint(mutation, endpointsToHint, responseHandler);

		if (insertLocal)
			StorageProxy.performLocally(stage, Optional.of(mutation), mutation::apply, responseHandler);

		if (localDc != null) {
			for (InetAddress destination : localDc)
				MessagingService.instance().sendRR(message, destination, responseHandler, true);

		}
		if (dcGroups != null) {
			for (Collection<InetAddress> dcTargets : dcGroups.values())
				StorageProxy.sendMessagesToNonlocalDC(message, dcTargets, responseHandler);

		}
	}

	private static void checkHintOverload(InetAddress destination) {
		if (((StorageMetrics.totalHintsInProgress.getCount()) > (StorageProxy.maxHintsInProgress)) && (((StorageProxy.getHintsInProgressFor(destination).get()) > 0) && (StorageProxy.shouldHint(destination)))) {
			throw new OverloadedException(((((("Too many in flight hints: " + (StorageMetrics.totalHintsInProgress.getCount())) + " destination: ") + destination) + " destination hints: ") + (StorageProxy.getHintsInProgressFor(destination).get())));
		}
	}

	private static void sendMessagesToNonlocalDC(MessageOut<? extends IMutation> message, Collection<InetAddress> targets, AbstractWriteResponseHandler<IMutation> handler) {
		Iterator<InetAddress> iter = targets.iterator();
		InetAddress target = iter.next();
		try (DataOutputBuffer out = new DataOutputBuffer()) {
			out.writeInt(((targets.size()) - 1));
			while (iter.hasNext()) {
				InetAddress destination = iter.next();
				CompactEndpointSerializationHelper.serialize(destination, out);
				int id = MessagingService.instance().addCallback(handler, message, destination, message.getTimeout(), handler.consistencyLevel, true);
				out.writeInt(id);
				StorageProxy.logger.trace("Adding FWD message to {}@{}", id, destination);
			} 
			message = message.withParameter(Mutation.FORWARD_TO, out.getData());
			int id = MessagingService.instance().sendRR(message, target, handler, true);
			StorageProxy.logger.trace("Sending message to {}@{}", id, target);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	private static void performLocally(Stage stage, final Runnable runnable) {
		StageManager.getStage(stage).maybeExecuteImmediately(new StorageProxy.LocalMutationRunnable() {
			public void runMayThrow() {
				try {
					runnable.run();
				} catch (Exception ex) {
					StorageProxy.logger.error("Failed to apply mutation locally : {}", ex);
				}
			}

			@Override
			protected MessagingService.Verb verb() {
				return MUTATION;
			}
		});
	}

	private static void performLocally(Stage stage, Optional<IMutation> mutation, final Runnable runnable, final IAsyncCallbackWithFailure<?> handler) {
		StageManager.getStage(stage).maybeExecuteImmediately(new StorageProxy.LocalMutationRunnable(mutation) {
			public void runMayThrow() {
				try {
					runnable.run();
					handler.response(null);
				} catch (Exception ex) {
					if (!(ex instanceof WriteTimeoutException))
						StorageProxy.logger.error("Failed to apply mutation locally : {}", ex);

					handler.onFailure(FBUtilities.getBroadcastAddress(), RequestFailureReason.UNKNOWN);
				}
			}

			@Override
			protected MessagingService.Verb verb() {
				return MUTATION;
			}
		});
	}

	public static AbstractWriteResponseHandler<IMutation> mutateCounter(CounterMutation cm, String localDataCenter, long queryStartNanoTime) throws OverloadedException, UnavailableException {
		InetAddress endpoint = StorageProxy.findSuitableEndpoint(cm.getKeyspaceName(), cm.key(), localDataCenter, cm.consistency());
		if (endpoint.equals(FBUtilities.getBroadcastAddress())) {
			return StorageProxy.applyCounterMutationOnCoordinator(cm, localDataCenter, queryStartNanoTime);
		}else {
			String keyspaceName = cm.getKeyspaceName();
			AbstractReplicationStrategy rs = Keyspace.open(keyspaceName).getReplicationStrategy();
			Token tk = cm.key().getToken();
			List<InetAddress> naturalEndpoints = StorageService.instance.getNaturalEndpoints(keyspaceName, tk);
			Collection<InetAddress> pendingEndpoints = StorageService.instance.getTokenMetadata().pendingEndpointsFor(tk, keyspaceName);
			rs.getWriteResponseHandler(naturalEndpoints, pendingEndpoints, cm.consistency(), null, WriteType.COUNTER, queryStartNanoTime).assureSufficientLiveNodes();
			AbstractWriteResponseHandler<IMutation> responseHandler = new WriteResponseHandler<>(endpoint, WriteType.COUNTER, queryStartNanoTime);
			Tracing.trace("Enqueuing counter update to {}", endpoint);
			MessagingService.instance().sendRR(cm.makeMutationMessage(), endpoint, responseHandler, false);
			return responseHandler;
		}
	}

	private static InetAddress findSuitableEndpoint(String keyspaceName, DecoratedKey key, String localDataCenter, ConsistencyLevel cl) throws UnavailableException {
		Keyspace keyspace = Keyspace.open(keyspaceName);
		IEndpointSnitch snitch = DatabaseDescriptor.getEndpointSnitch();
		List<InetAddress> endpoints = new ArrayList<>();
		StorageService.instance.getLiveNaturalEndpoints(keyspace, key, endpoints);
		endpoints.removeIf(( endpoint) -> !(StorageService.instance.isRpcReady(endpoint)));
		if (endpoints.isEmpty())
			throw new UnavailableException(cl, cl.blockFor(keyspace), 0);

		List<InetAddress> localEndpoints = new ArrayList<>(endpoints.size());
		for (InetAddress endpoint : endpoints)
			if (snitch.getDatacenter(endpoint).equals(localDataCenter))
				localEndpoints.add(endpoint);


		if (localEndpoints.isEmpty()) {
			if (cl.isDatacenterLocal())
				throw new UnavailableException(cl, cl.blockFor(keyspace), 0);

			snitch.sortByProximity(FBUtilities.getBroadcastAddress(), endpoints);
			return endpoints.get(0);
		}
		return localEndpoints.get(ThreadLocalRandom.current().nextInt(localEndpoints.size()));
	}

	public static AbstractWriteResponseHandler<IMutation> applyCounterMutationOnLeader(CounterMutation cm, String localDataCenter, Runnable callback, long queryStartNanoTime) throws OverloadedException, UnavailableException {
		return StorageProxy.performWrite(cm, cm.consistency(), localDataCenter, StorageProxy.counterWritePerformer, callback, WriteType.COUNTER, queryStartNanoTime);
	}

	public static AbstractWriteResponseHandler<IMutation> applyCounterMutationOnCoordinator(CounterMutation cm, String localDataCenter, long queryStartNanoTime) throws OverloadedException, UnavailableException {
		return StorageProxy.performWrite(cm, cm.consistency(), localDataCenter, StorageProxy.counterWriteOnCoordinatorPerformer, null, WriteType.COUNTER, queryStartNanoTime);
	}

	private static Runnable counterWriteTask(final IMutation mutation, final Iterable<InetAddress> targets, final AbstractWriteResponseHandler<IMutation> responseHandler, final String localDataCenter) {
		return new StorageProxy.DroppableRunnable(COUNTER_MUTATION) {
			@Override
			public void runMayThrow() throws OverloadedException, WriteTimeoutException {
				assert mutation instanceof CounterMutation;
				Mutation result = ((CounterMutation) (mutation)).applyCounterMutation();
				responseHandler.response(null);
				Set<InetAddress> remotes = Sets.difference(ImmutableSet.copyOf(targets), ImmutableSet.of(FBUtilities.getBroadcastAddress()));
				if (!(remotes.isEmpty()))
					StorageProxy.sendToHintedEndpoints(result, remotes, responseHandler, localDataCenter, Stage.COUNTER_MUTATION);

			}
		};
	}

	private static boolean systemKeyspaceQuery(List<? extends ReadCommand> cmds) {
		for (ReadCommand cmd : cmds)
			if (!(SchemaConstants.isLocalSystemKeyspace(cmd.metadata().ksName)))
				return false;


		return true;
	}

	public static RowIterator readOne(SinglePartitionReadCommand command, ConsistencyLevel consistencyLevel, long queryStartNanoTime) throws InvalidRequestException, IsBootstrappingException, ReadFailureException, ReadTimeoutException, UnavailableException {
		return StorageProxy.readOne(command, consistencyLevel, null, queryStartNanoTime);
	}

	public static RowIterator readOne(SinglePartitionReadCommand command, ConsistencyLevel consistencyLevel, ClientState state, long queryStartNanoTime) throws InvalidRequestException, IsBootstrappingException, ReadFailureException, ReadTimeoutException, UnavailableException {
		return PartitionIterators.getOnlyElement(StorageProxy.read(one(command), consistencyLevel, state, queryStartNanoTime), command);
	}

	public static PartitionIterator read(SinglePartitionReadCommand.Group group, ConsistencyLevel consistencyLevel, long queryStartNanoTime) throws InvalidRequestException, IsBootstrappingException, ReadFailureException, ReadTimeoutException, UnavailableException {
		assert !(consistencyLevel.isSerialConsistency());
		return StorageProxy.read(group, consistencyLevel, null, queryStartNanoTime);
	}

	public static PartitionIterator read(SinglePartitionReadCommand.Group group, ConsistencyLevel consistencyLevel, ClientState state, long queryStartNanoTime) throws InvalidRequestException, IsBootstrappingException, ReadFailureException, ReadTimeoutException, UnavailableException {
		if ((StorageService.instance.isBootstrapMode()) && (!(StorageProxy.systemKeyspaceQuery(group.commands)))) {
			StorageProxy.readMetrics.unavailables.mark();
			StorageProxy.readMetricsMap.get(consistencyLevel).unavailables.mark();
			throw new IsBootstrappingException();
		}
		return consistencyLevel.isSerialConsistency() ? StorageProxy.readWithPaxos(group, consistencyLevel, state, queryStartNanoTime) : StorageProxy.readRegular(group, consistencyLevel, queryStartNanoTime);
	}

	private static PartitionIterator readWithPaxos(SinglePartitionReadCommand.Group group, ConsistencyLevel consistencyLevel, ClientState state, long queryStartNanoTime) throws InvalidRequestException, ReadFailureException, ReadTimeoutException, UnavailableException {
		assert state != null;
		if ((group.commands.size()) > 1)
			throw new InvalidRequestException("SERIAL/LOCAL_SERIAL consistency may only be requested for one partition at a time");

		long start = System.nanoTime();
		SinglePartitionReadCommand command = group.commands.get(0);
		CFMetaData metadata = command.metadata();
		DecoratedKey key = command.partitionKey();
		PartitionIterator result = null;
		try {
			Pair<List<InetAddress>, Integer> p = StorageProxy.getPaxosParticipants(metadata, key, consistencyLevel);
			List<InetAddress> liveEndpoints = p.left;
			int requiredParticipants = p.right;
			final ConsistencyLevel consistencyForCommitOrFetch = (consistencyLevel == (ConsistencyLevel.LOCAL_SERIAL)) ? ConsistencyLevel.LOCAL_QUORUM : ConsistencyLevel.QUORUM;
			try {
				final Pair<UUID, Integer> pair = StorageProxy.beginAndRepairPaxos(start, key, metadata, liveEndpoints, requiredParticipants, consistencyLevel, consistencyForCommitOrFetch, false, state);
				if ((pair.right) > 0)
					StorageProxy.casReadMetrics.contention.update(pair.right);

			} catch (WriteTimeoutException e) {
				throw new ReadTimeoutException(consistencyLevel, 0, consistencyLevel.blockFor(Keyspace.open(metadata.ksName)), false);
			} catch (WriteFailureException e) {
				throw new ReadFailureException(consistencyLevel, e.received, e.blockFor, false, e.failureReasonByEndpoint);
			}
			result = StorageProxy.fetchRows(group.commands, consistencyForCommitOrFetch, queryStartNanoTime);
		} catch (UnavailableException e) {
			StorageProxy.readMetrics.unavailables.mark();
			StorageProxy.casReadMetrics.unavailables.mark();
			StorageProxy.readMetricsMap.get(consistencyLevel).unavailables.mark();
			throw e;
		} catch (ReadTimeoutException e) {
			StorageProxy.readMetrics.timeouts.mark();
			StorageProxy.casReadMetrics.timeouts.mark();
			StorageProxy.readMetricsMap.get(consistencyLevel).timeouts.mark();
			throw e;
		} catch (ReadFailureException e) {
			StorageProxy.readMetrics.failures.mark();
			StorageProxy.casReadMetrics.failures.mark();
			StorageProxy.readMetricsMap.get(consistencyLevel).failures.mark();
			throw e;
		} finally {
			long latency = (System.nanoTime()) - start;
			StorageProxy.readMetrics.addNano(latency);
			StorageProxy.casReadMetrics.addNano(latency);
			StorageProxy.readMetricsMap.get(consistencyLevel).addNano(latency);
			Keyspace.open(metadata.ksName).getColumnFamilyStore(metadata.cfName).metric.coordinatorReadLatency.update(latency, TimeUnit.NANOSECONDS);
		}
		return result;
	}

	@SuppressWarnings("resource")
	private static PartitionIterator readRegular(SinglePartitionReadCommand.Group group, ConsistencyLevel consistencyLevel, long queryStartNanoTime) throws ReadFailureException, ReadTimeoutException, UnavailableException {
		long start = System.nanoTime();
		try {
			PartitionIterator result = StorageProxy.fetchRows(group.commands, consistencyLevel, queryStartNanoTime);
			boolean enforceStrictLiveness = group.commands.get(0).metadata().enforceStrictLiveness();
			if ((group.commands.size()) > 1)
				result = group.limits().filter(result, group.nowInSec(), group.selectsFullPartition(), enforceStrictLiveness);

			return result;
		} catch (UnavailableException e) {
			StorageProxy.readMetrics.unavailables.mark();
			StorageProxy.readMetricsMap.get(consistencyLevel).unavailables.mark();
			throw e;
		} catch (ReadTimeoutException e) {
			StorageProxy.readMetrics.timeouts.mark();
			StorageProxy.readMetricsMap.get(consistencyLevel).timeouts.mark();
			throw e;
		} catch (ReadFailureException e) {
			StorageProxy.readMetrics.failures.mark();
			StorageProxy.readMetricsMap.get(consistencyLevel).failures.mark();
			throw e;
		} finally {
			long latency = (System.nanoTime()) - start;
			StorageProxy.readMetrics.addNano(latency);
			StorageProxy.readMetricsMap.get(consistencyLevel).addNano(latency);
			for (ReadCommand command : group.commands)
				Keyspace.openAndGetStore(command.metadata()).metric.coordinatorReadLatency.update(latency, TimeUnit.NANOSECONDS);

		}
	}

	private static PartitionIterator fetchRows(List<SinglePartitionReadCommand> commands, ConsistencyLevel consistencyLevel, long queryStartNanoTime) throws ReadFailureException, ReadTimeoutException, UnavailableException {
		int cmdCount = commands.size();
		StorageProxy.SinglePartitionReadLifecycle[] reads = new StorageProxy.SinglePartitionReadLifecycle[cmdCount];
		for (int i = 0; i < cmdCount; i++)
			reads[i] = new StorageProxy.SinglePartitionReadLifecycle(commands.get(i), consistencyLevel, queryStartNanoTime);

		for (int i = 0; i < cmdCount; i++)
			reads[i].doInitialQueries();

		for (int i = 0; i < cmdCount; i++)
			reads[i].maybeTryAdditionalReplicas();

		for (int i = 0; i < cmdCount; i++)
			reads[i].awaitResultsAndRetryOnDigestMismatch();

		for (int i = 0; i < cmdCount; i++)
			if (!(reads[i].isDone()))
				reads[i].maybeAwaitFullDataRead();


		List<PartitionIterator> results = new ArrayList<>(cmdCount);
		for (int i = 0; i < cmdCount; i++) {
			assert reads[i].isDone();
			results.add(reads[i].getResult());
		}
		return PartitionIterators.concat(results);
	}

	private static class SinglePartitionReadLifecycle {
		private final SinglePartitionReadCommand command;

		private final AbstractReadExecutor executor;

		private final ConsistencyLevel consistency;

		private final long queryStartNanoTime;

		private PartitionIterator result;

		private ReadCallback repairHandler;

		SinglePartitionReadLifecycle(SinglePartitionReadCommand command, ConsistencyLevel consistency, long queryStartNanoTime) {
			this.command = command;
			this.executor = AbstractReadExecutor.getReadExecutor(command, consistency, queryStartNanoTime);
			this.consistency = consistency;
			this.queryStartNanoTime = queryStartNanoTime;
		}

		boolean isDone() {
			return (result) != null;
		}

		void doInitialQueries() {
			executor.executeAsync();
		}

		void maybeTryAdditionalReplicas() {
			executor.maybeTryAdditionalReplicas();
		}

		void awaitResultsAndRetryOnDigestMismatch() throws ReadFailureException, ReadTimeoutException {
			try {
				result = executor.get();
			} catch (DigestMismatchException ex) {
				Tracing.trace("Digest mismatch: {}", ex);
				ReadRepairMetrics.repairedBlocking.mark();
				Keyspace keyspace = Keyspace.open(command.metadata().ksName);
				for (InetAddress endpoint : executor.getContactedReplicas()) {
					MessageOut<ReadCommand> message = command.createMessage(MessagingService.instance().getVersion(endpoint));
					Tracing.trace("Enqueuing full data read to {}", endpoint);
					MessagingService.instance().sendRRWithFailure(message, endpoint, repairHandler);
				}
			}
		}

		void maybeAwaitFullDataRead() throws ReadTimeoutException {
			if ((repairHandler) == null)
				return;

			try {
				result = repairHandler.get();
			} catch (DigestMismatchException e) {
				throw new AssertionError(e);
			} catch (ReadTimeoutException e) {
				if (Tracing.isTracing())
					Tracing.trace("Timed out waiting on digest mismatch repair requests");
				else
					StorageProxy.logger.trace("Timed out waiting on digest mismatch repair requests");

				int blockFor = consistency.blockFor(Keyspace.open(command.metadata().ksName));
				throw new ReadTimeoutException(consistency, (blockFor - 1), blockFor, true);
			}
		}

		PartitionIterator getResult() {
			assert (result) != null;
			return result;
		}
	}

	static class LocalReadRunnable extends StorageProxy.DroppableRunnable {
		private final ReadCommand command;

		private final ReadCallback handler;

		private final long start = System.nanoTime();

		LocalReadRunnable(ReadCommand command, ReadCallback handler) {
			super(READ);
			this.command = command;
			this.handler = handler;
		}

		protected void runMayThrow() {
			try {
				command.setMonitoringTime(constructionTime, false, verb.getTimeout(), DatabaseDescriptor.getSlowQueryTimeout());
				ReadResponse response;
				try (ReadExecutionController executionController = command.executionController();UnfilteredPartitionIterator iterator = command.executeLocally(executionController)) {
					response = command.createResponse(iterator);
				}
				if (command.complete()) {
					handler.response(response);
				}else {
					MessagingService.instance().incrementDroppedMessages(verb, ((System.currentTimeMillis()) - (constructionTime)));
					handler.onFailure(FBUtilities.getBroadcastAddress(), RequestFailureReason.UNKNOWN);
				}
				MessagingService.instance().addLatency(FBUtilities.getBroadcastAddress(), TimeUnit.NANOSECONDS.toMillis(((System.nanoTime()) - (start))));
			} catch (Throwable t) {
				if (t instanceof TombstoneOverwhelmingException) {
					handler.onFailure(FBUtilities.getBroadcastAddress(), RequestFailureReason.READ_TOO_MANY_TOMBSTONES);
					StorageProxy.logger.error(t.getMessage());
				}else {
					handler.onFailure(FBUtilities.getBroadcastAddress(), RequestFailureReason.UNKNOWN);
					throw t;
				}
			}
		}
	}

	public static List<InetAddress> getLiveSortedEndpoints(Keyspace keyspace, ByteBuffer key) {
		return StorageProxy.getLiveSortedEndpoints(keyspace, StorageService.instance.getTokenMetadata().decorateKey(key));
	}

	public static List<InetAddress> getLiveSortedEndpoints(Keyspace keyspace, RingPosition pos) {
		List<InetAddress> liveEndpoints = StorageService.instance.getLiveNaturalEndpoints(keyspace, pos);
		DatabaseDescriptor.getEndpointSnitch().sortByProximity(FBUtilities.getBroadcastAddress(), liveEndpoints);
		return liveEndpoints;
	}

	private static List<InetAddress> intersection(List<InetAddress> l1, List<InetAddress> l2) {
		List<InetAddress> inter = new ArrayList<InetAddress>(l1);
		inter.retainAll(l2);
		return inter;
	}

	private static float estimateResultsPerRange(PartitionRangeReadCommand command, Keyspace keyspace) {
		ColumnFamilyStore cfs = keyspace.getColumnFamilyStore(command.metadata().cfId);
		Index index = command.getIndex(cfs);
		float maxExpectedResults = (index == null) ? command.limits().estimateTotalResults(cfs) : index.getEstimatedResultRows();
		return (maxExpectedResults / (DatabaseDescriptor.getNumTokens())) / (keyspace.getReplicationStrategy().getReplicationFactor());
	}

	private static class RangeForQuery {
		public final AbstractBounds<PartitionPosition> range;

		public final List<InetAddress> liveEndpoints;

		public final List<InetAddress> filteredEndpoints;

		public RangeForQuery(AbstractBounds<PartitionPosition> range, List<InetAddress> liveEndpoints, List<InetAddress> filteredEndpoints) {
			this.range = range;
			this.liveEndpoints = liveEndpoints;
			this.filteredEndpoints = filteredEndpoints;
		}
	}

	private static class RangeIterator extends AbstractIterator<StorageProxy.RangeForQuery> {
		private final Keyspace keyspace;

		private final ConsistencyLevel consistency;

		private final Iterator<? extends AbstractBounds<PartitionPosition>> ranges;

		private final int rangeCount;

		public RangeIterator(PartitionRangeReadCommand command, Keyspace keyspace, ConsistencyLevel consistency) {
			this.keyspace = keyspace;
			this.consistency = consistency;
			List<? extends AbstractBounds<PartitionPosition>> l = ((keyspace.getReplicationStrategy()) instanceof LocalStrategy) ? command.dataRange().keyRange().unwrap() : StorageProxy.getRestrictedRanges(command.dataRange().keyRange());
			this.ranges = l.iterator();
			this.rangeCount = l.size();
		}

		public int rangeCount() {
			return rangeCount;
		}

		protected StorageProxy.RangeForQuery computeNext() {
			if (!(ranges.hasNext()))
				return endOfData();

			AbstractBounds<PartitionPosition> range = ranges.next();
			List<InetAddress> liveEndpoints = StorageProxy.getLiveSortedEndpoints(keyspace, range.right);
			return new StorageProxy.RangeForQuery(range, liveEndpoints, consistency.filterForQuery(keyspace, liveEndpoints));
		}
	}

	private static class RangeMerger extends AbstractIterator<StorageProxy.RangeForQuery> {
		private final Keyspace keyspace;

		private final ConsistencyLevel consistency;

		private final PeekingIterator<StorageProxy.RangeForQuery> ranges;

		private RangeMerger(Iterator<StorageProxy.RangeForQuery> iterator, Keyspace keyspace, ConsistencyLevel consistency) {
			this.keyspace = keyspace;
			this.consistency = consistency;
			this.ranges = Iterators.peekingIterator(iterator);
		}

		protected StorageProxy.RangeForQuery computeNext() {
			if (!(ranges.hasNext()))
				return endOfData();

			StorageProxy.RangeForQuery current = ranges.next();
			while (ranges.hasNext()) {
				if (current.range.right.isMinimum())
					break;

				StorageProxy.RangeForQuery next = ranges.peek();
				List<InetAddress> merged = StorageProxy.intersection(current.liveEndpoints, next.liveEndpoints);
				if (!(consistency.isSufficientLiveNodes(keyspace, merged)))
					break;

				List<InetAddress> filteredMerged = consistency.filterForQuery(keyspace, merged);
				if (!(DatabaseDescriptor.getEndpointSnitch().isWorthMergingForRangeQuery(filteredMerged, current.filteredEndpoints, next.filteredEndpoints)))
					break;

				current = new StorageProxy.RangeForQuery(current.range.withNewRight(next.range.right), merged, filteredMerged);
				ranges.next();
			} 
			return current;
		}
	}

	private static class SingleRangeResponse extends AbstractIterator<RowIterator> implements PartitionIterator {
		private final ReadCallback handler;

		private PartitionIterator result;

		private SingleRangeResponse(ReadCallback handler) {
			this.handler = handler;
		}

		private void waitForResponse() throws ReadTimeoutException {
			if ((result) != null)
				return;

			try {
				result = handler.get();
			} catch (DigestMismatchException e) {
				throw new AssertionError(e);
			}
		}

		protected RowIterator computeNext() {
			waitForResponse();
			return result.hasNext() ? result.next() : endOfData();
		}

		public void close() {
			if ((result) != null)
				result.close();

		}
	}

	private static class RangeCommandIterator extends AbstractIterator<RowIterator> implements PartitionIterator {
		private final Iterator<StorageProxy.RangeForQuery> ranges;

		private final int totalRangeCount;

		private final PartitionRangeReadCommand command;

		private final Keyspace keyspace;

		private final ConsistencyLevel consistency;

		private final boolean enforceStrictLiveness;

		private final long startTime;

		private final long queryStartNanoTime;

		private DataLimits.Counter counter;

		private PartitionIterator sentQueryIterator;

		private int concurrencyFactor;

		private int liveReturned;

		private int rangesQueried;

		public RangeCommandIterator(StorageProxy.RangeIterator ranges, PartitionRangeReadCommand command, int concurrencyFactor, Keyspace keyspace, ConsistencyLevel consistency, long queryStartNanoTime) {
			this.command = command;
			this.concurrencyFactor = concurrencyFactor;
			this.startTime = System.nanoTime();
			this.ranges = new StorageProxy.RangeMerger(ranges, keyspace, consistency);
			this.totalRangeCount = ranges.rangeCount();
			this.consistency = consistency;
			this.keyspace = keyspace;
			this.queryStartNanoTime = queryStartNanoTime;
			this.enforceStrictLiveness = command.metadata().enforceStrictLiveness();
		}

		public RowIterator computeNext() {
			try {
				while (((sentQueryIterator) == null) || (!(sentQueryIterator.hasNext()))) {
					if (!(ranges.hasNext()))
						return endOfData();

					if ((sentQueryIterator) != null) {
						liveReturned += counter.counted();
						sentQueryIterator.close();
						updateConcurrencyFactor();
					}
					sentQueryIterator = sendNextRequests();
				} 
				return sentQueryIterator.next();
			} catch (UnavailableException e) {
				StorageProxy.rangeMetrics.unavailables.mark();
				throw e;
			} catch (ReadTimeoutException e) {
				StorageProxy.rangeMetrics.timeouts.mark();
				throw e;
			} catch (ReadFailureException e) {
				StorageProxy.rangeMetrics.failures.mark();
				throw e;
			}
		}

		private void updateConcurrencyFactor() {
			if ((liveReturned) == 0) {
				concurrencyFactor = (totalRangeCount) - (rangesQueried);
				return;
			}
			int remainingRows = (command.limits().count()) - (liveReturned);
			float rowsPerRange = ((float) (liveReturned)) / ((float) (rangesQueried));
			concurrencyFactor = Math.max(1, Math.min(((totalRangeCount) - (rangesQueried)), Math.round((remainingRows / rowsPerRange))));
			StorageProxy.logger.trace("Didn't get enough response rows; actual rows per range: {}; remaining rows: {}, new concurrent requests: {}", rowsPerRange, remainingRows, concurrencyFactor);
		}

		private StorageProxy.SingleRangeResponse query(StorageProxy.RangeForQuery toQuery, boolean isFirst) {
			PartitionRangeReadCommand rangeCommand = command.forSubRange(toQuery.range, isFirst);
			int blockFor = consistency.blockFor(keyspace);
			int minResponses = Math.min(toQuery.filteredEndpoints.size(), blockFor);
			List<InetAddress> minimalEndpoints = toQuery.filteredEndpoints.subList(0, minResponses);
			if (((toQuery.filteredEndpoints.size()) == 1) && (StorageProxy.canDoLocalRequest(toQuery.filteredEndpoints.get(0)))) {
			}else {
				for (InetAddress endpoint : toQuery.filteredEndpoints) {
					MessageOut<ReadCommand> message = rangeCommand.createMessage(MessagingService.instance().getVersion(endpoint));
					Tracing.trace("Enqueuing request to {}", endpoint);
				}
			}
			return null;
		}

		private PartitionIterator sendNextRequests() {
			List<PartitionIterator> concurrentQueries = new ArrayList<>(concurrencyFactor);
			for (int i = 0; (i < (concurrencyFactor)) && (ranges.hasNext()); i++) {
				concurrentQueries.add(query(ranges.next(), (i == 0)));
				++(rangesQueried);
			}
			Tracing.trace("Submitted {} concurrent range requests", concurrentQueries.size());
			counter = DataLimits.NONE.newCounter(command.nowInSec(), true, command.selectsFullPartition(), enforceStrictLiveness);
			return counter.applyTo(PartitionIterators.concat(concurrentQueries));
		}

		public void close() {
			try {
				if ((sentQueryIterator) != null)
					sentQueryIterator.close();

			} finally {
				long latency = (System.nanoTime()) - (startTime);
				StorageProxy.rangeMetrics.addNano(latency);
				Keyspace.openAndGetStore(command.metadata()).metric.coordinatorScanLatency.update(latency, TimeUnit.NANOSECONDS);
			}
		}
	}

	@SuppressWarnings("resource")
	public static PartitionIterator getRangeSlice(PartitionRangeReadCommand command, ConsistencyLevel consistencyLevel, long queryStartNanoTime) {
		Tracing.trace("Computing ranges to query");
		Keyspace keyspace = Keyspace.open(command.metadata().ksName);
		StorageProxy.RangeIterator ranges = new StorageProxy.RangeIterator(command, keyspace, consistencyLevel);
		float resultsPerRange = StorageProxy.estimateResultsPerRange(command, keyspace);
		resultsPerRange -= resultsPerRange * (StorageProxy.CONCURRENT_SUBREQUESTS_MARGIN);
		int concurrencyFactor = (resultsPerRange == 0.0) ? 1 : Math.max(1, Math.min(ranges.rangeCount(), ((int) (Math.ceil(((command.limits().count()) / resultsPerRange))))));
		StorageProxy.logger.trace("Estimated result rows per range: {}; requested rows: {}, ranges.size(): {}; concurrent range requests: {}", resultsPerRange, command.limits().count(), ranges.rangeCount(), concurrencyFactor);
		Tracing.trace("Submitting range requests on {} ranges with a concurrency of {} ({} rows per range expected)", ranges.rangeCount(), concurrencyFactor, resultsPerRange);
		return command.limits().filter(command.postReconciliationProcessing(new StorageProxy.RangeCommandIterator(ranges, command, concurrencyFactor, keyspace, consistencyLevel, queryStartNanoTime)), command.nowInSec(), command.selectsFullPartition(), command.metadata().enforceStrictLiveness());
	}

	public Map<String, List<String>> getSchemaVersions() {
		return StorageProxy.describeSchemaVersions();
	}

	public static Map<String, List<String>> describeSchemaVersions() {
		final String myVersion = Schema.instance.getVersion().toString();
		final Map<InetAddress, UUID> versions = new ConcurrentHashMap<InetAddress, UUID>();
		final Set<InetAddress> liveHosts = Gossiper.instance.getLiveMembers();
		final CountDownLatch latch = new CountDownLatch(liveHosts.size());
		IAsyncCallback<UUID> cb = new IAsyncCallback<UUID>() {
			public void response(MessageIn<UUID> message) {
				versions.put(message.from, message.payload);
				latch.countDown();
			}

			public boolean isLatencyForSnitch() {
				return false;
			}
		};
		MessageOut message = new MessageOut(SCHEMA_CHECK);
		for (InetAddress endpoint : liveHosts)
			MessagingService.instance().sendRR(message, endpoint, cb);

		try {
			latch.await(DatabaseDescriptor.getRpcTimeout(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex) {
			throw new AssertionError("This latch shouldn't have been interrupted.");
		}
		Map<String, List<String>> results = new HashMap<String, List<String>>();
		Iterable<InetAddress> allHosts = Iterables.concat(Gossiper.instance.getLiveMembers(), Gossiper.instance.getUnreachableMembers());
		for (InetAddress host : allHosts) {
			UUID version = versions.get(host);
			String stringVersion = (version == null) ? StorageProxy.UNREACHABLE : version.toString();
			List<String> hosts = results.get(stringVersion);
			if (hosts == null) {
				hosts = new ArrayList<String>();
				results.put(stringVersion, hosts);
			}
			hosts.add(host.getHostAddress());
		}
		if ((results.get(StorageProxy.UNREACHABLE)) != null)
			StorageProxy.logger.debug("Hosts not in agreement. Didn't get a response from everybody: {}", StringUtils.join(results.get(StorageProxy.UNREACHABLE), ","));

		for (Map.Entry<String, List<String>> entry : results.entrySet()) {
			if ((entry.getKey().equals(StorageProxy.UNREACHABLE)) || (entry.getKey().equals(myVersion)))
				continue;

			for (String host : entry.getValue())
				StorageProxy.logger.debug("{} disagrees ({})", host, entry.getKey());

		}
		if ((results.size()) == 1)
			StorageProxy.logger.debug("Schemas are in agreement.");

		return results;
	}

	static <T extends RingPosition<T>> List<AbstractBounds<T>> getRestrictedRanges(final AbstractBounds<T> queryRange) {
		if (((queryRange instanceof Bounds) && (queryRange.left.equals(queryRange.right))) && (!(queryRange.left.isMinimum()))) {
			return Collections.singletonList(queryRange);
		}
		TokenMetadata tokenMetadata = StorageService.instance.getTokenMetadata();
		List<AbstractBounds<T>> ranges = new ArrayList<AbstractBounds<T>>();
		Iterator<Token> ringIter = TokenMetadata.ringIterator(tokenMetadata.sortedTokens(), queryRange.left.getToken(), true);
		AbstractBounds<T> remainder = queryRange;
		while (ringIter.hasNext()) {
			Token upperBoundToken = ringIter.next();
			T upperBound = ((T) (upperBoundToken.upperBound(queryRange.left.getClass())));
			if ((!(remainder.left.equals(upperBound))) && (!(remainder.contains(upperBound))))
				break;

			Pair<AbstractBounds<T>, AbstractBounds<T>> splits = remainder.split(upperBound);
			if (splits == null)
				continue;

			ranges.add(splits.left);
			remainder = splits.right;
		} 
		ranges.add(remainder);
		return ranges;
	}

	public boolean getHintedHandoffEnabled() {
		return DatabaseDescriptor.hintedHandoffEnabled();
	}

	public void setHintedHandoffEnabled(boolean b) {
		synchronized(StorageService.instance) {
			DatabaseDescriptor.setHintedHandoffEnabled(b);
		}
	}

	public void enableHintsForDC(String dc) {
		DatabaseDescriptor.enableHintsForDC(dc);
	}

	public void disableHintsForDC(String dc) {
		DatabaseDescriptor.disableHintsForDC(dc);
	}

	public Set<String> getHintedHandoffDisabledDCs() {
		return DatabaseDescriptor.hintedHandoffDisabledDCs();
	}

	public int getMaxHintWindow() {
		return DatabaseDescriptor.getMaxHintWindow();
	}

	public void setMaxHintWindow(int ms) {
		DatabaseDescriptor.setMaxHintWindow(ms);
	}

	public static boolean shouldHint(InetAddress ep) {
		if (DatabaseDescriptor.hintedHandoffEnabled()) {
			Set<String> disabledDCs = DatabaseDescriptor.hintedHandoffDisabledDCs();
			if (!(disabledDCs.isEmpty())) {
				final String dc = DatabaseDescriptor.getEndpointSnitch().getDatacenter(ep);
				if (disabledDCs.contains(dc)) {
					Tracing.trace("Not hinting {} since its data center {} has been disabled {}", ep, dc, disabledDCs);
					return false;
				}
			}
			boolean hintWindowExpired = (Gossiper.instance.getEndpointDowntime(ep)) > (DatabaseDescriptor.getMaxHintWindow());
			if (hintWindowExpired) {
				HintsService.instance.metrics.incrPastWindow(ep);
				Tracing.trace("Not hinting {} which has been down {} ms", ep, Gossiper.instance.getEndpointDowntime(ep));
			}
			return !hintWindowExpired;
		}else {
			return false;
		}
	}

	public static void truncateBlocking(String keyspace, String cfname) throws TimeoutException, UnavailableException {
		StorageProxy.logger.debug("Starting a blocking truncate operation on keyspace {}, CF {}", keyspace, cfname);
		if (StorageProxy.isAnyStorageHostDown()) {
			StorageProxy.logger.info("Cannot perform truncate, some hosts are down");
			int liveMembers = Gossiper.instance.getLiveMembers().size();
			throw new UnavailableException(ConsistencyLevel.ALL, (liveMembers + (Gossiper.instance.getUnreachableMembers().size())), liveMembers);
		}
		Set<InetAddress> allEndpoints = StorageService.instance.getLiveRingMembers(true);
		int blockFor = allEndpoints.size();
		final TruncateResponseHandler responseHandler = new TruncateResponseHandler(blockFor);
		Tracing.trace("Enqueuing truncate messages to hosts {}", allEndpoints);
		final Truncation truncation = new Truncation(keyspace, cfname);
		MessageOut<Truncation> message = truncation.createMessage();
		for (InetAddress endpoint : allEndpoints)
			MessagingService.instance().sendRR(message, endpoint, responseHandler);

		try {
			responseHandler.get();
		} catch (TimeoutException e) {
			Tracing.trace("Timed out");
			throw e;
		}
	}

	private static boolean isAnyStorageHostDown() {
		return !(Gossiper.instance.getUnreachableTokenOwners().isEmpty());
	}

	public interface WritePerformer {
		public void apply(IMutation mutation, Iterable<InetAddress> targets, AbstractWriteResponseHandler<IMutation> responseHandler, String localDataCenter, ConsistencyLevel consistencyLevel) throws OverloadedException;
	}

	private static class ViewWriteMetricsWrapped extends BatchlogResponseHandler<IMutation> {
		public ViewWriteMetricsWrapped(AbstractWriteResponseHandler<IMutation> writeHandler, int i, BatchlogResponseHandler.BatchlogCleanup cleanup, long queryStartNanoTime) {
			super(writeHandler, i, cleanup, queryStartNanoTime);
			StorageProxy.viewWriteMetrics.viewReplicasAttempted.inc(totalEndpoints());
		}

		public void response(MessageIn<IMutation> msg) {
			super.response(msg);
			StorageProxy.viewWriteMetrics.viewReplicasSuccess.inc();
		}
	}

	private static abstract class DroppableRunnable implements Runnable {
		final long constructionTime;

		final MessagingService.Verb verb;

		public DroppableRunnable(MessagingService.Verb verb) {
			this.constructionTime = System.currentTimeMillis();
			this.verb = verb;
		}

		public final void run() {
			long timeTaken = (System.currentTimeMillis()) - (constructionTime);
			if (timeTaken > (verb.getTimeout())) {
				MessagingService.instance().incrementDroppedMessages(verb, timeTaken);
				return;
			}
			try {
				runMayThrow();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		protected abstract void runMayThrow() throws Exception;
	}

	private static abstract class LocalMutationRunnable implements Runnable {
		private final long constructionTime = System.currentTimeMillis();

		private final Optional<IMutation> mutationOpt;

		public LocalMutationRunnable(Optional<IMutation> mutationOpt) {
			this.mutationOpt = mutationOpt;
		}

		public LocalMutationRunnable() {
			this.mutationOpt = Optional.empty();
		}

		public final void run() {
			final MessagingService.Verb verb = verb();
			long mutationTimeout = verb.getTimeout();
			long timeTaken = (System.currentTimeMillis()) - (constructionTime);
			if (timeTaken > mutationTimeout) {
				if (MessagingService.DROPPABLE_VERBS.contains(verb))
					MessagingService.instance().incrementDroppedMutations(mutationOpt, timeTaken);

				StorageProxy.HintRunnable runnable = new StorageProxy.HintRunnable(Collections.singleton(FBUtilities.getBroadcastAddress())) {
					protected void runMayThrow() throws Exception {
						StorageProxy.LocalMutationRunnable.this.runMayThrow();
					}
				};
				StorageProxy.submitHint(runnable);
				return;
			}
			try {
				runMayThrow();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		protected abstract MessagingService.Verb verb();

		protected abstract void runMayThrow() throws Exception;
	}

	private static abstract class HintRunnable implements Runnable {
		public final Collection<InetAddress> targets;

		protected HintRunnable(Collection<InetAddress> targets) {
			this.targets = targets;
		}

		public void run() {
			try {
				runMayThrow();
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				StorageMetrics.totalHintsInProgress.dec(targets.size());
				for (InetAddress target : targets)
					StorageProxy.getHintsInProgressFor(target).decrementAndGet();

			}
		}

		protected abstract void runMayThrow() throws Exception;
	}

	public long getTotalHints() {
		return StorageMetrics.totalHints.getCount();
	}

	public int getMaxHintsInProgress() {
		return StorageProxy.maxHintsInProgress;
	}

	public void setMaxHintsInProgress(int qs) {
		StorageProxy.maxHintsInProgress = qs;
	}

	public int getHintsInProgress() {
		return ((int) (StorageMetrics.totalHintsInProgress.getCount()));
	}

	public void verifyNoHintsInProgress() {
		if ((getHintsInProgress()) > 0)
			StorageProxy.logger.warn("Some hints were not written before shutdown.  This is not supposed to happen.  You should (a) run repair, and (b) file a bug report");

	}

	private static AtomicInteger getHintsInProgressFor(InetAddress destination) {
		try {
			return StorageProxy.hintsInProgress.load(destination);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	public static Future<Void> submitHint(Mutation mutation, InetAddress target, AbstractWriteResponseHandler<IMutation> responseHandler) {
		return StorageProxy.submitHint(mutation, Collections.singleton(target), responseHandler);
	}

	public static Future<Void> submitHint(Mutation mutation, Collection<InetAddress> targets, AbstractWriteResponseHandler<IMutation> responseHandler) {
		StorageProxy.HintRunnable runnable = new StorageProxy.HintRunnable(targets) {
			public void runMayThrow() {
				Set<InetAddress> validTargets = new HashSet<>(targets.size());
				Set<UUID> hostIds = new HashSet<>(targets.size());
				for (InetAddress target : targets) {
					UUID hostId = StorageService.instance.getHostIdForEndpoint(target);
					if (hostId != null) {
						hostIds.add(hostId);
						validTargets.add(target);
					}else
						StorageProxy.logger.debug("Discarding hint for endpoint not part of ring: {}", target);

				}
				StorageProxy.logger.trace("Adding hints for {}", validTargets);
				HintsService.instance.write(hostIds, Hint.create(mutation, System.currentTimeMillis()));
				validTargets.forEach(HintsService.instance.metrics::incrCreatedHints);
				if ((responseHandler != null) && ((responseHandler.consistencyLevel) == (ConsistencyLevel.ANY)))
					responseHandler.response(null);

			}
		};
		return StorageProxy.submitHint(runnable);
	}

	private static Future<Void> submitHint(StorageProxy.HintRunnable runnable) {
		StorageMetrics.totalHintsInProgress.inc(runnable.targets.size());
		for (InetAddress target : runnable.targets)
			StorageProxy.getHintsInProgressFor(target).incrementAndGet();

		return ((Future<Void>) (StageManager.getStage(Stage.MUTATION).submit(runnable)));
	}

	public Long getRpcTimeout() {
		return DatabaseDescriptor.getRpcTimeout();
	}

	public void setRpcTimeout(Long timeoutInMillis) {
		DatabaseDescriptor.setRpcTimeout(timeoutInMillis);
	}

	public Long getReadRpcTimeout() {
		return DatabaseDescriptor.getReadRpcTimeout();
	}

	public void setReadRpcTimeout(Long timeoutInMillis) {
		DatabaseDescriptor.setReadRpcTimeout(timeoutInMillis);
	}

	public Long getWriteRpcTimeout() {
		return DatabaseDescriptor.getWriteRpcTimeout();
	}

	public void setWriteRpcTimeout(Long timeoutInMillis) {
		DatabaseDescriptor.setWriteRpcTimeout(timeoutInMillis);
	}

	public Long getCounterWriteRpcTimeout() {
		return DatabaseDescriptor.getCounterWriteRpcTimeout();
	}

	public void setCounterWriteRpcTimeout(Long timeoutInMillis) {
		DatabaseDescriptor.setCounterWriteRpcTimeout(timeoutInMillis);
	}

	public Long getCasContentionTimeout() {
		return DatabaseDescriptor.getCasContentionTimeout();
	}

	public void setCasContentionTimeout(Long timeoutInMillis) {
		DatabaseDescriptor.setCasContentionTimeout(timeoutInMillis);
	}

	public Long getRangeRpcTimeout() {
		return DatabaseDescriptor.getRangeRpcTimeout();
	}

	public void setRangeRpcTimeout(Long timeoutInMillis) {
		DatabaseDescriptor.setRangeRpcTimeout(timeoutInMillis);
	}

	public Long getTruncateRpcTimeout() {
		return DatabaseDescriptor.getTruncateRpcTimeout();
	}

	public void setTruncateRpcTimeout(Long timeoutInMillis) {
		DatabaseDescriptor.setTruncateRpcTimeout(timeoutInMillis);
	}

	public Long getNativeTransportMaxConcurrentConnections() {
		return DatabaseDescriptor.getNativeTransportMaxConcurrentConnections();
	}

	public void setNativeTransportMaxConcurrentConnections(Long nativeTransportMaxConcurrentConnections) {
		DatabaseDescriptor.setNativeTransportMaxConcurrentConnections(nativeTransportMaxConcurrentConnections);
	}

	public Long getNativeTransportMaxConcurrentConnectionsPerIp() {
		return DatabaseDescriptor.getNativeTransportMaxConcurrentConnectionsPerIp();
	}

	public void setNativeTransportMaxConcurrentConnectionsPerIp(Long nativeTransportMaxConcurrentConnections) {
		DatabaseDescriptor.setNativeTransportMaxConcurrentConnectionsPerIp(nativeTransportMaxConcurrentConnections);
	}

	public void reloadTriggerClasses() {
		TriggerExecutor.instance.reloadClasses();
	}

	public long getReadRepairAttempted() {
		return ReadRepairMetrics.attempted.getCount();
	}

	public long getReadRepairRepairedBlocking() {
		return ReadRepairMetrics.repairedBlocking.getCount();
	}

	public long getReadRepairRepairedBackground() {
		return ReadRepairMetrics.repairedBackground.getCount();
	}

	public int getNumberOfTables() {
		return Schema.instance.getNumberOfTables();
	}

	public int getOtcBacklogExpirationInterval() {
		return DatabaseDescriptor.getOtcBacklogExpirationInterval();
	}

	public void setOtcBacklogExpirationInterval(int intervalInMillis) {
		DatabaseDescriptor.setOtcBacklogExpirationInterval(intervalInMillis);
	}
}

