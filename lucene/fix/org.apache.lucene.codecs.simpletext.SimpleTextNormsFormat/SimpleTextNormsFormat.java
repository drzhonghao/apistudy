

import java.io.IOException;
import java.util.Collection;
import org.apache.lucene.codecs.NormsConsumer;
import org.apache.lucene.codecs.NormsFormat;
import org.apache.lucene.codecs.NormsProducer;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.util.Accountable;


public class SimpleTextNormsFormat extends NormsFormat {
	private static final String NORMS_SEG_EXTENSION = "len";

	@Override
	public NormsConsumer normsConsumer(SegmentWriteState state) throws IOException {
		return new SimpleTextNormsFormat.SimpleTextNormsConsumer(state);
	}

	@Override
	public NormsProducer normsProducer(SegmentReadState state) throws IOException {
		return new SimpleTextNormsFormat.SimpleTextNormsProducer(state);
	}

	public static class SimpleTextNormsProducer extends NormsProducer {
		public SimpleTextNormsProducer(SegmentReadState state) throws IOException {
		}

		@Override
		public NumericDocValues getNorms(FieldInfo field) throws IOException {
			return null;
		}

		@Override
		public void close() throws IOException {
		}

		@Override
		public long ramBytesUsed() {
			return 0l;
		}

		@Override
		public Collection<Accountable> getChildResources() {
			return null;
		}

		@Override
		public void checkIntegrity() throws IOException {
		}

		@Override
		public String toString() {
			return null;
		}
	}

	public static class SimpleTextNormsConsumer extends NormsConsumer {
		public SimpleTextNormsConsumer(SegmentWriteState state) throws IOException {
		}

		@Override
		public void addNormsField(FieldInfo field, NormsProducer normsProducer) throws IOException {
		}

		@Override
		public void close() throws IOException {
		}
	}
}

