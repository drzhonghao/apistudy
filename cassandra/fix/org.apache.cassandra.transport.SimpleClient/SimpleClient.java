

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.AbstractQueue;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.apache.cassandra.config.EncryptionOptions;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.exceptions.TransportException;
import org.apache.cassandra.security.SSLFactory;
import org.apache.cassandra.transport.Connection;
import org.apache.cassandra.transport.Event;
import org.apache.cassandra.transport.Frame;
import org.apache.cassandra.transport.FrameCompressor;
import org.apache.cassandra.transport.Message;
import org.apache.cassandra.transport.Message.Response;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.transport.messages.CredentialsMessage;
import org.apache.cassandra.transport.messages.ErrorMessage;
import org.apache.cassandra.transport.messages.EventMessage;
import org.apache.cassandra.transport.messages.ExecuteMessage;
import org.apache.cassandra.transport.messages.PrepareMessage;
import org.apache.cassandra.transport.messages.QueryMessage;
import org.apache.cassandra.transport.messages.ResultMessage;
import org.apache.cassandra.transport.messages.StartupMessage;
import org.apache.cassandra.utils.MD5Digest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.transport.FrameCompressor.SnappyCompressor.instance;


public class SimpleClient implements Closeable {
	static {
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
	}

	private static final Logger logger = LoggerFactory.getLogger(SimpleClient.class);

	public final String host;

	public final int port;

	private final EncryptionOptions.ClientEncryptionOptions encryptionOptions;

	protected final SimpleClient.ResponseHandler responseHandler = new SimpleClient.ResponseHandler();

	protected final Connection.Tracker tracker = new SimpleClient.ConnectionTracker();

	protected final ProtocolVersion version;

	protected Connection connection;

	protected Bootstrap bootstrap;

	protected Channel channel;

	protected ChannelFuture lastWriteFuture;

	private final Connection.Factory connectionFactory = new Connection.Factory() {
		public Connection newConnection(Channel channel, ProtocolVersion version) {
			return connection;
		}
	};

	public SimpleClient(String host, int port, ProtocolVersion version, EncryptionOptions.ClientEncryptionOptions encryptionOptions) {
		this(host, port, version, false, encryptionOptions);
	}

	public SimpleClient(String host, int port, EncryptionOptions.ClientEncryptionOptions encryptionOptions) {
		this(host, port, ProtocolVersion.CURRENT, encryptionOptions);
	}

	public SimpleClient(String host, int port, ProtocolVersion version) {
		this(host, port, version, new EncryptionOptions.ClientEncryptionOptions());
	}

	public SimpleClient(String host, int port, ProtocolVersion version, boolean useBeta, EncryptionOptions.ClientEncryptionOptions encryptionOptions) {
		this.host = host;
		this.port = port;
		if ((version.isBeta()) && (!useBeta))
			throw new IllegalArgumentException(String.format("Beta version of server used (%s), but USE_BETA flag is not set", version));

		this.version = version;
		this.encryptionOptions = encryptionOptions;
	}

	public SimpleClient(String host, int port) {
		this(host, port, new EncryptionOptions.ClientEncryptionOptions());
	}

	public void connect(boolean useCompression) throws IOException {
		establishConnection();
		Map<String, String> options = new HashMap<>();
		options.put(StartupMessage.CQL_VERSION, "3.0.0");
		if (useCompression) {
			options.put(StartupMessage.COMPRESSION, "snappy");
			connection.setCompressor(instance);
		}
		execute(new StartupMessage(options));
	}

	public void setEventHandler(SimpleClient.EventHandler eventHandler) {
		responseHandler.eventHandler = eventHandler;
	}

	protected void establishConnection() throws IOException {
		bootstrap = new Bootstrap().group(new NioEventLoopGroup()).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true);
		if (encryptionOptions.enabled) {
			bootstrap.handler(new SimpleClient.SecureInitializer());
		}else {
			bootstrap.handler(new SimpleClient.Initializer());
		}
		ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
		channel = future.awaitUninterruptibly().channel();
		if (!(future.isSuccess())) {
			bootstrap.group().shutdownGracefully();
			throw new IOException("Connection Error", future.cause());
		}
	}

	public void login(Map<String, String> credentials) {
		CredentialsMessage msg = new CredentialsMessage();
		msg.credentials.putAll(credentials);
		execute(msg);
	}

	public ResultMessage execute(String query, ConsistencyLevel consistency) {
		return execute(query, Collections.<ByteBuffer>emptyList(), consistency);
	}

	public ResultMessage execute(String query, List<ByteBuffer> values, ConsistencyLevel consistencyLevel) {
		Message.Response msg = execute(new QueryMessage(query, QueryOptions.forInternalCalls(consistencyLevel, values)));
		assert msg instanceof ResultMessage;
		return ((ResultMessage) (msg));
	}

	public ResultMessage.Prepared prepare(String query) {
		Message.Response msg = execute(new PrepareMessage(query));
		assert msg instanceof ResultMessage.Prepared;
		return ((ResultMessage.Prepared) (msg));
	}

	public ResultMessage executePrepared(byte[] statementId, List<ByteBuffer> values, ConsistencyLevel consistency) {
		Message.Response msg = execute(new ExecuteMessage(MD5Digest.wrap(statementId), QueryOptions.forInternalCalls(consistency, values)));
		assert msg instanceof ResultMessage;
		return ((ResultMessage) (msg));
	}

	public void close() {
		if ((lastWriteFuture) != null)
			lastWriteFuture.awaitUninterruptibly();

		channel.close().awaitUninterruptibly();
		bootstrap.group().shutdownGracefully();
	}

	public Message.Response execute(Message.Request request) {
		try {
			request.attach(connection);
			lastWriteFuture = channel.writeAndFlush(request);
			Message.Response msg = responseHandler.responses.take();
			if (msg instanceof ErrorMessage)
				throw new RuntimeException(((Throwable) (((ErrorMessage) (msg)).error)));

			return msg;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public interface EventHandler {
		void onEvent(Event event);
	}

	public static class SimpleEventHandler implements SimpleClient.EventHandler {
		public final LinkedBlockingQueue<Event> queue = new LinkedBlockingQueue<>();

		public void onEvent(Event event) {
			queue.add(event);
		}
	}

	private static final Message.ProtocolDecoder messageDecoder = new Message.ProtocolDecoder();

	private static final Message.ProtocolEncoder messageEncoder = new Message.ProtocolEncoder();

	private static final Frame.Decompressor frameDecompressor = new Frame.Decompressor();

	private static final Frame.Compressor frameCompressor = new Frame.Compressor();

	private static final Frame.Encoder frameEncoder = new Frame.Encoder();

	private static class ConnectionTracker implements Connection.Tracker {
		public void addConnection(Channel ch, Connection connection) {
		}

		public boolean isRegistered(Event.Type type, Channel ch) {
			return false;
		}
	}

	private class Initializer extends ChannelInitializer<Channel> {
		protected void initChannel(Channel channel) throws Exception {
			connection = new Connection(channel, version, tracker);
			ChannelPipeline pipeline = channel.pipeline();
			pipeline.addLast("frameDecoder", new Frame.Decoder(connectionFactory));
			pipeline.addLast("frameEncoder", SimpleClient.frameEncoder);
			pipeline.addLast("frameDecompressor", SimpleClient.frameDecompressor);
			pipeline.addLast("frameCompressor", SimpleClient.frameCompressor);
			pipeline.addLast("messageDecoder", SimpleClient.messageDecoder);
			pipeline.addLast("messageEncoder", SimpleClient.messageEncoder);
			pipeline.addLast("handler", responseHandler);
		}
	}

	private class SecureInitializer extends SimpleClient.Initializer {
		private final SSLContext sslContext;

		public SecureInitializer() throws IOException {
			this.sslContext = SSLFactory.createSSLContext(encryptionOptions, true);
		}

		protected void initChannel(Channel channel) throws Exception {
			super.initChannel(channel);
			SSLEngine sslEngine = sslContext.createSSLEngine();
			sslEngine.setUseClientMode(true);
			String[] suites = SSLFactory.filterCipherSuites(sslEngine.getSupportedCipherSuites(), encryptionOptions.cipher_suites);
			sslEngine.setEnabledCipherSuites(suites);
			channel.pipeline().addFirst("ssl", new SslHandler(sslEngine));
		}
	}

	@ChannelHandler.Sharable
	private static class ResponseHandler extends SimpleChannelInboundHandler<Message.Response> {
		public final BlockingQueue<Message.Response> responses = new SynchronousQueue<>(true);

		public SimpleClient.EventHandler eventHandler;

		@Override
		public void channelRead0(ChannelHandlerContext ctx, Message.Response r) {
			try {
				if (r instanceof EventMessage) {
					if ((eventHandler) != null)
						eventHandler.onEvent(((EventMessage) (r)).event);

				}else
					responses.put(r);

			} catch (InterruptedException ie) {
				throw new RuntimeException(ie);
			}
		}

		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			if ((this) == (ctx.pipeline().last()))
				SimpleClient.logger.error("Exception in response", cause);

			ctx.fireExceptionCaught(cause);
		}
	}
}

