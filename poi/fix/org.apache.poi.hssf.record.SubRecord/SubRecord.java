

import java.io.ByteArrayOutputStream;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.LittleEndianOutputStream;


public abstract class SubRecord {
	private static final int MAX_RECORD_LENGTH = 1000000;

	protected SubRecord() {
	}

	public static SubRecord createSubRecord(LittleEndianInput in, int cmoOt) {
		int sid = in.readUShort();
		int secondUShort = in.readUShort();
		return new SubRecord.UnknownSubRecord(in, sid, secondUShort);
	}

	protected abstract int getDataSize();

	public byte[] serialize() {
		int size = (getDataSize()) + 4;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
		serialize(new LittleEndianOutputStream(baos));
		if ((baos.size()) != size) {
			throw new RuntimeException("write size mismatch");
		}
		return baos.toByteArray();
	}

	public abstract void serialize(LittleEndianOutput out);

	@Override
	public abstract SubRecord clone();

	public boolean isTerminating() {
		return false;
	}

	private static final class UnknownSubRecord extends SubRecord {
		private final int _sid;

		private final byte[] _data;

		public UnknownSubRecord(LittleEndianInput in, int sid, int size) {
			_sid = sid;
			byte[] buf = IOUtils.safelyAllocate(size, SubRecord.MAX_RECORD_LENGTH);
			in.readFully(buf);
			_data = buf;
		}

		@Override
		protected int getDataSize() {
			return _data.length;
		}

		@Override
		public void serialize(LittleEndianOutput out) {
			out.writeShort(_sid);
			out.writeShort(_data.length);
			out.write(_data);
		}

		@Override
		public SubRecord.UnknownSubRecord clone() {
			return this;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(64);
			sb.append(getClass().getName()).append(" [");
			sb.append("sid=").append(HexDump.shortToHex(_sid));
			sb.append(" size=").append(_data.length);
			sb.append(" : ").append(HexDump.toHex(_data));
			sb.append("]\n");
			return sb.toString();
		}
	}
}

