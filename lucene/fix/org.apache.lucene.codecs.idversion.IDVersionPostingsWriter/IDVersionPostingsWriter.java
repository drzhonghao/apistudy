

import java.io.IOException;
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.PushPostingsWriterBase;
import org.apache.lucene.codecs.idversion.IDVersionPostingsFormat;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;


final class IDVersionPostingsWriter extends PushPostingsWriterBase {
	static final String TERMS_CODEC = "IDVersionPostingsWriterTerms";

	static final int VERSION_START = 1;

	static final int VERSION_CURRENT = IDVersionPostingsWriter.VERSION_START;

	int lastDocID;

	private int lastPosition;

	private long lastVersion;

	private final Bits liveDocs;

	private String segment;

	public IDVersionPostingsWriter(Bits liveDocs) {
		this.liveDocs = liveDocs;
	}

	@Override
	public BlockTermState newTermState() {
		return null;
	}

	@Override
	public void init(IndexOutput termsOut, SegmentWriteState state) throws IOException {
		CodecUtil.writeIndexHeader(termsOut, IDVersionPostingsWriter.TERMS_CODEC, IDVersionPostingsWriter.VERSION_CURRENT, state.segmentInfo.getId(), state.segmentSuffix);
		segment = state.segmentInfo.name;
	}

	@Override
	public int setField(FieldInfo fieldInfo) {
		super.setField(fieldInfo);
		if ((fieldInfo.getIndexOptions()) != (IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)) {
			throw new IllegalArgumentException("field must be index using IndexOptions.DOCS_AND_FREQS_AND_POSITIONS");
		}
		if (fieldInfo.hasVectors()) {
			throw new IllegalArgumentException("field cannot index term vectors: CheckIndex will report this as index corruption");
		}
		return 0;
	}

	@Override
	public void startTerm() {
		lastDocID = -1;
	}

	@Override
	public void startDoc(int docID, int termDocFreq) throws IOException {
		if (((liveDocs) != null) && ((liveDocs.get(docID)) == false)) {
			return;
		}
		if ((lastDocID) != (-1)) {
			throw new IllegalArgumentException(((("term appears in more than one document: " + (lastDocID)) + " and ") + docID));
		}
		if (termDocFreq != 1) {
			throw new IllegalArgumentException("term appears more than once in the document");
		}
		lastDocID = docID;
		lastPosition = -1;
		lastVersion = -1;
	}

	@Override
	public void addPosition(int position, BytesRef payload, int startOffset, int endOffset) throws IOException {
		if ((lastDocID) == (-1)) {
			return;
		}
		if ((lastPosition) != (-1)) {
			throw new IllegalArgumentException("term appears more than once in document");
		}
		lastPosition = position;
		if (payload == null) {
			throw new IllegalArgumentException("token doens't have a payload");
		}
		if ((payload.length) != 8) {
			throw new IllegalArgumentException((("payload.length != 8 (got " + (payload.length)) + ")"));
		}
		lastVersion = IDVersionPostingsFormat.bytesToLong(payload);
		if ((lastVersion) < (IDVersionPostingsFormat.MIN_VERSION)) {
			throw new IllegalArgumentException((((((("version must be >= MIN_VERSION=" + (IDVersionPostingsFormat.MIN_VERSION)) + " (got: ") + (lastVersion)) + "; payload=") + payload) + ")"));
		}
		if ((lastVersion) > (IDVersionPostingsFormat.MAX_VERSION)) {
			throw new IllegalArgumentException((((((("version must be <= MAX_VERSION=" + (IDVersionPostingsFormat.MAX_VERSION)) + " (got: ") + (lastVersion)) + "; payload=") + payload) + ")"));
		}
	}

	@Override
	public void finishDoc() throws IOException {
		if ((lastDocID) == (-1)) {
			return;
		}
		if ((lastPosition) == (-1)) {
			throw new IllegalArgumentException("missing addPosition");
		}
	}

	@Override
	public void finishTerm(BlockTermState _state) throws IOException {
		if ((lastDocID) == (-1)) {
			return;
		}
	}

	private long lastEncodedVersion;

	@Override
	public void encodeTerm(long[] longs, DataOutput out, FieldInfo fieldInfo, BlockTermState _state, boolean absolute) throws IOException {
		if (absolute) {
		}else {
		}
	}

	@Override
	public void close() throws IOException {
	}
}

