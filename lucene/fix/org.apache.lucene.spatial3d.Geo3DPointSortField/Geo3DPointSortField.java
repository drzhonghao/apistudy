

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.SortField;
import org.apache.lucene.spatial3d.geom.GeoDistanceShape;

import static org.apache.lucene.search.SortField.Type.CUSTOM;


final class Geo3DPointSortField extends SortField {
	final GeoDistanceShape distanceShape;

	Geo3DPointSortField(final String field, final GeoDistanceShape distanceShape) {
		super(field, CUSTOM);
		if (field == null) {
			throw new IllegalArgumentException("field must not be null");
		}
		if (distanceShape == null) {
			throw new IllegalArgumentException("distanceShape must not be null");
		}
		this.distanceShape = distanceShape;
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
		temp = distanceShape.hashCode();
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

		final Geo3DPointSortField other = ((Geo3DPointSortField) (obj));
		return distanceShape.equals(other.distanceShape);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("<distanceShape:");
		builder.append('"');
		builder.append(getField());
		builder.append('"');
		builder.append(" shape=");
		builder.append(distanceShape);
		if ((Double.POSITIVE_INFINITY) != (getMissingValue())) {
			builder.append((" missingValue=" + (getMissingValue())));
		}
		builder.append('>');
		return builder.toString();
	}
}

