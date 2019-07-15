

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.IOUtils;


final class CompletionFieldsConsumer extends FieldsConsumer {
	private final String delegatePostingsFormatName;

	private final Map<String, CompletionFieldsConsumer.CompletionMetaData> seenFields = new HashMap<>();

	private final SegmentWriteState state;

	private IndexOutput dictOut;

	private FieldsConsumer delegateFieldsConsumer;

	CompletionFieldsConsumer(PostingsFormat delegatePostingsFormat, SegmentWriteState state) throws IOException {
		this.delegatePostingsFormatName = delegatePostingsFormat.getName();
		this.state = state;
		boolean success = false;
		try {
			this.delegateFieldsConsumer = delegatePostingsFormat.fieldsConsumer(state);
			success = true;
		} finally {
			if (success == false) {
				IOUtils.closeWhileHandlingException(dictOut, delegateFieldsConsumer);
			}
		}
	}

	@Override
	public void write(Fields fields) throws IOException {
		delegateFieldsConsumer.write(fields);
		for (String field : fields) {
			CompletionFieldsConsumer.CompletionTermWriter termWriter = new CompletionFieldsConsumer.CompletionTermWriter();
			Terms terms = fields.terms(field);
			if (terms == null) {
				continue;
			}
			TermsEnum termsEnum = terms.iterator();
			BytesRef term;
			while ((term = termsEnum.next()) != null) {
				termWriter.write(term, termsEnum);
			} 
			long filePointer = dictOut.getFilePointer();
			if (termWriter.finish(dictOut)) {
				seenFields.put(field, new CompletionFieldsConsumer.CompletionMetaData(filePointer, termWriter.minWeight, termWriter.maxWeight, termWriter.type));
			}
		}
	}

	private boolean closed = false;

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
		boolean success = false;
	}

	private static class CompletionMetaData {
		private final long filePointer;

		private final long minWeight;

		private final long maxWeight;

		private final byte type;

		private CompletionMetaData(long filePointer, long minWeight, long maxWeight, byte type) {
			this.filePointer = filePointer;
			this.minWeight = minWeight;
			this.maxWeight = maxWeight;
			this.type = type;
		}
	}

	private static class CompletionTermWriter {
		private PostingsEnum postingsEnum = null;

		private int docCount = 0;

		private long maxWeight = 0;

		private long minWeight = Long.MAX_VALUE;

		private byte type;

		private boolean first;

		private final BytesRefBuilder scratch = new BytesRefBuilder();

		public CompletionTermWriter() {
			first = true;
		}

		public boolean finish(IndexOutput output) throws IOException {
			if ((docCount) == 0) {
				minWeight = 0;
			}
			return false;
		}

		public void write(BytesRef term, TermsEnum termsEnum) throws IOException {
			postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.PAYLOADS);
			int docFreq = 0;
			while ((postingsEnum.nextDoc()) != (DocIdSetIterator.NO_MORE_DOCS)) {
				int docID = postingsEnum.docID();
				for (int i = 0; i < (postingsEnum.freq()); i++) {
					postingsEnum.nextPosition();
					assert (postingsEnum.getPayload()) != null;
					BytesRef payload = postingsEnum.getPayload();
					ByteArrayDataInput input = new ByteArrayDataInput(payload.bytes, payload.offset, payload.length);
					int len = input.readVInt();
					scratch.grow(len);
					scratch.setLength(len);
					input.readBytes(scratch.bytes(), 0, scratch.length());
					long weight = (input.readVInt()) - 1;
					maxWeight = Math.max(maxWeight, weight);
					minWeight = Math.min(minWeight, weight);
					byte type = input.readByte();
					if (first) {
						this.type = type;
						first = false;
					}else
						if ((this.type) != type) {
							throw new IllegalArgumentException("single field name has mixed types");
						}

				}
				docFreq++;
				docCount = Math.max(docCount, (docFreq + 1));
			} 
		}
	}
}

