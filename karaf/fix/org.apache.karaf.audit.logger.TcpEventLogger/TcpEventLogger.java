

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;


public class TcpEventLogger {
	private final String host = null;

	private final int port = 0;

	private final Charset encoding = null;

	private BufferedWriter writer;

	public void close() throws IOException {
		if ((writer) != null) {
			writer.close();
		}
	}

	public void flush() throws IOException {
		if ((writer) != null) {
			writer.flush();
		}
	}
}

