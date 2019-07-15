

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.geo.GeoUtils;
import org.apache.lucene.geo.Polygon;
import org.apache.lucene.search.Query;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.util.BytesRef;


public final class Geo3DPoint extends Field {
	public static final FieldType TYPE = new FieldType();

	static {
		Geo3DPoint.TYPE.setDimensions(3, Integer.BYTES);
		Geo3DPoint.TYPE.freeze();
	}

	public Geo3DPoint(String name, double latitude, double longitude) {
		super(name, Geo3DPoint.TYPE);
		GeoUtils.checkLatitude(latitude);
		GeoUtils.checkLongitude(longitude);
	}

	public static Query newDistanceQuery(final String field, final double latitude, final double longitude, final double radiusMeters) {
		return null;
	}

	public static Query newBoxQuery(final String field, final double minLatitude, final double maxLatitude, final double minLongitude, final double maxLongitude) {
		return null;
	}

	public static Query newPolygonQuery(final String field, final Polygon... polygons) {
		return null;
	}

	public static Query newLargePolygonQuery(final String field, final Polygon... polygons) {
		return null;
	}

	public static Query newPathQuery(final String field, final double[] pathLatitudes, final double[] pathLongitudes, final double pathWidthMeters) {
		return null;
	}

	public Geo3DPoint(String name, double x, double y, double z) {
		super(name, Geo3DPoint.TYPE);
		fillFieldsData(x, y, z);
	}

	private void fillFieldsData(double x, double y, double z) {
		byte[] bytes = new byte[12];
		Geo3DPoint.encodeDimension(x, bytes, 0);
		Geo3DPoint.encodeDimension(y, bytes, Integer.BYTES);
		Geo3DPoint.encodeDimension(z, bytes, (2 * (Integer.BYTES)));
		fieldsData = new BytesRef(bytes);
	}

	public static void encodeDimension(double value, byte[] bytes, int offset) {
	}

	public static double decodeDimension(byte[] value, int offset) {
		return 0.0;
	}

	public static Query newShapeQuery(String field, GeoShape shape) {
		return null;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append(" <");
		result.append(name);
		result.append(':');
		BytesRef bytes = ((BytesRef) (fieldsData));
		result.append((" x=" + (Geo3DPoint.decodeDimension(bytes.bytes, bytes.offset))));
		result.append((" y=" + (Geo3DPoint.decodeDimension(bytes.bytes, ((bytes.offset) + (Integer.BYTES))))));
		result.append((" z=" + (Geo3DPoint.decodeDimension(bytes.bytes, ((bytes.offset) + (2 * (Integer.BYTES)))))));
		result.append('>');
		return result.toString();
	}
}

