

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.bkd.OfflinePointReader;
import org.apache.lucene.util.bkd.PointReader;
import org.apache.lucene.util.bkd.PointWriter;


public final class OfflinePointWriter implements PointWriter {
	final Directory tempDir;

	public final IndexOutput out;

	public final String name;

	final int packedBytesLength;

	final boolean singleValuePerDoc;

	long count;

	private boolean closed;

	private boolean longOrds;

	private OfflinePointReader sharedReader;

	private long nextSharedRead;

	final long expectedCount;

	public OfflinePointWriter(Directory tempDir, String tempFileNamePrefix, int packedBytesLength, boolean longOrds, String desc, long expectedCount, boolean singleValuePerDoc) throws IOException {
		this.out = tempDir.createTempOutput(tempFileNamePrefix, ("bkd_" + desc), IOContext.DEFAULT);
		this.name = out.getName();
		this.tempDir = tempDir;
		this.packedBytesLength = packedBytesLength;
		this.longOrds = longOrds;
		this.singleValuePerDoc = singleValuePerDoc;
		this.expectedCount = expectedCount;
	}

	public OfflinePointWriter(Directory tempDir, String name, int packedBytesLength, long count, boolean longOrds, boolean singleValuePerDoc) {
		this.out = null;
		this.name = name;
		this.tempDir = tempDir;
		this.packedBytesLength = packedBytesLength;
		this.count = count;
		closed = true;
		this.longOrds = longOrds;
		this.singleValuePerDoc = singleValuePerDoc;
		this.expectedCount = 0;
	}

	@Override
	public void append(byte[] packedValue, long ord, int docID) throws IOException {
		assert (packedValue.length) == (packedBytesLength);
		out.writeBytes(packedValue, 0, packedValue.length);
		out.writeInt(docID);
		if ((singleValuePerDoc) == false) {
			if (longOrds) {
				out.writeLong(ord);
			}else {
				assert ord <= (Integer.MAX_VALUE);
				out.writeInt(((int) (ord)));
			}
		}
		(count)++;
		assert ((expectedCount) == 0) || ((count) <= (expectedCount));
	}

	@Override
	public PointReader getReader(long start, long length) throws IOException {
		assert closed;
		assert (start + length) <= (count) : (((("start=" + start) + " length=") + length) + " count=") + (count);
		assert ((expectedCount) == 0) || ((count) == (expectedCount));
		return new OfflinePointReader(tempDir, name, packedBytesLength, start, length, longOrds, singleValuePerDoc);
	}

	@Override
	public PointReader getSharedReader(long start, long length, List<Closeable> toCloseHeroically) throws IOException {
		if ((sharedReader) == null) {
			assert start == 0;
			assert length <= (count);
			sharedReader = new OfflinePointReader(tempDir, name, packedBytesLength, 0, count, longOrds, singleValuePerDoc);
			toCloseHeroically.add(sharedReader);
		}else {
			assert start == (nextSharedRead) : (((("start=" + start) + " length=") + length) + " nextSharedRead=") + (nextSharedRead);
		}
		nextSharedRead += length;
		return sharedReader;
	}

	@Override
	public void close() throws IOException {
		if ((closed) == false) {
			assert (sharedReader) == null;
			try {
				CodecUtil.writeFooter(out);
			} finally {
				out.close();
				closed = true;
			}
		}
	}

	@Override
	public void destroy() throws IOException {
		if ((sharedReader) != null) {
			assert (nextSharedRead) == (count);
			sharedReader.close();
			sharedReader = null;
		}
		tempDir.deleteFile(name);
	}

	@Override
	public String toString() {
		return ((("OfflinePointWriter(count=" + (count)) + " tempFileName=") + (name)) + ")";
	}
}

