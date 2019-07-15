

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.service.ActiveRepairService;
import org.apache.cassandra.streaming.DefaultConnectionFactory;
import org.apache.cassandra.streaming.StreamConnectionFactory;
import org.apache.cassandra.streaming.StreamCoordinator;
import org.apache.cassandra.streaming.StreamEventHandler;
import org.apache.cassandra.streaming.StreamResultFuture;
import org.apache.cassandra.streaming.StreamSession;
import org.apache.cassandra.utils.UUIDGen;


public class StreamPlan {
	public static final String[] EMPTY_COLUMN_FAMILIES = new String[0];

	private final UUID planId = UUIDGen.getTimeUUID();

	private final String description;

	private final List<StreamEventHandler> handlers = new ArrayList<>();

	private final long repairedAt;

	private final StreamCoordinator coordinator;

	private boolean flushBeforeTransfer = true;

	public StreamPlan(String description) {
		this(description, ActiveRepairService.UNREPAIRED_SSTABLE, 1, false, false, false);
	}

	public StreamPlan(String description, boolean keepSSTableLevels, boolean connectSequentially) {
		this(description, ActiveRepairService.UNREPAIRED_SSTABLE, 1, keepSSTableLevels, false, connectSequentially);
	}

	public StreamPlan(String description, long repairedAt, int connectionsPerHost, boolean keepSSTableLevels, boolean isIncremental, boolean connectSequentially) {
		this.description = description;
		this.repairedAt = repairedAt;
		this.coordinator = new StreamCoordinator(connectionsPerHost, keepSSTableLevels, isIncremental, new DefaultConnectionFactory(), connectSequentially);
	}

	public StreamPlan requestRanges(InetAddress from, InetAddress connecting, String keyspace, Collection<Range<Token>> ranges) {
		return requestRanges(from, connecting, keyspace, ranges, StreamPlan.EMPTY_COLUMN_FAMILIES);
	}

	public StreamPlan requestRanges(InetAddress from, InetAddress connecting, String keyspace, Collection<Range<Token>> ranges, String... columnFamilies) {
		StreamSession session = coordinator.getOrCreateNextSession(from, connecting);
		session.addStreamRequest(keyspace, ranges, Arrays.asList(columnFamilies), repairedAt);
		return this;
	}

	public StreamPlan transferRanges(InetAddress to, String keyspace, Collection<Range<Token>> ranges, String... columnFamilies) {
		return transferRanges(to, to, keyspace, ranges, columnFamilies);
	}

	public StreamPlan transferRanges(InetAddress to, InetAddress connecting, String keyspace, Collection<Range<Token>> ranges) {
		return transferRanges(to, connecting, keyspace, ranges, StreamPlan.EMPTY_COLUMN_FAMILIES);
	}

	public StreamPlan transferRanges(InetAddress to, InetAddress connecting, String keyspace, Collection<Range<Token>> ranges, String... columnFamilies) {
		StreamSession session = coordinator.getOrCreateNextSession(to, connecting);
		session.addTransferRanges(keyspace, ranges, Arrays.asList(columnFamilies), flushBeforeTransfer, repairedAt);
		return this;
	}

	public StreamPlan transferFiles(InetAddress to, Collection<StreamSession.SSTableStreamingSections> sstableDetails) {
		coordinator.transferFiles(to, sstableDetails);
		return this;
	}

	public StreamPlan listeners(StreamEventHandler handler, StreamEventHandler... handlers) {
		this.handlers.add(handler);
		if (handlers != null)
			Collections.addAll(this.handlers, handlers);

		return this;
	}

	public StreamPlan connectionFactory(StreamConnectionFactory factory) {
		this.coordinator.setConnectionFactory(factory);
		return this;
	}

	public boolean isEmpty() {
		return !(coordinator.hasActiveSessions());
	}

	public StreamResultFuture execute() {
		return null;
	}

	public StreamPlan flushBeforeTransfer(boolean flushBeforeTransfer) {
		this.flushBeforeTransfer = flushBeforeTransfer;
		return this;
	}
}

