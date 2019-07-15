

import java.io.IOException;
import org.apache.lucene.codecs.TermVectorsWriter;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IOUtils;


public class SimpleTextTermVectorsWriter extends TermVectorsWriter {
	static final BytesRef END = new BytesRef("END");

	static final BytesRef DOC = new BytesRef("doc ");

	static final BytesRef NUMFIELDS = new BytesRef("  numfields ");

	static final BytesRef FIELD = new BytesRef("  field ");

	static final BytesRef FIELDNAME = new BytesRef("    name ");

	static final BytesRef FIELDPOSITIONS = new BytesRef("    positions ");

	static final BytesRef FIELDOFFSETS = new BytesRef("    offsets   ");

	static final BytesRef FIELDPAYLOADS = new BytesRef("    payloads  ");

	static final BytesRef FIELDTERMCOUNT = new BytesRef("    numterms ");

	static final BytesRef TERMTEXT = new BytesRef("    term ");

	static final BytesRef TERMFREQ = new BytesRef("      freq ");

	static final BytesRef POSITION = new BytesRef("      position ");

	static final BytesRef PAYLOAD = new BytesRef("        payload ");

	static final BytesRef STARTOFFSET = new BytesRef("        startoffset ");

	static final BytesRef ENDOFFSET = new BytesRef("        endoffset ");

	static final String VECTORS_EXTENSION = "vec";

	private final Directory directory;

	private final String segment;

	private IndexOutput out;

	private int numDocsWritten = 0;

	private final BytesRefBuilder scratch = new BytesRefBuilder();

	private boolean offsets;

	private boolean positions;

	private boolean payloads;

	public SimpleTextTermVectorsWriter(Directory directory, String segment, IOContext context) throws IOException {
		this.directory = directory;
		this.segment = segment;
		boolean success = false;
		try {
			out = directory.createOutput(IndexFileNames.segmentFileName(segment, "", SimpleTextTermVectorsWriter.VECTORS_EXTENSION), context);
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(this);
			}
		}
	}

	@Override
	public void startDocument(int numVectorFields) throws IOException {
		write(SimpleTextTermVectorsWriter.DOC);
		write(Integer.toString(numDocsWritten));
		newLine();
		write(SimpleTextTermVectorsWriter.NUMFIELDS);
		write(Integer.toString(numVectorFields));
		newLine();
		(numDocsWritten)++;
	}

	@Override
	public void startField(FieldInfo info, int numTerms, boolean positions, boolean offsets, boolean payloads) throws IOException {
		write(SimpleTextTermVectorsWriter.FIELD);
		write(Integer.toString(info.number));
		newLine();
		write(SimpleTextTermVectorsWriter.FIELDNAME);
		write(info.name);
		newLine();
		write(SimpleTextTermVectorsWriter.FIELDPOSITIONS);
		write(Boolean.toString(positions));
		newLine();
		write(SimpleTextTermVectorsWriter.FIELDOFFSETS);
		write(Boolean.toString(offsets));
		newLine();
		write(SimpleTextTermVectorsWriter.FIELDPAYLOADS);
		write(Boolean.toString(payloads));
		newLine();
		write(SimpleTextTermVectorsWriter.FIELDTERMCOUNT);
		write(Integer.toString(numTerms));
		newLine();
		this.positions = positions;
		this.offsets = offsets;
		this.payloads = payloads;
	}

	@Override
	public void startTerm(BytesRef term, int freq) throws IOException {
		write(SimpleTextTermVectorsWriter.TERMTEXT);
		write(term);
		newLine();
		write(SimpleTextTermVectorsWriter.TERMFREQ);
		write(Integer.toString(freq));
		newLine();
	}

	@Override
	public void addPosition(int position, int startOffset, int endOffset, BytesRef payload) throws IOException {
		assert (positions) || (offsets);
		if (positions) {
			write(SimpleTextTermVectorsWriter.POSITION);
			write(Integer.toString(position));
			newLine();
			if (payloads) {
				write(SimpleTextTermVectorsWriter.PAYLOAD);
				if (payload != null) {
					assert (payload.length) > 0;
					write(payload);
				}
				newLine();
			}
		}
		if (offsets) {
			write(SimpleTextTermVectorsWriter.STARTOFFSET);
			write(Integer.toString(startOffset));
			newLine();
			write(SimpleTextTermVectorsWriter.ENDOFFSET);
			write(Integer.toString(endOffset));
			newLine();
		}
	}

	@Override
	public void finish(FieldInfos fis, int numDocs) throws IOException {
		if ((numDocsWritten) != numDocs) {
			throw new RuntimeException((((((("mergeVectors produced an invalid result: mergedDocs is " + numDocs) + " but vec numDocs is ") + (numDocsWritten)) + " file=") + (out.toString())) + "; now aborting this merge to prevent index corruption"));
		}
		write(SimpleTextTermVectorsWriter.END);
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

