

import java.io.IOException;


public class GelfLayout {
	public GelfLayout() {
	}

	private void datetime(long timestamp) throws IOException {
		long secs = timestamp / 1000;
		int ms = ((int) (timestamp - (secs * 1000)));
		int temp = ms / 100;
		ms -= 100 * temp;
		temp = ms / 10;
		ms -= 10 * temp;
	}

	protected void append(String key, Object val) throws IOException {
		append(key, val, true);
	}

	protected void append(String key, Object val, boolean custom) throws IOException {
		if (val != null) {
			if (custom) {
			}
			if (val instanceof Number) {
				if (val instanceof Long) {
				}else
					if (val instanceof Integer) {
					}else {
					}

			}else {
			}
		}
	}
}

