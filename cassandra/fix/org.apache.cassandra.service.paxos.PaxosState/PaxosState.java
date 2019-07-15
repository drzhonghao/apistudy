

import com.google.common.util.concurrent.Striped;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.metrics.LatencyMetrics;
import org.apache.cassandra.metrics.TableMetrics;
import org.apache.cassandra.service.paxos.Commit;
import org.apache.cassandra.service.paxos.PrepareResponse;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.UUIDGen;


public class PaxosState {
	private static final Striped<Lock> LOCKS = Striped.lazyWeakLock(((DatabaseDescriptor.getConcurrentWriters()) * 1024));

	private final Commit promised;

	private final Commit accepted;

	private final Commit mostRecentCommit;

	public PaxosState(DecoratedKey key, CFMetaData metadata) {
		this(Commit.emptyCommit(key, metadata), Commit.emptyCommit(key, metadata), Commit.emptyCommit(key, metadata));
	}

	public PaxosState(Commit promised, Commit accepted, Commit mostRecentCommit) {
		assert (promised.update.partitionKey().equals(accepted.update.partitionKey())) && (accepted.update.partitionKey().equals(mostRecentCommit.update.partitionKey()));
		assert ((promised.update.metadata()) == (accepted.update.metadata())) && ((accepted.update.metadata()) == (mostRecentCommit.update.metadata()));
		this.promised = promised;
		this.accepted = accepted;
		this.mostRecentCommit = mostRecentCommit;
	}

	public static PrepareResponse prepare(Commit toPrepare) {
		long start = System.nanoTime();
		try {
			Lock lock = PaxosState.LOCKS.get(toPrepare.update.partitionKey());
			lock.lock();
			try {
				int nowInSec = UUIDGen.unixTimestampInSec(toPrepare.ballot);
			} finally {
				lock.unlock();
			}
		} finally {
			Keyspace.open(toPrepare.update.metadata().ksName).getColumnFamilyStore(toPrepare.update.metadata().cfId).metric.casPrepare.addNano(((System.nanoTime()) - start));
		}
		return null;
	}

	public static Boolean propose(Commit proposal) {
		long start = System.nanoTime();
		try {
			Lock lock = PaxosState.LOCKS.get(proposal.update.partitionKey());
			lock.lock();
			try {
				int nowInSec = UUIDGen.unixTimestampInSec(proposal.ballot);
			} finally {
				lock.unlock();
			}
		} finally {
			Keyspace.open(proposal.update.metadata().ksName).getColumnFamilyStore(proposal.update.metadata().cfId).metric.casPropose.addNano(((System.nanoTime()) - start));
		}
		return false;
	}

	public static void commit(Commit proposal) {
		long start = System.nanoTime();
		try {
			if ((UUIDGen.unixTimestamp(proposal.ballot)) >= (SystemKeyspace.getTruncatedAt(proposal.update.metadata().cfId))) {
				Tracing.trace("Committing proposal {}", proposal);
				Mutation mutation = proposal.makeMutation();
				Keyspace.open(mutation.getKeyspaceName()).apply(mutation, true);
			}else {
				Tracing.trace("Not committing proposal {} as ballot timestamp predates last truncation time", proposal);
			}
			SystemKeyspace.savePaxosCommit(proposal);
		} finally {
			Keyspace.open(proposal.update.metadata().ksName).getColumnFamilyStore(proposal.update.metadata().cfId).metric.casCommit.addNano(((System.nanoTime()) - start));
		}
	}
}

