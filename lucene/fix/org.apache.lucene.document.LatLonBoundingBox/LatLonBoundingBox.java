

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;


public class LatLonBoundingBox extends Field {
	public static final int BYTES = LatLonPoint.BYTES;

	public LatLonBoundingBox(String name, final double minLat, final double minLon, final double maxLat, final double maxLon) {
		super(name, LatLonBoundingBox.getType(2));
		setRangeValues(minLat, minLon, maxLat, maxLon);
	}

	static FieldType getType(int geoDimensions) {
		FieldType ft = new FieldType();
		ft.setDimensions((geoDimensions * 2), LatLonBoundingBox.BYTES);
		ft.freeze();
		return ft;
	}

	public void setRangeValues(double minLat, double minLon, double maxLat, double maxLon) {
		LatLonBoundingBox.checkArgs(minLat, minLon, maxLat, maxLon);
		final byte[] bytes;
		if ((fieldsData) == null) {
			bytes = new byte[4 * (LatLonBoundingBox.BYTES)];
			fieldsData = new BytesRef(bytes);
		}else {
			bytes = ((BytesRef) (fieldsData)).bytes;
		}
		LatLonBoundingBox.encode(minLat, minLon, bytes, 0);
		LatLonBoundingBox.encode(maxLat, maxLon, bytes, (2 * (LatLonBoundingBox.BYTES)));
	}

	static void checkArgs(final double minLat, final double minLon, final double maxLat, final double maxLon) {
		if (minLon > maxLon) {
			throw new IllegalArgumentException((((("cannot have minLon [" + minLon) + "] exceed maxLon [") + maxLon) + "]."));
		}
		if (minLat > maxLat) {
			throw new IllegalArgumentException((((("cannot have minLat [" + minLat) + "] exceed maxLat [") + maxLat) + "]."));
		}
	}

	public static Query newIntersectsQuery(String field, final double minLat, final double minLon, final double maxLat, final double maxLon) {
		return null;
	}

	public static Query newWithinQuery(String field, final double minLat, final double minLon, final double maxLat, final double maxLon) {
		return null;
	}

	public static Query newContainsQuery(String field, final double minLat, final double minLon, final double maxLat, final double maxLon) {
		return null;
	}

	public static Query newCrossesQuery(String field, final double minLat, final double minLon, final double maxLat, final double maxLon) {
		return null;
	}

	static byte[] encode(double minLat, double minLon, double maxLat, double maxLon) {
		byte[] b = new byte[(LatLonBoundingBox.BYTES) * 4];
		LatLonBoundingBox.encode(minLat, minLon, b, 0);
		LatLonBoundingBox.encode(maxLat, maxLon, b, ((LatLonBoundingBox.BYTES) * 2));
		return b;
	}

	static void encode(double lat, double lon, byte[] result, int offset) {
		if (result == null) {
			result = new byte[(LatLonBoundingBox.BYTES) * 4];
		}
		NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLatitude(lat), result, offset);
		NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLongitude(lon), result, (offset + (LatLonBoundingBox.BYTES)));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(" <");
		sb.append(name);
		sb.append(':');
		sb.append('[');
		byte[] b = ((BytesRef) (fieldsData)).bytes;
		sb.append(LatLonBoundingBox.toString(b, 0));
		sb.append(',');
		sb.append(LatLonBoundingBox.toString(b, 1));
		sb.append(']');
		sb.append('>');
		return sb.toString();
	}

	private static String toString(byte[] ranges, int dimension) {
		double lat;
		double lon;
		switch (dimension) {
			case 0 :
				lat = GeoEncodingUtils.decodeLatitude(ranges, 0);
				lon = GeoEncodingUtils.decodeLongitude(ranges, 4);
				break;
			case 1 :
				lat = GeoEncodingUtils.decodeLatitude(ranges, 8);
				lon = GeoEncodingUtils.decodeLongitude(ranges, 12);
				break;
			default :
				throw new IllegalArgumentException((("invalid dimension [" + dimension) + "] in toString"));
		}
		return (lat + ",") + lon;
	}
}

