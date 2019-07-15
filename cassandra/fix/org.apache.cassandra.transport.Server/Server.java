

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Version;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.EncryptionOptions;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.security.SSLFactory;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.cassandra.service.IEndpointLifecycleSubscriber;
import org.apache.cassandra.service.MigrationListener;
import org.apache.cassandra.service.MigrationManager;
import org.apache.cassandra.service.NativeTransportService;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.transport.CBUtil;
import org.apache.cassandra.transport.Connection;
import org.apache.cassandra.transport.Event;
import org.apache.cassandra.transport.Event.Type;
import org.apache.cassandra.transport.Frame;
import org.apache.cassandra.transport.Message;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.transport.ServerConnection;
import org.apache.cassandra.transport.messages.EventMessage;
import org.apache.cassandra.utils.FBUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.transport.Event.SchemaChange.Change.CREATED;
import static org.apache.cassandra.transport.Event.SchemaChange.Change.DROPPED;
import static org.apache.cassandra.transport.Event.SchemaChange.Change.UPDATED;
import static org.apache.cassandra.transport.Event.SchemaChange.Target.AGGREGATE;
import static org.apache.cassandra.transport.Event.SchemaChange.Target.FUNCTION;
import static org.apache.cassandra.transport.Event.SchemaChange.Target.TABLE;
import static org.apache.cassandra.transport.Event.SchemaChange.Target.TYPE;
import static org.apache.cassandra.transport.Event.StatusChange.nodeDown;
import static org.apache.cassandra.transport.Event.StatusChange.nodeUp;
import static org.apache.cassandra.transport.Event.TopologyChange.movedNode;
import static org.apache.cassandra.transport.Event.TopologyChange.newNode;
import static org.apache.cassandra.transport.Event.TopologyChange.removedNode;
import static org.apache.cassandra.transport.Event.Type.values;


public class Server implements CassandraDaemon.Server {
	static {
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
	}

	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	private static final boolean useEpoll = NativeTransportService.useEpoll();

	private final Server.ConnectionTracker connectionTracker = new Server.ConnectionTracker();

	private final Connection.Factory connectionFactory = new Connection.Factory() {
		public Connection newConnection(Channel channel, ProtocolVersion version) {
			return new ServerConnection(channel, version, connectionTracker);
		}
	};

	public final InetSocketAddress socket;

	public boolean useSSL = false;

	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	private EventLoopGroup workerGroup;

	private EventExecutor eventExecutorGroup;

	private Server(Server.Builder builder) {
		this.socket = builder.getSocket();
		this.useSSL = builder.useSSL;
		if ((builder.workerGroup) != null) {
			workerGroup = builder.workerGroup;
		}else {
			if (Server.useEpoll)
				workerGroup = new EpollEventLoopGroup();
			else
				workerGroup = new NioEventLoopGroup();

		}
		if ((builder.eventExecutorGroup) != null)
			eventExecutorGroup = builder.eventExecutorGroup;

		Server.EventNotifier notifier = new Server.EventNotifier(this);
		StorageService.instance.register(notifier);
		MigrationManager.instance.register(notifier);
	}

	public void stop() {
		if (isRunning.compareAndSet(true, false))
			close();

	}

	public boolean isRunning() {
		return isRunning.get();
	}

	public synchronized void start() {
		if (isRunning())
			return;

		ServerBootstrap bootstrap = new ServerBootstrap().channel((Server.useEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)).childOption(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.SO_LINGER, 0).childOption(ChannelOption.SO_KEEPALIVE, DatabaseDescriptor.getRpcKeepAlive()).childOption(ChannelOption.ALLOCATOR, CBUtil.allocator).childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, (32 * 1024)).childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, (8 * 1024));
		if ((workerGroup) != null)
			bootstrap = bootstrap.group(workerGroup);

		if (this.useSSL) {
			final EncryptionOptions.ClientEncryptionOptions clientEnc = DatabaseDescriptor.getClientEncryptionOptions();
			if (clientEnc.optional) {
				Server.logger.info("Enabling optionally encrypted CQL connections between client and server");
				bootstrap.childHandler(new Server.OptionalSecureInitializer(this, clientEnc));
			}else {
				Server.logger.info("Enabling encrypted CQL connections between client and server");
				bootstrap.childHandler(new Server.SecureInitializer(this, clientEnc));
			}
		}else {
			bootstrap.childHandler(new Server.Initializer(this));
		}
		Server.logger.info("Using Netty Version: {}", Version.identify().entrySet());
		Server.logger.info("Starting listening for CQL clients on {} ({})...", socket, (this.useSSL ? "encrypted" : "unencrypted"));
		ChannelFuture bindFuture = bootstrap.bind(socket);
		if (!(bindFuture.awaitUninterruptibly().isSuccess()))
			throw new IllegalStateException(String.format("Failed to bind port %d on %s.", socket.getPort(), socket.getAddress().getHostAddress()));

		connectionTracker.allChannels.add(bindFuture.channel());
		isRunning.set(true);
	}

	public int getConnectedClients() {
		return connectionTracker.getConnectedClients();
	}

	private void close() {
		connectionTracker.closeAll();
		Server.logger.info("Stop listening for CQL clients");
	}

	public static class Builder {
		private EventLoopGroup workerGroup;

		private EventExecutor eventExecutorGroup;

		private boolean useSSL = false;

		private InetAddress hostAddr;

		private int port = -1;

		private InetSocketAddress socket;

		public Server.Builder withSSL(boolean useSSL) {
			this.useSSL = useSSL;
			return this;
		}

		public Server.Builder withEventLoopGroup(EventLoopGroup eventLoopGroup) {
			this.workerGroup = eventLoopGroup;
			return this;
		}

		public Server.Builder withEventExecutor(EventExecutor eventExecutor) {
			this.eventExecutorGroup = eventExecutor;
			return this;
		}

		public Server.Builder withHost(InetAddress host) {
			this.hostAddr = host;
			this.socket = null;
			return this;
		}

		public Server.Builder withPort(int port) {
			this.port = port;
			this.socket = null;
			return this;
		}

		public Server build() {
			return new Server(this);
		}

		private InetSocketAddress getSocket() {
			if ((this.socket) != null)
				return this.socket;
			else {
				if ((this.port) == (-1))
					throw new IllegalStateException("Missing port number");

				if ((this.hostAddr) != null)
					this.socket = new InetSocketAddress(this.hostAddr, this.port);
				else
					throw new IllegalStateException("Missing host");

				return this.socket;
			}
		}
	}

	public static class ConnectionTracker implements Connection.Tracker {
		public final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

		private final EnumMap<Event.Type, ChannelGroup> groups = new EnumMap<>(Event.Type.class);

		public ConnectionTracker() {
			for (Event.Type type : values())
				groups.put(type, new DefaultChannelGroup(type.toString(), GlobalEventExecutor.INSTANCE));

		}

		public void addConnection(Channel ch, Connection connection) {
			allChannels.add(ch);
		}

		public void register(Event.Type type, Channel ch) {
			groups.get(type).add(ch);
		}

		public void send(Event event) {
			groups.get(event.type).writeAndFlush(new EventMessage(event));
		}

		public void closeAll() {
			allChannels.close().awaitUninterruptibly();
		}

		public int getConnectedClients() {
			return (allChannels.size()) != 0 ? (allChannels.size()) - 1 : 0;
		}
	}

	private static class Initializer extends ChannelInitializer<Channel> {
		private static final Message.ProtocolDecoder messageDecoder = new Message.ProtocolDecoder();

		private static final Message.ProtocolEncoder messageEncoder = new Message.ProtocolEncoder();

		private static final Frame.Decompressor frameDecompressor = new Frame.Decompressor();

		private static final Frame.Compressor frameCompressor = new Frame.Compressor();

		private static final Frame.Encoder frameEncoder = new Frame.Encoder();

		private static final Message.ExceptionHandler exceptionHandler = new Message.ExceptionHandler();

		private static final Message.Dispatcher dispatcher = new Message.Dispatcher();

		private final Server server;

		public Initializer(Server server) {
			this.server = server;
		}

		protected void initChannel(Channel channel) throws Exception {
			ChannelPipeline pipeline = channel.pipeline();
			if (((DatabaseDescriptor.getNativeTransportMaxConcurrentConnections()) > 0) || ((DatabaseDescriptor.getNativeTransportMaxConcurrentConnectionsPerIp()) > 0)) {
			}
			pipeline.addLast("frameDecoder", new Frame.Decoder(server.connectionFactory));
			pipeline.addLast("frameEncoder", Server.Initializer.frameEncoder);
			pipeline.addLast("frameDecompressor", Server.Initializer.frameDecompressor);
			pipeline.addLast("frameCompressor", Server.Initializer.frameCompressor);
			pipeline.addLast("messageDecoder", Server.Initializer.messageDecoder);
			pipeline.addLast("messageEncoder", Server.Initializer.messageEncoder);
			pipeline.addLast("exceptionHandler", Server.Initializer.exceptionHandler);
			if ((server.eventExecutorGroup) != null)
				pipeline.addLast(server.eventExecutorGroup, "executor", Server.Initializer.dispatcher);
			else
				pipeline.addLast("executor", Server.Initializer.dispatcher);

		}
	}

	protected static abstract class AbstractSecureIntializer extends Server.Initializer {
		private final SSLContext sslContext;

		private final EncryptionOptions encryptionOptions;

		protected AbstractSecureIntializer(Server server, EncryptionOptions encryptionOptions) {
			super(server);
			this.encryptionOptions = encryptionOptions;
			try {
				this.sslContext = SSLFactory.createSSLContext(encryptionOptions, encryptionOptions.require_client_auth);
			} catch (IOException e) {
				throw new RuntimeException("Failed to setup secure pipeline", e);
			}
		}

		protected final SslHandler createSslHandler() {
			SSLEngine sslEngine = sslContext.createSSLEngine();
			sslEngine.setUseClientMode(false);
			String[] suites = SSLFactory.filterCipherSuites(sslEngine.getSupportedCipherSuites(), encryptionOptions.cipher_suites);
			sslEngine.setEnabledCipherSuites(suites);
			sslEngine.setNeedClientAuth(encryptionOptions.require_client_auth);
			return new SslHandler(sslEngine);
		}
	}

	private static class OptionalSecureInitializer extends Server.AbstractSecureIntializer {
		public OptionalSecureInitializer(Server server, EncryptionOptions encryptionOptions) {
			super(server, encryptionOptions);
		}

		protected void initChannel(final Channel channel) throws Exception {
			super.initChannel(channel);
			channel.pipeline().addFirst("sslDetectionHandler", new ByteToMessageDecoder() {
				@Override
				protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
					if ((byteBuf.readableBytes()) < 5) {
						return;
					}
					if (SslHandler.isEncrypted(byteBuf)) {
						SslHandler sslHandler = createSslHandler();
						channelHandlerContext.pipeline().replace(this, "ssl", sslHandler);
					}else {
						channelHandlerContext.pipeline().remove(this);
					}
				}
			});
		}
	}

	private static class SecureInitializer extends Server.AbstractSecureIntializer {
		public SecureInitializer(Server server, EncryptionOptions encryptionOptions) {
			super(server, encryptionOptions);
		}

		protected void initChannel(Channel channel) throws Exception {
			SslHandler sslHandler = createSslHandler();
			super.initChannel(channel);
			channel.pipeline().addFirst("ssl", sslHandler);
		}
	}

	private static class LatestEvent {
		public final Event.StatusChange.Status status;

		public final Event.TopologyChange.Change topology;

		private LatestEvent(Event.StatusChange.Status status, Event.TopologyChange.Change topology) {
			this.status = status;
			this.topology = topology;
		}

		@Override
		public String toString() {
			return String.format("Status %s, Topology %s", status, topology);
		}

		public static Server.LatestEvent forStatusChange(Event.StatusChange.Status status, Server.LatestEvent prev) {
			return new Server.LatestEvent(status, (prev == null ? null : prev.topology));
		}

		public static Server.LatestEvent forTopologyChange(Event.TopologyChange.Change change, Server.LatestEvent prev) {
			return new Server.LatestEvent((prev == null ? null : prev.status), change);
		}
	}

	private static class EventNotifier extends MigrationListener implements IEndpointLifecycleSubscriber {
		private final Server server;

		private final Map<InetAddress, Server.LatestEvent> latestEvents = new ConcurrentHashMap<>();

		private final Set<InetAddress> endpointsPendingJoinedNotification = ConcurrentHashMap.newKeySet();

		private static final InetAddress bindAll;

		static {
			try {
				bindAll = InetAddress.getByAddress(new byte[4]);
			} catch (UnknownHostException e) {
				throw new AssertionError(e);
			}
		}

		private EventNotifier(Server server) {
			this.server = server;
		}

		private InetAddress getRpcAddress(InetAddress endpoint) {
			try {
				InetAddress rpcAddress = InetAddress.getByName(StorageService.instance.getRpcaddress(endpoint));
				return rpcAddress.equals(Server.EventNotifier.bindAll) ? endpoint : rpcAddress;
			} catch (UnknownHostException e) {
				Server.logger.error("Problem retrieving RPC address for {}", endpoint, e);
				return endpoint;
			}
		}

		private void send(InetAddress endpoint, Event.NodeEvent event) {
			if (Server.logger.isTraceEnabled())
				Server.logger.trace("Sending event for endpoint {}, rpc address {}", endpoint, event.nodeAddress());

			if ((!(endpoint.equals(FBUtilities.getBroadcastAddress()))) && (event.nodeAddress().equals(FBUtilities.getBroadcastRpcAddress())))
				return;

			send(event);
		}

		private void send(Event event) {
			server.connectionTracker.send(event);
		}

		public void onJoinCluster(InetAddress endpoint) {
			if (!(StorageService.instance.isRpcReady(endpoint)))
				endpointsPendingJoinedNotification.add(endpoint);
			else
				onTopologyChange(endpoint, newNode(getRpcAddress(endpoint), server.socket.getPort()));

		}

		public void onLeaveCluster(InetAddress endpoint) {
			onTopologyChange(endpoint, removedNode(getRpcAddress(endpoint), server.socket.getPort()));
		}

		public void onMove(InetAddress endpoint) {
			onTopologyChange(endpoint, movedNode(getRpcAddress(endpoint), server.socket.getPort()));
		}

		public void onUp(InetAddress endpoint) {
			if (endpointsPendingJoinedNotification.remove(endpoint))
				onJoinCluster(endpoint);

			onStatusChange(endpoint, nodeUp(getRpcAddress(endpoint), server.socket.getPort()));
		}

		public void onDown(InetAddress endpoint) {
			onStatusChange(endpoint, nodeDown(getRpcAddress(endpoint), server.socket.getPort()));
		}

		private void onTopologyChange(InetAddress endpoint, Event.TopologyChange event) {
			if (Server.logger.isTraceEnabled())
				Server.logger.trace("Topology changed event : {}, {}", endpoint, event.change);

			Server.LatestEvent prev = latestEvents.get(endpoint);
			if ((prev == null) || ((prev.topology) != (event.change))) {
				Server.LatestEvent ret = latestEvents.put(endpoint, Server.LatestEvent.forTopologyChange(event.change, prev));
				if (ret == prev)
					send(endpoint, event);

			}
		}

		private void onStatusChange(InetAddress endpoint, Event.StatusChange event) {
			if (Server.logger.isTraceEnabled())
				Server.logger.trace("Status changed event : {}, {}", endpoint, event.status);

			Server.LatestEvent prev = latestEvents.get(endpoint);
			if ((prev == null) || ((prev.status) != (event.status))) {
				Server.LatestEvent ret = latestEvents.put(endpoint, Server.LatestEvent.forStatusChange(event.status, null));
				if (ret == prev)
					send(endpoint, event);

			}
		}

		public void onCreateKeyspace(String ksName) {
			send(new Event.SchemaChange(CREATED, ksName));
		}

		public void onCreateColumnFamily(String ksName, String cfName) {
			send(new Event.SchemaChange(CREATED, TABLE, ksName, cfName));
		}

		public void onCreateUserType(String ksName, String typeName) {
			send(new Event.SchemaChange(CREATED, TYPE, ksName, typeName));
		}

		public void onCreateFunction(String ksName, String functionName, List<AbstractType<?>> argTypes) {
			send(new Event.SchemaChange(CREATED, FUNCTION, ksName, functionName, AbstractType.asCQLTypeStringList(argTypes)));
		}

		public void onCreateAggregate(String ksName, String aggregateName, List<AbstractType<?>> argTypes) {
			send(new Event.SchemaChange(CREATED, AGGREGATE, ksName, aggregateName, AbstractType.asCQLTypeStringList(argTypes)));
		}

		public void onUpdateKeyspace(String ksName) {
			send(new Event.SchemaChange(UPDATED, ksName));
		}

		public void onUpdateColumnFamily(String ksName, String cfName, boolean affectsStatements) {
			send(new Event.SchemaChange(UPDATED, TABLE, ksName, cfName));
		}

		public void onUpdateUserType(String ksName, String typeName) {
			send(new Event.SchemaChange(UPDATED, TYPE, ksName, typeName));
		}

		public void onUpdateFunction(String ksName, String functionName, List<AbstractType<?>> argTypes) {
			send(new Event.SchemaChange(UPDATED, FUNCTION, ksName, functionName, AbstractType.asCQLTypeStringList(argTypes)));
		}

		public void onUpdateAggregate(String ksName, String aggregateName, List<AbstractType<?>> argTypes) {
			send(new Event.SchemaChange(UPDATED, AGGREGATE, ksName, aggregateName, AbstractType.asCQLTypeStringList(argTypes)));
		}

		public void onDropKeyspace(String ksName) {
			send(new Event.SchemaChange(DROPPED, ksName));
		}

		public void onDropColumnFamily(String ksName, String cfName) {
			send(new Event.SchemaChange(DROPPED, TABLE, ksName, cfName));
		}

		public void onDropUserType(String ksName, String typeName) {
			send(new Event.SchemaChange(DROPPED, TYPE, ksName, typeName));
		}

		public void onDropFunction(String ksName, String functionName, List<AbstractType<?>> argTypes) {
			send(new Event.SchemaChange(DROPPED, FUNCTION, ksName, functionName, AbstractType.asCQLTypeStringList(argTypes)));
		}

		public void onDropAggregate(String ksName, String aggregateName, List<AbstractType<?>> argTypes) {
			send(new Event.SchemaChange(DROPPED, AGGREGATE, ksName, aggregateName, AbstractType.asCQLTypeStringList(argTypes)));
		}
	}
}

