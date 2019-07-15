

import java.io.Closeable;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.CompoundFormat;
import org.apache.lucene.codecs.FieldInfosFormat;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.NormsFormat;
import org.apache.lucene.codecs.NormsProducer;
import org.apache.lucene.codecs.PointsFormat;
import org.apache.lucene.codecs.PointsReader;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.StoredFieldsReader;
import org.apache.lucene.codecs.TermVectorsFormat;
import org.apache.lucene.codecs.TermVectorsReader;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ClosedListener;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.util.CloseableThreadLocal;
import org.apache.lucene.util.IOUtils;


final class SegmentCoreReaders {
	private final AtomicInteger ref = new AtomicInteger(1);

	final FieldsProducer fields;

	final NormsProducer normsProducer;

	final StoredFieldsReader fieldsReaderOrig;

	final TermVectorsReader termVectorsReaderOrig;

	final PointsReader pointsReader;

	final Directory cfsReader;

	final String segment;

	final FieldInfos coreFieldInfos;

	final CloseableThreadLocal<StoredFieldsReader> fieldsReaderLocal = new CloseableThreadLocal<StoredFieldsReader>() {
		@Override
		protected StoredFieldsReader initialValue() {
			return fieldsReaderOrig.clone();
		}
	};

	final CloseableThreadLocal<TermVectorsReader> termVectorsLocal = new CloseableThreadLocal<TermVectorsReader>() {
		@Override
		protected TermVectorsReader initialValue() {
			return (termVectorsReaderOrig) == null ? null : termVectorsReaderOrig.clone();
		}
	};

	private final Set<IndexReader.ClosedListener> coreClosedListeners = Collections.synchronizedSet(new LinkedHashSet<IndexReader.ClosedListener>());

	SegmentCoreReaders(Directory dir, SegmentCommitInfo si, IOContext context) throws IOException {
		final Codec codec = si.info.getCodec();
		final Directory cfsDir;
		boolean success = false;
		try {
			if (si.info.getUseCompoundFile()) {
				cfsDir = cfsReader = codec.compoundFormat().getCompoundReader(dir, si.info, context);
			}else {
				cfsReader = null;
				cfsDir = dir;
			}
			segment = si.info.name;
			coreFieldInfos = codec.fieldInfosFormat().read(cfsDir, si.info, "", context);
			final SegmentReadState segmentReadState = new SegmentReadState(cfsDir, si.info, coreFieldInfos, context);
			final PostingsFormat format = codec.postingsFormat();
			fields = format.fieldsProducer(segmentReadState);
			assert (fields) != null;
			if (coreFieldInfos.hasNorms()) {
				normsProducer = codec.normsFormat().normsProducer(segmentReadState);
				assert (normsProducer) != null;
			}else {
				normsProducer = null;
			}
			fieldsReaderOrig = si.info.getCodec().storedFieldsFormat().fieldsReader(cfsDir, si.info, coreFieldInfos, context);
			if (coreFieldInfos.hasVectors()) {
				termVectorsReaderOrig = si.info.getCodec().termVectorsFormat().vectorsReader(cfsDir, si.info, coreFieldInfos, context);
			}else {
				termVectorsReaderOrig = null;
			}
			if (coreFieldInfos.hasPointValues()) {
				pointsReader = codec.pointsFormat().fieldsReader(segmentReadState);
			}else {
				pointsReader = null;
			}
			success = true;
		} catch (EOFException | FileNotFoundException e) {
			throw new CorruptIndexException(("Problem reading index from " + dir), dir.toString(), e);
		} catch (NoSuchFileException e) {
			throw new CorruptIndexException("Problem reading index.", e.getFile(), e);
		} finally {
			if (!success) {
				decRef();
			}
		}
	}

	int getRefCount() {
		return ref.get();
	}

	void incRef() {
		int count;
		while ((count = ref.get()) > 0) {
			if (ref.compareAndSet(count, (count + 1))) {
				return;
			}
		} 
		throw new AlreadyClosedException("SegmentCoreReaders is already closed");
	}

	@SuppressWarnings("try")
	void decRef() throws IOException {
		if ((ref.decrementAndGet()) == 0) {
			Throwable th = null;
			try (Closeable finalizer = this::notifyCoreClosedListeners) {
				IOUtils.close(termVectorsLocal, fieldsReaderLocal, fields, termVectorsReaderOrig, fieldsReaderOrig, cfsReader, normsProducer, pointsReader);
			}
		}
	}

	private final IndexReader.CacheHelper cacheHelper = new IndexReader.CacheHelper() {
		@Override
		public IndexReader.CacheKey getKey() {
			return null;
		}

		@Override
		public void addClosedListener(IndexReader.ClosedListener listener) {
			coreClosedListeners.add(listener);
		}
	};

	IndexReader.CacheHelper getCacheHelper() {
		return cacheHelper;
	}

	private void notifyCoreClosedListeners() throws IOException {
		synchronized(coreClosedListeners) {
			IOUtils.applyToAll(coreClosedListeners, ( l) -> l.onClose(cacheHelper.getKey()));
		}
	}

	@Override
	public String toString() {
		return ("SegmentCoreReader(" + (segment)) + ")";
	}
}

