

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.Objects;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.util.Constants;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;


public class MMapDirectory extends FSDirectory {
	private boolean useUnmapHack = MMapDirectory.UNMAP_SUPPORTED;

	private boolean preload;

	public static final int DEFAULT_MAX_CHUNK_SIZE = (Constants.JRE_IS_64BIT) ? 1 << 30 : 1 << 28;

	final int chunkSizePower;

	public MMapDirectory(Path path, LockFactory lockFactory) throws IOException {
		this(path, lockFactory, MMapDirectory.DEFAULT_MAX_CHUNK_SIZE);
	}

	public MMapDirectory(Path path) throws IOException {
		this(path, FSLockFactory.getDefault());
	}

	public MMapDirectory(Path path, int maxChunkSize) throws IOException {
		this(path, FSLockFactory.getDefault(), maxChunkSize);
	}

	public MMapDirectory(Path path, LockFactory lockFactory, int maxChunkSize) throws IOException {
		super(path, lockFactory);
		if (maxChunkSize <= 0) {
			throw new IllegalArgumentException("Maximum chunk size for mmap must be >0");
		}
		this.chunkSizePower = 31 - (Integer.numberOfLeadingZeros(maxChunkSize));
		assert ((this.chunkSizePower) >= 0) && ((this.chunkSizePower) <= 30);
	}

	public void setUseUnmap(final boolean useUnmapHack) {
		if (useUnmapHack && (!(MMapDirectory.UNMAP_SUPPORTED))) {
			throw new IllegalArgumentException(MMapDirectory.UNMAP_NOT_SUPPORTED_REASON);
		}
		this.useUnmapHack = useUnmapHack;
	}

	public boolean getUseUnmap() {
		return useUnmapHack;
	}

	public void setPreload(boolean preload) {
		this.preload = preload;
	}

	public boolean getPreload() {
		return preload;
	}

	public final int getMaxChunkSize() {
		return 1 << (chunkSizePower);
	}

	final ByteBuffer[] map(String resourceDescription, FileChannel fc, long offset, long length) throws IOException {
		if ((length >>> (chunkSizePower)) >= (Integer.MAX_VALUE))
			throw new IllegalArgumentException(("RandomAccessFile too big for chunk size: " + resourceDescription));

		final long chunkSize = 1L << (chunkSizePower);
		final int nrBuffers = ((int) (length >>> (chunkSizePower))) + 1;
		ByteBuffer[] buffers = new ByteBuffer[nrBuffers];
		long bufferStart = 0L;
		for (int bufNr = 0; bufNr < nrBuffers; bufNr++) {
			int bufSize = ((int) ((length > (bufferStart + chunkSize)) ? chunkSize : length - bufferStart));
			MappedByteBuffer buffer;
			try {
				buffer = fc.map(READ_ONLY, (offset + bufferStart), bufSize);
			} catch (IOException ioe) {
				throw convertMapFailedIOException(ioe, resourceDescription, bufSize);
			}
			if (preload) {
				buffer.load();
			}
			buffers[bufNr] = buffer;
			bufferStart += bufSize;
		}
		return buffers;
	}

	private IOException convertMapFailedIOException(IOException ioe, String resourceDescription, int bufSize) {
		final String originalMessage;
		final Throwable originalCause;
		if ((ioe.getCause()) instanceof OutOfMemoryError) {
			originalMessage = "Map failed";
			originalCause = null;
		}else {
			originalMessage = ioe.getMessage();
			originalCause = ioe.getCause();
		}
		final String moreInfo;
		if (!(Constants.JRE_IS_64BIT)) {
			moreInfo = "MMapDirectory should only be used on 64bit platforms, because the address space on 32bit operating systems is too small. ";
		}else
			if (Constants.WINDOWS) {
				moreInfo = "Windows is unfortunately very limited on virtual address space. If your index size is several hundred Gigabytes, consider changing to Linux. ";
			}else
				if (Constants.LINUX) {
					moreInfo = "Please review 'ulimit -v', 'ulimit -m' (both should return 'unlimited'), and 'sysctl vm.max_map_count'. ";
				}else {
					moreInfo = "Please review 'ulimit -v', 'ulimit -m' (both should return 'unlimited'). ";
				}


		final IOException newIoe = new IOException(String.format(Locale.ENGLISH, ("%s: %s [this may be caused by lack of enough unfragmented virtual address space " + (("or too restrictive virtual memory limits enforced by the operating system, " + "preventing us to map a chunk of %d bytes. %sMore information: ") + "http://blog.thetaphi.de/2012/07/use-lucenes-mmapdirectory-on-64bit.html]")), originalMessage, resourceDescription, bufSize, moreInfo), originalCause);
		newIoe.setStackTrace(ioe.getStackTrace());
		return newIoe;
	}

	public static final boolean UNMAP_SUPPORTED = false;

	public static final String UNMAP_NOT_SUPPORTED_REASON = null;

	static {
		final Object hack = AccessController.doPrivileged(((PrivilegedAction<Object>) (MMapDirectory::unmapHackImpl)));
	}

	@org.apache.lucene.util.SuppressForbidden(reason = "Needs access to private APIs in DirectBuffer, sun.misc.Cleaner, and sun.misc.Unsafe to enable hack")
	private static Object unmapHackImpl() {
		final MethodHandles.Lookup lookup = MethodHandles.lookup();
		try {
			try {
				final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
				final MethodHandle unmapper = lookup.findVirtual(unsafeClass, "invokeCleaner", MethodType.methodType(void.class, ByteBuffer.class));
				final Field f = unsafeClass.getDeclaredField("theUnsafe");
				f.setAccessible(true);
				final Object theUnsafe = f.get(null);
			} catch (SecurityException se) {
				throw se;
			} catch (ReflectiveOperationException | RuntimeException e) {
				final Class<?> directBufferClass = Class.forName("java.nio.DirectByteBuffer");
				final Method m = directBufferClass.getMethod("cleaner");
				m.setAccessible(true);
				final MethodHandle directBufferCleanerMethod = lookup.unreflect(m);
				final Class<?> cleanerClass = directBufferCleanerMethod.type().returnType();
				final MethodHandle cleanMethod = lookup.findVirtual(cleanerClass, "clean", MethodType.methodType(void.class));
				final MethodHandle nonNullTest = lookup.findStatic(Objects.class, "nonNull", MethodType.methodType(boolean.class, Object.class)).asType(MethodType.methodType(boolean.class, cleanerClass));
				final MethodHandle noop = MethodHandles.dropArguments(MethodHandles.constant(Void.class, null).asType(MethodType.methodType(void.class)), 0, cleanerClass);
				final MethodHandle unmapper = MethodHandles.filterReturnValue(directBufferCleanerMethod, MethodHandles.guardWithTest(nonNullTest, cleanMethod, noop)).asType(MethodType.methodType(void.class, ByteBuffer.class));
			}
		} catch (SecurityException se) {
			return (("Unmapping is not supported, because not all required permissions are given to the Lucene JAR file: " + se) + " [Please grant at least the following permissions: RuntimePermission(\"accessClassInPackage.sun.misc\") ") + " and ReflectPermission(\"suppressAccessChecks\")]";
		} catch (ReflectiveOperationException | RuntimeException e) {
			return "Unmapping is not supported on this platform, because internal Java APIs are not compatible with this Lucene version: " + e;
		}
		return null;
	}
}

