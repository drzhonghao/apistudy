

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


class GeoSouthLatitudeZone {
	protected final double topLat;

	protected final double cosTopLat;

	protected final SidedPlane topPlane;

	protected final GeoPoint interiorPoint;

	protected static final GeoPoint[] planePoints = new GeoPoint[0];

	protected final GeoPoint topBoundaryPoint;

	protected final GeoPoint[] edgePoints;

	public GeoSouthLatitudeZone(final PlanetModel planetModel, final double topLat) {
		this.topLat = topLat;
		final double sinTopLat = Math.sin(topLat);
		this.cosTopLat = Math.cos(topLat);
		final double middleLat = (topLat - ((Math.PI) * 0.5)) * 0.5;
		final double sinMiddleLat = Math.sin(middleLat);
		this.interiorPoint = new GeoPoint(planetModel, sinMiddleLat, 0.0, Math.sqrt((1.0 - (sinMiddleLat * sinMiddleLat))), 1.0);
		this.topBoundaryPoint = new GeoPoint(planetModel, sinTopLat, 0.0, Math.sqrt((1.0 - (sinTopLat * sinTopLat))), 1.0);
		this.topPlane = new SidedPlane(interiorPoint, planetModel, sinTopLat);
		this.edgePoints = new GeoPoint[]{ topBoundaryPoint };
	}

	public GeoSouthLatitudeZone(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, topLat);
	}

	public GeoBBox expand(final double angle) {
		final double newTopLat = (topLat) + angle;
		final double newBottomLat = (-(Math.PI)) * 0.5;
		return null;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		return topPlane.isWithin(x, y, z);
	}

	public double getRadius() {
		if ((topLat) > 0.0)
			return Math.PI;

		double maxCosLat = cosTopLat;
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
		return geoShape.intersects(topPlane, GeoSouthLatitudeZone.planePoints);
	}

	public void getBounds(Bounds bounds) {
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		return 0d;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoSouthLatitudeZone))
			return false;

		GeoSouthLatitudeZone other = ((GeoSouthLatitudeZone) (o));
		return (super.equals(other)) && (other.topBoundaryPoint.equals(topBoundaryPoint));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (topBoundaryPoint.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

