

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.codecs.StoredFieldsWriter;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.util.BytesRef;

import static org.apache.lucene.index.StoredFieldVisitor.Status.YES;


final class SortingStoredFieldsConsumer {
	protected void initStoredFieldsWriter() throws IOException {
	}

	void abort() {
		try {
		} finally {
		}
	}

	private static class CopyVisitor extends StoredFieldVisitor implements IndexableField {
		final StoredFieldsWriter writer;

		BytesRef binaryValue;

		String stringValue;

		Number numericValue;

		FieldInfo currentField;

		CopyVisitor(StoredFieldsWriter writer) {
			this.writer = writer;
		}

		@Override
		public void binaryField(FieldInfo fieldInfo, byte[] value) throws IOException {
			reset(fieldInfo);
			binaryValue = new BytesRef(value);
			write();
		}

		@Override
		public void stringField(FieldInfo fieldInfo, byte[] value) throws IOException {
			reset(fieldInfo);
			stringValue = new String(value, StandardCharsets.UTF_8);
			write();
		}

		@Override
		public void intField(FieldInfo fieldInfo, int value) throws IOException {
			reset(fieldInfo);
			numericValue = value;
			write();
		}

		@Override
		public void longField(FieldInfo fieldInfo, long value) throws IOException {
			reset(fieldInfo);
			numericValue = value;
			write();
		}

		@Override
		public void floatField(FieldInfo fieldInfo, float value) throws IOException {
			reset(fieldInfo);
			numericValue = value;
			write();
		}

		@Override
		public void doubleField(FieldInfo fieldInfo, double value) throws IOException {
			reset(fieldInfo);
			numericValue = value;
			write();
		}

		@Override
		public StoredFieldVisitor.Status needsField(FieldInfo fieldInfo) throws IOException {
			return YES;
		}

		@Override
		public String name() {
			return currentField.name;
		}

		@Override
		public IndexableFieldType fieldType() {
			return StoredField.TYPE;
		}

		@Override
		public BytesRef binaryValue() {
			return binaryValue;
		}

		@Override
		public String stringValue() {
			return stringValue;
		}

		@Override
		public Number numericValue() {
			return numericValue;
		}

		@Override
		public Reader readerValue() {
			return null;
		}

		@Override
		public TokenStream tokenStream(Analyzer analyzer, TokenStream reuse) {
			return null;
		}

		void reset(FieldInfo field) {
			currentField = field;
			binaryValue = null;
			stringValue = null;
			numericValue = null;
		}

		void write() throws IOException {
			writer.writeField(currentField, this);
		}
	}
}

