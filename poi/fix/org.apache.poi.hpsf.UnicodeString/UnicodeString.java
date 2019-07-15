

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import org.apache.poi.hpsf.IllegalPropertySetDataException;
import org.apache.poi.util.CodePageUtil;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianByteArrayInputStream;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StringUtil;


@Internal
public class UnicodeString {
	private static final POILogger LOG = POILogFactory.getLogger(UnicodeString.class);

	private static final int MAX_RECORD_LENGTH = 100000;

	private byte[] _value;

	public void read(LittleEndianByteArrayInputStream lei) {
		final int length = lei.readInt();
		final int unicodeBytes = length * 2;
		_value = IOUtils.safelyAllocate(unicodeBytes, UnicodeString.MAX_RECORD_LENGTH);
		if (length == 0) {
			return;
		}
		final int offset = lei.getReadIndex();
		lei.readFully(_value);
		if (((_value[(unicodeBytes - 2)]) != 0) || ((_value[(unicodeBytes - 1)]) != 0)) {
			String msg = ("UnicodeString started at offset #" + offset) + " is not NULL-terminated";
			throw new IllegalPropertySetDataException(msg);
		}
	}

	public byte[] getValue() {
		return _value;
	}

	public String toJavaString() {
		if ((_value.length) == 0) {
			return null;
		}
		String result = StringUtil.getFromUnicodeLE(_value, 0, ((_value.length) >> 1));
		final int terminator = result.indexOf('\u0000');
		if (terminator == (-1)) {
			String msg = "String terminator (\\0) for UnicodeString property value not found." + "Continue without trimming and hope for the best.";
			UnicodeString.LOG.log(POILogger.WARN, msg);
			return result;
		}
		if (terminator != ((result.length()) - 1)) {
			String msg = "String terminator (\\0) for UnicodeString property value occured before the end of string. " + "Trimming and hope for the best.";
			UnicodeString.LOG.log(POILogger.WARN, msg);
		}
		return result.substring(0, terminator);
	}

	public void setJavaValue(String string) throws UnsupportedEncodingException {
		_value = CodePageUtil.getBytesInCodePage((string + "\u0000"), CodePageUtil.CP_UNICODE);
	}

	public int write(OutputStream out) throws IOException {
		LittleEndian.putUInt(((_value.length) / 2), out);
		out.write(_value);
		return (LittleEndianConsts.INT_SIZE) + (_value.length);
	}
}

