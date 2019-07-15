

import io.netty.util.concurrent.FastThreadLocalThread;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.UnknownColumnFamilyException;
import org.apache.cassandra.db.monitoring.ApproximateTime;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.NIODataInputStream;
import org.apache.cassandra.net.CompactEndpointSerializationHelper;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.SnappyInputStream;


public class IncomingTcpConnection extends FastThreadLocalThread implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(IncomingTcpConnection.class);

	private static final int BUFFER_SIZE = Integer.getInteger(((Config.PROPERTY_PREFIX) + ".itc_buffer_size"), (1024 * 4));

	private final int version;

	private final boolean compressed;

	private final Socket socket;

	private final Set<Closeable> group;

	public InetAddress from;

	public IncomingTcpConnection(int version, boolean compressed, Socket socket, Set<Closeable> group) {
		super(("MessagingService-Incoming-" + (socket.getInetAddress())));
		this.version = version;
		this.compressed = compressed;
		this.socket = socket;
		this.group = group;
		if ((DatabaseDescriptor.getInternodeRecvBufferSize()) > 0) {
			try {
				this.socket.setReceiveBufferSize(DatabaseDescriptor.getInternodeRecvBufferSize());
			} catch (SocketException se) {
				IncomingTcpConnection.logger.warn("Failed to set receive buffer size on internode socket.", se);
			}
		}
	}

	@Override
	public void run() {
		try {
			if ((version) < (MessagingService.VERSION_20))
				throw new UnsupportedOperationException(String.format(("Unable to read obsolete message version %s; " + "The earliest version supported is 2.0.0"), version));

			receiveMessages();
		} catch (EOFException e) {
			IncomingTcpConnection.logger.trace("eof reading from socket; closing", e);
		} catch (UnknownColumnFamilyException e) {
			IncomingTcpConnection.logger.warn("UnknownColumnFamilyException reading from socket; closing", e);
		} catch (IOException e) {
			IncomingTcpConnection.logger.trace("IOException reading from socket; closing", e);
		} finally {
			close();
		}
	}

	@Override
	public void close() {
		try {
			if (IncomingTcpConnection.logger.isTraceEnabled())
				IncomingTcpConnection.logger.trace("Closing socket {} - isclosed: {}", socket, socket.isClosed());

			if (!(socket.isClosed())) {
				socket.close();
			}
		} catch (IOException e) {
			IncomingTcpConnection.logger.trace("Error closing socket", e);
		} finally {
			group.remove(this);
		}
	}

	@SuppressWarnings("resource")
	private void receiveMessages() throws IOException {
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.writeInt(MessagingService.current_version);
		out.flush();
		DataInputPlus in = new DataInputPlus.DataInputStreamPlus(socket.getInputStream());
		int maxVersion = in.readInt();
		assert (version) <= (MessagingService.current_version);
		from = CompactEndpointSerializationHelper.deserialize(in);
		MessagingService.instance().setVersion(from, maxVersion);
		IncomingTcpConnection.logger.trace("Set version for {} to {} (will use {})", from, maxVersion, MessagingService.instance().getVersion(from));
		if (compressed) {
			IncomingTcpConnection.logger.trace("Upgrading incoming connection to be compressed");
			if ((version) < (MessagingService.VERSION_21)) {
				in = new DataInputPlus.DataInputStreamPlus(new SnappyInputStream(socket.getInputStream()));
			}else {
				LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();
			}
		}else {
			ReadableByteChannel channel = socket.getChannel();
			in = new NIODataInputStream((channel != null ? channel : Channels.newChannel(socket.getInputStream())), IncomingTcpConnection.BUFFER_SIZE);
		}
		while (true) {
			MessagingService.validateMagic(in.readInt());
			receiveMessage(in, version);
		} 
	}

	private InetAddress receiveMessage(DataInputPlus input, int version) throws IOException {
		int id;
		if (version < (MessagingService.VERSION_20))
			id = Integer.parseInt(input.readUTF());
		else
			id = input.readInt();

		long currentTime = ApproximateTime.currentTimeMillis();
		MessageIn message = MessageIn.read(input, version, id, MessageIn.readConstructionTime(from, input, currentTime));
		if (message == null) {
			return null;
		}
		if (version <= (MessagingService.current_version)) {
			MessagingService.instance().receive(message, id);
		}else {
			IncomingTcpConnection.logger.trace("Received connection from newer protocol version {}. Ignoring message", version);
		}
		return message.from;
	}
}

