

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.InflaterInputStream;
import org.apache.poi.hslf.blip.Metafile;
import org.apache.poi.hslf.exceptions.HSLFException;
import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.sl.image.ImageHeaderPICT;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

import static org.apache.poi.sl.usermodel.PictureData.PictureType.PICT;


public final class PICT extends Metafile {
	private static final POILogger LOG = POILogFactory.getLogger(PICT.class);

	@Override
	public byte[] getData() {
		byte[] rawdata = getRawData();
		try {
			byte[] macheader = new byte[512];
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write(macheader);
			int pos = (HSLFPictureData.CHECKSUM_SIZE) * (getUIDInstanceCount());
			byte[] pict = read(rawdata, pos);
			out.write(pict);
			return out.toByteArray();
		} catch (IOException e) {
			throw new HSLFException(e);
		}
	}

	private byte[] read(byte[] data, int pos) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		Metafile.Header header = new Metafile.Header();
		header.read(data, pos);
		long bs_exp = ((long) (pos)) + (header.getSize());
		long bs_act = bis.skip(bs_exp);
		if (bs_exp != bs_act) {
			throw new EOFException();
		}
		byte[] chunk = new byte[4096];
		ByteArrayOutputStream out = new ByteArrayOutputStream(header.getWmfSize());
		InflaterInputStream inflater = new InflaterInputStream(bis);
		try {
			int count;
			while ((count = inflater.read(chunk)) >= 0) {
				out.write(chunk, 0, count);
			} 
		} catch (Exception e) {
			int lastLen;
			for (lastLen = (chunk.length) - 1; (lastLen >= 0) && ((chunk[lastLen]) == 0); lastLen--);
			if ((++lastLen) > 0) {
				if ((header.getWmfSize()) > (out.size())) {
					lastLen = Math.min(lastLen, ((header.getWmfSize()) - (out.size())));
				}
				out.write(chunk, 0, lastLen);
			}
		} finally {
			inflater.close();
		}
		return out.toByteArray();
	}

	@Override
	public void setData(byte[] data) throws IOException {
		final int nOffset = ImageHeaderPICT.PICT_HEADER_OFFSET;
		ImageHeaderPICT nHeader = new ImageHeaderPICT(data, nOffset);
		Metafile.Header header = new Metafile.Header();
		int wmfSize = (data.length) - nOffset;
		byte[] compressed = Metafile.compress(data, nOffset, wmfSize);
		Dimension nDim = nHeader.getSize();
		byte[] checksum = HSLFPictureData.getChecksum(data);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(checksum);
		if ((getUIDInstanceCount()) == 2) {
			out.write(checksum);
		}
		header.write(out);
		out.write(compressed);
		setRawData(out.toByteArray());
	}

	@Override
	public PictureData.PictureType getType() {
		return PICT;
	}

	public int getSignature() {
		return (getUIDInstanceCount()) == 1 ? 21536 : 21552;
	}

	public void setSignature(int signature) {
		switch (signature) {
			case 21536 :
				setUIDInstanceCount(1);
				break;
			case 21552 :
				setUIDInstanceCount(2);
				break;
			default :
				throw new IllegalArgumentException((signature + " is not a valid instance/signature value for PICT"));
		}
	}

	private static void bytefill(byte[] array, byte value) {
		int len = array.length;
		if (len > 0) {
			array[0] = value;
		}
		for (int i = 1; i < len; i += i) {
			System.arraycopy(array, 0, array, i, ((len - i) < i ? len - i : i));
		}
	}
}

