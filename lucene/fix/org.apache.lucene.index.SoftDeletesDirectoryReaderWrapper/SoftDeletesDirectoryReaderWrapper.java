

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.lucene.index.BaseCompositeReader;
import org.apache.lucene.index.CodecReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FilterCodecReader;
import org.apache.lucene.index.FilterDirectoryReader;
import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.CacheKey;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;


public final class SoftDeletesDirectoryReaderWrapper extends FilterDirectoryReader {
	private final String field;

	private final IndexReader.CacheHelper readerCacheHelper;

	public SoftDeletesDirectoryReaderWrapper(DirectoryReader in, String field) throws IOException {
		this(in, new SoftDeletesDirectoryReaderWrapper.SoftDeletesSubReaderWrapper(Collections.emptyMap(), field));
	}

	private SoftDeletesDirectoryReaderWrapper(DirectoryReader in, SoftDeletesDirectoryReaderWrapper.SoftDeletesSubReaderWrapper wrapper) throws IOException {
		super(in, wrapper);
		this.field = wrapper.field;
		readerCacheHelper = ((in.getReaderCacheHelper()) == null) ? null : new SoftDeletesDirectoryReaderWrapper.DelegatingCacheHelper(in.getReaderCacheHelper());
	}

	@Override
	protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) throws IOException {
		Map<IndexReader.CacheKey, LeafReader> readerCache = new HashMap<>();
		for (LeafReader reader : getSequentialSubReaders()) {
			if ((reader instanceof SoftDeletesDirectoryReaderWrapper.SoftDeletesFilterLeafReader) && ((reader.getReaderCacheHelper()) != null)) {
				readerCache.put(((SoftDeletesDirectoryReaderWrapper.SoftDeletesFilterLeafReader) (reader)).reader.getReaderCacheHelper().getKey(), reader);
			}else
				if ((reader instanceof SoftDeletesDirectoryReaderWrapper.SoftDeletesFilterCodecReader) && ((reader.getReaderCacheHelper()) != null)) {
					readerCache.put(((SoftDeletesDirectoryReaderWrapper.SoftDeletesFilterCodecReader) (reader)).reader.getReaderCacheHelper().getKey(), reader);
				}

		}
		return new SoftDeletesDirectoryReaderWrapper(in, new SoftDeletesDirectoryReaderWrapper.SoftDeletesSubReaderWrapper(readerCache, field));
	}

	@Override
	public IndexReader.CacheHelper getReaderCacheHelper() {
		return readerCacheHelper;
	}

	private static class SoftDeletesSubReaderWrapper extends FilterDirectoryReader.SubReaderWrapper {
		private final Map<IndexReader.CacheKey, LeafReader> mapping;

		private final String field;

		public SoftDeletesSubReaderWrapper(Map<IndexReader.CacheKey, LeafReader> oldReadersCache, String field) {
			Objects.requireNonNull(field, "Field must not be null");
			assert oldReadersCache != null;
			this.mapping = oldReadersCache;
			this.field = field;
		}

		@Override
		public LeafReader wrap(LeafReader reader) {
			IndexReader.CacheHelper readerCacheHelper = reader.getReaderCacheHelper();
			if ((readerCacheHelper != null) && (mapping.containsKey(readerCacheHelper.getKey()))) {
				return mapping.get(readerCacheHelper.getKey());
			}
			try {
				return SoftDeletesDirectoryReaderWrapper.wrap(reader, field);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	static LeafReader wrap(LeafReader reader, String field) throws IOException {
		DocIdSetIterator iterator = DocValuesFieldExistsQuery.getDocValuesDocIdSetIterator(field, reader);
		if (iterator == null) {
			return reader;
		}
		Bits liveDocs = reader.getLiveDocs();
		final FixedBitSet bits;
		if (liveDocs != null) {
		}else {
			bits = new FixedBitSet(reader.maxDoc());
			bits.set(0, reader.maxDoc());
		}
		return null;
	}

	private static boolean assertDocCounts(int expectedNumDocs, int numSoftDeletes, LeafReader reader) {
		if (reader instanceof SegmentReader) {
			SegmentReader segmentReader = ((SegmentReader) (reader));
			SegmentCommitInfo segmentInfo = segmentReader.getSegmentInfo();
		}
		return true;
	}

	static final class SoftDeletesFilterLeafReader extends FilterLeafReader {
		private final LeafReader reader;

		private final FixedBitSet bits;

		private final int numDocs;

		private final IndexReader.CacheHelper readerCacheHelper;

		private SoftDeletesFilterLeafReader(LeafReader reader, FixedBitSet bits, int numDocs) {
			super(reader);
			this.reader = reader;
			this.bits = bits;
			this.numDocs = numDocs;
			this.readerCacheHelper = ((reader.getReaderCacheHelper()) == null) ? null : new SoftDeletesDirectoryReaderWrapper.DelegatingCacheHelper(reader.getReaderCacheHelper());
		}

		@Override
		public Bits getLiveDocs() {
			return bits;
		}

		@Override
		public int numDocs() {
			return numDocs;
		}

		@Override
		public IndexReader.CacheHelper getCoreCacheHelper() {
			return reader.getCoreCacheHelper();
		}

		@Override
		public IndexReader.CacheHelper getReaderCacheHelper() {
			return readerCacheHelper;
		}
	}

	static final class SoftDeletesFilterCodecReader extends FilterCodecReader {
		private final LeafReader reader;

		private final FixedBitSet bits;

		private final int numDocs;

		private final IndexReader.CacheHelper readerCacheHelper;

		private SoftDeletesFilterCodecReader(CodecReader reader, FixedBitSet bits, int numDocs) {
			super(reader);
			this.reader = reader;
			this.bits = bits;
			this.numDocs = numDocs;
			this.readerCacheHelper = ((reader.getReaderCacheHelper()) == null) ? null : new SoftDeletesDirectoryReaderWrapper.DelegatingCacheHelper(reader.getReaderCacheHelper());
		}

		@Override
		public Bits getLiveDocs() {
			return bits;
		}

		@Override
		public int numDocs() {
			return numDocs;
		}

		@Override
		public IndexReader.CacheHelper getCoreCacheHelper() {
			return reader.getCoreCacheHelper();
		}

		@Override
		public IndexReader.CacheHelper getReaderCacheHelper() {
			return readerCacheHelper;
		}
	}

	private static class DelegatingCacheHelper implements IndexReader.CacheHelper {
		private final IndexReader.CacheHelper delegate;

		public DelegatingCacheHelper(IndexReader.CacheHelper delegate) {
			this.delegate = delegate;
		}

		@Override
		public IndexReader.CacheKey getKey() {
			return null;
		}

		@Override
		public void addClosedListener(IndexReader.ClosedListener listener) {
		}
	}
}

