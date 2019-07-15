

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.geo.Polygon;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.PointRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;


public class LatLonPoint extends Field {
	public static final int BYTES = Integer.BYTES;

	public static final FieldType TYPE = new FieldType();

	static {
		LatLonPoint.TYPE.setDimensions(2, Integer.BYTES);
		LatLonPoint.TYPE.freeze();
	}

	public void setLocationValue(double latitude, double longitude) {
		final byte[] bytes;
		if ((fieldsData) == null) {
			bytes = new byte[8];
			fieldsData = new BytesRef(bytes);
		}else {
			bytes = ((BytesRef) (fieldsData)).bytes;
		}
		int latitudeEncoded = GeoEncodingUtils.encodeLatitude(latitude);
		int longitudeEncoded = GeoEncodingUtils.encodeLongitude(longitude);
		NumericUtils.intToSortableBytes(latitudeEncoded, bytes, 0);
		NumericUtils.intToSortableBytes(longitudeEncoded, bytes, Integer.BYTES);
	}

	public LatLonPoint(String name, double latitude, double longitude) {
		super(name, LatLonPoint.TYPE);
		setLocationValue(latitude, longitude);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append(" <");
		result.append(name);
		result.append(':');
		byte[] bytes = ((BytesRef) (fieldsData)).bytes;
		result.append(GeoEncodingUtils.decodeLatitude(bytes, 0));
		result.append(',');
		result.append(GeoEncodingUtils.decodeLongitude(bytes, Integer.BYTES));
		result.append('>');
		return result.toString();
	}

	private static byte[] encode(double latitude, double longitude) {
		byte[] bytes = new byte[2 * (Integer.BYTES)];
		NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLatitude(latitude), bytes, 0);
		NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLongitude(longitude), bytes, Integer.BYTES);
		return bytes;
	}

	private static byte[] encodeCeil(double latitude, double longitude) {
		byte[] bytes = new byte[2 * (Integer.BYTES)];
		NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLatitudeCeil(latitude), bytes, 0);
		NumericUtils.intToSortableBytes(GeoEncodingUtils.encodeLongitudeCeil(longitude), bytes, Integer.BYTES);
		return bytes;
	}

	static void checkCompatible(FieldInfo fieldInfo) {
		if (((fieldInfo.getPointDimensionCount()) != 0) && ((fieldInfo.getPointDimensionCount()) != (LatLonPoint.TYPE.pointDimensionCount()))) {
			throw new IllegalArgumentException((((((("field=\"" + (fieldInfo.name)) + "\" was indexed with numDims=") + (fieldInfo.getPointDimensionCount())) + " but this point type has numDims=") + (LatLonPoint.TYPE.pointDimensionCount())) + ", is the field really a LatLonPoint?"));
		}
		if (((fieldInfo.getPointNumBytes()) != 0) && ((fieldInfo.getPointNumBytes()) != (LatLonPoint.TYPE.pointNumBytes()))) {
			throw new IllegalArgumentException((((((("field=\"" + (fieldInfo.name)) + "\" was indexed with bytesPerDim=") + (fieldInfo.getPointNumBytes())) + " but this point type has bytesPerDim=") + (LatLonPoint.TYPE.pointNumBytes())) + ", is the field really a LatLonPoint?"));
		}
	}

	public static Query newBoxQuery(String field, double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
		if (minLatitude == 90.0) {
			return new MatchNoDocsQuery("LatLonPoint.newBoxQuery with minLatitude=90.0");
		}
		if (minLongitude == 180.0) {
			if (maxLongitude == 180.0) {
				return new MatchNoDocsQuery("LatLonPoint.newBoxQuery with minLongitude=maxLongitude=180.0");
			}else
				if (maxLongitude < minLongitude) {
					minLongitude = -180.0;
				}

		}
		byte[] lower = LatLonPoint.encodeCeil(minLatitude, minLongitude);
		byte[] upper = LatLonPoint.encode(maxLatitude, maxLongitude);
		if (maxLongitude < minLongitude) {
			BooleanQuery.Builder q = new BooleanQuery.Builder();
			byte[] leftOpen = lower.clone();
			NumericUtils.intToSortableBytes(Integer.MIN_VALUE, leftOpen, Integer.BYTES);
			Query left = LatLonPoint.newBoxInternal(field, leftOpen, upper);
			q.add(new BooleanClause(left, SHOULD));
			byte[] rightOpen = upper.clone();
			NumericUtils.intToSortableBytes(Integer.MAX_VALUE, rightOpen, Integer.BYTES);
			Query right = LatLonPoint.newBoxInternal(field, lower, rightOpen);
			q.add(new BooleanClause(right, SHOULD));
			return new ConstantScoreQuery(q.build());
		}else {
			return LatLonPoint.newBoxInternal(field, lower, upper);
		}
	}

	private static Query newBoxInternal(String field, byte[] min, byte[] max) {
		return new PointRangeQuery(field, min, max, 2) {
			@Override
			protected String toString(int dimension, byte[] value) {
				if (dimension == 0) {
					return Double.toString(GeoEncodingUtils.decodeLatitude(value, 0));
				}else
					if (dimension == 1) {
						return Double.toString(GeoEncodingUtils.decodeLongitude(value, 0));
					}else {
						throw new AssertionError();
					}

			}
		};
	}

	public static Query newDistanceQuery(String field, double latitude, double longitude, double radiusMeters) {
		return null;
	}

	public static Query newPolygonQuery(String field, Polygon... polygons) {
		return null;
	}
}

