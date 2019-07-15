

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Iterator;
import org.apache.poi.poifs.common.POIFSConstants;
import org.apache.poi.poifs.filesystem.BlockStore;


public class POIFSStream implements Iterable<ByteBuffer> {
	private BlockStore blockStore;

	private int startBlock;

	private OutputStream outStream;

	public POIFSStream(BlockStore blockStore, int startBlock) {
		this.blockStore = blockStore;
		this.startBlock = startBlock;
	}

	public POIFSStream(BlockStore blockStore) {
		this.blockStore = blockStore;
		this.startBlock = POIFSConstants.END_OF_CHAIN;
	}

	public int getStartBlock() {
		return startBlock;
	}

	public Iterator<ByteBuffer> iterator() {
		return getBlockIterator();
	}

	Iterator<ByteBuffer> getBlockIterator() {
		if ((startBlock) == (POIFSConstants.END_OF_CHAIN)) {
			throw new IllegalStateException("Can't read from a new stream before it has been written to");
		}
		return new POIFSStream.StreamBlockByteBufferIterator(startBlock);
	}

	void updateContents(byte[] contents) throws IOException {
		OutputStream os = getOutputStream();
		os.write(contents);
		os.close();
	}

	public OutputStream getOutputStream() throws IOException {
		if ((outStream) == null) {
			outStream = new POIFSStream.StreamBlockByteBuffer();
		}
		return outStream;
	}

	public void free() throws IOException {
	}

	protected class StreamBlockByteBufferIterator implements Iterator<ByteBuffer> {
		private int nextBlock;

		StreamBlockByteBufferIterator(int firstBlock) {
			this.nextBlock = firstBlock;
		}

		public boolean hasNext() {
			return (nextBlock) != (POIFSConstants.END_OF_CHAIN);
		}

		public ByteBuffer next() {
			if ((nextBlock) == (POIFSConstants.END_OF_CHAIN)) {
				throw new IndexOutOfBoundsException("Can't read past the end of the stream");
			}
			return null;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	protected class StreamBlockByteBuffer extends OutputStream {
		byte[] oneByte = new byte[1];

		ByteBuffer buffer;

		int prevBlock;

		int nextBlock;

		StreamBlockByteBuffer() throws IOException {
			prevBlock = POIFSConstants.END_OF_CHAIN;
			nextBlock = startBlock;
		}

		void createBlockIfNeeded() throws IOException {
			if (((buffer) != null) && (buffer.hasRemaining()))
				return;

			int thisBlock = nextBlock;
			if (thisBlock == (POIFSConstants.END_OF_CHAIN)) {
				nextBlock = POIFSConstants.END_OF_CHAIN;
				if ((prevBlock) != (POIFSConstants.END_OF_CHAIN)) {
				}
				if ((startBlock) == (POIFSConstants.END_OF_CHAIN)) {
					startBlock = thisBlock;
				}
			}else {
			}
			prevBlock = thisBlock;
		}

		@Override
		public void write(int b) throws IOException {
			oneByte[0] = ((byte) (b & 255));
			write(oneByte);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (((((off < 0) || (off > (b.length))) || (len < 0)) || ((off + len) > (b.length))) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			}else
				if (len == 0) {
					return;
				}

			do {
				createBlockIfNeeded();
				int writeBytes = Math.min(buffer.remaining(), len);
				buffer.put(b, off, writeBytes);
				off += writeBytes;
				len -= writeBytes;
			} while (len > 0 );
		}

		public void close() throws IOException {
			POIFSStream toFree = new POIFSStream(blockStore, nextBlock);
			if ((prevBlock) != (POIFSConstants.END_OF_CHAIN)) {
			}
		}
	}
}

