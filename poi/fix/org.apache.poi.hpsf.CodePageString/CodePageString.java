

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import org.apache.poi.hpsf.Property;
import org.apache.poi.util.CodePageUtil;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianByteArrayInputStream;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


@Internal
public class CodePageString {
	private static final int MAX_RECORD_LENGTH = 100000;

	private static final POILogger LOG = POILogFactory.getLogger(CodePageString.class);

	private byte[] _value;

	public void read(LittleEndianByteArrayInputStream lei) {
		int offset = lei.getReadIndex();
		int size = lei.readInt();
		_value = IOUtils.safelyAllocate(size, CodePageString.MAX_RECORD_LENGTH);
		if (size == 0) {
			return;
		}
		lei.readFully(_value);
		if ((_value[(size - 1)]) != 0) {
			String msg = ("CodePageString started at offset #" + offset) + " is not NULL-terminated";
			CodePageString.LOG.log(POILogger.WARN, msg);
		}
	}

	public String getJavaValue(int codepage) throws UnsupportedEncodingException {
		int cp = (codepage == (-1)) ? Property.DEFAULT_CODEPAGE : codepage;
		String result = CodePageUtil.getStringFromCodePage(_value, cp);
		final int terminator = result.indexOf('\u0000');
		if (terminator == (-1)) {
			String msg = "String terminator (\\0) for CodePageString property value not found." + "Continue without trimming and hope for the best.";
			CodePageString.LOG.log(POILogger.WARN, msg);
			return result;
		}
		if (terminator != ((result.length()) - 1)) {
			String msg = "String terminator (\\0) for CodePageString property value occured before the end of string. " + "Trimming and hope for the best.";
			CodePageString.LOG.log(POILogger.WARN, msg);
		}
		return result.substring(0, terminator);
	}

	public int getSize() {
		return (LittleEndianConsts.INT_SIZE) + (_value.length);
	}

	public void setJavaValue(String string, int codepage) throws UnsupportedEncodingException {
		int cp = (codepage == (-1)) ? Property.DEFAULT_CODEPAGE : codepage;
		_value = CodePageUtil.getBytesInCodePage((string + "\u0000"), cp);
	}

	public int write(OutputStream out) throws IOException {
		LittleEndian.putUInt(_value.length, out);
		out.write(_value);
		return (LittleEndianConsts.INT_SIZE) + (_value.length);
	}
}

