

import com.sun.jna.LastErrorException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.FileChannelImpl;


public final class NativeLibrary {
	private static final Logger logger = LoggerFactory.getLogger(NativeLibrary.class);

	public enum OSType {

		LINUX,
		MAC,
		WINDOWS,
		AIX,
		OTHER;}

	private static final NativeLibrary.OSType osType;

	private static int MCL_CURRENT = 0;

	private static int MCL_FUTURE = 0;

	private static final int ENOMEM = 12;

	private static final int F_GETFL = 3;

	private static final int F_SETFL = 4;

	private static final int F_NOCACHE = 48;

	private static final int O_DIRECT = 16384;

	private static final int O_RDONLY = 0;

	private static final int POSIX_FADV_NORMAL = 0;

	private static final int POSIX_FADV_RANDOM = 1;

	private static final int POSIX_FADV_SEQUENTIAL = 2;

	private static final int POSIX_FADV_WILLNEED = 3;

	private static final int POSIX_FADV_DONTNEED = 4;

	private static final int POSIX_FADV_NOREUSE = 5;

	private static boolean jnaLockable = false;

	private static final Field FILE_DESCRIPTOR_FD_FIELD;

	private static final Field FILE_CHANNEL_FD_FIELD;

	static {
		FILE_DESCRIPTOR_FD_FIELD = FBUtilities.getProtectedField(FileDescriptor.class, "fd");
		FILE_CHANNEL_FD_FIELD = FBUtilities.getProtectedField(FileChannelImpl.class, "fd");
		osType = NativeLibrary.getOsType();
		if (System.getProperty("os.arch").toLowerCase().contains("ppc")) {
		}else {
			NativeLibrary.MCL_CURRENT = 1;
			NativeLibrary.MCL_FUTURE = 2;
		}
	}

	private NativeLibrary() {
	}

	private static NativeLibrary.OSType getOsType() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("mac")) {
		}else
			if (osName.contains("windows")) {
			}else
				if (osName.contains("aix")) {
				}else {
				}


		return null;
	}

	private static int errno(RuntimeException e) {
		assert e instanceof LastErrorException;
		try {
			return ((LastErrorException) (e)).getErrorCode();
		} catch (NoSuchMethodError x) {
			NativeLibrary.logger.warn("Obsolete version of JNA present; unable to read errno. Upgrade to JNA 3.2.7 or later");
			return 0;
		}
	}

	public static boolean isAvailable() {
		return false;
	}

	public static boolean jnaMemoryLockable() {
		return NativeLibrary.jnaLockable;
	}

	public static void tryMlockall() {
		try {
			NativeLibrary.jnaLockable = true;
			NativeLibrary.logger.info("JNA mlockall successful");
		} catch (UnsatisfiedLinkError e) {
		} catch (RuntimeException e) {
			if (!(e instanceof LastErrorException))
				throw e;

		}
	}

	public static void trySkipCache(String path, long offset, long len) {
		File f = new File(path);
		if (!(f.exists()))
			return;

		try (final FileInputStream fis = new FileInputStream(f)) {
			NativeLibrary.trySkipCache(NativeLibrary.getfd(fis.getChannel()), offset, len, path);
		} catch (IOException e) {
			NativeLibrary.logger.warn("Could not skip cache", e);
		}
	}

	public static void trySkipCache(int fd, long offset, long len, String path) {
		if (len == 0)
			NativeLibrary.trySkipCache(fd, 0, 0, path);

		while (len > 0) {
			int sublen = ((int) (Math.min(Integer.MAX_VALUE, len)));
			NativeLibrary.trySkipCache(fd, offset, sublen, path);
			len -= sublen;
			offset -= sublen;
		} 
	}

	public static void trySkipCache(int fd, long offset, int len, String path) {
		if (fd < 0)
			return;

		try {
		} catch (UnsatisfiedLinkError e) {
		} catch (RuntimeException e) {
			if (!(e instanceof LastErrorException))
				throw e;

			NativeLibrary.logger.warn("posix_fadvise({}, {}) failed, errno ({}).", fd, offset, NativeLibrary.errno(e));
		}
	}

	public static int tryFcntl(int fd, int command, int flags) {
		int result = -1;
		try {
		} catch (UnsatisfiedLinkError e) {
		} catch (RuntimeException e) {
			if (!(e instanceof LastErrorException))
				throw e;

			NativeLibrary.logger.warn("fcntl({}, {}, {}) failed, errno ({}).", fd, command, flags, NativeLibrary.errno(e));
		}
		return result;
	}

	public static int tryOpenDirectory(String path) {
		int fd = -1;
		try {
		} catch (UnsatisfiedLinkError e) {
		} catch (RuntimeException e) {
			if (!(e instanceof LastErrorException))
				throw e;

			NativeLibrary.logger.warn("open({}, O_RDONLY) failed, errno ({}).", path, NativeLibrary.errno(e));
		}
		return fd;
	}

	public static void trySync(int fd) {
		if (fd == (-1))
			return;

		try {
		} catch (UnsatisfiedLinkError e) {
		} catch (RuntimeException e) {
			if (!(e instanceof LastErrorException))
				throw e;

			NativeLibrary.logger.warn("fsync({}) failed, errorno ({}) {}", fd, NativeLibrary.errno(e), e);
		}
	}

	public static void tryCloseFD(int fd) {
		if (fd == (-1))
			return;

		try {
		} catch (UnsatisfiedLinkError e) {
		} catch (RuntimeException e) {
			if (!(e instanceof LastErrorException))
				throw e;

			NativeLibrary.logger.warn("close({}) failed, errno ({}).", fd, NativeLibrary.errno(e));
		}
	}

	public static int getfd(FileChannel channel) {
		try {
			return NativeLibrary.getfd(((FileDescriptor) (NativeLibrary.FILE_CHANNEL_FD_FIELD.get(channel))));
		} catch (IllegalArgumentException | IllegalAccessException e) {
			NativeLibrary.logger.warn("Unable to read fd field from FileChannel");
		}
		return -1;
	}

	public static int getfd(FileDescriptor descriptor) {
		try {
			return NativeLibrary.FILE_DESCRIPTOR_FD_FIELD.getInt(descriptor);
		} catch (Exception e) {
			JVMStabilityInspector.inspectThrowable(e);
			NativeLibrary.logger.warn("Unable to read fd field from FileDescriptor");
		}
		return -1;
	}

	public static long getProcessID() {
		try {
		} catch (Exception e) {
			NativeLibrary.logger.info("Failed to get PID from JNA", e);
		}
		return -1;
	}
}

