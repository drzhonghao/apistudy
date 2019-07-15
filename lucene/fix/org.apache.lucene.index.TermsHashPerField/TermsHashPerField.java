

import java.io.IOException;
import org.apache.lucene.analysis.tokenattributes.TermFrequencyAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefHash;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.IntBlockPool;


abstract class TermsHashPerField implements Comparable<TermsHashPerField> {
	private static final int HASH_INIT_SIZE = 4;

	final TermsHashPerField nextPerField = null;

	protected final FieldInvertState fieldState = null;

	TermToBytesRefAttribute termAtt;

	protected TermFrequencyAttribute termFreqAtt;

	final IntBlockPool intPool = null;

	final ByteBlockPool bytePool = null;

	final ByteBlockPool termBytePool = null;

	final int streamCount = 0;

	final int numPostingInt = 0;

	protected final FieldInfo fieldInfo = null;

	final BytesRefHash bytesHash = null;

	private final Counter bytesUsed = null;

	void reset() {
		bytesHash.clear(false);
		if ((nextPerField) != null) {
			nextPerField.reset();
		}
	}

	int[] sortedTermIDs;

	public int[] sortPostings() {
		sortedTermIDs = bytesHash.sort();
		return sortedTermIDs;
	}

	private boolean doNextCall;

	public void add(int textStart) throws IOException {
		int termID = bytesHash.addByPoolOffset(textStart);
		if (termID >= 0) {
			if (((numPostingInt) + (intPool.intUpto)) > (IntBlockPool.INT_BLOCK_SIZE)) {
				intPool.nextBuffer();
			}
			if (((ByteBlockPool.BYTE_BLOCK_SIZE) - (bytePool.byteUpto)) < ((numPostingInt) * (ByteBlockPool.FIRST_LEVEL_SIZE))) {
				bytePool.nextBuffer();
			}
			intUptos = intPool.buffer;
			intUptoStart = intPool.intUpto;
			intPool.intUpto += streamCount;
			for (int i = 0; i < (streamCount); i++) {
				final int upto = bytePool.newSlice(ByteBlockPool.FIRST_LEVEL_SIZE);
				intUptos[((intUptoStart) + i)] = upto + (bytePool.byteOffset);
			}
			newTerm(termID);
		}else {
			termID = (-termID) - 1;
			addTerm(termID);
		}
	}

	void add() throws IOException {
		int termID = bytesHash.add(termAtt.getBytesRef());
		if (termID >= 0) {
			bytesHash.byteStart(termID);
			if (((numPostingInt) + (intPool.intUpto)) > (IntBlockPool.INT_BLOCK_SIZE)) {
				intPool.nextBuffer();
			}
			if (((ByteBlockPool.BYTE_BLOCK_SIZE) - (bytePool.byteUpto)) < ((numPostingInt) * (ByteBlockPool.FIRST_LEVEL_SIZE))) {
				bytePool.nextBuffer();
			}
			intUptos = intPool.buffer;
			intUptoStart = intPool.intUpto;
			intPool.intUpto += streamCount;
			for (int i = 0; i < (streamCount); i++) {
				final int upto = bytePool.newSlice(ByteBlockPool.FIRST_LEVEL_SIZE);
				intUptos[((intUptoStart) + i)] = upto + (bytePool.byteOffset);
			}
			newTerm(termID);
		}else {
			termID = (-termID) - 1;
			addTerm(termID);
		}
		if (doNextCall) {
		}
	}

	int[] intUptos;

	int intUptoStart;

	void writeByte(int stream, byte b) {
		int upto = intUptos[((intUptoStart) + stream)];
		byte[] bytes = bytePool.buffers[(upto >> (ByteBlockPool.BYTE_BLOCK_SHIFT))];
		assert bytes != null;
		int offset = upto & (ByteBlockPool.BYTE_BLOCK_MASK);
		if ((bytes[offset]) != 0) {
			offset = bytePool.allocSlice(bytes, offset);
			bytes = bytePool.buffer;
			intUptos[((intUptoStart) + stream)] = offset + (bytePool.byteOffset);
		}
		bytes[offset] = b;
		(intUptos[((intUptoStart) + stream)])++;
	}

	public void writeBytes(int stream, byte[] b, int offset, int len) {
		final int end = offset + len;
		for (int i = offset; i < end; i++)
			writeByte(stream, b[i]);

	}

	void writeVInt(int stream, int i) {
		assert stream < (streamCount);
		while ((i & (~127)) != 0) {
			writeByte(stream, ((byte) ((i & 127) | 128)));
			i >>>= 7;
		} 
		writeByte(stream, ((byte) (i)));
	}

	private static final class PostingsBytesStartArray extends BytesRefHash.BytesStartArray {
		private final TermsHashPerField perField;

		private final Counter bytesUsed;

		private PostingsBytesStartArray(TermsHashPerField perField, Counter bytesUsed) {
			this.perField = perField;
			this.bytesUsed = bytesUsed;
		}

		@Override
		public int[] init() {
			return null;
		}

		@Override
		public int[] grow() {
			perField.newPostingsArray();
			return null;
		}

		@Override
		public int[] clear() {
			return null;
		}

		@Override
		public Counter bytesUsed() {
			return bytesUsed;
		}
	}

	@Override
	public int compareTo(TermsHashPerField other) {
		return fieldInfo.name.compareTo(other.fieldInfo.name);
	}

	void finish() throws IOException {
		if ((nextPerField) != null) {
			nextPerField.finish();
		}
	}

	boolean start(IndexableField field, boolean first) {
		if ((nextPerField) != null) {
			doNextCall = nextPerField.start(field, first);
		}
		return true;
	}

	abstract void newTerm(int termID) throws IOException;

	abstract void addTerm(int termID) throws IOException;

	abstract void newPostingsArray();
}

