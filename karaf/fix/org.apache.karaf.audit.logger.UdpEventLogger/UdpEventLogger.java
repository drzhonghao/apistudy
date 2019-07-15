

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;


public class UdpEventLogger {
	private final InetAddress host = null;

	private final int port = 0;

	private final CharsetEncoder encoder = null;

	private final DatagramSocket dgram = null;

	private ByteBuffer bb = ByteBuffer.allocate(1024);

	public void flush() throws IOException {
	}

	public void close() throws IOException {
		dgram.close();
	}
}

