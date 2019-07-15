

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FutureObjects;
import org.apache.lucene.util.NumericUtils;


public class LongRange extends Field {
	public static final int BYTES = Long.BYTES;

	public LongRange(String name, final long[] min, final long[] max) {
		super(name, LongRange.getType(min.length));
		setRangeValues(min, max);
	}

	private static FieldType getType(int dimensions) {
		if (dimensions > 4) {
			throw new IllegalArgumentException("LongRange does not support greater than 4 dimensions");
		}
		FieldType ft = new FieldType();
		ft.setDimensions((dimensions * 2), LongRange.BYTES);
		ft.freeze();
		return ft;
	}

	public void setRangeValues(long[] min, long[] max) {
		LongRange.checkArgs(min, max);
		if ((((min.length) * 2) != (type.pointDimensionCount())) || (((max.length) * 2) != (type.pointDimensionCount()))) {
			throw new IllegalArgumentException((((((("field (name=" + (name)) + ") uses ") + ((type.pointDimensionCount()) / 2)) + " dimensions; cannot change to (incoming) ") + (min.length)) + " dimensions"));
		}
		final byte[] bytes;
		if ((fieldsData) == null) {
			bytes = new byte[((LongRange.BYTES) * 2) * (min.length)];
			fieldsData = new BytesRef(bytes);
		}else {
			bytes = ((BytesRef) (fieldsData)).bytes;
		}
		LongRange.verifyAndEncode(min, max, bytes);
	}

	private static void checkArgs(final long[] min, final long[] max) {
		if ((((min == null) || (max == null)) || ((min.length) == 0)) || ((max.length) == 0)) {
			throw new IllegalArgumentException("min/max range values cannot be null or empty");
		}
		if ((min.length) != (max.length)) {
			throw new IllegalArgumentException("min/max ranges must agree");
		}
		if ((min.length) > 4) {
			throw new IllegalArgumentException("LongRange does not support greater than 4 dimensions");
		}
	}

	private static byte[] encode(long[] min, long[] max) {
		LongRange.checkArgs(min, max);
		byte[] b = new byte[((LongRange.BYTES) * 2) * (min.length)];
		LongRange.verifyAndEncode(min, max, b);
		return b;
	}

	static void verifyAndEncode(long[] min, long[] max, byte[] bytes) {
		for (int d = 0, i = 0, j = (min.length) * (LongRange.BYTES); d < (min.length); ++d , i += LongRange.BYTES , j += LongRange.BYTES) {
			if (Double.isNaN(min[d])) {
				throw new IllegalArgumentException(((("invalid min value (" + (Double.NaN)) + ")") + " in LongRange"));
			}
			if (Double.isNaN(max[d])) {
				throw new IllegalArgumentException(((("invalid max value (" + (Double.NaN)) + ")") + " in LongRange"));
			}
			if ((min[d]) > (max[d])) {
				throw new IllegalArgumentException((((("min value (" + (min[d])) + ") is greater than max value (") + (max[d])) + ")"));
			}
			LongRange.encode(min[d], bytes, i);
			LongRange.encode(max[d], bytes, j);
		}
	}

	private static void encode(long val, byte[] bytes, int offset) {
		NumericUtils.longToSortableBytes(val, bytes, offset);
	}

	public long getMin(int dimension) {
		FutureObjects.checkIndex(dimension, ((type.pointDimensionCount()) / 2));
		return LongRange.decodeMin(((BytesRef) (fieldsData)).bytes, dimension);
	}

	public long getMax(int dimension) {
		FutureObjects.checkIndex(dimension, ((type.pointDimensionCount()) / 2));
		return LongRange.decodeMax(((BytesRef) (fieldsData)).bytes, dimension);
	}

	static long decodeMin(byte[] b, int dimension) {
		int offset = dimension * (LongRange.BYTES);
		return NumericUtils.sortableBytesToLong(b, offset);
	}

	static long decodeMax(byte[] b, int dimension) {
		int offset = ((b.length) / 2) + (dimension * (LongRange.BYTES));
		return NumericUtils.sortableBytesToLong(b, offset);
	}

	public static Query newIntersectsQuery(String field, final long[] min, final long[] max) {
		return null;
	}

	public static Query newContainsQuery(String field, final long[] min, final long[] max) {
		return null;
	}

	public static Query newWithinQuery(String field, final long[] min, final long[] max) {
		return null;
	}

	public static Query newCrossesQuery(String field, final long[] min, final long[] max) {
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(" <");
		sb.append(name);
		sb.append(':');
		byte[] b = ((BytesRef) (fieldsData)).bytes;
		LongRange.toString(b, 0);
		for (int d = 0; d < ((type.pointDimensionCount()) / 2); ++d) {
			sb.append(' ');
			sb.append(LongRange.toString(b, d));
		}
		sb.append('>');
		return sb.toString();
	}

	private static String toString(byte[] ranges, int dimension) {
		return ((("[" + (Long.toString(LongRange.decodeMin(ranges, dimension)))) + " : ") + (Long.toString(LongRange.decodeMax(ranges, dimension)))) + "]";
	}
}

