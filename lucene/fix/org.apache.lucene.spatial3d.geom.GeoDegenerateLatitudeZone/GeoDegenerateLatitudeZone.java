

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


class GeoDegenerateLatitudeZone {
	protected final double latitude;

	protected final double sinLatitude;

	protected final Plane plane;

	protected final GeoPoint interiorPoint;

	protected final GeoPoint[] edgePoints;

	protected static final GeoPoint[] planePoints = new GeoPoint[0];

	public GeoDegenerateLatitudeZone(final PlanetModel planetModel, final double latitude) {
		this.latitude = latitude;
		this.sinLatitude = Math.sin(latitude);
		double cosLatitude = Math.cos(latitude);
		this.plane = new Plane(planetModel, sinLatitude);
		interiorPoint = new GeoPoint(planetModel, sinLatitude, 0.0, cosLatitude, 1.0);
		edgePoints = new GeoPoint[]{ interiorPoint };
	}

	public GeoDegenerateLatitudeZone(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, latitude);
	}

	public GeoBBox expand(final double angle) {
		double newTopLat = (latitude) + angle;
		double newBottomLat = (latitude) - angle;
		return null;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		return (Math.abs((z - (this.sinLatitude)))) < 1.0E-10;
	}

	public double getRadius() {
		return Math.PI;
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
		return geoShape.intersects(plane, GeoDegenerateLatitudeZone.planePoints);
	}

	public void getBounds(Bounds bounds) {
	}

	public int getRelationship(final GeoShape path) {
		if (intersects(path)) {
			return GeoArea.OVERLAPS;
		}
		if (path.isWithin(interiorPoint)) {
			return GeoArea.CONTAINS;
		}
		return GeoArea.DISJOINT;
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		return 0d;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoDegenerateLatitudeZone))
			return false;

		GeoDegenerateLatitudeZone other = ((GeoDegenerateLatitudeZone) (o));
		return (super.equals(other)) && ((other.latitude) == (latitude));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		long temp = Double.doubleToLongBits(latitude);
		result = (31 * result) + ((int) (temp ^ (temp >>> 32)));
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

