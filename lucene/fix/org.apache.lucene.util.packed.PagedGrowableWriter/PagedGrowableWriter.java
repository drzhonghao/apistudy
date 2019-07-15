

import org.apache.lucene.util.packed.GrowableWriter;
import org.apache.lucene.util.packed.PackedInts;


public final class PagedGrowableWriter {
	final float acceptableOverheadRatio;

	public PagedGrowableWriter(long size, int pageSize, int startBitsPerValue, float acceptableOverheadRatio) {
		this(size, pageSize, startBitsPerValue, acceptableOverheadRatio, true);
	}

	PagedGrowableWriter(long size, int pageSize, int startBitsPerValue, float acceptableOverheadRatio, boolean fillPages) {
		this.acceptableOverheadRatio = acceptableOverheadRatio;
		if (fillPages) {
		}
	}

	protected PackedInts.Mutable newMutable(int valueCount, int bitsPerValue) {
		return new GrowableWriter(bitsPerValue, valueCount, acceptableOverheadRatio);
	}

	protected PagedGrowableWriter newUnfilledCopy(long newSize) {
		return null;
	}

	protected long baseRamBytesUsed() {
		return 0l;
	}
}

