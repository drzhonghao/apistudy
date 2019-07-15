

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import org.apache.cassandra.auth.IInternodeAuthenticator;
import org.apache.cassandra.batchlog.Batch;
import org.apache.cassandra.concurrent.DebuggableScheduledThreadPoolExecutor;
import org.apache.cassandra.concurrent.ExecutorLocals;
import org.apache.cassandra.concurrent.LocalAwareExecutorService;
import org.apache.cassandra.concurrent.ScheduledExecutors;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.EncryptionOptions;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.CounterMutation;
import org.apache.cassandra.db.IMutation;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadResponse;
import org.apache.cassandra.db.SnapshotCommand;
import org.apache.cassandra.db.TruncateResponse;
import org.apache.cassandra.db.Truncation;
import org.apache.cassandra.db.WriteResponse;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.dht.BootStrapper;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.RingPosition;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.gms.EchoMessage;
import org.apache.cassandra.gms.GossipDigestAck;
import org.apache.cassandra.gms.GossipDigestAck2;
import org.apache.cassandra.gms.GossipDigestSyn;
import org.apache.cassandra.hints.HintMessage;
import org.apache.cassandra.hints.HintResponse;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.locator.ILatencySubscriber;
import org.apache.cassandra.locator.TokenMetadata;
import org.apache.cassandra.metrics.CassandraMetricsRegistry;
import org.apache.cassandra.metrics.ConnectionMetrics;
import org.apache.cassandra.metrics.DroppedMessageMetrics;
import org.apache.cassandra.metrics.MessagingMetrics;
import org.apache.cassandra.metrics.MetricNameFactory;
import org.apache.cassandra.metrics.TableMetrics;
import org.apache.cassandra.net.AsyncOneResponse;
import org.apache.cassandra.net.BackPressureState;
import org.apache.cassandra.net.BackPressureStrategy;
import org.apache.cassandra.net.CallbackInfo;
import org.apache.cassandra.net.IAsyncCallback;
import org.apache.cassandra.net.IAsyncCallbackWithFailure;
import org.apache.cassandra.net.IMessageSink;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.IncomingStreamingConnection;
import org.apache.cassandra.net.IncomingTcpConnection;
import org.apache.cassandra.net.MessageDeliveryTask;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingServiceMBean;
import org.apache.cassandra.net.OutboundTcpConnection;
import org.apache.cassandra.net.OutboundTcpConnectionPool;
import org.apache.cassandra.net.WriteCallbackInfo;
import org.apache.cassandra.repair.messages.RepairMessage;
import org.apache.cassandra.security.SSLFactory;
import org.apache.cassandra.service.AbstractWriteResponseHandler;
import org.apache.cassandra.service.MigrationManager;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.service.paxos.Commit;
import org.apache.cassandra.service.paxos.PrepareResponse;
import org.apache.cassandra.tracing.TraceState;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.BooleanSerializer;
import org.apache.cassandra.utils.ExpiringMap;
import org.apache.cassandra.utils.ExpiringMap.CacheableObject;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.StatusLogger;
import org.apache.cassandra.utils.UUIDSerializer;
import org.apache.cassandra.utils.concurrent.SimpleCondition;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.config.EncryptionOptions.ServerEncryptionOptions.InternodeEncryption.all;
import static org.apache.cassandra.config.EncryptionOptions.ServerEncryptionOptions.InternodeEncryption.none;
import static org.apache.cassandra.dht.BootStrapper.StringSerializer.instance;


public final class MessagingService implements MessagingServiceMBean {
	public static final boolean FORCE_3_0_PROTOCOL_VERSION = Boolean.getBoolean("cassandra.force_3_0_protocol_version");

	public static final String MBEAN_NAME = "org.apache.cassandra.net:type=MessagingService";

	public static final int VERSION_12 = 6;

	public static final int VERSION_20 = 7;

	public static final int VERSION_21 = 8;

	public static final int VERSION_22 = 9;

	public static final int VERSION_30 = 10;

	public static final int VERSION_3014 = 11;

	public static final int current_version = (MessagingService.FORCE_3_0_PROTOCOL_VERSION) ? MessagingService.VERSION_30 : MessagingService.VERSION_3014;

	public static final String FAILURE_CALLBACK_PARAM = "CAL_BAC";

	public static final byte[] ONE_BYTE = new byte[1];

	public static final String FAILURE_RESPONSE_PARAM = "FAIL";

	public static final String FAILURE_REASON_PARAM = "FAIL_REASON";

	public static final int PROTOCOL_MAGIC = -900387334;

	private boolean allNodesAtLeast22 = true;

	private boolean allNodesAtLeast30 = true;

	public final MessagingMetrics metrics = new MessagingMetrics();

	public enum Verb {

		MUTATION() {
			public long getTimeout() {
				return DatabaseDescriptor.getWriteRpcTimeout();
			}
		},
		HINT() {
			public long getTimeout() {
				return DatabaseDescriptor.getWriteRpcTimeout();
			}
		},
		READ_REPAIR() {
			public long getTimeout() {
				return DatabaseDescriptor.getWriteRpcTimeout();
			}
		},
		READ() {
			public long getTimeout() {
				return DatabaseDescriptor.getReadRpcTimeout();
			}
		},
		REQUEST_RESPONSE,
		BATCH_STORE() {
			public long getTimeout() {
				return DatabaseDescriptor.getWriteRpcTimeout();
			}
		},
		BATCH_REMOVE() {
			public long getTimeout() {
				return DatabaseDescriptor.getWriteRpcTimeout();
			}
		},
		@Deprecated
		STREAM_REPLY,
		@Deprecated
		STREAM_REQUEST,
		RANGE_SLICE() {
			public long getTimeout() {
				return DatabaseDescriptor.getRangeRpcTimeout();
			}
		},
		@Deprecated
		BOOTSTRAP_TOKEN,
		@Deprecated
		TREE_REQUEST,
		@Deprecated
		TREE_RESPONSE,
		@Deprecated
		JOIN,
		GOSSIP_DIGEST_SYN,
		GOSSIP_DIGEST_ACK,
		GOSSIP_DIGEST_ACK2,
		@Deprecated
		DEFINITIONS_ANNOUNCE,
		DEFINITIONS_UPDATE,
		TRUNCATE() {
			public long getTimeout() {
				return DatabaseDescriptor.getTruncateRpcTimeout();
			}
		},
		SCHEMA_CHECK,
		@Deprecated
		INDEX_SCAN,
		REPLICATION_FINISHED,
		INTERNAL_RESPONSE,
		COUNTER_MUTATION() {
			public long getTimeout() {
				return DatabaseDescriptor.getCounterWriteRpcTimeout();
			}
		},
		@Deprecated
		STREAMING_REPAIR_REQUEST,
		@Deprecated
		STREAMING_REPAIR_RESPONSE,
		SNAPSHOT,
		MIGRATION_REQUEST,
		GOSSIP_SHUTDOWN,
		_TRACE,
		ECHO,
		REPAIR_MESSAGE,
		PAXOS_PREPARE() {
			public long getTimeout() {
				return DatabaseDescriptor.getWriteRpcTimeout();
			}
		},
		PAXOS_PROPOSE() {
			public long getTimeout() {
				return DatabaseDescriptor.getWriteRpcTimeout();
			}
		},
		PAXOS_COMMIT() {
			public long getTimeout() {
				return DatabaseDescriptor.getWriteRpcTimeout();
			}
		},
		@Deprecated
		PAGED_RANGE() {
			public long getTimeout() {
				return DatabaseDescriptor.getRangeRpcTimeout();
			}
		},
		UNUSED_1,
		UNUSED_2,
		UNUSED_3,
		UNUSED_4,
		UNUSED_5;
		public static MessagingService.Verb convertForMessagingServiceVersion(MessagingService.Verb verb, int version) {
			if ((verb == (MessagingService.Verb.PAGED_RANGE)) && (version >= (MessagingService.VERSION_30)))
				return MessagingService.Verb.RANGE_SLICE;

			return verb;
		}

		public long getTimeout() {
			return DatabaseDescriptor.getRpcTimeout();
		}
	}

	public static final MessagingService.Verb[] verbValues = MessagingService.Verb.values();

	public static final EnumMap<MessagingService.Verb, Stage> verbStages = new EnumMap<MessagingService.Verb, Stage>(MessagingService.Verb.class) {
		{
			put(MessagingService.Verb.MUTATION, Stage.MUTATION);
			put(MessagingService.Verb.COUNTER_MUTATION, Stage.COUNTER_MUTATION);
			put(MessagingService.Verb.READ_REPAIR, Stage.MUTATION);
			put(MessagingService.Verb.HINT, Stage.MUTATION);
			put(MessagingService.Verb.TRUNCATE, Stage.MUTATION);
			put(MessagingService.Verb.PAXOS_PREPARE, Stage.MUTATION);
			put(MessagingService.Verb.PAXOS_PROPOSE, Stage.MUTATION);
			put(MessagingService.Verb.PAXOS_COMMIT, Stage.MUTATION);
			put(MessagingService.Verb.BATCH_STORE, Stage.MUTATION);
			put(MessagingService.Verb.BATCH_REMOVE, Stage.MUTATION);
			put(MessagingService.Verb.READ, Stage.READ);
			put(MessagingService.Verb.RANGE_SLICE, Stage.READ);
			put(MessagingService.Verb.INDEX_SCAN, Stage.READ);
			put(MessagingService.Verb.PAGED_RANGE, Stage.READ);
			put(MessagingService.Verb.REQUEST_RESPONSE, Stage.REQUEST_RESPONSE);
			put(MessagingService.Verb.INTERNAL_RESPONSE, Stage.INTERNAL_RESPONSE);
			put(MessagingService.Verb.STREAM_REPLY, Stage.MISC);
			put(MessagingService.Verb.STREAM_REQUEST, Stage.MISC);
			put(MessagingService.Verb.REPLICATION_FINISHED, Stage.MISC);
			put(MessagingService.Verb.SNAPSHOT, Stage.MISC);
			put(MessagingService.Verb.TREE_REQUEST, Stage.ANTI_ENTROPY);
			put(MessagingService.Verb.TREE_RESPONSE, Stage.ANTI_ENTROPY);
			put(MessagingService.Verb.STREAMING_REPAIR_REQUEST, Stage.ANTI_ENTROPY);
			put(MessagingService.Verb.STREAMING_REPAIR_RESPONSE, Stage.ANTI_ENTROPY);
			put(MessagingService.Verb.REPAIR_MESSAGE, Stage.ANTI_ENTROPY);
			put(MessagingService.Verb.GOSSIP_DIGEST_ACK, Stage.GOSSIP);
			put(MessagingService.Verb.GOSSIP_DIGEST_ACK2, Stage.GOSSIP);
			put(MessagingService.Verb.GOSSIP_DIGEST_SYN, Stage.GOSSIP);
			put(MessagingService.Verb.GOSSIP_SHUTDOWN, Stage.GOSSIP);
			put(MessagingService.Verb.DEFINITIONS_UPDATE, Stage.MIGRATION);
			put(MessagingService.Verb.SCHEMA_CHECK, Stage.MIGRATION);
			put(MessagingService.Verb.MIGRATION_REQUEST, Stage.MIGRATION);
			put(MessagingService.Verb.INDEX_SCAN, Stage.READ);
			put(MessagingService.Verb.REPLICATION_FINISHED, Stage.MISC);
			put(MessagingService.Verb.SNAPSHOT, Stage.MISC);
			put(MessagingService.Verb.ECHO, Stage.GOSSIP);
			put(MessagingService.Verb.UNUSED_1, Stage.INTERNAL_RESPONSE);
			put(MessagingService.Verb.UNUSED_2, Stage.INTERNAL_RESPONSE);
			put(MessagingService.Verb.UNUSED_3, Stage.INTERNAL_RESPONSE);
		}
	};

	public final EnumMap<MessagingService.Verb, IVersionedSerializer<?>> verbSerializers = new EnumMap<MessagingService.Verb, IVersionedSerializer<?>>(MessagingService.Verb.class) {
		{
			put(MessagingService.Verb.REQUEST_RESPONSE, MessagingService.CallbackDeterminedSerializer.instance);
			put(MessagingService.Verb.INTERNAL_RESPONSE, MessagingService.CallbackDeterminedSerializer.instance);
			put(MessagingService.Verb.MUTATION, Mutation.serializer);
			put(MessagingService.Verb.READ_REPAIR, Mutation.serializer);
			put(MessagingService.Verb.READ, ReadCommand.readSerializer);
			put(MessagingService.Verb.RANGE_SLICE, ReadCommand.rangeSliceSerializer);
			put(MessagingService.Verb.PAGED_RANGE, ReadCommand.pagedRangeSerializer);
			put(MessagingService.Verb.BOOTSTRAP_TOKEN, instance);
			put(MessagingService.Verb.REPAIR_MESSAGE, RepairMessage.serializer);
			put(MessagingService.Verb.GOSSIP_DIGEST_ACK, GossipDigestAck.serializer);
			put(MessagingService.Verb.GOSSIP_DIGEST_ACK2, GossipDigestAck2.serializer);
			put(MessagingService.Verb.GOSSIP_DIGEST_SYN, GossipDigestSyn.serializer);
			put(MessagingService.Verb.DEFINITIONS_UPDATE, MigrationManager.MigrationsSerializer.instance);
			put(MessagingService.Verb.TRUNCATE, Truncation.serializer);
			put(MessagingService.Verb.REPLICATION_FINISHED, null);
			put(MessagingService.Verb.COUNTER_MUTATION, CounterMutation.serializer);
			put(MessagingService.Verb.SNAPSHOT, SnapshotCommand.serializer);
			put(MessagingService.Verb.ECHO, EchoMessage.serializer);
			put(MessagingService.Verb.PAXOS_PREPARE, Commit.serializer);
			put(MessagingService.Verb.PAXOS_PROPOSE, Commit.serializer);
			put(MessagingService.Verb.PAXOS_COMMIT, Commit.serializer);
			put(MessagingService.Verb.HINT, HintMessage.serializer);
			put(MessagingService.Verb.BATCH_STORE, Batch.serializer);
			put(MessagingService.Verb.BATCH_REMOVE, UUIDSerializer.serializer);
		}
	};

	public static final EnumMap<MessagingService.Verb, IVersionedSerializer<?>> callbackDeserializers = new EnumMap<MessagingService.Verb, IVersionedSerializer<?>>(MessagingService.Verb.class) {
		{
			put(MessagingService.Verb.MUTATION, WriteResponse.serializer);
			put(MessagingService.Verb.HINT, HintResponse.serializer);
			put(MessagingService.Verb.READ_REPAIR, WriteResponse.serializer);
			put(MessagingService.Verb.COUNTER_MUTATION, WriteResponse.serializer);
			put(MessagingService.Verb.RANGE_SLICE, ReadResponse.rangeSliceSerializer);
			put(MessagingService.Verb.PAGED_RANGE, ReadResponse.rangeSliceSerializer);
			put(MessagingService.Verb.READ, ReadResponse.serializer);
			put(MessagingService.Verb.TRUNCATE, TruncateResponse.serializer);
			put(MessagingService.Verb.SNAPSHOT, null);
			put(MessagingService.Verb.MIGRATION_REQUEST, MigrationManager.MigrationsSerializer.instance);
			put(MessagingService.Verb.SCHEMA_CHECK, UUIDSerializer.serializer);
			put(MessagingService.Verb.BOOTSTRAP_TOKEN, instance);
			put(MessagingService.Verb.REPLICATION_FINISHED, null);
			put(MessagingService.Verb.PAXOS_PREPARE, PrepareResponse.serializer);
			put(MessagingService.Verb.PAXOS_PROPOSE, BooleanSerializer.serializer);
			put(MessagingService.Verb.BATCH_STORE, WriteResponse.serializer);
			put(MessagingService.Verb.BATCH_REMOVE, WriteResponse.serializer);
		}
	};

	private final ExpiringMap<Integer, CallbackInfo> callbacks;

	static class CallbackDeterminedSerializer implements IVersionedSerializer<Object> {
		public static final MessagingService.CallbackDeterminedSerializer instance = new MessagingService.CallbackDeterminedSerializer();

		public Object deserialize(DataInputPlus in, int version) throws IOException {
			throw new UnsupportedOperationException();
		}

		public void serialize(Object o, DataOutputPlus out, int version) throws IOException {
			throw new UnsupportedOperationException();
		}

		public long serializedSize(Object o, int version) {
			throw new UnsupportedOperationException();
		}
	}

	private final Map<MessagingService.Verb, IVerbHandler> verbHandlers;

	private final ConcurrentMap<InetAddress, OutboundTcpConnectionPool> connectionManagers = new NonBlockingHashMap<>();

	private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);

	private static final int LOG_DROPPED_INTERVAL_IN_MS = 5000;

	private final List<MessagingService.SocketThread> socketThreads = Lists.newArrayList();

	private final SimpleCondition listenGate;

	public static final EnumSet<MessagingService.Verb> DROPPABLE_VERBS = EnumSet.of(MessagingService.Verb._TRACE, MessagingService.Verb.MUTATION, MessagingService.Verb.COUNTER_MUTATION, MessagingService.Verb.HINT, MessagingService.Verb.READ_REPAIR, MessagingService.Verb.READ, MessagingService.Verb.RANGE_SLICE, MessagingService.Verb.PAGED_RANGE, MessagingService.Verb.REQUEST_RESPONSE, MessagingService.Verb.BATCH_STORE, MessagingService.Verb.BATCH_REMOVE);

	private static final class DroppedMessages {
		final DroppedMessageMetrics metrics;

		final AtomicInteger droppedInternal;

		final AtomicInteger droppedCrossNode;

		DroppedMessages(MessagingService.Verb verb) {
			droppedCrossNode = null;
			droppedInternal = null;
			metrics = null;
		}

		DroppedMessages(DroppedMessageMetrics metrics) {
			this.metrics = metrics;
			this.droppedInternal = new AtomicInteger(0);
			this.droppedCrossNode = new AtomicInteger(0);
		}
	}

	@VisibleForTesting
	public void resetDroppedMessagesMap(String scope) {
		for (MessagingService.Verb verb : droppedMessagesMap.keySet())
			droppedMessagesMap.put(verb, new MessagingService.DroppedMessages(new DroppedMessageMetrics(( metricName) -> {
				return new CassandraMetricsRegistry.MetricName("DroppedMessages", metricName, scope);
			})));

	}

	private final Map<MessagingService.Verb, MessagingService.DroppedMessages> droppedMessagesMap = new EnumMap<>(MessagingService.Verb.class);

	private final List<ILatencySubscriber> subscribers = new ArrayList<ILatencySubscriber>();

	private final ConcurrentMap<InetAddress, Integer> versions = new NonBlockingHashMap<InetAddress, Integer>();

	private final Set<IMessageSink> messageSinks = new CopyOnWriteArraySet<>();

	private final BackPressureStrategy backPressure = DatabaseDescriptor.getBackPressureStrategy();

	private static class MSHandle {
		public static final MessagingService instance = new MessagingService(false);
	}

	public static MessagingService instance() {
		return MessagingService.MSHandle.instance;
	}

	private static class MSTestHandle {
		public static final MessagingService instance = new MessagingService(true);
	}

	static MessagingService test() {
		return MessagingService.MSTestHandle.instance;
	}

	private MessagingService(boolean testOnly) {
		for (MessagingService.Verb verb : MessagingService.DROPPABLE_VERBS)
			droppedMessagesMap.put(verb, new MessagingService.DroppedMessages(verb));

		listenGate = new SimpleCondition();
		verbHandlers = new EnumMap<>(MessagingService.Verb.class);
		if (!testOnly) {
			Runnable logDropped = new Runnable() {
				public void run() {
					logDroppedMessages();
				}
			};
			ScheduledExecutors.scheduledTasks.scheduleWithFixedDelay(logDropped, MessagingService.LOG_DROPPED_INTERVAL_IN_MS, MessagingService.LOG_DROPPED_INTERVAL_IN_MS, TimeUnit.MILLISECONDS);
		}
		Function<Pair<Integer, ExpiringMap.CacheableObject<CallbackInfo>>, ?> timeoutReporter = new Function<Pair<Integer, ExpiringMap.CacheableObject<CallbackInfo>>, Object>() {
			public Object apply(Pair<Integer, ExpiringMap.CacheableObject<CallbackInfo>> pair) {
				final CallbackInfo expiredCallbackInfo = pair.right.value;
				ConnectionMetrics.totalTimeouts.mark();
				if (expiredCallbackInfo.isFailureCallback()) {
					StageManager.getStage(Stage.INTERNAL_RESPONSE).submit(new Runnable() {
						@Override
						public void run() {
						}
					});
				}
				if (expiredCallbackInfo.shouldHint()) {
					Mutation mutation = ((WriteCallbackInfo) (expiredCallbackInfo)).mutation();
				}
				return null;
			}
		};
		callbacks = new ExpiringMap<>(DatabaseDescriptor.getMinRpcTimeout(), timeoutReporter);
		if (!testOnly) {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			try {
				mbs.registerMBean(this, new ObjectName(MessagingService.MBEAN_NAME));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void addMessageSink(IMessageSink sink) {
		messageSinks.add(sink);
	}

	public void removeMessageSink(IMessageSink sink) {
		messageSinks.remove(sink);
	}

	public void clearMessageSinks() {
		messageSinks.clear();
	}

	public void updateBackPressureOnSend(InetAddress host, IAsyncCallback callback, MessageOut<?> message) {
		if ((DatabaseDescriptor.backPressureEnabled()) && (callback.supportsBackPressure())) {
			BackPressureState backPressureState = getConnectionPool(host).getBackPressureState();
			backPressureState.onMessageSent(message);
		}
	}

	public void updateBackPressureOnReceive(InetAddress host, IAsyncCallback callback, boolean timeout) {
		if ((DatabaseDescriptor.backPressureEnabled()) && (callback.supportsBackPressure())) {
			BackPressureState backPressureState = getConnectionPool(host).getBackPressureState();
			if (!timeout)
				backPressureState.onResponseReceived();
			else
				backPressureState.onResponseTimeout();

		}
	}

	public void applyBackPressure(Iterable<InetAddress> hosts, long timeoutInNanos) {
		if (DatabaseDescriptor.backPressureEnabled()) {
			backPressure.apply(StreamSupport.stream(hosts.spliterator(), false).filter(( h) -> !(h.equals(FBUtilities.getBroadcastAddress()))).map(( h) -> getConnectionPool(h).getBackPressureState()).collect(Collectors.toSet()), timeoutInNanos, TimeUnit.NANOSECONDS);
		}
	}

	public void maybeAddLatency(IAsyncCallback cb, InetAddress address, long latency) {
		if (cb.isLatencyForSnitch())
			addLatency(address, latency);

	}

	public void addLatency(InetAddress address, long latency) {
		for (ILatencySubscriber subscriber : subscribers)
			subscriber.receiveTiming(address, latency);

	}

	public void convict(InetAddress ep) {
		MessagingService.logger.trace("Resetting pool for {}", ep);
	}

	public void listen() {
		callbacks.reset();
		listen(FBUtilities.getLocalAddress());
		if ((DatabaseDescriptor.shouldListenOnBroadcastAddress()) && (!(FBUtilities.getLocalAddress().equals(FBUtilities.getBroadcastAddress())))) {
			listen(FBUtilities.getBroadcastAddress());
		}
		listenGate.signalAll();
	}

	private void listen(InetAddress localEp) throws ConfigurationException {
		for (ServerSocket ss : getServerSockets(localEp)) {
			MessagingService.SocketThread th = new MessagingService.SocketThread(ss, ("ACCEPT-" + localEp));
			th.start();
			socketThreads.add(th);
		}
	}

	@SuppressWarnings("resource")
	private List<ServerSocket> getServerSockets(InetAddress localEp) throws ConfigurationException {
		final List<ServerSocket> ss = new ArrayList<ServerSocket>(2);
		if ((DatabaseDescriptor.getServerEncryptionOptions().internode_encryption) != (none)) {
			try {
				ss.add(SSLFactory.getServerSocket(DatabaseDescriptor.getServerEncryptionOptions(), localEp, DatabaseDescriptor.getSSLStoragePort()));
			} catch (IOException e) {
				throw new ConfigurationException("Unable to create ssl socket", e);
			}
			MessagingService.logger.info("Starting Encrypted Messaging Service on SSL port {}", DatabaseDescriptor.getSSLStoragePort());
		}
		if ((DatabaseDescriptor.getServerEncryptionOptions().internode_encryption) != (all)) {
			ServerSocketChannel serverChannel = null;
			try {
				serverChannel = ServerSocketChannel.open();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			ServerSocket socket = serverChannel.socket();
			try {
				socket.setReuseAddress(true);
			} catch (SocketException e) {
				FileUtils.closeQuietly(socket);
				throw new ConfigurationException("Insufficient permissions to setReuseAddress", e);
			}
			InetSocketAddress address = new InetSocketAddress(localEp, DatabaseDescriptor.getStoragePort());
			try {
				socket.bind(address, 500);
			} catch (BindException e) {
				FileUtils.closeQuietly(socket);
				if (e.getMessage().contains("in use"))
					throw new ConfigurationException((address + " is in use by another process.  Change listen_address:storage_port in cassandra.yaml to values that do not conflict with other services"));
				else
					if (e.getMessage().contains("Cannot assign requested address"))
						throw new ConfigurationException((("Unable to bind to address " + address) + ". Set listen_address in cassandra.yaml to an interface you can bind to, e.g., your private IP address on EC2"));
					else
						throw new RuntimeException(e);


			} catch (IOException e) {
				FileUtils.closeQuietly(socket);
				throw new RuntimeException(e);
			}
			String nic = FBUtilities.getNetworkInterface(localEp);
			MessagingService.logger.info("Starting Messaging Service on {}:{}{}", localEp, DatabaseDescriptor.getStoragePort(), (nic == null ? "" : String.format(" (%s)", nic)));
			ss.add(socket);
		}
		return ss;
	}

	public void waitUntilListening() {
		try {
			listenGate.await();
		} catch (InterruptedException ie) {
			MessagingService.logger.trace("await interrupted");
		}
	}

	public boolean isListening() {
		return listenGate.isSignaled();
	}

	public void destroyConnectionPool(InetAddress to) {
		OutboundTcpConnectionPool cp = connectionManagers.get(to);
		if (cp == null)
			return;

		cp.close();
		connectionManagers.remove(to);
	}

	public OutboundTcpConnectionPool getConnectionPool(InetAddress to) {
		OutboundTcpConnectionPool cp = connectionManagers.get(to);
		if (cp == null) {
			OutboundTcpConnectionPool existingPool = connectionManagers.putIfAbsent(to, cp);
			if (existingPool != null)
				cp = existingPool;
			else
				cp.start();

		}
		cp.waitForStarted();
		return cp;
	}

	public OutboundTcpConnection getConnection(InetAddress to, MessageOut msg) {
		return null;
	}

	public void registerVerbHandlers(MessagingService.Verb verb, IVerbHandler verbHandler) {
		assert !(verbHandlers.containsKey(verb));
		verbHandlers.put(verb, verbHandler);
	}

	public IVerbHandler getVerbHandler(MessagingService.Verb type) {
		return verbHandlers.get(type);
	}

	public int addCallback(IAsyncCallback cb, MessageOut message, InetAddress to, long timeout, boolean failureCallback) {
		int messageId = MessagingService.nextId();
		CallbackInfo previous = callbacks.put(messageId, new CallbackInfo(to, cb, MessagingService.callbackDeserializers.get(message.verb), failureCallback), timeout);
		assert previous == null : String.format("Callback already exists for id %d! (%s)", messageId, previous);
		return messageId;
	}

	public int addCallback(IAsyncCallback cb, MessageOut<?> message, InetAddress to, long timeout, ConsistencyLevel consistencyLevel, boolean allowHints) {
		int messageId = MessagingService.nextId();
		CallbackInfo previous = callbacks.put(messageId, new WriteCallbackInfo(to, cb, message, MessagingService.callbackDeserializers.get(message.verb), consistencyLevel, allowHints), timeout);
		assert previous == null : String.format("Callback already exists for id %d! (%s)", messageId, previous);
		return messageId;
	}

	private static final AtomicInteger idGen = new AtomicInteger(0);

	private static int nextId() {
		return MessagingService.idGen.incrementAndGet();
	}

	public int sendRR(MessageOut message, InetAddress to, IAsyncCallback cb) {
		return sendRR(message, to, cb, message.getTimeout(), false);
	}

	public int sendRRWithFailure(MessageOut message, InetAddress to, IAsyncCallbackWithFailure cb) {
		return sendRR(message, to, cb, message.getTimeout(), true);
	}

	public int sendRR(MessageOut message, InetAddress to, IAsyncCallback cb, long timeout, boolean failureCallback) {
		int id = addCallback(cb, message, to, timeout, failureCallback);
		updateBackPressureOnSend(to, cb, message);
		sendOneWay((failureCallback ? message.withParameter(MessagingService.FAILURE_CALLBACK_PARAM, MessagingService.ONE_BYTE) : message), id, to);
		return id;
	}

	public int sendRR(MessageOut<?> message, InetAddress to, AbstractWriteResponseHandler<?> handler, boolean allowHints) {
		int id = addCallback(handler, message, to, message.getTimeout(), handler.consistencyLevel, allowHints);
		updateBackPressureOnSend(to, handler, message);
		sendOneWay(message.withParameter(MessagingService.FAILURE_CALLBACK_PARAM, MessagingService.ONE_BYTE), id, to);
		return id;
	}

	public void sendOneWay(MessageOut message, InetAddress to) {
		sendOneWay(message, MessagingService.nextId(), to);
	}

	public void sendReply(MessageOut message, int id, InetAddress to) {
		sendOneWay(message, id, to);
	}

	public void sendOneWay(MessageOut message, int id, InetAddress to) {
		if (MessagingService.logger.isTraceEnabled())
			MessagingService.logger.trace("{} sending {} to {}@{}", FBUtilities.getBroadcastAddress(), message.verb, id, to);

		if (to.equals(FBUtilities.getBroadcastAddress()))
			MessagingService.logger.trace("Message-to-self {} going over MessagingService", message);

		for (IMessageSink ms : messageSinks)
			if (!(ms.allowOutgoingMessage(message, id, to)))
				return;


		OutboundTcpConnection connection = getConnection(to, message);
		connection.enqueue(message, id);
	}

	public <T> AsyncOneResponse<T> sendRR(MessageOut message, InetAddress to) {
		AsyncOneResponse<T> iar = new AsyncOneResponse<T>();
		sendRR(message, to, iar);
		return iar;
	}

	public void register(ILatencySubscriber subcriber) {
		subscribers.add(subcriber);
	}

	public void clearCallbacksUnsafe() {
		callbacks.reset();
	}

	public void shutdown() {
		MessagingService.logger.info("Waiting for messaging service to quiesce");
		assert !(StageManager.getStage(Stage.MUTATION).isShutdown());
		if (!(callbacks.shutdownBlocking()))
			MessagingService.logger.warn("Failed to wait for messaging service callbacks shutdown");

		try {
			for (MessagingService.SocketThread th : socketThreads)
				try {
					th.close();
				} catch (IOException e) {
					MessagingService.handleIOExceptionOnClose(e);
				}

		} catch (IOException e) {
			throw new IOError(e);
		}
	}

	public void receive(MessageIn message, int id) {
		TraceState state = Tracing.instance.initializeFromMessage(message);
		if (state != null)
			state.trace("{} message received from {}", message.verb, message.from);

		for (IMessageSink ms : messageSinks)
			if (!(ms.allowIncomingMessage(message, id)))
				return;


		Runnable runnable = new MessageDeliveryTask(message, id);
		LocalAwareExecutorService stage = StageManager.getStage(message.getMessageType());
		assert stage != null : "No stage for message type " + (message.verb);
		stage.execute(runnable, ExecutorLocals.create(state));
	}

	public void setCallbackForTests(int messageId, CallbackInfo callback) {
		callbacks.put(messageId, callback);
	}

	public CallbackInfo getRegisteredCallback(int messageId) {
		return callbacks.get(messageId);
	}

	public CallbackInfo removeRegisteredCallback(int messageId) {
		return callbacks.remove(messageId);
	}

	public long getRegisteredCallbackAge(int messageId) {
		return callbacks.getAge(messageId);
	}

	public static void validateMagic(int magic) throws IOException {
		if (magic != (MessagingService.PROTOCOL_MAGIC))
			throw new IOException("invalid protocol header");

	}

	public static int getBits(int packed, int start, int count) {
		return (packed >>> ((start + 1) - count)) & (~((-1) << count));
	}

	public boolean areAllNodesAtLeast22() {
		return allNodesAtLeast22;
	}

	public boolean areAllNodesAtLeast30() {
		return allNodesAtLeast30;
	}

	public int setVersion(InetAddress endpoint, int version) {
		MessagingService.logger.trace("Setting version {} for {}", version, endpoint);
		if (version < (MessagingService.VERSION_22))
			allNodesAtLeast22 = false;

		if (version < (MessagingService.VERSION_30))
			allNodesAtLeast30 = false;

		Integer v = versions.put(endpoint, version);
		if ((v != null) && ((v < (MessagingService.VERSION_30)) && (version >= (MessagingService.VERSION_22))))
			refreshAllNodeMinVersions();

		return v == null ? version : v;
	}

	public void resetVersion(InetAddress endpoint) {
		MessagingService.logger.trace("Resetting version for {}", endpoint);
		Integer removed = versions.remove(endpoint);
		if ((removed != null) && ((Math.min(removed, MessagingService.current_version)) <= (MessagingService.VERSION_30)))
			refreshAllNodeMinVersions();

	}

	private void refreshAllNodeMinVersions() {
		boolean anyNodeLowerThan30 = false;
		for (Integer version : versions.values()) {
			if (version < (MessagingService.VERSION_30)) {
				anyNodeLowerThan30 = true;
				allNodesAtLeast30 = false;
			}
			if (version < (MessagingService.VERSION_22)) {
				allNodesAtLeast22 = false;
				return;
			}
		}
		allNodesAtLeast22 = true;
		allNodesAtLeast30 = !anyNodeLowerThan30;
	}

	public int getVersion(InetAddress endpoint) {
		Integer v = versions.get(endpoint);
		if (v == null) {
			MessagingService.logger.trace("Assuming current protocol version for {}", endpoint);
			return MessagingService.current_version;
		}else
			return Math.min(v, MessagingService.current_version);

	}

	public int getVersion(String endpoint) throws UnknownHostException {
		return getVersion(InetAddress.getByName(endpoint));
	}

	public int getRawVersion(InetAddress endpoint) {
		Integer v = versions.get(endpoint);
		if (v == null)
			throw new IllegalStateException("getRawVersion() was called without checking knowsVersion() result first");

		return v;
	}

	public boolean knowsVersion(InetAddress endpoint) {
		return versions.containsKey(endpoint);
	}

	public void incrementDroppedMutations(Optional<IMutation> mutationOpt, long timeTaken) {
		if (mutationOpt.isPresent()) {
			updateDroppedMutationCount(mutationOpt.get());
		}
		incrementDroppedMessages(MessagingService.Verb.MUTATION, timeTaken);
	}

	public void incrementDroppedMessages(MessagingService.Verb verb) {
		incrementDroppedMessages(verb, false);
	}

	public void incrementDroppedMessages(MessagingService.Verb verb, long timeTaken) {
		incrementDroppedMessages(verb, timeTaken, false);
	}

	public void incrementDroppedMessages(MessageIn message, long timeTaken) {
		if ((message.payload) instanceof IMutation) {
			updateDroppedMutationCount(((IMutation) (message.payload)));
		}
	}

	public void incrementDroppedMessages(MessagingService.Verb verb, long timeTaken, boolean isCrossNode) {
		assert MessagingService.DROPPABLE_VERBS.contains(verb) : ("Verb " + verb) + " should not legally be dropped";
		incrementDroppedMessages(droppedMessagesMap.get(verb), timeTaken, isCrossNode);
	}

	public void incrementDroppedMessages(MessagingService.Verb verb, boolean isCrossNode) {
		assert MessagingService.DROPPABLE_VERBS.contains(verb) : ("Verb " + verb) + " should not legally be dropped";
		incrementDroppedMessages(droppedMessagesMap.get(verb), isCrossNode);
	}

	private void updateDroppedMutationCount(IMutation mutation) {
		assert mutation != null : "Mutation should not be null when updating dropped mutations count";
		for (UUID columnFamilyId : mutation.getColumnFamilyIds()) {
			ColumnFamilyStore cfs = Keyspace.open(mutation.getKeyspaceName()).getColumnFamilyStore(columnFamilyId);
			if (cfs != null) {
				cfs.metric.droppedMutations.inc();
			}
		}
	}

	private void incrementDroppedMessages(MessagingService.DroppedMessages droppedMessages, long timeTaken, boolean isCrossNode) {
		if (isCrossNode)
			droppedMessages.metrics.crossNodeDroppedLatency.update(timeTaken, TimeUnit.MILLISECONDS);
		else
			droppedMessages.metrics.internalDroppedLatency.update(timeTaken, TimeUnit.MILLISECONDS);

		incrementDroppedMessages(droppedMessages, isCrossNode);
	}

	private void incrementDroppedMessages(MessagingService.DroppedMessages droppedMessages, boolean isCrossNode) {
		droppedMessages.metrics.dropped.mark();
		if (isCrossNode)
			droppedMessages.droppedCrossNode.incrementAndGet();
		else
			droppedMessages.droppedInternal.incrementAndGet();

	}

	private void logDroppedMessages() {
		List<String> logs = getDroppedMessagesLogs();
		for (String log : logs)
			MessagingService.logger.info(log);

		if ((logs.size()) > 0)
			StatusLogger.log();

	}

	@VisibleForTesting
	List<String> getDroppedMessagesLogs() {
		List<String> ret = new ArrayList<>();
		for (Map.Entry<MessagingService.Verb, MessagingService.DroppedMessages> entry : droppedMessagesMap.entrySet()) {
			MessagingService.Verb verb = entry.getKey();
			MessagingService.DroppedMessages droppedMessages = entry.getValue();
			int droppedInternal = droppedMessages.droppedInternal.getAndSet(0);
			int droppedCrossNode = droppedMessages.droppedCrossNode.getAndSet(0);
			if ((droppedInternal > 0) || (droppedCrossNode > 0)) {
				ret.add(String.format(("%s messages were dropped in last %d ms: %d internal and %d cross node." + " Mean internal dropped latency: %d ms and Mean cross-node dropped latency: %d ms"), verb, MessagingService.LOG_DROPPED_INTERVAL_IN_MS, droppedInternal, droppedCrossNode, TimeUnit.NANOSECONDS.toMillis(((long) (droppedMessages.metrics.internalDroppedLatency.getSnapshot().getMean()))), TimeUnit.NANOSECONDS.toMillis(((long) (droppedMessages.metrics.crossNodeDroppedLatency.getSnapshot().getMean())))));
			}
		}
		return ret;
	}

	@VisibleForTesting
	public static class SocketThread extends Thread {
		private final ServerSocket server;

		@VisibleForTesting
		public final Set<Closeable> connections = Sets.newConcurrentHashSet();

		SocketThread(ServerSocket server, String name) {
			super(name);
			this.server = server;
		}

		@SuppressWarnings("resource")
		public void run() {
			while (!(server.isClosed())) {
				Socket socket = null;
				try {
					socket = server.accept();
					if (!(authenticate(socket))) {
						MessagingService.logger.trace("remote failed to authenticate");
						socket.close();
						continue;
					}
					socket.setKeepAlive(true);
					socket.setSoTimeout((2 * (OutboundTcpConnection.WAIT_FOR_VERSION_MAX_TIME)));
					DataInputStream in = new DataInputStream(socket.getInputStream());
					MessagingService.validateMagic(in.readInt());
					int header = in.readInt();
					boolean isStream = (MessagingService.getBits(header, 3, 1)) == 1;
					int version = MessagingService.getBits(header, 15, 8);
					MessagingService.logger.trace("Connection version {} from {}", version, socket.getInetAddress());
					socket.setSoTimeout(0);
					Thread thread = (isStream) ? new IncomingStreamingConnection(version, socket, connections) : new IncomingTcpConnection(version, ((MessagingService.getBits(header, 2, 1)) == 1), socket, connections);
					thread.start();
					connections.add(((Closeable) (thread)));
				} catch (AsynchronousCloseException e) {
					MessagingService.logger.trace("Asynchronous close seen by server thread");
					break;
				} catch (ClosedChannelException e) {
					MessagingService.logger.trace("MessagingService server thread already closed");
					break;
				} catch (SSLHandshakeException e) {
					MessagingService.logger.error(("SSL handshake error for inbound connection from " + socket), e);
					FileUtils.closeQuietly(socket);
				} catch (Throwable t) {
					MessagingService.logger.trace("Error reading the socket {}", socket, t);
					FileUtils.closeQuietly(socket);
				}
			} 
			MessagingService.logger.info("MessagingService has terminated the accept() thread");
		}

		void close() throws IOException {
			MessagingService.logger.trace("Closing accept() thread");
			try {
				server.close();
			} catch (IOException e) {
				MessagingService.handleIOExceptionOnClose(e);
			}
			for (Closeable connection : connections) {
				connection.close();
			}
		}

		private boolean authenticate(Socket socket) {
			return DatabaseDescriptor.getInternodeAuthenticator().authenticate(socket.getInetAddress(), socket.getPort());
		}
	}

	private static void handleIOExceptionOnClose(IOException e) throws IOException {
		if ("Mac OS X".equals(System.getProperty("os.name"))) {
			switch (e.getMessage()) {
				case "Unknown error: 316" :
				case "No such file or directory" :
					return;
			}
		}
		throw e;
	}

	public Map<String, Integer> getLargeMessagePendingTasks() {
		Map<String, Integer> pendingTasks = new HashMap<String, Integer>(connectionManagers.size());
		for (Map.Entry<InetAddress, OutboundTcpConnectionPool> entry : connectionManagers.entrySet())
			pendingTasks.put(entry.getKey().getHostAddress(), entry.getValue().largeMessages.getPendingMessages());

		return pendingTasks;
	}

	public int getLargeMessagePendingTasks(InetAddress address) {
		OutboundTcpConnectionPool connection = connectionManagers.get(address);
		return connection == null ? 0 : connection.largeMessages.getPendingMessages();
	}

	public Map<String, Long> getLargeMessageCompletedTasks() {
		Map<String, Long> completedTasks = new HashMap<String, Long>(connectionManagers.size());
		for (Map.Entry<InetAddress, OutboundTcpConnectionPool> entry : connectionManagers.entrySet())
			completedTasks.put(entry.getKey().getHostAddress(), entry.getValue().largeMessages.getCompletedMesssages());

		return completedTasks;
	}

	public Map<String, Long> getLargeMessageDroppedTasks() {
		Map<String, Long> droppedTasks = new HashMap<String, Long>(connectionManagers.size());
		for (Map.Entry<InetAddress, OutboundTcpConnectionPool> entry : connectionManagers.entrySet())
			droppedTasks.put(entry.getKey().getHostAddress(), entry.getValue().largeMessages.getDroppedMessages());

		return droppedTasks;
	}

	public Map<String, Integer> getSmallMessagePendingTasks() {
		Map<String, Integer> pendingTasks = new HashMap<String, Integer>(connectionManagers.size());
		for (Map.Entry<InetAddress, OutboundTcpConnectionPool> entry : connectionManagers.entrySet())
			pendingTasks.put(entry.getKey().getHostAddress(), entry.getValue().smallMessages.getPendingMessages());

		return pendingTasks;
	}

	public Map<String, Long> getSmallMessageCompletedTasks() {
		Map<String, Long> completedTasks = new HashMap<String, Long>(connectionManagers.size());
		for (Map.Entry<InetAddress, OutboundTcpConnectionPool> entry : connectionManagers.entrySet())
			completedTasks.put(entry.getKey().getHostAddress(), entry.getValue().smallMessages.getCompletedMesssages());

		return completedTasks;
	}

	public Map<String, Long> getSmallMessageDroppedTasks() {
		Map<String, Long> droppedTasks = new HashMap<String, Long>(connectionManagers.size());
		for (Map.Entry<InetAddress, OutboundTcpConnectionPool> entry : connectionManagers.entrySet())
			droppedTasks.put(entry.getKey().getHostAddress(), entry.getValue().smallMessages.getDroppedMessages());

		return droppedTasks;
	}

	public Map<String, Integer> getGossipMessagePendingTasks() {
		Map<String, Integer> pendingTasks = new HashMap<String, Integer>(connectionManagers.size());
		for (Map.Entry<InetAddress, OutboundTcpConnectionPool> entry : connectionManagers.entrySet())
			pendingTasks.put(entry.getKey().getHostAddress(), entry.getValue().gossipMessages.getPendingMessages());

		return pendingTasks;
	}

	public Map<String, Long> getGossipMessageCompletedTasks() {
		Map<String, Long> completedTasks = new HashMap<String, Long>(connectionManagers.size());
		for (Map.Entry<InetAddress, OutboundTcpConnectionPool> entry : connectionManagers.entrySet())
			completedTasks.put(entry.getKey().getHostAddress(), entry.getValue().gossipMessages.getCompletedMesssages());

		return completedTasks;
	}

	public Map<String, Long> getGossipMessageDroppedTasks() {
		Map<String, Long> droppedTasks = new HashMap<String, Long>(connectionManagers.size());
		for (Map.Entry<InetAddress, OutboundTcpConnectionPool> entry : connectionManagers.entrySet())
			droppedTasks.put(entry.getKey().getHostAddress(), entry.getValue().gossipMessages.getDroppedMessages());

		return droppedTasks;
	}

	public Map<String, Integer> getDroppedMessages() {
		Map<String, Integer> map = new HashMap<>(droppedMessagesMap.size());
		for (Map.Entry<MessagingService.Verb, MessagingService.DroppedMessages> entry : droppedMessagesMap.entrySet())
			map.put(entry.getKey().toString(), ((int) (entry.getValue().metrics.dropped.getCount())));

		return map;
	}

	public long getTotalTimeouts() {
		return ConnectionMetrics.totalTimeouts.getCount();
	}

	public Map<String, Long> getTimeoutsPerHost() {
		Map<String, Long> result = new HashMap<String, Long>(connectionManagers.size());
		for (Map.Entry<InetAddress, OutboundTcpConnectionPool> entry : connectionManagers.entrySet()) {
			String ip = entry.getKey().getHostAddress();
			long recent = entry.getValue().getTimeouts();
			result.put(ip, recent);
		}
		return result;
	}

	public Map<String, Double> getBackPressurePerHost() {
		Map<String, Double> map = new HashMap<>(connectionManagers.size());
		for (Map.Entry<InetAddress, OutboundTcpConnectionPool> entry : connectionManagers.entrySet())
			map.put(entry.getKey().getHostAddress(), entry.getValue().getBackPressureState().getBackPressureRateLimit());

		return map;
	}

	@Override
	public void setBackPressureEnabled(boolean enabled) {
		DatabaseDescriptor.setBackPressureEnabled(enabled);
	}

	@Override
	public boolean isBackPressureEnabled() {
		return DatabaseDescriptor.backPressureEnabled();
	}

	public static IPartitioner globalPartitioner() {
		return StorageService.instance.getTokenMetadata().partitioner;
	}

	public static void validatePartitioner(Collection<? extends AbstractBounds<?>> allBounds) {
		for (AbstractBounds<?> bounds : allBounds)
			MessagingService.validatePartitioner(bounds);

	}

	public static void validatePartitioner(AbstractBounds<?> bounds) {
		if ((MessagingService.globalPartitioner()) != (bounds.left.getPartitioner()))
			throw new AssertionError(String.format("Partitioner in bounds serialization. Expected %s, was %s.", MessagingService.globalPartitioner().getClass().getName(), bounds.left.getPartitioner().getClass().getName()));

	}

	@VisibleForTesting
	public List<MessagingService.SocketThread> getSocketThreads() {
		return socketThreads;
	}
}

