

import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.hmef.attribute.TNEFProperty;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;


public class TNEFAttribute {
	private static final int MAX_RECORD_LENGTH = 1000000;

	private final TNEFProperty property;

	private final int type;

	private final byte[] data;

	private final int checksum;

	protected TNEFAttribute(int id, int type, InputStream inp) throws IOException {
		this.type = type;
		int length = LittleEndian.readInt(inp);
		property = TNEFProperty.getBest(id, type);
		data = IOUtils.safelyAllocate(length, TNEFAttribute.MAX_RECORD_LENGTH);
		IOUtils.readFully(inp, data);
		checksum = LittleEndian.readUShort(inp);
	}

	public static TNEFAttribute create(InputStream inp) throws IOException {
		int id = LittleEndian.readUShort(inp);
		int type = LittleEndian.readUShort(inp);
		if ((id == (TNEFProperty.ID_MAPIPROPERTIES.id)) || (id == (TNEFProperty.ID_ATTACHMENT.id))) {
		}
		if ((type == (TNEFProperty.TYPE_STRING)) || (type == (TNEFProperty.TYPE_TEXT))) {
		}
		if (type == (TNEFProperty.TYPE_DATE)) {
		}
		return new TNEFAttribute(id, type, inp);
	}

	public TNEFProperty getProperty() {
		return property;
	}

	public int getType() {
		return type;
	}

	public byte[] getData() {
		return data;
	}

	public String toString() {
		return (((("Attribute " + (property)) + ", type=") + (type)) + ", data length=") + (data.length);
	}
}

