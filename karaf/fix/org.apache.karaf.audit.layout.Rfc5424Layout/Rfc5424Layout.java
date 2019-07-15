

import java.io.IOException;
import java.util.TimeZone;


public class Rfc5424Layout {
	public static final int DEFAULT_ENTERPRISE_NUMBER = 18060;

	protected final int facility;

	protected final int priority;

	protected final int enterpriseNumber;

	protected String hdr1;

	protected String hdr2;

	protected String hdr3;

	public Rfc5424Layout(int facility, int priority, int enterpriseNumber, TimeZone timeZone) {
		this.facility = facility;
		this.priority = priority;
		this.enterpriseNumber = enterpriseNumber;
		hdr1 = ("<" + ((facility << 3) + priority)) + ">1 ";
		hdr3 = (enterpriseNumber > 0) ? "@" + enterpriseNumber : "";
	}

	protected void append(String key, Object val) throws IOException {
		if (val != null) {
		}
	}
}

