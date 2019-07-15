

import java.io.ByteArrayInputStream;
import org.apache.poi.hssf.record.RecordBase;
import org.apache.poi.hssf.record.RecordInputStream;


public abstract class Record extends RecordBase {
	protected Record() {
	}

	public final byte[] serialize() {
		byte[] retval = new byte[getRecordSize()];
		serialize(0, retval);
		return retval;
	}

	@Override
	public String toString() {
		return super.toString();
	}

	public abstract short getSid();

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException((("The class " + (getClass().getName())) + " needs to define a clone method"));
	}

	public Record cloneViaReserialise() {
		byte[] b = serialize();
		RecordInputStream rinp = new RecordInputStream(new ByteArrayInputStream(b));
		rinp.nextRecord();
		return null;
	}
}

