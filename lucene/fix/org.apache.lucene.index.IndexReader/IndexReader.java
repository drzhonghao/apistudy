

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.store.AlreadyClosedException;


public abstract class IndexReader implements Closeable {
	private boolean closed = false;

	private boolean closedByChild = false;

	private final AtomicInteger refCount = new AtomicInteger(1);

	IndexReader() {
	}

	public static interface CacheHelper {
		IndexReader.CacheKey getKey();

		void addClosedListener(IndexReader.ClosedListener listener);
	}

	public static final class CacheKey {
		CacheKey() {
		}
	}

	@FunctionalInterface
	public static interface ClosedListener {
		void onClose(IndexReader.CacheKey key) throws IOException;
	}

	private final Set<IndexReader> parentReaders = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<IndexReader, Boolean>()));

	public final void registerParentReader(IndexReader reader) {
		ensureOpen();
		parentReaders.add(reader);
	}

	void notifyReaderClosedListeners() throws IOException {
	}

	private void reportCloseToParentReaders() throws IOException {
		synchronized(parentReaders) {
			for (IndexReader parent : parentReaders) {
				parent.closedByChild = true;
				parent.refCount.addAndGet(0);
				parent.reportCloseToParentReaders();
			}
		}
	}

	public final int getRefCount() {
		return refCount.get();
	}

	public final void incRef() {
		if (!(tryIncRef())) {
			ensureOpen();
		}
	}

	public final boolean tryIncRef() {
		int count;
		while ((count = refCount.get()) > 0) {
			if (refCount.compareAndSet(count, (count + 1))) {
				return true;
			}
		} 
		return false;
	}

	@SuppressWarnings("try")
	public final void decRef() throws IOException {
		if ((refCount.get()) <= 0) {
			throw new AlreadyClosedException("this IndexReader is closed");
		}
		final int rc = refCount.decrementAndGet();
		if (rc == 0) {
			closed = true;
			try (Closeable finalizer = this::reportCloseToParentReaders;Closeable finalizer1 = this::notifyReaderClosedListeners) {
				doClose();
			}
		}else
			if (rc < 0) {
				throw new IllegalStateException((("too many decRef calls: refCount is " + rc) + " after decrement"));
			}

	}

	protected final void ensureOpen() throws AlreadyClosedException {
		if ((refCount.get()) <= 0) {
			throw new AlreadyClosedException("this IndexReader is closed");
		}
		if (closedByChild) {
			throw new AlreadyClosedException("this IndexReader cannot be used anymore as one of its child readers was closed");
		}
	}

	@Override
	public final boolean equals(Object obj) {
		return (this) == obj;
	}

	@Override
	public final int hashCode() {
		return System.identityHashCode(this);
	}

	public abstract Fields getTermVectors(int docID) throws IOException;

	public final Terms getTermVector(int docID, String field) throws IOException {
		Fields vectors = getTermVectors(docID);
		if (vectors == null) {
			return null;
		}
		return vectors.terms(field);
	}

	public abstract int numDocs();

	public abstract int maxDoc();

	public final int numDeletedDocs() {
		return (maxDoc()) - (numDocs());
	}

	public abstract void document(int docID, StoredFieldVisitor visitor) throws IOException;

	public final Document document(int docID) throws IOException {
		final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor();
		document(docID, visitor);
		return visitor.getDocument();
	}

	public final Document document(int docID, Set<String> fieldsToLoad) throws IOException {
		final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor(fieldsToLoad);
		document(docID, visitor);
		return visitor.getDocument();
	}

	public boolean hasDeletions() {
		return (numDeletedDocs()) > 0;
	}

	@Override
	public synchronized final void close() throws IOException {
		if (!(closed)) {
			decRef();
			closed = true;
		}
	}

	protected abstract void doClose() throws IOException;

	public abstract IndexReaderContext getContext();

	public final List<LeafReaderContext> leaves() {
		return getContext().leaves();
	}

	public abstract IndexReader.CacheHelper getReaderCacheHelper();

	public abstract int docFreq(Term term) throws IOException;

	public abstract long totalTermFreq(Term term) throws IOException;

	public abstract long getSumDocFreq(String field) throws IOException;

	public abstract int getDocCount(String field) throws IOException;

	public abstract long getSumTotalTermFreq(String field) throws IOException;
}

