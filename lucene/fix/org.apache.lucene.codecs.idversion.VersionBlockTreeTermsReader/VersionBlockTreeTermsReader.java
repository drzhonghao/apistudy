

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsReaderBase;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;


public final class VersionBlockTreeTermsReader extends FieldsProducer {
	final IndexInput in = null;

	final PostingsReaderBase postingsReader;

	public VersionBlockTreeTermsReader(PostingsReaderBase postingsReader, SegmentReadState state) throws IOException {
		this.postingsReader = postingsReader;
		boolean success = false;
		IndexInput indexIn = null;
		try {
			CodecUtil.checksumEntireFile(indexIn);
			postingsReader.init(in, state);
			CodecUtil.retrieveChecksum(in);
			seekDir(in);
			seekDir(indexIn);
			final int numFields = in.readVInt();
			if (numFields < 0) {
				throw new CorruptIndexException(("invalid numFields: " + numFields), in);
			}
			for (int i = 0; i < numFields; i++) {
				final int field = in.readVInt();
				final long numTerms = in.readVLong();
				assert numTerms >= 0;
				final int numBytes = in.readVInt();
				final BytesRef code = new BytesRef(new byte[numBytes]);
				in.readBytes(code.bytes, 0, numBytes);
				code.length = numBytes;
				final long version = in.readVLong();
				final FieldInfo fieldInfo = state.fieldInfos.fieldInfo(field);
				assert fieldInfo != null : "field=" + field;
				final long sumTotalTermFreq = numTerms;
				final long sumDocFreq = numTerms;
				assert numTerms <= (Integer.MAX_VALUE);
				final int docCount = ((int) (numTerms));
				final int longsSize = in.readVInt();
				BytesRef minTerm = VersionBlockTreeTermsReader.readBytesRef(in);
				BytesRef maxTerm = VersionBlockTreeTermsReader.readBytesRef(in);
				if ((docCount < 0) || (docCount > (state.segmentInfo.maxDoc()))) {
					throw new CorruptIndexException(((("invalid docCount: " + docCount) + " maxDoc: ") + (state.segmentInfo.maxDoc())), in);
				}
				if (sumDocFreq < docCount) {
					throw new CorruptIndexException(((("invalid sumDocFreq: " + sumDocFreq) + " docCount: ") + docCount), in);
				}
				if ((sumTotalTermFreq != (-1)) && (sumTotalTermFreq < sumDocFreq)) {
					throw new CorruptIndexException(((("invalid sumTotalTermFreq: " + sumTotalTermFreq) + " sumDocFreq: ") + sumDocFreq), in);
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
		BytesRef bytes = new BytesRef();
		bytes.length = in.readVInt();
		bytes.bytes = new byte[bytes.length];
		in.readBytes(bytes.bytes, 0, bytes.length);
		return bytes;
	}

	private void seekDir(IndexInput input) throws IOException {
		input.seek((((input.length()) - (CodecUtil.footerLength())) - 8));
		long dirOffset = input.readLong();
		input.seek(dirOffset);
	}

	@Override
	public void close() throws IOException {
		try {
			IOUtils.close(in, postingsReader);
		} finally {
		}
	}

	@Override
	public Iterator<String> iterator() {
		return null;
	}

	@Override
	public Terms terms(String field) throws IOException {
		assert field != null;
		return null;
	}

	@Override
	public int size() {
		return 0;
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
		return sizeInBytes;
	}

	@Override
	public Collection<Accountable> getChildResources() {
		List<Accountable> resources = new ArrayList<>();
		resources.add(Accountables.namedAccountable("delegate", postingsReader));
		return Collections.unmodifiableList(resources);
	}

	@Override
	public void checkIntegrity() throws IOException {
		CodecUtil.checksumEntireFile(in);
		postingsReader.checkIntegrity();
	}

	@Override
	public String toString() {
		return null;
	}
}

