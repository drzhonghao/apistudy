

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NativeLibraryDarwin {
	private static final Logger logger = LoggerFactory.getLogger(NativeLibraryDarwin.class);

	private static boolean available;

	static {
		try {
			Native.register("c");
			NativeLibraryDarwin.available = true;
		} catch (NoClassDefFoundError e) {
			NativeLibraryDarwin.logger.warn("JNA not found. Native methods will be disabled.");
		} catch (UnsatisfiedLinkError e) {
			NativeLibraryDarwin.logger.error("Failed to link the C library against JNA. Native methods will be unavailable.", e);
		} catch (NoSuchMethodError e) {
			NativeLibraryDarwin.logger.warn("Obsolete version of JNA present; unable to register C library. Upgrade to JNA 3.2.7 or later");
		}
	}

	private static native int mlockall(int flags) throws LastErrorException;

	private static native int munlockall() throws LastErrorException;

	private static native int fcntl(int fd, int command, long flags) throws LastErrorException;

	private static native int open(String path, int flags) throws LastErrorException;

	private static native int fsync(int fd) throws LastErrorException;

	private static native int close(int fd) throws LastErrorException;

	private static native Pointer strerror(int errnum) throws LastErrorException;

	private static native long getpid() throws LastErrorException;

	public int callMlockall(int flags) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryDarwin.mlockall(flags);
	}

	public int callMunlockall() throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryDarwin.munlockall();
	}

	public int callFcntl(int fd, int command, long flags) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryDarwin.fcntl(fd, command, flags);
	}

	public int callPosixFadvise(int fd, long offset, int len, int flag) throws RuntimeException, UnsatisfiedLinkError {
		throw new UnsatisfiedLinkError();
	}

	public int callOpen(String path, int flags) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryDarwin.open(path, flags);
	}

	public int callFsync(int fd) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryDarwin.fsync(fd);
	}

	public int callClose(int fd) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryDarwin.close(fd);
	}

	public Pointer callStrerror(int errnum) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryDarwin.strerror(errnum);
	}

	public long callGetpid() throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryDarwin.getpid();
	}

	public boolean isAvailable() {
		return NativeLibraryDarwin.available;
	}
}

