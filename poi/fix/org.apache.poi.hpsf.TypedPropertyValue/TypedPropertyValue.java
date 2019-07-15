

import java.io.ByteArrayInputStream;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianByteArrayInputStream;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


@Internal
public class TypedPropertyValue {
	private static final POILogger LOG = POILogFactory.getLogger(TypedPropertyValue.class);

	private int _type;

	private Object _value;

	public TypedPropertyValue(int type, Object value) {
		_type = type;
		_value = value;
	}

	public Object getValue() {
		return _value;
	}

	public void read(LittleEndianByteArrayInputStream lei) {
		_type = lei.readShort();
		short padding = lei.readShort();
		if (padding != 0) {
			TypedPropertyValue.LOG.log(POILogger.WARN, ((("TypedPropertyValue padding at offset " + (lei.getReadIndex())) + " MUST be 0, but it's value is ") + padding));
		}
		readValue(lei);
	}

	public void readValue(LittleEndianByteArrayInputStream lei) {
	}

	static void skipPadding(LittleEndianByteArrayInputStream lei) {
		final int offset = lei.getReadIndex();
		int skipBytes = (4 - (offset & 3)) & 3;
		for (int i = 0; i < skipBytes; i++) {
			lei.mark(1);
			int b = lei.read();
			if ((b == (-1)) || (b != 0)) {
				lei.reset();
				break;
			}
		}
	}
}

