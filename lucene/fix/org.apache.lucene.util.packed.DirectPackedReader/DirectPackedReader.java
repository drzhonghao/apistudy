

import java.io.IOException;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IndexInput;


class DirectPackedReader {
	final IndexInput in;

	final int bitsPerValue;

	final long startPointer;

	final long valueMask;

	DirectPackedReader(int bitsPerValue, int valueCount, IndexInput in) {
		this.in = in;
		this.bitsPerValue = bitsPerValue;
		startPointer = in.getFilePointer();
		if (bitsPerValue == 64) {
			valueMask = -1L;
		}else {
			valueMask = (1L << bitsPerValue) - 1;
		}
	}

	public long get(int index) {
		final long majorBitPos = ((long) (index)) * (bitsPerValue);
		final long elementPos = majorBitPos >>> 3;
		try {
			in.seek(((startPointer) + elementPos));
			final int bitPos = ((int) (majorBitPos & 7));
			final int roundedBits = ((bitPos + (bitsPerValue)) + 7) & (~7);
			int shiftRightBits = (roundedBits - bitPos) - (bitsPerValue);
			long rawValue;
			switch (roundedBits >>> 3) {
				case 1 :
					rawValue = in.readByte();
					break;
				case 2 :
					rawValue = in.readShort();
					break;
				case 3 :
					rawValue = (((long) (in.readShort())) << 8) | ((in.readByte()) & 255L);
					break;
				case 4 :
					rawValue = in.readInt();
					break;
				case 5 :
					rawValue = (((long) (in.readInt())) << 8) | ((in.readByte()) & 255L);
					break;
				case 6 :
					rawValue = (((long) (in.readInt())) << 16) | ((in.readShort()) & 65535L);
					break;
				case 7 :
					rawValue = ((((long) (in.readInt())) << 24) | (((in.readShort()) & 65535L) << 8)) | ((in.readByte()) & 255L);
					break;
				case 8 :
					rawValue = in.readLong();
					break;
				case 9 :
					rawValue = ((in.readLong()) << (8 - shiftRightBits)) | (((in.readByte()) & 255L) >>> shiftRightBits);
					shiftRightBits = 0;
					break;
				default :
					throw new AssertionError(("bitsPerValue too large: " + (bitsPerValue)));
			}
			return (rawValue >>> shiftRightBits) & (valueMask);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public long ramBytesUsed() {
		return 0;
	}
}

