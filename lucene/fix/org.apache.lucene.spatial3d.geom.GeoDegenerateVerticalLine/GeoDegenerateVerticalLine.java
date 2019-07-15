

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
import org.apache.lucene.spatial3d.geom.Vector;


public class GeoDegenerateVerticalLine {
	protected final double topLat;

	protected final double bottomLat;

	protected final double longitude;

	protected final GeoPoint UHC;

	protected final GeoPoint LHC;

	protected final SidedPlane topPlane;

	protected final SidedPlane bottomPlane;

	protected final SidedPlane boundingPlane;

	protected final Plane plane;

	protected final GeoPoint[] planePoints;

	protected final GeoPoint centerPoint;

	protected final GeoPoint[] edgePoints;

	public GeoDegenerateVerticalLine(final PlanetModel planetModel, final double topLat, final double bottomLat, final double longitude) {
		if ((topLat > ((Math.PI) * 0.5)) || (topLat < ((-(Math.PI)) * 0.5)))
			throw new IllegalArgumentException("Top latitude out of range");

		if ((bottomLat > ((Math.PI) * 0.5)) || (bottomLat < ((-(Math.PI)) * 0.5)))
			throw new IllegalArgumentException("Bottom latitude out of range");

		if (topLat < bottomLat)
			throw new IllegalArgumentException("Top latitude less than bottom latitude");

		if ((longitude < (-(Math.PI))) || (longitude > (Math.PI)))
			throw new IllegalArgumentException("Longitude out of range");

		this.topLat = topLat;
		this.bottomLat = bottomLat;
		this.longitude = longitude;
		final double sinTopLat = Math.sin(topLat);
		final double cosTopLat = Math.cos(topLat);
		final double sinBottomLat = Math.sin(bottomLat);
		final double cosBottomLat = Math.cos(bottomLat);
		final double sinLongitude = Math.sin(longitude);
		final double cosLongitude = Math.cos(longitude);
		this.UHC = new GeoPoint(planetModel, sinTopLat, sinLongitude, cosTopLat, cosLongitude, topLat, longitude);
		this.LHC = new GeoPoint(planetModel, sinBottomLat, sinLongitude, cosBottomLat, cosLongitude, bottomLat, longitude);
		this.plane = new Plane(cosLongitude, sinLongitude);
		final double middleLat = (topLat + bottomLat) * 0.5;
		final double sinMiddleLat = Math.sin(middleLat);
		final double cosMiddleLat = Math.cos(middleLat);
		this.centerPoint = new GeoPoint(planetModel, sinMiddleLat, sinLongitude, cosMiddleLat, cosLongitude);
		this.topPlane = new SidedPlane(centerPoint, planetModel, sinTopLat);
		this.bottomPlane = new SidedPlane(centerPoint, planetModel, sinBottomLat);
		this.boundingPlane = new SidedPlane(centerPoint, (-sinLongitude), cosLongitude);
		this.planePoints = new GeoPoint[]{ UHC, LHC };
		this.edgePoints = new GeoPoint[]{ centerPoint };
	}

	public GeoDegenerateVerticalLine(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, topLat);
		SerializableObject.writeDouble(outputStream, bottomLat);
		SerializableObject.writeDouble(outputStream, longitude);
	}

	public GeoBBox expand(final double angle) {
		final double newTopLat = (topLat) + angle;
		final double newBottomLat = (bottomLat) - angle;
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
		return (((plane.evaluateIsZero(x, y, z)) && (boundingPlane.isWithin(x, y, z))) && (topPlane.isWithin(x, y, z))) && (bottomPlane.isWithin(x, y, z));
	}

	public double getRadius() {
		final double topAngle = centerPoint.arcDistance(UHC);
		final double bottomAngle = centerPoint.arcDistance(LHC);
		return Math.max(topAngle, bottomAngle);
	}

	public GeoPoint getCenter() {
		return centerPoint;
	}

	public GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public boolean intersects(final Plane p, final GeoPoint[] notablePoints, final Membership... bounds) {
		return false;
	}

	public boolean intersects(final GeoShape geoShape) {
		return geoShape.intersects(plane, planePoints, boundingPlane, topPlane, bottomPlane);
	}

	public void getBounds(Bounds bounds) {
	}

	public int getRelationship(final GeoShape path) {
		if (intersects(path)) {
			return GeoArea.OVERLAPS;
		}
		if (path.isWithin(centerPoint)) {
			return GeoArea.CONTAINS;
		}
		return GeoArea.DISJOINT;
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		final double UHCDistance = distanceStyle.computeDistance(UHC, x, y, z);
		final double LHCDistance = distanceStyle.computeDistance(LHC, x, y, z);
		return 0d;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoDegenerateVerticalLine))
			return false;

		GeoDegenerateVerticalLine other = ((GeoDegenerateVerticalLine) (o));
		return ((super.equals(other)) && (other.UHC.equals(UHC))) && (other.LHC.equals(LHC));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (UHC.hashCode());
		result = (31 * result) + (LHC.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return ((((((((((("GeoDegenerateVerticalLine: {longitude=" + (longitude)) + "(") + (((longitude) * 180.0) / (Math.PI))) + "), toplat=") + (topLat)) + "(") + (((topLat) * 180.0) / (Math.PI))) + "), bottomlat=") + (bottomLat)) + "(") + (((bottomLat) * 180.0) / (Math.PI))) + ")}";
	}
}

