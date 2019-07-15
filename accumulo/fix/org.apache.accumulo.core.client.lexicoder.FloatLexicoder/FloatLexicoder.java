

import org.apache.accumulo.core.client.lexicoder.AbstractEncoder;
import org.apache.accumulo.core.client.lexicoder.UIntegerLexicoder;
import org.apache.accumulo.core.client.lexicoder.impl.AbstractLexicoder;
import org.apache.accumulo.core.iterators.ValueFormatException;


public class FloatLexicoder extends AbstractLexicoder<Float> {
	private UIntegerLexicoder intEncoder = new UIntegerLexicoder();

	@Override
	public byte[] encode(Float f) {
		int i = Float.floatToRawIntBits(f);
		if (i < 0) {
			i = ~i;
		}else {
			i = i ^ -2147483648;
		}
		return intEncoder.encode(i);
	}

	@Override
	public Float decode(byte[] b) {
		return super.decode(b);
	}

	@Override
	protected Float decodeUnchecked(byte[] b, int offset, int len) throws ValueFormatException {
		return 0f;
	}
}

