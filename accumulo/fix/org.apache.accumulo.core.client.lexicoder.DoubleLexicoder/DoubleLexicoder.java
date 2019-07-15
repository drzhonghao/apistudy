

import org.apache.accumulo.core.client.lexicoder.AbstractEncoder;
import org.apache.accumulo.core.client.lexicoder.ULongLexicoder;
import org.apache.accumulo.core.client.lexicoder.impl.AbstractLexicoder;


public class DoubleLexicoder extends AbstractLexicoder<Double> {
	private ULongLexicoder longEncoder = new ULongLexicoder();

	@Override
	public byte[] encode(Double d) {
		long l = Double.doubleToRawLongBits(d);
		if (l < 0)
			l = ~l;
		else
			l = l ^ -9223372036854775808L;

		return longEncoder.encode(l);
	}

	@Override
	public Double decode(byte[] b) {
		return super.decode(b);
	}

	@Override
	protected Double decodeUnchecked(byte[] data, int offset, int len) {
		return 0d;
	}
}

