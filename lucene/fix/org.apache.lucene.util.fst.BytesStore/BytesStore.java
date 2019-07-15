

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.fst.FST;


class BytesStore extends DataOutput implements Accountable {
	private static final long BASE_RAM_BYTES_USED = (RamUsageEstimator.shallowSizeOfInstance(BytesStore.class)) + (RamUsageEstimator.shallowSizeOfInstance(ArrayList.class));

	private final List<byte[]> blocks = new ArrayList<>();

	private final int blockSize;

	private final int blockBits;

	private final int blockMask;

	private byte[] current;

	private int nextWrite;

	public BytesStore(int blockBits) {
		this.blockBits = blockBits;
		blockSize = 1 << blockBits;
		blockMask = (blockSize) - 1;
		nextWrite = blockSize;
	}

	public BytesStore(DataInput in, long numBytes, int maxBlockSize) throws IOException {
		int blockSize = 2;
		int blockBits = 1;
		while ((blockSize < numBytes) && (blockSize < maxBlockSize)) {
			blockSize *= 2;
			blockBits++;
		} 
		this.blockBits = blockBits;
		this.blockSize = blockSize;
		this.blockMask = blockSize - 1;
		long left = numBytes;
		while (left > 0) {
			final int chunk = ((int) (Math.min(blockSize, left)));
			byte[] block = new byte[chunk];
			in.readBytes(block, 0, block.length);
			blocks.add(block);
			left -= chunk;
		} 
		nextWrite = blocks.get(((blocks.size()) - 1)).length;
	}

	public void writeByte(int dest, byte b) {
		int blockIndex = dest >> (blockBits);
		byte[] block = blocks.get(blockIndex);
		block[(dest & (blockMask))] = b;
	}

	@Override
	public void writeByte(byte b) {
		if ((nextWrite) == (blockSize)) {
			current = new byte[blockSize];
			blocks.add(current);
			nextWrite = 0;
		}
		current[((nextWrite)++)] = b;
	}

	@Override
	public void writeBytes(byte[] b, int offset, int len) {
		while (len > 0) {
			int chunk = (blockSize) - (nextWrite);
			if (len <= chunk) {
				assert b != null;
				assert (current) != null;
				System.arraycopy(b, offset, current, nextWrite, len);
				nextWrite += len;
				break;
			}else {
				if (chunk > 0) {
					System.arraycopy(b, offset, current, nextWrite, chunk);
					offset += chunk;
					len -= chunk;
				}
				current = new byte[blockSize];
				blocks.add(current);
				nextWrite = 0;
			}
		} 
	}

	int getBlockBits() {
		return blockBits;
	}

	void writeBytes(long dest, byte[] b, int offset, int len) {
		assert (dest + len) <= (getPosition()) : (((("dest=" + dest) + " pos=") + (getPosition())) + " len=") + len;
		final long end = dest + len;
		int blockIndex = ((int) (end >> (blockBits)));
		int downTo = ((int) (end & (blockMask)));
		if (downTo == 0) {
			blockIndex--;
			downTo = blockSize;
		}
		byte[] block = blocks.get(blockIndex);
		while (len > 0) {
			if (len <= downTo) {
				System.arraycopy(b, offset, block, (downTo - len), len);
				break;
			}else {
				len -= downTo;
				System.arraycopy(b, (offset + len), block, 0, downTo);
				blockIndex--;
				block = blocks.get(blockIndex);
				downTo = blockSize;
			}
		} 
	}

	public void copyBytes(long src, long dest, int len) {
		assert src < dest;
		long end = src + len;
		int blockIndex = ((int) (end >> (blockBits)));
		int downTo = ((int) (end & (blockMask)));
		if (downTo == 0) {
			blockIndex--;
			downTo = blockSize;
		}
		byte[] block = blocks.get(blockIndex);
		while (len > 0) {
			if (len <= downTo) {
				writeBytes(dest, block, (downTo - len), len);
				break;
			}else {
				len -= downTo;
				writeBytes((dest + len), block, 0, downTo);
				blockIndex--;
				block = blocks.get(blockIndex);
				downTo = blockSize;
			}
		} 
	}

	public void writeInt(long pos, int value) {
		int blockIndex = ((int) (pos >> (blockBits)));
		int upto = ((int) (pos & (blockMask)));
		byte[] block = blocks.get(blockIndex);
		int shift = 24;
		for (int i = 0; i < 4; i++) {
			block[(upto++)] = ((byte) (value >> shift));
			shift -= 8;
			if (upto == (blockSize)) {
				upto = 0;
				blockIndex++;
				block = blocks.get(blockIndex);
			}
		}
	}

	public void reverse(long srcPos, long destPos) {
		assert srcPos < destPos;
		assert destPos < (getPosition());
		int srcBlockIndex = ((int) (srcPos >> (blockBits)));
		int src = ((int) (srcPos & (blockMask)));
		byte[] srcBlock = blocks.get(srcBlockIndex);
		int destBlockIndex = ((int) (destPos >> (blockBits)));
		int dest = ((int) (destPos & (blockMask)));
		byte[] destBlock = blocks.get(destBlockIndex);
		int limit = ((int) ((destPos - srcPos) + 1)) / 2;
		for (int i = 0; i < limit; i++) {
			byte b = srcBlock[src];
			srcBlock[src] = destBlock[dest];
			destBlock[dest] = b;
			src++;
			if (src == (blockSize)) {
				srcBlockIndex++;
				srcBlock = blocks.get(srcBlockIndex);
				src = 0;
			}
			dest--;
			if (dest == (-1)) {
				destBlockIndex--;
				destBlock = blocks.get(destBlockIndex);
				dest = (blockSize) - 1;
			}
		}
	}

	public void skipBytes(int len) {
		while (len > 0) {
			int chunk = (blockSize) - (nextWrite);
			if (len <= chunk) {
				nextWrite += len;
				break;
			}else {
				len -= chunk;
				current = new byte[blockSize];
				blocks.add(current);
				nextWrite = 0;
			}
		} 
	}

	public long getPosition() {
		return ((((long) (blocks.size())) - 1) * (blockSize)) + (nextWrite);
	}

	public void truncate(long newLen) {
		assert newLen <= (getPosition());
		assert newLen >= 0;
		int blockIndex = ((int) (newLen >> (blockBits)));
		nextWrite = ((int) (newLen & (blockMask)));
		if ((nextWrite) == 0) {
			blockIndex--;
			nextWrite = blockSize;
		}
		blocks.subList((blockIndex + 1), blocks.size()).clear();
		if (newLen == 0) {
			current = null;
		}else {
			current = blocks.get(blockIndex);
		}
		assert newLen == (getPosition());
	}

	public void finish() {
		if ((current) != null) {
			byte[] lastBuffer = new byte[nextWrite];
			System.arraycopy(current, 0, lastBuffer, 0, nextWrite);
			blocks.set(((blocks.size()) - 1), lastBuffer);
			current = null;
		}
	}

	public void writeTo(DataOutput out) throws IOException {
		for (byte[] block : blocks) {
			out.writeBytes(block, 0, block.length);
		}
	}

	public FST.BytesReader getForwardReader() {
		if ((blocks.size()) == 1) {
		}
		return new FST.BytesReader() {
			private byte[] current;

			private int nextBuffer;

			private int nextRead = blockSize;

			@Override
			public byte readByte() {
				if ((nextRead) == (blockSize)) {
					current = blocks.get(((nextBuffer)++));
					nextRead = 0;
				}
				return current[((nextRead)++)];
			}

			@Override
			public void skipBytes(long count) {
				setPosition(((getPosition()) + count));
			}

			@Override
			public void readBytes(byte[] b, int offset, int len) {
				while (len > 0) {
					int chunkLeft = (blockSize) - (nextRead);
					if (len <= chunkLeft) {
						System.arraycopy(current, nextRead, b, offset, len);
						nextRead += len;
						break;
					}else {
						if (chunkLeft > 0) {
							System.arraycopy(current, nextRead, b, offset, chunkLeft);
							offset += chunkLeft;
							len -= chunkLeft;
						}
						current = blocks.get(((nextBuffer)++));
						nextRead = 0;
					}
				} 
			}

			@Override
			public long getPosition() {
				return ((((long) (nextBuffer)) - 1) * (blockSize)) + (nextRead);
			}

			@Override
			public void setPosition(long pos) {
				int bufferIndex = ((int) (pos >> (blockBits)));
				nextBuffer = bufferIndex + 1;
				current = blocks.get(bufferIndex);
				nextRead = ((int) (pos & (blockMask)));
				assert (getPosition()) == pos;
			}

			@Override
			public boolean reversed() {
				return false;
			}
		};
	}

	public FST.BytesReader getReverseReader() {
		return getReverseReader(true);
	}

	FST.BytesReader getReverseReader(boolean allowSingle) {
		if (allowSingle && ((blocks.size()) == 1)) {
		}
		return new FST.BytesReader() {
			private byte[] current = ((blocks.size()) == 0) ? null : blocks.get(0);

			private int nextBuffer = -1;

			private int nextRead = 0;

			@Override
			public byte readByte() {
				if ((nextRead) == (-1)) {
					current = blocks.get(((nextBuffer)--));
					nextRead = (blockSize) - 1;
				}
				return current[((nextRead)--)];
			}

			@Override
			public void skipBytes(long count) {
				setPosition(((getPosition()) - count));
			}

			@Override
			public void readBytes(byte[] b, int offset, int len) {
				for (int i = 0; i < len; i++) {
					b[(offset + i)] = readByte();
				}
			}

			@Override
			public long getPosition() {
				return ((((long) (nextBuffer)) + 1) * (blockSize)) + (nextRead);
			}

			@Override
			public void setPosition(long pos) {
				int bufferIndex = ((int) (pos >> (blockBits)));
				nextBuffer = bufferIndex - 1;
				current = blocks.get(bufferIndex);
				nextRead = ((int) (pos & (blockMask)));
				assert (getPosition()) == pos : (("pos=" + pos) + " getPos()=") + (getPosition());
			}

			@Override
			public boolean reversed() {
				return true;
			}
		};
	}

	@Override
	public long ramBytesUsed() {
		long size = BytesStore.BASE_RAM_BYTES_USED;
		for (byte[] block : blocks) {
			size += RamUsageEstimator.sizeOf(block);
		}
		return size;
	}

	@Override
	public String toString() {
		return (((getClass().getSimpleName()) + "(numBlocks=") + (blocks.size())) + ")";
	}
}

