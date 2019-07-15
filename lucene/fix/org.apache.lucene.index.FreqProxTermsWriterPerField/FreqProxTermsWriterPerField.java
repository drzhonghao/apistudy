

import java.io.IOException;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;


final class FreqProxTermsWriterPerField {
	private FreqProxTermsWriterPerField.FreqProxPostingsArray freqProxPostingsArray;

	final boolean hasFreq = false;

	final boolean hasProx = false;

	final boolean hasOffsets = false;

	PayloadAttribute payloadAttribute;

	OffsetAttribute offsetAttribute;

	long sumTotalTermFreq;

	long sumDocFreq;

	int docCount;

	boolean sawPayloads;

	void finish() throws IOException {
		if (sawPayloads) {
		}
	}

	boolean start(IndexableField f, boolean first) {
		return true;
	}

	void writeProx(int termID, int proxCode) {
		if ((payloadAttribute) == null) {
		}else {
			BytesRef payload = payloadAttribute.getPayload();
			if ((payload != null) && ((payload.length) > 0)) {
				sawPayloads = true;
			}else {
			}
		}
	}

	void writeOffsets(int termID, int offsetAccum) {
		final int startOffset = offsetAccum + (offsetAttribute.startOffset());
		final int endOffset = offsetAccum + (offsetAttribute.endOffset());
		assert (startOffset - (freqProxPostingsArray.lastOffsets[termID])) >= 0;
		freqProxPostingsArray.lastOffsets[termID] = startOffset;
	}

	void newTerm(final int termID) {
		final FreqProxTermsWriterPerField.FreqProxPostingsArray postings = freqProxPostingsArray;
		if (!(hasFreq)) {
			assert (postings.termFreqs) == null;
		}else {
			postings.termFreqs[termID] = getTermFreq();
			if (hasProx) {
				if (hasOffsets) {
				}
			}else {
				assert !(hasOffsets);
			}
		}
	}

	void addTerm(final int termID) {
		final FreqProxTermsWriterPerField.FreqProxPostingsArray postings = freqProxPostingsArray;
		assert (!(hasFreq)) || ((postings.termFreqs[termID]) > 0);
		if (!(hasFreq)) {
			assert (postings.termFreqs) == null;
		}else {
		}
	}

	private int getTermFreq() {
		return 0;
	}

	public void newPostingsArray() {
	}

	static final class FreqProxPostingsArray {
		public FreqProxPostingsArray(int size, boolean writeFreqs, boolean writeProx, boolean writeOffsets) {
			if (writeFreqs) {
				termFreqs = new int[size];
			}
			lastDocIDs = new int[size];
			lastDocCodes = new int[size];
			if (writeProx) {
				lastPositions = new int[size];
				if (writeOffsets) {
					lastOffsets = new int[size];
				}
			}else {
				assert !writeOffsets;
			}
		}

		int[] termFreqs;

		int[] lastDocIDs;

		int[] lastDocCodes;

		int[] lastPositions;

		int[] lastOffsets;

		int bytesPerPosting() {
			if ((lastPositions) != null) {
			}
			if ((lastOffsets) != null) {
			}
			if ((termFreqs) != null) {
			}
			return 0;
		}
	}
}

