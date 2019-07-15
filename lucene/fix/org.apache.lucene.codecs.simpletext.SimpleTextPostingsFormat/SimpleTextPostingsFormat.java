

import java.io.IOException;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;


final class SimpleTextPostingsFormat extends PostingsFormat {
	public SimpleTextPostingsFormat() {
		super("SimpleText");
	}

	@Override
	public FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
		return null;
	}

	@Override
	public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
		return null;
	}

	static final String POSTINGS_EXTENSION = "pst";

	static String getPostingsFileName(String segment, String segmentSuffix) {
		return IndexFileNames.segmentFileName(segment, segmentSuffix, SimpleTextPostingsFormat.POSTINGS_EXTENSION);
	}
}

