

import org.apache.poi.hwpf.model.Xst;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


@Internal
public final class ListLevel {
	private static final int MAX_RECORD_LENGTH = 10485760;

	private static final POILogger logger = POILogFactory.getLogger(ListLevel.class);

	private byte[] _grpprlChpx;

	private byte[] _grpprlPapx;

	private Xst _xst = new Xst();

	ListLevel() {
	}

	@Deprecated
	public ListLevel(final byte[] buf, final int startOffset) {
		read(buf, startOffset);
	}

	public ListLevel(int level, boolean numbered) {
		setStartAt(1);
		_grpprlPapx = new byte[0];
		_grpprlChpx = new byte[0];
		if (numbered) {
			_xst = new Xst((("" + ((char) (level))) + "."));
		}else {
			_xst = new Xst("\u2022");
		}
	}

	public ListLevel(int startAt, int numberFormatCode, int alignment, byte[] numberProperties, byte[] entryProperties, String numberText) {
		setStartAt(startAt);
		_grpprlChpx = numberProperties.clone();
		_grpprlPapx = entryProperties.clone();
		_xst = new Xst(numberText);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ListLevel))
			return false;

		ListLevel lvl = ((ListLevel) (obj));
		return false;
	}

	@Override
	public int hashCode() {
		assert false : "hashCode not designed";
		return 42;
	}

	public int getAlignment() {
		return 0;
	}

	public byte[] getGrpprlChpx() {
		return _grpprlChpx;
	}

	public byte[] getGrpprlPapx() {
		return _grpprlPapx;
	}

	public byte[] getLevelProperties() {
		return _grpprlPapx;
	}

	public int getNumberFormat() {
		return 0;
	}

	public String getNumberText() {
		return _xst.getAsJavaString();
	}

	public int getSizeInBytes() {
		return 0;
	}

	public int getStartAt() {
		return 0;
	}

	public byte getTypeOfCharFollowingTheNumber() {
		return 0;
	}

	public short getRestart() {
		return 0;
	}

	public boolean isLegalNumbering() {
		return false;
	}

	public byte[] getLevelNumberingPlaceholderOffsets() {
		return null;
	}

	int read(final byte[] data, final int startOffset) {
		int offset = startOffset;
		_xst = new Xst(data, offset);
		offset += _xst.getSize();
		return offset - startOffset;
	}

	public void setAlignment(int alignment) {
	}

	public void setLevelProperties(byte[] grpprl) {
		_grpprlPapx = grpprl;
	}

	public void setNumberFormat(int numberFormatCode) {
	}

	public void setNumberProperties(byte[] grpprl) {
		_grpprlChpx = grpprl;
	}

	public void setStartAt(int startAt) {
	}

	public void setTypeOfCharFollowingTheNumber(byte value) {
	}

	public byte[] toByteArray() {
		byte[] buf = IOUtils.safelyAllocate(getSizeInBytes(), ListLevel.MAX_RECORD_LENGTH);
		int offset = 0;
		System.arraycopy(_grpprlPapx, 0, buf, offset, _grpprlPapx.length);
		offset += _grpprlPapx.length;
		System.arraycopy(_grpprlChpx, 0, buf, offset, _grpprlChpx.length);
		offset += _grpprlChpx.length;
		_xst.serialize(buf, offset);
		offset += _xst.getSize();
		return buf;
	}

	@Override
	public String toString() {
		return null;
	}
}

