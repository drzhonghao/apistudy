

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.spatial3d.geom.Bounds;
import org.apache.lucene.spatial3d.geom.DistanceStyle;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.apache.lucene.spatial3d.geom.SidedPlane;
import org.apache.lucene.spatial3d.geom.Vector;


class GeoExactCircle {
	protected final GeoPoint center;

	protected final double radius;

	protected final double actualAccuracy;

	protected final GeoPoint[] edgePoints;

	protected final List<GeoExactCircle.CircleSlice> circleSlices;

	public GeoExactCircle(final PlanetModel planetModel, final double lat, final double lon, final double radius, final double accuracy) {
		if ((lat < ((-(Math.PI)) * 0.5)) || (lat > ((Math.PI) * 0.5)))
			throw new IllegalArgumentException("Latitude out of bounds");

		if ((lon < (-(Math.PI))) || (lon > (Math.PI)))
			throw new IllegalArgumentException("Longitude out of bounds");

		if (radius < 0.0)
			throw new IllegalArgumentException("Radius out of bounds");

		if (radius < (Vector.MINIMUM_RESOLUTION))
			throw new IllegalArgumentException("Radius cannot be effectively zero");

		if (((planetModel.minimumPoleDistance) - radius) < (Vector.MINIMUM_RESOLUTION))
			throw new IllegalArgumentException((("Radius out of bounds. It cannot be bigger than " + (planetModel.minimumPoleDistance)) + " for this planet model"));

		this.center = new GeoPoint(planetModel, lat, lon);
		this.radius = radius;
		if (accuracy < (Vector.MINIMUM_RESOLUTION)) {
			actualAccuracy = Vector.MINIMUM_RESOLUTION;
		}else {
			actualAccuracy = accuracy;
		}
		final List<GeoExactCircle.ApproximationSlice> slices = new ArrayList<>(100);
		final GeoPoint northPoint = planetModel.surfacePointOnBearing(center, radius, 0.0);
		final GeoPoint southPoint = planetModel.surfacePointOnBearing(center, radius, Math.PI);
		final GeoPoint eastPoint = planetModel.surfacePointOnBearing(center, radius, ((Math.PI) * 0.5));
		final GeoPoint westPoint = planetModel.surfacePointOnBearing(center, radius, ((Math.PI) * 1.5));
		final GeoPoint edgePoint;
		if ((planetModel.c) > (planetModel.ab)) {
			slices.add(new GeoExactCircle.ApproximationSlice(center, eastPoint, ((Math.PI) * 0.5), westPoint, ((Math.PI) * (-0.5)), northPoint, 0.0, true));
			slices.add(new GeoExactCircle.ApproximationSlice(center, westPoint, ((Math.PI) * 1.5), eastPoint, ((Math.PI) * 0.5), southPoint, Math.PI, true));
			edgePoint = eastPoint;
		}else {
			slices.add(new GeoExactCircle.ApproximationSlice(center, northPoint, 0.0, southPoint, Math.PI, eastPoint, ((Math.PI) * 0.5), true));
			slices.add(new GeoExactCircle.ApproximationSlice(center, southPoint, Math.PI, northPoint, ((Math.PI) * 2.0), westPoint, ((Math.PI) * 1.5), true));
			edgePoint = northPoint;
		}
		this.circleSlices = new ArrayList<>();
		while ((slices.size()) > 0) {
			final GeoExactCircle.ApproximationSlice thisSlice = slices.remove(((slices.size()) - 1));
			final double interpPoint1Bearing = ((thisSlice.point1Bearing) + (thisSlice.middlePointBearing)) * 0.5;
			final GeoPoint interpPoint1 = planetModel.surfacePointOnBearing(center, radius, interpPoint1Bearing);
			final double interpPoint2Bearing = ((thisSlice.point2Bearing) + (thisSlice.middlePointBearing)) * 0.5;
			final GeoPoint interpPoint2 = planetModel.surfacePointOnBearing(center, radius, interpPoint2Bearing);
			if (((!(thisSlice.mustSplit)) && ((Math.abs(thisSlice.plane.evaluate(interpPoint1))) < (actualAccuracy))) && ((Math.abs(thisSlice.plane.evaluate(interpPoint2))) < (actualAccuracy))) {
				circleSlices.add(new GeoExactCircle.CircleSlice(thisSlice.plane, thisSlice.endPoint1, thisSlice.endPoint2, center, thisSlice.middlePoint));
			}else {
				slices.add(new GeoExactCircle.ApproximationSlice(center, thisSlice.endPoint1, thisSlice.point1Bearing, thisSlice.middlePoint, thisSlice.middlePointBearing, interpPoint1, interpPoint1Bearing, false));
				slices.add(new GeoExactCircle.ApproximationSlice(center, thisSlice.middlePoint, thisSlice.middlePointBearing, thisSlice.endPoint2, thisSlice.point2Bearing, interpPoint2, interpPoint2Bearing, false));
			}
		} 
		this.edgePoints = new GeoPoint[]{ edgePoint };
	}

	public GeoExactCircle(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, center.getLatitude());
		SerializableObject.writeDouble(outputStream, center.getLongitude());
		SerializableObject.writeDouble(outputStream, radius);
		SerializableObject.writeDouble(outputStream, actualAccuracy);
	}

	public double getRadius() {
		return radius;
	}

	public GeoPoint getCenter() {
		return center;
	}

	protected double distance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		return distanceStyle.computeDistance(this.center, x, y, z);
	}

	protected void distanceBounds(final Bounds bounds, final DistanceStyle distanceStyle, final double distanceValue) {
		getBounds(bounds);
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		double outsideDistance = Double.POSITIVE_INFINITY;
		for (final GeoExactCircle.CircleSlice slice : circleSlices) {
		}
		return outsideDistance;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		for (final GeoExactCircle.CircleSlice slice : circleSlices) {
			if (((slice.circlePlane.isWithin(x, y, z)) && (slice.plane1.isWithin(x, y, z))) && (slice.plane2.isWithin(x, y, z))) {
				return true;
			}
		}
		return false;
	}

	public GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public boolean intersects(final Plane p, final GeoPoint[] notablePoints, final Membership... bounds) {
		for (final GeoExactCircle.CircleSlice slice : circleSlices) {
		}
		return false;
	}

	public boolean intersects(GeoShape geoShape) {
		for (final GeoExactCircle.CircleSlice slice : circleSlices) {
			if (geoShape.intersects(slice.circlePlane, slice.notableEdgePoints, slice.plane1, slice.plane2)) {
				return true;
			}
		}
		return false;
	}

	public void getBounds(Bounds bounds) {
		bounds.addPoint(center);
		for (final GeoExactCircle.CircleSlice slice : circleSlices) {
			for (final GeoPoint point : slice.notableEdgePoints) {
				bounds.addPoint(point);
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoExactCircle))
			return false;

		GeoExactCircle other = ((GeoExactCircle) (o));
		return (((super.equals(other)) && (other.center.equals(center))) && ((other.radius) == (radius))) && ((other.actualAccuracy) == (actualAccuracy));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (center.hashCode());
		long temp = Double.doubleToLongBits(radius);
		result = (31 * result) + ((int) (temp ^ (temp >>> 32)));
		temp = Double.doubleToLongBits(actualAccuracy);
		result = (31 * result) + ((int) (temp ^ (temp >>> 32)));
		return result;
	}

	@Override
	public String toString() {
		return null;
	}

	protected static class ApproximationSlice {
		public final SidedPlane plane;

		public final GeoPoint endPoint1;

		public final double point1Bearing;

		public final GeoPoint endPoint2;

		public final double point2Bearing;

		public final GeoPoint middlePoint;

		public final double middlePointBearing;

		public final boolean mustSplit;

		public ApproximationSlice(final GeoPoint center, final GeoPoint endPoint1, final double point1Bearing, final GeoPoint endPoint2, final double point2Bearing, final GeoPoint middlePoint, final double middlePointBearing, final boolean mustSplit) {
			this.endPoint1 = endPoint1;
			this.point1Bearing = point1Bearing;
			this.endPoint2 = endPoint2;
			this.point2Bearing = point2Bearing;
			this.middlePoint = middlePoint;
			this.middlePointBearing = middlePointBearing;
			this.mustSplit = mustSplit;
			this.plane = SidedPlane.constructNormalizedThreePointSidedPlane(center, endPoint1, endPoint2, middlePoint);
			if ((this.plane) == null) {
				throw new IllegalArgumentException(((((((((((("Either circle is too small or accuracy is too high; could not construct a plane with endPoint1=" + endPoint1) + " bearing ") + point1Bearing) + ", endPoint2=") + endPoint2) + " bearing ") + point2Bearing) + ", middle=") + middlePoint) + " bearing ") + middlePointBearing));
			}
			if (this.plane.isWithin((-(center.x)), (-(center.y)), (-(center.z)))) {
				throw new IllegalArgumentException(((((((((((("Could not construct a valid plane for this planet model with endPoint1=" + endPoint1) + " bearing ") + point1Bearing) + ", endPoint2=") + endPoint2) + " bearing ") + point2Bearing) + ", middle=") + middlePoint) + " bearing ") + middlePointBearing));
			}
		}

		@Override
		public String toString() {
			return ((((((((((("{end point 1 = " + (endPoint1)) + " bearing 1 = ") + (point1Bearing)) + " end point 2 = ") + (endPoint2)) + " bearing 2 = ") + (point2Bearing)) + " middle point = ") + (middlePoint)) + " middle bearing = ") + (middlePointBearing)) + "}";
		}
	}

	protected static class CircleSlice {
		final GeoPoint[] notableEdgePoints;

		public final SidedPlane circlePlane;

		public final SidedPlane plane1;

		public final SidedPlane plane2;

		public CircleSlice(SidedPlane circlePlane, GeoPoint endPoint1, GeoPoint endPoint2, GeoPoint center, GeoPoint check) {
			this.circlePlane = circlePlane;
			this.plane1 = new SidedPlane(check, endPoint1, center);
			this.plane2 = new SidedPlane(check, endPoint2, center);
			this.notableEdgePoints = new GeoPoint[]{ endPoint1, endPoint2 };
		}

		@Override
		public String toString() {
			return ((((((("{circle plane = " + (circlePlane)) + " plane 1 = ") + (plane1)) + " plane 2 = ") + (plane2)) + " notable edge points = ") + (notableEdgePoints)) + "}";
		}
	}
}

