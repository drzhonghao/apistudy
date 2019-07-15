

import java.io.IOException;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.blocktree.BlockTreeTermsWriter;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.util.BytesRef;


public class IDVersionPostingsFormat extends PostingsFormat {
	public static final long MIN_VERSION = 0;

	public static final long MAX_VERSION = 4611686018427387903L;

	private final int minTermsInBlock;

	private final int maxTermsInBlock;

	public IDVersionPostingsFormat() {
		this(BlockTreeTermsWriter.DEFAULT_MIN_BLOCK_SIZE, BlockTreeTermsWriter.DEFAULT_MAX_BLOCK_SIZE);
	}

	public IDVersionPostingsFormat(int minTermsInBlock, int maxTermsInBlock) {
		super("IDVersion");
		this.minTermsInBlock = minTermsInBlock;
		this.maxTermsInBlock = maxTermsInBlock;
		BlockTreeTermsWriter.validateSettings(minTermsInBlock, maxTermsInBlock);
	}

	@Override
	public FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
		boolean success = false;
		try {
			success = true;
		} finally {
			if (!success) {
			}
		}
		return null;
	}

	@Override
	public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
		boolean success = false;
		try {
			success = true;
		} finally {
			if (!success) {
			}
		}
		return null;
	}

	public static long bytesToLong(BytesRef bytes) {
		return (((((((((bytes.bytes[bytes.offset]) & 255L) << 56) | (((bytes.bytes[((bytes.offset) + 1)]) & 255L) << 48)) | (((bytes.bytes[((bytes.offset) + 2)]) & 255L) << 40)) | (((bytes.bytes[((bytes.offset) + 3)]) & 255L) << 32)) | (((bytes.bytes[((bytes.offset) + 4)]) & 255L) << 24)) | (((bytes.bytes[((bytes.offset) + 5)]) & 255L) << 16)) | (((bytes.bytes[((bytes.offset) + 6)]) & 255L) << 8)) | ((bytes.bytes[((bytes.offset) + 7)]) & 255L);
	}

	public static void longToBytes(long v, BytesRef bytes) {
		if ((v > (IDVersionPostingsFormat.MAX_VERSION)) || (v < (IDVersionPostingsFormat.MIN_VERSION))) {
			throw new IllegalArgumentException((((((("version must be >= MIN_VERSION=" + (IDVersionPostingsFormat.MIN_VERSION)) + " and <= MAX_VERSION=") + (IDVersionPostingsFormat.MAX_VERSION)) + " (got: ") + v) + ")"));
		}
		bytes.offset = 0;
		bytes.length = 8;
		bytes.bytes[0] = ((byte) (v >> 56));
		bytes.bytes[1] = ((byte) (v >> 48));
		bytes.bytes[2] = ((byte) (v >> 40));
		bytes.bytes[3] = ((byte) (v >> 32));
		bytes.bytes[4] = ((byte) (v >> 24));
		bytes.bytes[5] = ((byte) (v >> 16));
		bytes.bytes[6] = ((byte) (v >> 8));
		bytes.bytes[7] = ((byte) (v));
		assert (IDVersionPostingsFormat.bytesToLong(bytes)) == v : ((((IDVersionPostingsFormat.bytesToLong(bytes)) + " vs ") + v) + " bytes=") + bytes;
	}
}

