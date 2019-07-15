

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FutureObjects;
import org.apache.lucene.util.NumericUtils;


public class DoubleRange extends Field {
	public static final int BYTES = Double.BYTES;

	public DoubleRange(String name, final double[] min, final double[] max) {
		super(name, DoubleRange.getType(min.length));
		setRangeValues(min, max);
	}

	private static FieldType getType(int dimensions) {
		if (dimensions > 4) {
			throw new IllegalArgumentException("DoubleRange does not support greater than 4 dimensions");
		}
		FieldType ft = new FieldType();
		ft.setDimensions((dimensions * 2), DoubleRange.BYTES);
		ft.freeze();
		return ft;
	}

	public void setRangeValues(double[] min, double[] max) {
		DoubleRange.checkArgs(min, max);
		if ((((min.length) * 2) != (type.pointDimensionCount())) || (((max.length) * 2) != (type.pointDimensionCount()))) {
			throw new IllegalArgumentException((((((("field (name=" + (name)) + ") uses ") + ((type.pointDimensionCount()) / 2)) + " dimensions; cannot change to (incoming) ") + (min.length)) + " dimensions"));
		}
		final byte[] bytes;
		if ((fieldsData) == null) {
			bytes = new byte[((DoubleRange.BYTES) * 2) * (min.length)];
			fieldsData = new BytesRef(bytes);
		}else {
			bytes = ((BytesRef) (fieldsData)).bytes;
		}
		DoubleRange.verifyAndEncode(min, max, bytes);
	}

	private static void checkArgs(final double[] min, final double[] max) {
		if ((((min == null) || (max == null)) || ((min.length) == 0)) || ((max.length) == 0)) {
			throw new IllegalArgumentException("min/max range values cannot be null or empty");
		}
		if ((min.length) != (max.length)) {
			throw new IllegalArgumentException("min/max ranges must agree");
		}
		if ((min.length) > 4) {
			throw new IllegalArgumentException("DoubleRange does not support greater than 4 dimensions");
		}
	}

	private static byte[] encode(double[] min, double[] max) {
		DoubleRange.checkArgs(min, max);
		byte[] b = new byte[((DoubleRange.BYTES) * 2) * (min.length)];
		DoubleRange.verifyAndEncode(min, max, b);
		return b;
	}

	static void verifyAndEncode(double[] min, double[] max, byte[] bytes) {
		for (int d = 0, i = 0, j = (min.length) * (DoubleRange.BYTES); d < (min.length); ++d , i += DoubleRange.BYTES , j += DoubleRange.BYTES) {
			if (Double.isNaN(min[d])) {
				throw new IllegalArgumentException(((("invalid min value (" + (Double.NaN)) + ")") + " in DoubleRange"));
			}
			if (Double.isNaN(max[d])) {
				throw new IllegalArgumentException(((("invalid max value (" + (Double.NaN)) + ")") + " in DoubleRange"));
			}
			if ((min[d]) > (max[d])) {
				throw new IllegalArgumentException((((("min value (" + (min[d])) + ") is greater than max value (") + (max[d])) + ")"));
			}
			DoubleRange.encode(min[d], bytes, i);
			DoubleRange.encode(max[d], bytes, j);
		}
	}

	private static void encode(double val, byte[] bytes, int offset) {
		NumericUtils.longToSortableBytes(NumericUtils.doubleToSortableLong(val), bytes, offset);
	}

	public double getMin(int dimension) {
		FutureObjects.checkIndex(dimension, ((type.pointDimensionCount()) / 2));
		return DoubleRange.decodeMin(((BytesRef) (fieldsData)).bytes, dimension);
	}

	public double getMax(int dimension) {
		FutureObjects.checkIndex(dimension, ((type.pointDimensionCount()) / 2));
		return DoubleRange.decodeMax(((BytesRef) (fieldsData)).bytes, dimension);
	}

	static double decodeMin(byte[] b, int dimension) {
		int offset = dimension * (DoubleRange.BYTES);
		return NumericUtils.sortableLongToDouble(NumericUtils.sortableBytesToLong(b, offset));
	}

	static double decodeMax(byte[] b, int dimension) {
		int offset = ((b.length) / 2) + (dimension * (DoubleRange.BYTES));
		return NumericUtils.sortableLongToDouble(NumericUtils.sortableBytesToLong(b, offset));
	}

	public static Query newIntersectsQuery(String field, final double[] min, final double[] max) {
		return null;
	}

	public static Query newContainsQuery(String field, final double[] min, final double[] max) {
		return null;
	}

	public static Query newWithinQuery(String field, final double[] min, final double[] max) {
		return null;
	}

	public static Query newCrossesQuery(String field, final double[] min, final double[] max) {
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
		DoubleRange.toString(b, 0);
		for (int d = 0; d < ((type.pointDimensionCount()) / 2); ++d) {
			sb.append(' ');
			sb.append(DoubleRange.toString(b, d));
		}
		sb.append('>');
		return sb.toString();
	}

	private static String toString(byte[] ranges, int dimension) {
		return ((("[" + (Double.toString(DoubleRange.decodeMin(ranges, dimension)))) + " : ") + (Double.toString(DoubleRange.decodeMax(ranges, dimension)))) + "]";
	}
}

