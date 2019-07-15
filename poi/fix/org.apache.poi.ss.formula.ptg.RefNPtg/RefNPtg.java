

import org.apache.poi.util.LittleEndianInput;


public final class RefNPtg {
	public static final byte sid = 44;

	public RefNPtg(LittleEndianInput in) {
	}

	protected byte getSid() {
		return RefNPtg.sid;
	}

	protected final String formatReferenceAsString() {
		StringBuilder builder = new StringBuilder();
		return builder.toString();
	}
}

