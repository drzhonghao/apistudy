

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FutureObjects;
import org.apache.lucene.util.NumericUtils;


public class FloatRange extends Field {
	public static final int BYTES = Float.BYTES;

	public FloatRange(String name, final float[] min, final float[] max) {
		super(name, FloatRange.getType(min.length));
		setRangeValues(min, max);
	}

	private static FieldType getType(int dimensions) {
		if (dimensions > 4) {
			throw new IllegalArgumentException("FloatRange does not support greater than 4 dimensions");
		}
		FieldType ft = new FieldType();
		ft.setDimensions((dimensions * 2), FloatRange.BYTES);
		ft.freeze();
		return ft;
	}

	public void setRangeValues(float[] min, float[] max) {
		FloatRange.checkArgs(min, max);
		if ((((min.length) * 2) != (type.pointDimensionCount())) || (((max.length) * 2) != (type.pointDimensionCount()))) {
			throw new IllegalArgumentException((((((("field (name=" + (name)) + ") uses ") + ((type.pointDimensionCount()) / 2)) + " dimensions; cannot change to (incoming) ") + (min.length)) + " dimensions"));
		}
		final byte[] bytes;
		if ((fieldsData) == null) {
			bytes = new byte[((FloatRange.BYTES) * 2) * (min.length)];
			fieldsData = new BytesRef(bytes);
		}else {
			bytes = ((BytesRef) (fieldsData)).bytes;
		}
		FloatRange.verifyAndEncode(min, max, bytes);
	}

	private static void checkArgs(final float[] min, final float[] max) {
		if ((((min == null) || (max == null)) || ((min.length) == 0)) || ((max.length) == 0)) {
			throw new IllegalArgumentException("min/max range values cannot be null or empty");
		}
		if ((min.length) != (max.length)) {
			throw new IllegalArgumentException("min/max ranges must agree");
		}
		if ((min.length) > 4) {
			throw new IllegalArgumentException("FloatRange does not support greater than 4 dimensions");
		}
	}

	private static byte[] encode(float[] min, float[] max) {
		FloatRange.checkArgs(min, max);
		byte[] b = new byte[((FloatRange.BYTES) * 2) * (min.length)];
		FloatRange.verifyAndEncode(min, max, b);
		return b;
	}

	static void verifyAndEncode(float[] min, float[] max, byte[] bytes) {
		for (int d = 0, i = 0, j = (min.length) * (FloatRange.BYTES); d < (min.length); ++d , i += FloatRange.BYTES , j += FloatRange.BYTES) {
			if (Double.isNaN(min[d])) {
				throw new IllegalArgumentException(((("invalid min value (" + (Float.NaN)) + ")") + " in FloatRange"));
			}
			if (Double.isNaN(max[d])) {
				throw new IllegalArgumentException(((("invalid max value (" + (Float.NaN)) + ")") + " in FloatRange"));
			}
			if ((min[d]) > (max[d])) {
				throw new IllegalArgumentException((((("min value (" + (min[d])) + ") is greater than max value (") + (max[d])) + ")"));
			}
			FloatRange.encode(min[d], bytes, i);
			FloatRange.encode(max[d], bytes, j);
		}
	}

	private static void encode(float val, byte[] bytes, int offset) {
		NumericUtils.intToSortableBytes(NumericUtils.floatToSortableInt(val), bytes, offset);
	}

	public float getMin(int dimension) {
		FutureObjects.checkIndex(dimension, ((type.pointDimensionCount()) / 2));
		return FloatRange.decodeMin(((BytesRef) (fieldsData)).bytes, dimension);
	}

	public float getMax(int dimension) {
		FutureObjects.checkIndex(dimension, ((type.pointDimensionCount()) / 2));
		return FloatRange.decodeMax(((BytesRef) (fieldsData)).bytes, dimension);
	}

	static float decodeMin(byte[] b, int dimension) {
		int offset = dimension * (FloatRange.BYTES);
		return NumericUtils.sortableIntToFloat(NumericUtils.sortableBytesToInt(b, offset));
	}

	static float decodeMax(byte[] b, int dimension) {
		int offset = ((b.length) / 2) + (dimension * (FloatRange.BYTES));
		return NumericUtils.sortableIntToFloat(NumericUtils.sortableBytesToInt(b, offset));
	}

	public static Query newIntersectsQuery(String field, final float[] min, final float[] max) {
		return null;
	}

	public static Query newContainsQuery(String field, final float[] min, final float[] max) {
		return null;
	}

	public static Query newWithinQuery(String field, final float[] min, final float[] max) {
		return null;
	}

	public static Query newCrossesQuery(String field, final float[] min, final float[] max) {
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
		FloatRange.toString(b, 0);
		for (int d = 0; d < ((type.pointDimensionCount()) / 2); ++d) {
			sb.append(' ');
			sb.append(FloatRange.toString(b, d));
		}
		sb.append('>');
		return sb.toString();
	}

	private static String toString(byte[] ranges, int dimension) {
		return ((("[" + (Float.toString(FloatRange.decodeMin(ranges, dimension)))) + " : ") + (Float.toString(FloatRange.decodeMax(ranges, dimension)))) + "]";
	}
}

