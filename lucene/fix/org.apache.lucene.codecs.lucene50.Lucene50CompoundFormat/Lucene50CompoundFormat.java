

import java.io.IOException;
import java.util.Set;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.CompoundFormat;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;


public final class Lucene50CompoundFormat extends CompoundFormat {
	public Lucene50CompoundFormat() {
	}

	@Override
	public Directory getCompoundReader(Directory dir, SegmentInfo si, IOContext context) throws IOException {
		return null;
	}

	@Override
	public void write(Directory dir, SegmentInfo si, IOContext context) throws IOException {
		String dataFile = IndexFileNames.segmentFileName(si.name, "", Lucene50CompoundFormat.DATA_EXTENSION);
		String entriesFile = IndexFileNames.segmentFileName(si.name, "", Lucene50CompoundFormat.ENTRIES_EXTENSION);
		try (IndexOutput data = dir.createOutput(dataFile, context);IndexOutput entries = dir.createOutput(entriesFile, context)) {
			CodecUtil.writeIndexHeader(data, Lucene50CompoundFormat.DATA_CODEC, Lucene50CompoundFormat.VERSION_CURRENT, si.getId(), "");
			CodecUtil.writeIndexHeader(entries, Lucene50CompoundFormat.ENTRY_CODEC, Lucene50CompoundFormat.VERSION_CURRENT, si.getId(), "");
			entries.writeVInt(si.files().size());
			for (String file : si.files()) {
				long startOffset = data.getFilePointer();
				try (ChecksumIndexInput in = dir.openChecksumInput(file, IOContext.READONCE)) {
					CodecUtil.verifyAndCopyIndexHeader(in, data, si.getId());
					long numBytesToCopy = ((in.length()) - (CodecUtil.footerLength())) - (in.getFilePointer());
					data.copyBytes(in, numBytesToCopy);
					long checksum = CodecUtil.checkFooter(in);
					data.writeInt(CodecUtil.FOOTER_MAGIC);
					data.writeInt(0);
					data.writeLong(checksum);
				}
				long endOffset = data.getFilePointer();
				long length = endOffset - startOffset;
				entries.writeString(IndexFileNames.stripSegmentName(file));
				entries.writeLong(startOffset);
				entries.writeLong(length);
			}
			CodecUtil.writeFooter(data);
			CodecUtil.writeFooter(entries);
		}
	}

	static final String DATA_EXTENSION = "cfs";

	static final String ENTRIES_EXTENSION = "cfe";

	static final String DATA_CODEC = "Lucene50CompoundData";

	static final String ENTRY_CODEC = "Lucene50CompoundEntries";

	static final int VERSION_START = 0;

	static final int VERSION_CURRENT = Lucene50CompoundFormat.VERSION_START;
}

