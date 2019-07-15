

import java.io.IOException;
import java.util.Iterator;
import org.apache.lucene.codecs.TermVectorsWriter;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;


final class SortingTermVectorsConsumer {
	void initTermVectorsWriter() throws IOException {
	}

	private static void writeTermVectors(TermVectorsWriter writer, Fields vectors, FieldInfos fieldInfos) throws IOException {
		if (vectors == null) {
			writer.startDocument(0);
			writer.finishDocument();
			return;
		}
		int numFields = vectors.size();
		if (numFields == (-1)) {
			numFields = 0;
			for (final Iterator<String> it = vectors.iterator(); it.hasNext();) {
				it.next();
				numFields++;
			}
		}
		writer.startDocument(numFields);
		String lastFieldName = null;
		TermsEnum termsEnum = null;
		PostingsEnum docsAndPositionsEnum = null;
		int fieldCount = 0;
		for (String fieldName : vectors) {
			fieldCount++;
			final FieldInfo fieldInfo = fieldInfos.fieldInfo(fieldName);
			assert (lastFieldName == null) || ((fieldName.compareTo(lastFieldName)) > 0) : (("lastFieldName=" + lastFieldName) + " fieldName=") + fieldName;
			lastFieldName = fieldName;
			final Terms terms = vectors.terms(fieldName);
			if (terms == null) {
				continue;
			}
			final boolean hasPositions = terms.hasPositions();
			final boolean hasOffsets = terms.hasOffsets();
			final boolean hasPayloads = terms.hasPayloads();
			assert (!hasPayloads) || hasPositions;
			int numTerms = ((int) (terms.size()));
			if (numTerms == (-1)) {
				numTerms = 0;
				termsEnum = terms.iterator();
				while ((termsEnum.next()) != null) {
					numTerms++;
				} 
			}
			writer.startField(fieldInfo, numTerms, hasPositions, hasOffsets, hasPayloads);
			termsEnum = terms.iterator();
			int termCount = 0;
			while ((termsEnum.next()) != null) {
				termCount++;
				final int freq = ((int) (termsEnum.totalTermFreq()));
				writer.startTerm(termsEnum.term(), freq);
				if (hasPositions || hasOffsets) {
					docsAndPositionsEnum = termsEnum.postings(docsAndPositionsEnum, ((PostingsEnum.OFFSETS) | (PostingsEnum.PAYLOADS)));
					assert docsAndPositionsEnum != null;
					final int docID = docsAndPositionsEnum.nextDoc();
					assert docID != (DocIdSetIterator.NO_MORE_DOCS);
					assert (docsAndPositionsEnum.freq()) == freq;
					for (int posUpto = 0; posUpto < freq; posUpto++) {
						final int pos = docsAndPositionsEnum.nextPosition();
						final int startOffset = docsAndPositionsEnum.startOffset();
						final int endOffset = docsAndPositionsEnum.endOffset();
						final BytesRef payload = docsAndPositionsEnum.getPayload();
						assert (!hasPositions) || (pos >= 0);
						writer.addPosition(pos, startOffset, endOffset, payload);
					}
				}
				writer.finishTerm();
			} 
			assert termCount == numTerms;
			writer.finishField();
		}
		assert fieldCount == numFields;
		writer.finishDocument();
	}
}

