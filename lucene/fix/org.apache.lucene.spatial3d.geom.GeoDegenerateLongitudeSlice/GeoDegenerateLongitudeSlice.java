

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.lucene.spatial3d.geom.Bounds;
import org.apache.lucene.spatial3d.geom.DistanceStyle;
import org.apache.lucene.spatial3d.geom.GeoArea;
import org.apache.lucene.spatial3d.geom.GeoBBox;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.apache.lucene.spatial3d.geom.SidedPlane;


class GeoDegenerateLongitudeSlice {
	protected final double longitude;

	protected final SidedPlane boundingPlane;

	protected final Plane plane;

	protected final GeoPoint interiorPoint;

	protected final GeoPoint[] edgePoints;

	protected final GeoPoint[] planePoints;

	public GeoDegenerateLongitudeSlice(final PlanetModel planetModel, final double longitude) {
		if ((longitude < (-(Math.PI))) || (longitude > (Math.PI)))
			throw new IllegalArgumentException("Longitude out of range");

		this.longitude = longitude;
		final double sinLongitude = Math.sin(longitude);
		final double cosLongitude = Math.cos(longitude);
		this.plane = new Plane(cosLongitude, sinLongitude);
		this.interiorPoint = new GeoPoint(planetModel, 0.0, sinLongitude, 1.0, cosLongitude);
		this.boundingPlane = new SidedPlane(interiorPoint, (-sinLongitude), cosLongitude);
		this.edgePoints = new GeoPoint[]{ interiorPoint };
		this.planePoints = new GeoPoint[]{ planetModel.NORTH_POLE, planetModel.SOUTH_POLE };
	}

	public GeoDegenerateLongitudeSlice(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, longitude);
	}

	public GeoBBox expand(final double angle) {
		double newLeftLon = (longitude) - angle;
		double newRightLon = (longitude) + angle;
		double currentLonSpan = 2.0 * angle;
		if ((currentLonSpan + (2.0 * angle)) >= ((Math.PI) * 2.0)) {
			newLeftLon = -(Math.PI);
			newRightLon = Math.PI;
		}
		return null;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		return (plane.evaluateIsZero(x, y, z)) && (boundingPlane.isWithin(x, y, z));
	}

	public double getRadius() {
		return (Math.PI) * 0.5;
	}

	public GeoPoint getCenter() {
		return interiorPoint;
	}

	public GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public boolean intersects(final Plane p, final GeoPoint[] notablePoints, final Membership... bounds) {
		return false;
	}

	public boolean intersects(final GeoShape geoShape) {
		return geoShape.intersects(plane, planePoints, boundingPlane);
	}

	public void getBounds(Bounds bounds) {
	}

	public int getRelationship(final GeoShape path) {
		if (intersects(path))
			return GeoArea.OVERLAPS;

		if (path.isWithin(interiorPoint))
			return GeoArea.CONTAINS;

		return GeoArea.DISJOINT;
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		return 0d;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoDegenerateLongitudeSlice))
			return false;

		GeoDegenerateLongitudeSlice other = ((GeoDegenerateLongitudeSlice) (o));
		return (super.equals(other)) && ((other.longitude) == (longitude));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		long temp = Double.doubleToLongBits(longitude);
		result = (result * 31) + ((int) (temp ^ (temp >>> 32)));
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

