

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import org.apache.lucene.codecs.CompoundFormat;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.StringHelper;


public class SimpleTextCompoundFormat extends CompoundFormat {
	public SimpleTextCompoundFormat() {
	}

	@Override
	public Directory getCompoundReader(Directory dir, SegmentInfo si, IOContext context) throws IOException {
		String dataFile = IndexFileNames.segmentFileName(si.name, "", SimpleTextCompoundFormat.DATA_EXTENSION);
		final IndexInput in = dir.openInput(dataFile, context);
		BytesRefBuilder scratch = new BytesRefBuilder();
		DecimalFormat df = new DecimalFormat(SimpleTextCompoundFormat.OFFSETPATTERN, DecimalFormatSymbols.getInstance(Locale.ROOT));
		long pos = (((in.length()) - (SimpleTextCompoundFormat.TABLEPOS.length)) - (SimpleTextCompoundFormat.OFFSETPATTERN.length())) - 1;
		in.seek(pos);
		assert StringHelper.startsWith(scratch.get(), SimpleTextCompoundFormat.TABLEPOS);
		long tablePos = -1;
		try {
			tablePos = df.parse(stripPrefix(scratch, SimpleTextCompoundFormat.TABLEPOS)).longValue();
		} catch (ParseException e) {
			throw new CorruptIndexException(("can't parse CFS trailer, got: " + (scratch.get().utf8ToString())), in);
		}
		in.seek(tablePos);
		assert StringHelper.startsWith(scratch.get(), SimpleTextCompoundFormat.TABLE);
		int numEntries = Integer.parseInt(stripPrefix(scratch, SimpleTextCompoundFormat.TABLE));
		final String[] fileNames = new String[numEntries];
		final long[] startOffsets = new long[numEntries];
		final long[] endOffsets = new long[numEntries];
		for (int i = 0; i < numEntries; i++) {
			assert StringHelper.startsWith(scratch.get(), SimpleTextCompoundFormat.TABLENAME);
			fileNames[i] = (si.name) + (IndexFileNames.stripSegmentName(stripPrefix(scratch, SimpleTextCompoundFormat.TABLENAME)));
			if (i > 0) {
				assert (fileNames[i].compareTo(fileNames[(i - 1)])) > 0;
			}
			assert StringHelper.startsWith(scratch.get(), SimpleTextCompoundFormat.TABLESTART);
			startOffsets[i] = Long.parseLong(stripPrefix(scratch, SimpleTextCompoundFormat.TABLESTART));
			assert StringHelper.startsWith(scratch.get(), SimpleTextCompoundFormat.TABLEEND);
			endOffsets[i] = Long.parseLong(stripPrefix(scratch, SimpleTextCompoundFormat.TABLEEND));
		}
		return new Directory() {
			private int getIndex(String name) throws IOException {
				int index = Arrays.binarySearch(fileNames, name);
				if (index < 0) {
					throw new FileNotFoundException((((("No sub-file found (fileName=" + name) + " files: ") + (Arrays.toString(fileNames))) + ")"));
				}
				return index;
			}

			@Override
			public String[] listAll() throws IOException {
				ensureOpen();
				return fileNames.clone();
			}

			@Override
			public long fileLength(String name) throws IOException {
				ensureOpen();
				int index = getIndex(name);
				return (endOffsets[index]) - (startOffsets[index]);
			}

			@Override
			public IndexInput openInput(String name, IOContext context) throws IOException {
				ensureOpen();
				int index = getIndex(name);
				return in.slice(name, startOffsets[index], ((endOffsets[index]) - (startOffsets[index])));
			}

			@Override
			public void close() throws IOException {
				in.close();
			}

			@Override
			public IndexOutput createOutput(String name, IOContext context) {
				throw new UnsupportedOperationException();
			}

			@Override
			public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void sync(Collection<String> names) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void deleteFile(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void rename(String source, String dest) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void syncMetaData() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Lock obtainLock(String name) {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public void write(Directory dir, SegmentInfo si, IOContext context) throws IOException {
		String dataFile = IndexFileNames.segmentFileName(si.name, "", SimpleTextCompoundFormat.DATA_EXTENSION);
		int numFiles = si.files().size();
		String[] names = si.files().toArray(new String[numFiles]);
		Arrays.sort(names);
		long[] startOffsets = new long[numFiles];
		long[] endOffsets = new long[numFiles];
		BytesRefBuilder scratch = new BytesRefBuilder();
		try (IndexOutput out = dir.createOutput(dataFile, context)) {
			for (int i = 0; i < (names.length); i++) {
				startOffsets[i] = out.getFilePointer();
				try (final IndexInput in = dir.openInput(names[i], IOContext.READONCE)) {
					out.copyBytes(in, in.length());
				}
				endOffsets[i] = out.getFilePointer();
			}
			long tocPos = out.getFilePointer();
			for (int i = 0; i < (names.length); i++) {
			}
			DecimalFormat df = new DecimalFormat(SimpleTextCompoundFormat.OFFSETPATTERN, DecimalFormatSymbols.getInstance(Locale.ROOT));
		}
	}

	private String stripPrefix(BytesRefBuilder scratch, BytesRef prefix) {
		return new String(scratch.bytes(), prefix.length, ((scratch.length()) - (prefix.length)), StandardCharsets.UTF_8);
	}

	static final String DATA_EXTENSION = "scf";

	static final BytesRef HEADER = new BytesRef("cfs entry for: ");

	static final BytesRef TABLE = new BytesRef("table of contents, size: ");

	static final BytesRef TABLENAME = new BytesRef("  filename: ");

	static final BytesRef TABLESTART = new BytesRef("    start: ");

	static final BytesRef TABLEEND = new BytesRef("    end: ");

	static final BytesRef TABLEPOS = new BytesRef("table of contents begins at offset: ");

	static final String OFFSETPATTERN;

	static {
		int numDigits = Long.toString(Long.MAX_VALUE).length();
		char[] pattern = new char[numDigits];
		Arrays.fill(pattern, '0');
		OFFSETPATTERN = new String(pattern);
	}
}

