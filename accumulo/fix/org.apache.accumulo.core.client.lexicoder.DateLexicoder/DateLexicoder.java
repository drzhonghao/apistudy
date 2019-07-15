

import java.util.Date;
import org.apache.accumulo.core.client.lexicoder.AbstractEncoder;
import org.apache.accumulo.core.client.lexicoder.LongLexicoder;
import org.apache.accumulo.core.client.lexicoder.impl.AbstractLexicoder;


public class DateLexicoder extends AbstractLexicoder<Date> {
	private LongLexicoder longEncoder = new LongLexicoder();

	@Override
	public byte[] encode(Date data) {
		return longEncoder.encode(data.getTime());
	}

	@Override
	public Date decode(byte[] b) {
		return super.decode(b);
	}

	@Override
	protected Date decodeUnchecked(byte[] data, int offset, int len) {
		return null;
	}
}

