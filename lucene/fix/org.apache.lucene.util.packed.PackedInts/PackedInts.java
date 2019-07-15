

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.index.LegacyNumericDocValues;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.LongsRef;
import org.apache.lucene.util.RamUsageEstimator;


public class PackedInts {
	public static final float FASTEST = 7.0F;

	public static final float FAST = 0.5F;

	public static final float DEFAULT = 0.25F;

	public static final float COMPACT = 0.0F;

	public static final int DEFAULT_BUFFER_SIZE = 1024;

	public static final String CODEC_NAME = "PackedInts";

	public static final int VERSION_MONOTONIC_WITHOUT_ZIGZAG = 2;

	public static final int VERSION_START = PackedInts.VERSION_MONOTONIC_WITHOUT_ZIGZAG;

	public static final int VERSION_CURRENT = PackedInts.VERSION_MONOTONIC_WITHOUT_ZIGZAG;

	public static void checkVersion(int version) {
		if (version < (PackedInts.VERSION_START)) {
			throw new IllegalArgumentException((((("Version is too old, should be at least " + (PackedInts.VERSION_START)) + " (got ") + version) + ")"));
		}else
			if (version > (PackedInts.VERSION_CURRENT)) {
				throw new IllegalArgumentException((((("Version is too new, should be at most " + (PackedInts.VERSION_CURRENT)) + " (got ") + version) + ")"));
			}

	}

	public enum Format {

		PACKED(0) {
			@Override
			public long byteCount(int packedIntsVersion, int valueCount, int bitsPerValue) {
				return ((long) (Math.ceil(((((double) (valueCount)) * bitsPerValue) / 8))));
			}
		},
		PACKED_SINGLE_BLOCK(1) {
			@Override
			public int longCount(int packedIntsVersion, int valueCount, int bitsPerValue) {
				final int valuesPerBlock = 64 / bitsPerValue;
				return ((int) (Math.ceil((((double) (valueCount)) / valuesPerBlock))));
			}

			@Override
			public boolean isSupported(int bitsPerValue) {
				return false;
			}

			@Override
			public float overheadPerValue(int bitsPerValue) {
				assert isSupported(bitsPerValue);
				final int valuesPerBlock = 64 / bitsPerValue;
				final int overhead = 64 % bitsPerValue;
				return ((float) (overhead)) / valuesPerBlock;
			}
		};
		public static PackedInts.Format byId(int id) {
			for (PackedInts.Format format : PackedInts.Format.values()) {
				if ((format.getId()) == id) {
					return format;
				}
			}
			throw new IllegalArgumentException(("Unknown format id: " + id));
		}

		private Format(int id) {
			this.id = id;
		}

		public int id;

		public int getId() {
			return id;
		}

		public long byteCount(int packedIntsVersion, int valueCount, int bitsPerValue) {
			assert (bitsPerValue >= 0) && (bitsPerValue <= 64) : bitsPerValue;
			return 8L * (longCount(packedIntsVersion, valueCount, bitsPerValue));
		}

		public int longCount(int packedIntsVersion, int valueCount, int bitsPerValue) {
			assert (bitsPerValue >= 0) && (bitsPerValue <= 64) : bitsPerValue;
			final long byteCount = byteCount(packedIntsVersion, valueCount, bitsPerValue);
			assert byteCount < (8L * (Integer.MAX_VALUE));
			if ((byteCount % 8) == 0) {
				return ((int) (byteCount / 8));
			}else {
				return ((int) ((byteCount / 8) + 1));
			}
		}

		public boolean isSupported(int bitsPerValue) {
			return (bitsPerValue >= 1) && (bitsPerValue <= 64);
		}

		public float overheadPerValue(int bitsPerValue) {
			assert isSupported(bitsPerValue);
			return 0.0F;
		}

		public final float overheadRatio(int bitsPerValue) {
			assert isSupported(bitsPerValue);
			return (overheadPerValue(bitsPerValue)) / bitsPerValue;
		}
	}

	public static class FormatAndBits {
		public final PackedInts.Format format;

		public final int bitsPerValue;

		public FormatAndBits(PackedInts.Format format, int bitsPerValue) {
			this.format = format;
			this.bitsPerValue = bitsPerValue;
		}

		@Override
		public String toString() {
			return ((("FormatAndBits(format=" + (format)) + " bitsPerValue=") + (bitsPerValue)) + ")";
		}
	}

	public static PackedInts.FormatAndBits fastestFormatAndBits(int valueCount, int bitsPerValue, float acceptableOverheadRatio) {
		if (valueCount == (-1)) {
			valueCount = Integer.MAX_VALUE;
		}
		acceptableOverheadRatio = Math.max(PackedInts.COMPACT, acceptableOverheadRatio);
		acceptableOverheadRatio = Math.min(PackedInts.FASTEST, acceptableOverheadRatio);
		float acceptableOverheadPerValue = acceptableOverheadRatio * bitsPerValue;
		int maxBitsPerValue = bitsPerValue + ((int) (acceptableOverheadPerValue));
		int actualBitsPerValue = -1;
		PackedInts.Format format = PackedInts.Format.PACKED;
		if ((bitsPerValue <= 8) && (maxBitsPerValue >= 8)) {
			actualBitsPerValue = 8;
		}else
			if ((bitsPerValue <= 16) && (maxBitsPerValue >= 16)) {
				actualBitsPerValue = 16;
			}else
				if ((bitsPerValue <= 32) && (maxBitsPerValue >= 32)) {
					actualBitsPerValue = 32;
				}else
					if ((bitsPerValue <= 64) && (maxBitsPerValue >= 64)) {
						actualBitsPerValue = 64;
					}else {
					}



		return new PackedInts.FormatAndBits(format, actualBitsPerValue);
	}

	public static interface Decoder {
		int longBlockCount();

		int longValueCount();

		int byteBlockCount();

		int byteValueCount();

		void decode(long[] blocks, int blocksOffset, long[] values, int valuesOffset, int iterations);

		void decode(byte[] blocks, int blocksOffset, long[] values, int valuesOffset, int iterations);

		void decode(long[] blocks, int blocksOffset, int[] values, int valuesOffset, int iterations);

		void decode(byte[] blocks, int blocksOffset, int[] values, int valuesOffset, int iterations);
	}

	public static interface Encoder {
		int longBlockCount();

		int longValueCount();

		int byteBlockCount();

		int byteValueCount();

		void encode(long[] values, int valuesOffset, long[] blocks, int blocksOffset, int iterations);

		void encode(long[] values, int valuesOffset, byte[] blocks, int blocksOffset, int iterations);

		void encode(int[] values, int valuesOffset, long[] blocks, int blocksOffset, int iterations);

		void encode(int[] values, int valuesOffset, byte[] blocks, int blocksOffset, int iterations);
	}

	public abstract static class Reader extends LegacyNumericDocValues implements Accountable {
		public int get(int index, long[] arr, int off, int len) {
			assert len > 0 : ("len must be > 0 (got " + len) + ")";
			assert (index >= 0) && (index < (size()));
			assert (off + len) <= (arr.length);
			final int gets = Math.min(((size()) - index), len);
			for (int i = index, o = off, end = index + gets; i < end; ++i , ++o) {
				arr[o] = get(i);
			}
			return gets;
		}

		public abstract int size();
	}

	public static interface ReaderIterator {
		long next() throws IOException;

		LongsRef next(int count) throws IOException;

		int getBitsPerValue();

		int size();

		int ord();
	}

	abstract static class ReaderIteratorImpl implements PackedInts.ReaderIterator {
		protected final DataInput in;

		protected final int bitsPerValue;

		protected final int valueCount;

		protected ReaderIteratorImpl(int valueCount, int bitsPerValue, DataInput in) {
			this.in = in;
			this.bitsPerValue = bitsPerValue;
			this.valueCount = valueCount;
		}

		@Override
		public long next() throws IOException {
			LongsRef nextValues = next(1);
			assert (nextValues.length) > 0;
			final long result = nextValues.longs[nextValues.offset];
			++(nextValues.offset);
			--(nextValues.length);
			return result;
		}

		@Override
		public int getBitsPerValue() {
			return bitsPerValue;
		}

		@Override
		public int size() {
			return valueCount;
		}
	}

	public abstract static class Mutable extends PackedInts.Reader {
		public abstract int getBitsPerValue();

		public abstract void set(int index, long value);

		public int set(int index, long[] arr, int off, int len) {
			assert len > 0 : ("len must be > 0 (got " + len) + ")";
			assert (index >= 0) && (index < (size()));
			len = Math.min(len, ((size()) - index));
			assert (off + len) <= (arr.length);
			for (int i = index, o = off, end = index + len; i < end; ++i , ++o) {
				set(i, arr[o]);
			}
			return len;
		}

		public void fill(int fromIndex, int toIndex, long val) {
			assert val <= (PackedInts.maxValue(getBitsPerValue()));
			assert fromIndex <= toIndex;
			for (int i = fromIndex; i < toIndex; ++i) {
				set(i, val);
			}
		}

		public void clear() {
			fill(0, size(), 0);
		}

		public void save(DataOutput out) throws IOException {
			PackedInts.Writer writer = PackedInts.getWriterNoHeader(out, getFormat(), size(), getBitsPerValue(), PackedInts.DEFAULT_BUFFER_SIZE);
			writer.writeHeader();
			for (int i = 0; i < (size()); ++i) {
				writer.add(get(i));
			}
			writer.finish();
		}

		PackedInts.Format getFormat() {
			return PackedInts.Format.PACKED;
		}
	}

	abstract static class ReaderImpl extends PackedInts.Reader {
		protected final int valueCount;

		protected ReaderImpl(int valueCount) {
			this.valueCount = valueCount;
		}

		@Override
		public abstract long get(int index);

		@Override
		public final int size() {
			return valueCount;
		}
	}

	abstract static class MutableImpl extends PackedInts.Mutable {
		protected final int valueCount;

		protected final int bitsPerValue;

		protected MutableImpl(int valueCount, int bitsPerValue) {
			this.valueCount = valueCount;
			assert (bitsPerValue > 0) && (bitsPerValue <= 64) : "bitsPerValue=" + bitsPerValue;
			this.bitsPerValue = bitsPerValue;
		}

		@Override
		public final int getBitsPerValue() {
			return bitsPerValue;
		}

		@Override
		public final int size() {
			return valueCount;
		}

		@Override
		public String toString() {
			return (((((getClass().getSimpleName()) + "(valueCount=") + (valueCount)) + ",bitsPerValue=") + (bitsPerValue)) + ")";
		}
	}

	public static final class NullReader extends PackedInts.Reader {
		private final int valueCount;

		public NullReader(int valueCount) {
			this.valueCount = valueCount;
		}

		@Override
		public long get(int index) {
			return 0;
		}

		@Override
		public int get(int index, long[] arr, int off, int len) {
			assert len > 0 : ("len must be > 0 (got " + len) + ")";
			assert (index >= 0) && (index < (valueCount));
			len = Math.min(len, ((valueCount) - index));
			Arrays.fill(arr, off, (off + len), 0);
			return len;
		}

		@Override
		public int size() {
			return valueCount;
		}

		@Override
		public long ramBytesUsed() {
			return RamUsageEstimator.alignObjectSize(((RamUsageEstimator.NUM_BYTES_OBJECT_HEADER) + (Integer.BYTES)));
		}
	}

	public abstract static class Writer {
		protected final DataOutput out;

		protected final int valueCount;

		protected final int bitsPerValue;

		protected Writer(DataOutput out, int valueCount, int bitsPerValue) {
			assert bitsPerValue <= 64;
			assert (valueCount >= 0) || (valueCount == (-1));
			this.out = out;
			this.valueCount = valueCount;
			this.bitsPerValue = bitsPerValue;
		}

		void writeHeader() throws IOException {
			assert (valueCount) != (-1);
			CodecUtil.writeHeader(out, PackedInts.CODEC_NAME, PackedInts.VERSION_CURRENT);
			out.writeVInt(bitsPerValue);
			out.writeVInt(valueCount);
			out.writeVInt(getFormat().getId());
		}

		protected abstract PackedInts.Format getFormat();

		public abstract void add(long v) throws IOException;

		public final int bitsPerValue() {
			return bitsPerValue;
		}

		public abstract void finish() throws IOException;

		public abstract int ord();
	}

	public static PackedInts.Decoder getDecoder(PackedInts.Format format, int version, int bitsPerValue) {
		PackedInts.checkVersion(version);
		return null;
	}

	public static PackedInts.Encoder getEncoder(PackedInts.Format format, int version, int bitsPerValue) {
		PackedInts.checkVersion(version);
		return null;
	}

	public static PackedInts.Reader getReaderNoHeader(DataInput in, PackedInts.Format format, int version, int valueCount, int bitsPerValue) throws IOException {
		PackedInts.checkVersion(version);
		return null;
	}

	public static PackedInts.Reader getReader(DataInput in) throws IOException {
		final int version = CodecUtil.checkHeader(in, PackedInts.CODEC_NAME, PackedInts.VERSION_START, PackedInts.VERSION_CURRENT);
		final int bitsPerValue = in.readVInt();
		assert (bitsPerValue > 0) && (bitsPerValue <= 64) : "bitsPerValue=" + bitsPerValue;
		final int valueCount = in.readVInt();
		final PackedInts.Format format = PackedInts.Format.byId(in.readVInt());
		return PackedInts.getReaderNoHeader(in, format, version, valueCount, bitsPerValue);
	}

	public static PackedInts.ReaderIterator getReaderIteratorNoHeader(DataInput in, PackedInts.Format format, int version, int valueCount, int bitsPerValue, int mem) {
		PackedInts.checkVersion(version);
		return null;
	}

	public static PackedInts.ReaderIterator getReaderIterator(DataInput in, int mem) throws IOException {
		final int version = CodecUtil.checkHeader(in, PackedInts.CODEC_NAME, PackedInts.VERSION_START, PackedInts.VERSION_CURRENT);
		final int bitsPerValue = in.readVInt();
		assert (bitsPerValue > 0) && (bitsPerValue <= 64) : "bitsPerValue=" + bitsPerValue;
		final int valueCount = in.readVInt();
		final PackedInts.Format format = PackedInts.Format.byId(in.readVInt());
		return PackedInts.getReaderIteratorNoHeader(in, format, version, valueCount, bitsPerValue, mem);
	}

	public static PackedInts.Reader getDirectReaderNoHeader(final IndexInput in, PackedInts.Format format, int version, int valueCount, int bitsPerValue) {
		PackedInts.checkVersion(version);
		return null;
	}

	public static PackedInts.Reader getDirectReader(IndexInput in) throws IOException {
		final int version = CodecUtil.checkHeader(in, PackedInts.CODEC_NAME, PackedInts.VERSION_START, PackedInts.VERSION_CURRENT);
		final int bitsPerValue = in.readVInt();
		assert (bitsPerValue > 0) && (bitsPerValue <= 64) : "bitsPerValue=" + bitsPerValue;
		final int valueCount = in.readVInt();
		final PackedInts.Format format = PackedInts.Format.byId(in.readVInt());
		return PackedInts.getDirectReaderNoHeader(in, format, version, valueCount, bitsPerValue);
	}

	public static PackedInts.Mutable getMutable(int valueCount, int bitsPerValue, float acceptableOverheadRatio) {
		final PackedInts.FormatAndBits formatAndBits = PackedInts.fastestFormatAndBits(valueCount, bitsPerValue, acceptableOverheadRatio);
		return PackedInts.getMutable(valueCount, formatAndBits.bitsPerValue, formatAndBits.format);
	}

	public static PackedInts.Mutable getMutable(int valueCount, int bitsPerValue, PackedInts.Format format) {
		assert valueCount >= 0;
		return null;
	}

	public static PackedInts.Writer getWriterNoHeader(DataOutput out, PackedInts.Format format, int valueCount, int bitsPerValue, int mem) {
		return null;
	}

	public static PackedInts.Writer getWriter(DataOutput out, int valueCount, int bitsPerValue, float acceptableOverheadRatio) throws IOException {
		assert valueCount >= 0;
		final PackedInts.FormatAndBits formatAndBits = PackedInts.fastestFormatAndBits(valueCount, bitsPerValue, acceptableOverheadRatio);
		final PackedInts.Writer writer = PackedInts.getWriterNoHeader(out, formatAndBits.format, valueCount, formatAndBits.bitsPerValue, PackedInts.DEFAULT_BUFFER_SIZE);
		writer.writeHeader();
		return writer;
	}

	public static int bitsRequired(long maxValue) {
		if (maxValue < 0) {
			throw new IllegalArgumentException((("maxValue must be non-negative (got: " + maxValue) + ")"));
		}
		return PackedInts.unsignedBitsRequired(maxValue);
	}

	public static int unsignedBitsRequired(long bits) {
		return Math.max(1, (64 - (Long.numberOfLeadingZeros(bits))));
	}

	public static long maxValue(int bitsPerValue) {
		return bitsPerValue == 64 ? Long.MAX_VALUE : ~((~0L) << bitsPerValue);
	}

	public static void copy(PackedInts.Reader src, int srcPos, PackedInts.Mutable dest, int destPos, int len, int mem) {
		assert (srcPos + len) <= (src.size());
		assert (destPos + len) <= (dest.size());
		final int capacity = mem >>> 3;
		if (capacity == 0) {
			for (int i = 0; i < len; ++i) {
				dest.set((destPos++), src.get((srcPos++)));
			}
		}else
			if (len > 0) {
				final long[] buf = new long[Math.min(capacity, len)];
				PackedInts.copy(src, srcPos, dest, destPos, len, buf);
			}

	}

	static void copy(PackedInts.Reader src, int srcPos, PackedInts.Mutable dest, int destPos, int len, long[] buf) {
		assert (buf.length) > 0;
		int remaining = 0;
		while (len > 0) {
			final int read = src.get(srcPos, buf, remaining, Math.min(len, ((buf.length) - remaining)));
			assert read > 0;
			srcPos += read;
			len -= read;
			remaining += read;
			final int written = dest.set(destPos, buf, 0, remaining);
			assert written > 0;
			destPos += written;
			if (written < remaining) {
				System.arraycopy(buf, written, buf, 0, (remaining - written));
			}
			remaining -= written;
		} 
		while (remaining > 0) {
			final int written = dest.set(destPos, buf, 0, remaining);
			destPos += written;
			remaining -= written;
			System.arraycopy(buf, written, buf, 0, remaining);
		} 
	}

	static int checkBlockSize(int blockSize, int minBlockSize, int maxBlockSize) {
		if ((blockSize < minBlockSize) || (blockSize > maxBlockSize)) {
			throw new IllegalArgumentException(((((("blockSize must be >= " + minBlockSize) + " and <= ") + maxBlockSize) + ", got ") + blockSize));
		}
		if ((blockSize & (blockSize - 1)) != 0) {
			throw new IllegalArgumentException(("blockSize must be a power of two, got " + blockSize));
		}
		return Integer.numberOfTrailingZeros(blockSize);
	}

	static int numBlocks(long size, int blockSize) {
		final int numBlocks = ((int) (size / blockSize)) + ((size % blockSize) == 0 ? 0 : 1);
		if ((((long) (numBlocks)) * blockSize) < size) {
			throw new IllegalArgumentException("size is too large for this block size");
		}
		return numBlocks;
	}
}

