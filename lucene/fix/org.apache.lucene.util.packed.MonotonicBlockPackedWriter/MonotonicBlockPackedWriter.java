

import java.io.IOException;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.packed.PackedInts;


public final class MonotonicBlockPackedWriter {
	public MonotonicBlockPackedWriter(DataOutput out, int blockSize) {
	}

	public void add(long l) throws IOException {
		assert l >= 0;
	}

	protected void flush() throws IOException {
		long maxDelta = 0;
		if (maxDelta == 0) {
		}else {
			final int bitsRequired = PackedInts.bitsRequired(maxDelta);
		}
	}
}

