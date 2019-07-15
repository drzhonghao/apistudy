

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Set;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.streaming.messages.StreamInitMessage;
import org.apache.cassandra.streaming.messages.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IncomingStreamingConnection extends Thread implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(IncomingStreamingConnection.class);

	private final int version;

	public final Socket socket;

	private final Set<Closeable> group;

	public IncomingStreamingConnection(int version, Socket socket, Set<Closeable> group) {
		super(("STREAM-INIT-" + (socket.getRemoteSocketAddress())));
		this.version = version;
		this.socket = socket;
		this.group = group;
	}

	@Override
	@SuppressWarnings("resource")
	public void run() {
		try {
			if ((version) != (StreamMessage.CURRENT_VERSION))
				throw new IOException(String.format("Received stream using protocol version %d (my version %d). Terminating connection", version, StreamMessage.CURRENT_VERSION));

			DataInputPlus input = new DataInputPlus.DataInputStreamPlus(socket.getInputStream());
			StreamInitMessage init = StreamInitMessage.serializer.deserialize(input, version);
			if (!(init.isForOutgoing))
				socket.setSoTimeout(DatabaseDescriptor.getStreamingSocketTimeout());

		} catch (Throwable t) {
			IncomingStreamingConnection.logger.error("Error while reading from socket from {}.", socket.getRemoteSocketAddress(), t);
			close();
		}
	}

	@Override
	public void close() {
		try {
			if (!(socket.isClosed())) {
				socket.close();
			}
		} catch (IOException e) {
			IncomingStreamingConnection.logger.debug("Error closing socket", e);
		} finally {
			group.remove(this);
		}
	}
}

