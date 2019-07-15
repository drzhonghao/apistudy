

import java.io.IOException;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.packed.PackedInts;


public final class BlockPackedWriter {
	public BlockPackedWriter(DataOutput out, int blockSize) {
	}

	protected void flush() throws IOException {
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		final long delta = max - min;
		int bitsRequired = (delta == 0) ? 0 : PackedInts.unsignedBitsRequired(delta);
		if (bitsRequired == 64) {
			min = 0L;
		}else
			if (min > 0L) {
				min = Math.max(0L, (max - (PackedInts.maxValue(bitsRequired))));
			}

		if (min != 0) {
		}
		if (bitsRequired > 0) {
			if (min != 0) {
			}
		}
	}
}

