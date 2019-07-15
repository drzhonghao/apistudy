

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;


public abstract class AbstractLayout {
	protected final String hostName = null;

	protected final String appName = null;

	protected final String procId = null;

	protected abstract void append(String key, Object val) throws IOException;

	private static String hostname() {
		try {
			final InetAddress addr = InetAddress.getLocalHost();
			return addr.getHostName();
		} catch (final UnknownHostException uhe) {
			try {
				final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
				while (interfaces.hasMoreElements()) {
					final NetworkInterface nic = interfaces.nextElement();
					final Enumeration<InetAddress> addresses = nic.getInetAddresses();
					while (addresses.hasMoreElements()) {
						final InetAddress address = addresses.nextElement();
						if (!(address.isLoopbackAddress())) {
							final String hostname = address.getHostName();
							if (hostname != null) {
								return hostname;
							}
						}
					} 
				} 
			} catch (final SocketException se) {
			}
			return "-";
		}
	}

	private static String procId() {
		try {
			return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		} catch (final Exception ex) {
			try {
				return new File("/proc/self").getCanonicalFile().getName();
			} catch (final IOException ignoredUseDefault) {
			}
		}
		return "-";
	}
}

