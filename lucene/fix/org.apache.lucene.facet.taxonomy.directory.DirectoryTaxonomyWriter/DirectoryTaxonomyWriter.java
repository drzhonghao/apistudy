

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.FacetLabel;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.writercache.TaxonomyWriterCache;
import org.apache.lucene.facet.taxonomy.writercache.UTF8TaxonomyWriterCache;
import org.apache.lucene.index.BaseCompositeReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.LiveIndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderManager;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FutureObjects;

import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE;
import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE_OR_APPEND;


public class DirectoryTaxonomyWriter implements TaxonomyWriter {
	public static final String INDEX_EPOCH = "index.epoch";

	private final Directory dir;

	private final IndexWriter indexWriter;

	private final TaxonomyWriterCache cache;

	private final AtomicInteger cacheMisses = new AtomicInteger(0);

	private long indexEpoch;

	private Field parentStreamField;

	private Field fullPathField;

	private int cacheMissesUntilFill = 11;

	private boolean shouldFillCache = true;

	private ReaderManager readerManager;

	private volatile boolean initializedReaderManager = false;

	private volatile boolean shouldRefreshReaderManager;

	private volatile boolean cacheIsComplete;

	private volatile boolean isClosed = false;

	private volatile int nextID;

	private static Map<String, String> readCommitData(Directory dir) throws IOException {
		SegmentInfos infos = SegmentInfos.readLatestCommit(dir);
		return infos.getUserData();
	}

	public DirectoryTaxonomyWriter(Directory directory, IndexWriterConfig.OpenMode openMode, TaxonomyWriterCache cache) throws IOException {
		dir = directory;
		IndexWriterConfig config = createIndexWriterConfig(openMode);
		indexWriter = openIndexWriter(dir, config);
		assert !((indexWriter.getConfig().getMergePolicy()) instanceof TieredMergePolicy) : "for preserving category docids, merging none-adjacent segments is not allowed";
		openMode = config.getOpenMode();
		if (!(DirectoryReader.indexExists(directory))) {
			indexEpoch = 1;
		}else {
			String epochStr = null;
			Map<String, String> commitData = DirectoryTaxonomyWriter.readCommitData(directory);
			if (commitData != null) {
				epochStr = commitData.get(DirectoryTaxonomyWriter.INDEX_EPOCH);
			}
			indexEpoch = (epochStr == null) ? 1 : Long.parseLong(epochStr, 16);
		}
		if (openMode == (CREATE)) {
			++(indexEpoch);
		}
		FieldType ft = new FieldType(TextField.TYPE_NOT_STORED);
		ft.setOmitNorms(true);
		nextID = indexWriter.maxDoc();
		if (cache == null) {
			cache = DirectoryTaxonomyWriter.defaultTaxonomyWriterCache();
		}
		this.cache = cache;
		if ((nextID) == 0) {
			cacheIsComplete = true;
			addCategory(new FacetLabel());
		}else {
			cacheIsComplete = false;
		}
	}

	protected IndexWriter openIndexWriter(Directory directory, IndexWriterConfig config) throws IOException {
		return new IndexWriter(directory, config);
	}

	protected IndexWriterConfig createIndexWriterConfig(IndexWriterConfig.OpenMode openMode) {
		return new IndexWriterConfig(null).setOpenMode(openMode).setMergePolicy(new LogByteSizeMergePolicy());
	}

	private void initReaderManager() throws IOException {
		if (!(initializedReaderManager)) {
			synchronized(this) {
				ensureOpen();
				if (!(initializedReaderManager)) {
					readerManager = new ReaderManager(indexWriter, false, false);
					shouldRefreshReaderManager = false;
					initializedReaderManager = true;
				}
			}
		}
	}

	public DirectoryTaxonomyWriter(Directory directory, IndexWriterConfig.OpenMode openMode) throws IOException {
		this(directory, openMode, DirectoryTaxonomyWriter.defaultTaxonomyWriterCache());
	}

	public static TaxonomyWriterCache defaultTaxonomyWriterCache() {
		return new UTF8TaxonomyWriterCache();
	}

	public DirectoryTaxonomyWriter(Directory d) throws IOException {
		this(d, CREATE_OR_APPEND);
	}

	@Override
	public synchronized void close() throws IOException {
		if (!(isClosed)) {
			commit();
			indexWriter.close();
			doClose();
		}
	}

	private void doClose() throws IOException {
		isClosed = true;
		closeResources();
	}

	protected synchronized void closeResources() throws IOException {
		if (initializedReaderManager) {
			readerManager.close();
			readerManager = null;
			initializedReaderManager = false;
		}
		if ((cache) != null) {
			cache.close();
		}
	}

	protected synchronized int findCategory(FacetLabel categoryPath) throws IOException {
		int res = cache.get(categoryPath);
		if ((res >= 0) || (cacheIsComplete)) {
			return res;
		}
		cacheMisses.incrementAndGet();
		perhapsFillCache();
		res = cache.get(categoryPath);
		if ((res >= 0) || (cacheIsComplete)) {
			return res;
		}
		initReaderManager();
		int doc = -1;
		DirectoryReader reader = readerManager.acquire();
		try {
			final BytesRef catTerm = new BytesRef(FacetsConfig.pathToString(categoryPath.components, categoryPath.length));
			PostingsEnum docs = null;
			for (LeafReaderContext ctx : reader.leaves()) {
			}
		} finally {
			readerManager.release(reader);
		}
		if (doc > 0) {
			addToCache(categoryPath, doc);
		}
		return doc;
	}

	@Override
	public int addCategory(FacetLabel categoryPath) throws IOException {
		ensureOpen();
		int res = cache.get(categoryPath);
		if (res < 0) {
			synchronized(this) {
				res = findCategory(categoryPath);
				if (res < 0) {
					res = internalAddCategory(categoryPath);
				}
			}
		}
		return res;
	}

	private int internalAddCategory(FacetLabel cp) throws IOException {
		int parent;
		if ((cp.length) > 1) {
			FacetLabel parentPath = cp.subpath(((cp.length) - 1));
			parent = findCategory(parentPath);
			if (parent < 0) {
				parent = internalAddCategory(parentPath);
			}
		}else
			if ((cp.length) == 1) {
				parent = TaxonomyReader.ROOT_ORDINAL;
			}else {
				parent = TaxonomyReader.INVALID_ORDINAL;
			}

		int id = addCategoryDocument(cp, parent);
		return id;
	}

	protected final void ensureOpen() {
		if (isClosed) {
			throw new AlreadyClosedException("The taxonomy writer has already been closed");
		}
	}

	private int addCategoryDocument(FacetLabel categoryPath, int parent) throws IOException {
		Document d = new Document();
		d.add(parentStreamField);
		fullPathField.setStringValue(FacetsConfig.pathToString(categoryPath.components, categoryPath.length));
		d.add(fullPathField);
		indexWriter.addDocument(d);
		int id = (nextID)++;
		shouldRefreshReaderManager = true;
		addToCache(categoryPath, id);
		return id;
	}

	private static class SinglePositionTokenStream extends TokenStream {
		private CharTermAttribute termAtt;

		private PositionIncrementAttribute posIncrAtt;

		private boolean returned;

		private int val;

		private final String word;

		public SinglePositionTokenStream(String word) {
			termAtt = addAttribute(CharTermAttribute.class);
			posIncrAtt = addAttribute(PositionIncrementAttribute.class);
			this.word = word;
			returned = true;
		}

		public void set(int val) {
			this.val = val;
			returned = false;
		}

		@Override
		public boolean incrementToken() throws IOException {
			if (returned) {
				return false;
			}
			clearAttributes();
			posIncrAtt.setPositionIncrement(val);
			termAtt.setEmpty();
			termAtt.append(word);
			returned = true;
			return true;
		}
	}

	private void addToCache(FacetLabel categoryPath, int id) throws IOException {
		if (cache.put(categoryPath, id)) {
			refreshReaderManager();
			cacheIsComplete = false;
		}
	}

	private synchronized void refreshReaderManager() throws IOException {
		if ((shouldRefreshReaderManager) && (initializedReaderManager)) {
			readerManager.maybeRefresh();
			shouldRefreshReaderManager = false;
		}
	}

	@Override
	public synchronized long commit() throws IOException {
		ensureOpen();
		Map<String, String> data = new HashMap<>();
		Iterable<Map.Entry<String, String>> iter = indexWriter.getLiveCommitData();
		if (iter != null) {
			for (Map.Entry<String, String> ent : iter) {
				data.put(ent.getKey(), ent.getValue());
			}
		}
		String epochStr = data.get(DirectoryTaxonomyWriter.INDEX_EPOCH);
		if ((epochStr == null) || ((Long.parseLong(epochStr, 16)) != (indexEpoch))) {
			indexWriter.setLiveCommitData(combinedCommitData(indexWriter.getLiveCommitData()));
		}
		return indexWriter.commit();
	}

	private Iterable<Map.Entry<String, String>> combinedCommitData(Iterable<Map.Entry<String, String>> commitData) {
		Map<String, String> m = new HashMap<>();
		if (commitData != null) {
			for (Map.Entry<String, String> ent : commitData) {
				m.put(ent.getKey(), ent.getValue());
			}
		}
		m.put(DirectoryTaxonomyWriter.INDEX_EPOCH, Long.toString(indexEpoch, 16));
		return m.entrySet();
	}

	@Override
	public void setLiveCommitData(Iterable<Map.Entry<String, String>> commitUserData) {
		indexWriter.setLiveCommitData(combinedCommitData(commitUserData));
	}

	@Override
	public Iterable<Map.Entry<String, String>> getLiveCommitData() {
		return combinedCommitData(indexWriter.getLiveCommitData());
	}

	@Override
	public synchronized long prepareCommit() throws IOException {
		ensureOpen();
		Map<String, String> data = new HashMap<>();
		Iterable<Map.Entry<String, String>> iter = indexWriter.getLiveCommitData();
		if (iter != null) {
			for (Map.Entry<String, String> ent : iter) {
				data.put(ent.getKey(), ent.getValue());
			}
		}
		String epochStr = data.get(DirectoryTaxonomyWriter.INDEX_EPOCH);
		if ((epochStr == null) || ((Long.parseLong(epochStr, 16)) != (indexEpoch))) {
			indexWriter.setLiveCommitData(combinedCommitData(indexWriter.getLiveCommitData()));
		}
		return indexWriter.prepareCommit();
	}

	@Override
	public int getSize() {
		ensureOpen();
		return nextID;
	}

	public void setCacheMissesUntilFill(int i) {
		ensureOpen();
		cacheMissesUntilFill = i;
	}

	private synchronized void perhapsFillCache() throws IOException {
		if ((cacheMisses.get()) < (cacheMissesUntilFill)) {
			return;
		}
		if (!(shouldFillCache)) {
			return;
		}
		shouldFillCache = false;
		initReaderManager();
		boolean aborted = false;
		DirectoryReader reader = readerManager.acquire();
		try {
			PostingsEnum postingsEnum = null;
			for (LeafReaderContext ctx : reader.leaves()) {
				if (aborted) {
					break;
				}
			}
		} finally {
			readerManager.release(reader);
		}
		cacheIsComplete = !aborted;
		if (cacheIsComplete) {
			synchronized(this) {
				readerManager.close();
				readerManager = null;
				initializedReaderManager = false;
			}
		}
	}

	@Override
	public int getParent(int ordinal) throws IOException {
		ensureOpen();
		FutureObjects.checkIndex(ordinal, nextID);
		return 0;
	}

	public void addTaxonomy(Directory taxoDir, DirectoryTaxonomyWriter.OrdinalMap map) throws IOException {
		ensureOpen();
		DirectoryReader r = DirectoryReader.open(taxoDir);
		try {
			final int size = r.numDocs();
			final DirectoryTaxonomyWriter.OrdinalMap ordinalMap = map;
			ordinalMap.setSize(size);
			int base = 0;
			PostingsEnum docs = null;
			for (final LeafReaderContext ctx : r.leaves()) {
				final LeafReader ar = ctx.reader();
				base += ar.maxDoc();
			}
			ordinalMap.addDone();
		} finally {
			r.close();
		}
	}

	public static interface OrdinalMap {
		public abstract void setSize(int size) throws IOException;

		public abstract void addMapping(int origOrdinal, int newOrdinal) throws IOException;

		public abstract void addDone() throws IOException;

		public abstract int[] getMap() throws IOException;
	}

	public static final class MemoryOrdinalMap implements DirectoryTaxonomyWriter.OrdinalMap {
		int[] map;

		public MemoryOrdinalMap() {
		}

		@Override
		public void setSize(int taxonomySize) {
			map = new int[taxonomySize];
		}

		@Override
		public void addMapping(int origOrdinal, int newOrdinal) {
			map[origOrdinal] = newOrdinal;
		}

		@Override
		public void addDone() {
		}

		@Override
		public int[] getMap() {
			return map;
		}
	}

	public static final class DiskOrdinalMap implements DirectoryTaxonomyWriter.OrdinalMap {
		Path tmpfile;

		DataOutputStream out;

		public DiskOrdinalMap(Path tmpfile) throws IOException {
			this.tmpfile = tmpfile;
			out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmpfile)));
		}

		@Override
		public void addMapping(int origOrdinal, int newOrdinal) throws IOException {
			out.writeInt(origOrdinal);
			out.writeInt(newOrdinal);
		}

		@Override
		public void setSize(int taxonomySize) throws IOException {
			out.writeInt(taxonomySize);
		}

		@Override
		public void addDone() throws IOException {
			if ((out) != null) {
				out.close();
				out = null;
			}
		}

		int[] map = null;

		@Override
		public int[] getMap() throws IOException {
			if ((map) != null) {
				return map;
			}
			addDone();
			try (final DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(tmpfile)))) {
				map = new int[in.readInt()];
				for (int i = 0; i < (map.length); i++) {
					int origordinal = in.readInt();
					int newordinal = in.readInt();
					map[origordinal] = newordinal;
				}
			}
			Files.delete(tmpfile);
			return map;
		}
	}

	@Override
	public synchronized void rollback() throws IOException {
		ensureOpen();
		indexWriter.rollback();
		doClose();
	}

	public synchronized void replaceTaxonomy(Directory taxoDir) throws IOException {
		indexWriter.deleteAll();
		indexWriter.addIndexes(taxoDir);
		shouldRefreshReaderManager = true;
		initReaderManager();
		refreshReaderManager();
		nextID = indexWriter.maxDoc();
		cache.clear();
		cacheIsComplete = false;
		shouldFillCache = true;
		cacheMisses.set(0);
		++(indexEpoch);
	}

	public Directory getDirectory() {
		return dir;
	}

	final IndexWriter getInternalIndexWriter() {
		return indexWriter;
	}

	public final long getTaxonomyEpoch() {
		return indexEpoch;
	}
}

