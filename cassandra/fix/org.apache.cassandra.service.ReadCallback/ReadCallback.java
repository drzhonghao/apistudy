

import com.codahale.metrics.Meter;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.apache.cassandra.concurrent.LocalAwareExecutorService;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.PartitionRangeReadCommand;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadResponse;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.exceptions.ReadFailureException;
import org.apache.cassandra.exceptions.ReadTimeoutException;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.exceptions.RequestFailureReason;
import org.apache.cassandra.exceptions.UnavailableException;
import org.apache.cassandra.locator.IEndpointSnitch;
import org.apache.cassandra.metrics.ReadRepairMetrics;
import org.apache.cassandra.net.IAsyncCallbackWithFailure;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.service.DigestMismatchException;
import org.apache.cassandra.service.DigestResolver;
import org.apache.cassandra.service.ResponseResolver;
import org.apache.cassandra.tracing.TraceState;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.concurrent.SimpleCondition;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.net.MessagingService.Verb.INTERNAL_RESPONSE;


public class ReadCallback implements IAsyncCallbackWithFailure<ReadResponse> {
	protected static final Logger logger = LoggerFactory.getLogger(ReadCallback.class);

	public final ResponseResolver resolver;

	private final SimpleCondition condition = new SimpleCondition();

	private final long queryStartNanoTime;

	final int blockfor;

	final List<InetAddress> endpoints;

	private final ReadCommand command;

	private final ConsistencyLevel consistencyLevel;

	private static final AtomicIntegerFieldUpdater<ReadCallback> recievedUpdater = AtomicIntegerFieldUpdater.newUpdater(ReadCallback.class, "received");

	private volatile int received = 0;

	private static final AtomicIntegerFieldUpdater<ReadCallback> failuresUpdater = AtomicIntegerFieldUpdater.newUpdater(ReadCallback.class, "failures");

	private volatile int failures = 0;

	private final Map<InetAddress, RequestFailureReason> failureReasonByEndpoint;

	private final Keyspace keyspace;

	public ReadCallback(ResponseResolver resolver, ConsistencyLevel consistencyLevel, ReadCommand command, List<InetAddress> filteredEndpoints, long queryStartNanoTime) {
		this(resolver, consistencyLevel, consistencyLevel.blockFor(Keyspace.open(command.metadata().ksName)), command, Keyspace.open(command.metadata().ksName), filteredEndpoints, queryStartNanoTime);
	}

	public ReadCallback(ResponseResolver resolver, ConsistencyLevel consistencyLevel, int blockfor, ReadCommand command, Keyspace keyspace, List<InetAddress> endpoints, long queryStartNanoTime) {
		this.command = command;
		this.keyspace = keyspace;
		this.blockfor = blockfor;
		this.consistencyLevel = consistencyLevel;
		this.resolver = resolver;
		this.queryStartNanoTime = queryStartNanoTime;
		this.endpoints = endpoints;
		this.failureReasonByEndpoint = new ConcurrentHashMap<>();
		assert (!(command instanceof PartitionRangeReadCommand)) || (blockfor >= (endpoints.size()));
		if (ReadCallback.logger.isTraceEnabled())
			ReadCallback.logger.trace("Blockfor is {}; setting up requests to {}", blockfor, StringUtils.join(this.endpoints, ","));

	}

	public boolean await(long timePastStart, TimeUnit unit) {
		long time = (unit.toNanos(timePastStart)) - ((System.nanoTime()) - (queryStartNanoTime));
		try {
			return condition.await(time, TimeUnit.NANOSECONDS);
		} catch (InterruptedException ex) {
			throw new AssertionError(ex);
		}
	}

	public void awaitResults() throws ReadFailureException, ReadTimeoutException {
		boolean signaled = await(command.getTimeout(), TimeUnit.MILLISECONDS);
		boolean failed = ((blockfor) + (failures)) > (endpoints.size());
		if (signaled && (!failed))
			return;

		if (Tracing.isTracing()) {
			String gotData = ((received) > 0) ? resolver.isDataPresent() ? " (including data)" : " (only digests)" : "";
			Tracing.trace("{}; received {} of {} responses{}", new Object[]{ failed ? "Failed" : "Timed out", received, blockfor, gotData });
		}else
			if (ReadCallback.logger.isDebugEnabled()) {
				String gotData = ((received) > 0) ? resolver.isDataPresent() ? " (including data)" : " (only digests)" : "";
				ReadCallback.logger.debug("{}; received {} of {} responses{}", new Object[]{ failed ? "Failed" : "Timed out", received, blockfor, gotData });
			}

		throw failed ? new ReadFailureException(consistencyLevel, received, blockfor, resolver.isDataPresent(), failureReasonByEndpoint) : new ReadTimeoutException(consistencyLevel, received, blockfor, resolver.isDataPresent());
	}

	public PartitionIterator get() throws ReadFailureException, ReadTimeoutException, DigestMismatchException {
		awaitResults();
		PartitionIterator result = ((blockfor) == 1) ? resolver.getData() : resolver.resolve();
		if (ReadCallback.logger.isTraceEnabled())
			ReadCallback.logger.trace("Read: {} ms.", TimeUnit.NANOSECONDS.toMillis(((System.nanoTime()) - (queryStartNanoTime))));

		return result;
	}

	public int blockFor() {
		return blockfor;
	}

	public void response(MessageIn<ReadResponse> message) {
		resolver.preprocess(message);
		int n = (waitingFor(message.from)) ? ReadCallback.recievedUpdater.incrementAndGet(this) : received;
		if ((n >= (blockfor)) && (resolver.isDataPresent())) {
			condition.signalAll();
			if (((blockfor) < (endpoints.size())) && (n == (endpoints.size()))) {
				TraceState traceState = Tracing.instance.get();
				if (traceState != null)
					traceState.trace("Initiating read-repair");

				StageManager.getStage(Stage.READ_REPAIR).execute(new ReadCallback.AsyncRepairRunner(traceState, queryStartNanoTime));
			}
		}
	}

	private boolean waitingFor(InetAddress from) {
		return consistencyLevel.isDatacenterLocal() ? DatabaseDescriptor.getLocalDataCenter().equals(DatabaseDescriptor.getEndpointSnitch().getDatacenter(from)) : true;
	}

	public int getReceivedCount() {
		return received;
	}

	public void response(ReadResponse result) {
		MessageIn<ReadResponse> message = MessageIn.create(FBUtilities.getBroadcastAddress(), result, Collections.<String, byte[]>emptyMap(), INTERNAL_RESPONSE, MessagingService.current_version);
		response(message);
	}

	public void assureSufficientLiveNodes() throws UnavailableException {
		consistencyLevel.assureSufficientLiveNodes(keyspace, endpoints);
	}

	public boolean isLatencyForSnitch() {
		return true;
	}

	private class AsyncRepairRunner implements Runnable {
		private final TraceState traceState;

		private final long queryStartNanoTime;

		public AsyncRepairRunner(TraceState traceState, long queryStartNanoTime) {
			this.traceState = traceState;
			this.queryStartNanoTime = queryStartNanoTime;
		}

		public void run() {
			try {
				resolver.compareResponses();
			} catch (DigestMismatchException e) {
				assert (resolver) instanceof DigestResolver;
				if ((traceState) != null)
					traceState.trace("Digest mismatch: {}", e.toString());

				if (ReadCallback.logger.isDebugEnabled())
					ReadCallback.logger.debug("Digest mismatch:", e);

				ReadRepairMetrics.repairedBackground.mark();
				for (InetAddress endpoint : endpoints) {
					MessageOut<ReadCommand> message = command.createMessage(MessagingService.instance().getVersion(endpoint));
				}
			}
		}
	}

	@Override
	public void onFailure(InetAddress from, RequestFailureReason failureReason) {
		int n = (waitingFor(from)) ? ReadCallback.failuresUpdater.incrementAndGet(this) : failures;
		failureReasonByEndpoint.put(from, failureReason);
		if (((blockfor) + n) > (endpoints.size()))
			condition.signalAll();

	}
}

