

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NativeLibraryWindows {
	private static final Logger logger = LoggerFactory.getLogger(NativeLibraryWindows.class);

	private static boolean available;

	static {
		try {
			Native.register("kernel32");
			NativeLibraryWindows.available = true;
		} catch (NoClassDefFoundError e) {
			NativeLibraryWindows.logger.warn("JNA not found. Native methods will be disabled.");
		} catch (UnsatisfiedLinkError e) {
			NativeLibraryWindows.logger.error("Failed to link the Windows/Kernel32 library against JNA. Native methods will be unavailable.", e);
		} catch (NoSuchMethodError e) {
			NativeLibraryWindows.logger.warn("Obsolete version of JNA present; unable to register Windows/Kernel32 library. Upgrade to JNA 3.2.7 or later");
		}
	}

	private static native long GetCurrentProcessId() throws LastErrorException;

	public int callMlockall(int flags) throws RuntimeException, UnsatisfiedLinkError {
		throw new UnsatisfiedLinkError();
	}

	public int callMunlockall() throws RuntimeException, UnsatisfiedLinkError {
		throw new UnsatisfiedLinkError();
	}

	public int callFcntl(int fd, int command, long flags) throws RuntimeException, UnsatisfiedLinkError {
		throw new UnsatisfiedLinkError();
	}

	public int callPosixFadvise(int fd, long offset, int len, int flag) throws RuntimeException, UnsatisfiedLinkError {
		throw new UnsatisfiedLinkError();
	}

	public int callOpen(String path, int flags) throws RuntimeException, UnsatisfiedLinkError {
		throw new UnsatisfiedLinkError();
	}

	public int callFsync(int fd) throws RuntimeException, UnsatisfiedLinkError {
		throw new UnsatisfiedLinkError();
	}

	public int callClose(int fd) throws RuntimeException, UnsatisfiedLinkError {
		throw new UnsatisfiedLinkError();
	}

	public Pointer callStrerror(int errnum) throws RuntimeException, UnsatisfiedLinkError {
		throw new UnsatisfiedLinkError();
	}

	public long callGetpid() throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryWindows.GetCurrentProcessId();
	}

	public boolean isAvailable() {
		return NativeLibraryWindows.available;
	}
}

