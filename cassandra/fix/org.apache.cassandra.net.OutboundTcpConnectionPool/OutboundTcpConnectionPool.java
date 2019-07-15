

import com.codahale.metrics.Meter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocket;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.EncryptionOptions;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.locator.IEndpointSnitch;
import org.apache.cassandra.metrics.ConnectionMetrics;
import org.apache.cassandra.net.BackPressureState;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.OutboundTcpConnection;
import org.apache.cassandra.security.SSLFactory;
import org.apache.cassandra.utils.FBUtilities;

import static org.apache.cassandra.config.EncryptionOptions.ServerEncryptionOptions.InternodeEncryption.all;
import static org.apache.cassandra.config.EncryptionOptions.ServerEncryptionOptions.InternodeEncryption.dc;
import static org.apache.cassandra.config.EncryptionOptions.ServerEncryptionOptions.InternodeEncryption.none;
import static org.apache.cassandra.config.EncryptionOptions.ServerEncryptionOptions.InternodeEncryption.rack;


public class OutboundTcpConnectionPool {
	public static final long LARGE_MESSAGE_THRESHOLD = Long.getLong(((Config.PROPERTY_PREFIX) + "otcp_large_message_threshold"), (1024 * 64));

	private final InetAddress id;

	private final CountDownLatch started;

	public final OutboundTcpConnection smallMessages;

	public final OutboundTcpConnection largeMessages;

	public final OutboundTcpConnection gossipMessages;

	private InetAddress resetEndpoint;

	private ConnectionMetrics metrics;

	private final BackPressureState backPressureState;

	OutboundTcpConnectionPool(InetAddress remoteEp, BackPressureState backPressureState) {
		id = remoteEp;
		resetEndpoint = SystemKeyspace.getPreferredIP(remoteEp);
		started = new CountDownLatch(1);
		this.backPressureState = backPressureState;
		smallMessages = null;
		gossipMessages = null;
		largeMessages = null;
	}

	OutboundTcpConnection getConnection(MessageOut msg) {
		if ((Stage.GOSSIP) == (msg.getStage()))
			return gossipMessages;

		return (msg.payloadSize(smallMessages.getTargetVersion())) > (OutboundTcpConnectionPool.LARGE_MESSAGE_THRESHOLD) ? largeMessages : smallMessages;
	}

	public BackPressureState getBackPressureState() {
		return backPressureState;
	}

	void reset() {
		for (OutboundTcpConnection conn : new OutboundTcpConnection[]{ smallMessages, largeMessages, gossipMessages }) {
		}
	}

	public void resetToNewerVersion(int version) {
		for (OutboundTcpConnection conn : new OutboundTcpConnection[]{ smallMessages, largeMessages, gossipMessages }) {
		}
	}

	public void reset(InetAddress remoteEP) {
		SystemKeyspace.updatePreferredIP(id, remoteEP);
		resetEndpoint = remoteEP;
		for (OutboundTcpConnection conn : new OutboundTcpConnection[]{ smallMessages, largeMessages, gossipMessages }) {
		}
		metrics.release();
	}

	public long getTimeouts() {
		return metrics.timeouts.getCount();
	}

	public void incrementTimeout() {
		metrics.timeouts.mark();
	}

	public Socket newSocket() throws IOException {
		return OutboundTcpConnectionPool.newSocket(endPoint());
	}

	@SuppressWarnings("resource")
	public static Socket newSocket(InetAddress endpoint) throws IOException {
		if (OutboundTcpConnectionPool.isEncryptedChannel(endpoint)) {
			return SSLFactory.getSocket(DatabaseDescriptor.getServerEncryptionOptions(), endpoint, DatabaseDescriptor.getSSLStoragePort());
		}else {
			SocketChannel channel = SocketChannel.open();
			channel.connect(new InetSocketAddress(endpoint, DatabaseDescriptor.getStoragePort()));
			return channel.socket();
		}
	}

	public InetAddress endPoint() {
		if (id.equals(FBUtilities.getBroadcastAddress()))
			return FBUtilities.getLocalAddress();

		return resetEndpoint;
	}

	public static boolean isEncryptedChannel(InetAddress address) {
		IEndpointSnitch snitch = DatabaseDescriptor.getEndpointSnitch();
		switch (DatabaseDescriptor.getServerEncryptionOptions().internode_encryption) {
			case none :
				return false;
			case all :
				break;
			case dc :
				if (snitch.getDatacenter(address).equals(snitch.getDatacenter(FBUtilities.getBroadcastAddress())))
					return false;

				break;
			case rack :
				if ((snitch.getRack(address).equals(snitch.getRack(FBUtilities.getBroadcastAddress()))) && (snitch.getDatacenter(address).equals(snitch.getDatacenter(FBUtilities.getBroadcastAddress()))))
					return false;

				break;
		}
		return true;
	}

	public void start() {
		smallMessages.start();
		largeMessages.start();
		gossipMessages.start();
		started.countDown();
	}

	public void waitForStarted() {
		if ((started.getCount()) == 0)
			return;

		boolean error = false;
		try {
			if (!(started.await(1, TimeUnit.MINUTES)))
				error = true;

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			error = true;
		}
		if (error)
			throw new IllegalStateException(String.format("Connections to %s are not started!", id.getHostAddress()));

	}

	public void close() {
		metrics.release();
	}
}

