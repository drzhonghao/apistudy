

import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.ByteBlockPool;


final class ByteSliceWriter extends DataOutput {
	private byte[] slice;

	private int upto;

	private final ByteBlockPool pool;

	int offset0;

	public ByteSliceWriter(ByteBlockPool pool) {
		this.pool = pool;
	}

	public void init(int address) {
		slice = pool.buffers[(address >> (ByteBlockPool.BYTE_BLOCK_SHIFT))];
		assert (slice) != null;
		upto = address & (ByteBlockPool.BYTE_BLOCK_MASK);
		offset0 = address;
		assert (upto) < (slice.length);
	}

	@Override
	public void writeByte(byte b) {
		assert (slice) != null;
		if ((slice[upto]) != 0) {
			upto = pool.allocSlice(slice, upto);
			slice = pool.buffer;
			offset0 = pool.byteOffset;
			assert (slice) != null;
		}
		slice[((upto)++)] = b;
		assert (upto) != (slice.length);
	}

	@Override
	public void writeBytes(final byte[] b, int offset, final int len) {
		final int offsetEnd = offset + len;
		while (offset < offsetEnd) {
			if ((slice[upto]) != 0) {
				upto = pool.allocSlice(slice, upto);
				slice = pool.buffer;
				offset0 = pool.byteOffset;
			}
			slice[((upto)++)] = b[(offset++)];
			assert (upto) != (slice.length);
		} 
	}

	public int getAddress() {
		return 0;
	}
}

