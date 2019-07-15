

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsReaderBase;
import org.apache.lucene.codecs.blocktree.FieldReader;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.fst.ByteSequenceOutputs;
import org.apache.lucene.util.fst.Outputs;


public final class BlockTreeTermsReader extends FieldsProducer {
	static final Outputs<BytesRef> FST_OUTPUTS = ByteSequenceOutputs.getSingleton();

	static final BytesRef NO_OUTPUT = BlockTreeTermsReader.FST_OUTPUTS.getNoOutput();

	static final int OUTPUT_FLAGS_NUM_BITS = 2;

	static final int OUTPUT_FLAGS_MASK = 3;

	static final int OUTPUT_FLAG_IS_FLOOR = 1;

	static final int OUTPUT_FLAG_HAS_TERMS = 2;

	static final String TERMS_EXTENSION = "tim";

	static final String TERMS_CODEC_NAME = "BlockTreeTermsDict";

	public static final int VERSION_START = 2;

	public static final int VERSION_AUTO_PREFIX_TERMS_REMOVED = 3;

	public static final int VERSION_CURRENT = BlockTreeTermsReader.VERSION_AUTO_PREFIX_TERMS_REMOVED;

	static final String TERMS_INDEX_EXTENSION = "tip";

	static final String TERMS_INDEX_CODEC_NAME = "BlockTreeTermsIndex";

	final IndexInput termsIn;

	final PostingsReaderBase postingsReader;

	private final TreeMap<String, FieldReader> fields = new TreeMap<>();

	final String segment;

	final int version;

	public BlockTreeTermsReader(PostingsReaderBase postingsReader, SegmentReadState state) throws IOException {
		boolean success = false;
		IndexInput indexIn = null;
		this.postingsReader = postingsReader;
		this.segment = state.segmentInfo.name;
		String termsName = IndexFileNames.segmentFileName(segment, state.segmentSuffix, BlockTreeTermsReader.TERMS_EXTENSION);
		try {
			termsIn = state.directory.openInput(termsName, state.context);
			version = CodecUtil.checkIndexHeader(termsIn, BlockTreeTermsReader.TERMS_CODEC_NAME, BlockTreeTermsReader.VERSION_START, BlockTreeTermsReader.VERSION_CURRENT, state.segmentInfo.getId(), state.segmentSuffix);
			if ((version) < (BlockTreeTermsReader.VERSION_AUTO_PREFIX_TERMS_REMOVED)) {
				byte b = termsIn.readByte();
				if (b != 0) {
					throw new CorruptIndexException(("Index header pretends the index has auto-prefix terms: " + b), termsIn);
				}
			}
			String indexName = IndexFileNames.segmentFileName(segment, state.segmentSuffix, BlockTreeTermsReader.TERMS_INDEX_EXTENSION);
			indexIn = state.directory.openInput(indexName, state.context);
			CodecUtil.checkIndexHeader(indexIn, BlockTreeTermsReader.TERMS_INDEX_CODEC_NAME, version, version, state.segmentInfo.getId(), state.segmentSuffix);
			CodecUtil.checksumEntireFile(indexIn);
			postingsReader.init(termsIn, state);
			CodecUtil.retrieveChecksum(termsIn);
			BlockTreeTermsReader.seekDir(termsIn);
			BlockTreeTermsReader.seekDir(indexIn);
			final int numFields = termsIn.readVInt();
			if (numFields < 0) {
				throw new CorruptIndexException(("invalid numFields: " + numFields), termsIn);
			}
			for (int i = 0; i < numFields; ++i) {
				final int field = termsIn.readVInt();
				final long numTerms = termsIn.readVLong();
				if (numTerms <= 0) {
					throw new CorruptIndexException(("Illegal numTerms for field number: " + field), termsIn);
				}
				final BytesRef rootCode = BlockTreeTermsReader.readBytesRef(termsIn);
				final FieldInfo fieldInfo = state.fieldInfos.fieldInfo(field);
				if (fieldInfo == null) {
					throw new CorruptIndexException(("invalid field number: " + field), termsIn);
				}
				final long sumTotalTermFreq = ((fieldInfo.getIndexOptions()) == (IndexOptions.DOCS)) ? -1 : termsIn.readVLong();
				final long sumDocFreq = termsIn.readVLong();
				final int docCount = termsIn.readVInt();
				final int longsSize = termsIn.readVInt();
				if (longsSize < 0) {
					throw new CorruptIndexException(((("invalid longsSize for field: " + (fieldInfo.name)) + ", longsSize=") + longsSize), termsIn);
				}
				BytesRef minTerm = BlockTreeTermsReader.readBytesRef(termsIn);
				BytesRef maxTerm = BlockTreeTermsReader.readBytesRef(termsIn);
				if ((docCount < 0) || (docCount > (state.segmentInfo.maxDoc()))) {
					throw new CorruptIndexException(((("invalid docCount: " + docCount) + " maxDoc: ") + (state.segmentInfo.maxDoc())), termsIn);
				}
				if (sumDocFreq < docCount) {
					throw new CorruptIndexException(((("invalid sumDocFreq: " + sumDocFreq) + " docCount: ") + docCount), termsIn);
				}
				if ((sumTotalTermFreq != (-1)) && (sumTotalTermFreq < sumDocFreq)) {
					throw new CorruptIndexException(((("invalid sumTotalTermFreq: " + sumTotalTermFreq) + " sumDocFreq: ") + sumDocFreq), termsIn);
				}
				final long indexStartFP = indexIn.readVLong();
			}
			indexIn.close();
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(indexIn, this);
			}
		}
	}

	private static BytesRef readBytesRef(IndexInput in) throws IOException {
		int numBytes = in.readVInt();
		if (numBytes < 0) {
			throw new CorruptIndexException(("invalid bytes length: " + numBytes), in);
		}
		BytesRef bytes = new BytesRef();
		bytes.length = numBytes;
		bytes.bytes = new byte[numBytes];
		in.readBytes(bytes.bytes, 0, numBytes);
		return bytes;
	}

	private static void seekDir(IndexInput input) throws IOException {
		input.seek((((input.length()) - (CodecUtil.footerLength())) - 8));
		long offset = input.readLong();
		input.seek(offset);
	}

	@Override
	public void close() throws IOException {
		try {
			IOUtils.close(termsIn, postingsReader);
		} finally {
			fields.clear();
		}
	}

	@Override
	public Iterator<String> iterator() {
		return Collections.unmodifiableSet(fields.keySet()).iterator();
	}

	@Override
	public Terms terms(String field) throws IOException {
		assert field != null;
		return fields.get(field);
	}

	@Override
	public int size() {
		return fields.size();
	}

	String brToString(BytesRef b) {
		if (b == null) {
			return "null";
		}else {
			try {
				return ((b.utf8ToString()) + " ") + b;
			} catch (Throwable t) {
				return b.toString();
			}
		}
	}

	@Override
	public long ramBytesUsed() {
		long sizeInBytes = postingsReader.ramBytesUsed();
		for (FieldReader reader : fields.values()) {
			sizeInBytes += reader.ramBytesUsed();
		}
		return sizeInBytes;
	}

	@Override
	public Collection<Accountable> getChildResources() {
		List<Accountable> resources = new ArrayList<>();
		resources.addAll(Accountables.namedAccountables("field", fields));
		resources.add(Accountables.namedAccountable("delegate", postingsReader));
		return Collections.unmodifiableList(resources);
	}

	@Override
	public void checkIntegrity() throws IOException {
		CodecUtil.checksumEntireFile(termsIn);
		postingsReader.checkIntegrity();
	}

	@Override
	public String toString() {
		return (((((getClass().getSimpleName()) + "(fields=") + (fields.size())) + ",delegate=") + (postingsReader)) + ")";
	}
}

