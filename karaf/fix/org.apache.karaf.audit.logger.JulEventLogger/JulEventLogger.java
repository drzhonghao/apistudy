

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JulEventLogger {
	private final String logger = null;

	private final Level level = null;

	protected Logger getLogger(String t) {
		return Logger.getLogger((((this.logger) + ".") + t));
	}

	public void flush() throws IOException {
	}

	public void close() throws IOException {
	}
}

