

import org.apache.lucene.geo.GeoUtils;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.SortField;

import static org.apache.lucene.search.SortField.Type.CUSTOM;


final class LatLonPointSortField extends SortField {
	final double latitude;

	final double longitude;

	LatLonPointSortField(String field, double latitude, double longitude) {
		super(field, CUSTOM);
		if (field == null) {
			throw new IllegalArgumentException("field must not be null");
		}
		GeoUtils.checkLatitude(latitude);
		GeoUtils.checkLongitude(longitude);
		this.latitude = latitude;
		this.longitude = longitude;
		setMissingValue(Double.POSITIVE_INFINITY);
	}

	@Override
	public FieldComparator<?> getComparator(int numHits, int sortPos) {
		return null;
	}

	@Override
	public Double getMissingValue() {
		return ((Double) (super.getMissingValue()));
	}

	@Override
	public void setMissingValue(Object missingValue) {
		if ((Double.valueOf(Double.POSITIVE_INFINITY).equals(missingValue)) == false) {
			throw new IllegalArgumentException(("Missing value can only be Double.POSITIVE_INFINITY (missing values last), but got " + missingValue));
		}
		this.missingValue = missingValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(latitude);
		result = (prime * result) + ((int) (temp ^ (temp >>> 32)));
		temp = Double.doubleToLongBits(longitude);
		result = (prime * result) + ((int) (temp ^ (temp >>> 32)));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ((this) == obj)
			return true;

		if (!(super.equals(obj)))
			return false;

		if ((getClass()) != (obj.getClass()))
			return false;

		LatLonPointSortField other = ((LatLonPointSortField) (obj));
		if ((Double.doubleToLongBits(latitude)) != (Double.doubleToLongBits(other.latitude)))
			return false;

		if ((Double.doubleToLongBits(longitude)) != (Double.doubleToLongBits(other.longitude)))
			return false;

		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("<distance:");
		builder.append('"');
		builder.append(getField());
		builder.append('"');
		builder.append(" latitude=");
		builder.append(latitude);
		builder.append(" longitude=");
		builder.append(longitude);
		if ((Double.POSITIVE_INFINITY) != (getMissingValue())) {
			builder.append((" missingValue=" + (getMissingValue())));
		}
		builder.append('>');
		return builder.toString();
	}
}

