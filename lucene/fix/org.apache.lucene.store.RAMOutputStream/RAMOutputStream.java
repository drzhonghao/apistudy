

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.apache.lucene.store.BufferedChecksum;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMFile;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;


public class RAMOutputStream extends IndexOutput implements Accountable {
	static final int BUFFER_SIZE = 1024;

	private final RAMFile file;

	private byte[] currentBuffer;

	private int currentBufferIndex;

	private int bufferPosition;

	private long bufferStart;

	private int bufferLength;

	private final Checksum crc;

	public RAMOutputStream() {
		this("noname", new RAMFile(), false);
	}

	public RAMOutputStream(RAMFile f, boolean checksum) {
		this("noname", f, checksum);
	}

	public RAMOutputStream(String name, RAMFile f, boolean checksum) {
		super((("RAMOutputStream(name=\"" + name) + "\")"), name);
		file = f;
		currentBufferIndex = -1;
		currentBuffer = null;
		if (checksum) {
			crc = new BufferedChecksum(new CRC32());
		}else {
			crc = null;
		}
	}

	public void writeTo(DataOutput out) throws IOException {
		flush();
		long pos = 0;
		int buffer = 0;
	}

	public void writeTo(byte[] bytes, int offset) throws IOException {
		flush();
		long pos = 0;
		int buffer = 0;
		int bytesUpto = offset;
	}

	public void reset() {
		currentBuffer = null;
		currentBufferIndex = -1;
		bufferPosition = 0;
		bufferStart = 0;
		bufferLength = 0;
		if ((crc) != null) {
			crc.reset();
		}
	}

	@Override
	public void close() throws IOException {
		flush();
	}

	@Override
	public void writeByte(byte b) throws IOException {
		if ((bufferPosition) == (bufferLength)) {
			(currentBufferIndex)++;
			switchCurrentBuffer();
		}
		if ((crc) != null) {
			crc.update(b);
		}
		currentBuffer[((bufferPosition)++)] = b;
	}

	@Override
	public void writeBytes(byte[] b, int offset, int len) throws IOException {
		assert b != null;
		if ((crc) != null) {
			crc.update(b, offset, len);
		}
		while (len > 0) {
			if ((bufferPosition) == (bufferLength)) {
				(currentBufferIndex)++;
				switchCurrentBuffer();
			}
			int remainInBuffer = (currentBuffer.length) - (bufferPosition);
			int bytesToCopy = (len < remainInBuffer) ? len : remainInBuffer;
			System.arraycopy(b, offset, currentBuffer, bufferPosition, bytesToCopy);
			offset += bytesToCopy;
			len -= bytesToCopy;
			bufferPosition += bytesToCopy;
		} 
	}

	private final void switchCurrentBuffer() {
		bufferPosition = 0;
		bufferStart = ((long) (RAMOutputStream.BUFFER_SIZE)) * ((long) (currentBufferIndex));
		bufferLength = currentBuffer.length;
	}

	private void setFileLength() {
		long pointer = (bufferStart) + (bufferPosition);
	}

	protected void flush() throws IOException {
		setFileLength();
	}

	@Override
	public long getFilePointer() {
		return (currentBufferIndex) < 0 ? 0 : (bufferStart) + (bufferPosition);
	}

	@Override
	public long ramBytesUsed() {
		return 0l;
	}

	@Override
	public Collection<Accountable> getChildResources() {
		return Collections.singleton(Accountables.namedAccountable("file", file));
	}

	@Override
	public long getChecksum() throws IOException {
		if ((crc) == null) {
			throw new IllegalStateException("internal RAMOutputStream created with checksum disabled");
		}else {
			return crc.getValue();
		}
	}
}

