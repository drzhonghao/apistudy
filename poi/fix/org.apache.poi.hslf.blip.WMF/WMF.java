

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.InflaterInputStream;
import org.apache.poi.hslf.blip.Metafile;
import org.apache.poi.hslf.exceptions.HSLFException;
import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.sl.image.ImageHeaderWMF;
import org.apache.poi.sl.usermodel.PictureData;

import static org.apache.poi.sl.usermodel.PictureData.PictureType.WMF;


public final class WMF extends Metafile {
	@Override
	public byte[] getData() {
		try {
			byte[] rawdata = getRawData();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream is = new ByteArrayInputStream(rawdata);
			Metafile.Header header = new Metafile.Header();
			header.read(rawdata, ((HSLFPictureData.CHECKSUM_SIZE) * (getUIDInstanceCount())));
			long len = is.skip(((header.getSize()) + (((long) (HSLFPictureData.CHECKSUM_SIZE)) * (getUIDInstanceCount()))));
			assert len == ((header.getSize()) + ((HSLFPictureData.CHECKSUM_SIZE) * (getUIDInstanceCount())));
			ImageHeaderWMF aldus = new ImageHeaderWMF(header.getBounds());
			aldus.write(out);
			InflaterInputStream inflater = new InflaterInputStream(is);
			byte[] chunk = new byte[4096];
			int count;
			while ((count = inflater.read(chunk)) >= 0) {
				out.write(chunk, 0, count);
			} 
			inflater.close();
			return out.toByteArray();
		} catch (IOException e) {
			throw new HSLFException(e);
		}
	}

	@Override
	public void setData(byte[] data) throws IOException {
		int pos = 0;
		ImageHeaderWMF nHeader = new ImageHeaderWMF(data, pos);
		pos += nHeader.getLength();
		byte[] compressed = Metafile.compress(data, pos, ((data.length) - pos));
		Metafile.Header header = new Metafile.Header();
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
		return WMF;
	}

	public int getSignature() {
		return (getUIDInstanceCount()) == 1 ? 8544 : 8560;
	}

	public void setSignature(int signature) {
		switch (signature) {
			case 8544 :
				setUIDInstanceCount(1);
				break;
			case 8560 :
				setUIDInstanceCount(2);
				break;
			default :
				throw new IllegalArgumentException((signature + " is not a valid instance/signature value for WMF"));
		}
	}
}

