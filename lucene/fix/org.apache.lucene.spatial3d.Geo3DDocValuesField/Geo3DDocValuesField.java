

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.geo.Polygon;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.search.SortField;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.Vector;


public class Geo3DDocValuesField extends Field {
	private static final double inverseMaximumValue = 1.0 / ((double) (2097151));

	private static final double inverseXFactor = ((PlanetModel.WGS84.getMaximumXValue()) - (PlanetModel.WGS84.getMinimumXValue())) * (Geo3DDocValuesField.inverseMaximumValue);

	private static final double inverseYFactor = ((PlanetModel.WGS84.getMaximumYValue()) - (PlanetModel.WGS84.getMinimumYValue())) * (Geo3DDocValuesField.inverseMaximumValue);

	private static final double inverseZFactor = ((PlanetModel.WGS84.getMaximumZValue()) - (PlanetModel.WGS84.getMinimumZValue())) * (Geo3DDocValuesField.inverseMaximumValue);

	private static final double xFactor = 1.0 / (Geo3DDocValuesField.inverseXFactor);

	private static final double yFactor = 1.0 / (Geo3DDocValuesField.inverseYFactor);

	private static final double zFactor = 1.0 / (Geo3DDocValuesField.inverseZFactor);

	private static final double STEP_FUDGE = 10.0;

	private static final double xStep = (Geo3DDocValuesField.inverseXFactor) * (Geo3DDocValuesField.STEP_FUDGE);

	private static final double yStep = (Geo3DDocValuesField.inverseYFactor) * (Geo3DDocValuesField.STEP_FUDGE);

	private static final double zStep = (Geo3DDocValuesField.inverseZFactor) * (Geo3DDocValuesField.STEP_FUDGE);

	public static final FieldType TYPE = new FieldType();

	static {
		Geo3DDocValuesField.TYPE.setDocValuesType(DocValuesType.SORTED_NUMERIC);
		Geo3DDocValuesField.TYPE.freeze();
	}

	public Geo3DDocValuesField(final String name, final GeoPoint point) {
		super(name, Geo3DDocValuesField.TYPE);
		setLocationValue(point);
	}

	public Geo3DDocValuesField(final String name, final double x, final double y, final double z) {
		super(name, Geo3DDocValuesField.TYPE);
		setLocationValue(x, y, z);
	}

	public void setLocationValue(final GeoPoint point) {
		fieldsData = Long.valueOf(Geo3DDocValuesField.encodePoint(point));
	}

	public void setLocationValue(final double x, final double y, final double z) {
		fieldsData = Long.valueOf(Geo3DDocValuesField.encodePoint(x, y, z));
	}

	public static long encodePoint(final GeoPoint point) {
		return Geo3DDocValuesField.encodePoint(point.x, point.y, point.z);
	}

	public static long encodePoint(final double x, final double y, final double z) {
		int XEncoded = Geo3DDocValuesField.encodeX(x);
		int YEncoded = Geo3DDocValuesField.encodeY(y);
		int ZEncoded = Geo3DDocValuesField.encodeZ(z);
		return ((((long) (XEncoded & 2097151)) << 42) | (((long) (YEncoded & 2097151)) << 21)) | ((long) (ZEncoded & 2097151));
	}

	public static GeoPoint decodePoint(final long docValue) {
		return new GeoPoint(Geo3DDocValuesField.decodeX((((int) (docValue >> 42)) & 2097151)), Geo3DDocValuesField.decodeY((((int) (docValue >> 21)) & 2097151)), Geo3DDocValuesField.decodeZ((((int) (docValue)) & 2097151)));
	}

	public static double decodeXValue(final long docValue) {
		return Geo3DDocValuesField.decodeX((((int) (docValue >> 42)) & 2097151));
	}

	public static double decodeYValue(final long docValue) {
		return Geo3DDocValuesField.decodeY((((int) (docValue >> 21)) & 2097151));
	}

	public static double decodeZValue(final long docValue) {
		return Geo3DDocValuesField.decodeZ((((int) (docValue)) & 2097151));
	}

	public static double roundDownX(final double startValue) {
		return startValue - (Geo3DDocValuesField.xStep);
	}

	public static double roundUpX(final double startValue) {
		return startValue + (Geo3DDocValuesField.xStep);
	}

	public static double roundDownY(final double startValue) {
		return startValue - (Geo3DDocValuesField.yStep);
	}

	public static double roundUpY(final double startValue) {
		return startValue + (Geo3DDocValuesField.yStep);
	}

	public static double roundDownZ(final double startValue) {
		return startValue - (Geo3DDocValuesField.zStep);
	}

	public static double roundUpZ(final double startValue) {
		return startValue + (Geo3DDocValuesField.zStep);
	}

	private static int encodeX(final double x) {
		if (x > (PlanetModel.WGS84.getMaximumXValue())) {
			throw new IllegalArgumentException("x value exceeds WGS84 maximum");
		}else
			if (x < (PlanetModel.WGS84.getMinimumXValue())) {
				throw new IllegalArgumentException("x value less than WGS84 minimum");
			}

		return ((int) (Math.floor((((x - (PlanetModel.WGS84.getMinimumXValue())) * (Geo3DDocValuesField.xFactor)) + 0.5))));
	}

	private static double decodeX(final int x) {
		return (x * (Geo3DDocValuesField.inverseXFactor)) + (PlanetModel.WGS84.getMinimumXValue());
	}

	private static int encodeY(final double y) {
		if (y > (PlanetModel.WGS84.getMaximumYValue())) {
			throw new IllegalArgumentException("y value exceeds WGS84 maximum");
		}else
			if (y < (PlanetModel.WGS84.getMinimumYValue())) {
				throw new IllegalArgumentException("y value less than WGS84 minimum");
			}

		return ((int) (Math.floor((((y - (PlanetModel.WGS84.getMinimumYValue())) * (Geo3DDocValuesField.yFactor)) + 0.5))));
	}

	private static double decodeY(final int y) {
		return (y * (Geo3DDocValuesField.inverseYFactor)) + (PlanetModel.WGS84.getMinimumYValue());
	}

	private static int encodeZ(final double z) {
		if (z > (PlanetModel.WGS84.getMaximumZValue())) {
			throw new IllegalArgumentException("z value exceeds WGS84 maximum");
		}else
			if (z < (PlanetModel.WGS84.getMinimumZValue())) {
				throw new IllegalArgumentException("z value less than WGS84 minimum");
			}

		return ((int) (Math.floor((((z - (PlanetModel.WGS84.getMinimumZValue())) * (Geo3DDocValuesField.zFactor)) + 0.5))));
	}

	private static double decodeZ(final int z) {
		return (z * (Geo3DDocValuesField.inverseZFactor)) + (PlanetModel.WGS84.getMinimumZValue());
	}

	static void checkCompatible(FieldInfo fieldInfo) {
		if (((fieldInfo.getDocValuesType()) != (DocValuesType.NONE)) && ((fieldInfo.getDocValuesType()) != (Geo3DDocValuesField.TYPE.docValuesType()))) {
			throw new IllegalArgumentException((((((("field=\"" + (fieldInfo.name)) + "\" was indexed with docValuesType=") + (fieldInfo.getDocValuesType())) + " but this type has docValuesType=") + (Geo3DDocValuesField.TYPE.docValuesType())) + ", is the field really a Geo3DDocValuesField?"));
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append(" <");
		result.append(name);
		result.append(':');
		long currentValue = ((Long) (fieldsData));
		result.append(Geo3DDocValuesField.decodeXValue(currentValue));
		result.append(',');
		result.append(Geo3DDocValuesField.decodeYValue(currentValue));
		result.append(',');
		result.append(Geo3DDocValuesField.decodeZValue(currentValue));
		result.append('>');
		return result.toString();
	}

	public static SortField newDistanceSort(final String field, final double latitude, final double longitude, final double maxRadiusMeters) {
		return null;
	}

	public static SortField newPathSort(final String field, final double[] pathLatitudes, final double[] pathLongitudes, final double pathWidthMeters) {
		return null;
	}

	public static SortField newOutsideDistanceSort(final String field, final double latitude, final double longitude, final double maxRadiusMeters) {
		return null;
	}

	public static SortField newOutsideBoxSort(final String field, final double minLatitude, final double maxLatitude, final double minLongitude, final double maxLongitude) {
		return null;
	}

	public static SortField newOutsidePolygonSort(final String field, final Polygon... polygons) {
		return null;
	}

	public static SortField newOutsideLargePolygonSort(final String field, final Polygon... polygons) {
		return null;
	}

	public static SortField newOutsidePathSort(final String field, final double[] pathLatitudes, final double[] pathLongitudes, final double pathWidthMeters) {
		return null;
	}
}

