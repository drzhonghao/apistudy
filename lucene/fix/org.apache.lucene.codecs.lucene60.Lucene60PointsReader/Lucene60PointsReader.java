

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.PointsReader;
import org.apache.lucene.codecs.lucene60.Lucene60PointsFormat;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.bkd.BKDReader;


public class Lucene60PointsReader extends PointsReader implements Closeable {
	final IndexInput dataIn;

	final SegmentReadState readState;

	final Map<Integer, BKDReader> readers = new HashMap<>();

	public Lucene60PointsReader(SegmentReadState readState) throws IOException {
		this.readState = readState;
		String indexFileName = IndexFileNames.segmentFileName(readState.segmentInfo.name, readState.segmentSuffix, Lucene60PointsFormat.INDEX_EXTENSION);
		Map<Integer, Long> fieldToFileOffset = new HashMap<>();
		try (ChecksumIndexInput indexIn = readState.directory.openChecksumInput(indexFileName, readState.context)) {
			Throwable priorE = null;
			try {
				int count = indexIn.readVInt();
				for (int i = 0; i < count; i++) {
					int fieldNumber = indexIn.readVInt();
					long fp = indexIn.readVLong();
					fieldToFileOffset.put(fieldNumber, fp);
				}
			} catch (Throwable t) {
				priorE = t;
			} finally {
				CodecUtil.checkFooter(indexIn, priorE);
			}
		}
		String dataFileName = IndexFileNames.segmentFileName(readState.segmentInfo.name, readState.segmentSuffix, Lucene60PointsFormat.DATA_EXTENSION);
		boolean success = false;
		dataIn = readState.directory.openInput(dataFileName, readState.context);
		try {
			CodecUtil.retrieveChecksum(dataIn);
			for (Map.Entry<Integer, Long> ent : fieldToFileOffset.entrySet()) {
				int fieldNumber = ent.getKey();
				long fp = ent.getValue();
				dataIn.seek(fp);
				BKDReader reader = new BKDReader(dataIn);
				readers.put(fieldNumber, reader);
			}
			success = true;
		} finally {
			if (success == false) {
				IOUtils.closeWhileHandlingException(this);
			}
		}
	}

	@Override
	public PointValues getValues(String fieldName) {
		FieldInfo fieldInfo = readState.fieldInfos.fieldInfo(fieldName);
		if (fieldInfo == null) {
			throw new IllegalArgumentException((("field=\"" + fieldName) + "\" is unrecognized"));
		}
		if ((fieldInfo.getPointDimensionCount()) == 0) {
			throw new IllegalArgumentException((("field=\"" + fieldName) + "\" did not index point values"));
		}
		return readers.get(fieldInfo.number);
	}

	@Override
	public long ramBytesUsed() {
		long sizeInBytes = 0;
		for (BKDReader reader : readers.values()) {
			sizeInBytes += reader.ramBytesUsed();
		}
		return sizeInBytes;
	}

	@Override
	public Collection<Accountable> getChildResources() {
		List<Accountable> resources = new ArrayList<>();
		for (Map.Entry<Integer, BKDReader> ent : readers.entrySet()) {
			resources.add(Accountables.namedAccountable(readState.fieldInfos.fieldInfo(ent.getKey()).name, ent.getValue()));
		}
		return Collections.unmodifiableList(resources);
	}

	@Override
	public void checkIntegrity() throws IOException {
		CodecUtil.checksumEntireFile(dataIn);
	}

	@Override
	public void close() throws IOException {
		dataIn.close();
		readers.clear();
	}
}

