

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hmef.attribute.TNEFAttribute;
import org.apache.poi.hmef.attribute.TNEFProperty;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.Types;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.StringUtil;


public class MAPIAttribute {
	private static final int MAX_RECORD_LENGTH = 1000000;

	private final MAPIProperty property;

	private final int type;

	private final byte[] data;

	public MAPIAttribute(MAPIProperty property, int type, byte[] data) {
		this.property = property;
		this.type = type;
		this.data = data.clone();
	}

	public MAPIProperty getProperty() {
		return property;
	}

	public int getType() {
		return type;
	}

	public byte[] getData() {
		return data;
	}

	public String toString() {
		String hex;
		if ((data.length) <= 16) {
			hex = HexDump.toHex(data);
		}else {
			byte[] d = new byte[16];
			System.arraycopy(data, 0, d, 0, 16);
			hex = HexDump.toHex(d);
			hex = (hex.substring(0, ((hex.length()) - 1))) + ", ....]";
		}
		return ((property) + " ") + hex;
	}

	public static List<MAPIAttribute> create(TNEFAttribute parent) throws IOException {
		if ((parent.getProperty()) == (TNEFProperty.ID_MAPIPROPERTIES)) {
		}else
			if ((parent.getProperty()) == (TNEFProperty.ID_ATTACHMENT)) {
			}else {
				throw new IllegalArgumentException(((("Can only create from a MAPIProperty attribute, " + "instead received a ") + (parent.getProperty())) + " one"));
			}

		ByteArrayInputStream inp = new ByteArrayInputStream(parent.getData());
		int count = LittleEndian.readInt(inp);
		List<MAPIAttribute> attrs = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			int typeAndMV = LittleEndian.readUShort(inp);
			int id = LittleEndian.readUShort(inp);
			boolean isMV = false;
			boolean isVL = false;
			int typeId = typeAndMV;
			if ((typeAndMV & (Types.MULTIVALUED_FLAG)) != 0) {
				isMV = true;
				typeId -= Types.MULTIVALUED_FLAG;
			}
			if ((((typeId == (Types.ASCII_STRING.getId())) || (typeId == (Types.UNICODE_STRING.getId()))) || (typeId == (Types.BINARY.getId()))) || (typeId == (Types.DIRECTORY.getId()))) {
				isVL = true;
			}
			Types.MAPIType type = Types.getById(typeId);
			if (type == null) {
				type = Types.createCustom(typeId);
			}
			MAPIProperty prop = MAPIProperty.get(id);
			if ((id >= 32768) && (id <= 65535)) {
				byte[] guid = new byte[16];
				IOUtils.readFully(inp, guid);
				int mptype = LittleEndian.readInt(inp);
				String name;
				if (mptype == 0) {
					int mpid = LittleEndian.readInt(inp);
					MAPIProperty base = MAPIProperty.get(mpid);
					name = base.name;
				}else {
					int mplen = LittleEndian.readInt(inp);
					byte[] mpdata = IOUtils.safelyAllocate(mplen, MAPIAttribute.MAX_RECORD_LENGTH);
					IOUtils.readFully(inp, mpdata);
					name = StringUtil.getFromUnicodeLE(mpdata, 0, ((mplen / 2) - 1));
					MAPIAttribute.skipToBoundary(mplen, inp);
				}
				prop = MAPIProperty.createCustom(id, type, name);
			}
			if (prop == (MAPIProperty.UNKNOWN)) {
				prop = MAPIProperty.createCustom(id, type, (("(unknown " + (Integer.toHexString(id))) + ")"));
			}
			int values = 1;
			if (isMV || isVL) {
				values = LittleEndian.readInt(inp);
			}
			for (int j = 0; j < values; j++) {
				int len = MAPIAttribute.getLength(type, inp);
				byte[] data = IOUtils.safelyAllocate(len, MAPIAttribute.MAX_RECORD_LENGTH);
				IOUtils.readFully(inp, data);
				MAPIAttribute.skipToBoundary(len, inp);
				MAPIAttribute attr;
				if ((type == (Types.UNICODE_STRING)) || (type == (Types.ASCII_STRING))) {
				}else
					if ((type == (Types.APP_TIME)) || (type == (Types.TIME))) {
					}else
						if (id == (MAPIProperty.RTF_COMPRESSED.id)) {
						}else {
							attr = new MAPIAttribute(prop, typeId, data);
						}


				attr = null;
				attrs.add(attr);
			}
		}
		return attrs;
	}

	private static int getLength(Types.MAPIType type, InputStream inp) throws IOException {
		if (type.isFixedLength()) {
			return type.getLength();
		}
		if ((((type == (Types.ASCII_STRING)) || (type == (Types.UNICODE_STRING))) || (type == (Types.DIRECTORY))) || (type == (Types.BINARY))) {
			return LittleEndian.readInt(inp);
		}else {
			throw new IllegalArgumentException(("Unknown type " + type));
		}
	}

	private static void skipToBoundary(int length, InputStream inp) throws IOException {
		if ((length % 4) != 0) {
			int toSkip = 4 - (length % 4);
			long skipped = IOUtils.skipFully(inp, toSkip);
			if (skipped != toSkip) {
				throw new IOException(((("tried to skip " + toSkip) + " but only skipped:") + skipped));
			}
		}
	}
}

