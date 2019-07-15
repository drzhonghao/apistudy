

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.google.common.collect.Iterables;
import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ReadRepairDecision;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.exceptions.ReadFailureException;
import org.apache.cassandra.exceptions.ReadTimeoutException;
import org.apache.cassandra.exceptions.UnavailableException;
import org.apache.cassandra.metrics.ReadRepairMetrics;
import org.apache.cassandra.metrics.TableMetrics;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.SpeculativeRetryParam;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.service.DigestMismatchException;
import org.apache.cassandra.service.DigestResolver;
import org.apache.cassandra.service.ReadCallback;
import org.apache.cassandra.service.ResponseResolver;
import org.apache.cassandra.service.StorageProxy;
import org.apache.cassandra.tracing.TraceState;
import org.apache.cassandra.tracing.Tracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractReadExecutor {
	private static final Logger logger = LoggerFactory.getLogger(AbstractReadExecutor.class);

	protected final ReadCommand command;

	protected final List<InetAddress> targetReplicas;

	protected final ReadCallback handler;

	protected final TraceState traceState;

	AbstractReadExecutor(Keyspace keyspace, ReadCommand command, ConsistencyLevel consistencyLevel, List<InetAddress> targetReplicas, long queryStartNanoTime) {
		this.command = command;
		this.targetReplicas = targetReplicas;
		this.handler = new ReadCallback(new DigestResolver(keyspace, command, consistencyLevel, targetReplicas.size()), consistencyLevel, command, targetReplicas, queryStartNanoTime);
		this.traceState = Tracing.instance.get();
		int digestVersion = MessagingService.current_version;
		for (InetAddress replica : targetReplicas)
			digestVersion = Math.min(digestVersion, MessagingService.instance().getVersion(replica));

		command.setDigestVersion(digestVersion);
	}

	protected void makeDataRequests(Iterable<InetAddress> endpoints) {
		makeRequests(command, endpoints);
	}

	protected void makeDigestRequests(Iterable<InetAddress> endpoints) {
		makeRequests(command.copyAsDigestQuery(), endpoints);
	}

	private void makeRequests(ReadCommand readCommand, Iterable<InetAddress> endpoints) {
		boolean hasLocalEndpoint = false;
		for (InetAddress endpoint : endpoints) {
			if (StorageProxy.canDoLocalRequest(endpoint)) {
				hasLocalEndpoint = true;
				continue;
			}
			if ((traceState) != null)
				traceState.trace("reading {} from {}", (readCommand.isDigestQuery() ? "digest" : "data"), endpoint);

			AbstractReadExecutor.logger.trace("reading {} from {}", (readCommand.isDigestQuery() ? "digest" : "data"), endpoint);
			MessageOut<ReadCommand> message = readCommand.createMessage(MessagingService.instance().getVersion(endpoint));
			MessagingService.instance().sendRRWithFailure(message, endpoint, handler);
		}
		if (hasLocalEndpoint) {
			AbstractReadExecutor.logger.trace("reading {} locally", (readCommand.isDigestQuery() ? "digest" : "data"));
		}
	}

	public abstract void maybeTryAdditionalReplicas();

	public abstract Collection<InetAddress> getContactedReplicas();

	public abstract void executeAsync();

	public PartitionIterator get() throws ReadFailureException, ReadTimeoutException, DigestMismatchException {
		return handler.get();
	}

	public static AbstractReadExecutor getReadExecutor(SinglePartitionReadCommand command, ConsistencyLevel consistencyLevel, long queryStartNanoTime) throws UnavailableException {
		Keyspace keyspace = Keyspace.open(command.metadata().ksName);
		List<InetAddress> allReplicas = StorageProxy.getLiveSortedEndpoints(keyspace, command.partitionKey());
		ReadRepairDecision repairDecision = (consistencyLevel == (ConsistencyLevel.EACH_QUORUM)) ? ReadRepairDecision.NONE : command.metadata().newReadRepairDecision();
		List<InetAddress> targetReplicas = consistencyLevel.filterForQuery(keyspace, allReplicas, repairDecision);
		consistencyLevel.assureSufficientLiveNodes(keyspace, targetReplicas);
		if (repairDecision != (ReadRepairDecision.NONE)) {
			Tracing.trace("Read-repair {}", repairDecision);
			ReadRepairMetrics.attempted.mark();
		}
		ColumnFamilyStore cfs = keyspace.getColumnFamilyStore(command.metadata().cfId);
		SpeculativeRetryParam retry = cfs.metadata.params.speculativeRetry;
		if (((retry.equals(SpeculativeRetryParam.NONE)) || (consistencyLevel == (ConsistencyLevel.EACH_QUORUM))) || ((consistencyLevel.blockFor(keyspace)) == (allReplicas.size())))
			return new AbstractReadExecutor.NeverSpeculatingReadExecutor(keyspace, command, consistencyLevel, targetReplicas, queryStartNanoTime);

		if ((targetReplicas.size()) == (allReplicas.size())) {
			return new AbstractReadExecutor.AlwaysSpeculatingReadExecutor(keyspace, cfs, command, consistencyLevel, targetReplicas, queryStartNanoTime);
		}
		InetAddress extraReplica = allReplicas.get(targetReplicas.size());
		if ((repairDecision == (ReadRepairDecision.DC_LOCAL)) && (targetReplicas.contains(extraReplica))) {
			for (InetAddress address : allReplicas) {
				if (!(targetReplicas.contains(address))) {
					extraReplica = address;
					break;
				}
			}
		}
		targetReplicas.add(extraReplica);
		if (retry.equals(SpeculativeRetryParam.ALWAYS))
			return new AbstractReadExecutor.AlwaysSpeculatingReadExecutor(keyspace, cfs, command, consistencyLevel, targetReplicas, queryStartNanoTime);
		else
			return new AbstractReadExecutor.SpeculatingReadExecutor(keyspace, cfs, command, consistencyLevel, targetReplicas, queryStartNanoTime);

	}

	public static class NeverSpeculatingReadExecutor extends AbstractReadExecutor {
		public NeverSpeculatingReadExecutor(Keyspace keyspace, ReadCommand command, ConsistencyLevel consistencyLevel, List<InetAddress> targetReplicas, long queryStartNanoTime) {
			super(keyspace, command, consistencyLevel, targetReplicas, queryStartNanoTime);
		}

		public void executeAsync() {
			makeDataRequests(targetReplicas.subList(0, 1));
			if ((targetReplicas.size()) > 1)
				makeDigestRequests(targetReplicas.subList(1, targetReplicas.size()));

		}

		public void maybeTryAdditionalReplicas() {
		}

		public Collection<InetAddress> getContactedReplicas() {
			return targetReplicas;
		}
	}

	private static class SpeculatingReadExecutor extends AbstractReadExecutor {
		private final ColumnFamilyStore cfs;

		private volatile boolean speculated = false;

		public SpeculatingReadExecutor(Keyspace keyspace, ColumnFamilyStore cfs, ReadCommand command, ConsistencyLevel consistencyLevel, List<InetAddress> targetReplicas, long queryStartNanoTime) {
			super(keyspace, command, consistencyLevel, targetReplicas, queryStartNanoTime);
			this.cfs = cfs;
		}

		public void executeAsync() {
			List<InetAddress> initialReplicas = targetReplicas.subList(0, ((targetReplicas.size()) - 1));
		}

		public void maybeTryAdditionalReplicas() {
			if ((cfs.sampleLatencyNanos) > (TimeUnit.MILLISECONDS.toNanos(command.getTimeout())))
				return;

			if (!(handler.await(cfs.sampleLatencyNanos, TimeUnit.NANOSECONDS))) {
				ReadCommand retryCommand = command;
				if (handler.resolver.isDataPresent())
					retryCommand = command.copyAsDigestQuery();

				InetAddress extraReplica = Iterables.getLast(targetReplicas);
				if ((traceState) != null)
					traceState.trace("speculating read retry on {}", extraReplica);

				AbstractReadExecutor.logger.trace("speculating read retry on {}", extraReplica);
				int version = MessagingService.instance().getVersion(extraReplica);
				MessagingService.instance().sendRRWithFailure(retryCommand.createMessage(version), extraReplica, handler);
				speculated = true;
				cfs.metric.speculativeRetries.inc();
			}
		}

		public Collection<InetAddress> getContactedReplicas() {
			return speculated ? targetReplicas : targetReplicas.subList(0, ((targetReplicas.size()) - 1));
		}
	}

	private static class AlwaysSpeculatingReadExecutor extends AbstractReadExecutor {
		private final ColumnFamilyStore cfs;

		public AlwaysSpeculatingReadExecutor(Keyspace keyspace, ColumnFamilyStore cfs, ReadCommand command, ConsistencyLevel consistencyLevel, List<InetAddress> targetReplicas, long queryStartNanoTime) {
			super(keyspace, command, consistencyLevel, targetReplicas, queryStartNanoTime);
			this.cfs = cfs;
		}

		public void maybeTryAdditionalReplicas() {
		}

		public Collection<InetAddress> getContactedReplicas() {
			return targetReplicas;
		}

		@Override
		public void executeAsync() {
			makeDataRequests(targetReplicas.subList(0, ((targetReplicas.size()) > 1 ? 2 : 1)));
			if ((targetReplicas.size()) > 2)
				makeDigestRequests(targetReplicas.subList(2, targetReplicas.size()));

			cfs.metric.speculativeRetries.inc();
		}
	}
}

