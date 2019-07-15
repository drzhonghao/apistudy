

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.BufferedChecksumIndexInput;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;


public abstract class Directory implements Closeable {
	public abstract String[] listAll() throws IOException;

	public abstract void deleteFile(String name) throws IOException;

	public abstract long fileLength(String name) throws IOException;

	public abstract IndexOutput createOutput(String name, IOContext context) throws IOException;

	public abstract IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException;

	public abstract void sync(Collection<String> names) throws IOException;

	public abstract void rename(String source, String dest) throws IOException;

	public abstract void syncMetaData() throws IOException;

	public abstract IndexInput openInput(String name, IOContext context) throws IOException;

	public ChecksumIndexInput openChecksumInput(String name, IOContext context) throws IOException {
		return new BufferedChecksumIndexInput(openInput(name, context));
	}

	public abstract Lock obtainLock(String name) throws IOException;

	@Override
	public abstract void close() throws IOException;

	@Override
	public String toString() {
		return ((getClass().getSimpleName()) + '@') + (Integer.toHexString(hashCode()));
	}

	public void copyFrom(Directory from, String src, String dest, IOContext context) throws IOException {
		boolean success = false;
		try (IndexInput is = from.openInput(src, context);IndexOutput os = createOutput(dest, context)) {
			os.copyBytes(is, is.length());
			success = true;
		} finally {
			if (!success) {
			}
		}
	}

	protected void ensureOpen() throws AlreadyClosedException {
	}

	public Set<String> getPendingDeletions() throws IOException {
		return Collections.emptySet();
	}
}

