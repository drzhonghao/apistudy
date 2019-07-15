

import java.awt.Dimension;
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
import org.apache.poi.sl.image.ImageHeaderEMF;
import org.apache.poi.sl.usermodel.PictureData;

import static org.apache.poi.sl.usermodel.PictureData.PictureType.EMF;


public final class EMF extends Metafile {
	@Override
	public byte[] getData() {
		try {
			byte[] rawdata = getRawData();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream is = new ByteArrayInputStream(rawdata);
			Metafile.Header header = new Metafile.Header();
			header.read(rawdata, HSLFPictureData.CHECKSUM_SIZE);
			long len = is.skip(((header.getSize()) + ((long) (HSLFPictureData.CHECKSUM_SIZE))));
			assert len == ((header.getSize()) + (HSLFPictureData.CHECKSUM_SIZE));
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
		byte[] compressed = Metafile.compress(data, 0, data.length);
		ImageHeaderEMF nHeader = new ImageHeaderEMF(data, 0);
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
		return EMF;
	}

	public int getSignature() {
		return (getUIDInstanceCount()) == 1 ? 15680 : 15696;
	}

	public void setSignature(int signature) {
		switch (signature) {
			case 15680 :
				setUIDInstanceCount(1);
				break;
			case 15696 :
				setUIDInstanceCount(2);
				break;
			default :
				throw new IllegalArgumentException((signature + " is not a valid instance/signature value for EMF"));
		}
	}
}

