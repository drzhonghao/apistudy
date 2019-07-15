

import java.io.IOException;
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.PostingsReaderBase;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IndexInput;


final class IDVersionPostingsReader extends PostingsReaderBase {
	@Override
	public void init(IndexInput termsIn, SegmentReadState state) throws IOException {
	}

	@Override
	public BlockTermState newTermState() {
		return null;
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public void decodeTerm(long[] longs, DataInput in, FieldInfo fieldInfo, BlockTermState _termState, boolean absolute) throws IOException {
		if (absolute) {
		}else {
		}
	}

	@Override
	public PostingsEnum postings(FieldInfo fieldInfo, BlockTermState termState, PostingsEnum reuse, int flags) throws IOException {
		if (PostingsEnum.featureRequested(flags, PostingsEnum.POSITIONS)) {
		}
		return null;
	}

	@Override
	public long ramBytesUsed() {
		return 0;
	}

	@Override
	public void checkIntegrity() throws IOException {
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}

