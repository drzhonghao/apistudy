

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


class GeoWideDegenerateHorizontalLine {
	protected final double latitude;

	protected final double leftLon;

	protected final double rightLon;

	protected final GeoPoint LHC;

	protected final GeoPoint RHC;

	protected final Plane plane;

	protected final SidedPlane leftPlane;

	protected final SidedPlane rightPlane;

	protected final GeoPoint[] planePoints;

	protected final GeoPoint centerPoint;

	protected final GeoWideDegenerateHorizontalLine.EitherBound eitherBound;

	protected final GeoPoint[] edgePoints;

	public GeoWideDegenerateHorizontalLine(final PlanetModel planetModel, final double latitude, final double leftLon, double rightLon) {
		if ((latitude > ((Math.PI) * 0.5)) || (latitude < ((-(Math.PI)) * 0.5)))
			throw new IllegalArgumentException("Latitude out of range");

		if ((leftLon < (-(Math.PI))) || (leftLon > (Math.PI)))
			throw new IllegalArgumentException("Left longitude out of range");

		if ((rightLon < (-(Math.PI))) || (rightLon > (Math.PI)))
			throw new IllegalArgumentException("Right longitude out of range");

		double extent = rightLon - leftLon;
		if (extent < 0.0) {
			extent += 2.0 * (Math.PI);
		}
		if (extent < (Math.PI))
			throw new IllegalArgumentException("Width of rectangle too small");

		this.latitude = latitude;
		this.leftLon = leftLon;
		this.rightLon = rightLon;
		final double sinLatitude = Math.sin(latitude);
		final double cosLatitude = Math.cos(latitude);
		final double sinLeftLon = Math.sin(leftLon);
		final double cosLeftLon = Math.cos(leftLon);
		final double sinRightLon = Math.sin(rightLon);
		final double cosRightLon = Math.cos(rightLon);
		this.LHC = new GeoPoint(planetModel, sinLatitude, sinLeftLon, cosLatitude, cosLeftLon, latitude, leftLon);
		this.RHC = new GeoPoint(planetModel, sinLatitude, sinRightLon, cosLatitude, cosRightLon, latitude, rightLon);
		this.plane = new Plane(planetModel, sinLatitude);
		while (leftLon > rightLon) {
			rightLon += (Math.PI) * 2.0;
		} 
		double middleLon = (leftLon + rightLon) * 0.5;
		double sinMiddleLon = Math.sin(middleLon);
		double cosMiddleLon = Math.cos(middleLon);
		this.centerPoint = new GeoPoint(planetModel, sinLatitude, sinMiddleLon, cosLatitude, cosMiddleLon);
		this.leftPlane = new SidedPlane(centerPoint, cosLeftLon, sinLeftLon);
		this.rightPlane = new SidedPlane(centerPoint, cosRightLon, sinRightLon);
		this.planePoints = new GeoPoint[]{ LHC, RHC };
		this.eitherBound = new GeoWideDegenerateHorizontalLine.EitherBound();
		this.edgePoints = new GeoPoint[]{ centerPoint };
	}

	public GeoWideDegenerateHorizontalLine(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, latitude);
		SerializableObject.writeDouble(outputStream, leftLon);
		SerializableObject.writeDouble(outputStream, rightLon);
	}

	public GeoBBox expand(final double angle) {
		final double newTopLat = (latitude) + angle;
		final double newBottomLat = (latitude) - angle;
		double currentLonSpan = (rightLon) - (leftLon);
		if (currentLonSpan < 0.0)
			currentLonSpan += (Math.PI) * 2.0;

		double newLeftLon = (leftLon) - angle;
		double newRightLon = (rightLon) + angle;
		if ((currentLonSpan + (2.0 * angle)) >= ((Math.PI) * 2.0)) {
			newLeftLon = -(Math.PI);
			newRightLon = Math.PI;
		}
		return null;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		return (plane.evaluateIsZero(x, y, z)) && ((leftPlane.isWithin(x, y, z)) || (rightPlane.isWithin(x, y, z)));
	}

	public double getRadius() {
		final double topAngle = centerPoint.arcDistance(RHC);
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
		return geoShape.intersects(plane, planePoints, eitherBound);
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
		final double LHCDistance = distanceStyle.computeDistance(LHC, x, y, z);
		final double RHCDistance = distanceStyle.computeDistance(RHC, x, y, z);
		return 0d;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoWideDegenerateHorizontalLine))
			return false;

		GeoWideDegenerateHorizontalLine other = ((GeoWideDegenerateHorizontalLine) (o));
		return ((super.equals(other)) && (other.LHC.equals(LHC))) && (other.RHC.equals(RHC));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (LHC.hashCode());
		result = (31 * result) + (RHC.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return null;
	}

	protected class EitherBound implements Membership {
		public EitherBound() {
		}

		@Override
		public boolean isWithin(final double x, final double y, final double z) {
			return (leftPlane.isWithin(x, y, z)) || (rightPlane.isWithin(x, y, z));
		}
	}
}

