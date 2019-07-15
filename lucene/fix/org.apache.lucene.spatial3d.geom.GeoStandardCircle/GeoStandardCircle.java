

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.lucene.spatial3d.geom.Bounds;
import org.apache.lucene.spatial3d.geom.DistanceStyle;
import org.apache.lucene.spatial3d.geom.GeoArea;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.apache.lucene.spatial3d.geom.SidedPlane;
import org.apache.lucene.spatial3d.geom.Vector;


class GeoStandardCircle {
	protected final GeoPoint center;

	protected final double cutoffAngle;

	protected final SidedPlane circlePlane;

	protected final GeoPoint[] edgePoints;

	protected static final GeoPoint[] circlePoints = new GeoPoint[0];

	public GeoStandardCircle(final PlanetModel planetModel, final double lat, final double lon, final double cutoffAngle) {
		if ((lat < ((-(Math.PI)) * 0.5)) || (lat > ((Math.PI) * 0.5)))
			throw new IllegalArgumentException("Latitude out of bounds");

		if ((lon < (-(Math.PI))) || (lon > (Math.PI)))
			throw new IllegalArgumentException("Longitude out of bounds");

		if ((cutoffAngle < 0.0) || (cutoffAngle > (Math.PI)))
			throw new IllegalArgumentException("Cutoff angle out of bounds");

		if (cutoffAngle < (Vector.MINIMUM_RESOLUTION))
			throw new IllegalArgumentException("Cutoff angle cannot be effectively zero");

		this.center = new GeoPoint(planetModel, lat, lon);
		this.cutoffAngle = cutoffAngle;
		double upperLat = lat + cutoffAngle;
		double upperLon = lon;
		if (upperLat > ((Math.PI) * 0.5)) {
			upperLon += Math.PI;
			if (upperLon > (Math.PI))
				upperLon -= 2.0 * (Math.PI);

			upperLat = (Math.PI) - upperLat;
		}
		double lowerLat = lat - cutoffAngle;
		double lowerLon = lon;
		if (lowerLat < ((-(Math.PI)) * 0.5)) {
			lowerLon += Math.PI;
			if (lowerLon > (Math.PI))
				lowerLon -= 2.0 * (Math.PI);

			lowerLat = (-(Math.PI)) - lowerLat;
		}
		final GeoPoint upperPoint = new GeoPoint(planetModel, upperLat, upperLon);
		final GeoPoint lowerPoint = new GeoPoint(planetModel, lowerLat, lowerLon);
		if ((Math.abs((cutoffAngle - (Math.PI)))) < (Vector.MINIMUM_RESOLUTION)) {
			this.circlePlane = null;
			this.edgePoints = new GeoPoint[0];
		}else {
			final Plane normalPlane = Plane.constructNormalizedZPlane(upperPoint, lowerPoint, center);
			this.circlePlane = SidedPlane.constructNormalizedPerpendicularSidedPlane(center, normalPlane, upperPoint, lowerPoint);
			if ((circlePlane) == null)
				throw new IllegalArgumentException(((((("Couldn't construct circle plane, probably too small?  Cutoff angle = " + cutoffAngle) + "; upperPoint = ") + upperPoint) + "; lowerPoint = ") + lowerPoint));

			final GeoPoint recomputedIntersectionPoint = circlePlane.getSampleIntersectionPoint(planetModel, normalPlane);
			if (recomputedIntersectionPoint == null)
				throw new IllegalArgumentException(("Couldn't construct intersection point, probably circle too small?  Plane = " + (circlePlane)));

			this.edgePoints = new GeoPoint[]{ recomputedIntersectionPoint };
		}
	}

	public GeoStandardCircle(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, center.getLatitude());
		SerializableObject.writeDouble(outputStream, center.getLongitude());
		SerializableObject.writeDouble(outputStream, cutoffAngle);
	}

	public double getRadius() {
		return cutoffAngle;
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
		return 0d;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		if ((circlePlane) == null) {
			return true;
		}
		return circlePlane.isWithin(x, y, z);
	}

	public GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public boolean intersects(final Plane p, final GeoPoint[] notablePoints, final Membership... bounds) {
		if ((circlePlane) == null) {
			return false;
		}
		return false;
	}

	public boolean intersects(GeoShape geoShape) {
		if ((circlePlane) == null) {
			return false;
		}
		return geoShape.intersects(circlePlane, GeoStandardCircle.circlePoints);
	}

	public int getRelationship(GeoShape geoShape) {
		if ((circlePlane) == null) {
			if ((geoShape.getEdgePoints().length) > 0) {
				return GeoArea.WITHIN;
			}
			return GeoArea.OVERLAPS;
		}
		return 0;
	}

	public void getBounds(Bounds bounds) {
		if ((circlePlane) == null) {
			return;
		}
		bounds.addPoint(center);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoStandardCircle))
			return false;

		GeoStandardCircle other = ((GeoStandardCircle) (o));
		return ((super.equals(other)) && (other.center.equals(center))) && ((other.cutoffAngle) == (cutoffAngle));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (center.hashCode());
		long temp = Double.doubleToLongBits(cutoffAngle);
		result = (31 * result) + ((int) (temp ^ (temp >>> 32)));
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

