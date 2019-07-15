

import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import org.apache.poi.poifs.crypt.CryptoFunctions;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.Units;


public abstract class HSLFPictureData implements PictureData {
	protected static final int CHECKSUM_SIZE = 16;

	private byte[] rawdata;

	private int offset;

	private int uidInstanceCount = 1;

	private int index = -1;

	protected abstract int getSignature();

	public abstract void setSignature(int signature);

	protected int getUIDInstanceCount() {
		return uidInstanceCount;
	}

	protected void setUIDInstanceCount(int uidInstanceCount) {
		this.uidInstanceCount = uidInstanceCount;
	}

	public byte[] getRawData() {
		return rawdata;
	}

	public void setRawData(byte[] data) {
		rawdata = (data == null) ? null : data.clone();
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public byte[] getUID() {
		byte[] uid = new byte[16];
		System.arraycopy(rawdata, 0, uid, 0, uid.length);
		return uid;
	}

	@Override
	public byte[] getChecksum() {
		return HSLFPictureData.getChecksum(getData());
	}

	public static byte[] getChecksum(byte[] data) {
		MessageDigest md5 = CryptoFunctions.getMessageDigest(HashAlgorithm.md5);
		md5.update(data);
		return md5.digest();
	}

	public void write(OutputStream out) throws IOException {
		byte[] data;
		data = new byte[LittleEndian.SHORT_SIZE];
		LittleEndian.putUShort(data, 0, getSignature());
		out.write(data);
		data = new byte[LittleEndian.SHORT_SIZE];
		PictureData.PictureType pt = getType();
		LittleEndian.putUShort(data, 0, ((pt.nativeId) + 61464));
		out.write(data);
		byte[] rd = getRawData();
		data = new byte[LittleEndian.INT_SIZE];
		LittleEndian.putInt(data, 0, rd.length);
		out.write(data);
		out.write(rd);
	}

	public static HSLFPictureData create(PictureData.PictureType type) {
		HSLFPictureData pict;
		pict = null;
		return pict;
	}

	public byte[] getHeader() {
		byte[] header = new byte[16 + 8];
		LittleEndian.putInt(header, 0, getSignature());
		LittleEndian.putInt(header, 4, getRawData().length);
		System.arraycopy(rawdata, 0, header, 8, 16);
		return header;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public final String getContentType() {
		return getType().contentType;
	}

	@Override
	public Dimension getImageDimensionInPixels() {
		Dimension dim = getImageDimension();
		return new Dimension(Units.pointsToPixel(dim.getWidth()), Units.pointsToPixel(dim.getHeight()));
	}
}

