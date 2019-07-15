

import java.io.IOException;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.util.bkd.BKDWriter;


public abstract class PointValues {
	public static final int MAX_NUM_BYTES = 16;

	public static final int MAX_DIMENSIONS = BKDWriter.MAX_DIMS;

	public static long size(IndexReader reader, String field) throws IOException {
		long size = 0;
		for (LeafReaderContext ctx : reader.leaves()) {
		}
		return size;
	}

	public static int getDocCount(IndexReader reader, String field) throws IOException {
		int count = 0;
		for (LeafReaderContext ctx : reader.leaves()) {
		}
		return count;
	}

	public static byte[] getMinPackedValue(IndexReader reader, String field) throws IOException {
		byte[] minValue = null;
		for (LeafReaderContext ctx : reader.leaves()) {
			if (minValue == null) {
			}else {
			}
		}
		return minValue;
	}

	public static byte[] getMaxPackedValue(IndexReader reader, String field) throws IOException {
		byte[] maxValue = null;
		for (LeafReaderContext ctx : reader.leaves()) {
			if (maxValue == null) {
			}else {
			}
		}
		return maxValue;
	}

	protected PointValues() {
	}

	public enum Relation {

		CELL_INSIDE_QUERY,
		CELL_OUTSIDE_QUERY,
		CELL_CROSSES_QUERY;}

	public interface IntersectVisitor {
		void visit(int docID) throws IOException;

		void visit(int docID, byte[] packedValue) throws IOException;

		PointValues.Relation compare(byte[] minPackedValue, byte[] maxPackedValue);

		default void grow(int count) {
		}
	}

	public abstract void intersect(PointValues.IntersectVisitor visitor) throws IOException;

	public abstract long estimatePointCount(PointValues.IntersectVisitor visitor);

	public abstract byte[] getMinPackedValue() throws IOException;

	public abstract byte[] getMaxPackedValue() throws IOException;

	public abstract int getNumDimensions() throws IOException;

	public abstract int getBytesPerDimension() throws IOException;

	public abstract long size();

	public abstract int getDocCount();
}

