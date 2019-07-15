

import java.io.IOException;
import org.apache.lucene.codecs.TermVectorsWriter;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;


class TermVectorsConsumer {
	TermVectorsWriter writer;

	final BytesRef flushTerm = new BytesRef();

	boolean hasVectors;

	int numVectorFields;

	int lastDocID;

	void fill(int docID) throws IOException {
		while ((lastDocID) < docID) {
			writer.startDocument(0);
			writer.finishDocument();
			(lastDocID)++;
		} 
	}

	void initTermVectorsWriter() throws IOException {
		if ((writer) == null) {
			lastDocID = 0;
		}
	}

	void finishDocument() throws IOException {
		if (!(hasVectors)) {
			return;
		}
		initTermVectorsWriter();
		writer.startDocument(numVectorFields);
		for (int i = 0; i < (numVectorFields); i++) {
		}
		writer.finishDocument();
		(lastDocID)++;
		resetFields();
	}

	public void abort() {
		hasVectors = false;
		try {
		} finally {
			IOUtils.closeWhileHandlingException(writer);
			writer = null;
			lastDocID = 0;
		}
	}

	void resetFields() {
		numVectorFields = 0;
	}

	void startDocument() {
		resetFields();
		numVectorFields = 0;
	}
}

