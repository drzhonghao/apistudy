

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.poi.hsmf.datatypes.Chunk;
import org.apache.poi.hsmf.datatypes.Types;
import org.apache.poi.util.IOUtils;


public class ByteChunk extends Chunk {
	private byte[] value;

	public ByteChunk(String namePrefix, int chunkId, Types.MAPIType type) {
		super(namePrefix, chunkId, type);
	}

	public ByteChunk(int chunkId, Types.MAPIType type) {
		super(chunkId, type);
	}

	@Override
	public void readValue(InputStream value) throws IOException {
		this.value = IOUtils.toByteArray(value);
	}

	@Override
	public void writeValue(OutputStream out) throws IOException {
		out.write(value);
	}

	public byte[] getValue() {
		return value;
	}

	public void setValue(byte[] value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return ByteChunk.toDebugFriendlyString(value);
	}

	protected static String toDebugFriendlyString(byte[] value) {
		if (value == null) {
			return "(Null Byte Array)";
		}
		StringBuffer text = new StringBuffer();
		text.append("Bytes len=").append(value.length);
		text.append(" [");
		int limit = Math.min(value.length, 16);
		if ((value.length) > 16) {
			limit = 12;
		}
		for (int i = 0; i < limit; i++) {
			if (i > 0) {
				text.append(',');
			}
			text.append(value[i]);
		}
		if ((value.length) > 16) {
			text.append(",....");
		}
		text.append("]");
		return text.toString();
	}

	public String getAs7bitString() {
		return null;
	}
}

