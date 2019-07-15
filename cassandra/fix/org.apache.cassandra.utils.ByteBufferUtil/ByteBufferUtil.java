

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.compress.BufferType;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.FastByteOperations;
import org.apache.cassandra.utils.Hex;
import org.apache.cassandra.utils.UUIDGen;


public class ByteBufferUtil {
	public static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.wrap(new byte[0]);

	public static final ByteBuffer UNSET_BYTE_BUFFER = ByteBuffer.wrap(new byte[]{  });

	@net.nicoulaj.compilecommand.annotations.Inline
	public static int compareUnsigned(ByteBuffer o1, ByteBuffer o2) {
		return FastByteOperations.compareUnsigned(o1, o2);
	}

	@net.nicoulaj.compilecommand.annotations.Inline
	public static int compare(byte[] o1, ByteBuffer o2) {
		return FastByteOperations.compareUnsigned(o1, 0, o1.length, o2);
	}

	@net.nicoulaj.compilecommand.annotations.Inline
	public static int compare(ByteBuffer o1, byte[] o2) {
		return FastByteOperations.compareUnsigned(o1, o2, 0, o2.length);
	}

	public static String string(ByteBuffer buffer) throws CharacterCodingException {
		return ByteBufferUtil.string(buffer, StandardCharsets.UTF_8);
	}

	public static String string(ByteBuffer buffer, int position, int length) throws CharacterCodingException {
		return ByteBufferUtil.string(buffer, position, length, StandardCharsets.UTF_8);
	}

	public static String string(ByteBuffer buffer, int position, int length, Charset charset) throws CharacterCodingException {
		ByteBuffer copy = buffer.duplicate();
		copy.position(position);
		copy.limit(((copy.position()) + length));
		return ByteBufferUtil.string(copy, charset);
	}

	public static String string(ByteBuffer buffer, Charset charset) throws CharacterCodingException {
		return charset.newDecoder().decode(buffer.duplicate()).toString();
	}

	public static byte[] getArray(ByteBuffer buffer) {
		int length = buffer.remaining();
		if (buffer.hasArray()) {
			int boff = (buffer.arrayOffset()) + (buffer.position());
			return Arrays.copyOfRange(buffer.array(), boff, (boff + length));
		}
		byte[] bytes = new byte[length];
		buffer.duplicate().get(bytes);
		return bytes;
	}

	public static int lastIndexOf(ByteBuffer buffer, byte valueToFind, int startIndex) {
		assert buffer != null;
		if (startIndex < (buffer.position())) {
			return -1;
		}else
			if (startIndex >= (buffer.limit())) {
				startIndex = (buffer.limit()) - 1;
			}

		for (int i = startIndex; i >= (buffer.position()); i--) {
			if (valueToFind == (buffer.get(i)))
				return i;

		}
		return -1;
	}

	public static ByteBuffer bytes(String s) {
		return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
	}

	public static ByteBuffer bytes(String s, Charset charset) {
		return ByteBuffer.wrap(s.getBytes(charset));
	}

	public static ByteBuffer clone(ByteBuffer buffer) {
		assert buffer != null;
		if ((buffer.remaining()) == 0)
			return ByteBufferUtil.EMPTY_BYTE_BUFFER;

		ByteBuffer clone = ByteBuffer.allocate(buffer.remaining());
		if (buffer.hasArray()) {
			System.arraycopy(buffer.array(), ((buffer.arrayOffset()) + (buffer.position())), clone.array(), 0, buffer.remaining());
		}else {
			clone.put(buffer.duplicate());
			clone.flip();
		}
		return clone;
	}

	public static void arrayCopy(ByteBuffer src, int srcPos, byte[] dst, int dstPos, int length) {
		FastByteOperations.copy(src, srcPos, dst, dstPos, length);
	}

	public static void arrayCopy(ByteBuffer src, int srcPos, ByteBuffer dst, int dstPos, int length) {
		FastByteOperations.copy(src, srcPos, dst, dstPos, length);
	}

	public static int put(ByteBuffer src, ByteBuffer trg) {
		int length = Math.min(src.remaining(), trg.remaining());
		ByteBufferUtil.arrayCopy(src, src.position(), trg, trg.position(), length);
		trg.position(((trg.position()) + length));
		src.position(((src.position()) + length));
		return length;
	}

	public static void writeWithLength(ByteBuffer bytes, DataOutputPlus out) throws IOException {
		out.writeInt(bytes.remaining());
		out.write(bytes);
	}

	public static void writeWithVIntLength(ByteBuffer bytes, DataOutputPlus out) throws IOException {
		out.writeUnsignedVInt(bytes.remaining());
		out.write(bytes);
	}

	public static void writeWithLength(byte[] bytes, DataOutput out) throws IOException {
		out.writeInt(bytes.length);
		out.write(bytes);
	}

	public static void writeWithShortLength(ByteBuffer buffer, DataOutputPlus out) throws IOException {
		int length = buffer.remaining();
		assert (0 <= length) && (length <= (FBUtilities.MAX_UNSIGNED_SHORT)) : String.format("Attempted serializing to buffer exceeded maximum of %s bytes: %s", FBUtilities.MAX_UNSIGNED_SHORT, length);
		out.writeShort(length);
		out.write(buffer);
	}

	public static void writeWithShortLength(byte[] buffer, DataOutput out) throws IOException {
		int length = buffer.length;
		assert (0 <= length) && (length <= (FBUtilities.MAX_UNSIGNED_SHORT)) : String.format("Attempted serializing to buffer exceeded maximum of %s bytes: %s", FBUtilities.MAX_UNSIGNED_SHORT, length);
		out.writeShort(length);
		out.write(buffer);
	}

	public static ByteBuffer readWithLength(DataInput in) throws IOException {
		int length = in.readInt();
		if (length < 0) {
			throw new IOException("Corrupt (negative) value length encountered");
		}
		return ByteBufferUtil.read(in, length);
	}

	public static ByteBuffer readWithVIntLength(DataInputPlus in) throws IOException {
		int length = ((int) (in.readUnsignedVInt()));
		if (length < 0)
			throw new IOException("Corrupt (negative) value length encountered");

		return ByteBufferUtil.read(in, length);
	}

	public static int serializedSizeWithLength(ByteBuffer buffer) {
		int size = buffer.remaining();
		return (TypeSizes.sizeof(size)) + size;
	}

	public static int serializedSizeWithVIntLength(ByteBuffer buffer) {
		int size = buffer.remaining();
		return (TypeSizes.sizeofUnsignedVInt(size)) + size;
	}

	public static void skipWithVIntLength(DataInputPlus in) throws IOException {
		int length = ((int) (in.readUnsignedVInt()));
		if (length < 0)
			throw new IOException("Corrupt (negative) value length encountered");

		in.skipBytesFully(length);
	}

	public static int readShortLength(DataInput in) throws IOException {
		return in.readUnsignedShort();
	}

	public static ByteBuffer readWithShortLength(DataInput in) throws IOException {
		return ByteBufferUtil.read(in, ByteBufferUtil.readShortLength(in));
	}

	public static int serializedSizeWithShortLength(ByteBuffer buffer) {
		int size = buffer.remaining();
		return (TypeSizes.sizeof(((short) (size)))) + size;
	}

	public static void skipShortLength(DataInputPlus in) throws IOException {
		int skip = ByteBufferUtil.readShortLength(in);
		in.skipBytesFully(skip);
	}

	public static ByteBuffer read(DataInput in, int length) throws IOException {
		if (length == 0)
			return ByteBufferUtil.EMPTY_BYTE_BUFFER;

		byte[] buff = new byte[length];
		in.readFully(buff);
		return ByteBuffer.wrap(buff);
	}

	public static byte[] readBytes(DataInput in, int length) throws IOException {
		assert length > 0 : "length is not > 0: " + length;
		byte[] bytes = new byte[length];
		in.readFully(bytes);
		return bytes;
	}

	public static int toInt(ByteBuffer bytes) {
		return bytes.getInt(bytes.position());
	}

	public static short toShort(ByteBuffer bytes) {
		return bytes.getShort(bytes.position());
	}

	public static long toLong(ByteBuffer bytes) {
		return bytes.getLong(bytes.position());
	}

	public static float toFloat(ByteBuffer bytes) {
		return bytes.getFloat(bytes.position());
	}

	public static double toDouble(ByteBuffer bytes) {
		return bytes.getDouble(bytes.position());
	}

	public static ByteBuffer bytes(short s) {
		return ByteBuffer.allocate(2).putShort(0, s);
	}

	public static ByteBuffer bytes(int i) {
		return ByteBuffer.allocate(4).putInt(0, i);
	}

	public static ByteBuffer bytes(long n) {
		return ByteBuffer.allocate(8).putLong(0, n);
	}

	public static ByteBuffer bytes(float f) {
		return ByteBuffer.allocate(4).putFloat(0, f);
	}

	public static ByteBuffer bytes(double d) {
		return ByteBuffer.allocate(8).putDouble(0, d);
	}

	public static InputStream inputStream(ByteBuffer bytes) {
		final ByteBuffer copy = bytes.duplicate();
		return new InputStream() {
			public int read() {
				if (!(copy.hasRemaining()))
					return -1;

				return (copy.get()) & 255;
			}

			@Override
			public int read(byte[] bytes, int off, int len) {
				if (!(copy.hasRemaining()))
					return -1;

				len = Math.min(len, copy.remaining());
				copy.get(bytes, off, len);
				return len;
			}

			@Override
			public int available() {
				return copy.remaining();
			}
		};
	}

	public static String bytesToHex(ByteBuffer bytes) {
		if (bytes.hasArray()) {
			return Hex.bytesToHex(bytes.array(), ((bytes.arrayOffset()) + (bytes.position())), bytes.remaining());
		}
		final int offset = bytes.position();
		final int size = bytes.remaining();
		final char[] c = new char[size * 2];
		for (int i = 0; i < size; i++) {
			final int bint = bytes.get((i + offset));
		}
		return Hex.wrapCharArray(c);
	}

	public static ByteBuffer hexToBytes(String str) {
		return ByteBuffer.wrap(Hex.hexToBytes(str));
	}

	public static int compareSubArrays(ByteBuffer bytes1, int offset1, ByteBuffer bytes2, int offset2, int length) {
		if (bytes1 == null)
			return bytes2 == null ? 0 : -1;

		if (bytes2 == null)
			return 1;

		assert (bytes1.limit()) >= (offset1 + length) : "The first byte array isn't long enough for the specified offset and length.";
		assert (bytes2.limit()) >= (offset2 + length) : "The second byte array isn't long enough for the specified offset and length.";
		for (int i = 0; i < length; i++) {
			byte byte1 = bytes1.get((offset1 + i));
			byte byte2 = bytes2.get((offset2 + i));
			if (byte1 == byte2)
				continue;

			return (byte1 & 255) < (byte2 & 255) ? -1 : 1;
		}
		return 0;
	}

	public static ByteBuffer bytes(InetAddress address) {
		return ByteBuffer.wrap(address.getAddress());
	}

	public static ByteBuffer bytes(UUID uuid) {
		return ByteBuffer.wrap(UUIDGen.decompose(uuid));
	}

	public static boolean isPrefix(ByteBuffer prefix, ByteBuffer value) {
		if ((prefix.remaining()) > (value.remaining()))
			return false;

		int diff = (value.remaining()) - (prefix.remaining());
		return prefix.equals(value.duplicate().limit(((value.remaining()) - diff)));
	}

	public static ByteBuffer minimalBufferFor(ByteBuffer buf) {
		return ((buf.capacity()) > (buf.remaining())) || (!(buf.hasArray())) ? ByteBuffer.wrap(ByteBufferUtil.getArray(buf)) : buf;
	}

	public static int getShortLength(ByteBuffer bb, int position) {
		int length = ((bb.get(position)) & 255) << 8;
		return length | ((bb.get((position + 1))) & 255);
	}

	public static int readShortLength(ByteBuffer bb) {
		int length = ((bb.get()) & 255) << 8;
		return length | ((bb.get()) & 255);
	}

	public static void writeShortLength(ByteBuffer bb, int length) {
		bb.put(((byte) ((length >> 8) & 255)));
		bb.put(((byte) (length & 255)));
	}

	public static ByteBuffer readBytes(ByteBuffer bb, int length) {
		ByteBuffer copy = bb.duplicate();
		copy.limit(((copy.position()) + length));
		bb.position(((bb.position()) + length));
		return copy;
	}

	public static ByteBuffer readBytesWithShortLength(ByteBuffer bb) {
		int length = ByteBufferUtil.readShortLength(bb);
		return ByteBufferUtil.readBytes(bb, length);
	}

	public static ByteBuffer ensureCapacity(ByteBuffer buf, int outputLength, boolean allowBufferResize) {
		BufferType bufferType = (buf != null) ? BufferType.typeOf(buf) : BufferType.ON_HEAP;
		return ByteBufferUtil.ensureCapacity(buf, outputLength, allowBufferResize, bufferType);
	}

	public static ByteBuffer ensureCapacity(ByteBuffer buf, int outputLength, boolean allowBufferResize, BufferType bufferType) {
		if (0 > outputLength)
			throw new IllegalArgumentException(("invalid size for output buffer: " + outputLength));

		if ((buf == null) || ((buf.capacity()) < outputLength)) {
			if (!allowBufferResize)
				throw new IllegalStateException(String.format("output buffer is not large enough for data: current capacity %d, required %d", buf.capacity(), outputLength));

			FileUtils.clean(buf);
			buf = bufferType.allocate(outputLength);
		}else {
			buf.position(0).limit(outputLength);
		}
		return buf;
	}

	public static boolean contains(ByteBuffer buffer, ByteBuffer subBuffer) {
		int len = subBuffer.remaining();
		if (((buffer.remaining()) - len) < 0)
			return false;

		byte first = subBuffer.get(subBuffer.position());
		int max = (buffer.position()) + ((buffer.remaining()) - len);
		for (int i = buffer.position(); i <= max; i++) {
			if ((buffer.get(i)) != first) {
				while (((++i) <= max) && ((buffer.get(i)) != first)) {
				} 
			}
			if (i <= max) {
				int j = i + 1;
				int end = (j + len) - 1;
				for (int k = 1 + (subBuffer.position()); (j < end) && ((buffer.get(j)) == (subBuffer.get(k))); j++ , k++) {
				}
				if (j == end)
					return true;

			}
		}
		return false;
	}

	public static boolean startsWith(ByteBuffer src, ByteBuffer prefix) {
		return ByteBufferUtil.startsWith(src, prefix, 0);
	}

	public static boolean endsWith(ByteBuffer src, ByteBuffer suffix) {
		return ByteBufferUtil.startsWith(src, suffix, ((src.remaining()) - (suffix.remaining())));
	}

	private static boolean startsWith(ByteBuffer src, ByteBuffer prefix, int offset) {
		if (offset < 0)
			return false;

		int sPos = (src.position()) + offset;
		int pPos = prefix.position();
		if (((src.remaining()) - offset) < (prefix.remaining()))
			return false;

		int len = Math.min(((src.remaining()) - offset), prefix.remaining());
		while ((len--) > 0) {
			if ((src.get((sPos++))) != (prefix.get((pPos++))))
				return false;

		} 
		return true;
	}
}

