

import java.util.Arrays;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.StringHelper;


public final class BytesRefHash {
	public static final int DEFAULT_CAPACITY = 16;

	final ByteBlockPool pool;

	int[] bytesStart;

	private final BytesRef scratch1 = new BytesRef();

	private int hashSize;

	private int hashHalfSize;

	private int hashMask;

	private int count;

	private int lastCount = -1;

	private int[] ids;

	private final BytesRefHash.BytesStartArray bytesStartArray;

	private Counter bytesUsed;

	public BytesRefHash() {
		this(new ByteBlockPool(new ByteBlockPool.DirectAllocator()));
	}

	public BytesRefHash(ByteBlockPool pool) {
		this(pool, BytesRefHash.DEFAULT_CAPACITY, new BytesRefHash.DirectBytesStartArray(BytesRefHash.DEFAULT_CAPACITY));
	}

	public BytesRefHash(ByteBlockPool pool, int capacity, BytesRefHash.BytesStartArray bytesStartArray) {
		hashSize = capacity;
		hashHalfSize = (hashSize) >> 1;
		hashMask = (hashSize) - 1;
		this.pool = pool;
		ids = new int[hashSize];
		Arrays.fill(ids, (-1));
		this.bytesStartArray = bytesStartArray;
		bytesStart = bytesStartArray.init();
		bytesUsed = ((bytesStartArray.bytesUsed()) == null) ? Counter.newCounter() : bytesStartArray.bytesUsed();
		bytesUsed.addAndGet(((hashSize) * (Integer.BYTES)));
	}

	public int size() {
		return count;
	}

	public BytesRef get(int bytesID, BytesRef ref) {
		assert (bytesStart) != null : "bytesStart is null - not initialized";
		assert bytesID < (bytesStart.length) : "bytesID exceeds byteStart len: " + (bytesStart.length);
		pool.setBytesRef(ref, bytesStart[bytesID]);
		return ref;
	}

	int[] compact() {
		assert (bytesStart) != null : "bytesStart is null - not initialized";
		int upto = 0;
		for (int i = 0; i < (hashSize); i++) {
			if ((ids[i]) != (-1)) {
				if (upto < i) {
					ids[upto] = ids[i];
					ids[i] = -1;
				}
				upto++;
			}
		}
		assert upto == (count);
		lastCount = count;
		return ids;
	}

	public int[] sort() {
		final int[] compact = compact();
		return compact;
	}

	private boolean equals(int id, BytesRef b) {
		pool.setBytesRef(scratch1, bytesStart[id]);
		return scratch1.bytesEquals(b);
	}

	private boolean shrink(int targetSize) {
		int newSize = hashSize;
		while ((newSize >= 8) && ((newSize / 4) > targetSize)) {
			newSize /= 2;
		} 
		if (newSize != (hashSize)) {
			bytesUsed.addAndGet(((Integer.BYTES) * (-((hashSize) - newSize))));
			hashSize = newSize;
			ids = new int[hashSize];
			Arrays.fill(ids, (-1));
			hashHalfSize = newSize / 2;
			hashMask = newSize - 1;
			return true;
		}else {
			return false;
		}
	}

	public void clear(boolean resetPool) {
		lastCount = count;
		count = 0;
		if (resetPool) {
			pool.reset(false, false);
		}
		bytesStart = bytesStartArray.clear();
		if (((lastCount) != (-1)) && (shrink(lastCount))) {
			return;
		}
		Arrays.fill(ids, (-1));
	}

	public void clear() {
		clear(true);
	}

	public void close() {
		clear(true);
		ids = null;
		bytesUsed.addAndGet(((Integer.BYTES) * (-(hashSize))));
	}

	public int add(BytesRef bytes) {
		assert (bytesStart) != null : "Bytesstart is null - not initialized";
		final int length = bytes.length;
		final int hashPos = findHash(bytes);
		int e = ids[hashPos];
		if (e == (-1)) {
			final int len2 = 2 + (bytes.length);
			if ((len2 + (pool.byteUpto)) > (ByteBlockPool.BYTE_BLOCK_SIZE)) {
				if (len2 > (ByteBlockPool.BYTE_BLOCK_SIZE)) {
					throw new BytesRefHash.MaxBytesLengthExceededException(((("bytes can be at most " + ((ByteBlockPool.BYTE_BLOCK_SIZE) - 2)) + " in length; got ") + (bytes.length)));
				}
				pool.nextBuffer();
			}
			final byte[] buffer = pool.buffer;
			final int bufferUpto = pool.byteUpto;
			if ((count) >= (bytesStart.length)) {
				bytesStart = bytesStartArray.grow();
				assert (count) < ((bytesStart.length) + 1) : (("count: " + (count)) + " len: ") + (bytesStart.length);
			}
			e = (count)++;
			bytesStart[e] = bufferUpto + (pool.byteOffset);
			if (length < 128) {
				buffer[bufferUpto] = ((byte) (length));
				pool.byteUpto += length + 1;
				assert length >= 0 : "Length must be positive: " + length;
				System.arraycopy(bytes.bytes, bytes.offset, buffer, (bufferUpto + 1), length);
			}else {
				buffer[bufferUpto] = ((byte) (128 | (length & 127)));
				buffer[(bufferUpto + 1)] = ((byte) ((length >> 7) & 255));
				pool.byteUpto += length + 2;
				System.arraycopy(bytes.bytes, bytes.offset, buffer, (bufferUpto + 2), length);
			}
			assert (ids[hashPos]) == (-1);
			ids[hashPos] = e;
			if ((count) == (hashHalfSize)) {
				rehash((2 * (hashSize)), true);
			}
			return e;
		}
		return -(e + 1);
	}

	public int find(BytesRef bytes) {
		return ids[findHash(bytes)];
	}

	private int findHash(BytesRef bytes) {
		assert (bytesStart) != null : "bytesStart is null - not initialized";
		int code = doHash(bytes.bytes, bytes.offset, bytes.length);
		int hashPos = code & (hashMask);
		int e = ids[hashPos];
		if ((e != (-1)) && (!(equals(e, bytes)))) {
			do {
				code++;
				hashPos = code & (hashMask);
				e = ids[hashPos];
			} while ((e != (-1)) && (!(equals(e, bytes))) );
		}
		return hashPos;
	}

	public int addByPoolOffset(int offset) {
		assert (bytesStart) != null : "Bytesstart is null - not initialized";
		int code = offset;
		int hashPos = offset & (hashMask);
		int e = ids[hashPos];
		if ((e != (-1)) && ((bytesStart[e]) != offset)) {
			do {
				code++;
				hashPos = code & (hashMask);
				e = ids[hashPos];
			} while ((e != (-1)) && ((bytesStart[e]) != offset) );
		}
		if (e == (-1)) {
			if ((count) >= (bytesStart.length)) {
				bytesStart = bytesStartArray.grow();
				assert (count) < ((bytesStart.length) + 1) : (("count: " + (count)) + " len: ") + (bytesStart.length);
			}
			e = (count)++;
			bytesStart[e] = offset;
			assert (ids[hashPos]) == (-1);
			ids[hashPos] = e;
			if ((count) == (hashHalfSize)) {
				rehash((2 * (hashSize)), false);
			}
			return e;
		}
		return -(e + 1);
	}

	private void rehash(final int newSize, boolean hashOnData) {
		final int newMask = newSize - 1;
		bytesUsed.addAndGet(((Integer.BYTES) * newSize));
		final int[] newHash = new int[newSize];
		Arrays.fill(newHash, (-1));
		for (int i = 0; i < (hashSize); i++) {
			final int e0 = ids[i];
			if (e0 != (-1)) {
				int code;
				if (hashOnData) {
					final int off = bytesStart[e0];
					final int start = off & (ByteBlockPool.BYTE_BLOCK_MASK);
					final byte[] bytes = pool.buffers[(off >> (ByteBlockPool.BYTE_BLOCK_SHIFT))];
					final int len;
					int pos;
					if (((bytes[start]) & 128) == 0) {
						len = bytes[start];
						pos = start + 1;
					}else {
						len = ((bytes[start]) & 127) + (((bytes[(start + 1)]) & 255) << 7);
						pos = start + 2;
					}
					code = doHash(bytes, pos, len);
				}else {
					code = bytesStart[e0];
				}
				int hashPos = code & newMask;
				assert hashPos >= 0;
				if ((newHash[hashPos]) != (-1)) {
					do {
						code++;
						hashPos = code & newMask;
					} while ((newHash[hashPos]) != (-1) );
				}
				newHash[hashPos] = e0;
			}
		}
		hashMask = newMask;
		bytesUsed.addAndGet(((Integer.BYTES) * (-(ids.length))));
		ids = newHash;
		hashSize = newSize;
		hashHalfSize = newSize / 2;
	}

	private int doHash(byte[] bytes, int offset, int length) {
		return StringHelper.murmurhash3_x86_32(bytes, offset, length, StringHelper.GOOD_FAST_HASH_SEED);
	}

	public void reinit() {
		if ((bytesStart) == null) {
			bytesStart = bytesStartArray.init();
		}
		if ((ids) == null) {
			ids = new int[hashSize];
			bytesUsed.addAndGet(((Integer.BYTES) * (hashSize)));
		}
	}

	public int byteStart(int bytesID) {
		assert (bytesStart) != null : "bytesStart is null - not initialized";
		assert (bytesID >= 0) && (bytesID < (count)) : bytesID;
		return bytesStart[bytesID];
	}

	@SuppressWarnings("serial")
	public static class MaxBytesLengthExceededException extends RuntimeException {
		MaxBytesLengthExceededException(String message) {
			super(message);
		}
	}

	public static abstract class BytesStartArray {
		public abstract int[] init();

		public abstract int[] grow();

		public abstract int[] clear();

		public abstract Counter bytesUsed();
	}

	public static class DirectBytesStartArray extends BytesRefHash.BytesStartArray {
		protected final int initSize;

		private int[] bytesStart;

		private final Counter bytesUsed;

		public DirectBytesStartArray(int initSize, Counter counter) {
			this.bytesUsed = counter;
			this.initSize = initSize;
		}

		public DirectBytesStartArray(int initSize) {
			this(initSize, Counter.newCounter());
		}

		@Override
		public int[] clear() {
			return bytesStart = null;
		}

		@Override
		public int[] grow() {
			assert (bytesStart) != null;
			return bytesStart = ArrayUtil.grow(bytesStart, ((bytesStart.length) + 1));
		}

		@Override
		public int[] init() {
			return bytesStart = new int[ArrayUtil.oversize(initSize, Integer.BYTES)];
		}

		@Override
		public Counter bytesUsed() {
			return bytesUsed;
		}
	}
}

