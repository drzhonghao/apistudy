

import org.apache.karaf.log.core.internal.layout.FormattingInfo;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;


public abstract class PatternConverter {
	public PatternConverter next;

	int min = -1;

	int max = 2147483647;

	boolean leftAlign = false;

	protected PatternConverter() {
	}

	protected PatternConverter(FormattingInfo fi) {
	}

	protected abstract String convert(PaxLoggingEvent event);

	public void format(StringBuffer sbuf, PaxLoggingEvent e) {
		String s = convert(e);
		if (s == null) {
			if (0 < (min))
				spacePad(sbuf, min);

			return;
		}
		int len = s.length();
		if (len > (max))
			sbuf.append(s.substring((len - (max))));
		else
			if (len < (min)) {
				if (leftAlign) {
					sbuf.append(s);
					spacePad(sbuf, ((min) - len));
				}else {
					spacePad(sbuf, ((min) - len));
					sbuf.append(s);
				}
			}else
				sbuf.append(s);


	}

	static String[] SPACES = new String[]{ " ", "  ", "    ", "        ", "                ", "                                " };

	public void spacePad(StringBuffer sbuf, int length) {
		while (length >= 32) {
			sbuf.append(PatternConverter.SPACES[5]);
			length -= 32;
		} 
		for (int i = 4; i >= 0; i--) {
			if ((length & (1 << i)) != 0) {
				sbuf.append(PatternConverter.SPACES[i]);
			}
		}
	}
}

