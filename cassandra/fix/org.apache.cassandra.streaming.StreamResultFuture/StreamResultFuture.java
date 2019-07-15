

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.Futures;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.cassandra.net.IncomingStreamingConnection;
import org.apache.cassandra.streaming.ConnectionHandler;
import org.apache.cassandra.streaming.DefaultConnectionFactory;
import org.apache.cassandra.streaming.ProgressInfo;
import org.apache.cassandra.streaming.SessionInfo;
import org.apache.cassandra.streaming.StreamCoordinator;
import org.apache.cassandra.streaming.StreamEvent;
import org.apache.cassandra.streaming.StreamEventHandler;
import org.apache.cassandra.streaming.StreamException;
import org.apache.cassandra.streaming.StreamSession;
import org.apache.cassandra.streaming.StreamState;
import org.apache.cassandra.utils.FBUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class StreamResultFuture extends AbstractFuture<StreamState> {
	private static final Logger logger = LoggerFactory.getLogger(StreamResultFuture.class);

	public final UUID planId;

	public final String description;

	private final StreamCoordinator coordinator;

	private final Collection<StreamEventHandler> eventListeners = new ConcurrentLinkedQueue<>();

	private StreamResultFuture(UUID planId, String description, StreamCoordinator coordinator) {
		this.planId = planId;
		this.description = description;
		this.coordinator = coordinator;
		if ((!(coordinator.isReceiving())) && (!(coordinator.hasActiveSessions())))
			set(getCurrentState());

	}

	private StreamResultFuture(UUID planId, String description, boolean keepSSTableLevels, boolean isIncremental) {
		this(planId, description, new StreamCoordinator(0, keepSSTableLevels, isIncremental, new DefaultConnectionFactory(), false));
	}

	static StreamResultFuture init(UUID planId, String description, Collection<StreamEventHandler> listeners, StreamCoordinator coordinator) {
		StreamResultFuture future = StreamResultFuture.createAndRegister(planId, description, coordinator);
		if (listeners != null) {
			for (StreamEventHandler listener : listeners)
				future.addEventListener(listener);

		}
		StreamResultFuture.logger.info("[Stream #{}] Executing streaming plan for {}", planId, description);
		for (final StreamSession session : coordinator.getAllStreamSessions()) {
		}
		return future;
	}

	public static synchronized StreamResultFuture initReceivingSide(int sessionIndex, UUID planId, String description, InetAddress from, IncomingStreamingConnection connection, boolean isForOutgoing, int version, boolean keepSSTableLevel, boolean isIncremental) throws IOException {
		StreamResultFuture.logger.info("[Stream #{}, ID#{}] Received streaming plan for {}", planId, sessionIndex, description);
		return null;
	}

	private static StreamResultFuture createAndRegister(UUID planId, String description, StreamCoordinator coordinator) {
		StreamResultFuture future = new StreamResultFuture(planId, description, coordinator);
		return future;
	}

	private void attachConnection(InetAddress from, int sessionIndex, IncomingStreamingConnection connection, boolean isForOutgoing, int version) throws IOException {
		StreamSession session = coordinator.getOrCreateSessionById(from, sessionIndex, connection.socket.getInetAddress());
		session.handler.initiateOnReceivingSide(connection, isForOutgoing, version);
	}

	public void addEventListener(StreamEventHandler listener) {
		Futures.addCallback(this, listener);
		eventListeners.add(listener);
	}

	public StreamState getCurrentState() {
		return new StreamState(planId, description, coordinator.getAllSessionInfo());
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if ((o == null) || ((getClass()) != (o.getClass())))
			return false;

		StreamResultFuture that = ((StreamResultFuture) (o));
		return planId.equals(that.planId);
	}

	@Override
	public int hashCode() {
		return planId.hashCode();
	}

	void handleSessionPrepared(StreamSession session) {
		SessionInfo sessionInfo = session.getSessionInfo();
		StreamResultFuture.logger.info("[Stream #{} ID#{}] Prepare completed. Receiving {} files({}), sending {} files({})", session.planId(), session.sessionIndex(), sessionInfo.getTotalFilesToReceive(), FBUtilities.prettyPrintMemory(sessionInfo.getTotalSizeToReceive()), sessionInfo.getTotalFilesToSend(), FBUtilities.prettyPrintMemory(sessionInfo.getTotalSizeToSend()));
		StreamEvent.SessionPreparedEvent event = new StreamEvent.SessionPreparedEvent(planId, sessionInfo);
		coordinator.addSessionInfo(sessionInfo);
		fireStreamEvent(event);
	}

	void handleSessionComplete(StreamSession session) {
		StreamResultFuture.logger.info("[Stream #{}] Session with {} is complete", session.planId(), session.peer);
		fireStreamEvent(new StreamEvent.SessionCompleteEvent(session));
		SessionInfo sessionInfo = session.getSessionInfo();
		coordinator.addSessionInfo(sessionInfo);
		maybeComplete();
	}

	public void handleProgress(ProgressInfo progress) {
		coordinator.updateProgress(progress);
		fireStreamEvent(new StreamEvent.ProgressEvent(planId, progress));
	}

	synchronized void fireStreamEvent(StreamEvent event) {
		for (StreamEventHandler listener : eventListeners)
			listener.handleStreamEvent(event);

	}

	private synchronized void maybeComplete() {
		if (!(coordinator.hasActiveSessions())) {
			StreamState finalState = getCurrentState();
			if (finalState.hasFailedSession()) {
				StreamResultFuture.logger.warn("[Stream #{}] Stream failed", planId);
				setException(new StreamException(finalState, "Stream failed"));
			}else {
				StreamResultFuture.logger.info("[Stream #{}] All sessions completed", planId);
				set(finalState);
			}
		}
	}
}

