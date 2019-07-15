import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Integer (unsigned short integer) Stores an unsigned short value (java int) in
 * a formula
 * 
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 */
public final class IntPtg extends ScalarConstantPtg {
	// 16 bit unsigned integer
	private static final int MIN_VALUE = 0x0000;
	private static final int MAX_VALUE = 0xFFFF;

	/**
	 * Excel represents integers 0..65535 with the tInt token.
	 * 
	 * @return <code>true</code> if the specified value is within the range of values 
	 * <tt>IntPtg</tt> can represent.
	 */
	public static boolean isInRange(int i) {
		return i >= MIN_VALUE && i <= MAX_VALUE;
	}

	public final static int SIZE = 3;
	public final static byte sid = 0x1e;
	private final int field_1_value;

	public IntPtg(LittleEndianInput in)  {
		this(in.readUShort());
	}

	public IntPtg(int value) {
		if (!isInRange(value)) {
			throw new IllegalArgumentException("value is out of range: " + value);
		}
		field_1_value = value;
	}

	public int getValue() {
		return field_1_value;
	}

	public void write(LittleEndianOutput out) {
		out.writeByte(sid + getPtgClass());
		out.writeShort(getValue());
	}

	public int getSize() {
		return SIZE;
	}

	public String toFormulaString() {
		return String.valueOf(getValue());
	}
}
