

import java.io.EOFException;
import java.io.IOException;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.RAMFile;


public class RAMInputStream extends IndexInput implements Cloneable {
	private final RAMFile file;

	private final long length;

	private byte[] currentBuffer;

	private int currentBufferIndex;

	private int bufferPosition;

	private int bufferLength;

	RAMInputStream(String name, RAMFile f, long length) throws IOException {
		super((("RAMInputStream(name=" + name) + ")"));
		this.file = f;
		this.length = length;
		setCurrentBuffer();
	}

	@Override
	public void close() {
	}

	@Override
	public long length() {
		return length;
	}

	@Override
	public byte readByte() throws IOException {
		if ((bufferPosition) == (bufferLength)) {
			nextBuffer();
		}
		return currentBuffer[((bufferPosition)++)];
	}

	@Override
	public void readBytes(byte[] b, int offset, int len) throws IOException {
		while (len > 0) {
			if ((bufferPosition) == (bufferLength)) {
				nextBuffer();
			}
			int remainInBuffer = (bufferLength) - (bufferPosition);
			int bytesToCopy = (len < remainInBuffer) ? len : remainInBuffer;
			System.arraycopy(currentBuffer, bufferPosition, b, offset, bytesToCopy);
			offset += bytesToCopy;
			len -= bytesToCopy;
			bufferPosition += bytesToCopy;
		} 
	}

	@Override
	public long getFilePointer() {
		return 0L;
	}

	@Override
	public void seek(long pos) throws IOException {
		if ((getFilePointer()) > (length())) {
			throw new EOFException(((((("seek beyond EOF: pos=" + (getFilePointer())) + " vs length=") + (length())) + ": ") + (this)));
		}
	}

	private void nextBuffer() throws IOException {
		if ((getFilePointer()) >= (length())) {
			throw new EOFException(((((("cannot read another byte at EOF: pos=" + (getFilePointer())) + " vs length=") + (length())) + ": ") + (this)));
		}
		(currentBufferIndex)++;
		setCurrentBuffer();
		assert (currentBuffer) != null;
		bufferPosition = 0;
	}

	private final void setCurrentBuffer() throws IOException {
	}

	@Override
	public IndexInput slice(String sliceDescription, final long offset, final long sliceLength) throws IOException {
		if (((offset < 0) || (sliceLength < 0)) || ((offset + sliceLength) > (this.length))) {
			throw new IllegalArgumentException(((("slice() " + sliceDescription) + " out of bounds: ") + (this)));
		}
		return new RAMInputStream(getFullSliceDescription(sliceDescription), file, (offset + sliceLength)) {
			{
				seek(0L);
			}

			@Override
			public void seek(long pos) throws IOException {
				if (pos < 0L) {
					throw new IllegalArgumentException(("Seeking to negative position: " + (this)));
				}
				super.seek((pos + offset));
			}

			@Override
			public long getFilePointer() {
				return (super.getFilePointer()) - offset;
			}

			@Override
			public long length() {
				return sliceLength;
			}

			@Override
			public IndexInput slice(String sliceDescription, long ofs, long len) throws IOException {
				return super.slice(sliceDescription, (offset + ofs), len);
			}
		};
	}
}

