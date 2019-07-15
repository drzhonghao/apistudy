

import java.io.PrintStream;
import org.apache.karaf.main.ShutdownCallback;


public class Main extends Thread implements ShutdownCallback {
	private org.apache.karaf.main.Main main;

	private volatile boolean destroying;

	private Main() {
	}

	public Integer start(String[] args) {
		main = new org.apache.karaf.main.Main(args);
		try {
			main.launch();
			main.setShutdownCallback(this);
			start();
			return null;
		} catch (Throwable ex) {
			System.err.println(("Could not create framework: " + ex));
			ex.printStackTrace();
			return -1;
		}
	}

	public void run() {
		try {
			main.awaitShutdown();
			if (!(destroying)) {
			}
		} catch (Exception e) {
		}
	}

	public int stop(int exitCode) {
		try {
			destroying = true;
			if (!(main.destroy())) {
				System.err.println("Timeout waiting for Karaf to shutdown");
				return -3;
			}
		} catch (Throwable ex) {
			System.err.println(("Error occured shutting down framework: " + ex));
			ex.printStackTrace();
			return -2;
		}
		return main.getExitCode();
	}

	public void waitingForShutdown(int delay) {
	}

	public void controlEvent(int event) {
	}

	public static void main(String[] args) {
	}
}

