

import java.util.Arrays;
import org.apache.lucene.util.FutureArrays;
import org.apache.lucene.util.UnicodeUtil;


public final class BytesRef implements Cloneable , Comparable<BytesRef> {
	public static final byte[] EMPTY_BYTES = new byte[0];

	public byte[] bytes;

	public int offset;

	public int length;

	public BytesRef() {
		this(BytesRef.EMPTY_BYTES);
	}

	public BytesRef(byte[] bytes, int offset, int length) {
		this.bytes = bytes;
		this.offset = offset;
		this.length = length;
		assert isValid();
	}

	public BytesRef(byte[] bytes) {
		this(bytes, 0, bytes.length);
	}

	public BytesRef(int capacity) {
		this.bytes = new byte[capacity];
	}

	public BytesRef(CharSequence text) {
		this(new byte[UnicodeUtil.maxUTF8Length(text.length())]);
		length = UnicodeUtil.UTF16toUTF8(text, 0, text.length(), bytes);
	}

	public boolean bytesEquals(BytesRef other) {
		return FutureArrays.equals(this.bytes, this.offset, ((this.offset) + (this.length)), other.bytes, other.offset, ((other.offset) + (other.length)));
	}

	@Override
	public BytesRef clone() {
		return new BytesRef(bytes, offset, length);
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (other instanceof BytesRef) {
			return this.bytesEquals(((BytesRef) (other)));
		}
		return false;
	}

	public String utf8ToString() {
		final char[] ref = new char[length];
		final int len = UnicodeUtil.UTF8toUTF16(bytes, offset, length, ref);
		return new String(ref, 0, len);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		final int end = (offset) + (length);
		for (int i = offset; i < end; i++) {
			if (i > (offset)) {
				sb.append(' ');
			}
			sb.append(Integer.toHexString(((bytes[i]) & 255)));
		}
		sb.append(']');
		return sb.toString();
	}

	@Override
	public int compareTo(BytesRef other) {
		return FutureArrays.compareUnsigned(this.bytes, this.offset, ((this.offset) + (this.length)), other.bytes, other.offset, ((other.offset) + (other.length)));
	}

	public static BytesRef deepCopyOf(BytesRef other) {
		BytesRef copy = new BytesRef();
		copy.bytes = Arrays.copyOfRange(other.bytes, other.offset, ((other.offset) + (other.length)));
		copy.offset = 0;
		copy.length = other.length;
		return copy;
	}

	public boolean isValid() {
		if ((bytes) == null) {
			throw new IllegalStateException("bytes is null");
		}
		if ((length) < 0) {
			throw new IllegalStateException(("length is negative: " + (length)));
		}
		if ((length) > (bytes.length)) {
			throw new IllegalStateException(((("length is out of bounds: " + (length)) + ",bytes.length=") + (bytes.length)));
		}
		if ((offset) < 0) {
			throw new IllegalStateException(("offset is negative: " + (offset)));
		}
		if ((offset) > (bytes.length)) {
			throw new IllegalStateException(((("offset out of bounds: " + (offset)) + ",bytes.length=") + (bytes.length)));
		}
		if (((offset) + (length)) < 0) {
			throw new IllegalStateException(((("offset+length is negative: offset=" + (offset)) + ",length=") + (length)));
		}
		if (((offset) + (length)) > (bytes.length)) {
			throw new IllegalStateException(((((("offset+length out of bounds: offset=" + (offset)) + ",length=") + (length)) + ",bytes.length=") + (bytes.length)));
		}
		return true;
	}
}

