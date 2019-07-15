

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.jmx.JMXConfiguratorMBean;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.hook.DelayingShutdownHook;
import ch.qos.logback.core.spi.ContextAwareBase;
import com.codahale.metrics.Counter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.apache.cassandra.auth.AuthKeyspace;
import org.apache.cassandra.auth.AuthMigrationListener;
import org.apache.cassandra.auth.IAuthenticator;
import org.apache.cassandra.auth.IAuthorizer;
import org.apache.cassandra.auth.IRoleManager;
import org.apache.cassandra.batchlog.BatchRemoveVerbHandler;
import org.apache.cassandra.batchlog.BatchStoreVerbHandler;
import org.apache.cassandra.batchlog.BatchlogManager;
import org.apache.cassandra.cache.AutoSavingCache;
import org.apache.cassandra.cache.CounterCacheKey;
import org.apache.cassandra.concurrent.DebuggableScheduledThreadPoolExecutor;
import org.apache.cassandra.concurrent.LocalAwareExecutorService;
import org.apache.cassandra.concurrent.NamedThreadFactory;
import org.apache.cassandra.concurrent.ScheduledExecutors;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.config.ViewDefinition;
import org.apache.cassandra.db.ClockAndCount;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.CounterMutationVerbHandler;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DefinitionsUpdateVerbHandler;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.MigrationRequestVerbHandler;
import org.apache.cassandra.db.MutationVerbHandler;
import org.apache.cassandra.db.RangeSliceVerbHandler;
import org.apache.cassandra.db.ReadCommandVerbHandler;
import org.apache.cassandra.db.ReadRepairVerbHandler;
import org.apache.cassandra.db.SchemaCheckVerbHandler;
import org.apache.cassandra.db.SizeEstimatesRecorder;
import org.apache.cassandra.db.SnapshotDetailsTabularData;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.TruncateVerbHandler;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.db.commitlog.CommitLogPosition;
import org.apache.cassandra.db.compaction.CompactionManager;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.dht.BootStrapper;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.RangeStreamer;
import org.apache.cassandra.dht.RingPosition;
import org.apache.cassandra.dht.StreamStateStore;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.exceptions.AlreadyExistsException;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.UnavailableException;
import org.apache.cassandra.gms.ApplicationState;
import org.apache.cassandra.gms.EndpointState;
import org.apache.cassandra.gms.FailureDetector;
import org.apache.cassandra.gms.GossipDigestAck2VerbHandler;
import org.apache.cassandra.gms.GossipDigestAckVerbHandler;
import org.apache.cassandra.gms.GossipDigestSynVerbHandler;
import org.apache.cassandra.gms.GossipShutdownVerbHandler;
import org.apache.cassandra.gms.Gossiper;
import org.apache.cassandra.gms.IEndpointStateChangeSubscriber;
import org.apache.cassandra.gms.IFailureDetector;
import org.apache.cassandra.gms.TokenSerializer;
import org.apache.cassandra.gms.VersionedValue;
import org.apache.cassandra.hints.HintVerbHandler;
import org.apache.cassandra.hints.HintsService;
import org.apache.cassandra.index.SecondaryIndexManager;
import org.apache.cassandra.io.sstable.SSTableLoader;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.locator.AbstractReplicationStrategy;
import org.apache.cassandra.locator.DynamicEndpointSnitch;
import org.apache.cassandra.locator.IEndpointSnitch;
import org.apache.cassandra.locator.LocalStrategy;
import org.apache.cassandra.locator.TokenMetadata;
import org.apache.cassandra.metrics.StorageMetrics;
import org.apache.cassandra.net.AsyncOneResponse;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.net.ResponseVerbHandler;
import org.apache.cassandra.repair.RepairMessageVerbHandler;
import org.apache.cassandra.repair.RepairParallelism;
import org.apache.cassandra.repair.SystemDistributedKeyspace;
import org.apache.cassandra.repair.messages.RepairOption;
import org.apache.cassandra.schema.CompactionParams;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.SchemaKeyspace;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.schema.Tables;
import org.apache.cassandra.schema.Views;
import org.apache.cassandra.service.ActiveRepairService;
import org.apache.cassandra.service.CacheService;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.EchoVerbHandler;
import org.apache.cassandra.service.IEndpointLifecycleSubscriber;
import org.apache.cassandra.service.LoadBroadcaster;
import org.apache.cassandra.service.MigrationManager;
import org.apache.cassandra.service.PendingRangeCalculatorService;
import org.apache.cassandra.service.SnapshotVerbHandler;
import org.apache.cassandra.service.StorageProxy;
import org.apache.cassandra.service.StorageServiceMBean;
import org.apache.cassandra.service.paxos.CommitVerbHandler;
import org.apache.cassandra.service.paxos.PrepareVerbHandler;
import org.apache.cassandra.service.paxos.ProposeVerbHandler;
import org.apache.cassandra.streaming.ReplicationFinishedVerbHandler;
import org.apache.cassandra.streaming.StreamManager;
import org.apache.cassandra.streaming.StreamPlan;
import org.apache.cassandra.streaming.StreamResultFuture;
import org.apache.cassandra.streaming.StreamState;
import org.apache.cassandra.thrift.EndpointDetails;
import org.apache.cassandra.thrift.TokenRange;
import org.apache.cassandra.tracing.TraceKeyspace;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.BiMultiValMap;
import org.apache.cassandra.utils.CassandraVersion;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.cassandra.utils.OutputHandler;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.Throwables;
import org.apache.cassandra.utils.WindowsTimer;
import org.apache.cassandra.utils.WrappedRunnable;
import org.apache.cassandra.utils.progress.ProgressEvent;
import org.apache.cassandra.utils.progress.ProgressEventNotifierSupport;
import org.apache.cassandra.utils.progress.ProgressEventType;
import org.apache.cassandra.utils.progress.jmx.JMXProgressSupport;
import org.apache.cassandra.utils.progress.jmx.LegacyJMXProgressSupport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.db.SystemKeyspace.BootstrapState.COMPLETED;
import static org.apache.cassandra.db.SystemKeyspace.BootstrapState.DECOMMISSIONED;
import static org.apache.cassandra.db.SystemKeyspace.BootstrapState.IN_PROGRESS;
import static org.apache.cassandra.db.SystemKeyspace.BootstrapState.NEEDS_BOOTSTRAP;
import static org.apache.cassandra.db.compaction.CompactionManager.AllSSTableOpStatus.SUCCESSFUL;
import static org.apache.cassandra.net.MessagingService.Verb.BATCH_REMOVE;
import static org.apache.cassandra.net.MessagingService.Verb.BATCH_STORE;
import static org.apache.cassandra.net.MessagingService.Verb.COUNTER_MUTATION;
import static org.apache.cassandra.net.MessagingService.Verb.DEFINITIONS_UPDATE;
import static org.apache.cassandra.net.MessagingService.Verb.ECHO;
import static org.apache.cassandra.net.MessagingService.Verb.GOSSIP_DIGEST_ACK;
import static org.apache.cassandra.net.MessagingService.Verb.GOSSIP_DIGEST_ACK2;
import static org.apache.cassandra.net.MessagingService.Verb.GOSSIP_DIGEST_SYN;
import static org.apache.cassandra.net.MessagingService.Verb.GOSSIP_SHUTDOWN;
import static org.apache.cassandra.net.MessagingService.Verb.HINT;
import static org.apache.cassandra.net.MessagingService.Verb.INTERNAL_RESPONSE;
import static org.apache.cassandra.net.MessagingService.Verb.MIGRATION_REQUEST;
import static org.apache.cassandra.net.MessagingService.Verb.MUTATION;
import static org.apache.cassandra.net.MessagingService.Verb.PAGED_RANGE;
import static org.apache.cassandra.net.MessagingService.Verb.PAXOS_COMMIT;
import static org.apache.cassandra.net.MessagingService.Verb.PAXOS_PREPARE;
import static org.apache.cassandra.net.MessagingService.Verb.PAXOS_PROPOSE;
import static org.apache.cassandra.net.MessagingService.Verb.RANGE_SLICE;
import static org.apache.cassandra.net.MessagingService.Verb.READ;
import static org.apache.cassandra.net.MessagingService.Verb.READ_REPAIR;
import static org.apache.cassandra.net.MessagingService.Verb.REPAIR_MESSAGE;
import static org.apache.cassandra.net.MessagingService.Verb.REPLICATION_FINISHED;
import static org.apache.cassandra.net.MessagingService.Verb.REQUEST_RESPONSE;
import static org.apache.cassandra.net.MessagingService.Verb.SCHEMA_CHECK;
import static org.apache.cassandra.net.MessagingService.Verb.SNAPSHOT;
import static org.apache.cassandra.net.MessagingService.Verb.TRUNCATE;
import static org.apache.cassandra.schema.CompactionParams.TombstoneOption.valueOf;


public class StorageService extends NotificationBroadcasterSupport implements IEndpointStateChangeSubscriber , StorageServiceMBean {
	private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

	public static final int RING_DELAY = StorageService.getRingDelay();

	private final JMXProgressSupport progressSupport = new JMXProgressSupport(this);

	@Deprecated
	private final LegacyJMXProgressSupport legacyProgressSupport;

	private static final AtomicInteger threadCounter = new AtomicInteger(1);

	private static int getRingDelay() {
		String newdelay = System.getProperty("cassandra.ring_delay_ms");
		if (newdelay != null) {
			StorageService.logger.info("Overriding RING_DELAY to {}ms", newdelay);
			return Integer.parseInt(newdelay);
		}else
			return 30 * 1000;

	}

	private TokenMetadata tokenMetadata = new TokenMetadata();

	public volatile VersionedValue.VersionedValueFactory valueFactory = new VersionedValue.VersionedValueFactory(tokenMetadata.partitioner);

	private Thread drainOnShutdown = null;

	private volatile boolean isShutdown = false;

	private final List<Runnable> preShutdownHooks = new ArrayList<>();

	private final List<Runnable> postShutdownHooks = new ArrayList<>();

	public static final StorageService instance = new StorageService();

	@Deprecated
	public boolean isInShutdownHook() {
		return isShutdown();
	}

	public boolean isShutdown() {
		return isShutdown;
	}

	public Collection<Range<Token>> getLocalRanges(String keyspaceName) {
		return getRangesForEndpoint(keyspaceName, FBUtilities.getBroadcastAddress());
	}

	public Collection<Range<Token>> getPrimaryRanges(String keyspace) {
		return getPrimaryRangesForEndpoint(keyspace, FBUtilities.getBroadcastAddress());
	}

	public Collection<Range<Token>> getPrimaryRangesWithinDC(String keyspace) {
		return getPrimaryRangeForEndpointWithinDC(keyspace, FBUtilities.getBroadcastAddress());
	}

	private final Set<InetAddress> replicatingNodes = Collections.synchronizedSet(new HashSet<InetAddress>());

	private CassandraDaemon daemon;

	private InetAddress removingNode;

	private volatile boolean isBootstrapMode;

	private boolean isSurveyMode = Boolean.parseBoolean(System.getProperty("cassandra.write_survey", "false"));

	private final AtomicBoolean isRebuilding = new AtomicBoolean();

	private final AtomicBoolean isDecommissioning = new AtomicBoolean();

	private volatile boolean initialized = false;

	private volatile boolean joined = false;

	private volatile boolean gossipActive = false;

	private final AtomicBoolean authSetupCalled = new AtomicBoolean(false);

	private volatile boolean authSetupComplete = false;

	private double traceProbability = 0.0;

	private static enum Mode {

		STARTING,
		NORMAL,
		JOINING,
		LEAVING,
		DECOMMISSIONED,
		MOVING,
		DRAINING,
		DRAINED;}

	private volatile StorageService.Mode operationMode = StorageService.Mode.STARTING;

	private volatile int totalCFs;

	private volatile int remainingCFs;

	private static final AtomicInteger nextRepairCommand = new AtomicInteger();

	private final List<IEndpointLifecycleSubscriber> lifecycleSubscribers = new CopyOnWriteArrayList<>();

	private final ObjectName jmxObjectName;

	private Collection<Token> bootstrapTokens = null;

	private static final boolean useStrictConsistency = Boolean.parseBoolean(System.getProperty("cassandra.consistent.rangemovement", "true"));

	private static final boolean allowSimultaneousMoves = Boolean.parseBoolean(System.getProperty("cassandra.consistent.simultaneousmoves.allow", "false"));

	private static final boolean joinRing = Boolean.parseBoolean(System.getProperty("cassandra.join_ring", "true"));

	private boolean replacing;

	private final StreamStateStore streamStateStore = new StreamStateStore();

	public void setTokens(Collection<Token> tokens) {
		assert (tokens != null) && (!(tokens.isEmpty())) : "Node needs at least one token.";
		if (StorageService.logger.isDebugEnabled())
			StorageService.logger.debug("Setting tokens to {}", tokens);

		SystemKeyspace.updateTokens(tokens);
		Collection<Token> localTokens = getLocalTokens();
		setGossipTokens(localTokens);
		tokenMetadata.updateNormalTokens(tokens, FBUtilities.getBroadcastAddress());
		setMode(StorageService.Mode.NORMAL, false);
	}

	public void setGossipTokens(Collection<Token> tokens) {
		List<Pair<ApplicationState, VersionedValue>> states = new ArrayList<Pair<ApplicationState, VersionedValue>>();
		states.add(Pair.create(ApplicationState.TOKENS, valueFactory.tokens(tokens)));
		states.add(Pair.create(ApplicationState.STATUS, valueFactory.normal(tokens)));
		Gossiper.instance.addLocalApplicationStates(states);
	}

	public StorageService() {
		super(Executors.newSingleThreadExecutor());
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			jmxObjectName = new ObjectName("org.apache.cassandra.db:type=StorageService");
			mbs.registerMBean(this, jmxObjectName);
			mbs.registerMBean(StreamManager.instance, new ObjectName(StreamManager.OBJECT_NAME));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		legacyProgressSupport = new LegacyJMXProgressSupport(this, jmxObjectName);
		MessagingService.instance().registerVerbHandlers(MUTATION, new MutationVerbHandler());
		MessagingService.instance().registerVerbHandlers(READ_REPAIR, new ReadRepairVerbHandler());
		MessagingService.instance().registerVerbHandlers(READ, new ReadCommandVerbHandler());
		MessagingService.instance().registerVerbHandlers(RANGE_SLICE, new RangeSliceVerbHandler());
		MessagingService.instance().registerVerbHandlers(PAGED_RANGE, new RangeSliceVerbHandler());
		MessagingService.instance().registerVerbHandlers(COUNTER_MUTATION, new CounterMutationVerbHandler());
		MessagingService.instance().registerVerbHandlers(TRUNCATE, new TruncateVerbHandler());
		MessagingService.instance().registerVerbHandlers(PAXOS_PREPARE, new PrepareVerbHandler());
		MessagingService.instance().registerVerbHandlers(PAXOS_PROPOSE, new ProposeVerbHandler());
		MessagingService.instance().registerVerbHandlers(PAXOS_COMMIT, new CommitVerbHandler());
		MessagingService.instance().registerVerbHandlers(HINT, new HintVerbHandler());
		MessagingService.instance().registerVerbHandlers(REPLICATION_FINISHED, new ReplicationFinishedVerbHandler());
		MessagingService.instance().registerVerbHandlers(REQUEST_RESPONSE, new ResponseVerbHandler());
		MessagingService.instance().registerVerbHandlers(INTERNAL_RESPONSE, new ResponseVerbHandler());
		MessagingService.instance().registerVerbHandlers(REPAIR_MESSAGE, new RepairMessageVerbHandler());
		MessagingService.instance().registerVerbHandlers(GOSSIP_SHUTDOWN, new GossipShutdownVerbHandler());
		MessagingService.instance().registerVerbHandlers(GOSSIP_DIGEST_SYN, new GossipDigestSynVerbHandler());
		MessagingService.instance().registerVerbHandlers(GOSSIP_DIGEST_ACK, new GossipDigestAckVerbHandler());
		MessagingService.instance().registerVerbHandlers(GOSSIP_DIGEST_ACK2, new GossipDigestAck2VerbHandler());
		MessagingService.instance().registerVerbHandlers(DEFINITIONS_UPDATE, new DefinitionsUpdateVerbHandler());
		MessagingService.instance().registerVerbHandlers(SCHEMA_CHECK, new SchemaCheckVerbHandler());
		MessagingService.instance().registerVerbHandlers(MIGRATION_REQUEST, new MigrationRequestVerbHandler());
		MessagingService.instance().registerVerbHandlers(SNAPSHOT, new SnapshotVerbHandler());
		MessagingService.instance().registerVerbHandlers(ECHO, new EchoVerbHandler());
		MessagingService.instance().registerVerbHandlers(BATCH_STORE, new BatchStoreVerbHandler());
		MessagingService.instance().registerVerbHandlers(BATCH_REMOVE, new BatchRemoveVerbHandler());
	}

	public void registerDaemon(CassandraDaemon daemon) {
		this.daemon = daemon;
	}

	public void register(IEndpointLifecycleSubscriber subscriber) {
		lifecycleSubscribers.add(subscriber);
	}

	public void unregister(IEndpointLifecycleSubscriber subscriber) {
		lifecycleSubscribers.remove(subscriber);
	}

	public void stopGossiping() {
		if (gossipActive) {
			StorageService.logger.warn("Stopping gossip by operator request");
			Gossiper.instance.stop();
			gossipActive = false;
		}
	}

	public synchronized void startGossiping() {
		if (!(gossipActive)) {
			checkServiceAllowedToStart("gossip");
			StorageService.logger.warn("Starting gossip by operator request");
			Collection<Token> tokens = SystemKeyspace.getSavedTokens();
			boolean validTokens = (tokens != null) && (!(tokens.isEmpty()));
			if ((joined) || (StorageService.joinRing))
				assert validTokens : "Cannot start gossiping for a node intended to join without valid tokens";

			if (validTokens)
				setGossipTokens(tokens);

			Gossiper.instance.forceNewerGeneration();
			Gossiper.instance.start(((int) ((System.currentTimeMillis()) / 1000)));
			gossipActive = true;
		}
	}

	public boolean isGossipRunning() {
		return Gossiper.instance.isEnabled();
	}

	public synchronized void startRPCServer() {
		checkServiceAllowedToStart("thrift");
		if ((daemon) == null) {
			throw new IllegalStateException("No configured daemon");
		}
		daemon.thriftServer.start();
	}

	public void stopRPCServer() {
		if ((daemon) == null) {
			throw new IllegalStateException("No configured daemon");
		}
		if ((daemon.thriftServer) != null)
			daemon.thriftServer.stop();

	}

	public boolean isRPCServerRunning() {
		if (((daemon) == null) || ((daemon.thriftServer) == null)) {
			return false;
		}
		return daemon.thriftServer.isRunning();
	}

	public synchronized void startNativeTransport() {
		checkServiceAllowedToStart("native transport");
		if ((daemon) == null) {
			throw new IllegalStateException("No configured daemon");
		}
		try {
			daemon.startNativeTransport();
		} catch (Exception e) {
			throw new RuntimeException(("Error starting native transport: " + (e.getMessage())));
		}
	}

	public void stopNativeTransport() {
		if ((daemon) == null) {
			throw new IllegalStateException("No configured daemon");
		}
		daemon.stopNativeTransport();
	}

	public boolean isNativeTransportRunning() {
		if ((daemon) == null) {
			return false;
		}
		return daemon.isNativeTransportRunning();
	}

	public void stopTransports() {
		if (isGossipActive()) {
			StorageService.logger.error("Stopping gossiper");
			stopGossiping();
		}
		if (isRPCServerRunning()) {
			StorageService.logger.error("Stopping RPC server");
			stopRPCServer();
		}
		if (isNativeTransportRunning()) {
			StorageService.logger.error("Stopping native transport");
			stopNativeTransport();
		}
	}

	private void shutdownClientServers() {
		setRpcReady(false);
		stopRPCServer();
		stopNativeTransport();
	}

	public void stopClient() {
		Gossiper.instance.unregister(this);
		Gossiper.instance.stop();
		MessagingService.instance().shutdown();
		Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
		StageManager.shutdownNow();
	}

	public boolean isInitialized() {
		return initialized;
	}

	public boolean isGossipActive() {
		return gossipActive;
	}

	public boolean isDaemonSetupCompleted() {
		return (daemon) == null ? false : daemon.setupCompleted();
	}

	public void stopDaemon() {
		if ((daemon) == null)
			throw new IllegalStateException("No configured daemon");

		daemon.deactivate();
	}

	private synchronized UUID prepareForReplacement() throws ConfigurationException {
		if (SystemKeyspace.bootstrapComplete())
			throw new RuntimeException("Cannot replace address with a node that is already bootstrapped");

		if (!(StorageService.joinRing))
			throw new ConfigurationException("Cannot set both join_ring=false and attempt to replace a node");

		if ((!(DatabaseDescriptor.isAutoBootstrap())) && (!(Boolean.getBoolean("cassandra.allow_unsafe_replace"))))
			throw new RuntimeException(("Replacing a node without bootstrapping risks invalidating consistency " + (("guarantees as the expected data may not be present until repair is run. " + "To perform this operation, please restart with ") + "-Dcassandra.allow_unsafe_replace=true")));

		InetAddress replaceAddress = DatabaseDescriptor.getReplaceAddress();
		StorageService.logger.info("Gathering node replacement information for {}", replaceAddress);
		Map<InetAddress, EndpointState> epStates = Gossiper.instance.doShadowRound();
		if ((epStates.get(replaceAddress)) == null)
			throw new RuntimeException(String.format("Cannot replace_address %s because it doesn't exist in gossip", replaceAddress));

		try {
			VersionedValue tokensVersionedValue = epStates.get(replaceAddress).getApplicationState(ApplicationState.TOKENS);
			if (tokensVersionedValue == null)
				throw new RuntimeException(String.format("Could not find tokens for %s to replace", replaceAddress));

			bootstrapTokens = TokenSerializer.deserialize(tokenMetadata.partitioner, new DataInputStream(new ByteArrayInputStream(tokensVersionedValue.toBytes())));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		UUID localHostId = SystemKeyspace.getLocalHostId();
		if (StorageService.isReplacingSameAddress()) {
			localHostId = Gossiper.instance.getHostId(replaceAddress, epStates);
			SystemKeyspace.setLocalHostId(localHostId);
		}
		return localHostId;
	}

	private synchronized void checkForEndpointCollision(UUID localHostId) throws ConfigurationException {
		if (Boolean.getBoolean("cassandra.allow_unsafe_join")) {
			StorageService.logger.warn("Skipping endpoint collision check as cassandra.allow_unsafe_join=true");
			return;
		}
		StorageService.logger.debug("Starting shadow gossip round to check for endpoint collision");
		Map<InetAddress, EndpointState> epStates = Gossiper.instance.doShadowRound();
		if (!(Gossiper.instance.isSafeForStartup(FBUtilities.getBroadcastAddress(), localHostId, shouldBootstrap(), epStates))) {
			throw new RuntimeException(String.format(("A node with address %s already exists, cancelling join. " + "Use cassandra.replace_address if you want to replace this node."), FBUtilities.getBroadcastAddress()));
		}
		if (((shouldBootstrap()) && (StorageService.useStrictConsistency)) && (!(allowSimultaneousMoves()))) {
			for (Map.Entry<InetAddress, EndpointState> entry : epStates.entrySet()) {
				if ((entry.getKey().equals(FBUtilities.getBroadcastAddress())) || ((entry.getValue().getApplicationState(ApplicationState.STATUS)) == null))
					continue;

				String[] pieces = StorageService.splitValue(entry.getValue().getApplicationState(ApplicationState.STATUS));
				assert (pieces.length) > 0;
				String state = pieces[0];
				if (((state.equals(VersionedValue.STATUS_BOOTSTRAPPING)) || (state.equals(VersionedValue.STATUS_LEAVING))) || (state.equals(VersionedValue.STATUS_MOVING)))
					throw new UnsupportedOperationException("Other bootstrapping/leaving/moving nodes detected, cannot bootstrap while cassandra.consistent.rangemovement is true");

			}
		}
	}

	private boolean allowSimultaneousMoves() {
		return (StorageService.allowSimultaneousMoves) && ((DatabaseDescriptor.getNumTokens()) == 1);
	}

	public void unsafeInitialize() throws ConfigurationException {
		initialized = true;
		gossipActive = true;
		Gossiper.instance.register(this);
		Gossiper.instance.start(((int) ((System.currentTimeMillis()) / 1000)));
		Gossiper.instance.addLocalApplicationState(ApplicationState.NET_VERSION, valueFactory.networkVersion());
		if (!(MessagingService.instance().isListening()))
			MessagingService.instance().listen();

	}

	public void populateTokenMetadata() {
		if (Boolean.parseBoolean(System.getProperty("cassandra.load_ring_state", "true"))) {
			StorageService.logger.info("Populating token metadata from system tables");
			Multimap<InetAddress, Token> loadedTokens = SystemKeyspace.loadTokens();
			if (!(shouldBootstrap()))
				loadedTokens.putAll(FBUtilities.getBroadcastAddress(), SystemKeyspace.getSavedTokens());

			for (InetAddress ep : loadedTokens.keySet())
				tokenMetadata.updateNormalTokens(loadedTokens.get(ep), ep);

			StorageService.logger.info("Token metadata: {}", tokenMetadata);
		}
	}

	public synchronized void initServer() throws ConfigurationException {
		initServer(StorageService.RING_DELAY);
	}

	public synchronized void initServer(int delay) throws ConfigurationException {
		StorageService.logger.info("Cassandra version: {}", FBUtilities.getReleaseVersionString());
		StorageService.logger.info("CQL supported versions: {} (default: {})", StringUtils.join(ClientState.getCQLSupportedVersion(), ", "), ClientState.DEFAULT_CQL_VERSION);
		StorageService.logger.info("Native protocol supported versions: {} (default: {})", StringUtils.join(ProtocolVersion.supportedVersions(), ", "), ProtocolVersion.CURRENT);
		try {
			Class.forName("org.apache.cassandra.service.StorageProxy");
			Class.forName("org.apache.cassandra.io.sstable.IndexSummaryManager");
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
		drainOnShutdown = NamedThreadFactory.createThread(new WrappedRunnable() {
			@Override
			public void runMayThrow() throws IOException, InterruptedException, ExecutionException {
				drain(true);
				if (FBUtilities.isWindows)
					WindowsTimer.endTimerPeriod(DatabaseDescriptor.getWindowsTimerInterval());

				DelayingShutdownHook logbackHook = new DelayingShutdownHook();
				logbackHook.setContext(((LoggerContext) (LoggerFactory.getILoggerFactory())));
				logbackHook.run();
			}
		}, "StorageServiceShutdownHook");
		Runtime.getRuntime().addShutdownHook(drainOnShutdown);
		replacing = isReplacing();
		if (!(Boolean.parseBoolean(System.getProperty("cassandra.start_gossip", "true")))) {
			StorageService.logger.info("Not starting gossip as requested.");
			loadRingState();
			initialized = true;
			return;
		}
		prepareToJoin();
		try {
			CacheService.instance.counterCache.loadSavedAsync().get();
		} catch (Throwable t) {
			JVMStabilityInspector.inspectThrowable(t);
			StorageService.logger.warn("Error loading counter cache", t);
		}
		if (StorageService.joinRing) {
			joinTokenRing(delay);
		}else {
			Collection<Token> tokens = SystemKeyspace.getSavedTokens();
			if (!(tokens.isEmpty())) {
				tokenMetadata.updateNormalTokens(tokens, FBUtilities.getBroadcastAddress());
				List<Pair<ApplicationState, VersionedValue>> states = new ArrayList<Pair<ApplicationState, VersionedValue>>();
				states.add(Pair.create(ApplicationState.TOKENS, valueFactory.tokens(tokens)));
				states.add(Pair.create(ApplicationState.STATUS, valueFactory.hibernate(true)));
				Gossiper.instance.addLocalApplicationStates(states);
			}
			doAuthSetup();
			StorageService.logger.info("Not joining ring as requested. Use JMX (StorageService->joinRing()) to initiate ring joining");
		}
		initialized = true;
	}

	private void loadRingState() {
		if (Boolean.parseBoolean(System.getProperty("cassandra.load_ring_state", "true"))) {
			StorageService.logger.info("Loading persisted ring state");
			Multimap<InetAddress, Token> loadedTokens = SystemKeyspace.loadTokens();
			Map<InetAddress, UUID> loadedHostIds = SystemKeyspace.loadHostIds();
			for (InetAddress ep : loadedTokens.keySet()) {
				if (ep.equals(FBUtilities.getBroadcastAddress())) {
					SystemKeyspace.removeEndpoint(ep);
				}else {
					if (loadedHostIds.containsKey(ep))
						tokenMetadata.updateHostId(loadedHostIds.get(ep), ep);

					Gossiper.instance.addSavedEndpoint(ep);
				}
			}
		}
	}

	private boolean isReplacing() {
		if (((System.getProperty("cassandra.replace_address_first_boot", null)) != null) && (SystemKeyspace.bootstrapComplete())) {
			StorageService.logger.info("Replace address on first boot requested; this node is already bootstrapped");
			return false;
		}
		return (DatabaseDescriptor.getReplaceAddress()) != null;
	}

	public void removeShutdownHook() {
		if ((drainOnShutdown) != null)
			Runtime.getRuntime().removeShutdownHook(drainOnShutdown);

		if (FBUtilities.isWindows)
			WindowsTimer.endTimerPeriod(DatabaseDescriptor.getWindowsTimerInterval());

	}

	private boolean shouldBootstrap() {
		return ((DatabaseDescriptor.isAutoBootstrap()) && (!(SystemKeyspace.bootstrapComplete()))) && (!(StorageService.isSeed()));
	}

	public static boolean isSeed() {
		return DatabaseDescriptor.getSeeds().contains(FBUtilities.getBroadcastAddress());
	}

	private void prepareToJoin() throws ConfigurationException {
		if (!(joined)) {
			Map<ApplicationState, VersionedValue> appStates = new EnumMap<>(ApplicationState.class);
			if (SystemKeyspace.wasDecommissioned()) {
				if (Boolean.getBoolean("cassandra.override_decommission")) {
					StorageService.logger.warn("This node was decommissioned, but overriding by operator request.");
					SystemKeyspace.setBootstrapState(COMPLETED);
				}else
					throw new ConfigurationException("This node was decommissioned and will not rejoin the ring unless cassandra.override_decommission=true has been set, or all existing data is removed and the node is bootstrapped again");

			}
			if (((DatabaseDescriptor.getReplaceTokens().size()) > 0) || ((DatabaseDescriptor.getReplaceNode()) != null))
				throw new RuntimeException("Replace method removed; use cassandra.replace_address instead");

			if (!(MessagingService.instance().isListening()))
				MessagingService.instance().listen();

			UUID localHostId = SystemKeyspace.getLocalHostId();
			if (replacing) {
				localHostId = prepareForReplacement();
				appStates.put(ApplicationState.TOKENS, valueFactory.tokens(bootstrapTokens));
				if (!(DatabaseDescriptor.isAutoBootstrap())) {
					SystemKeyspace.updateTokens(bootstrapTokens);
				}else
					if (StorageService.isReplacingSameAddress()) {
						StorageService.logger.warn(("Writes will not be forwarded to this node during replacement because it has the same address as " + ("the node to be replaced ({}). If the previous node has been down for longer than max_hint_window_in_ms, " + "repair must be run after the replacement process in order to make this node consistent.")), DatabaseDescriptor.getReplaceAddress());
						appStates.put(ApplicationState.STATUS, valueFactory.hibernate(true));
					}

			}else {
				checkForEndpointCollision(localHostId);
			}
			getTokenMetadata().updateHostId(localHostId, FBUtilities.getBroadcastAddress());
			appStates.put(ApplicationState.NET_VERSION, valueFactory.networkVersion());
			appStates.put(ApplicationState.HOST_ID, valueFactory.hostId(localHostId));
			appStates.put(ApplicationState.RPC_ADDRESS, valueFactory.rpcaddress(FBUtilities.getBroadcastRpcAddress()));
			appStates.put(ApplicationState.RELEASE_VERSION, valueFactory.releaseVersion());
			loadRingState();
			StorageService.logger.info("Starting up server gossip");
			Gossiper.instance.register(this);
			Gossiper.instance.start(SystemKeyspace.incrementAndGetGeneration(), appStates);
			gossipActive = true;
			gossipSnitchInfo();
			Schema.instance.updateVersionAndAnnounce();
			LoadBroadcaster.instance.startBroadcasting();
			HintsService.instance.startDispatch();
			BatchlogManager.instance.start();
		}
	}

	public void waitForSchema(int delay) {
		for (int i = 0; i < delay; i += 1000) {
			if (!(Schema.instance.getVersion().equals(SchemaConstants.emptyVersion))) {
				StorageService.logger.debug("got schema: {}", Schema.instance.getVersion());
				break;
			}
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
		}
		if (!(MigrationManager.isReadyForBootstrap())) {
			setMode(StorageService.Mode.JOINING, "waiting for schema information to complete", true);
			MigrationManager.waitUntilReadyForBootstrap();
		}
	}

	private void joinTokenRing(int delay) throws ConfigurationException {
		joined = true;
		Set<InetAddress> current = new HashSet<>();
		if (StorageService.logger.isDebugEnabled()) {
			StorageService.logger.debug("Bootstrap variables: {} {} {} {}", DatabaseDescriptor.isAutoBootstrap(), SystemKeyspace.bootstrapInProgress(), SystemKeyspace.bootstrapComplete(), DatabaseDescriptor.getSeeds().contains(FBUtilities.getBroadcastAddress()));
		}
		if (((DatabaseDescriptor.isAutoBootstrap()) && (!(SystemKeyspace.bootstrapComplete()))) && (DatabaseDescriptor.getSeeds().contains(FBUtilities.getBroadcastAddress()))) {
			StorageService.logger.info("This node will not auto bootstrap because it is configured to be a seed node.");
		}
		boolean dataAvailable = true;
		boolean bootstrap = shouldBootstrap();
		if (bootstrap) {
			if (SystemKeyspace.bootstrapInProgress())
				StorageService.logger.warn("Detected previous bootstrap failure; retrying");
			else
				SystemKeyspace.setBootstrapState(IN_PROGRESS);

			setMode(StorageService.Mode.JOINING, "waiting for ring information", true);
			waitForSchema(delay);
			setMode(StorageService.Mode.JOINING, "schema complete, ready to bootstrap", true);
			setMode(StorageService.Mode.JOINING, "waiting for pending range calculation", true);
			PendingRangeCalculatorService.instance.blockUntilFinished();
			setMode(StorageService.Mode.JOINING, "calculation complete, ready to bootstrap", true);
			StorageService.logger.debug("... got ring + schema info");
			if (((StorageService.useStrictConsistency) && (!(allowSimultaneousMoves()))) && ((((tokenMetadata.getBootstrapTokens().valueSet().size()) > 0) || ((tokenMetadata.getLeavingEndpoints().size()) > 0)) || ((tokenMetadata.getMovingEndpoints().size()) > 0))) {
				String bootstrapTokens = StringUtils.join(tokenMetadata.getBootstrapTokens().valueSet(), ',');
				String leavingTokens = StringUtils.join(tokenMetadata.getLeavingEndpoints(), ',');
				String movingTokens = StringUtils.join(tokenMetadata.getMovingEndpoints().stream().map(( e) -> e.right).toArray(), ',');
				throw new UnsupportedOperationException(String.format("Other bootstrapping/leaving/moving nodes detected, cannot bootstrap while cassandra.consistent.rangemovement is true. Nodes detected, bootstrapping: %s; leaving: %s; moving: %s;", bootstrapTokens, leavingTokens, movingTokens));
			}
			if (!(replacing)) {
				if (tokenMetadata.isMember(FBUtilities.getBroadcastAddress())) {
					String s = "This node is already a member of the token ring; bootstrap aborted. (If replacing a dead node, remove the old one from the ring first.)";
					throw new UnsupportedOperationException(s);
				}
				setMode(StorageService.Mode.JOINING, "getting bootstrap token", true);
				bootstrapTokens = BootStrapper.getBootstrapTokens(tokenMetadata, FBUtilities.getBroadcastAddress(), delay);
			}else {
				if (!(StorageService.isReplacingSameAddress())) {
					for (Token token : bootstrapTokens) {
						InetAddress existing = tokenMetadata.getEndpoint(token);
						if (existing != null) {
							long nanoDelay = delay * 1000000L;
							if ((Gossiper.instance.getEndpointStateForEndpoint(existing).getUpdateTimestamp()) > ((System.nanoTime()) - nanoDelay))
								throw new UnsupportedOperationException("Cannot replace a live node... ");

							current.add(existing);
						}else {
							throw new UnsupportedOperationException((("Cannot replace token " + token) + " which does not exist!"));
						}
					}
				}else {
					try {
						Thread.sleep(StorageService.RING_DELAY);
					} catch (InterruptedException e) {
						throw new AssertionError(e);
					}
				}
				setMode(StorageService.Mode.JOINING, ("Replacing a node with token(s): " + (bootstrapTokens)), true);
			}
			dataAvailable = bootstrap(bootstrapTokens);
		}else {
			bootstrapTokens = SystemKeyspace.getSavedTokens();
			if (bootstrapTokens.isEmpty()) {
				bootstrapTokens = BootStrapper.getBootstrapTokens(tokenMetadata, FBUtilities.getBroadcastAddress(), delay);
			}else {
				if ((bootstrapTokens.size()) != (DatabaseDescriptor.getNumTokens()))
					throw new ConfigurationException(((("Cannot change the number of tokens from " + (bootstrapTokens.size())) + " to ") + (DatabaseDescriptor.getNumTokens())));
				else
					StorageService.logger.info("Using saved tokens {}", bootstrapTokens);

			}
		}
		maybeAddOrUpdateKeyspace(TraceKeyspace.metadata());
		maybeAddOrUpdateKeyspace(SystemDistributedKeyspace.metadata());
		if (!(isSurveyMode)) {
			if (dataAvailable) {
				finishJoiningRing(bootstrap, bootstrapTokens);
				if (!(current.isEmpty())) {
					for (InetAddress existing : current)
						Gossiper.instance.replacedEndpoint(existing);

				}
			}else {
				StorageService.logger.warn("Some data streaming failed. Use nodetool to check bootstrap state and resume. For more, see `nodetool help bootstrap`. {}", SystemKeyspace.getBootstrapState());
			}
		}else {
			StorageService.logger.info("Startup complete, but write survey mode is active, not becoming an active ring member. Use JMX (StorageService->joinRing()) to finalize ring joining.");
		}
	}

	public static boolean isReplacingSameAddress() {
		InetAddress replaceAddress = DatabaseDescriptor.getReplaceAddress();
		return (replaceAddress != null) && (replaceAddress.equals(FBUtilities.getBroadcastAddress()));
	}

	public void gossipSnitchInfo() {
		IEndpointSnitch snitch = DatabaseDescriptor.getEndpointSnitch();
		String dc = snitch.getDatacenter(FBUtilities.getBroadcastAddress());
		String rack = snitch.getRack(FBUtilities.getBroadcastAddress());
		Gossiper.instance.addLocalApplicationState(ApplicationState.DC, StorageService.instance.valueFactory.datacenter(dc));
		Gossiper.instance.addLocalApplicationState(ApplicationState.RACK, StorageService.instance.valueFactory.rack(rack));
	}

	public void joinRing() throws IOException {
		SystemKeyspace.BootstrapState state = SystemKeyspace.getBootstrapState();
		joinRing(state.equals(IN_PROGRESS));
	}

	private synchronized void joinRing(boolean resumedBootstrap) throws IOException {
		if (!(joined)) {
			StorageService.logger.info("Joining ring by operator request");
			try {
				joinTokenRing(0);
			} catch (ConfigurationException e) {
				throw new IOException(e.getMessage());
			}
		}else
			if (isSurveyMode) {
				StorageService.logger.info("Leaving write survey mode and joining ring at operator request");
				finishJoiningRing(resumedBootstrap, SystemKeyspace.getSavedTokens());
				isSurveyMode = false;
			}

	}

	private void executePreJoinTasks(boolean bootstrap) {
		StreamSupport.stream(ColumnFamilyStore.all().spliterator(), false).filter(( cfs) -> Schema.instance.getUserKeyspaces().contains(cfs.keyspace.getName())).forEach(( cfs) -> cfs.indexManager.executePreJoinTasksBlocking(bootstrap));
	}

	private void finishJoiningRing(boolean didBootstrap, Collection<Token> tokens) {
		setMode(StorageService.Mode.JOINING, "Finish joining ring", true);
		SystemKeyspace.setBootstrapState(COMPLETED);
		executePreJoinTasks(didBootstrap);
		setTokens(tokens);
		assert (tokenMetadata.sortedTokens().size()) > 0;
		doAuthSetup();
	}

	private void doAuthSetup() {
		if (!(authSetupCalled.getAndSet(true))) {
			maybeAddOrUpdateKeyspace(AuthKeyspace.metadata());
			DatabaseDescriptor.getRoleManager().setup();
			DatabaseDescriptor.getAuthenticator().setup();
			DatabaseDescriptor.getAuthorizer().setup();
			MigrationManager.instance.register(new AuthMigrationListener());
			authSetupComplete = true;
		}
	}

	public boolean isAuthSetupComplete() {
		return authSetupComplete;
	}

	private void maybeAddKeyspace(KeyspaceMetadata ksm) {
		try {
			MigrationManager.announceNewKeyspace(ksm, 0, false);
		} catch (AlreadyExistsException e) {
			StorageService.logger.debug("Attempted to create new keyspace {}, but it already exists", ksm.name);
		}
	}

	private void maybeAddOrUpdateKeyspace(KeyspaceMetadata expected) {
		KeyspaceMetadata defined = Schema.instance.getKSMetaData(expected.name);
		if (defined == null) {
			maybeAddKeyspace(expected);
			defined = Schema.instance.getKSMetaData(expected.name);
		}
		for (CFMetaData expectedTable : expected.tables) {
			CFMetaData definedTable = defined.tables.get(expectedTable.cfName).orElse(null);
			if ((definedTable == null) || (!(definedTable.equals(expectedTable))))
				MigrationManager.forceAnnounceNewColumnFamily(expectedTable);

		}
	}

	public boolean isJoined() {
		return (tokenMetadata.isMember(FBUtilities.getBroadcastAddress())) && (!(isSurveyMode));
	}

	public void rebuild(String sourceDc) {
		rebuild(sourceDc, null, null, null);
	}

	public void rebuild(String sourceDc, String keyspace, String tokens, String specificSources) {
		if (!(isRebuilding.compareAndSet(false, true))) {
			throw new IllegalStateException("Node is still rebuilding. Check nodetool netstats.");
		}
		if ((keyspace == null) && (tokens != null)) {
			throw new IllegalArgumentException("Cannot specify tokens without keyspace.");
		}
		StorageService.logger.info("rebuild from dc: {}, {}, {}", (sourceDc == null ? "(any dc)" : sourceDc), (keyspace == null ? "(All keyspaces)" : keyspace), (tokens == null ? "(All tokens)" : tokens));
		try {
			RangeStreamer streamer = new RangeStreamer(tokenMetadata, null, FBUtilities.getBroadcastAddress(), "Rebuild", ((StorageService.useStrictConsistency) && (!(replacing))), DatabaseDescriptor.getEndpointSnitch(), streamStateStore, false);
			streamer.addSourceFilter(new RangeStreamer.FailureDetectorSourceFilter(FailureDetector.instance));
			if (sourceDc != null)
				streamer.addSourceFilter(new RangeStreamer.SingleDatacenterFilter(DatabaseDescriptor.getEndpointSnitch(), sourceDc));

			if (keyspace == null) {
				for (String keyspaceName : Schema.instance.getNonLocalStrategyKeyspaces())
					streamer.addRanges(keyspaceName, getLocalRanges(keyspaceName));

			}else
				if (tokens == null) {
					streamer.addRanges(keyspace, getLocalRanges(keyspace));
				}else {
					Token.TokenFactory factory = getTokenFactory();
					List<Range<Token>> ranges = new ArrayList<>();
					Pattern rangePattern = Pattern.compile("\\(\\s*(-?\\w+)\\s*,\\s*(-?\\w+)\\s*\\]");
					try (Scanner tokenScanner = new Scanner(tokens)) {
						while ((tokenScanner.findInLine(rangePattern)) != null) {
							MatchResult range = tokenScanner.match();
							Token startToken = factory.fromString(range.group(1));
							Token endToken = factory.fromString(range.group(2));
							StorageService.logger.info("adding range: ({},{}]", startToken, endToken);
							ranges.add(new Range<>(startToken, endToken));
						} 
						if (tokenScanner.hasNext())
							throw new IllegalArgumentException(("Unexpected string: " + (tokenScanner.next())));

					}
					Collection<Range<Token>> localRanges = getLocalRanges(keyspace);
					for (Range<Token> specifiedRange : ranges) {
						boolean foundParentRange = false;
						for (Range<Token> localRange : localRanges) {
							if (localRange.contains(specifiedRange)) {
								foundParentRange = true;
								break;
							}
						}
						if (!foundParentRange) {
							throw new IllegalArgumentException(String.format("The specified range %s is not a range that is owned by this node. Please ensure that all token ranges specified to be rebuilt belong to this node.", specifiedRange.toString()));
						}
					}
					if (specificSources != null) {
						String[] stringHosts = specificSources.split(",");
						Set<InetAddress> sources = new HashSet<>(stringHosts.length);
						for (String stringHost : stringHosts) {
							try {
								InetAddress endpoint = InetAddress.getByName(stringHost);
								if (FBUtilities.getBroadcastAddress().equals(endpoint)) {
									throw new IllegalArgumentException("This host was specified as a source for rebuilding. Sources for a rebuild can only be other nodes in the cluster.");
								}
								sources.add(endpoint);
							} catch (UnknownHostException ex) {
								throw new IllegalArgumentException(("Unknown host specified " + stringHost), ex);
							}
						}
						streamer.addSourceFilter(new RangeStreamer.WhitelistedSourcesFilter(sources));
					}
					streamer.addRanges(keyspace, ranges);
				}

			StreamResultFuture resultFuture = streamer.fetchAsync();
			resultFuture.get();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted while waiting on rebuild streaming");
		} catch (ExecutionException e) {
			StorageService.logger.error("Error while rebuilding node", e.getCause());
			throw new RuntimeException(("Error while rebuilding node: " + (e.getCause().getMessage())));
		} finally {
			isRebuilding.set(false);
		}
	}

	public void setRpcTimeout(long value) {
		DatabaseDescriptor.setRpcTimeout(value);
		StorageService.logger.info("set rpc timeout to {} ms", value);
	}

	public long getRpcTimeout() {
		return DatabaseDescriptor.getRpcTimeout();
	}

	public void setReadRpcTimeout(long value) {
		DatabaseDescriptor.setReadRpcTimeout(value);
		StorageService.logger.info("set read rpc timeout to {} ms", value);
	}

	public long getReadRpcTimeout() {
		return DatabaseDescriptor.getReadRpcTimeout();
	}

	public void setRangeRpcTimeout(long value) {
		DatabaseDescriptor.setRangeRpcTimeout(value);
		StorageService.logger.info("set range rpc timeout to {} ms", value);
	}

	public long getRangeRpcTimeout() {
		return DatabaseDescriptor.getRangeRpcTimeout();
	}

	public void setWriteRpcTimeout(long value) {
		DatabaseDescriptor.setWriteRpcTimeout(value);
		StorageService.logger.info("set write rpc timeout to {} ms", value);
	}

	public long getWriteRpcTimeout() {
		return DatabaseDescriptor.getWriteRpcTimeout();
	}

	public void setCounterWriteRpcTimeout(long value) {
		DatabaseDescriptor.setCounterWriteRpcTimeout(value);
		StorageService.logger.info("set counter write rpc timeout to {} ms", value);
	}

	public long getCounterWriteRpcTimeout() {
		return DatabaseDescriptor.getCounterWriteRpcTimeout();
	}

	public void setCasContentionTimeout(long value) {
		DatabaseDescriptor.setCasContentionTimeout(value);
		StorageService.logger.info("set cas contention rpc timeout to {} ms", value);
	}

	public long getCasContentionTimeout() {
		return DatabaseDescriptor.getCasContentionTimeout();
	}

	public void setTruncateRpcTimeout(long value) {
		DatabaseDescriptor.setTruncateRpcTimeout(value);
		StorageService.logger.info("set truncate rpc timeout to {} ms", value);
	}

	public long getTruncateRpcTimeout() {
		return DatabaseDescriptor.getTruncateRpcTimeout();
	}

	public void setStreamingSocketTimeout(int value) {
		DatabaseDescriptor.setStreamingSocketTimeout(value);
		StorageService.logger.info("set streaming socket timeout to {} ms", value);
	}

	public int getStreamingSocketTimeout() {
		return DatabaseDescriptor.getStreamingSocketTimeout();
	}

	public void setStreamThroughputMbPerSec(int value) {
		DatabaseDescriptor.setStreamThroughputOutboundMegabitsPerSec(value);
		StorageService.logger.info("setstreamthroughput: throttle set to {}", value);
	}

	public int getStreamThroughputMbPerSec() {
		return DatabaseDescriptor.getStreamThroughputOutboundMegabitsPerSec();
	}

	public void setInterDCStreamThroughputMbPerSec(int value) {
		DatabaseDescriptor.setInterDCStreamThroughputOutboundMegabitsPerSec(value);
		StorageService.logger.info("setinterdcstreamthroughput: throttle set to {}", value);
	}

	public int getInterDCStreamThroughputMbPerSec() {
		return DatabaseDescriptor.getInterDCStreamThroughputOutboundMegabitsPerSec();
	}

	public int getCompactionThroughputMbPerSec() {
		return DatabaseDescriptor.getCompactionThroughputMbPerSec();
	}

	public void setCompactionThroughputMbPerSec(int value) {
		DatabaseDescriptor.setCompactionThroughputMbPerSec(value);
		CompactionManager.instance.setRate(value);
	}

	public int getConcurrentCompactors() {
		return DatabaseDescriptor.getConcurrentCompactors();
	}

	public void setConcurrentCompactors(int value) {
		if (value <= 0)
			throw new IllegalArgumentException("Number of concurrent compactors should be greater than 0.");

		DatabaseDescriptor.setConcurrentCompactors(value);
		CompactionManager.instance.setConcurrentCompactors(value);
	}

	public boolean isIncrementalBackupsEnabled() {
		return DatabaseDescriptor.isIncrementalBackupsEnabled();
	}

	public void setIncrementalBackupsEnabled(boolean value) {
		DatabaseDescriptor.setIncrementalBackupsEnabled(value);
	}

	private void setMode(StorageService.Mode m, boolean log) {
		setMode(m, null, log);
	}

	private void setMode(StorageService.Mode m, String msg, boolean log) {
		operationMode = m;
		String logMsg = (msg == null) ? m.toString() : String.format("%s: %s", m, msg);
		if (log)
			StorageService.logger.info(logMsg);
		else
			StorageService.logger.debug(logMsg);

	}

	private boolean bootstrap(final Collection<Token> tokens) {
		isBootstrapMode = true;
		SystemKeyspace.updateTokens(tokens);
		if ((!(replacing)) || (!(StorageService.isReplacingSameAddress()))) {
			List<Pair<ApplicationState, VersionedValue>> states = new ArrayList<>();
			states.add(Pair.create(ApplicationState.TOKENS, valueFactory.tokens(tokens)));
			states.add(Pair.create(ApplicationState.STATUS, (replacing ? valueFactory.bootReplacing(DatabaseDescriptor.getReplaceAddress()) : valueFactory.bootstrapping(tokens))));
			Gossiper.instance.addLocalApplicationStates(states);
			setMode(StorageService.Mode.JOINING, (("sleeping " + (StorageService.RING_DELAY)) + " ms for pending range setup"), true);
			Uninterruptibles.sleepUninterruptibly(StorageService.RING_DELAY, TimeUnit.MILLISECONDS);
		}else {
			tokenMetadata.updateNormalTokens(tokens, FBUtilities.getBroadcastAddress());
			SystemKeyspace.removeEndpoint(DatabaseDescriptor.getReplaceAddress());
		}
		if (!(Gossiper.instance.seenAnySeed()))
			throw new IllegalStateException("Unable to contact any seeds!");

		if (Boolean.getBoolean("cassandra.reset_bootstrap_progress")) {
			StorageService.logger.info("Resetting bootstrap progress to start fresh");
			SystemKeyspace.resetAvailableRanges();
		}
		invalidateDiskBoundaries();
		setMode(StorageService.Mode.JOINING, "Starting to bootstrap...", true);
		BootStrapper bootstrapper = new BootStrapper(FBUtilities.getBroadcastAddress(), tokens, tokenMetadata);
		bootstrapper.addProgressListener(progressSupport);
		ListenableFuture<StreamState> bootstrapStream = bootstrapper.bootstrap(streamStateStore, ((StorageService.useStrictConsistency) && (!(replacing))));
		Futures.addCallback(bootstrapStream, new FutureCallback<StreamState>() {
			@Override
			public void onSuccess(StreamState streamState) {
				bootstrapFinished();
				StorageService.logger.info("Bootstrap completed! for the tokens {}", tokens);
			}

			@Override
			public void onFailure(Throwable e) {
				StorageService.logger.warn("Error during bootstrap.", e);
			}
		});
		try {
			bootstrapStream.get();
			return true;
		} catch (Throwable e) {
			StorageService.logger.error("Error while waiting on bootstrap to complete. Bootstrap will have to be restarted.", e);
			return false;
		}
	}

	private void invalidateDiskBoundaries() {
		for (Keyspace keyspace : Keyspace.all()) {
			for (ColumnFamilyStore cfs : keyspace.getColumnFamilyStores()) {
				for (final ColumnFamilyStore store : cfs.concatWithIndexes()) {
					store.invalidateDiskBoundaries();
				}
			}
		}
	}

	private void markViewsAsBuilt() {
		for (String keyspace : Schema.instance.getUserKeyspaces()) {
			for (ViewDefinition view : Schema.instance.getKSMetaData(keyspace).views)
				SystemKeyspace.finishViewBuildStatus(view.ksName, view.viewName);

		}
	}

	private void bootstrapFinished() {
		markViewsAsBuilt();
		isBootstrapMode = false;
	}

	public boolean resumeBootstrap() {
		if ((isBootstrapMode) && (SystemKeyspace.bootstrapInProgress())) {
			StorageService.logger.info("Resuming bootstrap...");
			final Collection<Token> tokens = SystemKeyspace.getSavedTokens();
			BootStrapper bootstrapper = new BootStrapper(FBUtilities.getBroadcastAddress(), tokens, tokenMetadata);
			bootstrapper.addProgressListener(progressSupport);
			ListenableFuture<StreamState> bootstrapStream = bootstrapper.bootstrap(streamStateStore, ((StorageService.useStrictConsistency) && (!(replacing))));
			Futures.addCallback(bootstrapStream, new FutureCallback<StreamState>() {
				@Override
				public void onSuccess(StreamState streamState) {
					bootstrapFinished();
					isSurveyMode = true;
					try {
						progressSupport.progress("bootstrap", ProgressEvent.createNotification("Joining ring..."));
						joinRing(true);
					} catch (IOException ignore) {
					}
					progressSupport.progress("bootstrap", new ProgressEvent(ProgressEventType.COMPLETE, 1, 1, "Resume bootstrap complete"));
					StorageService.logger.info("Resume complete");
				}

				@Override
				public void onFailure(Throwable e) {
					String message = "Error during bootstrap: ";
					if ((e instanceof ExecutionException) && ((e.getCause()) != null)) {
						message += e.getCause().getMessage();
					}else {
						message += e.getMessage();
					}
					StorageService.logger.error(message, e);
					progressSupport.progress("bootstrap", new ProgressEvent(ProgressEventType.ERROR, 1, 1, message));
					progressSupport.progress("bootstrap", new ProgressEvent(ProgressEventType.COMPLETE, 1, 1, "Resume bootstrap complete"));
				}
			});
			return true;
		}else {
			StorageService.logger.info("Resuming bootstrap is requested, but the node is already bootstrapped.");
			return false;
		}
	}

	public boolean isBootstrapMode() {
		return isBootstrapMode;
	}

	public TokenMetadata getTokenMetadata() {
		return tokenMetadata;
	}

	public Map<List<String>, List<String>> getRangeToEndpointMap(String keyspace) {
		Map<List<String>, List<String>> map = new HashMap<>();
		for (Map.Entry<Range<Token>, List<InetAddress>> entry : getRangeToAddressMap(keyspace).entrySet()) {
			map.put(entry.getKey().asList(), stringify(entry.getValue()));
		}
		return map;
	}

	public String getRpcaddress(InetAddress endpoint) {
		if (endpoint.equals(FBUtilities.getBroadcastAddress()))
			return FBUtilities.getBroadcastRpcAddress().getHostAddress();
		else
			if ((Gossiper.instance.getEndpointStateForEndpoint(endpoint).getApplicationState(ApplicationState.RPC_ADDRESS)) == null)
				return endpoint.getHostAddress();
			else
				return Gossiper.instance.getEndpointStateForEndpoint(endpoint).getApplicationState(ApplicationState.RPC_ADDRESS).value;


	}

	public Map<List<String>, List<String>> getRangeToRpcaddressMap(String keyspace) {
		Map<List<String>, List<String>> map = new HashMap<>();
		for (Map.Entry<Range<Token>, List<InetAddress>> entry : getRangeToAddressMap(keyspace).entrySet()) {
			List<String> rpcaddrs = new ArrayList<>(entry.getValue().size());
			for (InetAddress endpoint : entry.getValue()) {
				rpcaddrs.add(getRpcaddress(endpoint));
			}
			map.put(entry.getKey().asList(), rpcaddrs);
		}
		return map;
	}

	public Map<List<String>, List<String>> getPendingRangeToEndpointMap(String keyspace) {
		if (keyspace == null)
			keyspace = Schema.instance.getNonLocalStrategyKeyspaces().get(0);

		Map<List<String>, List<String>> map = new HashMap<>();
		for (Map.Entry<Range<Token>, Collection<InetAddress>> entry : tokenMetadata.getPendingRangesMM(keyspace).asMap().entrySet()) {
			List<InetAddress> l = new ArrayList<>(entry.getValue());
			map.put(entry.getKey().asList(), stringify(l));
		}
		return map;
	}

	public Map<Range<Token>, List<InetAddress>> getRangeToAddressMap(String keyspace) {
		return getRangeToAddressMap(keyspace, tokenMetadata.sortedTokens());
	}

	public Map<Range<Token>, List<InetAddress>> getRangeToAddressMapInLocalDC(String keyspace) {
		com.google.common.base.Predicate<InetAddress> isLocalDC = new com.google.common.base.Predicate<InetAddress>() {
			public boolean apply(InetAddress address) {
				return isLocalDC(address);
			}
		};
		Map<Range<Token>, List<InetAddress>> origMap = getRangeToAddressMap(keyspace, getTokensInLocalDC());
		Map<Range<Token>, List<InetAddress>> filteredMap = Maps.newHashMap();
		for (Map.Entry<Range<Token>, List<InetAddress>> entry : origMap.entrySet()) {
			List<InetAddress> endpointsInLocalDC = Lists.newArrayList(Collections2.filter(entry.getValue(), isLocalDC));
			filteredMap.put(entry.getKey(), endpointsInLocalDC);
		}
		return filteredMap;
	}

	private List<Token> getTokensInLocalDC() {
		List<Token> filteredTokens = Lists.newArrayList();
		for (Token token : tokenMetadata.sortedTokens()) {
			InetAddress endpoint = tokenMetadata.getEndpoint(token);
			if (isLocalDC(endpoint))
				filteredTokens.add(token);

		}
		return filteredTokens;
	}

	private boolean isLocalDC(InetAddress targetHost) {
		String remoteDC = DatabaseDescriptor.getEndpointSnitch().getDatacenter(targetHost);
		String localDC = DatabaseDescriptor.getEndpointSnitch().getDatacenter(FBUtilities.getBroadcastAddress());
		return remoteDC.equals(localDC);
	}

	private Map<Range<Token>, List<InetAddress>> getRangeToAddressMap(String keyspace, List<Token> sortedTokens) {
		if (keyspace == null)
			keyspace = Schema.instance.getNonLocalStrategyKeyspaces().get(0);

		List<Range<Token>> ranges = getAllRanges(sortedTokens);
		return constructRangeToEndpointMap(keyspace, ranges);
	}

	public List<String> describeRingJMX(String keyspace) throws IOException {
		List<TokenRange> tokenRanges;
		try {
			tokenRanges = describeRing(keyspace);
		} catch (InvalidRequestException e) {
			throw new IOException(e.getMessage());
		}
		List<String> result = new ArrayList<>(tokenRanges.size());
		for (TokenRange tokenRange : tokenRanges)
			result.add(tokenRange.toString());

		return result;
	}

	public List<TokenRange> describeRing(String keyspace) throws InvalidRequestException {
		return describeRing(keyspace, false);
	}

	public List<TokenRange> describeLocalRing(String keyspace) throws InvalidRequestException {
		return describeRing(keyspace, true);
	}

	private List<TokenRange> describeRing(String keyspace, boolean includeOnlyLocalDC) throws InvalidRequestException {
		if (!(Schema.instance.getKeyspaces().contains(keyspace)))
			throw new InvalidRequestException(("No such keyspace: " + keyspace));

		if ((keyspace == null) || ((Keyspace.open(keyspace).getReplicationStrategy()) instanceof LocalStrategy))
			throw new InvalidRequestException(("There is no ring for the keyspace: " + keyspace));

		List<TokenRange> ranges = new ArrayList<>();
		Token.TokenFactory tf = getTokenFactory();
		Map<Range<Token>, List<InetAddress>> rangeToAddressMap = (includeOnlyLocalDC) ? getRangeToAddressMapInLocalDC(keyspace) : getRangeToAddressMap(keyspace);
		for (Map.Entry<Range<Token>, List<InetAddress>> entry : rangeToAddressMap.entrySet()) {
			Range<Token> range = entry.getKey();
			List<InetAddress> addresses = entry.getValue();
			List<String> endpoints = new ArrayList<>(addresses.size());
			List<String> rpc_endpoints = new ArrayList<>(addresses.size());
			List<EndpointDetails> epDetails = new ArrayList<>(addresses.size());
			for (InetAddress endpoint : addresses) {
				EndpointDetails details = new EndpointDetails();
				details.host = endpoint.getHostAddress();
				details.datacenter = DatabaseDescriptor.getEndpointSnitch().getDatacenter(endpoint);
				details.rack = DatabaseDescriptor.getEndpointSnitch().getRack(endpoint);
				endpoints.add(details.host);
				rpc_endpoints.add(getRpcaddress(endpoint));
				epDetails.add(details);
			}
			TokenRange tr = new TokenRange(tf.toString(range.left.getToken()), tf.toString(range.right.getToken()), endpoints).setEndpoint_details(epDetails).setRpc_endpoints(rpc_endpoints);
			ranges.add(tr);
		}
		return ranges;
	}

	public Map<String, String> getTokenToEndpointMap() {
		Map<Token, InetAddress> mapInetAddress = tokenMetadata.getNormalAndBootstrappingTokenToEndpointMap();
		Map<String, String> mapString = new LinkedHashMap<>(mapInetAddress.size());
		List<Token> tokens = new ArrayList<>(mapInetAddress.keySet());
		Collections.sort(tokens);
		for (Token token : tokens) {
			mapString.put(token.toString(), mapInetAddress.get(token).getHostAddress());
		}
		return mapString;
	}

	public String getLocalHostId() {
		return getTokenMetadata().getHostId(FBUtilities.getBroadcastAddress()).toString();
	}

	public UUID getLocalHostUUID() {
		return getTokenMetadata().getHostId(FBUtilities.getBroadcastAddress());
	}

	public Map<String, String> getHostIdMap() {
		return getEndpointToHostId();
	}

	public Map<String, String> getEndpointToHostId() {
		Map<String, String> mapOut = new HashMap<>();
		for (Map.Entry<InetAddress, UUID> entry : getTokenMetadata().getEndpointToHostIdMapForReading().entrySet())
			mapOut.put(entry.getKey().getHostAddress(), entry.getValue().toString());

		return mapOut;
	}

	public Map<String, String> getHostIdToEndpoint() {
		Map<String, String> mapOut = new HashMap<>();
		for (Map.Entry<InetAddress, UUID> entry : getTokenMetadata().getEndpointToHostIdMapForReading().entrySet())
			mapOut.put(entry.getValue().toString(), entry.getKey().getHostAddress());

		return mapOut;
	}

	private Map<Range<Token>, List<InetAddress>> constructRangeToEndpointMap(String keyspace, List<Range<Token>> ranges) {
		Map<Range<Token>, List<InetAddress>> rangeToEndpointMap = new HashMap<>(ranges.size());
		for (Range<Token> range : ranges) {
			rangeToEndpointMap.put(range, Keyspace.open(keyspace).getReplicationStrategy().getNaturalEndpoints(range.right));
		}
		return rangeToEndpointMap;
	}

	public void beforeChange(InetAddress endpoint, EndpointState currentState, ApplicationState newStateKey, VersionedValue newValue) {
	}

	public void onChange(InetAddress endpoint, ApplicationState state, VersionedValue value) {
		if (state == (ApplicationState.STATUS)) {
			String[] pieces = StorageService.splitValue(value);
			assert (pieces.length) > 0;
			String moveName = pieces[0];
			switch (moveName) {
				case VersionedValue.STATUS_BOOTSTRAPPING_REPLACE :
					handleStateBootreplacing(endpoint, pieces);
					break;
				case VersionedValue.STATUS_BOOTSTRAPPING :
					handleStateBootstrap(endpoint);
					break;
				case VersionedValue.STATUS_NORMAL :
					handleStateNormal(endpoint, VersionedValue.STATUS_NORMAL);
					break;
				case VersionedValue.SHUTDOWN :
					handleStateNormal(endpoint, VersionedValue.SHUTDOWN);
					break;
				case VersionedValue.REMOVING_TOKEN :
				case VersionedValue.REMOVED_TOKEN :
					handleStateRemoving(endpoint, pieces);
					break;
				case VersionedValue.STATUS_LEAVING :
					handleStateLeaving(endpoint);
					break;
				case VersionedValue.STATUS_LEFT :
					handleStateLeft(endpoint, pieces);
					break;
				case VersionedValue.STATUS_MOVING :
					handleStateMoving(endpoint, pieces);
					break;
			}
		}else {
			EndpointState epState = Gossiper.instance.getEndpointStateForEndpoint(endpoint);
			if ((epState == null) || (Gossiper.instance.isDeadState(epState))) {
				StorageService.logger.debug("Ignoring state change for dead or unknown endpoint: {}", endpoint);
				return;
			}
			if (getTokenMetadata().isMember(endpoint)) {
				final ExecutorService executor = StageManager.getStage(Stage.MUTATION);
				switch (state) {
					case RELEASE_VERSION :
						SystemKeyspace.updatePeerInfo(endpoint, "release_version", value.value, executor);
						break;
					case DC :
						updateTopology(endpoint);
						SystemKeyspace.updatePeerInfo(endpoint, "data_center", value.value, executor);
						break;
					case RACK :
						updateTopology(endpoint);
						SystemKeyspace.updatePeerInfo(endpoint, "rack", value.value, executor);
						break;
					case RPC_ADDRESS :
						try {
							SystemKeyspace.updatePeerInfo(endpoint, "rpc_address", InetAddress.getByName(value.value), executor);
						} catch (UnknownHostException e) {
							throw new RuntimeException(e);
						}
						break;
					case SCHEMA :
						SystemKeyspace.updatePeerInfo(endpoint, "schema_version", UUID.fromString(value.value), executor);
						MigrationManager.instance.scheduleSchemaPull(endpoint, epState);
						break;
					case HOST_ID :
						SystemKeyspace.updatePeerInfo(endpoint, "host_id", UUID.fromString(value.value), executor);
						break;
					case RPC_READY :
						notifyRpcChange(endpoint, epState.isRpcReady());
						break;
					case NET_VERSION :
						updateNetVersion(endpoint, value);
						break;
				}
			}
		}
	}

	private static String[] splitValue(VersionedValue value) {
		return value.value.split(VersionedValue.DELIMITER_STR, (-1));
	}

	private void updateNetVersion(InetAddress endpoint, VersionedValue value) {
		try {
			MessagingService.instance().setVersion(endpoint, Integer.parseInt(value.value));
		} catch (NumberFormatException e) {
			throw new AssertionError(("Got invalid value for NET_VERSION application state: " + (value.value)));
		}
	}

	public void updateTopology(InetAddress endpoint) {
		if (getTokenMetadata().isMember(endpoint)) {
			getTokenMetadata().updateTopology(endpoint);
		}
	}

	public void updateTopology() {
		getTokenMetadata().updateTopology();
	}

	private void updatePeerInfo(InetAddress endpoint) {
		EndpointState epState = Gossiper.instance.getEndpointStateForEndpoint(endpoint);
		final ExecutorService executor = StageManager.getStage(Stage.MUTATION);
		for (Map.Entry<ApplicationState, VersionedValue> entry : epState.states()) {
			switch (entry.getKey()) {
				case RELEASE_VERSION :
					SystemKeyspace.updatePeerInfo(endpoint, "release_version", entry.getValue().value, executor);
					break;
				case DC :
					SystemKeyspace.updatePeerInfo(endpoint, "data_center", entry.getValue().value, executor);
					break;
				case RACK :
					SystemKeyspace.updatePeerInfo(endpoint, "rack", entry.getValue().value, executor);
					break;
				case RPC_ADDRESS :
					try {
						SystemKeyspace.updatePeerInfo(endpoint, "rpc_address", InetAddress.getByName(entry.getValue().value), executor);
					} catch (UnknownHostException e) {
						throw new RuntimeException(e);
					}
					break;
				case SCHEMA :
					SystemKeyspace.updatePeerInfo(endpoint, "schema_version", UUID.fromString(entry.getValue().value), executor);
					break;
				case HOST_ID :
					SystemKeyspace.updatePeerInfo(endpoint, "host_id", UUID.fromString(entry.getValue().value), executor);
					break;
			}
		}
	}

	private void notifyRpcChange(InetAddress endpoint, boolean ready) {
		if (ready)
			notifyUp(endpoint);
		else
			notifyDown(endpoint);

	}

	private void notifyUp(InetAddress endpoint) {
		if ((!(isRpcReady(endpoint))) || (!(Gossiper.instance.isAlive(endpoint))))
			return;

		for (IEndpointLifecycleSubscriber subscriber : lifecycleSubscribers)
			subscriber.onUp(endpoint);

	}

	private void notifyDown(InetAddress endpoint) {
		for (IEndpointLifecycleSubscriber subscriber : lifecycleSubscribers)
			subscriber.onDown(endpoint);

	}

	private void notifyJoined(InetAddress endpoint) {
		if (!(isStatus(endpoint, VersionedValue.STATUS_NORMAL)))
			return;

		for (IEndpointLifecycleSubscriber subscriber : lifecycleSubscribers)
			subscriber.onJoinCluster(endpoint);

	}

	private void notifyMoved(InetAddress endpoint) {
		for (IEndpointLifecycleSubscriber subscriber : lifecycleSubscribers)
			subscriber.onMove(endpoint);

	}

	private void notifyLeft(InetAddress endpoint) {
		for (IEndpointLifecycleSubscriber subscriber : lifecycleSubscribers)
			subscriber.onLeaveCluster(endpoint);

	}

	private boolean isStatus(InetAddress endpoint, String status) {
		EndpointState state = Gossiper.instance.getEndpointStateForEndpoint(endpoint);
		return (state != null) && (state.getStatus().equals(status));
	}

	public boolean isRpcReady(InetAddress endpoint) {
		if ((MessagingService.instance().getVersion(endpoint)) < (MessagingService.VERSION_22))
			return true;

		EndpointState state = Gossiper.instance.getEndpointStateForEndpoint(endpoint);
		return (state != null) && (state.isRpcReady());
	}

	public void setRpcReady(boolean value) {
		EndpointState state = Gossiper.instance.getEndpointStateForEndpoint(FBUtilities.getBroadcastAddress());
		assert (!value) || (state != null);
		if (state != null)
			Gossiper.instance.addLocalApplicationState(ApplicationState.RPC_READY, valueFactory.rpcReady(value));

	}

	private Collection<Token> getTokensFor(InetAddress endpoint) {
		try {
			EndpointState state = Gossiper.instance.getEndpointStateForEndpoint(endpoint);
			if (state == null)
				return Collections.emptyList();

			VersionedValue versionedValue = state.getApplicationState(ApplicationState.TOKENS);
			if (versionedValue == null)
				return Collections.emptyList();

			return TokenSerializer.deserialize(tokenMetadata.partitioner, new DataInputStream(new ByteArrayInputStream(versionedValue.toBytes())));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void handleStateBootstrap(InetAddress endpoint) {
		Collection<Token> tokens;
		tokens = getTokensFor(endpoint);
		if (StorageService.logger.isDebugEnabled())
			StorageService.logger.debug("Node {} state bootstrapping, token {}", endpoint, tokens);

		if (tokenMetadata.isMember(endpoint)) {
			if (!(tokenMetadata.isLeaving(endpoint)))
				StorageService.logger.info("Node {} state jump to bootstrap", endpoint);

			tokenMetadata.removeEndpoint(endpoint);
		}
		tokenMetadata.addBootstrapTokens(tokens, endpoint);
		PendingRangeCalculatorService.instance.update();
		tokenMetadata.updateHostId(Gossiper.instance.getHostId(endpoint), endpoint);
	}

	private void handleStateBootreplacing(InetAddress newNode, String[] pieces) {
		InetAddress oldNode;
		try {
			oldNode = InetAddress.getByName(pieces[1]);
		} catch (Exception e) {
			StorageService.logger.error("Node {} tried to replace malformed endpoint {}.", newNode, pieces[1], e);
			return;
		}
		if (FailureDetector.instance.isAlive(oldNode)) {
			throw new RuntimeException(String.format("Node %s is trying to replace alive node %s.", newNode, oldNode));
		}
		Optional<InetAddress> replacingNode = tokenMetadata.getReplacingNode(newNode);
		if ((replacingNode.isPresent()) && (!(replacingNode.get().equals(oldNode)))) {
			throw new RuntimeException(String.format("Node %s is already replacing %s but is trying to replace %s.", newNode, replacingNode.get(), oldNode));
		}
		Collection<Token> tokens = getTokensFor(newNode);
		if (StorageService.logger.isDebugEnabled())
			StorageService.logger.debug("Node {} is replacing {}, tokens {}", newNode, oldNode, tokens);

		tokenMetadata.addReplaceTokens(tokens, newNode, oldNode);
		PendingRangeCalculatorService.instance.update();
		tokenMetadata.updateHostId(Gossiper.instance.getHostId(newNode), newNode);
	}

	private void handleStateNormal(final InetAddress endpoint, final String status) {
		Collection<Token> tokens = getTokensFor(endpoint);
		Set<Token> tokensToUpdateInMetadata = new HashSet<>();
		Set<Token> tokensToUpdateInSystemKeyspace = new HashSet<>();
		Set<InetAddress> endpointsToRemove = new HashSet<>();
		if (StorageService.logger.isDebugEnabled())
			StorageService.logger.debug("Node {} state {}, token {}", endpoint, status, tokens);

		if (tokenMetadata.isMember(endpoint))
			StorageService.logger.info("Node {} state jump to {}", endpoint, status);

		if ((tokens.isEmpty()) && (status.equals(VersionedValue.STATUS_NORMAL)))
			StorageService.logger.error("Node {} is in state normal but it has no tokens, state: {}", endpoint, Gossiper.instance.getEndpointStateForEndpoint(endpoint));

		Optional<InetAddress> replacingNode = tokenMetadata.getReplacingNode(endpoint);
		if (replacingNode.isPresent()) {
			assert !(endpoint.equals(replacingNode.get())) : "Pending replacement endpoint with same address is not supported";
			StorageService.logger.info("Node {} will complete replacement of {} for tokens {}", endpoint, replacingNode.get(), tokens);
			if (FailureDetector.instance.isAlive(replacingNode.get())) {
				StorageService.logger.error("Node {} cannot complete replacement of alive node {}.", endpoint, replacingNode.get());
				return;
			}
			endpointsToRemove.add(replacingNode.get());
		}
		Optional<InetAddress> replacementNode = tokenMetadata.getReplacementNode(endpoint);
		if (replacementNode.isPresent()) {
			StorageService.logger.warn("Node {} is currently being replaced by node {}.", endpoint, replacementNode.get());
		}
		updatePeerInfo(endpoint);
		UUID hostId = Gossiper.instance.getHostId(endpoint);
		InetAddress existing = tokenMetadata.getEndpointForHostId(hostId);
		if ((((replacing) && (StorageService.isReplacingSameAddress())) && ((Gossiper.instance.getEndpointStateForEndpoint(DatabaseDescriptor.getReplaceAddress())) != null)) && (hostId.equals(Gossiper.instance.getHostId(DatabaseDescriptor.getReplaceAddress()))))
			StorageService.logger.warn("Not updating token metadata for {} because I am replacing it", endpoint);
		else {
			if ((existing != null) && (!(existing.equals(endpoint)))) {
				if (existing.equals(FBUtilities.getBroadcastAddress())) {
					StorageService.logger.warn("Not updating host ID {} for {} because it's mine", hostId, endpoint);
					tokenMetadata.removeEndpoint(endpoint);
					endpointsToRemove.add(endpoint);
				}else
					if ((Gossiper.instance.compareEndpointStartup(endpoint, existing)) > 0) {
						StorageService.logger.warn("Host ID collision for {} between {} and {}; {} is the new owner", hostId, existing, endpoint, endpoint);
						tokenMetadata.removeEndpoint(existing);
						endpointsToRemove.add(existing);
						tokenMetadata.updateHostId(hostId, endpoint);
					}else {
						StorageService.logger.warn("Host ID collision for {} between {} and {}; ignored {}", hostId, existing, endpoint, endpoint);
						tokenMetadata.removeEndpoint(endpoint);
						endpointsToRemove.add(endpoint);
					}

			}else
				tokenMetadata.updateHostId(hostId, endpoint);

		}
		for (final Token token : tokens) {
			InetAddress currentOwner = tokenMetadata.getEndpoint(token);
			if (currentOwner == null) {
				StorageService.logger.debug("New node {} at token {}", endpoint, token);
				tokensToUpdateInMetadata.add(token);
				tokensToUpdateInSystemKeyspace.add(token);
			}else
				if (endpoint.equals(currentOwner)) {
					tokensToUpdateInMetadata.add(token);
					tokensToUpdateInSystemKeyspace.add(token);
				}else
					if ((Gossiper.instance.compareEndpointStartup(endpoint, currentOwner)) > 0) {
						tokensToUpdateInMetadata.add(token);
						tokensToUpdateInSystemKeyspace.add(token);
						Multimap<InetAddress, Token> epToTokenCopy = getTokenMetadata().getEndpointToTokenMapForReading();
						epToTokenCopy.get(currentOwner).remove(token);
						if ((epToTokenCopy.get(currentOwner).size()) < 1)
							endpointsToRemove.add(currentOwner);

						StorageService.logger.info("Nodes {} and {} have the same token {}.  {} is the new owner", endpoint, currentOwner, token, endpoint);
					}else {
						StorageService.logger.info("Nodes {} and {} have the same token {}.  Ignoring {}", endpoint, currentOwner, token, endpoint);
					}


		}
		boolean isMember = tokenMetadata.isMember(endpoint);
		boolean isMoving = tokenMetadata.isMoving(endpoint);
		tokenMetadata.updateNormalTokens(tokensToUpdateInMetadata, endpoint);
		for (InetAddress ep : endpointsToRemove) {
			removeEndpoint(ep);
			if ((replacing) && (DatabaseDescriptor.getReplaceAddress().equals(ep)))
				Gossiper.instance.replacementQuarantine(ep);

		}
		if (!(tokensToUpdateInSystemKeyspace.isEmpty()))
			SystemKeyspace.updateTokens(endpoint, tokensToUpdateInSystemKeyspace, StageManager.getStage(Stage.MUTATION));

		if (isMoving || ((operationMode) == (StorageService.Mode.MOVING))) {
			tokenMetadata.removeFromMoving(endpoint);
			notifyMoved(endpoint);
		}else
			if (!isMember) {
				notifyJoined(endpoint);
			}

		PendingRangeCalculatorService.instance.update();
	}

	private void handleStateLeaving(InetAddress endpoint) {
		Collection<Token> tokens = getTokensFor(endpoint);
		if (StorageService.logger.isDebugEnabled())
			StorageService.logger.debug("Node {} state leaving, tokens {}", endpoint, tokens);

		if (!(tokenMetadata.isMember(endpoint))) {
			StorageService.logger.info("Node {} state jump to leaving", endpoint);
			tokenMetadata.updateNormalTokens(tokens, endpoint);
		}else
			if (!(tokenMetadata.getTokens(endpoint).containsAll(tokens))) {
				StorageService.logger.warn("Node {} 'leaving' token mismatch. Long network partition?", endpoint);
				tokenMetadata.updateNormalTokens(tokens, endpoint);
			}

		tokenMetadata.addLeavingEndpoint(endpoint);
		PendingRangeCalculatorService.instance.update();
	}

	private void handleStateLeft(InetAddress endpoint, String[] pieces) {
		assert (pieces.length) >= 2;
		Collection<Token> tokens = getTokensFor(endpoint);
		if (StorageService.logger.isDebugEnabled())
			StorageService.logger.debug("Node {} state left, tokens {}", endpoint, tokens);

		excise(tokens, endpoint, extractExpireTime(pieces));
	}

	private void handleStateMoving(InetAddress endpoint, String[] pieces) {
		assert (pieces.length) >= 2;
		Token token = getTokenFactory().fromString(pieces[1]);
		if (StorageService.logger.isDebugEnabled())
			StorageService.logger.debug("Node {} state moving, new token {}", endpoint, token);

		tokenMetadata.addMovingEndpoint(token, endpoint);
		PendingRangeCalculatorService.instance.update();
	}

	private void handleStateRemoving(InetAddress endpoint, String[] pieces) {
		assert (pieces.length) > 0;
		if (endpoint.equals(FBUtilities.getBroadcastAddress())) {
			StorageService.logger.info("Received removenode gossip about myself. Is this node rejoining after an explicit removenode?");
			try {
				drain();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return;
		}
		if (tokenMetadata.isMember(endpoint)) {
			String state = pieces[0];
			Collection<Token> removeTokens = tokenMetadata.getTokens(endpoint);
			if (VersionedValue.REMOVED_TOKEN.equals(state)) {
				excise(removeTokens, endpoint, extractExpireTime(pieces));
			}else
				if (VersionedValue.REMOVING_TOKEN.equals(state)) {
					if (StorageService.logger.isDebugEnabled())
						StorageService.logger.debug("Tokens {} removed manually (endpoint was {})", removeTokens, endpoint);

					tokenMetadata.addLeavingEndpoint(endpoint);
					PendingRangeCalculatorService.instance.update();
					String[] coordinator = StorageService.splitValue(Gossiper.instance.getEndpointStateForEndpoint(endpoint).getApplicationState(ApplicationState.REMOVAL_COORDINATOR));
					UUID hostId = UUID.fromString(coordinator[1]);
					restoreReplicaCount(endpoint, tokenMetadata.getEndpointForHostId(hostId));
				}

		}else {
			if (VersionedValue.REMOVED_TOKEN.equals(pieces[0]))
				addExpireTimeIfFound(endpoint, extractExpireTime(pieces));

			removeEndpoint(endpoint);
		}
	}

	private void excise(Collection<Token> tokens, InetAddress endpoint) {
		StorageService.logger.info("Removing tokens {} for {}", tokens, endpoint);
		UUID hostId = tokenMetadata.getHostId(endpoint);
		if ((hostId != null) && (tokenMetadata.isMember(endpoint)))
			HintsService.instance.excise(hostId);

		removeEndpoint(endpoint);
		tokenMetadata.removeEndpoint(endpoint);
		if (!(tokens.isEmpty()))
			tokenMetadata.removeBootstrapTokens(tokens);

		notifyLeft(endpoint);
		PendingRangeCalculatorService.instance.update();
	}

	private void excise(Collection<Token> tokens, InetAddress endpoint, long expireTime) {
		addExpireTimeIfFound(endpoint, expireTime);
		excise(tokens, endpoint);
	}

	private void removeEndpoint(InetAddress endpoint) {
		Gossiper.instance.removeEndpoint(endpoint);
		SystemKeyspace.removeEndpoint(endpoint);
	}

	protected void addExpireTimeIfFound(InetAddress endpoint, long expireTime) {
		if (expireTime != 0L) {
			Gossiper.instance.addExpireTimeForEndpoint(endpoint, expireTime);
		}
	}

	protected long extractExpireTime(String[] pieces) {
		return Long.parseLong(pieces[2]);
	}

	private Multimap<InetAddress, Range<Token>> getNewSourceRanges(String keyspaceName, Set<Range<Token>> ranges) {
		InetAddress myAddress = FBUtilities.getBroadcastAddress();
		Multimap<Range<Token>, InetAddress> rangeAddresses = Keyspace.open(keyspaceName).getReplicationStrategy().getRangeAddresses(tokenMetadata.cloneOnlyTokenMap());
		Multimap<InetAddress, Range<Token>> sourceRanges = HashMultimap.create();
		IFailureDetector failureDetector = FailureDetector.instance;
		for (Range<Token> range : ranges) {
			Collection<InetAddress> possibleRanges = rangeAddresses.get(range);
			IEndpointSnitch snitch = DatabaseDescriptor.getEndpointSnitch();
			List<InetAddress> sources = snitch.getSortedListByProximity(myAddress, possibleRanges);
			assert !(sources.contains(myAddress));
			for (InetAddress source : sources) {
				if (failureDetector.isAlive(source)) {
					sourceRanges.put(source, range);
					break;
				}
			}
		}
		return sourceRanges;
	}

	private void sendReplicationNotification(InetAddress remote) {
		MessageOut msg = new MessageOut(REPLICATION_FINISHED);
		IFailureDetector failureDetector = FailureDetector.instance;
		if (StorageService.logger.isDebugEnabled())
			StorageService.logger.debug("Notifying {} of replication completion\n", remote);

		while (failureDetector.isAlive(remote)) {
			AsyncOneResponse iar = MessagingService.instance().sendRR(msg, remote);
			try {
				iar.get(DatabaseDescriptor.getRpcTimeout(), TimeUnit.MILLISECONDS);
				return;
			} catch (TimeoutException e) {
			}
		} 
	}

	private void restoreReplicaCount(InetAddress endpoint, final InetAddress notifyEndpoint) {
		Multimap<String, Map.Entry<InetAddress, Collection<Range<Token>>>> rangesToFetch = HashMultimap.create();
		InetAddress myAddress = FBUtilities.getBroadcastAddress();
		for (String keyspaceName : Schema.instance.getNonLocalStrategyKeyspaces()) {
			Multimap<Range<Token>, InetAddress> changedRanges = getChangedRangesForLeaving(keyspaceName, endpoint);
			Set<Range<Token>> myNewRanges = new HashSet<>();
			for (Map.Entry<Range<Token>, InetAddress> entry : changedRanges.entries()) {
				if (entry.getValue().equals(myAddress))
					myNewRanges.add(entry.getKey());

			}
			Multimap<InetAddress, Range<Token>> sourceRanges = getNewSourceRanges(keyspaceName, myNewRanges);
			for (Map.Entry<InetAddress, Collection<Range<Token>>> entry : sourceRanges.asMap().entrySet()) {
				rangesToFetch.put(keyspaceName, entry);
			}
		}
		StreamPlan stream = new StreamPlan("Restore replica count");
		for (String keyspaceName : rangesToFetch.keySet()) {
			for (Map.Entry<InetAddress, Collection<Range<Token>>> entry : rangesToFetch.get(keyspaceName)) {
				InetAddress source = entry.getKey();
				InetAddress preferred = SystemKeyspace.getPreferredIP(source);
				Collection<Range<Token>> ranges = entry.getValue();
				if (StorageService.logger.isDebugEnabled())
					StorageService.logger.debug("Requesting from {} ranges {}", source, StringUtils.join(ranges, ", "));

				stream.requestRanges(source, preferred, keyspaceName, ranges);
			}
		}
		StreamResultFuture future = stream.execute();
		Futures.addCallback(future, new FutureCallback<StreamState>() {
			public void onSuccess(StreamState finalState) {
				sendReplicationNotification(notifyEndpoint);
			}

			public void onFailure(Throwable t) {
				StorageService.logger.warn("Streaming to restore replica count failed", t);
				sendReplicationNotification(notifyEndpoint);
			}
		});
	}

	private Multimap<Range<Token>, InetAddress> getChangedRangesForLeaving(String keyspaceName, InetAddress endpoint) {
		Collection<Range<Token>> ranges = getRangesForEndpoint(keyspaceName, endpoint);
		if (StorageService.logger.isDebugEnabled())
			StorageService.logger.debug("Node {} ranges [{}]", endpoint, StringUtils.join(ranges, ", "));

		Map<Range<Token>, List<InetAddress>> currentReplicaEndpoints = new HashMap<>(ranges.size());
		TokenMetadata metadata = tokenMetadata.cloneOnlyTokenMap();
		for (Range<Token> range : ranges)
			currentReplicaEndpoints.put(range, Keyspace.open(keyspaceName).getReplicationStrategy().calculateNaturalEndpoints(range.right, metadata));

		TokenMetadata temp = tokenMetadata.cloneAfterAllLeft();
		if (temp.isMember(endpoint))
			temp.removeEndpoint(endpoint);

		Multimap<Range<Token>, InetAddress> changedRanges = HashMultimap.create();
		for (Range<Token> range : ranges) {
			Collection<InetAddress> newReplicaEndpoints = Keyspace.open(keyspaceName).getReplicationStrategy().calculateNaturalEndpoints(range.right, temp);
			newReplicaEndpoints.removeAll(currentReplicaEndpoints.get(range));
			if (StorageService.logger.isDebugEnabled())
				if (newReplicaEndpoints.isEmpty())
					StorageService.logger.debug("Range {} already in all replicas", range);
				else
					StorageService.logger.debug("Range {} will be responsibility of {}", range, StringUtils.join(newReplicaEndpoints, ", "));


			changedRanges.putAll(range, newReplicaEndpoints);
		}
		return changedRanges;
	}

	public void onJoin(InetAddress endpoint, EndpointState epState) {
		for (Map.Entry<ApplicationState, VersionedValue> entry : epState.states()) {
			onChange(endpoint, entry.getKey(), entry.getValue());
		}
		MigrationManager.instance.scheduleSchemaPull(endpoint, epState);
	}

	public void onAlive(InetAddress endpoint, EndpointState state) {
		MigrationManager.instance.scheduleSchemaPull(endpoint, state);
		if (tokenMetadata.isMember(endpoint))
			notifyUp(endpoint);

	}

	public void onRemove(InetAddress endpoint) {
		tokenMetadata.removeEndpoint(endpoint);
		PendingRangeCalculatorService.instance.update();
	}

	public void onDead(InetAddress endpoint, EndpointState state) {
		MessagingService.instance().convict(endpoint);
		notifyDown(endpoint);
	}

	public void onRestart(InetAddress endpoint, EndpointState state) {
		if (state.isAlive())
			onDead(endpoint, state);

		VersionedValue netVersion = state.getApplicationState(ApplicationState.NET_VERSION);
		if (netVersion != null)
			updateNetVersion(endpoint, netVersion);

	}

	public String getLoadString() {
		return FileUtils.stringifyFileSize(StorageMetrics.load.getCount());
	}

	public Map<String, String> getLoadMap() {
		Map<String, String> map = new HashMap<>();
		for (Map.Entry<InetAddress, Double> entry : LoadBroadcaster.instance.getLoadInfo().entrySet()) {
			map.put(entry.getKey().getHostAddress(), FileUtils.stringifyFileSize(entry.getValue()));
		}
		map.put(FBUtilities.getBroadcastAddress().getHostAddress(), getLoadString());
		return map;
	}

	public final void deliverHints(String host) {
		throw new UnsupportedOperationException();
	}

	public Collection<Token> getLocalTokens() {
		Collection<Token> tokens = SystemKeyspace.getSavedTokens();
		assert (tokens != null) && (!(tokens.isEmpty()));
		return tokens;
	}

	@javax.annotation.Nullable
	public InetAddress getEndpointForHostId(UUID hostId) {
		return tokenMetadata.getEndpointForHostId(hostId);
	}

	@javax.annotation.Nullable
	public UUID getHostIdForEndpoint(InetAddress address) {
		return tokenMetadata.getHostId(address);
	}

	public List<String> getTokens() {
		return getTokens(FBUtilities.getBroadcastAddress());
	}

	public List<String> getTokens(String endpoint) throws UnknownHostException {
		return getTokens(InetAddress.getByName(endpoint));
	}

	private List<String> getTokens(InetAddress endpoint) {
		List<String> strTokens = new ArrayList<>();
		for (Token tok : getTokenMetadata().getTokens(endpoint))
			strTokens.add(tok.toString());

		return strTokens;
	}

	public String getReleaseVersion() {
		return FBUtilities.getReleaseVersionString();
	}

	public String getSchemaVersion() {
		return Schema.instance.getVersion().toString();
	}

	public List<String> getLeavingNodes() {
		return stringify(tokenMetadata.getLeavingEndpoints());
	}

	public List<String> getMovingNodes() {
		List<String> endpoints = new ArrayList<>();
		for (Pair<Token, InetAddress> node : tokenMetadata.getMovingEndpoints()) {
			endpoints.add(node.right.getHostAddress());
		}
		return endpoints;
	}

	public List<String> getJoiningNodes() {
		return stringify(tokenMetadata.getBootstrapTokens().valueSet());
	}

	public List<String> getLiveNodes() {
		return stringify(Gossiper.instance.getLiveMembers());
	}

	public Set<InetAddress> getLiveRingMembers() {
		return getLiveRingMembers(false);
	}

	public Set<InetAddress> getLiveRingMembers(boolean excludeDeadStates) {
		Set<InetAddress> ret = new HashSet<>();
		for (InetAddress ep : Gossiper.instance.getLiveMembers()) {
			if (excludeDeadStates) {
				EndpointState epState = Gossiper.instance.getEndpointStateForEndpoint(ep);
				if ((epState == null) || (Gossiper.instance.isDeadState(epState)))
					continue;

			}
			if (tokenMetadata.isMember(ep))
				ret.add(ep);

		}
		return ret;
	}

	public List<String> getUnreachableNodes() {
		return stringify(Gossiper.instance.getUnreachableMembers());
	}

	public String[] getAllDataFileLocations() {
		String[] locations = DatabaseDescriptor.getAllDataFileLocations();
		for (int i = 0; i < (locations.length); i++)
			locations[i] = FileUtils.getCanonicalPath(locations[i]);

		return locations;
	}

	public String getCommitLogLocation() {
		return FileUtils.getCanonicalPath(DatabaseDescriptor.getCommitLogLocation());
	}

	public String getSavedCachesLocation() {
		return FileUtils.getCanonicalPath(DatabaseDescriptor.getSavedCachesLocation());
	}

	private List<String> stringify(Iterable<InetAddress> endpoints) {
		List<String> stringEndpoints = new ArrayList<>();
		for (InetAddress ep : endpoints) {
			stringEndpoints.add(ep.getHostAddress());
		}
		return stringEndpoints;
	}

	public int getCurrentGenerationNumber() {
		return Gossiper.instance.getCurrentGenerationNumber(FBUtilities.getBroadcastAddress());
	}

	public int forceKeyspaceCleanup(String keyspaceName, String... tables) throws IOException, InterruptedException, ExecutionException {
		return forceKeyspaceCleanup(0, keyspaceName, tables);
	}

	public int forceKeyspaceCleanup(int jobs, String keyspaceName, String... tables) throws IOException, InterruptedException, ExecutionException {
		if (SchemaConstants.isLocalSystemKeyspace(keyspaceName))
			throw new RuntimeException("Cleanup of the system keyspace is neither necessary nor wise");

		CompactionManager.AllSSTableOpStatus status = SUCCESSFUL;
		for (ColumnFamilyStore cfStore : getValidColumnFamilies(false, false, keyspaceName, tables)) {
			CompactionManager.AllSSTableOpStatus oneStatus = cfStore.forceCleanup(jobs);
			if (oneStatus != (SUCCESSFUL))
				status = oneStatus;

		}
		return status.statusCode;
	}

	public int scrub(boolean disableSnapshot, boolean skipCorrupted, String keyspaceName, String... tables) throws IOException, InterruptedException, ExecutionException {
		return scrub(disableSnapshot, skipCorrupted, true, 0, keyspaceName, tables);
	}

	public int scrub(boolean disableSnapshot, boolean skipCorrupted, boolean checkData, String keyspaceName, String... tables) throws IOException, InterruptedException, ExecutionException {
		return scrub(disableSnapshot, skipCorrupted, checkData, 0, keyspaceName, tables);
	}

	public int scrub(boolean disableSnapshot, boolean skipCorrupted, boolean checkData, int jobs, String keyspaceName, String... tables) throws IOException, InterruptedException, ExecutionException {
		return scrub(disableSnapshot, skipCorrupted, checkData, false, jobs, keyspaceName, tables);
	}

	public int scrub(boolean disableSnapshot, boolean skipCorrupted, boolean checkData, boolean reinsertOverflowedTTL, int jobs, String keyspaceName, String... tables) throws IOException, InterruptedException, ExecutionException {
		CompactionManager.AllSSTableOpStatus status = SUCCESSFUL;
		for (ColumnFamilyStore cfStore : getValidColumnFamilies(true, false, keyspaceName, tables)) {
			CompactionManager.AllSSTableOpStatus oneStatus = cfStore.scrub(disableSnapshot, skipCorrupted, reinsertOverflowedTTL, checkData, jobs);
			if (oneStatus != (SUCCESSFUL))
				status = oneStatus;

		}
		return status.statusCode;
	}

	public int verify(boolean extendedVerify, String keyspaceName, String... tableNames) throws IOException, InterruptedException, ExecutionException {
		CompactionManager.AllSSTableOpStatus status = SUCCESSFUL;
		for (ColumnFamilyStore cfStore : getValidColumnFamilies(false, false, keyspaceName, tableNames)) {
			CompactionManager.AllSSTableOpStatus oneStatus = cfStore.verify(extendedVerify);
			if (oneStatus != (SUCCESSFUL))
				status = oneStatus;

		}
		return status.statusCode;
	}

	public int upgradeSSTables(String keyspaceName, boolean excludeCurrentVersion, String... tableNames) throws IOException, InterruptedException, ExecutionException {
		return upgradeSSTables(keyspaceName, excludeCurrentVersion, 0, tableNames);
	}

	public int upgradeSSTables(String keyspaceName, boolean excludeCurrentVersion, int jobs, String... tableNames) throws IOException, InterruptedException, ExecutionException {
		CompactionManager.AllSSTableOpStatus status = SUCCESSFUL;
		for (ColumnFamilyStore cfStore : getValidColumnFamilies(true, true, keyspaceName, tableNames)) {
			CompactionManager.AllSSTableOpStatus oneStatus = cfStore.sstablesRewrite(excludeCurrentVersion, jobs);
			if (oneStatus != (SUCCESSFUL))
				status = oneStatus;

		}
		return status.statusCode;
	}

	public void forceKeyspaceCompaction(boolean splitOutput, String keyspaceName, String... tableNames) throws IOException, InterruptedException, ExecutionException {
		for (ColumnFamilyStore cfStore : getValidColumnFamilies(true, false, keyspaceName, tableNames)) {
			cfStore.forceMajorCompaction(splitOutput);
		}
	}

	public int relocateSSTables(String keyspaceName, String... columnFamilies) throws IOException, InterruptedException, ExecutionException {
		return relocateSSTables(0, keyspaceName, columnFamilies);
	}

	public int relocateSSTables(int jobs, String keyspaceName, String... columnFamilies) throws IOException, InterruptedException, ExecutionException {
		CompactionManager.AllSSTableOpStatus status = SUCCESSFUL;
		for (ColumnFamilyStore cfs : getValidColumnFamilies(false, false, keyspaceName, columnFamilies)) {
			CompactionManager.AllSSTableOpStatus oneStatus = cfs.relocateSSTables(jobs);
			if (oneStatus != (SUCCESSFUL))
				status = oneStatus;

		}
		return status.statusCode;
	}

	public int garbageCollect(String tombstoneOptionString, int jobs, String keyspaceName, String... columnFamilies) throws IOException, InterruptedException, ExecutionException {
		CompactionParams.TombstoneOption tombstoneOption = valueOf(tombstoneOptionString);
		CompactionManager.AllSSTableOpStatus status = SUCCESSFUL;
		for (ColumnFamilyStore cfs : getValidColumnFamilies(false, false, keyspaceName, columnFamilies)) {
			CompactionManager.AllSSTableOpStatus oneStatus = cfs.garbageCollect(tombstoneOption, jobs);
			if (oneStatus != (SUCCESSFUL))
				status = oneStatus;

		}
		return status.statusCode;
	}

	@Override
	public void takeSnapshot(String tag, Map<String, String> options, String... entities) throws IOException {
		boolean skipFlush = Boolean.parseBoolean(options.getOrDefault("skipFlush", "false"));
		if (((entities != null) && ((entities.length) > 0)) && (entities[0].contains("."))) {
			takeMultipleTableSnapshot(tag, skipFlush, entities);
		}else {
			takeSnapshot(tag, skipFlush, entities);
		}
	}

	public void takeTableSnapshot(String keyspaceName, String tableName, String tag) throws IOException {
		takeMultipleTableSnapshot(tag, false, ((keyspaceName + ".") + tableName));
	}

	public void forceKeyspaceCompactionForTokenRange(String keyspaceName, String startToken, String endToken, String... tableNames) throws IOException, InterruptedException, ExecutionException {
		Collection<Range<Token>> tokenRanges = createRepairRangeFrom(startToken, endToken);
		for (ColumnFamilyStore cfStore : getValidColumnFamilies(true, false, keyspaceName, tableNames)) {
			cfStore.forceCompactionForTokenRange(tokenRanges);
		}
	}

	public void takeSnapshot(String tag, String... keyspaceNames) throws IOException {
		takeSnapshot(tag, false, keyspaceNames);
	}

	public void takeMultipleTableSnapshot(String tag, String... tableList) throws IOException {
		takeMultipleTableSnapshot(tag, false, tableList);
	}

	private void takeSnapshot(String tag, boolean skipFlush, String... keyspaceNames) throws IOException {
		if ((operationMode) == (StorageService.Mode.JOINING))
			throw new IOException("Cannot snapshot until bootstrap completes");

		if ((tag == null) || (tag.equals("")))
			throw new IOException("You must supply a snapshot name.");

		Iterable<Keyspace> keyspaces;
		if ((keyspaceNames.length) == 0) {
			keyspaces = Keyspace.all();
		}else {
			ArrayList<Keyspace> t = new ArrayList<>(keyspaceNames.length);
			for (String keyspaceName : keyspaceNames)
				t.add(getValidKeyspace(keyspaceName));

			keyspaces = t;
		}
		for (Keyspace keyspace : keyspaces)
			if (keyspace.snapshotExists(tag))
				throw new IOException((("Snapshot " + tag) + " already exists."));


		for (Keyspace keyspace : keyspaces)
			keyspace.snapshot(tag, null, skipFlush);

	}

	private void takeMultipleTableSnapshot(String tag, boolean skipFlush, String... tableList) throws IOException {
		Map<Keyspace, List<String>> keyspaceColumnfamily = new HashMap<Keyspace, List<String>>();
		for (String table : tableList) {
			String[] splittedString = StringUtils.split(table, '.');
			if ((splittedString.length) == 2) {
				String keyspaceName = splittedString[0];
				String tableName = splittedString[1];
				if (keyspaceName == null)
					throw new IOException("You must supply a keyspace name");

				if (operationMode.equals(StorageService.Mode.JOINING))
					throw new IOException("Cannot snapshot until bootstrap completes");

				if (tableName == null)
					throw new IOException("You must supply a table name");

				if ((tag == null) || (tag.equals("")))
					throw new IOException("You must supply a snapshot name.");

				Keyspace keyspace = getValidKeyspace(keyspaceName);
				ColumnFamilyStore columnFamilyStore = keyspace.getColumnFamilyStore(tableName);
				if (columnFamilyStore.snapshotExists(tag))
					throw new IOException((("Snapshot " + tag) + " already exists."));

				if (!(keyspaceColumnfamily.containsKey(keyspace))) {
					keyspaceColumnfamily.put(keyspace, new ArrayList<String>());
				}
				keyspaceColumnfamily.get(keyspace).add(tableName);
			}else {
				throw new IllegalArgumentException("Cannot take a snapshot on secondary index or invalid column family name. You must supply a column family name in the form of keyspace.columnfamily");
			}
		}
		for (Map.Entry<Keyspace, List<String>> entry : keyspaceColumnfamily.entrySet()) {
			for (String table : entry.getValue())
				entry.getKey().snapshot(tag, table, skipFlush);

		}
	}

	private Keyspace getValidKeyspace(String keyspaceName) throws IOException {
		if (!(Schema.instance.getKeyspaces().contains(keyspaceName))) {
			throw new IOException((("Keyspace " + keyspaceName) + " does not exist"));
		}
		return Keyspace.open(keyspaceName);
	}

	public void clearSnapshot(String tag, String... keyspaceNames) throws IOException {
		if (tag == null)
			tag = "";

		Set<String> keyspaces = new HashSet<>();
		for (String dataDir : DatabaseDescriptor.getAllDataFileLocations()) {
			for (String keyspaceDir : new File(dataDir).list()) {
				if (((keyspaceNames.length) > 0) && (!(Arrays.asList(keyspaceNames).contains(keyspaceDir))))
					continue;

				keyspaces.add(keyspaceDir);
			}
		}
		for (String keyspace : keyspaces)
			Keyspace.clearSnapshot(tag, keyspace);

		if (StorageService.logger.isDebugEnabled())
			StorageService.logger.debug("Cleared out snapshot directories");

	}

	public Map<String, TabularData> getSnapshotDetails() {
		Map<String, TabularData> snapshotMap = new HashMap<>();
		for (Keyspace keyspace : Keyspace.all()) {
			if (SchemaConstants.isLocalSystemKeyspace(keyspace.getName()))
				continue;

			for (ColumnFamilyStore cfStore : keyspace.getColumnFamilyStores()) {
				for (Map.Entry<String, Pair<Long, Long>> snapshotDetail : cfStore.getSnapshotDetails().entrySet()) {
					TabularDataSupport data = ((TabularDataSupport) (snapshotMap.get(snapshotDetail.getKey())));
					if (data == null) {
						data = new TabularDataSupport(SnapshotDetailsTabularData.TABULAR_TYPE);
						snapshotMap.put(snapshotDetail.getKey(), data);
					}
					SnapshotDetailsTabularData.from(snapshotDetail.getKey(), keyspace.getName(), cfStore.getColumnFamilyName(), snapshotDetail, data);
				}
			}
		}
		return snapshotMap;
	}

	public long trueSnapshotsSize() {
		long total = 0;
		for (Keyspace keyspace : Keyspace.all()) {
			if (SchemaConstants.isLocalSystemKeyspace(keyspace.getName()))
				continue;

			for (ColumnFamilyStore cfStore : keyspace.getColumnFamilyStores()) {
				total += cfStore.trueSnapshotsSize();
			}
		}
		return total;
	}

	public void refreshSizeEstimates() throws ExecutionException {
		FBUtilities.waitOnFuture(ScheduledExecutors.optionalTasks.submit(SizeEstimatesRecorder.instance));
	}

	public Iterable<ColumnFamilyStore> getValidColumnFamilies(boolean allowIndexes, boolean autoAddIndexes, String keyspaceName, String... cfNames) throws IOException {
		Keyspace keyspace = getValidKeyspace(keyspaceName);
		return keyspace.getValidColumnFamilies(allowIndexes, autoAddIndexes, cfNames);
	}

	public void forceKeyspaceFlush(String keyspaceName, String... tableNames) throws IOException {
		for (ColumnFamilyStore cfStore : getValidColumnFamilies(true, false, keyspaceName, tableNames)) {
			StorageService.logger.debug("Forcing flush on keyspace {}, CF {}", keyspaceName, cfStore.name);
			cfStore.forceBlockingFlush();
		}
	}

	public int repairAsync(String keyspace, Map<String, String> repairSpec) {
		RepairOption option = RepairOption.parse(repairSpec, tokenMetadata.partitioner);
		if (option.getRanges().isEmpty()) {
			if (option.isPrimaryRange()) {
				if ((option.getDataCenters().isEmpty()) && (option.getHosts().isEmpty()))
					option.getRanges().addAll(getPrimaryRanges(keyspace));
				else
					if (option.isInLocalDCOnly())
						option.getRanges().addAll(getPrimaryRangesWithinDC(keyspace));
					else
						throw new IllegalArgumentException("You need to run primary range repair on all nodes in the cluster.");


			}else {
				option.getRanges().addAll(getLocalRanges(keyspace));
			}
		}
		return forceRepairAsync(keyspace, option, false);
	}

	@Deprecated
	public int forceRepairAsync(String keyspace, boolean isSequential, Collection<String> dataCenters, Collection<String> hosts, boolean primaryRange, boolean fullRepair, String... tableNames) {
		return forceRepairAsync(keyspace, (isSequential ? RepairParallelism.SEQUENTIAL.ordinal() : RepairParallelism.PARALLEL.ordinal()), dataCenters, hosts, primaryRange, fullRepair, tableNames);
	}

	@Deprecated
	public int forceRepairAsync(String keyspace, int parallelismDegree, Collection<String> dataCenters, Collection<String> hosts, boolean primaryRange, boolean fullRepair, String... tableNames) {
		if ((parallelismDegree < 0) || (parallelismDegree > ((RepairParallelism.values().length) - 1))) {
			throw new IllegalArgumentException(("Invalid parallelism degree specified: " + parallelismDegree));
		}
		RepairParallelism parallelism = RepairParallelism.values()[parallelismDegree];
		if ((FBUtilities.isWindows) && (parallelism != (RepairParallelism.PARALLEL))) {
			StorageService.logger.warn("Snapshot-based repair is not yet supported on Windows.  Reverting to parallel repair.");
			parallelism = RepairParallelism.PARALLEL;
		}
		RepairOption options = new RepairOption(parallelism, primaryRange, (!fullRepair), false, 1, Collections.<Range<Token>>emptyList(), false, false);
		if (dataCenters != null) {
			options.getDataCenters().addAll(dataCenters);
		}
		if (hosts != null) {
			options.getHosts().addAll(hosts);
		}
		if (primaryRange) {
			if ((options.getDataCenters().isEmpty()) && (options.getHosts().isEmpty()))
				options.getRanges().addAll(getPrimaryRanges(keyspace));
			else
				if (((options.getDataCenters().size()) == 1) && (options.getDataCenters().contains(DatabaseDescriptor.getLocalDataCenter())))
					options.getRanges().addAll(getPrimaryRangesWithinDC(keyspace));
				else
					throw new IllegalArgumentException("You need to run primary range repair on all nodes in the cluster.");


		}else {
			options.getRanges().addAll(getLocalRanges(keyspace));
		}
		if (tableNames != null) {
			for (String table : tableNames) {
				options.getColumnFamilies().add(table);
			}
		}
		return forceRepairAsync(keyspace, options, true);
	}

	@Deprecated
	public int forceRepairAsync(String keyspace, boolean isSequential, boolean isLocal, boolean primaryRange, boolean fullRepair, String... tableNames) {
		Set<String> dataCenters = null;
		if (isLocal) {
			dataCenters = Sets.newHashSet(DatabaseDescriptor.getLocalDataCenter());
		}
		return forceRepairAsync(keyspace, isSequential, dataCenters, null, primaryRange, fullRepair, tableNames);
	}

	@Deprecated
	public int forceRepairRangeAsync(String beginToken, String endToken, String keyspaceName, boolean isSequential, Collection<String> dataCenters, Collection<String> hosts, boolean fullRepair, String... tableNames) {
		return forceRepairRangeAsync(beginToken, endToken, keyspaceName, (isSequential ? RepairParallelism.SEQUENTIAL.ordinal() : RepairParallelism.PARALLEL.ordinal()), dataCenters, hosts, fullRepair, tableNames);
	}

	@Deprecated
	public int forceRepairRangeAsync(String beginToken, String endToken, String keyspaceName, int parallelismDegree, Collection<String> dataCenters, Collection<String> hosts, boolean fullRepair, String... tableNames) {
		if ((parallelismDegree < 0) || (parallelismDegree > ((RepairParallelism.values().length) - 1))) {
			throw new IllegalArgumentException(("Invalid parallelism degree specified: " + parallelismDegree));
		}
		RepairParallelism parallelism = RepairParallelism.values()[parallelismDegree];
		if ((FBUtilities.isWindows) && (parallelism != (RepairParallelism.PARALLEL))) {
			StorageService.logger.warn("Snapshot-based repair is not yet supported on Windows.  Reverting to parallel repair.");
			parallelism = RepairParallelism.PARALLEL;
		}
		if (!fullRepair)
			StorageService.logger.warn(("Incremental repair can't be requested with subrange repair " + ("because each subrange repair would generate an anti-compacted table. " + "The repair will occur but without anti-compaction.")));

		Collection<Range<Token>> repairingRange = createRepairRangeFrom(beginToken, endToken);
		RepairOption options = new RepairOption(parallelism, false, (!fullRepair), false, 1, repairingRange, true, false);
		if (dataCenters != null) {
			options.getDataCenters().addAll(dataCenters);
		}
		if (hosts != null) {
			options.getHosts().addAll(hosts);
		}
		if (tableNames != null) {
			for (String table : tableNames) {
				options.getColumnFamilies().add(table);
			}
		}
		StorageService.logger.info("starting user-requested repair of range {} for keyspace {} and column families {}", repairingRange, keyspaceName, tableNames);
		return forceRepairAsync(keyspaceName, options, true);
	}

	@Deprecated
	public int forceRepairRangeAsync(String beginToken, String endToken, String keyspaceName, boolean isSequential, boolean isLocal, boolean fullRepair, String... tableNames) {
		Set<String> dataCenters = null;
		if (isLocal) {
			dataCenters = Sets.newHashSet(DatabaseDescriptor.getLocalDataCenter());
		}
		return forceRepairRangeAsync(beginToken, endToken, keyspaceName, isSequential, dataCenters, null, fullRepair, tableNames);
	}

	@com.google.common.annotations.VisibleForTesting
	Collection<Range<Token>> createRepairRangeFrom(String beginToken, String endToken) {
		Token parsedBeginToken = getTokenFactory().fromString(beginToken);
		Token parsedEndToken = getTokenFactory().fromString(endToken);
		ArrayList<Range<Token>> repairingRange = new ArrayList<>();
		ArrayList<Token> tokens = new ArrayList<>(tokenMetadata.sortedTokens());
		if (!(tokens.contains(parsedBeginToken))) {
			tokens.add(parsedBeginToken);
		}
		if (!(tokens.contains(parsedEndToken))) {
			tokens.add(parsedEndToken);
		}
		Collections.sort(tokens);
		int start = tokens.indexOf(parsedBeginToken);
		int end = tokens.indexOf(parsedEndToken);
		for (int i = start; i != end; i = (i + 1) % (tokens.size())) {
			Range<Token> range = new Range<>(tokens.get(i), tokens.get(((i + 1) % (tokens.size()))));
			repairingRange.add(range);
		}
		return repairingRange;
	}

	public Token.TokenFactory getTokenFactory() {
		return tokenMetadata.partitioner.getTokenFactory();
	}

	public int forceRepairAsync(String keyspace, RepairOption options, boolean legacy) {
		if ((options.getRanges().isEmpty()) || ((Keyspace.open(keyspace).getReplicationStrategy().getReplicationFactor()) < 2))
			return 0;

		int cmd = StorageService.nextRepairCommand.incrementAndGet();
		NamedThreadFactory.createThread(createRepairTask(cmd, keyspace, options, legacy), ("Repair-Task-" + (StorageService.threadCounter.incrementAndGet()))).start();
		return cmd;
	}

	private FutureTask<Object> createRepairTask(final int cmd, final String keyspace, final RepairOption options, boolean legacy) {
		if ((!(options.getDataCenters().isEmpty())) && (!(options.getDataCenters().contains(DatabaseDescriptor.getLocalDataCenter())))) {
			throw new IllegalArgumentException("the local data center must be part of the repair");
		}
		if (legacy) {
		}
		return null;
	}

	public void forceTerminateAllRepairSessions() {
		ActiveRepairService.instance.terminateSessions();
	}

	public Collection<Range<Token>> getPrimaryRangesForEndpoint(String keyspace, InetAddress ep) {
		AbstractReplicationStrategy strategy = Keyspace.open(keyspace).getReplicationStrategy();
		Collection<Range<Token>> primaryRanges = new HashSet<>();
		TokenMetadata metadata = tokenMetadata.cloneOnlyTokenMap();
		for (Token token : metadata.sortedTokens()) {
			List<InetAddress> endpoints = strategy.calculateNaturalEndpoints(token, metadata);
			if (((endpoints.size()) > 0) && (endpoints.get(0).equals(ep)))
				primaryRanges.add(new Range<>(metadata.getPredecessor(token), token));

		}
		return primaryRanges;
	}

	public Collection<Range<Token>> getPrimaryRangeForEndpointWithinDC(String keyspace, InetAddress referenceEndpoint) {
		TokenMetadata metadata = tokenMetadata.cloneOnlyTokenMap();
		String localDC = DatabaseDescriptor.getEndpointSnitch().getDatacenter(referenceEndpoint);
		Collection<InetAddress> localDcNodes = metadata.getTopology().getDatacenterEndpoints().get(localDC);
		AbstractReplicationStrategy strategy = Keyspace.open(keyspace).getReplicationStrategy();
		Collection<Range<Token>> localDCPrimaryRanges = new HashSet<>();
		for (Token token : metadata.sortedTokens()) {
			List<InetAddress> endpoints = strategy.calculateNaturalEndpoints(token, metadata);
			for (InetAddress endpoint : endpoints) {
				if (localDcNodes.contains(endpoint)) {
					if (endpoint.equals(referenceEndpoint)) {
						localDCPrimaryRanges.add(new Range<>(metadata.getPredecessor(token), token));
					}
					break;
				}
			}
		}
		return localDCPrimaryRanges;
	}

	Collection<Range<Token>> getRangesForEndpoint(String keyspaceName, InetAddress ep) {
		return Keyspace.open(keyspaceName).getReplicationStrategy().getAddressRanges().get(ep);
	}

	public List<Range<Token>> getAllRanges(List<Token> sortedTokens) {
		if (StorageService.logger.isTraceEnabled())
			StorageService.logger.trace("computing ranges for {}", StringUtils.join(sortedTokens, ", "));

		if (sortedTokens.isEmpty())
			return Collections.emptyList();

		int size = sortedTokens.size();
		List<Range<Token>> ranges = new ArrayList<>((size + 1));
		for (int i = 1; i < size; ++i) {
			Range<Token> range = new Range<>(sortedTokens.get((i - 1)), sortedTokens.get(i));
			ranges.add(range);
		}
		Range<Token> range = new Range<>(sortedTokens.get((size - 1)), sortedTokens.get(0));
		ranges.add(range);
		return ranges;
	}

	public List<InetAddress> getNaturalEndpoints(String keyspaceName, String cf, String key) {
		KeyspaceMetadata ksMetaData = Schema.instance.getKSMetaData(keyspaceName);
		if (ksMetaData == null)
			throw new IllegalArgumentException((("Unknown keyspace '" + keyspaceName) + "'"));

		CFMetaData cfMetaData = ksMetaData.getTableOrViewNullable(cf);
		if (cfMetaData == null)
			throw new IllegalArgumentException((((("Unknown table '" + cf) + "' in keyspace '") + keyspaceName) + "'"));

		return getNaturalEndpoints(keyspaceName, tokenMetadata.partitioner.getToken(cfMetaData.getKeyValidator().fromString(key)));
	}

	public List<InetAddress> getNaturalEndpoints(String keyspaceName, ByteBuffer key) {
		return getNaturalEndpoints(keyspaceName, tokenMetadata.partitioner.getToken(key));
	}

	public List<InetAddress> getNaturalEndpoints(String keyspaceName, RingPosition pos) {
		return Keyspace.open(keyspaceName).getReplicationStrategy().getNaturalEndpoints(pos);
	}

	public Iterable<InetAddress> getNaturalAndPendingEndpoints(String keyspaceName, Token token) {
		return Iterables.concat(getNaturalEndpoints(keyspaceName, token), tokenMetadata.pendingEndpointsFor(token, keyspaceName));
	}

	public List<InetAddress> getLiveNaturalEndpoints(Keyspace keyspace, ByteBuffer key) {
		return getLiveNaturalEndpoints(keyspace, tokenMetadata.decorateKey(key));
	}

	public List<InetAddress> getLiveNaturalEndpoints(Keyspace keyspace, RingPosition pos) {
		List<InetAddress> liveEps = new ArrayList<>();
		getLiveNaturalEndpoints(keyspace, pos, liveEps);
		return liveEps;
	}

	public void getLiveNaturalEndpoints(Keyspace keyspace, RingPosition pos, List<InetAddress> liveEps) {
		List<InetAddress> endpoints = keyspace.getReplicationStrategy().getNaturalEndpoints(pos);
		for (InetAddress endpoint : endpoints) {
			if (FailureDetector.instance.isAlive(endpoint))
				liveEps.add(endpoint);

		}
	}

	public void setLoggingLevel(String classQualifier, String rawLevel) throws Exception {
		ch.qos.logback.classic.Logger logBackLogger = ((ch.qos.logback.classic.Logger) (LoggerFactory.getLogger(classQualifier)));
		if ((StringUtils.isBlank(classQualifier)) && (StringUtils.isBlank(rawLevel))) {
			JMXConfiguratorMBean jmxConfiguratorMBean = JMX.newMBeanProxy(ManagementFactory.getPlatformMBeanServer(), new ObjectName("ch.qos.logback.classic:Name=default,Type=ch.qos.logback.classic.jmx.JMXConfigurator"), JMXConfiguratorMBean.class);
			jmxConfiguratorMBean.reloadDefaultConfiguration();
			return;
		}else
			if ((StringUtils.isNotBlank(classQualifier)) && (StringUtils.isBlank(rawLevel))) {
				if (((logBackLogger.getLevel()) != null) || (hasAppenders(logBackLogger)))
					logBackLogger.setLevel(null);

				return;
			}

		Level level = Level.toLevel(rawLevel);
		logBackLogger.setLevel(level);
		StorageService.logger.info("set log level to {} for classes under '{}' (if the level doesn't look like '{}' then the logger couldn't parse '{}')", level, classQualifier, rawLevel, rawLevel);
	}

	@Override
	public Map<String, String> getLoggingLevels() {
		Map<String, String> logLevelMaps = Maps.newLinkedHashMap();
		LoggerContext lc = ((LoggerContext) (LoggerFactory.getILoggerFactory()));
		for (ch.qos.logback.classic.Logger logger : lc.getLoggerList()) {
			if (((logger.getLevel()) != null) || (hasAppenders(logger)))
				logLevelMaps.put(logger.getName(), logger.getLevel().toString());

		}
		return logLevelMaps;
	}

	private boolean hasAppenders(ch.qos.logback.classic.Logger logger) {
		Iterator<Appender<ILoggingEvent>> it = logger.iteratorForAppenders();
		return it.hasNext();
	}

	public List<Pair<Range<Token>, Long>> getSplits(String keyspaceName, String cfName, Range<Token> range, int keysPerSplit) {
		Keyspace t = Keyspace.open(keyspaceName);
		ColumnFamilyStore cfs = t.getColumnFamilyStore(cfName);
		List<DecoratedKey> keys = keySamples(Collections.singleton(cfs), range);
		long totalRowCountEstimate = cfs.estimatedKeysForRange(range);
		int minSamplesPerSplit = 4;
		int maxSplitCount = ((keys.size()) / minSamplesPerSplit) + 1;
		int splitCount = Math.max(1, Math.min(maxSplitCount, ((int) (totalRowCountEstimate / keysPerSplit))));
		List<Token> tokens = keysToTokens(range, keys);
		return getSplits(tokens, splitCount, cfs);
	}

	private List<Pair<Range<Token>, Long>> getSplits(List<Token> tokens, int splitCount, ColumnFamilyStore cfs) {
		double step = ((double) ((tokens.size()) - 1)) / splitCount;
		Token prevToken = tokens.get(0);
		List<Pair<Range<Token>, Long>> splits = Lists.newArrayListWithExpectedSize(splitCount);
		for (int i = 1; i <= splitCount; i++) {
			int index = ((int) (Math.round((i * step))));
			Token token = tokens.get(index);
			Range<Token> range = new Range<>(prevToken, token);
			splits.add(Pair.create(range, Math.max(cfs.metadata.params.minIndexInterval, cfs.estimatedKeysForRange(range))));
			prevToken = token;
		}
		return splits;
	}

	private List<Token> keysToTokens(Range<Token> range, List<DecoratedKey> keys) {
		List<Token> tokens = Lists.newArrayListWithExpectedSize(((keys.size()) + 2));
		tokens.add(range.left);
		for (DecoratedKey key : keys)
			tokens.add(key.getToken());

		tokens.add(range.right);
		return tokens;
	}

	private List<DecoratedKey> keySamples(Iterable<ColumnFamilyStore> cfses, Range<Token> range) {
		List<DecoratedKey> keys = new ArrayList<>();
		for (ColumnFamilyStore cfs : cfses)
			Iterables.addAll(keys, cfs.keySamples(range));

		FBUtilities.sortSampledKeys(keys, range);
		return keys;
	}

	private void startLeaving() {
		Gossiper.instance.addLocalApplicationState(ApplicationState.STATUS, valueFactory.leaving(getLocalTokens()));
		tokenMetadata.addLeavingEndpoint(FBUtilities.getBroadcastAddress());
		PendingRangeCalculatorService.instance.update();
	}

	public void decommission() throws InterruptedException {
		if (!(tokenMetadata.isMember(FBUtilities.getBroadcastAddress())))
			throw new UnsupportedOperationException("local node is not a member of the token ring yet");

		if ((tokenMetadata.cloneAfterAllLeft().sortedTokens().size()) < 2)
			throw new UnsupportedOperationException("no other normal nodes in the ring; decommission would be pointless");

		if (((operationMode) != (StorageService.Mode.LEAVING)) && ((operationMode) != (StorageService.Mode.NORMAL)))
			throw new UnsupportedOperationException((("Node in " + (operationMode)) + " state; wait for status to become normal or restart"));

		if (isDecommissioning.compareAndSet(true, true))
			throw new IllegalStateException("Node is still decommissioning. Check nodetool netstats.");

		if (StorageService.logger.isDebugEnabled())
			StorageService.logger.debug("DECOMMISSIONING");

		try {
			PendingRangeCalculatorService.instance.blockUntilFinished();
			for (String keyspaceName : Schema.instance.getNonLocalStrategyKeyspaces()) {
				if ((tokenMetadata.getPendingRanges(keyspaceName, FBUtilities.getBroadcastAddress()).size()) > 0)
					throw new UnsupportedOperationException("data is currently moving to this node; unable to leave the ring");

			}
			startLeaving();
			long timeout = Math.max(StorageService.RING_DELAY, BatchlogManager.instance.getBatchlogTimeout());
			setMode(StorageService.Mode.LEAVING, (("sleeping " + timeout) + " ms for batch processing and pending range setup"), true);
			Thread.sleep(timeout);
			Runnable finishLeaving = new Runnable() {
				public void run() {
					shutdownClientServers();
					Gossiper.instance.stop();
					try {
						MessagingService.instance().shutdown();
					} catch (IOError ioe) {
						StorageService.logger.info("failed to shutdown message service: {}", ioe);
					}
					StageManager.shutdownNow();
					SystemKeyspace.setBootstrapState(DECOMMISSIONED);
					setMode(StorageService.Mode.DECOMMISSIONED, true);
				}
			};
			unbootstrap(finishLeaving);
		} catch (InterruptedException e) {
			throw new RuntimeException("Node interrupted while decommissioning");
		} catch (ExecutionException e) {
			StorageService.logger.error("Error while decommissioning node ", e.getCause());
			throw new RuntimeException(("Error while decommissioning node: " + (e.getCause().getMessage())));
		} finally {
			isDecommissioning.set(false);
		}
	}

	private void leaveRing() {
		SystemKeyspace.setBootstrapState(NEEDS_BOOTSTRAP);
		tokenMetadata.removeEndpoint(FBUtilities.getBroadcastAddress());
		PendingRangeCalculatorService.instance.update();
		Gossiper.instance.addLocalApplicationState(ApplicationState.STATUS, valueFactory.left(getLocalTokens(), Gossiper.computeExpireTime()));
		int delay = Math.max(StorageService.RING_DELAY, ((Gossiper.intervalInMillis) * 2));
		StorageService.logger.info("Announcing that I have left the ring for {}ms", delay);
		Uninterruptibles.sleepUninterruptibly(delay, TimeUnit.MILLISECONDS);
	}

	private void unbootstrap(Runnable onFinish) throws InterruptedException, ExecutionException {
		Map<String, Multimap<Range<Token>, InetAddress>> rangesToStream = new HashMap<>();
		for (String keyspaceName : Schema.instance.getNonLocalStrategyKeyspaces()) {
			Multimap<Range<Token>, InetAddress> rangesMM = getChangedRangesForLeaving(keyspaceName, FBUtilities.getBroadcastAddress());
			if (StorageService.logger.isDebugEnabled())
				StorageService.logger.debug("Ranges needing transfer are [{}]", StringUtils.join(rangesMM.keySet(), ","));

			rangesToStream.put(keyspaceName, rangesMM);
		}
		setMode(StorageService.Mode.LEAVING, "replaying batch log and streaming data to other nodes", true);
		Future<?> batchlogReplay = BatchlogManager.instance.startBatchlogReplay();
		Future<StreamState> streamSuccess = streamRanges(rangesToStream);
		StorageService.logger.debug("waiting for batch log processing.");
		batchlogReplay.get();
		setMode(StorageService.Mode.LEAVING, "streaming hints to other nodes", true);
		Future hintsSuccess = streamHints();
		StorageService.logger.debug("waiting for stream acks.");
		streamSuccess.get();
		hintsSuccess.get();
		StorageService.logger.debug("stream acks all received.");
		leaveRing();
		onFinish.run();
	}

	private Future streamHints() {
		return HintsService.instance.transferHints(this::getPreferredHintsStreamTarget);
	}

	private UUID getPreferredHintsStreamTarget() {
		List<InetAddress> candidates = new ArrayList<>(StorageService.instance.getTokenMetadata().cloneAfterAllLeft().getAllEndpoints());
		candidates.remove(FBUtilities.getBroadcastAddress());
		for (Iterator<InetAddress> iter = candidates.iterator(); iter.hasNext();) {
			InetAddress address = iter.next();
			if (!(FailureDetector.instance.isAlive(address)))
				iter.remove();

		}
		if (candidates.isEmpty()) {
			StorageService.logger.warn("Unable to stream hints since no live endpoints seen");
			throw new RuntimeException("Unable to stream hints since no live endpoints seen");
		}else {
			DatabaseDescriptor.getEndpointSnitch().sortByProximity(FBUtilities.getBroadcastAddress(), candidates);
			InetAddress hintsDestinationHost = candidates.get(0);
			return tokenMetadata.getHostId(hintsDestinationHost);
		}
	}

	public void move(String newToken) throws IOException {
		try {
			getTokenFactory().validate(newToken);
		} catch (ConfigurationException e) {
			throw new IOException(e.getMessage());
		}
		move(getTokenFactory().fromString(newToken));
	}

	private void move(Token newToken) throws IOException {
		if (newToken == null)
			throw new IOException("Can't move to the undefined (null) token.");

		if (tokenMetadata.sortedTokens().contains(newToken))
			throw new IOException((("target token " + newToken) + " is already owned by another node."));

		InetAddress localAddress = FBUtilities.getBroadcastAddress();
		if ((getTokenMetadata().getTokens(localAddress).size()) > 1) {
			StorageService.logger.error("Invalid request to move(Token); This node has more than one token and cannot be moved thusly.");
			throw new UnsupportedOperationException("This node has more than one token and cannot be moved thusly.");
		}
		List<String> keyspacesToProcess = Schema.instance.getNonLocalStrategyKeyspaces();
		PendingRangeCalculatorService.instance.blockUntilFinished();
		for (String keyspaceName : keyspacesToProcess) {
			if ((tokenMetadata.getPendingRanges(keyspaceName, localAddress).size()) > 0)
				throw new UnsupportedOperationException("data is currently moving to this node; unable to leave the ring");

		}
		Gossiper.instance.addLocalApplicationState(ApplicationState.STATUS, valueFactory.moving(newToken));
		setMode(StorageService.Mode.MOVING, String.format("Moving %s from %s to %s.", localAddress, getLocalTokens().iterator().next(), newToken), true);
		setMode(StorageService.Mode.MOVING, String.format("Sleeping %s ms before start streaming/fetching ranges", StorageService.RING_DELAY), true);
		Uninterruptibles.sleepUninterruptibly(StorageService.RING_DELAY, TimeUnit.MILLISECONDS);
		StorageService.RangeRelocator relocator = new StorageService.RangeRelocator(Collections.singleton(newToken), keyspacesToProcess);
		if (relocator.streamsNeeded()) {
			setMode(StorageService.Mode.MOVING, "fetching new ranges and streaming old ranges", true);
			try {
				relocator.stream().get();
			} catch (ExecutionException | InterruptedException e) {
				throw new RuntimeException(("Interrupted while waiting for stream/fetch ranges to finish: " + (e.getMessage())));
			}
		}else {
			setMode(StorageService.Mode.MOVING, "No ranges to fetch/stream", true);
		}
		setTokens(Collections.singleton(newToken));
		if (StorageService.logger.isDebugEnabled())
			StorageService.logger.debug("Successfully moved to new token {}", getLocalTokens().iterator().next());

	}

	private class RangeRelocator {
		private final StreamPlan streamPlan = new StreamPlan("Relocation");

		private RangeRelocator(Collection<Token> tokens, List<String> keyspaceNames) {
			calculateToFromStreams(tokens, keyspaceNames);
		}

		private void calculateToFromStreams(Collection<Token> newTokens, List<String> keyspaceNames) {
			InetAddress localAddress = FBUtilities.getBroadcastAddress();
			IEndpointSnitch snitch = DatabaseDescriptor.getEndpointSnitch();
			TokenMetadata tokenMetaCloneAllSettled = tokenMetadata.cloneAfterAllSettled();
			TokenMetadata tokenMetaClone = tokenMetadata.cloneOnlyTokenMap();
			for (String keyspace : keyspaceNames) {
				AbstractReplicationStrategy strategy = Keyspace.open(keyspace).getReplicationStrategy();
				Multimap<InetAddress, Range<Token>> endpointToRanges = strategy.getAddressRanges();
				StorageService.logger.debug("Calculating ranges to stream and request for keyspace {}", keyspace);
				for (Token newToken : newTokens) {
					Collection<Range<Token>> currentRanges = endpointToRanges.get(localAddress);
					Collection<Range<Token>> updatedRanges = strategy.getPendingAddressRanges(tokenMetaClone, newToken, localAddress);
					Multimap<Range<Token>, InetAddress> rangeAddresses = strategy.getRangeAddresses(tokenMetaClone);
					Pair<Set<Range<Token>>, Set<Range<Token>>> rangesPerKeyspace = calculateStreamAndFetchRanges(currentRanges, updatedRanges);
					Multimap<Range<Token>, InetAddress> rangesToFetchWithPreferredEndpoints = ArrayListMultimap.create();
					for (Range<Token> toFetch : rangesPerKeyspace.right) {
						for (Range<Token> range : rangeAddresses.keySet()) {
							if (range.contains(toFetch)) {
								List<InetAddress> endpoints = null;
								if (StorageService.useStrictConsistency) {
									Set<InetAddress> oldEndpoints = Sets.newHashSet(rangeAddresses.get(range));
									Set<InetAddress> newEndpoints = Sets.newHashSet(strategy.calculateNaturalEndpoints(toFetch.right, tokenMetaCloneAllSettled));
									if ((oldEndpoints.size()) == (strategy.getReplicationFactor())) {
										oldEndpoints.removeAll(newEndpoints);
										if (oldEndpoints.isEmpty())
											continue;

										assert (oldEndpoints.size()) == 1 : "Expected 1 endpoint but found " + (oldEndpoints.size());
									}
									endpoints = Lists.newArrayList(oldEndpoints.iterator().next());
								}else {
									endpoints = snitch.getSortedListByProximity(localAddress, rangeAddresses.get(range));
								}
								rangesToFetchWithPreferredEndpoints.putAll(toFetch, endpoints);
							}
						}
						Collection<InetAddress> addressList = rangesToFetchWithPreferredEndpoints.get(toFetch);
						if ((addressList == null) || (addressList.isEmpty()))
							continue;

						if (StorageService.useStrictConsistency) {
							if ((addressList.size()) > 1)
								throw new IllegalStateException(("Multiple strict sources found for " + toFetch));

							InetAddress sourceIp = addressList.iterator().next();
							if ((Gossiper.instance.isEnabled()) && (!(Gossiper.instance.getEndpointStateForEndpoint(sourceIp).isAlive())))
								throw new RuntimeException((("A node required to move the data consistently is down (" + sourceIp) + ").  If you wish to move the data from a potentially inconsistent replica, restart the node with -Dcassandra.consistent.rangemovement=false"));

						}
					}
					Multimap<InetAddress, Range<Token>> endpointRanges = HashMultimap.create();
					for (Range<Token> toStream : rangesPerKeyspace.left) {
						Set<InetAddress> currentEndpoints = ImmutableSet.copyOf(strategy.calculateNaturalEndpoints(toStream.right, tokenMetaClone));
						Set<InetAddress> newEndpoints = ImmutableSet.copyOf(strategy.calculateNaturalEndpoints(toStream.right, tokenMetaCloneAllSettled));
						StorageService.logger.debug("Range: {} Current endpoints: {} New endpoints: {}", toStream, currentEndpoints, newEndpoints);
						for (InetAddress address : Sets.difference(newEndpoints, currentEndpoints)) {
							StorageService.logger.debug("Range {} has new owner {}", toStream, address);
							endpointRanges.put(address, toStream);
						}
					}
					for (InetAddress address : endpointRanges.keySet()) {
						StorageService.logger.debug("Will stream range {} of keyspace {} to endpoint {}", endpointRanges.get(address), keyspace, address);
						InetAddress preferred = SystemKeyspace.getPreferredIP(address);
						streamPlan.transferRanges(address, preferred, keyspace, endpointRanges.get(address));
					}
					Multimap<InetAddress, Range<Token>> workMap = RangeStreamer.getWorkMap(rangesToFetchWithPreferredEndpoints, keyspace, FailureDetector.instance, StorageService.useStrictConsistency);
					for (InetAddress address : workMap.keySet()) {
						StorageService.logger.debug("Will request range {} of keyspace {} from endpoint {}", workMap.get(address), keyspace, address);
						InetAddress preferred = SystemKeyspace.getPreferredIP(address);
						streamPlan.requestRanges(address, preferred, keyspace, workMap.get(address));
					}
					StorageService.logger.debug("Keyspace {}: work map {}.", keyspace, workMap);
				}
			}
		}

		public Future<StreamState> stream() {
			return streamPlan.execute();
		}

		public boolean streamsNeeded() {
			return !(streamPlan.isEmpty());
		}
	}

	public String getRemovalStatus() {
		if ((removingNode) == null) {
			return "No token removals in process.";
		}
		return String.format("Removing token (%s). Waiting for replication confirmation from [%s].", tokenMetadata.getToken(removingNode), StringUtils.join(replicatingNodes, ","));
	}

	public void forceRemoveCompletion() {
		if ((!(replicatingNodes.isEmpty())) || (!(tokenMetadata.getLeavingEndpoints().isEmpty()))) {
			StorageService.logger.warn("Removal not confirmed for for {}", StringUtils.join(this.replicatingNodes, ","));
			for (InetAddress endpoint : tokenMetadata.getLeavingEndpoints()) {
				UUID hostId = tokenMetadata.getHostId(endpoint);
				Gossiper.instance.advertiseTokenRemoved(endpoint, hostId);
				excise(tokenMetadata.getTokens(endpoint), endpoint);
			}
			replicatingNodes.clear();
			removingNode = null;
		}else {
			StorageService.logger.warn("No nodes to force removal on, call 'removenode' first");
		}
	}

	public void removeNode(String hostIdString) {
		InetAddress myAddress = FBUtilities.getBroadcastAddress();
		UUID localHostId = tokenMetadata.getHostId(myAddress);
		UUID hostId = UUID.fromString(hostIdString);
		InetAddress endpoint = tokenMetadata.getEndpointForHostId(hostId);
		if (endpoint == null)
			throw new UnsupportedOperationException("Host ID not found.");

		if (!(tokenMetadata.isMember(endpoint)))
			throw new UnsupportedOperationException("Node to be removed is not a member of the token ring");

		if (endpoint.equals(myAddress))
			throw new UnsupportedOperationException("Cannot remove self");

		if (Gossiper.instance.getLiveMembers().contains(endpoint))
			throw new UnsupportedOperationException((("Node " + endpoint) + " is alive and owns this ID. Use decommission command to remove it from the ring"));

		if (tokenMetadata.isLeaving(endpoint))
			StorageService.logger.warn("Node {} is already being removed, continuing removal anyway", endpoint);

		if (!(replicatingNodes.isEmpty()))
			throw new UnsupportedOperationException("This node is already processing a removal. Wait for it to complete, or use 'removenode force' if this has failed.");

		Collection<Token> tokens = tokenMetadata.getTokens(endpoint);
		for (String keyspaceName : Schema.instance.getNonLocalStrategyKeyspaces()) {
			if ((Keyspace.open(keyspaceName).getReplicationStrategy().getReplicationFactor()) == 1)
				continue;

			Multimap<Range<Token>, InetAddress> changedRanges = getChangedRangesForLeaving(keyspaceName, endpoint);
			IFailureDetector failureDetector = FailureDetector.instance;
			for (InetAddress ep : changedRanges.values()) {
				if (failureDetector.isAlive(ep))
					replicatingNodes.add(ep);
				else
					StorageService.logger.warn("Endpoint {} is down and will not receive data for re-replication of {}", ep, endpoint);

			}
		}
		removingNode = endpoint;
		tokenMetadata.addLeavingEndpoint(endpoint);
		PendingRangeCalculatorService.instance.update();
		Gossiper.instance.advertiseRemoving(endpoint, hostId, localHostId);
		restoreReplicaCount(endpoint, myAddress);
		while (!(replicatingNodes.isEmpty())) {
			Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
		} 
		excise(tokens, endpoint);
		Gossiper.instance.advertiseTokenRemoved(endpoint, hostId);
		replicatingNodes.clear();
		removingNode = null;
	}

	public void confirmReplication(InetAddress node) {
		if (!(replicatingNodes.isEmpty())) {
			replicatingNodes.remove(node);
		}else {
			StorageService.logger.info("Received unexpected REPLICATION_FINISHED message from {}. Was this node recently a removal coordinator?", node);
		}
	}

	public String getOperationMode() {
		return operationMode.toString();
	}

	public boolean isStarting() {
		return (operationMode) == (StorageService.Mode.STARTING);
	}

	public boolean isMoving() {
		return (operationMode) == (StorageService.Mode.MOVING);
	}

	public boolean isJoining() {
		return (operationMode) == (StorageService.Mode.JOINING);
	}

	public boolean isDrained() {
		return (operationMode) == (StorageService.Mode.DRAINED);
	}

	public boolean isDraining() {
		return (operationMode) == (StorageService.Mode.DRAINING);
	}

	public String getDrainProgress() {
		return String.format("Drained %s/%s ColumnFamilies", remainingCFs, totalCFs);
	}

	public synchronized void drain() throws IOException, InterruptedException, ExecutionException {
		drain(false);
	}

	protected synchronized void drain(boolean isFinalShutdown) throws IOException, InterruptedException, ExecutionException {
		ExecutorService counterMutationStage = StageManager.getStage(Stage.COUNTER_MUTATION);
		ExecutorService viewMutationStage = StageManager.getStage(Stage.VIEW_MUTATION);
		ExecutorService mutationStage = StageManager.getStage(Stage.MUTATION);
		if (((mutationStage.isTerminated()) && (counterMutationStage.isTerminated())) && (viewMutationStage.isTerminated())) {
			if (!isFinalShutdown)
				StorageService.logger.warn("Cannot drain node (did it already happen?)");

			return;
		}
		assert !(isShutdown);
		isShutdown = true;
		Throwable preShutdownHookThrowable = Throwables.perform(null, preShutdownHooks.stream().map(( h) -> h::run));
		if (preShutdownHookThrowable != null)
			StorageService.logger.error("Attempting to continue draining after pre-shutdown hooks returned exception", preShutdownHookThrowable);

		try {
			setMode(StorageService.Mode.DRAINING, "starting drain process", (!isFinalShutdown));
			BatchlogManager.instance.shutdown();
			HintsService.instance.pauseDispatch();
			if ((daemon) != null)
				shutdownClientServers();

			ScheduledExecutors.optionalTasks.shutdown();
			Gossiper.instance.stop();
			if (!isFinalShutdown)
				setMode(StorageService.Mode.DRAINING, "shutting down MessageService", false);

			MessagingService.instance().shutdown();
			if (!isFinalShutdown)
				setMode(StorageService.Mode.DRAINING, "clearing mutation stage", false);

			viewMutationStage.shutdown();
			counterMutationStage.shutdown();
			mutationStage.shutdown();
			viewMutationStage.awaitTermination(3600, TimeUnit.SECONDS);
			counterMutationStage.awaitTermination(3600, TimeUnit.SECONDS);
			mutationStage.awaitTermination(3600, TimeUnit.SECONDS);
			StorageProxy.instance.verifyNoHintsInProgress();
			if (!isFinalShutdown)
				setMode(StorageService.Mode.DRAINING, "flushing column families", false);

			for (Keyspace keyspace : Keyspace.all())
				for (ColumnFamilyStore cfs : keyspace.getColumnFamilyStores())
					cfs.disableAutoCompaction();


			totalCFs = 0;
			for (Keyspace keyspace : Keyspace.nonSystem())
				totalCFs += keyspace.getColumnFamilyStores().size();

			remainingCFs = totalCFs;
			List<Future<?>> flushes = new ArrayList<>();
			for (Keyspace keyspace : Keyspace.nonSystem()) {
				for (ColumnFamilyStore cfs : keyspace.getColumnFamilyStores())
					flushes.add(cfs.forceFlush());

			}
			for (Future f : flushes) {
				try {
					FBUtilities.waitOnFuture(f);
				} catch (Throwable t) {
					JVMStabilityInspector.inspectThrowable(t);
					StorageService.logger.warn("Caught exception while waiting for memtable flushes during shutdown hook", t);
				}
				(remainingCFs)--;
			}
			CompactionManager.instance.forceShutdown();
			flushes.clear();
			for (Keyspace keyspace : Keyspace.system()) {
				for (ColumnFamilyStore cfs : keyspace.getColumnFamilyStores())
					flushes.add(cfs.forceFlush());

			}
			FBUtilities.waitOnFutures(flushes);
			HintsService.instance.shutdownBlocking();
			CompactionManager.instance.forceShutdown();
			CommitLog.instance.forceRecycleAllSegments();
			CommitLog.instance.shutdownBlocking();
			ScheduledExecutors.nonPeriodicTasks.shutdown();
			if (!(ScheduledExecutors.nonPeriodicTasks.awaitTermination(1, TimeUnit.MINUTES)))
				StorageService.logger.warn("Failed to wait for non periodic tasks to shutdown");

			ColumnFamilyStore.shutdownPostFlushExecutor();
			setMode(StorageService.Mode.DRAINED, (!isFinalShutdown));
		} catch (Throwable t) {
			StorageService.logger.error("Caught an exception while draining ", t);
		} finally {
			Throwable postShutdownHookThrowable = Throwables.perform(null, postShutdownHooks.stream().map(( h) -> h::run));
			if (postShutdownHookThrowable != null)
				StorageService.logger.error("Post-shutdown hooks returned exception", postShutdownHookThrowable);

		}
	}

	public synchronized boolean addPreShutdownHook(Runnable hook) {
		if ((!(isDraining())) && (!(isDrained())))
			return preShutdownHooks.add(hook);

		return false;
	}

	public synchronized boolean removePreShutdownHook(Runnable hook) {
		return preShutdownHooks.remove(hook);
	}

	public synchronized boolean addPostShutdownHook(Runnable hook) {
		if ((!(isDraining())) && (!(isDrained())))
			return postShutdownHooks.add(hook);

		return false;
	}

	public synchronized boolean removePostShutdownHook(Runnable hook) {
		return postShutdownHooks.remove(hook);
	}

	synchronized void checkServiceAllowedToStart(String service) {
		if (isDraining())
			throw new IllegalStateException(String.format("Unable to start %s because the node is draining.", service));

		if (isShutdown())
			throw new IllegalStateException(String.format("Unable to start %s because the node was drained.", service));

	}

	@com.google.common.annotations.VisibleForTesting
	public IPartitioner setPartitionerUnsafe(IPartitioner newPartitioner) {
		IPartitioner oldPartitioner = DatabaseDescriptor.setPartitionerUnsafe(newPartitioner);
		tokenMetadata = tokenMetadata.cloneWithNewPartitioner(newPartitioner);
		valueFactory = new VersionedValue.VersionedValueFactory(newPartitioner);
		return oldPartitioner;
	}

	TokenMetadata setTokenMetadataUnsafe(TokenMetadata tmd) {
		TokenMetadata old = tokenMetadata;
		tokenMetadata = tmd;
		return old;
	}

	public void truncate(String keyspace, String table) throws IOException, TimeoutException {
		try {
			StorageProxy.truncateBlocking(keyspace, table);
		} catch (UnavailableException e) {
			throw new IOException(e.getMessage());
		}
	}

	public Map<InetAddress, Float> getOwnership() {
		List<Token> sortedTokens = tokenMetadata.sortedTokens();
		Map<Token, Float> tokenMap = new TreeMap<Token, Float>(tokenMetadata.partitioner.describeOwnership(sortedTokens));
		Map<InetAddress, Float> nodeMap = new LinkedHashMap<>();
		for (Map.Entry<Token, Float> entry : tokenMap.entrySet()) {
			InetAddress endpoint = tokenMetadata.getEndpoint(entry.getKey());
			Float tokenOwnership = entry.getValue();
			if (nodeMap.containsKey(endpoint))
				nodeMap.put(endpoint, ((nodeMap.get(endpoint)) + tokenOwnership));
			else
				nodeMap.put(endpoint, tokenOwnership);

		}
		return nodeMap;
	}

	public LinkedHashMap<InetAddress, Float> effectiveOwnership(String keyspace) throws IllegalStateException {
		AbstractReplicationStrategy strategy;
		if (keyspace != null) {
			Keyspace keyspaceInstance = Schema.instance.getKeyspaceInstance(keyspace);
			if (keyspaceInstance == null)
				throw new IllegalArgumentException((("The keyspace " + keyspace) + ", does not exist"));

			if ((keyspaceInstance.getReplicationStrategy()) instanceof LocalStrategy)
				throw new IllegalStateException("Ownership values for keyspaces with LocalStrategy are meaningless");

			strategy = keyspaceInstance.getReplicationStrategy();
		}else {
			List<String> userKeyspaces = Schema.instance.getUserKeyspaces();
			if ((userKeyspaces.size()) > 0) {
				keyspace = userKeyspaces.get(0);
				AbstractReplicationStrategy replicationStrategy = Schema.instance.getKeyspaceInstance(keyspace).getReplicationStrategy();
				for (String keyspaceName : userKeyspaces) {
					if (!(Schema.instance.getKeyspaceInstance(keyspaceName).getReplicationStrategy().hasSameSettings(replicationStrategy)))
						throw new IllegalStateException("Non-system keyspaces don't have the same replication settings, effective ownership information is meaningless");

				}
			}else {
				keyspace = "system_traces";
			}
			Keyspace keyspaceInstance = Schema.instance.getKeyspaceInstance(keyspace);
			if (keyspaceInstance == null)
				throw new IllegalArgumentException((("The node does not have " + keyspace) + " yet, probably still bootstrapping"));

			strategy = keyspaceInstance.getReplicationStrategy();
		}
		TokenMetadata metadata = tokenMetadata.cloneOnlyTokenMap();
		Collection<Collection<InetAddress>> endpointsGroupedByDc = new ArrayList<>();
		SortedMap<String, Collection<InetAddress>> sortedDcsToEndpoints = new TreeMap<>();
		sortedDcsToEndpoints.putAll(metadata.getTopology().getDatacenterEndpoints().asMap());
		for (Collection<InetAddress> endpoints : sortedDcsToEndpoints.values())
			endpointsGroupedByDc.add(endpoints);

		Map<Token, Float> tokenOwnership = tokenMetadata.partitioner.describeOwnership(tokenMetadata.sortedTokens());
		LinkedHashMap<InetAddress, Float> finalOwnership = Maps.newLinkedHashMap();
		Multimap<InetAddress, Range<Token>> endpointToRanges = strategy.getAddressRanges();
		for (Collection<InetAddress> endpoints : endpointsGroupedByDc) {
			for (InetAddress endpoint : endpoints) {
				float ownership = 0.0F;
				for (Range<Token> range : endpointToRanges.get(endpoint)) {
					if (tokenOwnership.containsKey(range.right))
						ownership += tokenOwnership.get(range.right);

				}
				finalOwnership.put(endpoint, ownership);
			}
		}
		return finalOwnership;
	}

	public List<String> getKeyspaces() {
		List<String> keyspaceNamesList = new ArrayList<>(Schema.instance.getKeyspaces());
		return Collections.unmodifiableList(keyspaceNamesList);
	}

	public List<String> getNonSystemKeyspaces() {
		return Collections.unmodifiableList(Schema.instance.getNonSystemKeyspaces());
	}

	public List<String> getNonLocalStrategyKeyspaces() {
		return Collections.unmodifiableList(Schema.instance.getNonLocalStrategyKeyspaces());
	}

	public Map<String, String> getViewBuildStatuses(String keyspace, String view) {
		Map<UUID, String> coreViewStatus = SystemDistributedKeyspace.viewStatus(keyspace, view);
		Map<InetAddress, UUID> hostIdToEndpoint = tokenMetadata.getEndpointToHostIdMapForReading();
		Map<String, String> result = new HashMap<>();
		for (Map.Entry<InetAddress, UUID> entry : hostIdToEndpoint.entrySet()) {
			UUID hostId = entry.getValue();
			InetAddress endpoint = entry.getKey();
			result.put(endpoint.toString(), (coreViewStatus.containsKey(hostId) ? coreViewStatus.get(hostId) : "UNKNOWN"));
		}
		return Collections.unmodifiableMap(result);
	}

	public void setDynamicUpdateInterval(int dynamicUpdateInterval) {
		if ((DatabaseDescriptor.getEndpointSnitch()) instanceof DynamicEndpointSnitch) {
			try {
				updateSnitch(null, true, dynamicUpdateInterval, null, null);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public int getDynamicUpdateInterval() {
		return DatabaseDescriptor.getDynamicUpdateInterval();
	}

	public void updateSnitch(String epSnitchClassName, Boolean dynamic, Integer dynamicUpdateInterval, Integer dynamicResetInterval, Double dynamicBadnessThreshold) throws ClassNotFoundException {
		if (dynamicUpdateInterval != null)
			DatabaseDescriptor.setDynamicUpdateInterval(dynamicUpdateInterval);

		if (dynamicResetInterval != null)
			DatabaseDescriptor.setDynamicResetInterval(dynamicResetInterval);

		if (dynamicBadnessThreshold != null)
			DatabaseDescriptor.setDynamicBadnessThreshold(dynamicBadnessThreshold);

		IEndpointSnitch oldSnitch = DatabaseDescriptor.getEndpointSnitch();
		if (epSnitchClassName != null) {
			if (oldSnitch instanceof DynamicEndpointSnitch)
				((DynamicEndpointSnitch) (oldSnitch)).close();

			IEndpointSnitch newSnitch;
			try {
				newSnitch = DatabaseDescriptor.createEndpointSnitch(((dynamic != null) && dynamic), epSnitchClassName);
			} catch (ConfigurationException e) {
				throw new ClassNotFoundException(e.getMessage());
			}
			if (newSnitch instanceof DynamicEndpointSnitch) {
				StorageService.logger.info("Created new dynamic snitch {} with update-interval={}, reset-interval={}, badness-threshold={}", ((DynamicEndpointSnitch) (newSnitch)).subsnitch.getClass().getName(), DatabaseDescriptor.getDynamicUpdateInterval(), DatabaseDescriptor.getDynamicResetInterval(), DatabaseDescriptor.getDynamicBadnessThreshold());
			}else {
				StorageService.logger.info("Created new non-dynamic snitch {}", newSnitch.getClass().getName());
			}
			DatabaseDescriptor.setEndpointSnitch(newSnitch);
			for (String ks : Schema.instance.getKeyspaces()) {
				Keyspace.open(ks).getReplicationStrategy().snitch = newSnitch;
			}
		}else {
			if (oldSnitch instanceof DynamicEndpointSnitch) {
				StorageService.logger.info("Applying config change to dynamic snitch {} with update-interval={}, reset-interval={}, badness-threshold={}", ((DynamicEndpointSnitch) (oldSnitch)).subsnitch.getClass().getName(), DatabaseDescriptor.getDynamicUpdateInterval(), DatabaseDescriptor.getDynamicResetInterval(), DatabaseDescriptor.getDynamicBadnessThreshold());
				DynamicEndpointSnitch snitch = ((DynamicEndpointSnitch) (oldSnitch));
				snitch.applyConfigChanges();
			}
		}
		updateTopology();
	}

	private Future<StreamState> streamRanges(Map<String, Multimap<Range<Token>, InetAddress>> rangesToStreamByKeyspace) {
		Map<String, Map<InetAddress, List<Range<Token>>>> sessionsToStreamByKeyspace = new HashMap<>();
		for (Map.Entry<String, Multimap<Range<Token>, InetAddress>> entry : rangesToStreamByKeyspace.entrySet()) {
			String keyspace = entry.getKey();
			Multimap<Range<Token>, InetAddress> rangesWithEndpoints = entry.getValue();
			if (rangesWithEndpoints.isEmpty())
				continue;

			Map<InetAddress, Set<Range<Token>>> transferredRangePerKeyspace = SystemKeyspace.getTransferredRanges("Unbootstrap", keyspace, StorageService.instance.getTokenMetadata().partitioner);
			Map<InetAddress, List<Range<Token>>> rangesPerEndpoint = new HashMap<>();
			for (Map.Entry<Range<Token>, InetAddress> endPointEntry : rangesWithEndpoints.entries()) {
				Range<Token> range = endPointEntry.getKey();
				InetAddress endpoint = endPointEntry.getValue();
				Set<Range<Token>> transferredRanges = transferredRangePerKeyspace.get(endpoint);
				if ((transferredRanges != null) && (transferredRanges.contains(range))) {
					StorageService.logger.debug("Skipping transferred range {} of keyspace {}, endpoint {}", range, keyspace, endpoint);
					continue;
				}
				List<Range<Token>> curRanges = rangesPerEndpoint.get(endpoint);
				if (curRanges == null) {
					curRanges = new LinkedList<>();
					rangesPerEndpoint.put(endpoint, curRanges);
				}
				curRanges.add(range);
			}
			sessionsToStreamByKeyspace.put(keyspace, rangesPerEndpoint);
		}
		StreamPlan streamPlan = new StreamPlan("Unbootstrap");
		streamPlan.listeners(streamStateStore);
		for (Map.Entry<String, Map<InetAddress, List<Range<Token>>>> entry : sessionsToStreamByKeyspace.entrySet()) {
			String keyspaceName = entry.getKey();
			Map<InetAddress, List<Range<Token>>> rangesPerEndpoint = entry.getValue();
			for (Map.Entry<InetAddress, List<Range<Token>>> rangesEntry : rangesPerEndpoint.entrySet()) {
				List<Range<Token>> ranges = rangesEntry.getValue();
				InetAddress newEndpoint = rangesEntry.getKey();
				InetAddress preferred = SystemKeyspace.getPreferredIP(newEndpoint);
				streamPlan.transferRanges(newEndpoint, preferred, keyspaceName, ranges);
			}
		}
		return streamPlan.execute();
	}

	public Pair<Set<Range<Token>>, Set<Range<Token>>> calculateStreamAndFetchRanges(Collection<Range<Token>> current, Collection<Range<Token>> updated) {
		Set<Range<Token>> toStream = new HashSet<>();
		Set<Range<Token>> toFetch = new HashSet<>();
		for (Range<Token> r1 : current) {
			boolean intersect = false;
			for (Range<Token> r2 : updated) {
				if (r1.intersects(r2)) {
					toStream.addAll(r1.subtract(r2));
					intersect = true;
				}
			}
			if (!intersect) {
				toStream.add(r1);
			}
		}
		for (Range<Token> r2 : updated) {
			boolean intersect = false;
			for (Range<Token> r1 : current) {
				if (r2.intersects(r1)) {
					toFetch.addAll(r2.subtract(r1));
					intersect = true;
				}
			}
			if (!intersect) {
				toFetch.add(r2);
			}
		}
		return Pair.create(toStream, toFetch);
	}

	public void bulkLoad(String directory) {
		try {
			bulkLoadInternal(directory).get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String bulkLoadAsync(String directory) {
		return bulkLoadInternal(directory).planId.toString();
	}

	private StreamResultFuture bulkLoadInternal(String directory) {
		File dir = new File(directory);
		if ((!(dir.exists())) || (!(dir.isDirectory())))
			throw new IllegalArgumentException(("Invalid directory " + directory));

		SSTableLoader.Client client = new SSTableLoader.Client() {
			private String keyspace;

			public void init(String keyspace) {
				this.keyspace = keyspace;
				try {
					for (Map.Entry<Range<Token>, List<InetAddress>> entry : StorageService.instance.getRangeToAddressMap(keyspace).entrySet()) {
						Range<Token> range = entry.getKey();
						for (InetAddress endpoint : entry.getValue())
							addRangeForEndpoint(range, endpoint);

					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			public CFMetaData getTableMetadata(String tableName) {
				return Schema.instance.getCFMetaData(keyspace, tableName);
			}
		};
		return new SSTableLoader(dir, client, new OutputHandler.LogOutput()).stream();
	}

	public void rescheduleFailedDeletions() {
		LifecycleTransaction.rescheduleFailedDeletions();
	}

	public void loadNewSSTables(String ksName, String cfName) {
		ColumnFamilyStore.loadNewSSTables(ksName, cfName);
	}

	public List<String> sampleKeyRange() {
		List<DecoratedKey> keys = new ArrayList<>();
		for (Keyspace keyspace : Keyspace.nonLocalStrategy()) {
			for (Range<Token> range : getPrimaryRangesForEndpoint(keyspace.getName(), FBUtilities.getBroadcastAddress()))
				keys.addAll(keySamples(keyspace.getColumnFamilyStores(), range));

		}
		List<String> sampledKeys = new ArrayList<>(keys.size());
		for (DecoratedKey key : keys)
			sampledKeys.add(key.getToken().toString());

		return sampledKeys;
	}

	public void rebuildSecondaryIndex(String ksName, String cfName, String... idxNames) {
		String[] indices = Arrays.asList(idxNames).stream().map(( p) -> SecondaryIndexManager.isIndexColumnFamily(p) ? SecondaryIndexManager.getIndexName(p) : p).collect(Collectors.toList()).toArray(new String[idxNames.length]);
		ColumnFamilyStore.rebuildSecondaryIndex(ksName, cfName, indices);
	}

	public void resetLocalSchema() throws IOException {
		MigrationManager.resetLocalSchema();
	}

	public void reloadLocalSchema() {
		SchemaKeyspace.reloadSchemaAndAnnounceVersion();
	}

	public void setTraceProbability(double probability) {
		this.traceProbability = probability;
	}

	public double getTraceProbability() {
		return traceProbability;
	}

	public void disableAutoCompaction(String ks, String... tables) throws IOException {
		for (ColumnFamilyStore cfs : getValidColumnFamilies(true, true, ks, tables)) {
			cfs.disableAutoCompaction();
		}
	}

	public synchronized void enableAutoCompaction(String ks, String... tables) throws IOException {
		checkServiceAllowedToStart("auto compaction");
		for (ColumnFamilyStore cfs : getValidColumnFamilies(true, true, ks, tables)) {
			cfs.enableAutoCompaction();
		}
	}

	public String getClusterName() {
		return DatabaseDescriptor.getClusterName();
	}

	public String getPartitionerName() {
		return DatabaseDescriptor.getPartitionerName();
	}

	public int getTombstoneWarnThreshold() {
		return DatabaseDescriptor.getTombstoneWarnThreshold();
	}

	public void setTombstoneWarnThreshold(int threshold) {
		DatabaseDescriptor.setTombstoneWarnThreshold(threshold);
	}

	public int getTombstoneFailureThreshold() {
		return DatabaseDescriptor.getTombstoneFailureThreshold();
	}

	public void setTombstoneFailureThreshold(int threshold) {
		DatabaseDescriptor.setTombstoneFailureThreshold(threshold);
	}

	public int getBatchSizeFailureThreshold() {
		return DatabaseDescriptor.getBatchSizeFailThresholdInKB();
	}

	public void setBatchSizeFailureThreshold(int threshold) {
		DatabaseDescriptor.setBatchSizeFailThresholdInKB(threshold);
	}

	public void setHintedHandoffThrottleInKB(int throttleInKB) {
		DatabaseDescriptor.setHintedHandoffThrottleInKB(throttleInKB);
		StorageService.logger.info("Updated hinted_handoff_throttle_in_kb to {}", throttleInKB);
	}
}

