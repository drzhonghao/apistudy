

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.MutablePointValues;
import org.apache.lucene.codecs.PointsReader;
import org.apache.lucene.codecs.PointsWriter;
import org.apache.lucene.codecs.lucene60.Lucene60PointsFormat;
import org.apache.lucene.codecs.lucene60.Lucene60PointsReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.bkd.BKDReader;
import org.apache.lucene.util.bkd.BKDWriter;

import static org.apache.lucene.index.PointValues.Relation.CELL_CROSSES_QUERY;


public class Lucene60PointsWriter extends PointsWriter implements Closeable {
	protected final IndexOutput dataOut;

	protected final Map<String, Long> indexFPs = new HashMap<>();

	final SegmentWriteState writeState;

	final int maxPointsInLeafNode;

	final double maxMBSortInHeap;

	private boolean finished;

	public Lucene60PointsWriter(SegmentWriteState writeState, int maxPointsInLeafNode, double maxMBSortInHeap) throws IOException {
		assert writeState.fieldInfos.hasPointValues();
		this.writeState = writeState;
		this.maxPointsInLeafNode = maxPointsInLeafNode;
		this.maxMBSortInHeap = maxMBSortInHeap;
		String dataFileName = IndexFileNames.segmentFileName(writeState.segmentInfo.name, writeState.segmentSuffix, Lucene60PointsFormat.DATA_EXTENSION);
		dataOut = writeState.directory.createOutput(dataFileName, writeState.context);
		boolean success = false;
		try {
			success = true;
		} finally {
			if (success == false) {
				IOUtils.closeWhileHandlingException(dataOut);
			}
		}
	}

	public Lucene60PointsWriter(SegmentWriteState writeState) throws IOException {
		this(writeState, BKDWriter.DEFAULT_MAX_POINTS_IN_LEAF_NODE, BKDWriter.DEFAULT_MAX_MB_SORT_IN_HEAP);
	}

	@Override
	public void writeField(FieldInfo fieldInfo, PointsReader reader) throws IOException {
		PointValues values = reader.getValues(fieldInfo.name);
		boolean singleValuePerDoc = (values.size()) == (values.getDocCount());
		try (BKDWriter writer = new BKDWriter(writeState.segmentInfo.maxDoc(), writeState.directory, writeState.segmentInfo.name, fieldInfo.getPointDimensionCount(), fieldInfo.getPointNumBytes(), maxPointsInLeafNode, maxMBSortInHeap, values.size(), singleValuePerDoc)) {
			if (values instanceof MutablePointValues) {
				final long fp = writer.writeField(dataOut, fieldInfo.name, ((MutablePointValues) (values)));
				if (fp != (-1)) {
					indexFPs.put(fieldInfo.name, fp);
				}
				return;
			}
			values.intersect(new PointValues.IntersectVisitor() {
				@Override
				public void visit(int docID) {
					throw new IllegalStateException();
				}

				public void visit(int docID, byte[] packedValue) throws IOException {
					writer.add(packedValue, docID);
				}

				@Override
				public PointValues.Relation compare(byte[] minPackedValue, byte[] maxPackedValue) {
					return CELL_CROSSES_QUERY;
				}
			});
			if ((writer.getPointCount()) > 0) {
				indexFPs.put(fieldInfo.name, writer.finish(dataOut));
			}
		}
	}

	@Override
	public void merge(MergeState mergeState) throws IOException {
		for (PointsReader reader : mergeState.pointsReaders) {
			if ((reader instanceof Lucene60PointsReader) == false) {
				super.merge(mergeState);
				return;
			}
		}
		for (PointsReader reader : mergeState.pointsReaders) {
			if (reader != null) {
				reader.checkIntegrity();
			}
		}
		for (FieldInfo fieldInfo : mergeState.mergeFieldInfos) {
			if ((fieldInfo.getPointDimensionCount()) != 0) {
				if ((fieldInfo.getPointDimensionCount()) == 1) {
					boolean singleValuePerDoc = true;
					long totMaxSize = 0;
					for (int i = 0; i < (mergeState.pointsReaders.length); i++) {
						PointsReader reader = mergeState.pointsReaders[i];
						if (reader != null) {
							FieldInfos readerFieldInfos = mergeState.fieldInfos[i];
							FieldInfo readerFieldInfo = readerFieldInfos.fieldInfo(fieldInfo.name);
							if ((readerFieldInfo != null) && ((readerFieldInfo.getPointDimensionCount()) > 0)) {
								PointValues values = reader.getValues(fieldInfo.name);
								if (values != null) {
									totMaxSize += values.size();
									singleValuePerDoc &= (values.size()) == (values.getDocCount());
								}
							}
						}
					}
					try (BKDWriter writer = new BKDWriter(writeState.segmentInfo.maxDoc(), writeState.directory, writeState.segmentInfo.name, fieldInfo.getPointDimensionCount(), fieldInfo.getPointNumBytes(), maxPointsInLeafNode, maxMBSortInHeap, totMaxSize, singleValuePerDoc)) {
						List<BKDReader> bkdReaders = new ArrayList<>();
						List<MergeState.DocMap> docMaps = new ArrayList<>();
						for (int i = 0; i < (mergeState.pointsReaders.length); i++) {
							PointsReader reader = mergeState.pointsReaders[i];
							if (reader != null) {
								assert reader instanceof Lucene60PointsReader;
								Lucene60PointsReader reader60 = ((Lucene60PointsReader) (reader));
								FieldInfos readerFieldInfos = mergeState.fieldInfos[i];
								FieldInfo readerFieldInfo = readerFieldInfos.fieldInfo(fieldInfo.name);
								if ((readerFieldInfo != null) && ((readerFieldInfo.getPointDimensionCount()) > 0)) {
								}
							}
						}
						long fp = writer.merge(dataOut, docMaps, bkdReaders);
						if (fp != (-1)) {
							indexFPs.put(fieldInfo.name, fp);
						}
					}
				}else {
					mergeOneField(mergeState, fieldInfo);
				}
			}
		}
		finish();
	}

	@Override
	public void finish() throws IOException {
		if (finished) {
			throw new IllegalStateException("already finished");
		}
		finished = true;
		CodecUtil.writeFooter(dataOut);
		String indexFileName = IndexFileNames.segmentFileName(writeState.segmentInfo.name, writeState.segmentSuffix, Lucene60PointsFormat.INDEX_EXTENSION);
		try (IndexOutput indexOut = writeState.directory.createOutput(indexFileName, writeState.context)) {
			int count = indexFPs.size();
			indexOut.writeVInt(count);
			for (Map.Entry<String, Long> ent : indexFPs.entrySet()) {
				FieldInfo fieldInfo = writeState.fieldInfos.fieldInfo(ent.getKey());
				if (fieldInfo == null) {
					throw new IllegalStateException((("wrote field=\"" + (ent.getKey())) + "\" but that field doesn\'t exist in FieldInfos"));
				}
				indexOut.writeVInt(fieldInfo.number);
				indexOut.writeVLong(ent.getValue());
			}
			CodecUtil.writeFooter(indexOut);
		}
	}

	@Override
	public void close() throws IOException {
		dataOut.close();
	}
}

