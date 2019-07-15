

import com.codahale.metrics.Gauge;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.apache.cassandra.concurrent.DebuggableScheduledThreadPoolExecutor;
import org.apache.cassandra.concurrent.JMXEnabledThreadPoolExecutor;
import org.apache.cassandra.concurrent.LocalAwareExecutorService;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.gms.ApplicationState;
import org.apache.cassandra.gms.EchoMessage;
import org.apache.cassandra.gms.EndpointState;
import org.apache.cassandra.gms.FailureDetector;
import org.apache.cassandra.gms.GossipDigest;
import org.apache.cassandra.gms.GossipDigestSyn;
import org.apache.cassandra.gms.GossiperMBean;
import org.apache.cassandra.gms.IEndpointStateChangeSubscriber;
import org.apache.cassandra.gms.IFailureDetectionEventListener;
import org.apache.cassandra.gms.IFailureDetector;
import org.apache.cassandra.gms.VersionedValue;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.locator.IEndpointSnitch;
import org.apache.cassandra.locator.TokenMetadata;
import org.apache.cassandra.metrics.ThreadPoolMetrics;
import org.apache.cassandra.net.IAsyncCallback;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.utils.CassandraVersion;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.cassandra.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.net.MessagingService.Verb.ECHO;
import static org.apache.cassandra.net.MessagingService.Verb.GOSSIP_DIGEST_SYN;
import static org.apache.cassandra.net.MessagingService.Verb.GOSSIP_SHUTDOWN;


public class Gossiper implements GossiperMBean , IFailureDetectionEventListener {
	public static final String MBEAN_NAME = "org.apache.cassandra.net:type=Gossiper";

	private static final DebuggableScheduledThreadPoolExecutor executor = new DebuggableScheduledThreadPoolExecutor("GossipTasks");

	static final ApplicationState[] STATES = ApplicationState.values();

	static final List<String> DEAD_STATES = Arrays.asList(VersionedValue.REMOVING_TOKEN, VersionedValue.REMOVED_TOKEN, VersionedValue.STATUS_LEFT, VersionedValue.HIBERNATE);

	static ArrayList<String> SILENT_SHUTDOWN_STATES = new ArrayList<>();

	static {
		Gossiper.SILENT_SHUTDOWN_STATES.addAll(Gossiper.DEAD_STATES);
		Gossiper.SILENT_SHUTDOWN_STATES.add(VersionedValue.STATUS_BOOTSTRAPPING);
		Gossiper.SILENT_SHUTDOWN_STATES.add(VersionedValue.STATUS_BOOTSTRAPPING_REPLACE);
	}

	private volatile ScheduledFuture<?> scheduledGossipTask;

	private static final ReentrantLock taskLock = new ReentrantLock();

	public static final int intervalInMillis = 1000;

	public static final int QUARANTINE_DELAY = (StorageService.RING_DELAY) * 2;

	private static final Logger logger = LoggerFactory.getLogger(Gossiper.class);

	public static final Gossiper instance = new Gossiper();

	volatile long firstSynSendAt = 0L;

	public static final long aVeryLongTime = 259200 * 1000;

	static final int MAX_GENERATION_DIFFERENCE = 86400 * 365;

	private long fatClientTimeout;

	private final Random random = new Random();

	private final Comparator<InetAddress> inetcomparator = null;

	private final List<IEndpointStateChangeSubscriber> subscribers = new CopyOnWriteArrayList<IEndpointStateChangeSubscriber>();

	private final Set<InetAddress> liveEndpoints = new ConcurrentSkipListSet<InetAddress>(inetcomparator);

	private final Map<InetAddress, Long> unreachableEndpoints = new ConcurrentHashMap<InetAddress, Long>();

	@VisibleForTesting
	final Set<InetAddress> seeds = new ConcurrentSkipListSet<InetAddress>(inetcomparator);

	final ConcurrentMap<InetAddress, EndpointState> endpointStateMap = new ConcurrentHashMap<InetAddress, EndpointState>();

	private final Map<InetAddress, Long> justRemovedEndpoints = new ConcurrentHashMap<InetAddress, Long>();

	private final Map<InetAddress, Long> expireTimeEndpointMap = new ConcurrentHashMap<InetAddress, Long>();

	private volatile boolean anyNodeOn30 = false;

	private volatile boolean inShadowRound = false;

	private final Set<InetAddress> seedsInShadowRound = new ConcurrentSkipListSet<>(inetcomparator);

	private final Map<InetAddress, EndpointState> endpointShadowStateMap = new ConcurrentHashMap<>();

	private volatile long lastProcessedMessageAt = System.currentTimeMillis();

	private class GossipTask implements Runnable {
		public void run() {
			try {
				MessagingService.instance().waitUntilListening();
				Gossiper.taskLock.lock();
				if (Gossiper.logger.isTraceEnabled()) {
				}
				final List<GossipDigest> gDigests = new ArrayList<GossipDigest>();
				Gossiper.instance.makeRandomGossipDigest(gDigests);
				if ((gDigests.size()) > 0) {
					GossipDigestSyn digestSynMessage = new GossipDigestSyn(DatabaseDescriptor.getClusterName(), DatabaseDescriptor.getPartitionerName(), gDigests);
					MessageOut<GossipDigestSyn> message = new MessageOut<GossipDigestSyn>(GOSSIP_DIGEST_SYN, digestSynMessage, GossipDigestSyn.serializer);
					boolean gossipedToSeed = doGossipToLiveMember(message);
					maybeGossipToUnreachableMember(message);
					if ((!gossipedToSeed) || ((liveEndpoints.size()) < (seeds.size())))
						maybeGossipToSeed(message);

					doStatusCheck();
				}
			} catch (Exception e) {
				JVMStabilityInspector.inspectThrowable(e);
				Gossiper.logger.error("Gossip error", e);
			} finally {
				Gossiper.taskLock.unlock();
			}
		}
	}

	private Gossiper() {
		fatClientTimeout = (Gossiper.QUARANTINE_DELAY) / 2;
		FailureDetector.instance.registerFailureDetectionEventListener(this);
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.registerMBean(this, new ObjectName(Gossiper.MBEAN_NAME));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setLastProcessedMessageAt(long timeInMillis) {
		this.lastProcessedMessageAt = timeInMillis;
	}

	public boolean seenAnySeed() {
		for (Map.Entry<InetAddress, EndpointState> entry : endpointStateMap.entrySet()) {
			if (seeds.contains(entry.getKey()))
				return true;

			try {
				VersionedValue internalIp = entry.getValue().getApplicationState(ApplicationState.INTERNAL_IP);
				if ((internalIp != null) && (seeds.contains(InetAddress.getByName(internalIp.value))))
					return true;

			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
		}
		return false;
	}

	public void register(IEndpointStateChangeSubscriber subscriber) {
		subscribers.add(subscriber);
	}

	public void unregister(IEndpointStateChangeSubscriber subscriber) {
		subscribers.remove(subscriber);
	}

	public Set<InetAddress> getLiveMembers() {
		Set<InetAddress> liveMembers = new HashSet<>(liveEndpoints);
		if (!(liveMembers.contains(FBUtilities.getBroadcastAddress())))
			liveMembers.add(FBUtilities.getBroadcastAddress());

		return liveMembers;
	}

	public Set<InetAddress> getLiveTokenOwners() {
		return StorageService.instance.getLiveRingMembers(true);
	}

	public Set<InetAddress> getUnreachableMembers() {
		return unreachableEndpoints.keySet();
	}

	public Set<InetAddress> getUnreachableTokenOwners() {
		Set<InetAddress> tokenOwners = new HashSet<>();
		for (InetAddress endpoint : unreachableEndpoints.keySet()) {
			if (StorageService.instance.getTokenMetadata().isMember(endpoint))
				tokenOwners.add(endpoint);

		}
		return tokenOwners;
	}

	public long getEndpointDowntime(InetAddress ep) {
		Long downtime = unreachableEndpoints.get(ep);
		if (downtime != null)
			return TimeUnit.NANOSECONDS.toMillis(((System.nanoTime()) - downtime));
		else
			return 0L;

	}

	private boolean isShutdown(InetAddress endpoint) {
		EndpointState epState = endpointStateMap.get(endpoint);
		if (epState == null)
			return false;

		if ((epState.getApplicationState(ApplicationState.STATUS)) == null)
			return false;

		String value = epState.getApplicationState(ApplicationState.STATUS).value;
		String[] pieces = value.split(VersionedValue.DELIMITER_STR, (-1));
		assert (pieces.length) > 0;
		String state = pieces[0];
		return state.equals(VersionedValue.SHUTDOWN);
	}

	public void convict(InetAddress endpoint, double phi) {
		EndpointState epState = endpointStateMap.get(endpoint);
		if (epState == null)
			return;

		if (!(epState.isAlive()))
			return;

		Gossiper.logger.debug("Convicting {} with status {} - alive {}", endpoint, Gossiper.getGossipStatus(epState), epState.isAlive());
		if (isShutdown(endpoint)) {
			markAsShutdown(endpoint);
		}else {
			markDead(endpoint, epState);
		}
	}

	protected void markAsShutdown(InetAddress endpoint) {
		EndpointState epState = endpointStateMap.get(endpoint);
		if (epState == null)
			return;

		epState.addApplicationState(ApplicationState.STATUS, StorageService.instance.valueFactory.shutdown(true));
		epState.addApplicationState(ApplicationState.RPC_READY, StorageService.instance.valueFactory.rpcReady(false));
		markDead(endpoint, epState);
		FailureDetector.instance.forceConviction(endpoint);
	}

	int getMaxEndpointStateVersion(EndpointState epState) {
		for (Map.Entry<ApplicationState, VersionedValue> state : epState.states()) {
		}
		return 0;
	}

	private void evictFromMembership(InetAddress endpoint) {
		unreachableEndpoints.remove(endpoint);
		endpointStateMap.remove(endpoint);
		expireTimeEndpointMap.remove(endpoint);
		FailureDetector.instance.remove(endpoint);
		quarantineEndpoint(endpoint);
		if (Gossiper.logger.isDebugEnabled())
			Gossiper.logger.debug("evicting {} from gossip", endpoint);

	}

	public void removeEndpoint(InetAddress endpoint) {
		for (IEndpointStateChangeSubscriber subscriber : subscribers)
			subscriber.onRemove(endpoint);

		if (seeds.contains(endpoint)) {
			buildSeedsList();
			seeds.remove(endpoint);
			Gossiper.logger.info("removed {} from seeds, updated seeds list = {}", endpoint, seeds);
		}
		liveEndpoints.remove(endpoint);
		unreachableEndpoints.remove(endpoint);
		MessagingService.instance().resetVersion(endpoint);
		quarantineEndpoint(endpoint);
		MessagingService.instance().destroyConnectionPool(endpoint);
		if (Gossiper.logger.isDebugEnabled())
			Gossiper.logger.debug("removing endpoint {}", endpoint);

	}

	private void quarantineEndpoint(InetAddress endpoint) {
		quarantineEndpoint(endpoint, System.currentTimeMillis());
	}

	private void quarantineEndpoint(InetAddress endpoint, long quarantineExpiration) {
		justRemovedEndpoints.put(endpoint, quarantineExpiration);
	}

	public void replacementQuarantine(InetAddress endpoint) {
		Gossiper.logger.debug("");
		quarantineEndpoint(endpoint, ((System.currentTimeMillis()) + (Gossiper.QUARANTINE_DELAY)));
	}

	public void replacedEndpoint(InetAddress endpoint) {
		removeEndpoint(endpoint);
		evictFromMembership(endpoint);
		replacementQuarantine(endpoint);
	}

	private void makeRandomGossipDigest(List<GossipDigest> gDigests) {
		EndpointState epState;
		int generation = 0;
		int maxVersion = 0;
		List<InetAddress> endpoints = new ArrayList<InetAddress>(endpointStateMap.keySet());
		Collections.shuffle(endpoints, random);
		for (InetAddress endpoint : endpoints) {
			epState = endpointStateMap.get(endpoint);
			if (epState != null) {
				maxVersion = getMaxEndpointStateVersion(epState);
			}
		}
		if (Gossiper.logger.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (GossipDigest gDigest : gDigests) {
				sb.append(gDigest);
				sb.append(" ");
			}
			Gossiper.logger.trace("Gossip Digests are : {}", sb);
		}
	}

	public void advertiseRemoving(InetAddress endpoint, UUID hostId, UUID localHostId) {
		EndpointState epState = endpointStateMap.get(endpoint);
		Gossiper.logger.info("Removing host: {}", hostId);
		Gossiper.logger.info("Sleeping for {}ms to ensure {} does not change", StorageService.RING_DELAY, endpoint);
		Uninterruptibles.sleepUninterruptibly(StorageService.RING_DELAY, TimeUnit.MILLISECONDS);
		epState = endpointStateMap.get(endpoint);
		Gossiper.logger.info("Advertising removal for {}", endpoint);
		Map<ApplicationState, VersionedValue> states = new EnumMap<>(ApplicationState.class);
		states.put(ApplicationState.STATUS, StorageService.instance.valueFactory.removingNonlocal(hostId));
		states.put(ApplicationState.REMOVAL_COORDINATOR, StorageService.instance.valueFactory.removalCoordinator(localHostId));
		epState.addApplicationStates(states);
		endpointStateMap.put(endpoint, epState);
	}

	public void advertiseTokenRemoved(InetAddress endpoint, UUID hostId) {
		EndpointState epState = endpointStateMap.get(endpoint);
		long expireTime = Gossiper.computeExpireTime();
		epState.addApplicationState(ApplicationState.STATUS, StorageService.instance.valueFactory.removedNonlocal(hostId, expireTime));
		Gossiper.logger.info("Completing removal of {}", endpoint);
		addExpireTimeForEndpoint(endpoint, expireTime);
		endpointStateMap.put(endpoint, epState);
		Uninterruptibles.sleepUninterruptibly(((Gossiper.intervalInMillis) * 2), TimeUnit.MILLISECONDS);
	}

	public void unsafeAssassinateEndpoint(String address) throws UnknownHostException {
		Gossiper.logger.warn("Gossiper.unsafeAssassinateEndpoint is deprecated and will be removed in the next release; use assassinateEndpoint instead");
		assassinateEndpoint(address);
	}

	public void assassinateEndpoint(String address) throws UnknownHostException {
		InetAddress endpoint = InetAddress.getByName(address);
		EndpointState epState = endpointStateMap.get(endpoint);
		Collection<Token> tokens = null;
		Gossiper.logger.warn("Assassinating {} via gossip", endpoint);
		if (epState == null) {
		}else {
			Gossiper.logger.info("Sleeping for {}ms to ensure {} does not change", StorageService.RING_DELAY, endpoint);
			Uninterruptibles.sleepUninterruptibly(StorageService.RING_DELAY, TimeUnit.MILLISECONDS);
			EndpointState newState = endpointStateMap.get(endpoint);
			if (newState == null)
				Gossiper.logger.warn("Endpoint {} disappeared while trying to assassinate, continuing anyway", endpoint);
			else {
			}
		}
		try {
			tokens = StorageService.instance.getTokenMetadata().getTokens(endpoint);
		} catch (Throwable th) {
			JVMStabilityInspector.inspectThrowable(th);
			Gossiper.logger.warn("Unable to calculate tokens for {}.  Will use a random one", address);
			tokens = Collections.singletonList(StorageService.instance.getTokenMetadata().partitioner.getRandomToken());
		}
		epState.addApplicationState(ApplicationState.STATUS, StorageService.instance.valueFactory.left(tokens, Gossiper.computeExpireTime()));
		handleMajorStateChange(endpoint, epState);
		Uninterruptibles.sleepUninterruptibly(((Gossiper.intervalInMillis) * 4), TimeUnit.MILLISECONDS);
		Gossiper.logger.warn("Finished assassinating {}", endpoint);
	}

	public boolean isKnownEndpoint(InetAddress endpoint) {
		return endpointStateMap.containsKey(endpoint);
	}

	public int getCurrentGenerationNumber(InetAddress endpoint) {
		return 0;
	}

	private boolean sendGossip(MessageOut<GossipDigestSyn> message, Set<InetAddress> epSet) {
		List<InetAddress> liveEndpoints = ImmutableList.copyOf(epSet);
		int size = liveEndpoints.size();
		if (size < 1)
			return false;

		int index = (size == 1) ? 0 : random.nextInt(size);
		InetAddress to = liveEndpoints.get(index);
		if (Gossiper.logger.isTraceEnabled())
			Gossiper.logger.trace("Sending a GossipDigestSyn to {} ...", to);

		if ((firstSynSendAt) == 0)
			firstSynSendAt = System.nanoTime();

		MessagingService.instance().sendOneWay(message, to);
		return seeds.contains(to);
	}

	private boolean doGossipToLiveMember(MessageOut<GossipDigestSyn> message) {
		int size = liveEndpoints.size();
		if (size == 0)
			return false;

		return sendGossip(message, liveEndpoints);
	}

	private void maybeGossipToUnreachableMember(MessageOut<GossipDigestSyn> message) {
		double liveEndpointCount = liveEndpoints.size();
		double unreachableEndpointCount = unreachableEndpoints.size();
		if (unreachableEndpointCount > 0) {
			double prob = unreachableEndpointCount / (liveEndpointCount + 1);
			double randDbl = random.nextDouble();
			if (randDbl < prob)
				sendGossip(message, unreachableEndpoints.keySet());

		}
	}

	private void maybeGossipToSeed(MessageOut<GossipDigestSyn> prod) {
		int size = seeds.size();
		if (size > 0) {
			if ((size == 1) && (seeds.contains(FBUtilities.getBroadcastAddress()))) {
				return;
			}
			if ((liveEndpoints.size()) == 0) {
				sendGossip(prod, seeds);
			}else {
				double probability = (seeds.size()) / ((double) ((liveEndpoints.size()) + (unreachableEndpoints.size())));
				double randDbl = random.nextDouble();
				if (randDbl <= probability)
					sendGossip(prod, seeds);

			}
		}
	}

	public boolean isGossipOnlyMember(InetAddress endpoint) {
		EndpointState epState = endpointStateMap.get(endpoint);
		if (epState == null) {
			return false;
		}
		return (!(isDeadState(epState))) && (!(StorageService.instance.getTokenMetadata().isMember(endpoint)));
	}

	public boolean isSafeForStartup(InetAddress endpoint, UUID localHostUUID, boolean isBootstrapping, Map<InetAddress, EndpointState> epStates) {
		EndpointState epState = epStates.get(endpoint);
		if ((epState == null) || (isDeadState(epState)))
			return true;

		if (isBootstrapping) {
			String status = Gossiper.getGossipStatus(epState);
			final List<String> unsafeStatuses = new ArrayList<String>() {
				{
					add("");
					add(VersionedValue.STATUS_NORMAL);
					add(VersionedValue.SHUTDOWN);
				}
			};
			return !(unsafeStatuses.contains(status));
		}else {
			VersionedValue previous = epState.getApplicationState(ApplicationState.HOST_ID);
			return UUID.fromString(previous.value).equals(localHostUUID);
		}
	}

	private void doStatusCheck() {
		if (Gossiper.logger.isTraceEnabled())
			Gossiper.logger.trace("Performing status check ...");

		long now = System.currentTimeMillis();
		long nowNano = System.nanoTime();
		long pending = ((JMXEnabledThreadPoolExecutor) (StageManager.getStage(Stage.GOSSIP))).metrics.pendingTasks.getValue();
		if ((pending > 0) && ((lastProcessedMessageAt) < (now - 1000))) {
			Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
			if ((lastProcessedMessageAt) < (now - 1000)) {
				Gossiper.logger.warn("Gossip stage has {} pending tasks; skipping status check (no nodes will be marked down)", pending);
				return;
			}
		}
		Set<InetAddress> eps = endpointStateMap.keySet();
		for (InetAddress endpoint : eps) {
			if (endpoint.equals(FBUtilities.getBroadcastAddress()))
				continue;

			FailureDetector.instance.interpret(endpoint);
			EndpointState epState = endpointStateMap.get(endpoint);
			if (epState != null) {
				if (((isGossipOnlyMember(endpoint)) && (!(justRemovedEndpoints.containsKey(endpoint)))) && ((TimeUnit.NANOSECONDS.toMillis((nowNano - (epState.getUpdateTimestamp())))) > (fatClientTimeout))) {
					Gossiper.logger.info("FatClient {} has been silent for {}ms, removing from gossip", endpoint, fatClientTimeout);
					removeEndpoint(endpoint);
					evictFromMembership(endpoint);
				}
				long expireTime = getExpireTimeForEndpoint(endpoint);
				if (((!(epState.isAlive())) && (now > expireTime)) && (!(StorageService.instance.getTokenMetadata().isMember(endpoint)))) {
					if (Gossiper.logger.isDebugEnabled()) {
						Gossiper.logger.debug("time is expiring for endpoint : {} ({})", endpoint, expireTime);
					}
					evictFromMembership(endpoint);
				}
			}
		}
		if (!(justRemovedEndpoints.isEmpty())) {
			for (Map.Entry<InetAddress, Long> entry : justRemovedEndpoints.entrySet()) {
				if ((now - (entry.getValue())) > (Gossiper.QUARANTINE_DELAY)) {
					if (Gossiper.logger.isDebugEnabled())
						Gossiper.logger.debug("{} elapsed, {} gossip quarantine over", Gossiper.QUARANTINE_DELAY, entry.getKey());

					justRemovedEndpoints.remove(entry.getKey());
				}
			}
		}
	}

	protected long getExpireTimeForEndpoint(InetAddress endpoint) {
		Long storedTime = expireTimeEndpointMap.get(endpoint);
		return storedTime == null ? Gossiper.computeExpireTime() : storedTime;
	}

	public EndpointState getEndpointStateForEndpoint(InetAddress ep) {
		return endpointStateMap.get(ep);
	}

	public Set<Map.Entry<InetAddress, EndpointState>> getEndpointStates() {
		return endpointStateMap.entrySet();
	}

	public UUID getHostId(InetAddress endpoint) {
		return getHostId(endpoint, endpointStateMap);
	}

	public UUID getHostId(InetAddress endpoint, Map<InetAddress, EndpointState> epStates) {
		return UUID.fromString(epStates.get(endpoint).getApplicationState(ApplicationState.HOST_ID).value);
	}

	EndpointState getStateForVersionBiggerThan(InetAddress forEndpoint, int version) {
		EndpointState epState = endpointStateMap.get(forEndpoint);
		EndpointState reqdEndpointState = null;
		if (epState != null) {
			Map<ApplicationState, VersionedValue> states = new EnumMap<>(ApplicationState.class);
			for (Map.Entry<ApplicationState, VersionedValue> entry : epState.states()) {
				VersionedValue value = entry.getValue();
				if ((value.version) > version) {
					if (reqdEndpointState == null) {
					}
					final ApplicationState key = entry.getKey();
					if (Gossiper.logger.isTraceEnabled())
						Gossiper.logger.trace("Adding state {}: {}", key, value.value);

					states.put(key, value);
				}
			}
			if (reqdEndpointState != null)
				reqdEndpointState.addApplicationStates(states);

		}
		return reqdEndpointState;
	}

	public int compareEndpointStartup(InetAddress addr1, InetAddress addr2) {
		EndpointState ep1 = getEndpointStateForEndpoint(addr1);
		EndpointState ep2 = getEndpointStateForEndpoint(addr2);
		assert (ep1 != null) && (ep2 != null);
		return 0;
	}

	void notifyFailureDetector(Map<InetAddress, EndpointState> remoteEpStateMap) {
		for (Map.Entry<InetAddress, EndpointState> entry : remoteEpStateMap.entrySet()) {
			notifyFailureDetector(entry.getKey(), entry.getValue());
		}
	}

	void notifyFailureDetector(InetAddress endpoint, EndpointState remoteEndpointState) {
		EndpointState localEndpointState = endpointStateMap.get(endpoint);
		if (localEndpointState != null) {
			IFailureDetector fd = FailureDetector.instance;
		}
	}

	private void markAlive(final InetAddress addr, final EndpointState localState) {
		if ((MessagingService.instance().getVersion(addr)) < (MessagingService.VERSION_20)) {
			realMarkAlive(addr, localState);
			return;
		}
		MessageOut<EchoMessage> echoMessage = new MessageOut<EchoMessage>(ECHO, EchoMessage.instance, EchoMessage.serializer);
		Gossiper.logger.trace("Sending a EchoMessage to {}", addr);
		IAsyncCallback echoHandler = new IAsyncCallback() {
			public boolean isLatencyForSnitch() {
				return false;
			}

			public void response(MessageIn msg) {
				realMarkAlive(addr, localState);
			}
		};
		MessagingService.instance().sendRR(echoMessage, addr, echoHandler);
	}

	@VisibleForTesting
	public void realMarkAlive(final InetAddress addr, final EndpointState localState) {
		if (Gossiper.logger.isTraceEnabled())
			Gossiper.logger.trace("marking as alive {}", addr);

		liveEndpoints.add(addr);
		unreachableEndpoints.remove(addr);
		expireTimeEndpointMap.remove(addr);
		Gossiper.logger.debug("removing expire time for endpoint : {}", addr);
		Gossiper.logger.info("InetAddress {} is now UP", addr);
		for (IEndpointStateChangeSubscriber subscriber : subscribers)
			subscriber.onAlive(addr, localState);

		if (Gossiper.logger.isTraceEnabled())
			Gossiper.logger.trace("Notified {}", subscribers);

	}

	@VisibleForTesting
	public void markDead(InetAddress addr, EndpointState localState) {
		if (Gossiper.logger.isTraceEnabled())
			Gossiper.logger.trace("marking as down {}", addr);

		liveEndpoints.remove(addr);
		unreachableEndpoints.put(addr, System.nanoTime());
		Gossiper.logger.info("InetAddress {} is now DOWN", addr);
		for (IEndpointStateChangeSubscriber subscriber : subscribers)
			subscriber.onDead(addr, localState);

		if (Gossiper.logger.isTraceEnabled())
			Gossiper.logger.trace("Notified {}", subscribers);

	}

	private void handleMajorStateChange(InetAddress ep, EndpointState epState) {
		EndpointState localEpState = endpointStateMap.get(ep);
		if (!(isDeadState(epState))) {
			if (localEpState != null)
				Gossiper.logger.info("Node {} has restarted, now UP", ep);
			else
				Gossiper.logger.info("Node {} is now part of the cluster", ep);

		}
		if (Gossiper.logger.isTraceEnabled())
			Gossiper.logger.trace("Adding endpoint state for {}", ep);

		endpointStateMap.put(ep, epState);
		if (localEpState != null) {
			for (IEndpointStateChangeSubscriber subscriber : subscribers)
				subscriber.onRestart(ep, localEpState);

		}
		if (!(isDeadState(epState)))
			markAlive(ep, epState);
		else {
			Gossiper.logger.debug("Not marking {} alive due to dead state", ep);
			markDead(ep, epState);
		}
		for (IEndpointStateChangeSubscriber subscriber : subscribers)
			subscriber.onJoin(ep, epState);

		if (isShutdown(ep))
			markAsShutdown(ep);

	}

	public boolean isAlive(InetAddress endpoint) {
		EndpointState epState = getEndpointStateForEndpoint(endpoint);
		if (epState == null)
			return false;

		return (epState.isAlive()) && (!(isDeadState(epState)));
	}

	public boolean isDeadState(EndpointState epState) {
		String status = Gossiper.getGossipStatus(epState);
		if (status.isEmpty())
			return false;

		return Gossiper.DEAD_STATES.contains(status);
	}

	public boolean isSilentShutdownState(EndpointState epState) {
		String status = Gossiper.getGossipStatus(epState);
		if (status.isEmpty())
			return false;

		return Gossiper.SILENT_SHUTDOWN_STATES.contains(status);
	}

	private static String getGossipStatus(EndpointState epState) {
		if ((epState == null) || ((epState.getApplicationState(ApplicationState.STATUS)) == null))
			return "";

		String value = epState.getApplicationState(ApplicationState.STATUS).value;
		String[] pieces = value.split(VersionedValue.DELIMITER_STR, (-1));
		assert (pieces.length) > 0;
		return pieces[0];
	}

	void applyStateLocally(Map<InetAddress, EndpointState> epStateMap) {
		for (Map.Entry<InetAddress, EndpointState> entry : epStateMap.entrySet()) {
			InetAddress ep = entry.getKey();
			if ((ep.equals(FBUtilities.getBroadcastAddress())) && (!(isInShadowRound())))
				continue;

			if (justRemovedEndpoints.containsKey(ep)) {
				if (Gossiper.logger.isTraceEnabled())
					Gossiper.logger.trace("Ignoring gossip for {} because it is quarantined", ep);

				continue;
			}
			EndpointState localEpStatePtr = endpointStateMap.get(ep);
			EndpointState remoteState = entry.getValue();
			if (localEpStatePtr != null) {
				long localTime = (System.currentTimeMillis()) / 1000;
				if (Gossiper.logger.isTraceEnabled()) {
				}
			}else {
				FailureDetector.instance.report(ep);
				handleMajorStateChange(ep, remoteState);
			}
		}
		boolean any30 = anyEndpointOn30();
		if (any30 != (anyNodeOn30)) {
			Gossiper.logger.info((any30 ? "There is at least one 3.0 node in the cluster - will store and announce compatible schema version" : "There are no 3.0 nodes in the cluster - will store and announce real schema version"));
			anyNodeOn30 = any30;
			Gossiper.executor.submit(Schema.instance::updateVersionAndAnnounce);
		}
	}

	private boolean anyEndpointOn30() {
		return endpointStateMap.values().stream().map(EndpointState::getReleaseVersion).filter(Objects::nonNull).anyMatch(CassandraVersion::is30);
	}

	private void applyNewStates(InetAddress addr, EndpointState localState, EndpointState remoteState) {
		if (Gossiper.logger.isTraceEnabled()) {
		}
		Set<Map.Entry<ApplicationState, VersionedValue>> remoteStates = remoteState.states();
		localState.addApplicationStates(remoteStates);
		for (Map.Entry<ApplicationState, VersionedValue> remoteEntry : remoteStates)
			doOnChangeNotifications(addr, remoteEntry.getKey(), remoteEntry.getValue());

	}

	private void doBeforeChangeNotifications(InetAddress addr, EndpointState epState, ApplicationState apState, VersionedValue newValue) {
		for (IEndpointStateChangeSubscriber subscriber : subscribers) {
			subscriber.beforeChange(addr, epState, apState, newValue);
		}
	}

	private void doOnChangeNotifications(InetAddress addr, ApplicationState state, VersionedValue value) {
		for (IEndpointStateChangeSubscriber subscriber : subscribers) {
			subscriber.onChange(addr, state, value);
		}
	}

	private void requestAll(GossipDigest gDigest, List<GossipDigest> deltaGossipDigestList, int remoteGeneration) {
		if (Gossiper.logger.isTraceEnabled()) {
		}
	}

	private void sendAll(GossipDigest gDigest, Map<InetAddress, EndpointState> deltaEpStateMap, int maxRemoteVersion) {
	}

	void examineGossiper(List<GossipDigest> gDigestList, List<GossipDigest> deltaGossipDigestList, Map<InetAddress, EndpointState> deltaEpStateMap) {
		if ((gDigestList.size()) == 0) {
			Gossiper.logger.debug("Shadow request received, adding all states");
			for (Map.Entry<InetAddress, EndpointState> entry : endpointStateMap.entrySet()) {
			}
		}
		for (GossipDigest gDigest : gDigestList) {
		}
	}

	public void start(int generationNumber) {
		start(generationNumber, new EnumMap<ApplicationState, VersionedValue>(ApplicationState.class));
	}

	public void start(int generationNbr, Map<ApplicationState, VersionedValue> preloadLocalStates) {
		buildSeedsList();
		maybeInitializeLocalState(generationNbr);
		EndpointState localState = endpointStateMap.get(FBUtilities.getBroadcastAddress());
		localState.addApplicationStates(preloadLocalStates);
		DatabaseDescriptor.getEndpointSnitch().gossiperStarting();
		if (Gossiper.logger.isTraceEnabled()) {
		}
		scheduledGossipTask = Gossiper.executor.scheduleWithFixedDelay(new Gossiper.GossipTask(), Gossiper.intervalInMillis, Gossiper.intervalInMillis, TimeUnit.MILLISECONDS);
	}

	public synchronized Map<InetAddress, EndpointState> doShadowRound() {
		buildSeedsList();
		if (seeds.isEmpty())
			return endpointShadowStateMap;

		seedsInShadowRound.clear();
		endpointShadowStateMap.clear();
		List<GossipDigest> gDigests = new ArrayList<GossipDigest>();
		GossipDigestSyn digestSynMessage = new GossipDigestSyn(DatabaseDescriptor.getClusterName(), DatabaseDescriptor.getPartitionerName(), gDigests);
		MessageOut<GossipDigestSyn> message = new MessageOut<GossipDigestSyn>(GOSSIP_DIGEST_SYN, digestSynMessage, GossipDigestSyn.serializer);
		inShadowRound = true;
		int slept = 0;
		try {
			while (true) {
				if ((slept % 5000) == 0) {
					Gossiper.logger.trace("Sending shadow round GOSSIP DIGEST SYN to seeds {}", seeds);
					for (InetAddress seed : seeds)
						MessagingService.instance().sendOneWay(message, seed);

				}
				Thread.sleep(1000);
				if (!(inShadowRound))
					break;

				slept += 1000;
				if (slept > (StorageService.RING_DELAY)) {
					if (!(DatabaseDescriptor.getSeeds().contains(FBUtilities.getBroadcastAddress())))
						throw new RuntimeException("Unable to gossip with any seeds");

					Gossiper.logger.warn("Unable to gossip with any seeds but continuing since node is in its own seed list");
					inShadowRound = false;
					break;
				}
			} 
		} catch (InterruptedException wtf) {
			throw new RuntimeException(wtf);
		}
		return ImmutableMap.copyOf(endpointShadowStateMap);
	}

	@VisibleForTesting
	void buildSeedsList() {
		for (InetAddress seed : DatabaseDescriptor.getSeeds()) {
			if (seed.equals(FBUtilities.getBroadcastAddress()))
				continue;

			seeds.add(seed);
		}
	}

	public void maybeInitializeLocalState(int generationNbr) {
	}

	public void forceNewerGeneration() {
		EndpointState epstate = endpointStateMap.get(FBUtilities.getBroadcastAddress());
	}

	public void addSavedEndpoint(InetAddress ep) {
		if (ep.equals(FBUtilities.getBroadcastAddress())) {
			Gossiper.logger.debug("Attempt to add self as saved endpoint");
			return;
		}
		EndpointState epState = endpointStateMap.get(ep);
		if (epState != null) {
			Gossiper.logger.debug("not replacing a previous epState for {}, but reusing it: {}", ep, epState);
		}else {
		}
		endpointStateMap.put(ep, epState);
		unreachableEndpoints.put(ep, System.nanoTime());
		if (Gossiper.logger.isTraceEnabled()) {
		}
	}

	private void addLocalApplicationStateInternal(ApplicationState state, VersionedValue value) {
		assert Gossiper.taskLock.isHeldByCurrentThread();
		EndpointState epState = endpointStateMap.get(FBUtilities.getBroadcastAddress());
		InetAddress epAddr = FBUtilities.getBroadcastAddress();
		assert epState != null;
		doBeforeChangeNotifications(epAddr, epState, state, value);
		value = StorageService.instance.valueFactory.cloneWithHigherVersion(value);
		epState.addApplicationState(state, value);
		doOnChangeNotifications(epAddr, state, value);
	}

	public void addLocalApplicationState(ApplicationState applicationState, VersionedValue value) {
		addLocalApplicationStates(Arrays.asList(Pair.create(applicationState, value)));
	}

	public void addLocalApplicationStates(List<Pair<ApplicationState, VersionedValue>> states) {
		Gossiper.taskLock.lock();
		try {
			for (Pair<ApplicationState, VersionedValue> pair : states) {
				addLocalApplicationStateInternal(pair.left, pair.right);
			}
		} finally {
			Gossiper.taskLock.unlock();
		}
	}

	public void stop() {
		EndpointState mystate = endpointStateMap.get(FBUtilities.getBroadcastAddress());
		if (((mystate != null) && (!(isSilentShutdownState(mystate)))) && (StorageService.instance.isJoined())) {
			Gossiper.logger.info("Announcing shutdown");
			addLocalApplicationState(ApplicationState.STATUS, StorageService.instance.valueFactory.shutdown(true));
			MessageOut message = new MessageOut(GOSSIP_SHUTDOWN);
			for (InetAddress ep : liveEndpoints)
				MessagingService.instance().sendOneWay(message, ep);

			Uninterruptibles.sleepUninterruptibly(Integer.getInteger("cassandra.shutdown_announce_in_ms", 2000), TimeUnit.MILLISECONDS);
		}else
			Gossiper.logger.warn("No local state, state is in silent shutdown, or node hasn't joined, not announcing shutdown");

		if ((scheduledGossipTask) != null)
			scheduledGossipTask.cancel(false);

	}

	public boolean isEnabled() {
		return ((scheduledGossipTask) != null) && (!(scheduledGossipTask.isCancelled()));
	}

	public boolean isAnyNodeOn30() {
		return anyNodeOn30;
	}

	protected void maybeFinishShadowRound(InetAddress respondent, boolean isInShadowRound, Map<InetAddress, EndpointState> epStateMap) {
		if (inShadowRound) {
			if (!isInShadowRound) {
				Gossiper.logger.debug("Received a regular ack from {}, can now exit shadow round", respondent);
				endpointShadowStateMap.putAll(epStateMap);
				inShadowRound = false;
				seedsInShadowRound.clear();
			}else {
				Gossiper.logger.debug("Received an ack from {} indicating it is also in shadow round", respondent);
				seedsInShadowRound.add(respondent);
				if (seedsInShadowRound.containsAll(seeds)) {
					Gossiper.logger.debug("All seeds are in a shadow round, clearing this node to exit its own");
					inShadowRound = false;
					seedsInShadowRound.clear();
				}
			}
		}
	}

	protected boolean isInShadowRound() {
		return inShadowRound;
	}

	@VisibleForTesting
	public void initializeNodeUnsafe(InetAddress addr, UUID uuid, int generationNbr) {
		Map<ApplicationState, VersionedValue> states = new EnumMap<>(ApplicationState.class);
		states.put(ApplicationState.NET_VERSION, StorageService.instance.valueFactory.networkVersion());
		states.put(ApplicationState.HOST_ID, StorageService.instance.valueFactory.hostId(uuid));
	}

	@VisibleForTesting
	public void injectApplicationState(InetAddress endpoint, ApplicationState state, VersionedValue value) {
		EndpointState localState = endpointStateMap.get(endpoint);
		localState.addApplicationState(state, value);
	}

	public long getEndpointDowntime(String address) throws UnknownHostException {
		return getEndpointDowntime(InetAddress.getByName(address));
	}

	public int getCurrentGenerationNumber(String address) throws UnknownHostException {
		return getCurrentGenerationNumber(InetAddress.getByName(address));
	}

	public void addExpireTimeForEndpoint(InetAddress endpoint, long expireTime) {
		if (Gossiper.logger.isDebugEnabled()) {
			Gossiper.logger.debug("adding expire time for endpoint : {} ({})", endpoint, expireTime);
		}
		expireTimeEndpointMap.put(endpoint, expireTime);
	}

	public static long computeExpireTime() {
		return (System.currentTimeMillis()) + (Gossiper.aVeryLongTime);
	}

	@javax.annotation.Nullable
	public CassandraVersion getReleaseVersion(InetAddress ep) {
		EndpointState state = getEndpointStateForEndpoint(ep);
		return state != null ? state.getReleaseVersion() : null;
	}

	@javax.annotation.Nullable
	public UUID getSchemaVersion(InetAddress ep) {
		EndpointState state = getEndpointStateForEndpoint(ep);
		return state != null ? state.getSchemaVersion() : null;
	}

	public static void waitToSettle() {
		int forceAfter = Integer.getInteger("cassandra.skip_wait_for_gossip_to_settle", (-1));
		if (forceAfter == 0) {
			return;
		}
		final int GOSSIP_SETTLE_MIN_WAIT_MS = 5000;
		final int GOSSIP_SETTLE_POLL_INTERVAL_MS = 1000;
		final int GOSSIP_SETTLE_POLL_SUCCESSES_REQUIRED = 3;
		Gossiper.logger.info("Waiting for gossip to settle...");
		Uninterruptibles.sleepUninterruptibly(GOSSIP_SETTLE_MIN_WAIT_MS, TimeUnit.MILLISECONDS);
		int totalPolls = 0;
		int numOkay = 0;
		int epSize = Gossiper.instance.getEndpointStates().size();
		while (numOkay < GOSSIP_SETTLE_POLL_SUCCESSES_REQUIRED) {
			Uninterruptibles.sleepUninterruptibly(GOSSIP_SETTLE_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
			int currentSize = Gossiper.instance.getEndpointStates().size();
			totalPolls++;
			if (currentSize == epSize) {
				Gossiper.logger.debug("Gossip looks settled.");
				numOkay++;
			}else {
				Gossiper.logger.info("Gossip not settled after {} polls.", totalPolls);
				numOkay = 0;
			}
			epSize = currentSize;
			if ((forceAfter > 0) && (totalPolls > forceAfter)) {
				Gossiper.logger.warn("Gossip not settled but startup forced by cassandra.skip_wait_for_gossip_to_settle. Gossip total polls: {}", totalPolls);
				break;
			}
		} 
		if (totalPolls > GOSSIP_SETTLE_POLL_SUCCESSES_REQUIRED)
			Gossiper.logger.info("Gossip settled after {} extra polls; proceeding", (totalPolls - GOSSIP_SETTLE_POLL_SUCCESSES_REQUIRED));
		else
			Gossiper.logger.info("No gossip backlog; proceeding");

	}
}

