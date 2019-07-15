

import java.io.IOException;
import org.apache.lucene.codecs.StoredFieldsWriter;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IOUtils;


public class SimpleTextStoredFieldsWriter extends StoredFieldsWriter {
	private int numDocsWritten = 0;

	private final Directory directory;

	private final String segment;

	private IndexOutput out;

	static final String FIELDS_EXTENSION = "fld";

	static final BytesRef TYPE_STRING = new BytesRef("string");

	static final BytesRef TYPE_BINARY = new BytesRef("binary");

	static final BytesRef TYPE_INT = new BytesRef("int");

	static final BytesRef TYPE_LONG = new BytesRef("long");

	static final BytesRef TYPE_FLOAT = new BytesRef("float");

	static final BytesRef TYPE_DOUBLE = new BytesRef("double");

	static final BytesRef END = new BytesRef("END");

	static final BytesRef DOC = new BytesRef("doc ");

	static final BytesRef FIELD = new BytesRef("  field ");

	static final BytesRef NAME = new BytesRef("    name ");

	static final BytesRef TYPE = new BytesRef("    type ");

	static final BytesRef VALUE = new BytesRef("    value ");

	private final BytesRefBuilder scratch = new BytesRefBuilder();

	public SimpleTextStoredFieldsWriter(Directory directory, String segment, IOContext context) throws IOException {
		this.directory = directory;
		this.segment = segment;
		boolean success = false;
		try {
			out = directory.createOutput(IndexFileNames.segmentFileName(segment, "", SimpleTextStoredFieldsWriter.FIELDS_EXTENSION), context);
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(this);
			}
		}
	}

	@Override
	public void startDocument() throws IOException {
		write(SimpleTextStoredFieldsWriter.DOC);
		write(Integer.toString(numDocsWritten));
		newLine();
		(numDocsWritten)++;
	}

	@Override
	public void writeField(FieldInfo info, IndexableField field) throws IOException {
		write(SimpleTextStoredFieldsWriter.FIELD);
		write(Integer.toString(info.number));
		newLine();
		write(SimpleTextStoredFieldsWriter.NAME);
		write(field.name());
		newLine();
		write(SimpleTextStoredFieldsWriter.TYPE);
		final Number n = field.numericValue();
		if (n != null) {
			if (((n instanceof Byte) || (n instanceof Short)) || (n instanceof Integer)) {
				write(SimpleTextStoredFieldsWriter.TYPE_INT);
				newLine();
				write(SimpleTextStoredFieldsWriter.VALUE);
				write(Integer.toString(n.intValue()));
				newLine();
			}else
				if (n instanceof Long) {
					write(SimpleTextStoredFieldsWriter.TYPE_LONG);
					newLine();
					write(SimpleTextStoredFieldsWriter.VALUE);
					write(Long.toString(n.longValue()));
					newLine();
				}else
					if (n instanceof Float) {
						write(SimpleTextStoredFieldsWriter.TYPE_FLOAT);
						newLine();
						write(SimpleTextStoredFieldsWriter.VALUE);
						write(Float.toString(n.floatValue()));
						newLine();
					}else
						if (n instanceof Double) {
							write(SimpleTextStoredFieldsWriter.TYPE_DOUBLE);
							newLine();
							write(SimpleTextStoredFieldsWriter.VALUE);
							write(Double.toString(n.doubleValue()));
							newLine();
						}else {
							throw new IllegalArgumentException(("cannot store numeric type " + (n.getClass())));
						}



		}else {
			BytesRef bytes = field.binaryValue();
			if (bytes != null) {
				write(SimpleTextStoredFieldsWriter.TYPE_BINARY);
				newLine();
				write(SimpleTextStoredFieldsWriter.VALUE);
				write(bytes);
				newLine();
			}else
				if ((field.stringValue()) == null) {
					throw new IllegalArgumentException((("field " + (field.name())) + " is stored but does not have binaryValue, stringValue nor numericValue"));
				}else {
					write(SimpleTextStoredFieldsWriter.TYPE_STRING);
					newLine();
					write(SimpleTextStoredFieldsWriter.VALUE);
					write(field.stringValue());
					newLine();
				}

		}
	}

	@Override
	public void finish(FieldInfos fis, int numDocs) throws IOException {
		if ((numDocsWritten) != numDocs) {
			throw new RuntimeException((((((("mergeFields produced an invalid result: docCount is " + numDocs) + " but only saw ") + (numDocsWritten)) + " file=") + (out.toString())) + "; now aborting this merge to prevent index corruption"));
		}
		write(SimpleTextStoredFieldsWriter.END);
		newLine();
	}

	@Override
	public void close() throws IOException {
		try {
			IOUtils.close(out);
		} finally {
			out = null;
		}
	}

	private void write(String s) throws IOException {
	}

	private void write(BytesRef bytes) throws IOException {
	}

	private void newLine() throws IOException {
	}
}

