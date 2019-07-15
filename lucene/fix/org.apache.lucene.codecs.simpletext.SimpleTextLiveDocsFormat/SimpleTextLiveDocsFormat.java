

import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import org.apache.lucene.codecs.LiveDocsFormat;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.StringHelper;


public class SimpleTextLiveDocsFormat extends LiveDocsFormat {
	static final String LIVEDOCS_EXTENSION = "liv";

	static final BytesRef SIZE = new BytesRef("size ");

	static final BytesRef DOC = new BytesRef("  doc ");

	static final BytesRef END = new BytesRef("END");

	@Override
	public Bits readLiveDocs(Directory dir, SegmentCommitInfo info, IOContext context) throws IOException {
		assert info.hasDeletions();
		BytesRefBuilder scratch = new BytesRefBuilder();
		CharsRefBuilder scratchUTF16 = new CharsRefBuilder();
		String fileName = IndexFileNames.fileNameFromGeneration(info.info.name, SimpleTextLiveDocsFormat.LIVEDOCS_EXTENSION, info.getDelGen());
		ChecksumIndexInput in = null;
		boolean success = false;
		try {
			in = dir.openChecksumInput(fileName, context);
			assert StringHelper.startsWith(scratch.get(), SimpleTextLiveDocsFormat.SIZE);
			int size = parseIntAt(scratch.get(), SimpleTextLiveDocsFormat.SIZE.length, scratchUTF16);
			BitSet bits = new BitSet(size);
			while (!(scratch.get().equals(SimpleTextLiveDocsFormat.END))) {
				assert StringHelper.startsWith(scratch.get(), SimpleTextLiveDocsFormat.DOC);
				int docid = parseIntAt(scratch.get(), SimpleTextLiveDocsFormat.DOC.length, scratchUTF16);
				bits.set(docid);
			} 
			success = true;
			return new SimpleTextLiveDocsFormat.SimpleTextBits(bits, size);
		} finally {
			if (success) {
				IOUtils.close(in);
			}else {
				IOUtils.closeWhileHandlingException(in);
			}
		}
	}

	private int parseIntAt(BytesRef bytes, int offset, CharsRefBuilder scratch) {
		scratch.copyUTF8Bytes(bytes.bytes, ((bytes.offset) + offset), ((bytes.length) - offset));
		return ArrayUtil.parseInt(scratch.chars(), 0, scratch.length());
	}

	@Override
	public void writeLiveDocs(Bits bits, Directory dir, SegmentCommitInfo info, int newDelCount, IOContext context) throws IOException {
		int size = bits.length();
		BytesRefBuilder scratch = new BytesRefBuilder();
		String fileName = IndexFileNames.fileNameFromGeneration(info.info.name, SimpleTextLiveDocsFormat.LIVEDOCS_EXTENSION, info.getNextDelGen());
		IndexOutput out = null;
		boolean success = false;
		try {
			out = dir.createOutput(fileName, context);
			for (int i = 0; i < size; ++i) {
				if (bits.get(i)) {
				}
			}
			success = true;
		} finally {
			if (success) {
				IOUtils.close(out);
			}else {
				IOUtils.closeWhileHandlingException(out);
			}
		}
	}

	@Override
	public void files(SegmentCommitInfo info, Collection<String> files) throws IOException {
		if (info.hasDeletions()) {
			files.add(IndexFileNames.fileNameFromGeneration(info.info.name, SimpleTextLiveDocsFormat.LIVEDOCS_EXTENSION, info.getDelGen()));
		}
	}

	static class SimpleTextBits implements Bits {
		final BitSet bits;

		final int size;

		SimpleTextBits(BitSet bits, int size) {
			this.bits = bits;
			this.size = size;
		}

		@Override
		public boolean get(int index) {
			return bits.get(index);
		}

		@Override
		public int length() {
			return size;
		}
	}
}

