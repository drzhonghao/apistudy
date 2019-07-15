

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NativeLibraryLinux {
	private static boolean available;

	private static final Logger logger = LoggerFactory.getLogger(NativeLibraryLinux.class);

	static {
		try {
			Native.register("c");
			NativeLibraryLinux.available = true;
		} catch (NoClassDefFoundError e) {
			NativeLibraryLinux.logger.warn("JNA not found. Native methods will be disabled.");
		} catch (UnsatisfiedLinkError e) {
			NativeLibraryLinux.logger.error("Failed to link the C library against JNA. Native methods will be unavailable.", e);
		} catch (NoSuchMethodError e) {
			NativeLibraryLinux.logger.warn("Obsolete version of JNA present; unable to register C library. Upgrade to JNA 3.2.7 or later");
		}
	}

	private static native int mlockall(int flags) throws LastErrorException;

	private static native int munlockall() throws LastErrorException;

	private static native int fcntl(int fd, int command, long flags) throws LastErrorException;

	private static native int posix_fadvise(int fd, long offset, int len, int flag) throws LastErrorException;

	private static native int open(String path, int flags) throws LastErrorException;

	private static native int fsync(int fd) throws LastErrorException;

	private static native int close(int fd) throws LastErrorException;

	private static native Pointer strerror(int errnum) throws LastErrorException;

	private static native long getpid() throws LastErrorException;

	public int callMlockall(int flags) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryLinux.mlockall(flags);
	}

	public int callMunlockall() throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryLinux.munlockall();
	}

	public int callFcntl(int fd, int command, long flags) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryLinux.fcntl(fd, command, flags);
	}

	public int callPosixFadvise(int fd, long offset, int len, int flag) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryLinux.posix_fadvise(fd, offset, len, flag);
	}

	public int callOpen(String path, int flags) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryLinux.open(path, flags);
	}

	public int callFsync(int fd) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryLinux.fsync(fd);
	}

	public int callClose(int fd) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryLinux.close(fd);
	}

	public Pointer callStrerror(int errnum) throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryLinux.strerror(errnum);
	}

	public long callGetpid() throws RuntimeException, UnsatisfiedLinkError {
		return NativeLibraryLinux.getpid();
	}

	public boolean isAvailable() {
		return NativeLibraryLinux.available;
	}
}

