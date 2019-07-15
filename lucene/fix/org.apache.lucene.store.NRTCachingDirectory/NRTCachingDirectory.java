

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.store.FlushInfo;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.MergeInfo;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.IOUtils;


public class NRTCachingDirectory extends FilterDirectory implements Accountable {
	private final RAMDirectory cache = new RAMDirectory();

	private final long maxMergeSizeBytes;

	private final long maxCachedBytes;

	private static final boolean VERBOSE = false;

	public NRTCachingDirectory(Directory delegate, double maxMergeSizeMB, double maxCachedMB) {
		super(delegate);
		maxMergeSizeBytes = ((long) ((maxMergeSizeMB * 1024) * 1024));
		maxCachedBytes = ((long) ((maxCachedMB * 1024) * 1024));
	}

	@Override
	public String toString() {
		return ((((("NRTCachingDirectory(" + (in)) + "; maxCacheMB=") + (((maxCachedBytes) / 1024) / 1024.0)) + " maxMergeSizeMB=") + (((maxMergeSizeBytes) / 1024) / 1024.0)) + ")";
	}

	@Override
	public synchronized String[] listAll() throws IOException {
		final Set<String> files = new HashSet<>();
		for (String f : cache.listAll()) {
			files.add(f);
		}
		for (String f : in.listAll()) {
			files.add(f);
		}
		String[] result = files.toArray(new String[files.size()]);
		Arrays.sort(result);
		return result;
	}

	@Override
	public synchronized void deleteFile(String name) throws IOException {
		if (NRTCachingDirectory.VERBOSE) {
			System.out.println(("nrtdir.deleteFile name=" + name));
		}
		if (cache.fileNameExists(name)) {
			cache.deleteFile(name);
		}else {
			in.deleteFile(name);
		}
	}

	@Override
	public synchronized long fileLength(String name) throws IOException {
		if (cache.fileNameExists(name)) {
			return cache.fileLength(name);
		}else {
			return in.fileLength(name);
		}
	}

	public String[] listCachedFiles() {
		return cache.listAll();
	}

	@Override
	public IndexOutput createOutput(String name, IOContext context) throws IOException {
		if (NRTCachingDirectory.VERBOSE) {
			System.out.println(("nrtdir.createOutput name=" + name));
		}
		if (doCacheWrite(name, context)) {
			if (NRTCachingDirectory.VERBOSE) {
				System.out.println("  to cache");
			}
			return cache.createOutput(name, context);
		}else {
			return in.createOutput(name, context);
		}
	}

	@Override
	public void sync(Collection<String> fileNames) throws IOException {
		if (NRTCachingDirectory.VERBOSE) {
			System.out.println(("nrtdir.sync files=" + fileNames));
		}
		for (String fileName : fileNames) {
			unCache(fileName);
		}
		in.sync(fileNames);
	}

	@Override
	public void rename(String source, String dest) throws IOException {
		unCache(source);
		if (cache.fileNameExists(dest)) {
			throw new IllegalArgumentException((("target file " + dest) + " already exists"));
		}
		in.rename(source, dest);
	}

	@Override
	public synchronized IndexInput openInput(String name, IOContext context) throws IOException {
		if (NRTCachingDirectory.VERBOSE) {
			System.out.println(("nrtdir.openInput name=" + name));
		}
		if (cache.fileNameExists(name)) {
			if (NRTCachingDirectory.VERBOSE) {
				System.out.println("  from cache");
			}
			return cache.openInput(name, context);
		}else {
			return in.openInput(name, context);
		}
	}

	@Override
	public void close() throws IOException {
		boolean success = false;
		try {
			success = true;
		} finally {
			if (success) {
				IOUtils.close(cache, in);
			}else {
				IOUtils.closeWhileHandlingException(cache, in);
			}
		}
	}

	protected boolean doCacheWrite(String name, IOContext context) {
		long bytes = 0;
		if ((context.mergeInfo) != null) {
			bytes = context.mergeInfo.estimatedMergeBytes;
		}else
			if ((context.flushInfo) != null) {
				bytes = context.flushInfo.estimatedSegmentSize;
			}

		return (bytes <= (maxMergeSizeBytes)) && ((bytes + (cache.ramBytesUsed())) <= (maxCachedBytes));
	}

	@Override
	public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
		if (NRTCachingDirectory.VERBOSE) {
			System.out.println(((("nrtdir.createTempOutput prefix=" + prefix) + " suffix=") + suffix));
		}
		Set<String> toDelete = new HashSet<>();
		boolean success = false;
		Directory first;
		Directory second;
		if (doCacheWrite(prefix, context)) {
			first = cache;
			second = in;
		}else {
			first = in;
			second = cache;
		}
		IndexOutput out = null;
		try {
			while (true) {
				out = first.createTempOutput(prefix, suffix, context);
				String name = out.getName();
				toDelete.add(name);
				if (NRTCachingDirectory.slowFileExists(second, name)) {
					out.close();
				}else {
					toDelete.remove(name);
					success = true;
					break;
				}
			} 
		} finally {
			if (success) {
				IOUtils.deleteFiles(first, toDelete);
			}else {
				IOUtils.closeWhileHandlingException(out);
				IOUtils.deleteFilesIgnoringExceptions(first, toDelete);
			}
		}
		return out;
	}

	static boolean slowFileExists(Directory dir, String fileName) throws IOException {
		try {
			dir.openInput(fileName, IOContext.DEFAULT).close();
			return true;
		} catch (NoSuchFileException | FileNotFoundException e) {
			return false;
		}
	}

	private final Object uncacheLock = new Object();

	private void unCache(String fileName) throws IOException {
		synchronized(uncacheLock) {
			if (NRTCachingDirectory.VERBOSE) {
				System.out.println(("nrtdir.unCache name=" + fileName));
			}
			if (!(cache.fileNameExists(fileName))) {
				return;
			}
			assert (NRTCachingDirectory.slowFileExists(in, fileName)) == false : ("fileName=" + fileName) + " exists both in cache and in delegate";
			final IOContext context = IOContext.DEFAULT;
			final IndexOutput out = in.createOutput(fileName, context);
			IndexInput in = null;
			try {
				in = cache.openInput(fileName, context);
				out.copyBytes(in, in.length());
			} finally {
				IOUtils.close(in, out);
			}
			synchronized(this) {
				cache.deleteFile(fileName);
			}
		}
	}

	@Override
	public long ramBytesUsed() {
		return cache.ramBytesUsed();
	}

	@Override
	public Collection<Accountable> getChildResources() {
		return Collections.singleton(Accountables.namedAccountable("cache", cache));
	}
}

