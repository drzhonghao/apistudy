

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.lucene.spatial3d.geom.Bounds;
import org.apache.lucene.spatial3d.geom.DistanceStyle;
import org.apache.lucene.spatial3d.geom.GeoBBox;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.apache.lucene.spatial3d.geom.SidedPlane;
import org.apache.lucene.spatial3d.geom.Vector;


class GeoNorthLatitudeZone {
	protected final double bottomLat;

	protected final double cosBottomLat;

	protected final SidedPlane bottomPlane;

	protected final GeoPoint interiorPoint;

	protected static final GeoPoint[] planePoints = new GeoPoint[0];

	protected final GeoPoint bottomBoundaryPoint;

	protected final GeoPoint[] edgePoints;

	public GeoNorthLatitudeZone(final PlanetModel planetModel, final double bottomLat) {
		this.bottomLat = bottomLat;
		final double sinBottomLat = Math.sin(bottomLat);
		this.cosBottomLat = Math.cos(bottomLat);
		final double middleLat = (((Math.PI) * 0.5) + bottomLat) * 0.5;
		final double sinMiddleLat = Math.sin(middleLat);
		this.interiorPoint = new GeoPoint(planetModel, sinMiddleLat, 0.0, Math.sqrt((1.0 - (sinMiddleLat * sinMiddleLat))), 1.0);
		this.bottomBoundaryPoint = new GeoPoint(planetModel, sinBottomLat, 0.0, Math.sqrt((1.0 - (sinBottomLat * sinBottomLat))), 1.0);
		this.bottomPlane = new SidedPlane(interiorPoint, planetModel, sinBottomLat);
		this.edgePoints = new GeoPoint[]{ bottomBoundaryPoint };
	}

	public GeoNorthLatitudeZone(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, bottomLat);
	}

	public GeoBBox expand(final double angle) {
		final double newTopLat = (Math.PI) * 0.5;
		final double newBottomLat = (bottomLat) - angle;
		return null;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		return bottomPlane.isWithin(x, y, z);
	}

	public double getRadius() {
		if ((bottomLat) < 0.0)
			return Math.PI;

		double maxCosLat = cosBottomLat;
		return maxCosLat * (Math.PI);
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
		return geoShape.intersects(bottomPlane, GeoNorthLatitudeZone.planePoints);
	}

	public void getBounds(Bounds bounds) {
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		return 0d;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoNorthLatitudeZone))
			return false;

		GeoNorthLatitudeZone other = ((GeoNorthLatitudeZone) (o));
		return (super.equals(other)) && (other.bottomBoundaryPoint.equals(bottomBoundaryPoint));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (bottomBoundaryPoint.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

