

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.lucene.index.CompositeReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.index.Term;


public abstract class BaseCompositeReader<R extends IndexReader> extends CompositeReader {
	private final R[] subReaders;

	private final int[] starts;

	private final int maxDoc;

	private final int numDocs;

	private final List<R> subReadersList;

	protected BaseCompositeReader(R[] subReaders) throws IOException {
		this.subReaders = subReaders;
		this.subReadersList = Collections.unmodifiableList(Arrays.asList(subReaders));
		starts = new int[(subReaders.length) + 1];
		long maxDoc = 0;
		long numDocs = 0;
		for (int i = 0; i < (subReaders.length); i++) {
			starts[i] = ((int) (maxDoc));
			final IndexReader r = subReaders[i];
			maxDoc += r.maxDoc();
			numDocs += r.numDocs();
			r.registerParentReader(this);
		}
		this.maxDoc = Math.toIntExact(maxDoc);
		starts[subReaders.length] = this.maxDoc;
		this.numDocs = Math.toIntExact(numDocs);
	}

	@Override
	public final Fields getTermVectors(int docID) throws IOException {
		ensureOpen();
		final int i = readerIndex(docID);
		return subReaders[i].getTermVectors((docID - (starts[i])));
	}

	@Override
	public final int numDocs() {
		return numDocs;
	}

	@Override
	public final int maxDoc() {
		return maxDoc;
	}

	@Override
	public final void document(int docID, StoredFieldVisitor visitor) throws IOException {
		ensureOpen();
		final int i = readerIndex(docID);
		subReaders[i].document((docID - (starts[i])), visitor);
	}

	@Override
	public final int docFreq(Term term) throws IOException {
		ensureOpen();
		int total = 0;
		for (int i = 0; i < (subReaders.length); i++) {
			total += subReaders[i].docFreq(term);
		}
		return total;
	}

	@Override
	public final long totalTermFreq(Term term) throws IOException {
		ensureOpen();
		long total = 0;
		for (int i = 0; i < (subReaders.length); i++) {
			long sub = subReaders[i].totalTermFreq(term);
			if (sub == (-1)) {
				return -1;
			}
			total += sub;
		}
		return total;
	}

	@Override
	public final long getSumDocFreq(String field) throws IOException {
		ensureOpen();
		long total = 0;
		for (R reader : subReaders) {
			long sub = reader.getSumDocFreq(field);
			if (sub == (-1)) {
				return -1;
			}
			total += sub;
		}
		return total;
	}

	@Override
	public final int getDocCount(String field) throws IOException {
		ensureOpen();
		int total = 0;
		for (R reader : subReaders) {
			int sub = reader.getDocCount(field);
			if (sub == (-1)) {
				return -1;
			}
			total += sub;
		}
		return total;
	}

	@Override
	public final long getSumTotalTermFreq(String field) throws IOException {
		ensureOpen();
		long total = 0;
		for (R reader : subReaders) {
			long sub = reader.getSumTotalTermFreq(field);
			if (sub == (-1)) {
				return -1;
			}
			total += sub;
		}
		return total;
	}

	protected final int readerIndex(int docID) {
		if ((docID < 0) || (docID >= (maxDoc))) {
			throw new IllegalArgumentException((((("docID must be >= 0 and < maxDoc=" + (maxDoc)) + " (got docID=") + docID) + ")"));
		}
		return ReaderUtil.subIndex(docID, this.starts);
	}

	protected final int readerBase(int readerIndex) {
		if ((readerIndex < 0) || (readerIndex >= (subReaders.length))) {
			throw new IllegalArgumentException("readerIndex must be >= 0 and < getSequentialSubReaders().size()");
		}
		return this.starts[readerIndex];
	}

	@Override
	protected final List<? extends R> getSequentialSubReaders() {
		return subReadersList;
	}
}

