

import java.nio.charset.Charset;
import org.apache.poi.hdgf.chunks.ChunkHeaderV11;
import org.apache.poi.hdgf.chunks.ChunkHeaderV4V5;
import org.apache.poi.hdgf.chunks.ChunkHeaderV6;


public abstract class ChunkHeader {
	private int type;

	private int id;

	private int length;

	private int unknown1;

	public static ChunkHeader createChunkHeader(int documentVersion, byte[] data, int offset) {
		if (documentVersion >= 6) {
			ChunkHeaderV6 ch;
			if (documentVersion > 6) {
				ch = new ChunkHeaderV11();
			}else {
				ch = new ChunkHeaderV6();
			}
		}else
			if ((documentVersion == 5) || (documentVersion == 4)) {
				ChunkHeaderV4V5 ch = new ChunkHeaderV4V5();
			}else {
				throw new IllegalArgumentException(("Visio files with versions below 4 are not supported, yours was " + documentVersion));
			}

		return null;
	}

	public static int getHeaderSize(int documentVersion) {
		if (documentVersion > 6) {
		}else
			if (documentVersion == 6) {
			}else {
			}

		return 0;
	}

	public abstract int getSizeInBytes();

	public abstract boolean hasTrailer();

	public abstract boolean hasSeparator();

	public abstract Charset getChunkCharset();

	public int getId() {
		return id;
	}

	public int getLength() {
		return length;
	}

	public int getType() {
		return type;
	}

	public int getUnknown1() {
		return unknown1;
	}

	void setType(int type) {
		this.type = type;
	}

	void setId(int id) {
		this.id = id;
	}

	void setLength(int length) {
		this.length = length;
	}

	void setUnknown1(int unknown1) {
		this.unknown1 = unknown1;
	}
}

