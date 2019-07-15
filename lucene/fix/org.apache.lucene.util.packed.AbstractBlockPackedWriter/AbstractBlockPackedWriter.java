

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.packed.PackedInts;

import static org.apache.lucene.util.packed.PackedInts.Format.PACKED;


abstract class AbstractBlockPackedWriter {
	static final int MIN_BLOCK_SIZE = 64;

	static final int MAX_BLOCK_SIZE = 1 << (30 - 3);

	static final int MIN_VALUE_EQUALS_0 = 1 << 0;

	static final int BPV_SHIFT = 1;

	static void writeVLong(DataOutput out, long i) throws IOException {
		int k = 0;
		while (((i & (~127L)) != 0L) && ((k++) < 8)) {
			out.writeByte(((byte) ((i & 127L) | 128L)));
			i >>>= 7;
		} 
		out.writeByte(((byte) (i)));
	}

	protected DataOutput out;

	protected final long[] values;

	protected byte[] blocks;

	protected int off;

	protected long ord;

	protected boolean finished;

	public AbstractBlockPackedWriter(DataOutput out, int blockSize) {
		reset(out);
		values = new long[blockSize];
	}

	public void reset(DataOutput out) {
		assert out != null;
		this.out = out;
		off = 0;
		ord = 0L;
		finished = false;
	}

	private void checkNotFinished() {
		if (finished) {
			throw new IllegalStateException("Already finished");
		}
	}

	public void add(long l) throws IOException {
		checkNotFinished();
		if ((off) == (values.length)) {
			flush();
		}
		values[((off)++)] = l;
		++(ord);
	}

	void addBlockOfZeros() throws IOException {
		checkNotFinished();
		if (((off) != 0) && ((off) != (values.length))) {
			throw new IllegalStateException(("" + (off)));
		}
		if ((off) == (values.length)) {
			flush();
		}
		Arrays.fill(values, 0);
		off = values.length;
		ord += values.length;
	}

	public void finish() throws IOException {
		checkNotFinished();
		if ((off) > 0) {
			flush();
		}
		finished = true;
	}

	public long ord() {
		return ord;
	}

	protected abstract void flush() throws IOException;

	protected final void writeValues(int bitsRequired) throws IOException {
		final PackedInts.Encoder encoder = PackedInts.getEncoder(PACKED, PackedInts.VERSION_CURRENT, bitsRequired);
		final int iterations = (values.length) / (encoder.byteValueCount());
		final int blockSize = (encoder.byteBlockCount()) * iterations;
		if (((blocks) == null) || ((blocks.length) < blockSize)) {
			blocks = new byte[blockSize];
		}
		if ((off) < (values.length)) {
			Arrays.fill(values, off, values.length, 0L);
		}
		encoder.encode(values, 0, blocks, 0, iterations);
		final int blockCount = ((int) (PACKED.byteCount(PackedInts.VERSION_CURRENT, off, bitsRequired)));
		out.writeBytes(blocks, blockCount);
	}
}

