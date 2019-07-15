

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.cassandra.concurrent.DebuggableThreadPoolExecutor;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.gms.ApplicationState;
import org.apache.cassandra.gms.EndpointState;
import org.apache.cassandra.gms.FailureDetector;
import org.apache.cassandra.gms.IEndpointStateChangeSubscriber;
import org.apache.cassandra.gms.IFailureDetectionEventListener;
import org.apache.cassandra.gms.IFailureDetector;
import org.apache.cassandra.gms.VersionedValue;
import org.apache.cassandra.repair.NodePair;
import org.apache.cassandra.repair.RemoteSyncTask;
import org.apache.cassandra.repair.RepairJobDesc;
import org.apache.cassandra.repair.RepairParallelism;
import org.apache.cassandra.repair.RepairResult;
import org.apache.cassandra.repair.RepairSessionResult;
import org.apache.cassandra.repair.SystemDistributedKeyspace;
import org.apache.cassandra.repair.ValidationTask;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.MerkleTrees;
import org.apache.cassandra.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RepairSession extends AbstractFuture<RepairSessionResult> implements IEndpointStateChangeSubscriber , IFailureDetectionEventListener {
	private static Logger logger = LoggerFactory.getLogger(RepairSession.class);

	public final UUID parentRepairSession;

	private final UUID id;

	public final String keyspace;

	private final String[] cfnames;

	public final RepairParallelism parallelismDegree;

	public final boolean pullRepair;

	public final Collection<Range<Token>> ranges;

	public final Set<InetAddress> endpoints;

	public final long repairedAt;

	private final AtomicBoolean isFailed = new AtomicBoolean(false);

	private final ConcurrentMap<Pair<RepairJobDesc, InetAddress>, ValidationTask> validating = new ConcurrentHashMap<>();

	private final ConcurrentMap<Pair<RepairJobDesc, NodePair>, RemoteSyncTask> syncingTasks = new ConcurrentHashMap<>();

	public final ListeningExecutorService taskExecutor = MoreExecutors.listeningDecorator(DebuggableThreadPoolExecutor.createCachedThreadpoolWithMaxSize("RepairJobTask"));

	private volatile boolean terminated = false;

	public RepairSession(UUID parentRepairSession, UUID id, Collection<Range<Token>> ranges, String keyspace, RepairParallelism parallelismDegree, Set<InetAddress> endpoints, long repairedAt, boolean pullRepair, String... cfnames) {
		assert (cfnames.length) > 0 : "Repairing no column families seems pointless, doesn't it";
		this.parentRepairSession = parentRepairSession;
		this.id = id;
		this.parallelismDegree = parallelismDegree;
		this.keyspace = keyspace;
		this.cfnames = cfnames;
		this.ranges = ranges;
		this.endpoints = endpoints;
		this.repairedAt = repairedAt;
		this.pullRepair = pullRepair;
	}

	public UUID getId() {
		return id;
	}

	public Collection<Range<Token>> getRanges() {
		return ranges;
	}

	public void waitForValidation(Pair<RepairJobDesc, InetAddress> key, ValidationTask task) {
		validating.put(key, task);
	}

	public void waitForSync(Pair<RepairJobDesc, NodePair> key, RemoteSyncTask task) {
		syncingTasks.put(key, task);
	}

	public void validationComplete(RepairJobDesc desc, InetAddress endpoint, MerkleTrees trees) {
		ValidationTask task = validating.remove(Pair.create(desc, endpoint));
		if (task == null) {
			assert terminated;
			return;
		}
		String message = String.format("Received merkle tree for %s from %s", desc.columnFamily, endpoint);
		RepairSession.logger.info("[repair #{}] {}", getId(), message);
		Tracing.traceRepair(message);
		task.treesReceived(trees);
	}

	public void syncComplete(RepairJobDesc desc, NodePair nodes, boolean success) {
		RemoteSyncTask task = syncingTasks.get(Pair.create(desc, nodes));
		if (task == null) {
			assert terminated;
			return;
		}
		RepairSession.logger.debug("[repair #{}] Repair completed between {} and {} on {}", getId(), nodes.endpoint1, nodes.endpoint2, desc.columnFamily);
		task.syncComplete(success);
	}

	private String repairedNodes() {
		StringBuilder sb = new StringBuilder();
		sb.append(FBUtilities.getBroadcastAddress());
		for (InetAddress ep : endpoints)
			sb.append(", ").append(ep);

		return sb.toString();
	}

	public void start(ListeningExecutorService executor) {
		String message;
		if (terminated)
			return;

		RepairSession.logger.info("[repair #{}] new session: will sync {} on range {} for {}.{}", getId(), repairedNodes(), ranges, keyspace, Arrays.toString(cfnames));
		Tracing.traceRepair("Syncing range {}", ranges);
		SystemDistributedKeyspace.startRepairs(getId(), parentRepairSession, keyspace, cfnames, ranges, endpoints);
		if (endpoints.isEmpty()) {
			RepairSession.logger.info("[repair #{}] {}", getId(), (message = String.format("No neighbors to repair with on range %s: session completed", ranges)));
			Tracing.traceRepair(message);
			set(new RepairSessionResult(id, keyspace, ranges, Lists.<RepairResult>newArrayList()));
			SystemDistributedKeyspace.failRepairs(getId(), keyspace, cfnames, new RuntimeException(message));
			return;
		}
		for (InetAddress endpoint : endpoints) {
			if (!(FailureDetector.instance.isAlive(endpoint))) {
				message = String.format("Cannot proceed on repair because a neighbor (%s) is dead: session failed", endpoint);
				RepairSession.logger.error("[repair #{}] {}", getId(), message);
				Exception e = new IOException(message);
				setException(e);
				SystemDistributedKeyspace.failRepairs(getId(), keyspace, cfnames, e);
				return;
			}
		}
		List<ListenableFuture<RepairResult>> jobs = new ArrayList<>(cfnames.length);
		for (String cfname : cfnames) {
		}
		Futures.addCallback(Futures.allAsList(jobs), new FutureCallback<List<RepairResult>>() {
			public void onSuccess(List<RepairResult> results) {
				RepairSession.logger.info("[repair #{}] {}", getId(), "Session completed successfully");
				Tracing.traceRepair("Completed sync of range {}", ranges);
				set(new RepairSessionResult(id, keyspace, ranges, results));
				taskExecutor.shutdown();
				terminate();
			}

			public void onFailure(Throwable t) {
				RepairSession.logger.error(String.format("[repair #%s] Session completed with the following error", getId()), t);
				Tracing.traceRepair("Session completed with the following error: {}", t);
				forceShutdown(t);
			}
		});
	}

	public void terminate() {
		terminated = true;
		validating.clear();
		syncingTasks.clear();
	}

	public void forceShutdown(Throwable reason) {
		setException(reason);
		taskExecutor.shutdownNow();
		terminate();
	}

	public void onJoin(InetAddress endpoint, EndpointState epState) {
	}

	public void beforeChange(InetAddress endpoint, EndpointState currentState, ApplicationState newStateKey, VersionedValue newValue) {
	}

	public void onChange(InetAddress endpoint, ApplicationState state, VersionedValue value) {
	}

	public void onAlive(InetAddress endpoint, EndpointState state) {
	}

	public void onDead(InetAddress endpoint, EndpointState state) {
	}

	public void onRemove(InetAddress endpoint) {
		convict(endpoint, Double.MAX_VALUE);
	}

	public void onRestart(InetAddress endpoint, EndpointState epState) {
		convict(endpoint, Double.MAX_VALUE);
	}

	public void convict(InetAddress endpoint, double phi) {
		if (!(endpoints.contains(endpoint)))
			return;

		if (phi < (2 * (DatabaseDescriptor.getPhiConvictThreshold())))
			return;

		if (!(isFailed.compareAndSet(false, true)))
			return;

		Exception exception = new IOException(String.format("Endpoint %s died", endpoint));
		RepairSession.logger.error(String.format("[repair #%s] session completed with the following error", getId()), exception);
		forceShutdown(exception);
	}
}

