

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.NormsProducer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.RandomAccessInput;
import org.apache.lucene.util.IOUtils;


class Lucene53NormsProducer extends NormsProducer {
	private final Map<Integer, Lucene53NormsProducer.NormsEntry> norms = new HashMap<>();

	private final IndexInput data;

	private final int maxDoc;

	Lucene53NormsProducer(SegmentReadState state, String dataCodec, String dataExtension, String metaCodec, String metaExtension) throws IOException {
		maxDoc = state.segmentInfo.maxDoc();
		String metaName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, metaExtension);
		int version = -1;
		try (ChecksumIndexInput in = state.directory.openChecksumInput(metaName, state.context)) {
			Throwable priorE = null;
			try {
				readFields(in, state.fieldInfos);
			} catch (Throwable exception) {
				priorE = exception;
			} finally {
				CodecUtil.checkFooter(in, priorE);
			}
		}
		String dataName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, dataExtension);
		data = state.directory.openInput(dataName, state.context);
		boolean success = false;
		try {
			CodecUtil.retrieveChecksum(data);
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(this.data);
			}
		}
	}

	private void readFields(IndexInput meta, FieldInfos infos) throws IOException {
		int fieldNumber = meta.readVInt();
		while (fieldNumber != (-1)) {
			FieldInfo info = infos.fieldInfo(fieldNumber);
			if (info == null) {
				throw new CorruptIndexException(("Invalid field number: " + fieldNumber), meta);
			}else
				if (!(info.hasNorms())) {
					throw new CorruptIndexException(("Invalid field: " + (info.name)), meta);
				}

			Lucene53NormsProducer.NormsEntry entry = new Lucene53NormsProducer.NormsEntry();
			entry.bytesPerValue = meta.readByte();
			switch (entry.bytesPerValue) {
				case 0 :
				case 1 :
				case 2 :
				case 4 :
				case 8 :
					break;
				default :
					throw new CorruptIndexException(((("Invalid bytesPerValue: " + (entry.bytesPerValue)) + ", field: ") + (info.name)), meta);
			}
			entry.offset = meta.readLong();
			norms.put(info.number, entry);
			fieldNumber = meta.readVInt();
		} 
	}

	@Override
	public NumericDocValues getNorms(FieldInfo field) throws IOException {
		final Lucene53NormsProducer.NormsEntry entry = norms.get(field.number);
		if ((entry.bytesPerValue) == 0) {
			final long value = entry.offset;
			return new Lucene53NormsProducer.NormsIterator(maxDoc) {
				@Override
				public long longValue() {
					return value;
				}
			};
		}else {
			RandomAccessInput slice;
			synchronized(data) {
				switch (entry.bytesPerValue) {
					case 1 :
						slice = data.randomAccessSlice(entry.offset, maxDoc);
						return new Lucene53NormsProducer.NormsIterator(maxDoc) {
							@Override
							public long longValue() throws IOException {
								return slice.readByte(docID);
							}
						};
					case 2 :
						slice = data.randomAccessSlice(entry.offset, ((maxDoc) * 2L));
						return new Lucene53NormsProducer.NormsIterator(maxDoc) {
							@Override
							public long longValue() throws IOException {
								return slice.readShort((((long) (docID)) << 1L));
							}
						};
					case 4 :
						slice = data.randomAccessSlice(entry.offset, ((maxDoc) * 4L));
						return new Lucene53NormsProducer.NormsIterator(maxDoc) {
							@Override
							public long longValue() throws IOException {
								return slice.readInt((((long) (docID)) << 2L));
							}
						};
					case 8 :
						slice = data.randomAccessSlice(entry.offset, ((maxDoc) * 8L));
						return new Lucene53NormsProducer.NormsIterator(maxDoc) {
							@Override
							public long longValue() throws IOException {
								return slice.readLong((((long) (docID)) << 3L));
							}
						};
					default :
						throw new AssertionError();
				}
			}
		}
	}

	@Override
	public void close() throws IOException {
		data.close();
	}

	@Override
	public long ramBytesUsed() {
		return 64L * (norms.size());
	}

	@Override
	public void checkIntegrity() throws IOException {
		CodecUtil.checksumEntireFile(data);
	}

	static class NormsEntry {
		byte bytesPerValue;

		long offset;
	}

	@Override
	public String toString() {
		return (((getClass().getSimpleName()) + "(fields=") + (norms.size())) + ")";
	}

	private abstract static class NormsIterator extends NumericDocValues {
		private final int maxDoc;

		protected int docID = -1;

		public NormsIterator(int maxDoc) {
			this.maxDoc = maxDoc;
		}

		@Override
		public int docID() {
			return docID;
		}

		@Override
		public int nextDoc() {
			(docID)++;
			if ((docID) == (maxDoc)) {
				docID = DocIdSetIterator.NO_MORE_DOCS;
			}
			return docID;
		}

		@Override
		public int advance(int target) {
			docID = target;
			if ((docID) >= (maxDoc)) {
				docID = DocIdSetIterator.NO_MORE_DOCS;
			}
			return docID;
		}

		@Override
		public boolean advanceExact(int target) throws IOException {
			docID = target;
			return true;
		}

		@Override
		public long cost() {
			return 0;
		}
	}
}

