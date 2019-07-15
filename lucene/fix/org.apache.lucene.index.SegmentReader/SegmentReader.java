

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.DocValuesProducer;
import org.apache.lucene.codecs.FieldInfosFormat;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.LiveDocsFormat;
import org.apache.lucene.codecs.NormsProducer;
import org.apache.lucene.codecs.PointsReader;
import org.apache.lucene.codecs.StoredFieldsReader;
import org.apache.lucene.index.CodecReader;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ClosedListener;
import org.apache.lucene.index.LeafMetaData;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.search.Sort;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;


public final class SegmentReader extends CodecReader {
	private final SegmentCommitInfo si;

	private final SegmentCommitInfo originalSi;

	private final LeafMetaData metaData;

	private final Bits liveDocs;

	private final int numDocs;

	final boolean isNRT;

	final DocValuesProducer docValuesProducer;

	final FieldInfos fieldInfos;

	SegmentReader(SegmentCommitInfo si, int createdVersionMajor, IOContext context) throws IOException {
		this.si = si.clone();
		this.originalSi = si;
		this.metaData = new LeafMetaData(createdVersionMajor, si.info.getMinVersion(), si.info.getIndexSort());
		this.isNRT = false;
		boolean success = false;
		final Codec codec = si.info.getCodec();
		try {
			if (si.hasDeletions()) {
				liveDocs = codec.liveDocsFormat().readLiveDocs(directory(), si, IOContext.READONCE);
			}else {
				assert (si.getDelCount()) == 0;
				liveDocs = null;
			}
			numDocs = (si.info.maxDoc()) - (si.getDelCount());
			fieldInfos = initFieldInfos();
			docValuesProducer = initDocValuesProducer();
			success = true;
		} finally {
			if (!success) {
				doClose();
			}
		}
		readerCacheHelper = null;
		coreCacheHelper = null;
	}

	SegmentReader(SegmentCommitInfo si, SegmentReader sr) throws IOException {
		this(si, sr, (si.hasDeletions() ? si.info.getCodec().liveDocsFormat().readLiveDocs(si.info.dir, si, IOContext.READONCE) : null), ((si.info.maxDoc()) - (si.getDelCount())), false);
	}

	SegmentReader(SegmentCommitInfo si, SegmentReader sr, Bits liveDocs, int numDocs) throws IOException {
		this(si, sr, liveDocs, numDocs, true);
	}

	SegmentReader(SegmentCommitInfo si, SegmentReader sr, Bits liveDocs, int numDocs, boolean isNRT) throws IOException {
		if (numDocs > (si.info.maxDoc())) {
			throw new IllegalArgumentException(((("numDocs=" + numDocs) + " but maxDoc=") + (si.info.maxDoc())));
		}
		if ((liveDocs != null) && ((liveDocs.length()) != (si.info.maxDoc()))) {
			throw new IllegalArgumentException(((("maxDoc=" + (si.info.maxDoc())) + " but liveDocs.size()=") + (liveDocs.length())));
		}
		this.si = si.clone();
		this.originalSi = si;
		this.metaData = sr.getMetaData();
		this.liveDocs = liveDocs;
		this.isNRT = isNRT;
		this.numDocs = numDocs;
		boolean success = false;
		try {
			fieldInfos = initFieldInfos();
			docValuesProducer = initDocValuesProducer();
			success = true;
		} finally {
			if (!success) {
				doClose();
			}
		}
		readerCacheHelper = null;
		coreCacheHelper = null;
	}

	private DocValuesProducer initDocValuesProducer() throws IOException {
		if ((fieldInfos.hasDocValues()) == false) {
			return null;
		}else {
			Directory dir;
			if (si.hasFieldUpdates()) {
			}else {
			}
		}
		return null;
	}

	private FieldInfos initFieldInfos() throws IOException {
		if (!(si.hasFieldUpdates())) {
		}else {
			FieldInfosFormat fisFormat = si.info.getCodec().fieldInfosFormat();
			final String segmentSuffix = Long.toString(si.getFieldInfosGen(), Character.MAX_RADIX);
			return fisFormat.read(si.info.dir, si.info, segmentSuffix, IOContext.READONCE);
		}
		return null;
	}

	@Override
	public Bits getLiveDocs() {
		ensureOpen();
		return liveDocs;
	}

	@Override
	protected void doClose() throws IOException {
		try {
		} finally {
		}
	}

	@Override
	public FieldInfos getFieldInfos() {
		ensureOpen();
		return fieldInfos;
	}

	@Override
	public int numDocs() {
		return numDocs;
	}

	@Override
	public int maxDoc() {
		return si.info.maxDoc();
	}

	@Override
	public StoredFieldsReader getFieldsReader() {
		ensureOpen();
		return null;
	}

	@Override
	public PointsReader getPointsReader() {
		ensureOpen();
		return null;
	}

	@Override
	public NormsProducer getNormsReader() {
		ensureOpen();
		return null;
	}

	@Override
	public DocValuesProducer getDocValuesReader() {
		ensureOpen();
		return docValuesProducer;
	}

	@Override
	public FieldsProducer getPostingsReader() {
		ensureOpen();
		return null;
	}

	@Override
	public String toString() {
		return si.toString((((si.info.maxDoc()) - (numDocs)) - (si.getDelCount())));
	}

	public String getSegmentName() {
		return si.info.name;
	}

	public SegmentCommitInfo getSegmentInfo() {
		return si;
	}

	public Directory directory() {
		return si.info.dir;
	}

	private final Set<IndexReader.ClosedListener> readerClosedListeners = new CopyOnWriteArraySet<>();

	void notifyReaderClosedListeners() throws IOException {
		synchronized(readerClosedListeners) {
			IOUtils.applyToAll(readerClosedListeners, ( l) -> l.onClose(readerCacheHelper.getKey()));
		}
	}

	private final IndexReader.CacheHelper readerCacheHelper;

	@Override
	public IndexReader.CacheHelper getReaderCacheHelper() {
		return readerCacheHelper;
	}

	private final IndexReader.CacheHelper coreCacheHelper;

	@Override
	public IndexReader.CacheHelper getCoreCacheHelper() {
		return coreCacheHelper;
	}

	@Override
	public LeafMetaData getMetaData() {
		return metaData;
	}

	SegmentCommitInfo getOriginalSegmentInfo() {
		return originalSi;
	}
}

