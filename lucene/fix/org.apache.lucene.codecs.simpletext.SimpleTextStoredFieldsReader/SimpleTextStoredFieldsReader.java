

import java.io.IOException;
import org.apache.lucene.codecs.StoredFieldsReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.BufferedChecksumIndexInput;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.FutureArrays;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.RamUsageEstimator;


public class SimpleTextStoredFieldsReader extends StoredFieldsReader {
	private static final long BASE_RAM_BYTES_USED = ((RamUsageEstimator.shallowSizeOfInstance(SimpleTextStoredFieldsReader.class)) + (RamUsageEstimator.shallowSizeOfInstance(BytesRef.class))) + (RamUsageEstimator.shallowSizeOfInstance(CharsRef.class));

	private long[] offsets;

	private IndexInput in;

	private BytesRefBuilder scratch = new BytesRefBuilder();

	private CharsRefBuilder scratchUTF16 = new CharsRefBuilder();

	private final FieldInfos fieldInfos;

	public SimpleTextStoredFieldsReader(Directory directory, SegmentInfo si, FieldInfos fn, IOContext context) throws IOException {
		this.fieldInfos = fn;
		boolean success = false;
		try {
			success = true;
		} finally {
			if (!success) {
				try {
					close();
				} catch (Throwable t) {
				}
			}
		}
		readIndex(si.maxDoc());
	}

	SimpleTextStoredFieldsReader(long[] offsets, IndexInput in, FieldInfos fieldInfos) {
		this.offsets = offsets;
		this.in = in;
		this.fieldInfos = fieldInfos;
	}

	private void readIndex(int size) throws IOException {
		ChecksumIndexInput input = new BufferedChecksumIndexInput(in);
		offsets = new long[size];
		int upto = 0;
		assert upto == (offsets.length);
	}

	@Override
	public void visitDocument(int n, StoredFieldVisitor visitor) throws IOException {
		in.seek(offsets[n]);
		while (true) {
			readLine();
			readLine();
			readLine();
			final BytesRef type;
		} 
	}

	private void readField(BytesRef type, FieldInfo fieldInfo, StoredFieldVisitor visitor) throws IOException {
		readLine();
	}

	@Override
	public StoredFieldsReader clone() {
		if ((in) == null) {
			throw new AlreadyClosedException("this FieldsReader is closed");
		}
		return new SimpleTextStoredFieldsReader(offsets, in.clone(), fieldInfos);
	}

	@Override
	public void close() throws IOException {
		try {
			IOUtils.close(in);
		} finally {
			in = null;
			offsets = null;
		}
	}

	private void readLine() throws IOException {
	}

	private int parseIntAt(int offset) {
		scratchUTF16.copyUTF8Bytes(scratch.bytes(), offset, ((scratch.length()) - offset));
		return ArrayUtil.parseInt(scratchUTF16.chars(), 0, scratchUTF16.length());
	}

	private boolean equalsAt(BytesRef a, BytesRef b, int bOffset) {
		return ((a.length) == ((b.length) - bOffset)) && (FutureArrays.equals(a.bytes, a.offset, ((a.offset) + (a.length)), b.bytes, ((b.offset) + bOffset), ((b.offset) + (b.length))));
	}

	@Override
	public long ramBytesUsed() {
		return (((SimpleTextStoredFieldsReader.BASE_RAM_BYTES_USED) + (RamUsageEstimator.sizeOf(offsets))) + (RamUsageEstimator.sizeOf(scratch.bytes()))) + (RamUsageEstimator.sizeOf(scratchUTF16.chars()));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void checkIntegrity() throws IOException {
	}
}

