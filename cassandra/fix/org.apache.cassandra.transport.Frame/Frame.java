

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.ReferenceCounted;
import java.io.IOException;
import java.util.AbstractCollection;
import java.util.EnumSet;
import java.util.List;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.transport.CBUtil;
import org.apache.cassandra.transport.Connection;
import org.apache.cassandra.transport.Message;
import org.apache.cassandra.transport.ProtocolException;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.transport.messages.ErrorMessage;

import static org.apache.cassandra.transport.Message.Direction.extractFromVersion;
import static org.apache.cassandra.transport.Message.Type.fromOpcode;


public class Frame {
	public static final byte PROTOCOL_VERSION_MASK = 127;

	public final Frame.Header header;

	public final ByteBuf body;

	private Frame(Frame.Header header, ByteBuf body) {
		this.header = header;
		this.body = body;
	}

	public void retain() {
		body.retain();
	}

	public boolean release() {
		return body.release();
	}

	public static Frame create(Message.Type type, int streamId, ProtocolVersion version, EnumSet<Frame.Header.Flag> flags, ByteBuf body) {
		Frame.Header header = new Frame.Header(version, flags, streamId, type);
		return new Frame(header, body);
	}

	public static class Header {
		public static final int LENGTH = 9;

		public static final int BODY_LENGTH_SIZE = 4;

		public final ProtocolVersion version;

		public final EnumSet<Frame.Header.Flag> flags;

		public final int streamId;

		public final Message.Type type;

		private Header(ProtocolVersion version, EnumSet<Frame.Header.Flag> flags, int streamId, Message.Type type) {
			this.version = version;
			this.flags = flags;
			this.streamId = streamId;
			this.type = type;
		}

		public enum Flag {

			COMPRESSED,
			TRACING,
			CUSTOM_PAYLOAD,
			WARNING,
			USE_BETA;
			private static final Frame.Header.Flag[] ALL_VALUES = Frame.Header.Flag.values();

			public static EnumSet<Frame.Header.Flag> deserialize(int flags) {
				EnumSet<Frame.Header.Flag> set = EnumSet.noneOf(Frame.Header.Flag.class);
				for (int n = 0; n < (Frame.Header.Flag.ALL_VALUES.length); n++) {
					if ((flags & (1 << n)) != 0)
						set.add(Frame.Header.Flag.ALL_VALUES[n]);

				}
				return set;
			}

			public static int serialize(EnumSet<Frame.Header.Flag> flags) {
				int i = 0;
				for (Frame.Header.Flag flag : flags)
					i |= 1 << (flag.ordinal());

				return i;
			}
		}
	}

	public Frame with(ByteBuf newBody) {
		return new Frame(header, newBody);
	}

	public static class Decoder extends ByteToMessageDecoder {
		private static final int MAX_FRAME_LENGTH = DatabaseDescriptor.getNativeTransportMaxFrameSize();

		private boolean discardingTooLongFrame;

		private long tooLongFrameLength;

		private long bytesToDiscard;

		private int tooLongStreamId;

		private final Connection.Factory factory;

		public Decoder(Connection.Factory factory) {
			this.factory = factory;
		}

		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> results) throws Exception {
			if (discardingTooLongFrame) {
				bytesToDiscard = Frame.discard(buffer, bytesToDiscard);
				if ((bytesToDiscard) <= 0)
					fail();

				return;
			}
			int readableBytes = buffer.readableBytes();
			if (readableBytes == 0)
				return;

			int idx = buffer.readerIndex();
			int firstByte = buffer.getByte((idx++));
			Message.Direction direction = extractFromVersion(firstByte);
			int versionNum = firstByte & (Frame.PROTOCOL_VERSION_MASK);
			ProtocolVersion version = ProtocolVersion.decode(versionNum);
			if (readableBytes < (Frame.Header.LENGTH))
				return;

			int flags = buffer.getByte((idx++));
			EnumSet<Frame.Header.Flag> decodedFlags = Frame.Header.Flag.deserialize(flags);
			if ((version.isBeta()) && (!(decodedFlags.contains(Frame.Header.Flag.USE_BETA))))
				throw new ProtocolException(String.format("Beta version of the protocol used (%s), but USE_BETA flag is unset", version), version);

			int streamId = buffer.getShort(idx);
			idx += 2;
			Message.Type type;
			try {
				type = fromOpcode(buffer.getByte((idx++)), direction);
			} catch (ProtocolException e) {
				throw ErrorMessage.wrap(e, streamId);
			}
			long bodyLength = buffer.getUnsignedInt(idx);
			idx += Frame.Header.BODY_LENGTH_SIZE;
			long frameLength = bodyLength + (Frame.Header.LENGTH);
			if (frameLength > (Frame.Decoder.MAX_FRAME_LENGTH)) {
				discardingTooLongFrame = true;
				tooLongStreamId = streamId;
				tooLongFrameLength = frameLength;
				bytesToDiscard = Frame.discard(buffer, frameLength);
				if ((bytesToDiscard) <= 0)
					fail();

				return;
			}
			if ((buffer.readableBytes()) < frameLength)
				return;

			ByteBuf body = buffer.slice(idx, ((int) (bodyLength)));
			body.retain();
			idx += bodyLength;
			buffer.readerIndex(idx);
			results.add(new Frame(new Frame.Header(version, decodedFlags, streamId, type), body));
		}

		private void fail() {
			long tooLongFrameLength = this.tooLongFrameLength;
			this.tooLongFrameLength = 0;
			discardingTooLongFrame = false;
			String msg = String.format("Request is too big: length %d exceeds maximum allowed length %d.", tooLongFrameLength, Frame.Decoder.MAX_FRAME_LENGTH);
			throw ErrorMessage.wrap(new InvalidRequestException(msg), tooLongStreamId);
		}
	}

	private static long discard(ByteBuf buffer, long remainingToDiscard) {
		int availableToDiscard = ((int) (Math.min(remainingToDiscard, buffer.readableBytes())));
		buffer.skipBytes(availableToDiscard);
		return remainingToDiscard - availableToDiscard;
	}

	@ChannelHandler.Sharable
	public static class Encoder extends MessageToMessageEncoder<Frame> {
		public void encode(ChannelHandlerContext ctx, Frame frame, List<Object> results) throws IOException {
			ByteBuf header = CBUtil.allocator.buffer(Frame.Header.LENGTH);
			Message.Type type = frame.header.type;
			header.writeByte(type.direction.addToVersion(frame.header.version.asInt()));
			header.writeByte(Frame.Header.Flag.serialize(frame.header.flags));
			if (frame.header.version.isGreaterOrEqualTo(ProtocolVersion.V3))
				header.writeShort(frame.header.streamId);
			else
				header.writeByte(frame.header.streamId);

			header.writeByte(type.opcode);
			header.writeInt(frame.body.readableBytes());
			results.add(header);
			results.add(frame.body);
		}
	}

	@ChannelHandler.Sharable
	public static class Decompressor extends MessageToMessageDecoder<Frame> {
		public void decode(ChannelHandlerContext ctx, Frame frame, List<Object> results) throws IOException {
		}
	}

	@ChannelHandler.Sharable
	public static class Compressor extends MessageToMessageEncoder<Frame> {
		public void encode(ChannelHandlerContext ctx, Frame frame, List<Object> results) throws IOException {
			frame.header.flags.add(Frame.Header.Flag.COMPRESSED);
		}
	}
}

