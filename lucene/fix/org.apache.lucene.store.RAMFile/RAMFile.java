

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Accountable;


public class RAMFile implements Accountable {
	protected final ArrayList<byte[]> buffers = new ArrayList<>();

	long length;

	RAMDirectory directory;

	protected long sizeInBytes;

	public RAMFile() {
	}

	RAMFile(RAMDirectory directory) {
		this.directory = directory;
	}

	public synchronized long getLength() {
		return length;
	}

	protected synchronized void setLength(long length) {
		this.length = length;
	}

	protected final byte[] addBuffer(int size) {
		byte[] buffer = newBuffer(size);
		synchronized(this) {
			buffers.add(buffer);
			sizeInBytes += size;
		}
		if ((directory) != null) {
		}
		return buffer;
	}

	protected synchronized final byte[] getBuffer(int index) {
		return buffers.get(index);
	}

	protected synchronized final int numBuffers() {
		return buffers.size();
	}

	protected byte[] newBuffer(int size) {
		return new byte[size];
	}

	@Override
	public synchronized long ramBytesUsed() {
		return sizeInBytes;
	}

	@Override
	public String toString() {
		return (((getClass().getSimpleName()) + "(length=") + (length)) + ")";
	}

	@Override
	public int hashCode() {
		int h = ((int) ((length) ^ ((length) >>> 32)));
		for (byte[] block : buffers) {
			h = (31 * h) + (Arrays.hashCode(block));
		}
		return h;
	}

	@Override
	public boolean equals(Object obj) {
		if ((this) == obj)
			return true;

		if (obj == null)
			return false;

		if ((getClass()) != (obj.getClass()))
			return false;

		RAMFile other = ((RAMFile) (obj));
		if ((length) != (other.length))
			return false;

		if ((buffers.size()) != (other.buffers.size())) {
			return false;
		}
		for (int i = 0; i < (buffers.size()); i++) {
			if (!(Arrays.equals(buffers.get(i), other.buffers.get(i)))) {
				return false;
			}
		}
		return true;
	}
}

