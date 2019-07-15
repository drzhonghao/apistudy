

import java.io.IOException;
import org.apache.lucene.codecs.StoredFieldsWriter;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.IOUtils;


class StoredFieldsConsumer {
	StoredFieldsWriter writer;

	int lastDoc;

	protected void initStoredFieldsWriter() throws IOException {
		if ((writer) == null) {
		}
	}

	void startDocument(int docID) throws IOException {
		assert (lastDoc) < docID;
		initStoredFieldsWriter();
		while ((++(lastDoc)) < docID) {
			writer.startDocument();
			writer.finishDocument();
		} 
		writer.startDocument();
	}

	void writeField(FieldInfo info, IndexableField field) throws IOException {
		writer.writeField(info, field);
	}

	void finishDocument() throws IOException {
		writer.finishDocument();
	}

	void finish(int maxDoc) throws IOException {
		while ((lastDoc) < (maxDoc - 1)) {
			startDocument(lastDoc);
			finishDocument();
			++(lastDoc);
		} 
	}

	void abort() {
		if ((writer) != null) {
			IOUtils.closeWhileHandlingException(writer);
			writer = null;
		}
	}
}

