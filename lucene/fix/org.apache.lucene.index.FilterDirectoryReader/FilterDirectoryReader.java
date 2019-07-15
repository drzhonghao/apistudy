

import java.io.IOException;
import java.util.List;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;


public abstract class FilterDirectoryReader extends DirectoryReader {
	public static DirectoryReader unwrap(DirectoryReader reader) {
		while (reader instanceof FilterDirectoryReader) {
			reader = ((FilterDirectoryReader) (reader)).in;
		} 
		return reader;
	}

	public abstract static class SubReaderWrapper {
		private LeafReader[] wrap(List<? extends LeafReader> readers) {
			LeafReader[] wrapped = new LeafReader[readers.size()];
			for (int i = 0; i < (readers.size()); i++) {
				wrapped[i] = wrap(readers.get(i));
			}
			return wrapped;
		}

		public SubReaderWrapper() {
		}

		public abstract LeafReader wrap(LeafReader reader);
	}

	protected final DirectoryReader in;

	protected abstract DirectoryReader doWrapDirectoryReader(DirectoryReader in) throws IOException;

	private final DirectoryReader wrapDirectoryReader(DirectoryReader in) throws IOException {
		return in == null ? null : doWrapDirectoryReader(in);
	}

	@Override
	protected final DirectoryReader doOpenIfChanged() throws IOException {
		return null;
	}

	@Override
	protected final DirectoryReader doOpenIfChanged(IndexCommit commit) throws IOException {
		return null;
	}

	@Override
	protected final DirectoryReader doOpenIfChanged(IndexWriter writer, boolean applyAllDeletes) throws IOException {
		return null;
	}

	@Override
	public long getVersion() {
		return in.getVersion();
	}

	@Override
	public boolean isCurrent() throws IOException {
		return in.isCurrent();
	}

	@Override
	public IndexCommit getIndexCommit() throws IOException {
		return in.getIndexCommit();
	}

	@Override
	protected void doClose() throws IOException {
		in.close();
	}

	public DirectoryReader getDelegate() {
		return in;
	}
}

