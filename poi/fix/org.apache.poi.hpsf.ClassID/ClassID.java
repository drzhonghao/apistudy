

import java.util.Arrays;
import org.apache.poi.util.HexDump;


public class ClassID {
	public static final int LENGTH = 16;

	private final byte[] bytes = new byte[ClassID.LENGTH];

	public ClassID(final byte[] src, final int offset) {
		read(src, offset);
	}

	public ClassID() {
		Arrays.fill(bytes, ((byte) (0)));
	}

	public ClassID(String externalForm) {
		String clsStr = externalForm.replaceAll("[{}-]", "");
		for (int i = 0; i < (clsStr.length()); i += 2) {
			bytes[(i / 2)] = ((byte) (Integer.parseInt(clsStr.substring(i, (i + 2)), 16)));
		}
	}

	public int length() {
		return ClassID.LENGTH;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(final byte[] bytes) {
		System.arraycopy(bytes, 0, this.bytes, 0, ClassID.LENGTH);
	}

	public byte[] read(final byte[] src, final int offset) {
		bytes[0] = src[(3 + offset)];
		bytes[1] = src[(2 + offset)];
		bytes[2] = src[(1 + offset)];
		bytes[3] = src[(0 + offset)];
		bytes[4] = src[(5 + offset)];
		bytes[5] = src[(4 + offset)];
		bytes[6] = src[(7 + offset)];
		bytes[7] = src[(6 + offset)];
		System.arraycopy(src, (8 + offset), bytes, 8, 8);
		return bytes;
	}

	public void write(final byte[] dst, final int offset) throws ArrayStoreException {
		if ((dst.length) < (ClassID.LENGTH)) {
			throw new ArrayStoreException(((("Destination byte[] must have room for at least 16 bytes, " + "but has a length of only ") + (dst.length)) + "."));
		}
		dst[(0 + offset)] = bytes[3];
		dst[(1 + offset)] = bytes[2];
		dst[(2 + offset)] = bytes[1];
		dst[(3 + offset)] = bytes[0];
		dst[(4 + offset)] = bytes[5];
		dst[(5 + offset)] = bytes[4];
		dst[(6 + offset)] = bytes[7];
		dst[(7 + offset)] = bytes[6];
		System.arraycopy(bytes, 8, dst, (8 + offset), 8);
	}

	@Override
	public boolean equals(final Object o) {
		return (o instanceof ClassID) && (Arrays.equals(bytes, ((ClassID) (o)).bytes));
	}

	public boolean equalsInverted(ClassID o) {
		return ((((((((((((((((o.bytes[0]) == (bytes[3])) && ((o.bytes[1]) == (bytes[2]))) && ((o.bytes[2]) == (bytes[1]))) && ((o.bytes[3]) == (bytes[0]))) && ((o.bytes[4]) == (bytes[5]))) && ((o.bytes[5]) == (bytes[4]))) && ((o.bytes[6]) == (bytes[7]))) && ((o.bytes[7]) == (bytes[6]))) && ((o.bytes[8]) == (bytes[8]))) && ((o.bytes[9]) == (bytes[9]))) && ((o.bytes[10]) == (bytes[10]))) && ((o.bytes[11]) == (bytes[11]))) && ((o.bytes[12]) == (bytes[12]))) && ((o.bytes[13]) == (bytes[13]))) && ((o.bytes[14]) == (bytes[14]))) && ((o.bytes[15]) == (bytes[15]));
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sbClassId = new StringBuilder(38);
		sbClassId.append('{');
		for (int i = 0; i < (ClassID.LENGTH); i++) {
			sbClassId.append(HexDump.toHex(bytes[i]));
			if ((((i == 3) || (i == 5)) || (i == 7)) || (i == 9)) {
				sbClassId.append('-');
			}
		}
		sbClassId.append('}');
		return sbClassId.toString();
	}
}

