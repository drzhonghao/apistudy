

import java.io.IOException;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.BytesRef;


final class TermVectorsConsumerPerField {
	private TermVectorsConsumerPerField.TermVectorsPostingsArray termVectorsPostingsArray;

	boolean doVectors;

	boolean doVectorPositions;

	boolean doVectorOffsets;

	boolean doVectorPayloads;

	OffsetAttribute offsetAttribute;

	PayloadAttribute payloadAttribute;

	boolean hasPayloads;

	void finish() {
	}

	void finishDocument() throws IOException {
		if ((doVectors) == false) {
			return;
		}
		doVectors = false;
		TermVectorsConsumerPerField.TermVectorsPostingsArray postings = termVectorsPostingsArray;
	}

	boolean start(IndexableField field, boolean first) {
		assert (field.fieldType().indexOptions()) != (IndexOptions.NONE);
		if (first) {
			hasPayloads = false;
			doVectors = field.fieldType().storeTermVectors();
			if (doVectors) {
				doVectorPositions = field.fieldType().storeTermVectorPositions();
				doVectorOffsets = field.fieldType().storeTermVectorOffsets();
				if (doVectorPositions) {
					doVectorPayloads = field.fieldType().storeTermVectorPayloads();
				}else {
					doVectorPayloads = false;
					if (field.fieldType().storeTermVectorPayloads()) {
						throw new IllegalArgumentException((("cannot index term vector payloads without term vector positions (field=\"" + (field.name())) + "\")"));
					}
				}
			}else {
				if (field.fieldType().storeTermVectorOffsets()) {
					throw new IllegalArgumentException((("cannot index term vector offsets when term vectors are not indexed (field=\"" + (field.name())) + "\")"));
				}
				if (field.fieldType().storeTermVectorPositions()) {
					throw new IllegalArgumentException((("cannot index term vector positions when term vectors are not indexed (field=\"" + (field.name())) + "\")"));
				}
				if (field.fieldType().storeTermVectorPayloads()) {
					throw new IllegalArgumentException((("cannot index term vector payloads when term vectors are not indexed (field=\"" + (field.name())) + "\")"));
				}
			}
		}else {
			if ((doVectors) != (field.fieldType().storeTermVectors())) {
				throw new IllegalArgumentException((("all instances of a given field name must have the same term vectors settings (storeTermVectors changed for field=\"" + (field.name())) + "\")"));
			}
			if ((doVectorPositions) != (field.fieldType().storeTermVectorPositions())) {
				throw new IllegalArgumentException((("all instances of a given field name must have the same term vectors settings (storeTermVectorPositions changed for field=\"" + (field.name())) + "\")"));
			}
			if ((doVectorOffsets) != (field.fieldType().storeTermVectorOffsets())) {
				throw new IllegalArgumentException((("all instances of a given field name must have the same term vectors settings (storeTermVectorOffsets changed for field=\"" + (field.name())) + "\")"));
			}
			if ((doVectorPayloads) != (field.fieldType().storeTermVectorPayloads())) {
				throw new IllegalArgumentException((("all instances of a given field name must have the same term vectors settings (storeTermVectorPayloads changed for field=\"" + (field.name())) + "\")"));
			}
		}
		if (doVectors) {
			if (doVectorOffsets) {
				assert (offsetAttribute) != null;
			}
			if (doVectorPayloads) {
			}else {
				payloadAttribute = null;
			}
		}
		return doVectors;
	}

	void writeProx(TermVectorsConsumerPerField.TermVectorsPostingsArray postings, int termID) {
		if (doVectorOffsets) {
		}
		if (doVectorPositions) {
			final BytesRef payload;
			if ((payloadAttribute) == null) {
				payload = null;
			}else {
				payload = payloadAttribute.getPayload();
			}
			if ((payload != null) && ((payload.length) > 0)) {
				hasPayloads = true;
			}else {
			}
		}
	}

	void newTerm(final int termID) {
		TermVectorsConsumerPerField.TermVectorsPostingsArray postings = termVectorsPostingsArray;
		postings.freqs[termID] = getTermFreq();
		postings.lastOffsets[termID] = 0;
		postings.lastPositions[termID] = 0;
		writeProx(postings, termID);
	}

	void addTerm(final int termID) {
		TermVectorsConsumerPerField.TermVectorsPostingsArray postings = termVectorsPostingsArray;
		postings.freqs[termID] += getTermFreq();
		writeProx(postings, termID);
	}

	private int getTermFreq() {
		return 0;
	}

	public void newPostingsArray() {
	}

	static final class TermVectorsPostingsArray {
		public TermVectorsPostingsArray(int size) {
			freqs = new int[size];
			lastOffsets = new int[size];
			lastPositions = new int[size];
		}

		int[] freqs;

		int[] lastOffsets;

		int[] lastPositions;
	}
}

