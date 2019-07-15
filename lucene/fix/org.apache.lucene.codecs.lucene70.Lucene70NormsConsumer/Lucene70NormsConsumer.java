

import java.io.IOException;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.NormsConsumer;
import org.apache.lucene.codecs.NormsProducer;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.IOUtils;


final class Lucene70NormsConsumer extends NormsConsumer {
	IndexOutput data;

	IndexOutput meta;

	final int maxDoc;

	Lucene70NormsConsumer(SegmentWriteState state, String dataCodec, String dataExtension, String metaCodec, String metaExtension) throws IOException {
		boolean success = false;
		try {
			String dataName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, dataExtension);
			data = state.directory.createOutput(dataName, state.context);
			String metaName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, metaExtension);
			meta = state.directory.createOutput(metaName, state.context);
			maxDoc = state.segmentInfo.maxDoc();
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(this);
			}
		}
	}

	@Override
	public void close() throws IOException {
		boolean success = false;
		try {
			if ((meta) != null) {
				meta.writeInt((-1));
				CodecUtil.writeFooter(meta);
			}
			if ((data) != null) {
				CodecUtil.writeFooter(data);
			}
			success = true;
		} finally {
			if (success) {
				IOUtils.close(data, meta);
			}else {
				IOUtils.closeWhileHandlingException(data, meta);
			}
			meta = data = null;
		}
	}

	@Override
	public void addNormsField(FieldInfo field, NormsProducer normsProducer) throws IOException {
		NumericDocValues values = normsProducer.getNorms(field);
		int numDocsWithValue = 0;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
			numDocsWithValue++;
			long v = values.longValue();
			min = Math.min(min, v);
			max = Math.max(max, v);
		}
		assert numDocsWithValue <= (maxDoc);
		meta.writeInt(field.number);
		if (numDocsWithValue == 0) {
			meta.writeLong((-2));
			meta.writeLong(0L);
		}else
			if (numDocsWithValue == (maxDoc)) {
				meta.writeLong((-1));
				meta.writeLong(0L);
			}else {
				long offset = data.getFilePointer();
				meta.writeLong(offset);
				values = normsProducer.getNorms(field);
				meta.writeLong(((data.getFilePointer()) - offset));
			}

		meta.writeInt(numDocsWithValue);
		int numBytesPerValue = numBytesPerValue(min, max);
		meta.writeByte(((byte) (numBytesPerValue)));
		if (numBytesPerValue == 0) {
			meta.writeLong(min);
		}else {
			meta.writeLong(data.getFilePointer());
			values = normsProducer.getNorms(field);
			writeValues(values, numBytesPerValue, data);
		}
	}

	private int numBytesPerValue(long min, long max) {
		if (min >= max) {
			return 0;
		}else
			if ((min >= (Byte.MIN_VALUE)) && (max <= (Byte.MAX_VALUE))) {
				return 1;
			}else
				if ((min >= (Short.MIN_VALUE)) && (max <= (Short.MAX_VALUE))) {
					return 2;
				}else
					if ((min >= (Integer.MIN_VALUE)) && (max <= (Integer.MAX_VALUE))) {
						return 4;
					}else {
						return 8;
					}



	}

	private void writeValues(NumericDocValues values, int numBytesPerValue, IndexOutput out) throws IOException, AssertionError {
		for (int doc = values.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = values.nextDoc()) {
			long value = values.longValue();
			switch (numBytesPerValue) {
				case 1 :
					out.writeByte(((byte) (value)));
					break;
				case 2 :
					out.writeShort(((short) (value)));
					break;
				case 4 :
					out.writeInt(((int) (value)));
					break;
				case 8 :
					out.writeLong(value);
					break;
				default :
					throw new AssertionError();
			}
		}
	}
}

