

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FutureObjects;
import org.apache.lucene.util.NumericUtils;


public class IntRange extends Field {
	public static final int BYTES = Integer.BYTES;

	public IntRange(String name, final int[] min, final int[] max) {
		super(name, IntRange.getType(min.length));
		setRangeValues(min, max);
	}

	private static FieldType getType(int dimensions) {
		if (dimensions > 4) {
			throw new IllegalArgumentException("IntRange does not support greater than 4 dimensions");
		}
		FieldType ft = new FieldType();
		ft.setDimensions((dimensions * 2), IntRange.BYTES);
		ft.freeze();
		return ft;
	}

	public void setRangeValues(int[] min, int[] max) {
		IntRange.checkArgs(min, max);
		if ((((min.length) * 2) != (type.pointDimensionCount())) || (((max.length) * 2) != (type.pointDimensionCount()))) {
			throw new IllegalArgumentException((((((("field (name=" + (name)) + ") uses ") + ((type.pointDimensionCount()) / 2)) + " dimensions; cannot change to (incoming) ") + (min.length)) + " dimensions"));
		}
		final byte[] bytes;
		if ((fieldsData) == null) {
			bytes = new byte[((IntRange.BYTES) * 2) * (min.length)];
			fieldsData = new BytesRef(bytes);
		}else {
			bytes = ((BytesRef) (fieldsData)).bytes;
		}
		IntRange.verifyAndEncode(min, max, bytes);
	}

	private static void checkArgs(final int[] min, final int[] max) {
		if ((((min == null) || (max == null)) || ((min.length) == 0)) || ((max.length) == 0)) {
			throw new IllegalArgumentException("min/max range values cannot be null or empty");
		}
		if ((min.length) != (max.length)) {
			throw new IllegalArgumentException("min/max ranges must agree");
		}
		if ((min.length) > 4) {
			throw new IllegalArgumentException("IntRange does not support greater than 4 dimensions");
		}
	}

	private static byte[] encode(int[] min, int[] max) {
		IntRange.checkArgs(min, max);
		byte[] b = new byte[((IntRange.BYTES) * 2) * (min.length)];
		IntRange.verifyAndEncode(min, max, b);
		return b;
	}

	static void verifyAndEncode(int[] min, int[] max, byte[] bytes) {
		for (int d = 0, i = 0, j = (min.length) * (IntRange.BYTES); d < (min.length); ++d , i += IntRange.BYTES , j += IntRange.BYTES) {
			if (Double.isNaN(min[d])) {
				throw new IllegalArgumentException(((("invalid min value (" + (Double.NaN)) + ")") + " in IntRange"));
			}
			if (Double.isNaN(max[d])) {
				throw new IllegalArgumentException(((("invalid max value (" + (Double.NaN)) + ")") + " in IntRange"));
			}
			if ((min[d]) > (max[d])) {
				throw new IllegalArgumentException((((("min value (" + (min[d])) + ") is greater than max value (") + (max[d])) + ")"));
			}
			IntRange.encode(min[d], bytes, i);
			IntRange.encode(max[d], bytes, j);
		}
	}

	private static void encode(int val, byte[] bytes, int offset) {
		NumericUtils.intToSortableBytes(val, bytes, offset);
	}

	public int getMin(int dimension) {
		FutureObjects.checkIndex(dimension, ((type.pointDimensionCount()) / 2));
		return IntRange.decodeMin(((BytesRef) (fieldsData)).bytes, dimension);
	}

	public int getMax(int dimension) {
		FutureObjects.checkIndex(dimension, ((type.pointDimensionCount()) / 2));
		return IntRange.decodeMax(((BytesRef) (fieldsData)).bytes, dimension);
	}

	static int decodeMin(byte[] b, int dimension) {
		int offset = dimension * (IntRange.BYTES);
		return NumericUtils.sortableBytesToInt(b, offset);
	}

	static int decodeMax(byte[] b, int dimension) {
		int offset = ((b.length) / 2) + (dimension * (IntRange.BYTES));
		return NumericUtils.sortableBytesToInt(b, offset);
	}

	public static Query newIntersectsQuery(String field, final int[] min, final int[] max) {
		return null;
	}

	public static Query newContainsQuery(String field, final int[] min, final int[] max) {
		return null;
	}

	public static Query newWithinQuery(String field, final int[] min, final int[] max) {
		return null;
	}

	public static Query newCrossesQuery(String field, final int[] min, final int[] max) {
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
		IntRange.toString(b, 0);
		for (int d = 0; d < ((type.pointDimensionCount()) / 2); ++d) {
			sb.append(' ');
			sb.append(IntRange.toString(b, d));
		}
		sb.append('>');
		return sb.toString();
	}

	private static String toString(byte[] ranges, int dimension) {
		return ((("[" + (Integer.toString(IntRange.decodeMin(ranges, dimension)))) + " : ") + (Integer.toString(IntRange.decodeMax(ranges, dimension)))) + "]";
	}
}

