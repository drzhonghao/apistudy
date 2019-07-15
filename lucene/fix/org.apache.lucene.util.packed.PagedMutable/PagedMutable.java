

import org.apache.lucene.util.packed.PackedInts;


public final class PagedMutable {
	final PackedInts.Format format;

	public PagedMutable(long size, int pageSize, int bitsPerValue, float acceptableOverheadRatio) {
		this(size, pageSize, PackedInts.fastestFormatAndBits(pageSize, bitsPerValue, acceptableOverheadRatio));
	}

	PagedMutable(long size, int pageSize, PackedInts.FormatAndBits formatAndBits) {
		this(size, pageSize, formatAndBits.bitsPerValue, formatAndBits.format);
	}

	PagedMutable(long size, int pageSize, int bitsPerValue, PackedInts.Format format) {
		this.format = format;
	}

	protected PackedInts.Mutable newMutable(int valueCount, int bitsPerValue) {
		return null;
	}

	protected PagedMutable newUnfilledCopy(long newSize) {
		return null;
	}

	protected long baseRamBytesUsed() {
		return 0L;
	}
}

