

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.util.LittleEndianByteArrayOutputStream;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;


public abstract class Ptg {
	public static final Ptg[] EMPTY_PTG_ARRAY = new Ptg[]{  };

	public static Ptg[] readTokens(int size, LittleEndianInput in) {
		List<Ptg> temp = new ArrayList<>((4 + (size / 2)));
		int pos = 0;
		boolean hasArrayPtgs = false;
		while (pos < size) {
			Ptg ptg = Ptg.createPtg(in);
			pos += ptg.getSize();
			temp.add(ptg);
		} 
		if (pos != size) {
			throw new RuntimeException("Ptg array size mismatch");
		}
		if (hasArrayPtgs) {
			Ptg[] result = Ptg.toPtgArray(temp);
			for (int i = 0; i < (result.length); i++) {
			}
			return result;
		}
		return Ptg.toPtgArray(temp);
	}

	public static Ptg createPtg(LittleEndianInput in) {
		byte id = in.readByte();
		if (id < 32) {
			return Ptg.createBasePtg(id, in);
		}
		Ptg retval = Ptg.createClassifiedPtg(id, in);
		if (id >= 96) {
			retval.setClass(Ptg.CLASS_ARRAY);
		}else
			if (id >= 64) {
				retval.setClass(Ptg.CLASS_VALUE);
			}else {
				retval.setClass(Ptg.CLASS_REF);
			}

		return retval;
	}

	private static Ptg createClassifiedPtg(byte id, LittleEndianInput in) {
		int baseId = (id & 31) | 32;
		throw new UnsupportedOperationException(((((" Unknown Ptg in Formula: 0x" + (Integer.toHexString(id))) + " (") + ((int) (id))) + ")"));
	}

	private static Ptg createBasePtg(byte id, LittleEndianInput in) {
		throw new RuntimeException((("Unexpected base token id (" + id) + ")"));
	}

	private static Ptg[] toPtgArray(List<Ptg> l) {
		if (l.isEmpty()) {
			return Ptg.EMPTY_PTG_ARRAY;
		}
		Ptg[] result = new Ptg[l.size()];
		l.toArray(result);
		return result;
	}

	public static int getEncodedSize(Ptg[] ptgs) {
		int result = 0;
		for (Ptg ptg : ptgs) {
			result += ptg.getSize();
		}
		return result;
	}

	public static int getEncodedSizeWithoutArrayData(Ptg[] ptgs) {
		int result = 0;
		for (Ptg ptg : ptgs) {
		}
		return result;
	}

	public static int serializePtgs(Ptg[] ptgs, byte[] array, int offset) {
		LittleEndianByteArrayOutputStream out = new LittleEndianByteArrayOutputStream(array, offset);
		List<Ptg> arrayPtgs = null;
		for (Ptg ptg : ptgs) {
			ptg.write(out);
		}
		if (arrayPtgs != null) {
			for (Ptg arrayPtg : arrayPtgs) {
			}
		}
		return (out.getWriteIndex()) - offset;
	}

	public abstract int getSize();

	public abstract void write(LittleEndianOutput out);

	public abstract String toFormulaString();

	@Override
	public String toString() {
		return this.getClass().toString();
	}

	public static final byte CLASS_REF = 0;

	public static final byte CLASS_VALUE = 32;

	public static final byte CLASS_ARRAY = 64;

	private byte ptgClass = Ptg.CLASS_REF;

	public final void setClass(byte thePtgClass) {
		if (isBaseToken()) {
			throw new RuntimeException("setClass should not be called on a base token");
		}
		ptgClass = thePtgClass;
	}

	public final byte getPtgClass() {
		return ptgClass;
	}

	public final char getRVAType() {
		if (isBaseToken()) {
			return '.';
		}
		switch (ptgClass) {
			case Ptg.CLASS_REF :
				return 'R';
			case Ptg.CLASS_VALUE :
				return 'V';
			case Ptg.CLASS_ARRAY :
				return 'A';
		}
		throw new RuntimeException((("Unknown operand class (" + (ptgClass)) + ")"));
	}

	public abstract byte getDefaultOperandClass();

	public abstract boolean isBaseToken();

	public static boolean doesFormulaReferToDeletedCell(Ptg[] ptgs) {
		for (Ptg ptg : ptgs) {
			if (Ptg.isDeletedCellRef(ptg)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isDeletedCellRef(Ptg ptg) {
		return false;
	}
}

