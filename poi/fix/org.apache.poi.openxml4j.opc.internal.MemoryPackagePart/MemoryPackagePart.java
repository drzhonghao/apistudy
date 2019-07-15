

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.internal.ContentType;
import org.apache.poi.openxml4j.opc.internal.marshallers.ZipPartMarshaller;
import org.apache.poi.util.IOUtils;


public final class MemoryPackagePart extends PackagePart {
	protected byte[] data;

	public MemoryPackagePart(OPCPackage pack, PackagePartName partName, String contentType) throws InvalidFormatException {
		super(pack, partName, contentType);
	}

	public MemoryPackagePart(OPCPackage pack, PackagePartName partName, String contentType, boolean loadRelationships) throws InvalidFormatException {
		super(pack, partName, new ContentType(contentType), loadRelationships);
	}

	@Override
	protected InputStream getInputStreamImpl() {
		if ((data) == null) {
			data = new byte[0];
		}
		return new ByteArrayInputStream(data);
	}

	@Override
	protected OutputStream getOutputStreamImpl() {
		return null;
	}

	@Override
	public long getSize() {
		return (data) == null ? 0 : data.length;
	}

	@Override
	public void clear() {
		data = null;
	}

	@Override
	public boolean save(OutputStream os) throws OpenXML4JException {
		return new ZipPartMarshaller().marshall(this, os);
	}

	@Override
	public boolean load(InputStream ios) throws InvalidFormatException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			IOUtils.copy(ios, baos);
		} catch (IOException e) {
			throw new InvalidFormatException(e.getMessage());
		}
		data = baos.toByteArray();
		return true;
	}

	@Override
	public void close() {
	}

	@Override
	public void flush() {
	}
}

