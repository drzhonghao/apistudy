

import java.io.EOFException;
import java.io.IOException;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.LongBitSet;
import org.apache.lucene.util.bkd.OfflinePointWriter;
import org.apache.lucene.util.bkd.PointReader;
import org.apache.lucene.util.bkd.PointWriter;


public final class OfflinePointReader extends PointReader {
	long countLeft;

	final IndexInput in;

	private final byte[] packedValue;

	final boolean singleValuePerDoc;

	final int bytesPerDoc;

	private long ord;

	private int docID;

	private boolean longOrds;

	private boolean checked;

	final String name;

	public OfflinePointReader(Directory tempDir, String tempFileName, int packedBytesLength, long start, long length, boolean longOrds, boolean singleValuePerDoc) throws IOException {
		this.singleValuePerDoc = singleValuePerDoc;
		int bytesPerDoc = packedBytesLength + (Integer.BYTES);
		if (singleValuePerDoc == false) {
			if (longOrds) {
				bytesPerDoc += Long.BYTES;
			}else {
				bytesPerDoc += Integer.BYTES;
			}
		}
		this.bytesPerDoc = bytesPerDoc;
		if ((((start + length) * bytesPerDoc) + (CodecUtil.footerLength())) > (tempDir.fileLength(tempFileName))) {
			throw new IllegalArgumentException(((((((((("requested slice is beyond the length of this file: start=" + start) + " length=") + length) + " bytesPerDoc=") + bytesPerDoc) + " fileLength=") + (tempDir.fileLength(tempFileName))) + " tempFileName=") + tempFileName));
		}
		if ((start == 0) && ((length * bytesPerDoc) == ((tempDir.fileLength(tempFileName)) - (CodecUtil.footerLength())))) {
			in = tempDir.openChecksumInput(tempFileName, IOContext.READONCE);
		}else {
			in = tempDir.openInput(tempFileName, IOContext.READONCE);
		}
		name = tempFileName;
		long seekFP = start * bytesPerDoc;
		in.seek(seekFP);
		countLeft = length;
		packedValue = new byte[packedBytesLength];
		this.longOrds = longOrds;
	}

	@Override
	public boolean next() throws IOException {
		if ((countLeft) >= 0) {
			if ((countLeft) == 0) {
				return false;
			}
			(countLeft)--;
		}
		try {
			in.readBytes(packedValue, 0, packedValue.length);
		} catch (EOFException eofe) {
			assert (countLeft) == (-1);
			return false;
		}
		docID = in.readInt();
		if ((singleValuePerDoc) == false) {
			if (longOrds) {
				ord = in.readLong();
			}else {
				ord = in.readInt();
			}
		}else {
			ord = docID;
		}
		return true;
	}

	@Override
	public byte[] packedValue() {
		return packedValue;
	}

	@Override
	public long ord() {
		return ord;
	}

	@Override
	public int docID() {
		return docID;
	}

	@Override
	public void close() throws IOException {
		try {
			if ((((countLeft) == 0) && ((in) instanceof ChecksumIndexInput)) && ((checked) == false)) {
				checked = true;
				CodecUtil.checkFooter(((ChecksumIndexInput) (in)));
			}
		} finally {
			in.close();
		}
	}

	@Override
	public void markOrds(long count, LongBitSet ordBitSet) throws IOException {
		if ((countLeft) < count) {
			throw new IllegalStateException((((("only " + (countLeft)) + " points remain, but ") + count) + " were requested"));
		}
		long fp = (in.getFilePointer()) + (packedValue.length);
		if ((singleValuePerDoc) == false) {
			fp += Integer.BYTES;
		}
		for (long i = 0; i < count; i++) {
			in.seek(fp);
			long ord;
			if (longOrds) {
				ord = in.readLong();
			}else {
				ord = in.readInt();
			}
			assert (ordBitSet.get(ord)) == false : (((("ord=" + ord) + " i=") + i) + " was seen twice from ") + (this);
			ordBitSet.set(ord);
			fp += bytesPerDoc;
		}
	}

	@Override
	public long split(long count, LongBitSet rightTree, PointWriter left, PointWriter right, boolean doClearBits) throws IOException {
		if (((left instanceof OfflinePointWriter) == false) || ((right instanceof OfflinePointWriter) == false)) {
			return super.split(count, rightTree, left, right, doClearBits);
		}
		int packedBytesLength = packedValue.length;
		int bytesPerDoc = packedBytesLength + (Integer.BYTES);
		if ((singleValuePerDoc) == false) {
			if (longOrds) {
				bytesPerDoc += Long.BYTES;
			}else {
				bytesPerDoc += Integer.BYTES;
			}
		}
		long rightCount = 0;
		IndexOutput rightOut = ((OfflinePointWriter) (right)).out;
		IndexOutput leftOut = ((OfflinePointWriter) (left)).out;
		assert count <= (countLeft) : (("count=" + count) + " countLeft=") + (countLeft);
		countLeft -= count;
		long countStart = count;
		byte[] buffer = new byte[bytesPerDoc];
		while (count > 0) {
			in.readBytes(buffer, 0, buffer.length);
			long ord;
			if (longOrds) {
				ord = OfflinePointReader.readLong(buffer, (packedBytesLength + (Integer.BYTES)));
			}else
				if (singleValuePerDoc) {
					ord = OfflinePointReader.readInt(buffer, packedBytesLength);
				}else {
					ord = OfflinePointReader.readInt(buffer, (packedBytesLength + (Integer.BYTES)));
				}

			if (rightTree.get(ord)) {
				rightOut.writeBytes(buffer, 0, bytesPerDoc);
				if (doClearBits) {
					rightTree.clear(ord);
				}
				rightCount++;
			}else {
				leftOut.writeBytes(buffer, 0, bytesPerDoc);
			}
			count--;
		} 
		return rightCount;
	}

	private static long readLong(byte[] bytes, int pos) {
		final int i1 = (((((bytes[(pos++)]) & 255) << 24) | (((bytes[(pos++)]) & 255) << 16)) | (((bytes[(pos++)]) & 255) << 8)) | ((bytes[(pos++)]) & 255);
		final int i2 = (((((bytes[(pos++)]) & 255) << 24) | (((bytes[(pos++)]) & 255) << 16)) | (((bytes[(pos++)]) & 255) << 8)) | ((bytes[(pos++)]) & 255);
		return (((long) (i1)) << 32) | (i2 & 4294967295L);
	}

	private static int readInt(byte[] bytes, int pos) {
		return (((((bytes[(pos++)]) & 255) << 24) | (((bytes[(pos++)]) & 255) << 16)) | (((bytes[(pos++)]) & 255) << 8)) | ((bytes[(pos++)]) & 255);
	}
}

