

import java.io.IOException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;


public abstract class FSLockFactory extends LockFactory {
	public static final FSLockFactory getDefault() {
		return null;
	}

	@Override
	public final Lock obtainLock(Directory dir, String lockName) throws IOException {
		if (!(dir instanceof FSDirectory)) {
			throw new UnsupportedOperationException((((getClass().getSimpleName()) + " can only be used with FSDirectory subclasses, got: ") + dir));
		}
		return obtainFSLock(((FSDirectory) (dir)), lockName);
	}

	protected abstract Lock obtainFSLock(FSDirectory dir, String lockName) throws IOException;
}

