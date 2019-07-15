

import java.io.PrintStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexDeletionPolicy;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LiveIndexWriterConfig;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.InfoStream;
import org.apache.lucene.util.PrintStreamInfoStream;
import org.apache.lucene.util.SetOnce;

import static org.apache.lucene.search.SortField.Type.DOUBLE;
import static org.apache.lucene.search.SortField.Type.FLOAT;
import static org.apache.lucene.search.SortField.Type.INT;
import static org.apache.lucene.search.SortField.Type.LONG;
import static org.apache.lucene.search.SortField.Type.STRING;


public final class IndexWriterConfig extends LiveIndexWriterConfig {
	public static enum OpenMode {

		CREATE,
		APPEND,
		CREATE_OR_APPEND;}

	public static final int DISABLE_AUTO_FLUSH = -1;

	public static final int DEFAULT_MAX_BUFFERED_DELETE_TERMS = IndexWriterConfig.DISABLE_AUTO_FLUSH;

	public static final int DEFAULT_MAX_BUFFERED_DOCS = IndexWriterConfig.DISABLE_AUTO_FLUSH;

	public static final double DEFAULT_RAM_BUFFER_SIZE_MB = 16.0;

	public static final boolean DEFAULT_READER_POOLING = true;

	public static final int DEFAULT_RAM_PER_THREAD_HARD_LIMIT_MB = 1945;

	public static final boolean DEFAULT_USE_COMPOUND_FILE_SYSTEM = true;

	public static final boolean DEFAULT_COMMIT_ON_CLOSE = true;

	private SetOnce<IndexWriter> writer = new SetOnce<>();

	IndexWriterConfig setIndexWriter(IndexWriter writer) {
		if ((this.writer.get()) != null) {
			throw new IllegalStateException("do not share IndexWriterConfig instances across IndexWriters");
		}
		this.writer.set(writer);
		return this;
	}

	public IndexWriterConfig setOpenMode(IndexWriterConfig.OpenMode openMode) {
		if (openMode == null) {
			throw new IllegalArgumentException("openMode must not be null");
		}
		return this;
	}

	public IndexWriterConfig setIndexDeletionPolicy(IndexDeletionPolicy delPolicy) {
		if (delPolicy == null) {
			throw new IllegalArgumentException("indexDeletionPolicy must not be null");
		}
		this.delPolicy = delPolicy;
		return this;
	}

	@Override
	public IndexDeletionPolicy getIndexDeletionPolicy() {
		return delPolicy;
	}

	public IndexWriterConfig setIndexCommit(IndexCommit commit) {
		this.commit = commit;
		return this;
	}

	@Override
	public IndexCommit getIndexCommit() {
		return commit;
	}

	public IndexWriterConfig setSimilarity(Similarity similarity) {
		if (similarity == null) {
			throw new IllegalArgumentException("similarity must not be null");
		}
		this.similarity = similarity;
		return this;
	}

	@Override
	public Similarity getSimilarity() {
		return similarity;
	}

	public IndexWriterConfig setMergeScheduler(MergeScheduler mergeScheduler) {
		if (mergeScheduler == null) {
			throw new IllegalArgumentException("mergeScheduler must not be null");
		}
		this.mergeScheduler = mergeScheduler;
		return this;
	}

	@Override
	public MergeScheduler getMergeScheduler() {
		return mergeScheduler;
	}

	public IndexWriterConfig setCodec(Codec codec) {
		if (codec == null) {
			throw new IllegalArgumentException("codec must not be null");
		}
		this.codec = codec;
		return this;
	}

	@Override
	public Codec getCodec() {
		return codec;
	}

	@Override
	public MergePolicy getMergePolicy() {
		return mergePolicy;
	}

	public IndexWriterConfig setReaderPooling(boolean readerPooling) {
		this.readerPooling = readerPooling;
		return this;
	}

	@Override
	public boolean getReaderPooling() {
		return readerPooling;
	}

	public IndexWriterConfig setRAMPerThreadHardLimitMB(int perThreadHardLimitMB) {
		if ((perThreadHardLimitMB <= 0) || (perThreadHardLimitMB >= 2048)) {
			throw new IllegalArgumentException("PerThreadHardLimit must be greater than 0 and less than 2048MB");
		}
		this.perThreadHardLimitMB = perThreadHardLimitMB;
		return this;
	}

	@Override
	public int getRAMPerThreadHardLimitMB() {
		return perThreadHardLimitMB;
	}

	@Override
	public InfoStream getInfoStream() {
		return infoStream;
	}

	@Override
	public Analyzer getAnalyzer() {
		return super.getAnalyzer();
	}

	@Override
	public int getMaxBufferedDocs() {
		return super.getMaxBufferedDocs();
	}

	@Override
	public IndexWriter.IndexReaderWarmer getMergedSegmentWarmer() {
		return super.getMergedSegmentWarmer();
	}

	@Override
	public double getRAMBufferSizeMB() {
		return super.getRAMBufferSizeMB();
	}

	public IndexWriterConfig setInfoStream(InfoStream infoStream) {
		if (infoStream == null) {
			throw new IllegalArgumentException(("Cannot set InfoStream implementation to null. " + "To disable logging use InfoStream.NO_OUTPUT"));
		}
		this.infoStream = infoStream;
		return this;
	}

	public IndexWriterConfig setInfoStream(PrintStream printStream) {
		if (printStream == null) {
			throw new IllegalArgumentException("printStream must not be null");
		}
		return setInfoStream(new PrintStreamInfoStream(printStream));
	}

	@Override
	public IndexWriterConfig setMergePolicy(MergePolicy mergePolicy) {
		return ((IndexWriterConfig) (super.setMergePolicy(mergePolicy)));
	}

	@Override
	public IndexWriterConfig setMaxBufferedDocs(int maxBufferedDocs) {
		return ((IndexWriterConfig) (super.setMaxBufferedDocs(maxBufferedDocs)));
	}

	@Override
	public IndexWriterConfig setMergedSegmentWarmer(IndexWriter.IndexReaderWarmer mergeSegmentWarmer) {
		return ((IndexWriterConfig) (super.setMergedSegmentWarmer(mergeSegmentWarmer)));
	}

	@Override
	public IndexWriterConfig setRAMBufferSizeMB(double ramBufferSizeMB) {
		return ((IndexWriterConfig) (super.setRAMBufferSizeMB(ramBufferSizeMB)));
	}

	@Override
	public IndexWriterConfig setUseCompoundFile(boolean useCompoundFile) {
		return ((IndexWriterConfig) (super.setUseCompoundFile(useCompoundFile)));
	}

	public IndexWriterConfig setCommitOnClose(boolean commitOnClose) {
		this.commitOnClose = commitOnClose;
		return this;
	}

	private static final EnumSet<SortField.Type> ALLOWED_INDEX_SORT_TYPES = EnumSet.of(STRING, LONG, INT, DOUBLE, FLOAT);

	public IndexWriterConfig setIndexSort(Sort sort) {
		for (SortField sortField : sort.getSort()) {
		}
		this.indexSort = sort;
		this.indexSortFields = Arrays.stream(sort.getSort()).map(SortField::getField).collect(Collectors.toSet());
		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("writer=").append(writer.get()).append("\n");
		return sb.toString();
	}

	@Override
	public IndexWriterConfig setCheckPendingFlushUpdate(boolean checkPendingFlushOnUpdate) {
		return ((IndexWriterConfig) (super.setCheckPendingFlushUpdate(checkPendingFlushOnUpdate)));
	}

	public IndexWriterConfig setSoftDeletesField(String softDeletesField) {
		this.softDeletesField = softDeletesField;
		return this;
	}
}

