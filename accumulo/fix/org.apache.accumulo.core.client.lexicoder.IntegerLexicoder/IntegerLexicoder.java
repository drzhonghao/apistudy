

import org.apache.accumulo.core.client.lexicoder.AbstractEncoder;
import org.apache.accumulo.core.client.lexicoder.UIntegerLexicoder;
import org.apache.accumulo.core.client.lexicoder.impl.AbstractLexicoder;


public class IntegerLexicoder extends AbstractLexicoder<Integer> {
	private UIntegerLexicoder uil = new UIntegerLexicoder();

	@Override
	public byte[] encode(Integer i) {
		return uil.encode((i ^ -2147483648));
	}

	@Override
	public Integer decode(byte[] b) {
		return super.decode(b);
	}

	@Override
	protected Integer decodeUnchecked(byte[] data, int offset, int len) {
		return 0;
	}
}

