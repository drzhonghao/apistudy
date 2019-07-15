

import org.apache.lucene.spatial3d.geom.Bounds;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.PlanetModel;


public class LatLonBounds implements Bounds {
	private boolean noLongitudeBound = false;

	private boolean noTopLatitudeBound = false;

	private boolean noBottomLatitudeBound = false;

	private Double minLatitude = null;

	private Double maxLatitude = null;

	private Double leftLongitude = null;

	private Double rightLongitude = null;

	public LatLonBounds() {
	}

	public Double getMaxLatitude() {
		return maxLatitude;
	}

	public Double getMinLatitude() {
		return minLatitude;
	}

	public Double getLeftLongitude() {
		return leftLongitude;
	}

	public Double getRightLongitude() {
		return rightLongitude;
	}

	public boolean checkNoLongitudeBound() {
		return noLongitudeBound;
	}

	public boolean checkNoTopLatitudeBound() {
		return noTopLatitudeBound;
	}

	public boolean checkNoBottomLatitudeBound() {
		return noBottomLatitudeBound;
	}

	@Override
	public Bounds addPlane(final PlanetModel planetModel, final Plane plane, final Membership... bounds) {
		return this;
	}

	@Override
	public Bounds addHorizontalPlane(final PlanetModel planetModel, final double latitude, final Plane horizontalPlane, final Membership... bounds) {
		if ((!(noTopLatitudeBound)) || (!(noBottomLatitudeBound))) {
			addLatitudeBound(latitude);
		}
		return this;
	}

	@Override
	public Bounds addVerticalPlane(final PlanetModel planetModel, final double longitude, final Plane verticalPlane, final Membership... bounds) {
		if (!(noLongitudeBound)) {
			addLongitudeBound(longitude);
		}
		return this;
	}

	@Override
	public Bounds isWide() {
		return noLongitudeBound();
	}

	@Override
	public Bounds addXValue(final GeoPoint point) {
		if (!(noLongitudeBound)) {
			addLongitudeBound(point.getLongitude());
		}
		return this;
	}

	@Override
	public Bounds addYValue(final GeoPoint point) {
		if (!(noLongitudeBound)) {
			addLongitudeBound(point.getLongitude());
		}
		return this;
	}

	@Override
	public Bounds addZValue(final GeoPoint point) {
		if ((!(noTopLatitudeBound)) || (!(noBottomLatitudeBound))) {
			double latitude = point.getLatitude();
			addLatitudeBound(latitude);
		}
		return this;
	}

	@Override
	public Bounds addIntersection(final PlanetModel planetModel, final Plane plane1, final Plane plane2, final Membership... bounds) {
		return this;
	}

	@Override
	public Bounds addPoint(GeoPoint point) {
		if (!(noLongitudeBound)) {
			addLongitudeBound(point.getLongitude());
		}
		if ((!(noTopLatitudeBound)) || (!(noBottomLatitudeBound))) {
			addLatitudeBound(point.getLatitude());
		}
		return this;
	}

	@Override
	public Bounds noLongitudeBound() {
		noLongitudeBound = true;
		leftLongitude = null;
		rightLongitude = null;
		return this;
	}

	@Override
	public Bounds noTopLatitudeBound() {
		noTopLatitudeBound = true;
		maxLatitude = null;
		return this;
	}

	@Override
	public Bounds noBottomLatitudeBound() {
		noBottomLatitudeBound = true;
		minLatitude = null;
		return this;
	}

	@Override
	public Bounds noBound(final PlanetModel planetModel) {
		return noLongitudeBound().noTopLatitudeBound().noBottomLatitudeBound();
	}

	private void addLatitudeBound(double latitude) {
		if ((!(noTopLatitudeBound)) && (((maxLatitude) == null) || (latitude > (maxLatitude))))
			maxLatitude = latitude;

		if ((!(noBottomLatitudeBound)) && (((minLatitude) == null) || (latitude < (minLatitude))))
			minLatitude = latitude;

	}

	private void addLongitudeBound(double longitude) {
		if (((leftLongitude) == null) && ((rightLongitude) == null)) {
			leftLongitude = longitude;
			rightLongitude = longitude;
		}else {
			double currentLeftLongitude = leftLongitude;
			double currentRightLongitude = rightLongitude;
			if (currentRightLongitude < currentLeftLongitude)
				currentRightLongitude += 2.0 * (Math.PI);

			if (longitude < currentLeftLongitude)
				longitude += 2.0 * (Math.PI);

			if ((longitude < currentLeftLongitude) || (longitude > currentRightLongitude)) {
				double leftExtensionAmt;
				double rightExtensionAmt;
				if (longitude < currentLeftLongitude) {
					leftExtensionAmt = currentLeftLongitude - longitude;
				}else {
					leftExtensionAmt = (currentLeftLongitude + (2.0 * (Math.PI))) - longitude;
				}
				if (longitude > currentRightLongitude) {
					rightExtensionAmt = longitude - currentRightLongitude;
				}else {
					rightExtensionAmt = (longitude + (2.0 * (Math.PI))) - currentRightLongitude;
				}
				if (leftExtensionAmt < rightExtensionAmt) {
					currentLeftLongitude = (leftLongitude) - leftExtensionAmt;
					while (currentLeftLongitude <= (-(Math.PI))) {
						currentLeftLongitude += 2.0 * (Math.PI);
					} 
					leftLongitude = currentLeftLongitude;
				}else {
					currentRightLongitude = (rightLongitude) + rightExtensionAmt;
					while (currentRightLongitude > (Math.PI)) {
						currentRightLongitude -= 2.0 * (Math.PI);
					} 
					rightLongitude = currentRightLongitude;
				}
			}
		}
		double testRightLongitude = rightLongitude;
		if (testRightLongitude < (leftLongitude))
			testRightLongitude += (Math.PI) * 2.0;

		if ((testRightLongitude - (leftLongitude)) >= (Math.PI)) {
			noLongitudeBound = true;
			leftLongitude = null;
			rightLongitude = null;
		}
	}
}

