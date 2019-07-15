

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;


public class LatLonDocValuesField extends Field {
	public static final FieldType TYPE = new FieldType();

	static {
		LatLonDocValuesField.TYPE.setDocValuesType(DocValuesType.SORTED_NUMERIC);
		LatLonDocValuesField.TYPE.freeze();
	}

	public LatLonDocValuesField(String name, double latitude, double longitude) {
		super(name, LatLonDocValuesField.TYPE);
		setLocationValue(latitude, longitude);
	}

	public void setLocationValue(double latitude, double longitude) {
		int latitudeEncoded = GeoEncodingUtils.encodeLatitude(latitude);
		int longitudeEncoded = GeoEncodingUtils.encodeLongitude(longitude);
		fieldsData = Long.valueOf(((((long) (latitudeEncoded)) << 32) | (longitudeEncoded & 4294967295L)));
	}

	static void checkCompatible(FieldInfo fieldInfo) {
		if (((fieldInfo.getDocValuesType()) != (DocValuesType.NONE)) && ((fieldInfo.getDocValuesType()) != (LatLonDocValuesField.TYPE.docValuesType()))) {
			throw new IllegalArgumentException((((((("field=\"" + (fieldInfo.name)) + "\" was indexed with docValuesType=") + (fieldInfo.getDocValuesType())) + " but this type has docValuesType=") + (LatLonDocValuesField.TYPE.docValuesType())) + ", is the field really a LatLonDocValuesField?"));
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
		result.append(GeoEncodingUtils.decodeLatitude(((int) (currentValue >> 32))));
		result.append(',');
		result.append(GeoEncodingUtils.decodeLongitude(((int) (currentValue & (-1)))));
		result.append('>');
		return result.toString();
	}

	public static SortField newDistanceSort(String field, double latitude, double longitude) {
		return null;
	}

	public static Query newSlowBoxQuery(String field, double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
		if (minLatitude == 90.0) {
			return new MatchNoDocsQuery("LatLonDocValuesField.newBoxQuery with minLatitude=90.0");
		}
		if (minLongitude == 180.0) {
			if (maxLongitude == 180.0) {
				return new MatchNoDocsQuery("LatLonDocValuesField.newBoxQuery with minLongitude=maxLongitude=180.0");
			}else
				if (maxLongitude < minLongitude) {
					minLongitude = -180.0;
				}

		}
		return null;
	}

	public static Query newSlowDistanceQuery(String field, double latitude, double longitude, double radiusMeters) {
		return null;
	}
}

