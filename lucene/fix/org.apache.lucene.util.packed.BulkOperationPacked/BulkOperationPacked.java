

import org.apache.lucene.util.packed.PackedInts;


class BulkOperationPacked {
	private final int bitsPerValue;

	private final int longBlockCount;

	private final int longValueCount;

	private final int byteBlockCount;

	private final int byteValueCount;

	private final long mask;

	private final int intMask;

	public BulkOperationPacked(int bitsPerValue) {
		this.bitsPerValue = bitsPerValue;
		assert (bitsPerValue > 0) && (bitsPerValue <= 64);
		int blocks = bitsPerValue;
		while ((blocks & 1) == 0) {
			blocks >>>= 1;
		} 
		this.longBlockCount = blocks;
		this.longValueCount = (64 * (longBlockCount)) / bitsPerValue;
		int byteBlockCount = 8 * (longBlockCount);
		int byteValueCount = longValueCount;
		while (((byteBlockCount & 1) == 0) && ((byteValueCount & 1) == 0)) {
			byteBlockCount >>>= 1;
			byteValueCount >>>= 1;
		} 
		this.byteBlockCount = byteBlockCount;
		this.byteValueCount = byteValueCount;
		if (bitsPerValue == 64) {
			this.mask = ~0L;
		}else {
			this.mask = (1L << bitsPerValue) - 1;
		}
		this.intMask = ((int) (mask));
		assert ((longValueCount) * bitsPerValue) == (64 * (longBlockCount));
	}

	public int longBlockCount() {
		return longBlockCount;
	}

	public int longValueCount() {
		return longValueCount;
	}

	public int byteBlockCount() {
		return byteBlockCount;
	}

	public int byteValueCount() {
		return byteValueCount;
	}

	public void decode(long[] blocks, int blocksOffset, long[] values, int valuesOffset, int iterations) {
		int bitsLeft = 64;
		for (int i = 0; i < ((longValueCount) * iterations); ++i) {
			bitsLeft -= bitsPerValue;
			if (bitsLeft < 0) {
				values[(valuesOffset++)] = (((blocks[(blocksOffset++)]) & ((1L << ((bitsPerValue) + bitsLeft)) - 1)) << (-bitsLeft)) | ((blocks[blocksOffset]) >>> (64 + bitsLeft));
				bitsLeft += 64;
			}else {
				values[(valuesOffset++)] = ((blocks[blocksOffset]) >>> bitsLeft) & (mask);
			}
		}
	}

	public void decode(byte[] blocks, int blocksOffset, long[] values, int valuesOffset, int iterations) {
		long nextValue = 0L;
		int bitsLeft = bitsPerValue;
		for (int i = 0; i < (iterations * (byteBlockCount)); ++i) {
			final long bytes = (blocks[(blocksOffset++)]) & 255L;
			if (bitsLeft > 8) {
				bitsLeft -= 8;
				nextValue |= bytes << bitsLeft;
			}else {
				int bits = 8 - bitsLeft;
				values[(valuesOffset++)] = nextValue | (bytes >>> bits);
				while (bits >= (bitsPerValue)) {
					bits -= bitsPerValue;
					values[(valuesOffset++)] = (bytes >>> bits) & (mask);
				} 
				bitsLeft = (bitsPerValue) - bits;
				nextValue = (bytes & ((1L << bits) - 1)) << bitsLeft;
			}
		}
		assert bitsLeft == (bitsPerValue);
	}

	public void decode(long[] blocks, int blocksOffset, int[] values, int valuesOffset, int iterations) {
		if ((bitsPerValue) > 32) {
			throw new UnsupportedOperationException((("Cannot decode " + (bitsPerValue)) + "-bits values into an int[]"));
		}
		int bitsLeft = 64;
		for (int i = 0; i < ((longValueCount) * iterations); ++i) {
			bitsLeft -= bitsPerValue;
			if (bitsLeft < 0) {
				values[(valuesOffset++)] = ((int) ((((blocks[(blocksOffset++)]) & ((1L << ((bitsPerValue) + bitsLeft)) - 1)) << (-bitsLeft)) | ((blocks[blocksOffset]) >>> (64 + bitsLeft))));
				bitsLeft += 64;
			}else {
				values[(valuesOffset++)] = ((int) (((blocks[blocksOffset]) >>> bitsLeft) & (mask)));
			}
		}
	}

	public void decode(byte[] blocks, int blocksOffset, int[] values, int valuesOffset, int iterations) {
		int nextValue = 0;
		int bitsLeft = bitsPerValue;
		for (int i = 0; i < (iterations * (byteBlockCount)); ++i) {
			final int bytes = (blocks[(blocksOffset++)]) & 255;
			if (bitsLeft > 8) {
				bitsLeft -= 8;
				nextValue |= bytes << bitsLeft;
			}else {
				int bits = 8 - bitsLeft;
				values[(valuesOffset++)] = nextValue | (bytes >>> bits);
				while (bits >= (bitsPerValue)) {
					bits -= bitsPerValue;
					values[(valuesOffset++)] = (bytes >>> bits) & (intMask);
				} 
				bitsLeft = (bitsPerValue) - bits;
				nextValue = (bytes & ((1 << bits) - 1)) << bitsLeft;
			}
		}
		assert bitsLeft == (bitsPerValue);
	}

	public void encode(long[] values, int valuesOffset, long[] blocks, int blocksOffset, int iterations) {
		long nextBlock = 0;
		int bitsLeft = 64;
		for (int i = 0; i < ((longValueCount) * iterations); ++i) {
			bitsLeft -= bitsPerValue;
			if (bitsLeft > 0) {
				nextBlock |= (values[(valuesOffset++)]) << bitsLeft;
			}else
				if (bitsLeft == 0) {
					nextBlock |= values[(valuesOffset++)];
					blocks[(blocksOffset++)] = nextBlock;
					nextBlock = 0;
					bitsLeft = 64;
				}else {
					nextBlock |= (values[valuesOffset]) >>> (-bitsLeft);
					blocks[(blocksOffset++)] = nextBlock;
					nextBlock = ((values[(valuesOffset++)]) & ((1L << (-bitsLeft)) - 1)) << (64 + bitsLeft);
					bitsLeft += 64;
				}

		}
	}

	public void encode(int[] values, int valuesOffset, long[] blocks, int blocksOffset, int iterations) {
		long nextBlock = 0;
		int bitsLeft = 64;
		for (int i = 0; i < ((longValueCount) * iterations); ++i) {
			bitsLeft -= bitsPerValue;
			if (bitsLeft > 0) {
				nextBlock |= ((values[(valuesOffset++)]) & 4294967295L) << bitsLeft;
			}else
				if (bitsLeft == 0) {
					nextBlock |= (values[(valuesOffset++)]) & 4294967295L;
					blocks[(blocksOffset++)] = nextBlock;
					nextBlock = 0;
					bitsLeft = 64;
				}else {
					nextBlock |= ((values[valuesOffset]) & 4294967295L) >>> (-bitsLeft);
					blocks[(blocksOffset++)] = nextBlock;
					nextBlock = ((values[(valuesOffset++)]) & ((1L << (-bitsLeft)) - 1)) << (64 + bitsLeft);
					bitsLeft += 64;
				}

		}
	}

	public void encode(long[] values, int valuesOffset, byte[] blocks, int blocksOffset, int iterations) {
		int nextBlock = 0;
		int bitsLeft = 8;
		for (int i = 0; i < ((byteValueCount) * iterations); ++i) {
			final long v = values[(valuesOffset++)];
			assert (PackedInts.unsignedBitsRequired(v)) <= (bitsPerValue);
			if ((bitsPerValue) < bitsLeft) {
				nextBlock |= v << (bitsLeft - (bitsPerValue));
				bitsLeft -= bitsPerValue;
			}else {
				int bits = (bitsPerValue) - bitsLeft;
				blocks[(blocksOffset++)] = ((byte) (nextBlock | (v >>> bits)));
				while (bits >= 8) {
					bits -= 8;
					blocks[(blocksOffset++)] = ((byte) (v >>> bits));
				} 
				bitsLeft = 8 - bits;
				nextBlock = ((int) ((v & ((1L << bits) - 1)) << bitsLeft));
			}
		}
		assert bitsLeft == 8;
	}

	public void encode(int[] values, int valuesOffset, byte[] blocks, int blocksOffset, int iterations) {
		int nextBlock = 0;
		int bitsLeft = 8;
		for (int i = 0; i < ((byteValueCount) * iterations); ++i) {
			final int v = values[(valuesOffset++)];
			assert (PackedInts.bitsRequired((v & 4294967295L))) <= (bitsPerValue);
			if ((bitsPerValue) < bitsLeft) {
				nextBlock |= v << (bitsLeft - (bitsPerValue));
				bitsLeft -= bitsPerValue;
			}else {
				int bits = (bitsPerValue) - bitsLeft;
				blocks[(blocksOffset++)] = ((byte) (nextBlock | (v >>> bits)));
				while (bits >= 8) {
					bits -= 8;
					blocks[(blocksOffset++)] = ((byte) (v >>> bits));
				} 
				bitsLeft = 8 - bits;
				nextBlock = (v & ((1 << bits) - 1)) << bitsLeft;
			}
		}
		assert bitsLeft == 8;
	}
}

