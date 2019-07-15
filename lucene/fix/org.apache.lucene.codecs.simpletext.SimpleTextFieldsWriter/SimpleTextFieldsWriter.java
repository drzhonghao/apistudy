

import java.io.IOException;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.BytesRefIterator;


class SimpleTextFieldsWriter extends FieldsConsumer {
	private IndexOutput out;

	private final BytesRefBuilder scratch = new BytesRefBuilder();

	private final SegmentWriteState writeState;

	final String segment;

	static final BytesRef END = new BytesRef("END");

	static final BytesRef FIELD = new BytesRef("field ");

	static final BytesRef TERM = new BytesRef("  term ");

	static final BytesRef DOC = new BytesRef("    doc ");

	static final BytesRef FREQ = new BytesRef("      freq ");

	static final BytesRef POS = new BytesRef("      pos ");

	static final BytesRef START_OFFSET = new BytesRef("      startOffset ");

	static final BytesRef END_OFFSET = new BytesRef("      endOffset ");

	static final BytesRef PAYLOAD = new BytesRef("        payload ");

	public SimpleTextFieldsWriter(SegmentWriteState writeState) throws IOException {
		segment = writeState.segmentInfo.name;
		this.writeState = writeState;
	}

	@Override
	public void write(Fields fields) throws IOException {
		write(writeState.fieldInfos, fields);
	}

	public void write(FieldInfos fieldInfos, Fields fields) throws IOException {
		for (String field : fields) {
			Terms terms = fields.terms(field);
			if (terms == null) {
				continue;
			}
			FieldInfo fieldInfo = fieldInfos.fieldInfo(field);
			boolean wroteField = false;
			boolean hasPositions = terms.hasPositions();
			boolean hasFreqs = terms.hasFreqs();
			boolean hasPayloads = fieldInfo.hasPayloads();
			boolean hasOffsets = terms.hasOffsets();
			int flags = 0;
			if (hasPositions) {
				flags = PostingsEnum.POSITIONS;
				if (hasPayloads) {
					flags = flags | (PostingsEnum.PAYLOADS);
				}
				if (hasOffsets) {
					flags = flags | (PostingsEnum.OFFSETS);
				}
			}else {
				if (hasFreqs) {
					flags = flags | (PostingsEnum.FREQS);
				}
			}
			TermsEnum termsEnum = terms.iterator();
			PostingsEnum postingsEnum = null;
			while (true) {
				BytesRef term = termsEnum.next();
				if (term == null) {
					break;
				}
				postingsEnum = termsEnum.postings(postingsEnum, flags);
				assert postingsEnum != null : (((("termsEnum=" + termsEnum) + " hasPos=") + hasPositions) + " flags=") + flags;
				boolean wroteTerm = false;
				while (true) {
					int doc = postingsEnum.nextDoc();
					if (doc == (PostingsEnum.NO_MORE_DOCS)) {
						break;
					}
					if (!wroteTerm) {
						if (!wroteField) {
							write(SimpleTextFieldsWriter.FIELD);
							write(field);
							newline();
							wroteField = true;
						}
						write(SimpleTextFieldsWriter.TERM);
						write(term);
						newline();
						wroteTerm = true;
					}
					write(SimpleTextFieldsWriter.DOC);
					write(Integer.toString(doc));
					newline();
					if (hasFreqs) {
						int freq = postingsEnum.freq();
						write(SimpleTextFieldsWriter.FREQ);
						write(Integer.toString(freq));
						newline();
						if (hasPositions) {
							int lastStartOffset = 0;
							for (int i = 0; i < freq; i++) {
								int position = postingsEnum.nextPosition();
								write(SimpleTextFieldsWriter.POS);
								write(Integer.toString(position));
								newline();
								if (hasOffsets) {
									int startOffset = postingsEnum.startOffset();
									int endOffset = postingsEnum.endOffset();
									assert endOffset >= startOffset;
									assert startOffset >= lastStartOffset : (("startOffset=" + startOffset) + " lastStartOffset=") + lastStartOffset;
									lastStartOffset = startOffset;
									write(SimpleTextFieldsWriter.START_OFFSET);
									write(Integer.toString(startOffset));
									newline();
									write(SimpleTextFieldsWriter.END_OFFSET);
									write(Integer.toString(endOffset));
									newline();
								}
								BytesRef payload = postingsEnum.getPayload();
								if ((payload != null) && ((payload.length) > 0)) {
									assert (payload.length) != 0;
									write(SimpleTextFieldsWriter.PAYLOAD);
									write(payload);
									newline();
								}
							}
						}
					}
				} 
			} 
		}
	}

	private void write(String s) throws IOException {
	}

	private void write(BytesRef b) throws IOException {
	}

	private void newline() throws IOException {
	}

	@Override
	public void close() throws IOException {
		if ((out) != null) {
			try {
				write(SimpleTextFieldsWriter.END);
				newline();
			} finally {
				out.close();
				out = null;
			}
		}
	}
}

