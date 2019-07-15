

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.karaf.main.ConfigProperties;


public class Status {
	public static void main(String[] args) throws Exception {
		ConfigProperties config = new ConfigProperties();
	}

	private static int getPortFromShutdownPortFile(String portFile) throws IOException {
		int port;
		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(portFile)));
		String portStr = r.readLine();
		port = Integer.parseInt(portStr);
		r.close();
		return port;
	}

	private static int getPidFromPidFile(String pidFile) throws IOException {
		int pid;
		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(pidFile)));
		String pidString = r.readLine();
		pid = Integer.parseInt(pidString);
		r.close();
		return pid;
	}
}

