

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.codecs.PointsReader;
import org.apache.lucene.codecs.PointsWriter;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;


class SimpleTextPointsWriter extends PointsWriter {
	public static final BytesRef NUM_DIMS = new BytesRef("num dims ");

	public static final BytesRef BYTES_PER_DIM = new BytesRef("bytes per dim ");

	public static final BytesRef MAX_LEAF_POINTS = new BytesRef("max leaf points ");

	public static final BytesRef INDEX_COUNT = new BytesRef("index count ");

	public static final BytesRef BLOCK_COUNT = new BytesRef("block count ");

	public static final BytesRef BLOCK_DOC_ID = new BytesRef("  doc ");

	public static final BytesRef BLOCK_FP = new BytesRef("  block fp ");

	public static final BytesRef BLOCK_VALUE = new BytesRef("  block value ");

	public static final BytesRef SPLIT_COUNT = new BytesRef("split count ");

	public static final BytesRef SPLIT_DIM = new BytesRef("  split dim ");

	public static final BytesRef SPLIT_VALUE = new BytesRef("  split value ");

	public static final BytesRef FIELD_COUNT = new BytesRef("field count ");

	public static final BytesRef FIELD_FP_NAME = new BytesRef("  field fp name ");

	public static final BytesRef FIELD_FP = new BytesRef("  field fp ");

	public static final BytesRef MIN_VALUE = new BytesRef("min value ");

	public static final BytesRef MAX_VALUE = new BytesRef("max value ");

	public static final BytesRef POINT_COUNT = new BytesRef("point count ");

	public static final BytesRef DOC_COUNT = new BytesRef("doc count ");

	public static final BytesRef END = new BytesRef("END");

	private IndexOutput dataOut;

	final BytesRefBuilder scratch = new BytesRefBuilder();

	final SegmentWriteState writeState;

	final Map<String, Long> indexFPs = new HashMap<>();

	public SimpleTextPointsWriter(SegmentWriteState writeState) throws IOException {
		this.writeState = writeState;
	}

	@Override
	public void writeField(FieldInfo fieldInfo, PointsReader reader) throws IOException {
		PointValues values = reader.getValues(fieldInfo.name);
		boolean singleValuePerDoc = (values.size()) == (values.getDocCount());
	}

	@Override
	public void finish() throws IOException {
	}

	@Override
	public void close() throws IOException {
		if ((dataOut) != null) {
			dataOut.close();
			dataOut = null;
		}
	}

	private void write(IndexOutput out, String s) throws IOException {
	}

	private void writeInt(IndexOutput out, int x) throws IOException {
	}

	private void writeLong(IndexOutput out, long x) throws IOException {
	}

	private void write(IndexOutput out, BytesRef b) throws IOException {
	}

	private void newline(IndexOutput out) throws IOException {
	}
}

