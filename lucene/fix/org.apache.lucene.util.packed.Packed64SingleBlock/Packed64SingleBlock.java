

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.packed.PackedInts;

import static org.apache.lucene.util.packed.PackedInts.Format.PACKED_SINGLE_BLOCK;


abstract class Packed64SingleBlock {
	public static final int MAX_SUPPORTED_BITS_PER_VALUE = 32;

	private static final int[] SUPPORTED_BITS_PER_VALUE = new int[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 16, 21, 32 };

	public static boolean isSupported(int bitsPerValue) {
		return (Arrays.binarySearch(Packed64SingleBlock.SUPPORTED_BITS_PER_VALUE, bitsPerValue)) >= 0;
	}

	private static int requiredCapacity(int valueCount, int valuesPerBlock) {
		return (valueCount / valuesPerBlock) + ((valueCount % valuesPerBlock) == 0 ? 0 : 1);
	}

	final long[] blocks;

	Packed64SingleBlock(int valueCount, int bitsPerValue) {
		assert Packed64SingleBlock.isSupported(bitsPerValue);
		final int valuesPerBlock = 64 / bitsPerValue;
		blocks = new long[Packed64SingleBlock.requiredCapacity(valueCount, valuesPerBlock)];
	}

	public void clear() {
		Arrays.fill(blocks, 0L);
	}

	public long ramBytesUsed() {
		return (RamUsageEstimator.alignObjectSize((((RamUsageEstimator.NUM_BYTES_OBJECT_HEADER) + (2 * (RamUsageEstimator.NUM_BYTES_INT))) + (RamUsageEstimator.NUM_BYTES_OBJECT_REF)))) + (RamUsageEstimator.sizeOf(blocks));
	}

	public int get(int index, long[] arr, int off, int len) {
		assert len > 0 : ("len must be > 0 (got " + len) + ")";
		assert (off + len) <= (arr.length);
		final int originalIndex = index;
		if (index > originalIndex) {
			return index - originalIndex;
		}else {
			assert index == originalIndex;
		}
		return 0;
	}

	public int set(int index, long[] arr, int off, int len) {
		assert len > 0 : ("len must be > 0 (got " + len) + ")";
		assert (off + len) <= (arr.length);
		final int originalIndex = index;
		if (index > originalIndex) {
			return index - originalIndex;
		}else {
			assert index == originalIndex;
		}
		return 0;
	}

	public void fill(int fromIndex, int toIndex, long val) {
		assert fromIndex >= 0;
		assert fromIndex <= toIndex;
		long blockValue = 0L;
	}

	protected PackedInts.Format getFormat() {
		return PACKED_SINGLE_BLOCK;
	}

	@Override
	public String toString() {
		return null;
	}

	public static Packed64SingleBlock create(DataInput in, int valueCount, int bitsPerValue) throws IOException {
		Packed64SingleBlock reader = Packed64SingleBlock.create(valueCount, bitsPerValue);
		for (int i = 0; i < (reader.blocks.length); ++i) {
			reader.blocks[i] = in.readLong();
		}
		return reader;
	}

	public static Packed64SingleBlock create(int valueCount, int bitsPerValue) {
		switch (bitsPerValue) {
			case 1 :
				return new Packed64SingleBlock.Packed64SingleBlock1(valueCount);
			case 2 :
				return new Packed64SingleBlock.Packed64SingleBlock2(valueCount);
			case 3 :
				return new Packed64SingleBlock.Packed64SingleBlock3(valueCount);
			case 4 :
				return new Packed64SingleBlock.Packed64SingleBlock4(valueCount);
			case 5 :
				return new Packed64SingleBlock.Packed64SingleBlock5(valueCount);
			case 6 :
				return new Packed64SingleBlock.Packed64SingleBlock6(valueCount);
			case 7 :
				return new Packed64SingleBlock.Packed64SingleBlock7(valueCount);
			case 8 :
				return new Packed64SingleBlock.Packed64SingleBlock8(valueCount);
			case 9 :
				return new Packed64SingleBlock.Packed64SingleBlock9(valueCount);
			case 10 :
				return new Packed64SingleBlock.Packed64SingleBlock10(valueCount);
			case 12 :
				return new Packed64SingleBlock.Packed64SingleBlock12(valueCount);
			case 16 :
				return new Packed64SingleBlock.Packed64SingleBlock16(valueCount);
			case 21 :
				return new Packed64SingleBlock.Packed64SingleBlock21(valueCount);
			case 32 :
				return new Packed64SingleBlock.Packed64SingleBlock32(valueCount);
			default :
				throw new IllegalArgumentException(("Unsupported number of bits per value: " + 32));
		}
	}

	static class Packed64SingleBlock1 extends Packed64SingleBlock {
		Packed64SingleBlock1(int valueCount) {
			super(valueCount, 1);
		}

		public long get(int index) {
			final int o = index >>> 6;
			final int b = index & 63;
			final int shift = b << 0;
			return ((blocks[o]) >>> shift) & 1L;
		}

		public void set(int index, long value) {
			final int o = index >>> 6;
			final int b = index & 63;
			final int shift = b << 0;
			blocks[o] = ((blocks[o]) & (~(1L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock2 extends Packed64SingleBlock {
		Packed64SingleBlock2(int valueCount) {
			super(valueCount, 2);
		}

		public long get(int index) {
			final int o = index >>> 5;
			final int b = index & 31;
			final int shift = b << 1;
			return ((blocks[o]) >>> shift) & 3L;
		}

		public void set(int index, long value) {
			final int o = index >>> 5;
			final int b = index & 31;
			final int shift = b << 1;
			blocks[o] = ((blocks[o]) & (~(3L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock3 extends Packed64SingleBlock {
		Packed64SingleBlock3(int valueCount) {
			super(valueCount, 3);
		}

		public long get(int index) {
			final int o = index / 21;
			final int b = index % 21;
			final int shift = b * 3;
			return ((blocks[o]) >>> shift) & 7L;
		}

		public void set(int index, long value) {
			final int o = index / 21;
			final int b = index % 21;
			final int shift = b * 3;
			blocks[o] = ((blocks[o]) & (~(7L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock4 extends Packed64SingleBlock {
		Packed64SingleBlock4(int valueCount) {
			super(valueCount, 4);
		}

		public long get(int index) {
			final int o = index >>> 4;
			final int b = index & 15;
			final int shift = b << 2;
			return ((blocks[o]) >>> shift) & 15L;
		}

		public void set(int index, long value) {
			final int o = index >>> 4;
			final int b = index & 15;
			final int shift = b << 2;
			blocks[o] = ((blocks[o]) & (~(15L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock5 extends Packed64SingleBlock {
		Packed64SingleBlock5(int valueCount) {
			super(valueCount, 5);
		}

		public long get(int index) {
			final int o = index / 12;
			final int b = index % 12;
			final int shift = b * 5;
			return ((blocks[o]) >>> shift) & 31L;
		}

		public void set(int index, long value) {
			final int o = index / 12;
			final int b = index % 12;
			final int shift = b * 5;
			blocks[o] = ((blocks[o]) & (~(31L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock6 extends Packed64SingleBlock {
		Packed64SingleBlock6(int valueCount) {
			super(valueCount, 6);
		}

		public long get(int index) {
			final int o = index / 10;
			final int b = index % 10;
			final int shift = b * 6;
			return ((blocks[o]) >>> shift) & 63L;
		}

		public void set(int index, long value) {
			final int o = index / 10;
			final int b = index % 10;
			final int shift = b * 6;
			blocks[o] = ((blocks[o]) & (~(63L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock7 extends Packed64SingleBlock {
		Packed64SingleBlock7(int valueCount) {
			super(valueCount, 7);
		}

		public long get(int index) {
			final int o = index / 9;
			final int b = index % 9;
			final int shift = b * 7;
			return ((blocks[o]) >>> shift) & 127L;
		}

		public void set(int index, long value) {
			final int o = index / 9;
			final int b = index % 9;
			final int shift = b * 7;
			blocks[o] = ((blocks[o]) & (~(127L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock8 extends Packed64SingleBlock {
		Packed64SingleBlock8(int valueCount) {
			super(valueCount, 8);
		}

		public long get(int index) {
			final int o = index >>> 3;
			final int b = index & 7;
			final int shift = b << 3;
			return ((blocks[o]) >>> shift) & 255L;
		}

		public void set(int index, long value) {
			final int o = index >>> 3;
			final int b = index & 7;
			final int shift = b << 3;
			blocks[o] = ((blocks[o]) & (~(255L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock9 extends Packed64SingleBlock {
		Packed64SingleBlock9(int valueCount) {
			super(valueCount, 9);
		}

		public long get(int index) {
			final int o = index / 7;
			final int b = index % 7;
			final int shift = b * 9;
			return ((blocks[o]) >>> shift) & 511L;
		}

		public void set(int index, long value) {
			final int o = index / 7;
			final int b = index % 7;
			final int shift = b * 9;
			blocks[o] = ((blocks[o]) & (~(511L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock10 extends Packed64SingleBlock {
		Packed64SingleBlock10(int valueCount) {
			super(valueCount, 10);
		}

		public long get(int index) {
			final int o = index / 6;
			final int b = index % 6;
			final int shift = b * 10;
			return ((blocks[o]) >>> shift) & 1023L;
		}

		public void set(int index, long value) {
			final int o = index / 6;
			final int b = index % 6;
			final int shift = b * 10;
			blocks[o] = ((blocks[o]) & (~(1023L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock12 extends Packed64SingleBlock {
		Packed64SingleBlock12(int valueCount) {
			super(valueCount, 12);
		}

		public long get(int index) {
			final int o = index / 5;
			final int b = index % 5;
			final int shift = b * 12;
			return ((blocks[o]) >>> shift) & 4095L;
		}

		public void set(int index, long value) {
			final int o = index / 5;
			final int b = index % 5;
			final int shift = b * 12;
			blocks[o] = ((blocks[o]) & (~(4095L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock16 extends Packed64SingleBlock {
		Packed64SingleBlock16(int valueCount) {
			super(valueCount, 16);
		}

		public long get(int index) {
			final int o = index >>> 2;
			final int b = index & 3;
			final int shift = b << 4;
			return ((blocks[o]) >>> shift) & 65535L;
		}

		public void set(int index, long value) {
			final int o = index >>> 2;
			final int b = index & 3;
			final int shift = b << 4;
			blocks[o] = ((blocks[o]) & (~(65535L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock21 extends Packed64SingleBlock {
		Packed64SingleBlock21(int valueCount) {
			super(valueCount, 21);
		}

		public long get(int index) {
			final int o = index / 3;
			final int b = index % 3;
			final int shift = b * 21;
			return ((blocks[o]) >>> shift) & 2097151L;
		}

		public void set(int index, long value) {
			final int o = index / 3;
			final int b = index % 3;
			final int shift = b * 21;
			blocks[o] = ((blocks[o]) & (~(2097151L << shift))) | (value << shift);
		}
	}

	static class Packed64SingleBlock32 extends Packed64SingleBlock {
		Packed64SingleBlock32(int valueCount) {
			super(valueCount, 32);
		}

		public long get(int index) {
			final int o = index >>> 1;
			final int b = index & 1;
			final int shift = b << 5;
			return ((blocks[o]) >>> shift) & 4294967295L;
		}

		public void set(int index, long value) {
			final int o = index >>> 1;
			final int b = index & 1;
			final int shift = b << 5;
			blocks[o] = ((blocks[o]) & (~(4294967295L << shift))) | (value << shift);
		}
	}
}

