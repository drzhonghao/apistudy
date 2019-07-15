

import com.codahale.metrics.Counter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.cassandra.concurrent.DebuggableScheduledThreadPoolExecutor;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.commitlog.CommitLogPosition;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.lifecycle.SSTableIntervalTree;
import org.apache.cassandra.db.lifecycle.SSTableSet;
import org.apache.cassandra.db.lifecycle.View;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.gms.ApplicationState;
import org.apache.cassandra.gms.EndpointState;
import org.apache.cassandra.gms.Gossiper;
import org.apache.cassandra.gms.IEndpointStateChangeSubscriber;
import org.apache.cassandra.gms.VersionedValue;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.SSTableMultiWriter;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.metadata.StatsMetadata;
import org.apache.cassandra.metrics.StreamingMetrics;
import org.apache.cassandra.service.ActiveRepairService;
import org.apache.cassandra.streaming.ConnectionHandler;
import org.apache.cassandra.streaming.ProgressInfo;
import org.apache.cassandra.streaming.SessionInfo;
import org.apache.cassandra.streaming.StreamConnectionFactory;
import org.apache.cassandra.streaming.StreamReceiveTask;
import org.apache.cassandra.streaming.StreamRequest;
import org.apache.cassandra.streaming.StreamResultFuture;
import org.apache.cassandra.streaming.StreamSummary;
import org.apache.cassandra.streaming.StreamTask;
import org.apache.cassandra.streaming.StreamTransferTask;
import org.apache.cassandra.streaming.messages.CompleteMessage;
import org.apache.cassandra.streaming.messages.FileMessageHeader;
import org.apache.cassandra.streaming.messages.IncomingFileMessage;
import org.apache.cassandra.streaming.messages.KeepAliveMessage;
import org.apache.cassandra.streaming.messages.OutgoingFileMessage;
import org.apache.cassandra.streaming.messages.PrepareMessage;
import org.apache.cassandra.streaming.messages.ReceivedMessage;
import org.apache.cassandra.streaming.messages.SessionFailedMessage;
import org.apache.cassandra.streaming.messages.StreamMessage;
import org.apache.cassandra.utils.CassandraVersion;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.concurrent.Ref;
import org.apache.cassandra.utils.concurrent.Refs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.streaming.messages.StreamMessage.Type.COMPLETE;
import static org.apache.cassandra.streaming.messages.StreamMessage.Type.FILE;
import static org.apache.cassandra.streaming.messages.StreamMessage.Type.PREPARE;
import static org.apache.cassandra.streaming.messages.StreamMessage.Type.RECEIVED;
import static org.apache.cassandra.streaming.messages.StreamMessage.Type.SESSION_FAILED;


public class StreamSession implements IEndpointStateChangeSubscriber {
	private static final CassandraVersion STREAM_KEEP_ALIVE = new CassandraVersion("3.10");

	private static final Logger logger = LoggerFactory.getLogger(StreamSession.class);

	private static final DebuggableScheduledThreadPoolExecutor keepAliveExecutor = new DebuggableScheduledThreadPoolExecutor("StreamKeepAliveExecutor");

	static {
		StreamSession.keepAliveExecutor.setRemoveOnCancelPolicy(true);
	}

	public final InetAddress peer;

	private final int index;

	public final InetAddress connecting;

	private StreamResultFuture streamResult;

	protected final Set<StreamRequest> requests = Sets.newConcurrentHashSet();

	@VisibleForTesting
	protected final ConcurrentHashMap<UUID, StreamTransferTask> transfers = new ConcurrentHashMap<>();

	private final Map<UUID, StreamReceiveTask> receivers = new ConcurrentHashMap<>();

	private final StreamingMetrics metrics;

	private final StreamConnectionFactory factory;

	public final Map<String, Set<Range<Token>>> transferredRangesPerKeyspace = new HashMap<>();

	public final ConnectionHandler handler;

	private AtomicBoolean isAborted = new AtomicBoolean(false);

	private final boolean keepSSTableLevel;

	private final boolean isIncremental;

	private ScheduledFuture<?> keepAliveFuture = null;

	public static enum State {

		INITIALIZED,
		PREPARING,
		STREAMING,
		WAIT_COMPLETE,
		COMPLETE,
		FAILED;}

	private volatile StreamSession.State state = StreamSession.State.INITIALIZED;

	private volatile boolean completeSent = false;

	public StreamSession(InetAddress peer, InetAddress connecting, StreamConnectionFactory factory, int index, boolean keepSSTableLevel, boolean isIncremental) {
		this.peer = peer;
		this.connecting = connecting;
		this.index = index;
		this.factory = factory;
		this.metrics = StreamingMetrics.get(connecting);
		this.keepSSTableLevel = keepSSTableLevel;
		this.isIncremental = isIncremental;
		handler = null;
	}

	public UUID planId() {
		return (streamResult) == null ? null : streamResult.planId;
	}

	public int sessionIndex() {
		return index;
	}

	public String description() {
		return (streamResult) == null ? null : streamResult.description;
	}

	public boolean keepSSTableLevel() {
		return keepSSTableLevel;
	}

	public boolean isIncremental() {
		return isIncremental;
	}

	public LifecycleTransaction getTransaction(UUID cfId) {
		assert receivers.containsKey(cfId);
		return receivers.get(cfId).getTransaction();
	}

	private boolean isKeepAliveSupported() {
		CassandraVersion peerVersion = Gossiper.instance.getReleaseVersion(peer);
		return StreamSession.STREAM_KEEP_ALIVE.isSupportedBy(peerVersion);
	}

	public void init(StreamResultFuture streamResult) {
		this.streamResult = streamResult;
		if (isKeepAliveSupported())
			scheduleKeepAliveTask();
		else
			StreamSession.logger.debug("Peer {} does not support keep-alive.", peer);

	}

	public void start() {
		if ((requests.isEmpty()) && (transfers.isEmpty())) {
			StreamSession.logger.info("[Stream #{}] Session does not have any tasks.", planId());
			closeSession(StreamSession.State.COMPLETE);
			return;
		}
		try {
			StreamSession.logger.info("[Stream #{}] Starting streaming to {}{}", planId(), peer, (peer.equals(connecting) ? "" : " through " + (connecting)));
			handler.initiate();
			onInitializationComplete();
		} catch (Exception e) {
			JVMStabilityInspector.inspectThrowable(e);
			onError(e);
		}
	}

	public Socket createConnection() throws IOException {
		assert (factory) != null;
		return factory.createConnection(connecting);
	}

	public void addStreamRequest(String keyspace, Collection<Range<Token>> ranges, Collection<String> columnFamilies, long repairedAt) {
		requests.add(new StreamRequest(keyspace, ranges, columnFamilies, repairedAt));
	}

	public synchronized void addTransferRanges(String keyspace, Collection<Range<Token>> ranges, Collection<String> columnFamilies, boolean flushTables, long repairedAt) {
		failIfFinished();
		Collection<ColumnFamilyStore> stores = getColumnFamilyStores(keyspace, columnFamilies);
		if (flushTables)
			flushSSTables(stores);

		List<Range<Token>> normalizedRanges = Range.normalize(ranges);
		List<StreamSession.SSTableStreamingSections> sections = StreamSession.getSSTableSectionsForRanges(normalizedRanges, stores, repairedAt, isIncremental);
		try {
			addTransferFiles(sections);
			Set<Range<Token>> toBeUpdated = transferredRangesPerKeyspace.get(keyspace);
			if (toBeUpdated == null) {
				toBeUpdated = new HashSet<>();
			}
			toBeUpdated.addAll(ranges);
			transferredRangesPerKeyspace.put(keyspace, toBeUpdated);
		} finally {
			for (StreamSession.SSTableStreamingSections release : sections)
				release.ref.release();

		}
	}

	private void failIfFinished() {
		if (((state()) == (StreamSession.State.COMPLETE)) || ((state()) == (StreamSession.State.FAILED)))
			throw new RuntimeException(String.format("Stream %s is finished with state %s", planId(), state().name()));

	}

	private Collection<ColumnFamilyStore> getColumnFamilyStores(String keyspace, Collection<String> columnFamilies) {
		Collection<ColumnFamilyStore> stores = new HashSet<>();
		if (columnFamilies.isEmpty()) {
			stores.addAll(Keyspace.open(keyspace).getColumnFamilyStores());
		}else {
			for (String cf : columnFamilies)
				stores.add(Keyspace.open(keyspace).getColumnFamilyStore(cf));

		}
		return stores;
	}

	@VisibleForTesting
	public static List<StreamSession.SSTableStreamingSections> getSSTableSectionsForRanges(Collection<Range<Token>> ranges, Collection<ColumnFamilyStore> stores, long overriddenRepairedAt, final boolean isIncremental) {
		Refs<SSTableReader> refs = new Refs<>();
		try {
			for (ColumnFamilyStore cfStore : stores) {
				final List<Range<PartitionPosition>> keyRanges = new ArrayList<>(ranges.size());
				for (Range<Token> range : ranges)
					keyRanges.add(Range.makeRowRange(range));

				refs.addAll(cfStore.selectAndReference(( view) -> {
					Set<SSTableReader> sstables = Sets.newHashSet();
					SSTableIntervalTree intervalTree = SSTableIntervalTree.build(view.select(SSTableSet.CANONICAL));
					for (Range<PartitionPosition> keyRange : keyRanges) {
						for (SSTableReader sstable : View.sstablesInBounds(keyRange.left, keyRange.right, intervalTree)) {
							if ((!isIncremental) || (!(sstable.isRepaired())))
								sstables.add(sstable);

						}
					}
					if (StreamSession.logger.isDebugEnabled())
						StreamSession.logger.debug("ViewFilter for {}/{} sstables", sstables.size(), Iterables.size(view.select(SSTableSet.CANONICAL)));

					return sstables;
				}).refs);
			}
			List<StreamSession.SSTableStreamingSections> sections = new ArrayList<>(refs.size());
			for (SSTableReader sstable : refs) {
				long repairedAt = overriddenRepairedAt;
				if (overriddenRepairedAt == (ActiveRepairService.UNREPAIRED_SSTABLE))
					repairedAt = sstable.getSSTableMetadata().repairedAt;

				sections.add(new StreamSession.SSTableStreamingSections(refs.get(sstable), sstable.getPositionsForRanges(ranges), sstable.estimatedKeysForRanges(ranges), repairedAt));
			}
			return sections;
		} catch (Throwable t) {
			refs.release();
			throw t;
		}
	}

	public synchronized void addTransferFiles(Collection<StreamSession.SSTableStreamingSections> sstableDetails) {
		failIfFinished();
		Iterator<StreamSession.SSTableStreamingSections> iter = sstableDetails.iterator();
		while (iter.hasNext()) {
			StreamSession.SSTableStreamingSections details = iter.next();
			if (details.sections.isEmpty()) {
				details.ref.release();
				iter.remove();
				continue;
			}
			UUID cfId = details.ref.get().metadata.cfId;
			StreamTransferTask task = transfers.get(cfId);
			if (task == null) {
				if (task == null) {
				}
			}
			task.addTransferFile(details.ref, details.estimatedKeys, details.sections, details.repairedAt);
			iter.remove();
		} 
	}

	public static class SSTableStreamingSections {
		public final Ref<SSTableReader> ref;

		public final List<Pair<Long, Long>> sections;

		public final long estimatedKeys;

		public final long repairedAt;

		public SSTableStreamingSections(Ref<SSTableReader> ref, List<Pair<Long, Long>> sections, long estimatedKeys, long repairedAt) {
			this.ref = ref;
			this.sections = sections;
			this.estimatedKeys = estimatedKeys;
			this.repairedAt = repairedAt;
		}
	}

	private synchronized void closeSession(StreamSession.State finalState) {
		if (isAborted.compareAndSet(false, true)) {
			state(finalState);
			if (finalState == (StreamSession.State.FAILED)) {
				for (StreamTask task : Iterables.concat(receivers.values(), transfers.values()))
					task.abort();

			}
			if ((keepAliveFuture) != null) {
				StreamSession.logger.debug("[Stream #{}] Finishing keep-alive task.", planId());
				keepAliveFuture.cancel(false);
				keepAliveFuture = null;
			}
			handler.close();
		}
	}

	public void state(StreamSession.State newState) {
		state = newState;
	}

	public StreamSession.State state() {
		return state;
	}

	public boolean isSuccess() {
		return (state) == (StreamSession.State.COMPLETE);
	}

	public void messageReceived(StreamMessage message) {
		switch (message.type) {
			case PREPARE :
				PrepareMessage msg = ((PrepareMessage) (message));
				prepare(msg.requests, msg.summaries);
				break;
			case FILE :
				receive(((IncomingFileMessage) (message)));
				break;
			case RECEIVED :
				ReceivedMessage received = ((ReceivedMessage) (message));
				received(received.cfId, received.sequenceNumber);
				break;
			case COMPLETE :
				complete();
				break;
			case SESSION_FAILED :
				sessionFailed();
				break;
		}
	}

	public void onInitializationComplete() {
		state(StreamSession.State.PREPARING);
		PrepareMessage prepare = new PrepareMessage();
		prepare.requests.addAll(requests);
		for (StreamTransferTask task : transfers.values())
			prepare.summaries.add(task.getSummary());

		handler.sendMessage(prepare);
		if (requests.isEmpty())
			startStreamingFiles();

	}

	public void onError(Throwable e) {
		logError(e);
		if (handler.isOutgoingConnected())
			handler.sendMessage(new SessionFailedMessage());

		closeSession(StreamSession.State.FAILED);
	}

	private void logError(Throwable e) {
		if (e instanceof SocketTimeoutException) {
			if (isKeepAliveSupported())
				StreamSession.logger.error(("[Stream #{}] Did not receive response from peer {}{} for {} secs. Is peer down? " + "If not, maybe try increasing streaming_keep_alive_period_in_secs."), planId(), peer.getHostAddress(), (peer.equals(connecting) ? "" : " through " + (connecting.getHostAddress())), (2 * (DatabaseDescriptor.getStreamingKeepAlivePeriod())), e);
			else
				StreamSession.logger.error(("[Stream #{}] Streaming socket timed out. This means the session peer stopped responding or " + (("is still processing received data. If there is no sign of failure in the other end or a very " + "dense table is being transferred you may want to increase streaming_socket_timeout_in_ms ") + "property. Current value is {}ms.")), planId(), DatabaseDescriptor.getStreamingSocketTimeout(), e);

		}else {
			StreamSession.logger.error("[Stream #{}] Streaming error occurred on session with peer {}{}", planId(), peer.getHostAddress(), (peer.equals(connecting) ? "" : " through " + (connecting.getHostAddress())), e);
		}
	}

	public void prepare(Collection<StreamRequest> requests, Collection<StreamSummary> summaries) {
		state(StreamSession.State.PREPARING);
		for (StreamRequest request : requests)
			addTransferRanges(request.keyspace, request.ranges, request.columnFamilies, true, request.repairedAt);

		for (StreamSummary summary : summaries)
			prepareReceiving(summary);

		if (!(requests.isEmpty())) {
			PrepareMessage prepare = new PrepareMessage();
			for (StreamTransferTask task : transfers.values())
				prepare.summaries.add(task.getSummary());

			handler.sendMessage(prepare);
		}
		if (!(maybeCompleted()))
			startStreamingFiles();

	}

	public void fileSent(FileMessageHeader header) {
		long headerSize = header.size();
		StreamingMetrics.totalOutgoingBytes.inc(headerSize);
		metrics.outgoingBytes.inc(headerSize);
		StreamTransferTask task = transfers.get(header.cfId);
		if (task != null) {
			task.scheduleTimeout(header.sequenceNumber, 12, TimeUnit.HOURS);
		}
	}

	public void receive(IncomingFileMessage message) {
		long headerSize = message.header.size();
		StreamingMetrics.totalIncomingBytes.inc(headerSize);
		metrics.incomingBytes.inc(headerSize);
		handler.sendMessage(new ReceivedMessage(message.header.cfId, message.header.sequenceNumber));
		receivers.get(message.header.cfId).received(message.sstable);
	}

	public void progress(String filename, ProgressInfo.Direction direction, long bytes, long total) {
		ProgressInfo progress = new ProgressInfo(peer, index, filename, direction, bytes, total);
		streamResult.handleProgress(progress);
	}

	public void received(UUID cfId, int sequenceNumber) {
		transfers.get(cfId).complete(sequenceNumber);
	}

	public synchronized void complete() {
		if ((state) == (StreamSession.State.WAIT_COMPLETE)) {
			if (!(completeSent)) {
				handler.sendMessage(new CompleteMessage());
				completeSent = true;
			}
			closeSession(StreamSession.State.COMPLETE);
		}else {
			state(StreamSession.State.WAIT_COMPLETE);
			handler.closeIncoming();
		}
	}

	private synchronized void scheduleKeepAliveTask() {
		if ((keepAliveFuture) == null) {
			int keepAlivePeriod = DatabaseDescriptor.getStreamingKeepAlivePeriod();
			StreamSession.logger.debug("[Stream #{}] Scheduling keep-alive task with {}s period.", planId(), keepAlivePeriod);
			keepAliveFuture = StreamSession.keepAliveExecutor.scheduleAtFixedRate(new StreamSession.KeepAliveTask(), 0, keepAlivePeriod, TimeUnit.SECONDS);
		}
	}

	public synchronized void sessionFailed() {
		StreamSession.logger.error("[Stream #{}] Remote peer {} failed stream session.", planId(), peer.getHostAddress());
		closeSession(StreamSession.State.FAILED);
	}

	public SessionInfo getSessionInfo() {
		List<StreamSummary> receivingSummaries = Lists.newArrayList();
		for (StreamTask receiver : receivers.values())
			receivingSummaries.add(receiver.getSummary());

		List<StreamSummary> transferSummaries = Lists.newArrayList();
		for (StreamTask transfer : transfers.values())
			transferSummaries.add(transfer.getSummary());

		return null;
	}

	public synchronized void taskCompleted(StreamReceiveTask completedTask) {
		maybeCompleted();
	}

	public synchronized void taskCompleted(StreamTransferTask completedTask) {
		maybeCompleted();
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
		StreamSession.logger.error("[Stream #{}] Session failed because remote peer {} has left.", planId(), peer.getHostAddress());
		closeSession(StreamSession.State.FAILED);
	}

	public void onRestart(InetAddress endpoint, EndpointState epState) {
		StreamSession.logger.error("[Stream #{}] Session failed because remote peer {} was restarted.", planId(), peer.getHostAddress());
		closeSession(StreamSession.State.FAILED);
	}

	private boolean maybeCompleted() {
		boolean completed = (receivers.isEmpty()) && (transfers.isEmpty());
		if (completed) {
			if ((state) == (StreamSession.State.WAIT_COMPLETE)) {
				if (!(completeSent)) {
					handler.sendMessage(new CompleteMessage());
					completeSent = true;
				}
				closeSession(StreamSession.State.COMPLETE);
			}else {
				handler.sendMessage(new CompleteMessage());
				completeSent = true;
				state(StreamSession.State.WAIT_COMPLETE);
				handler.closeOutgoing();
			}
		}
		return completed;
	}

	private void flushSSTables(Iterable<ColumnFamilyStore> stores) {
		List<Future<?>> flushes = new ArrayList<>();
		for (ColumnFamilyStore cfs : stores)
			flushes.add(cfs.forceFlush());

		FBUtilities.waitOnFutures(flushes);
	}

	private synchronized void prepareReceiving(StreamSummary summary) {
		failIfFinished();
		if ((summary.files) > 0) {
		}
	}

	private void startStreamingFiles() {
		state(StreamSession.State.STREAMING);
		for (StreamTransferTask task : transfers.values()) {
			Collection<OutgoingFileMessage> messages = task.getFileMessages();
			if ((messages.size()) > 0)
				handler.sendMessages(messages);
			else
				taskCompleted(task);

		}
	}

	class KeepAliveTask implements Runnable {
		private KeepAliveMessage last = null;

		public void run() {
			if (((last) == null) || (last.wasSent())) {
				StreamSession.logger.trace("[Stream #{}] Sending keep-alive to {}.", planId(), peer);
				last = new KeepAliveMessage();
				try {
					handler.sendMessage(last);
				} catch (RuntimeException e) {
					StreamSession.logger.debug("[Stream #{}] Could not send keep-alive message (perhaps stream session is finished?).", planId(), e);
				}
			}else {
				StreamSession.logger.trace("[Stream #{}] Skip sending keep-alive to {} (previous was not yet sent).", planId(), peer);
			}
		}
	}
}

