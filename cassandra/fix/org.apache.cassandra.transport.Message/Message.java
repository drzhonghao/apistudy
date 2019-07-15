

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.ScheduledFuture;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.cassandra.service.ClientWarn;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.transport.CBCodec;
import org.apache.cassandra.transport.CBUtil;
import org.apache.cassandra.transport.Connection;
import org.apache.cassandra.transport.Frame;
import org.apache.cassandra.transport.Frame.Header.Flag;
import org.apache.cassandra.transport.ProtocolException;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.transport.ServerConnection;
import org.apache.cassandra.transport.messages.ErrorMessage;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.transport.Frame.Header.Flag.CUSTOM_PAYLOAD;
import static org.apache.cassandra.transport.Frame.Header.Flag.TRACING;
import static org.apache.cassandra.transport.Frame.Header.Flag.WARNING;


public abstract class Message {
	protected static final Logger logger = LoggerFactory.getLogger(Message.class);

	private static final Set<String> ioExceptionsAtDebugLevel = ImmutableSet.<String>builder().add("Connection reset by peer").add("Broken pipe").add("Connection timed out").build();

	public interface Codec<M extends Message> extends CBCodec<M> {}

	public enum Direction {

		REQUEST,
		RESPONSE;
		public static Message.Direction extractFromVersion(int versionWithDirection) {
			return (versionWithDirection & 128) == 0 ? Message.Direction.REQUEST : Message.Direction.RESPONSE;
		}

		public int addToVersion(int rawVersion) {
			return (this) == (Message.Direction.REQUEST) ? rawVersion & 127 : rawVersion | 128;
		}
	}

	public enum Type {
		;

		public final int opcode;

		public final Message.Direction direction;

		public final Message.Codec<?> codec;

		private static final Message.Type[] opcodeIdx;

		static {
			int maxOpcode = -1;
			for (Message.Type type : Message.Type.values())
				maxOpcode = Math.max(maxOpcode, type.opcode);

			opcodeIdx = new Message.Type[maxOpcode + 1];
			for (Message.Type type : Message.Type.values()) {
				if ((Message.Type.opcodeIdx[type.opcode]) != null)
					throw new IllegalStateException("Duplicate opcode");

				Message.Type.opcodeIdx[type.opcode] = type;
			}
		}

		Type(int opcode, Message.Direction direction, Message.Codec<?> codec) {
			this.opcode = opcode;
			this.direction = direction;
			this.codec = codec;
		}

		public static Message.Type fromOpcode(int opcode, Message.Direction direction) {
			if (opcode >= (Message.Type.opcodeIdx.length))
				throw new ProtocolException(String.format("Unknown opcode %d", opcode));

			Message.Type t = Message.Type.opcodeIdx[opcode];
			if (t == null)
				throw new ProtocolException(String.format("Unknown opcode %d", opcode));

			if ((t.direction) != direction)
				throw new ProtocolException(String.format("Wrong protocol direction (expected %s, got %s) for opcode %d (%s)", t.direction, direction, opcode, t));

			return t;
		}
	}

	public final Message.Type type;

	protected Connection connection;

	private int streamId;

	private Frame sourceFrame;

	private Map<String, ByteBuffer> customPayload;

	protected ProtocolVersion forcedProtocolVersion = null;

	protected Message(Message.Type type) {
		this.type = type;
	}

	public void attach(Connection connection) {
		this.connection = connection;
	}

	public Connection connection() {
		return connection;
	}

	public Message setStreamId(int streamId) {
		this.streamId = streamId;
		return this;
	}

	public int getStreamId() {
		return streamId;
	}

	public void setSourceFrame(Frame sourceFrame) {
		this.sourceFrame = sourceFrame;
	}

	public Frame getSourceFrame() {
		return sourceFrame;
	}

	public Map<String, ByteBuffer> getCustomPayload() {
		return customPayload;
	}

	public void setCustomPayload(Map<String, ByteBuffer> customPayload) {
		this.customPayload = customPayload;
	}

	public abstract static class Request extends Message {
		protected boolean tracingRequested;

		protected Request(Message.Type type) {
			super(type);
			if ((type.direction) != (Message.Direction.REQUEST))
				throw new IllegalArgumentException();

		}

		public abstract Message.Response execute(QueryState queryState, long queryStartNanoTime);

		public void setTracingRequested() {
			this.tracingRequested = true;
		}

		public boolean isTracingRequested() {
			return tracingRequested;
		}
	}

	public abstract static class Response extends Message {
		protected UUID tracingId;

		protected List<String> warnings;

		protected Response(Message.Type type) {
			super(type);
			if ((type.direction) != (Message.Direction.RESPONSE))
				throw new IllegalArgumentException();

		}

		public Message setTracingId(UUID tracingId) {
			this.tracingId = tracingId;
			return this;
		}

		public UUID getTracingId() {
			return tracingId;
		}

		public Message setWarnings(List<String> warnings) {
			this.warnings = warnings;
			return this;
		}

		public List<String> getWarnings() {
			return warnings;
		}
	}

	@ChannelHandler.Sharable
	public static class ProtocolDecoder extends MessageToMessageDecoder<Frame> {
		public void decode(ChannelHandlerContext ctx, Frame frame, List results) {
			boolean isTracing = frame.header.flags.contains(TRACING);
			boolean isCustomPayload = frame.header.flags.contains(CUSTOM_PAYLOAD);
			boolean hasWarning = frame.header.flags.contains(WARNING);
			Map<String, ByteBuffer> customPayload = (!isCustomPayload) ? null : CBUtil.readBytesMap(frame.body);
			try {
				if (isCustomPayload && (frame.header.version.isSmallerThan(ProtocolVersion.V4)))
					throw new ProtocolException("Received frame with CUSTOM_PAYLOAD flag for native protocol version < 4");

			} catch (Throwable ex) {
				frame.release();
				throw ErrorMessage.wrap(ex, frame.header.streamId);
			}
		}
	}

	@ChannelHandler.Sharable
	public static class ProtocolEncoder extends MessageToMessageEncoder<Message> {
		public void encode(ChannelHandlerContext ctx, Message message, List results) {
			EnumSet<Frame.Header.Flag> flags = EnumSet.noneOf(Frame.Header.Flag.class);
			Message.Codec<Message> codec = ((Message.Codec<Message>) (message.type.codec));
			try {
				ByteBuf body;
				if (message instanceof Message.Response) {
					UUID tracingId = ((Message.Response) (message)).getTracingId();
					Map<String, ByteBuffer> customPayload = message.getCustomPayload();
					if (tracingId != null) {
					}
					List<String> warnings = ((Message.Response) (message)).getWarnings();
					if (warnings != null) {
					}
					if (customPayload != null) {
					}
					if (tracingId != null) {
						body = null;
						CBUtil.writeUUID(tracingId, body);
						flags.add(TRACING);
					}
					if (warnings != null) {
						body = null;
						CBUtil.writeStringList(warnings, body);
						flags.add(WARNING);
					}
					if (customPayload != null) {
						body = null;
						CBUtil.writeBytesMap(customPayload, body);
						flags.add(CUSTOM_PAYLOAD);
					}
				}else {
					assert message instanceof Message.Request;
					if (((Message.Request) (message)).isTracingRequested())
						flags.add(TRACING);

					Map<String, ByteBuffer> payload = message.getCustomPayload();
					if (payload != null) {
					}
					if (payload != null) {
						body = null;
						CBUtil.writeBytesMap(payload, body);
						flags.add(CUSTOM_PAYLOAD);
					}
				}
				try {
				} catch (Throwable e) {
					body = null;
					body.release();
					throw e;
				}
			} catch (Throwable e) {
				throw ErrorMessage.wrap(e, message.getStreamId());
			}
		}
	}

	@ChannelHandler.Sharable
	public static class Dispatcher extends SimpleChannelInboundHandler<Message.Request> {
		private static class FlushItem {
			final ChannelHandlerContext ctx;

			final Object response;

			final Frame sourceFrame;

			private FlushItem(ChannelHandlerContext ctx, Object response, Frame sourceFrame) {
				this.ctx = ctx;
				this.sourceFrame = sourceFrame;
				this.response = response;
			}
		}

		private static final class Flusher implements Runnable {
			final EventLoop eventLoop;

			final ConcurrentLinkedQueue<Message.Dispatcher.FlushItem> queued = new ConcurrentLinkedQueue<>();

			final AtomicBoolean running = new AtomicBoolean(false);

			final HashSet<ChannelHandlerContext> channels = new HashSet<>();

			final List<Message.Dispatcher.FlushItem> flushed = new ArrayList<>();

			int runsSinceFlush = 0;

			int runsWithNoWork = 0;

			private Flusher(EventLoop eventLoop) {
				this.eventLoop = eventLoop;
			}

			void start() {
				if ((!(running.get())) && (running.compareAndSet(false, true))) {
					this.eventLoop.execute(this);
				}
			}

			public void run() {
				boolean doneWork = false;
				Message.Dispatcher.FlushItem flush;
				while (null != (flush = queued.poll())) {
					channels.add(flush.ctx);
					flush.ctx.write(flush.response, flush.ctx.voidPromise());
					flushed.add(flush);
					doneWork = true;
				} 
				(runsSinceFlush)++;
				if (((!doneWork) || ((runsSinceFlush) > 2)) || ((flushed.size()) > 50)) {
					for (ChannelHandlerContext channel : channels)
						channel.flush();

					for (Message.Dispatcher.FlushItem item : flushed)
						item.sourceFrame.release();

					channels.clear();
					flushed.clear();
					runsSinceFlush = 0;
				}
				if (doneWork) {
					runsWithNoWork = 0;
				}else {
					if ((++(runsWithNoWork)) > 5) {
						running.set(false);
						if ((queued.isEmpty()) || (!(running.compareAndSet(false, true))))
							return;

					}
				}
				eventLoop.schedule(this, 10000, TimeUnit.NANOSECONDS);
			}
		}

		private static final ConcurrentMap<EventLoop, Message.Dispatcher.Flusher> flusherLookup = new ConcurrentHashMap<>();

		public Dispatcher() {
			super(false);
		}

		@Override
		public void channelRead0(ChannelHandlerContext ctx, Message.Request request) {
			final Message.Response response;
			final ServerConnection connection;
			long queryStartNanoTime = System.nanoTime();
			try {
				assert (request.connection()) instanceof ServerConnection;
				connection = ((ServerConnection) (request.connection()));
				if (connection.getVersion().isGreaterOrEqualTo(ProtocolVersion.V4))
					ClientWarn.instance.captureWarnings();

				Message.logger.trace("Received: {}, v={}", request, connection.getVersion());
				response = null;
				response.setStreamId(request.getStreamId());
				response.setWarnings(ClientWarn.instance.getWarnings());
				response.attach(connection);
			} catch (Throwable t) {
				JVMStabilityInspector.inspectThrowable(t);
				Message.UnexpectedChannelExceptionHandler handler = new Message.UnexpectedChannelExceptionHandler(ctx.channel(), true);
				flush(new Message.Dispatcher.FlushItem(ctx, ErrorMessage.fromException(t, handler).setStreamId(request.getStreamId()), request.getSourceFrame()));
				return;
			} finally {
				ClientWarn.instance.resetWarnings();
			}
			Message.logger.trace("Responding: {}, v={}", response, connection.getVersion());
			flush(new Message.Dispatcher.FlushItem(ctx, response, request.getSourceFrame()));
		}

		private void flush(Message.Dispatcher.FlushItem item) {
			EventLoop loop = item.ctx.channel().eventLoop();
			Message.Dispatcher.Flusher flusher = Message.Dispatcher.flusherLookup.get(loop);
			if (flusher == null) {
				Message.Dispatcher.Flusher alt = Message.Dispatcher.flusherLookup.putIfAbsent(loop, (flusher = new Message.Dispatcher.Flusher(loop)));
				if (alt != null)
					flusher = alt;

			}
			flusher.queued.add(item);
			flusher.start();
		}
	}

	@ChannelHandler.Sharable
	public static final class ExceptionHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void exceptionCaught(final ChannelHandlerContext ctx, Throwable cause) {
			Message.UnexpectedChannelExceptionHandler handler = new Message.UnexpectedChannelExceptionHandler(ctx.channel(), false);
			ErrorMessage errorMessage = ErrorMessage.fromException(cause, handler);
			if (ctx.channel().isOpen()) {
				ChannelFuture future = ctx.writeAndFlush(errorMessage);
				if (cause instanceof ProtocolException) {
					future.addListener(new ChannelFutureListener() {
						public void operationComplete(ChannelFuture future) {
							ctx.close();
						}
					});
				}
			}
		}
	}

	static final class UnexpectedChannelExceptionHandler implements Predicate<Throwable> {
		private final Channel channel;

		private final boolean alwaysLogAtError;

		UnexpectedChannelExceptionHandler(Channel channel, boolean alwaysLogAtError) {
			this.channel = channel;
			this.alwaysLogAtError = alwaysLogAtError;
		}

		@Override
		public boolean apply(Throwable exception) {
			String message;
			try {
				message = "Unexpected exception during request; channel = " + (channel);
			} catch (Exception ignore) {
				message = "Unexpected exception during request; channel = <unprintable>";
			}
			if ((!(alwaysLogAtError)) && (exception instanceof IOException)) {
				if (Message.ioExceptionsAtDebugLevel.contains(exception.getMessage())) {
					Message.logger.trace(message, exception);
				}else {
					Message.logger.info(message, exception);
				}
			}else {
				Message.logger.error(message, exception);
			}
			return true;
		}
	}
}

