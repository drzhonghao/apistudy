import org.apache.poi.hdgf.streams.*;


import org.apache.poi.util.IOUtils;

/**
 * Holds the representation of the stream on-disk, and
 *  handles de-compressing it as required.
 * In future, may also handle writing it back out again
 */
public class StreamStore { // TODO - instantiable superclass
	//arbitrarily selected; may need to increase
	private static final int MAX_RECORD_LENGTH = 10_000_000;

	private byte[] contents;

	/**
	 * Creates a new, non compressed Stream Store
	 */
	protected StreamStore(byte[] data, int offset, int length) {
		contents = IOUtils.safelyAllocate(length, MAX_RECORD_LENGTH);
		System.arraycopy(data, offset, contents, 0, length);
	}

	protected void prependContentsWith(byte[] b) {
		byte[] newContents = IOUtils.safelyAllocate(contents.length + b.length, MAX_RECORD_LENGTH);
		System.arraycopy(b, 0, newContents, 0, b.length);
		System.arraycopy(contents, 0, newContents, b.length, contents.length);
		contents = newContents;
	}
	protected void copyBlockHeaderToContents() {}

	protected byte[] getContents() { return contents; }
	public byte[] _getContents() { return contents; }
}
