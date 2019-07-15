

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


class GeoWideNorthRectangle {
	protected final double bottomLat;

	protected final double leftLon;

	protected final double rightLon;

	protected final double cosMiddleLat;

	protected final GeoPoint LRHC;

	protected final GeoPoint LLHC;

	protected final SidedPlane bottomPlane;

	protected final SidedPlane leftPlane;

	protected final SidedPlane rightPlane;

	protected final GeoPoint[] bottomPlanePoints;

	protected final GeoPoint[] leftPlanePoints;

	protected final GeoPoint[] rightPlanePoints;

	protected final GeoPoint centerPoint;

	protected final GeoWideNorthRectangle.EitherBound eitherBound;

	protected final GeoPoint[] edgePoints;

	public GeoWideNorthRectangle(final PlanetModel planetModel, final double bottomLat, final double leftLon, double rightLon) {
		if ((bottomLat > ((Math.PI) * 0.5)) || (bottomLat < ((-(Math.PI)) * 0.5)))
			throw new IllegalArgumentException("Bottom latitude out of range");

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

		this.bottomLat = bottomLat;
		this.leftLon = leftLon;
		this.rightLon = rightLon;
		final double sinBottomLat = Math.sin(bottomLat);
		final double cosBottomLat = Math.cos(bottomLat);
		final double sinLeftLon = Math.sin(leftLon);
		final double cosLeftLon = Math.cos(leftLon);
		final double sinRightLon = Math.sin(rightLon);
		final double cosRightLon = Math.cos(rightLon);
		this.LRHC = new GeoPoint(planetModel, sinBottomLat, sinRightLon, cosBottomLat, cosRightLon, bottomLat, rightLon);
		this.LLHC = new GeoPoint(planetModel, sinBottomLat, sinLeftLon, cosBottomLat, cosLeftLon, bottomLat, leftLon);
		final double middleLat = (((Math.PI) * 0.5) + bottomLat) * 0.5;
		final double sinMiddleLat = Math.sin(middleLat);
		this.cosMiddleLat = Math.cos(middleLat);
		while (leftLon > rightLon) {
			rightLon += (Math.PI) * 2.0;
		} 
		final double middleLon = (leftLon + rightLon) * 0.5;
		final double sinMiddleLon = Math.sin(middleLon);
		final double cosMiddleLon = Math.cos(middleLon);
		this.centerPoint = new GeoPoint(planetModel, sinMiddleLat, sinMiddleLon, cosMiddleLat, cosMiddleLon);
		this.bottomPlane = new SidedPlane(centerPoint, planetModel, sinBottomLat);
		this.leftPlane = new SidedPlane(centerPoint, cosLeftLon, sinLeftLon);
		this.rightPlane = new SidedPlane(centerPoint, cosRightLon, sinRightLon);
		this.bottomPlanePoints = new GeoPoint[]{ LLHC, LRHC };
		this.leftPlanePoints = new GeoPoint[]{ planetModel.NORTH_POLE, LLHC };
		this.rightPlanePoints = new GeoPoint[]{ planetModel.NORTH_POLE, LRHC };
		this.eitherBound = new GeoWideNorthRectangle.EitherBound();
		this.edgePoints = new GeoPoint[]{ planetModel.NORTH_POLE };
	}

	public GeoWideNorthRectangle(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, bottomLat);
		SerializableObject.writeDouble(outputStream, leftLon);
		SerializableObject.writeDouble(outputStream, rightLon);
	}

	public GeoBBox expand(final double angle) {
		final double newTopLat = (Math.PI) * 0.5;
		final double newBottomLat = (bottomLat) - angle;
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
		return (bottomPlane.isWithin(x, y, z)) && ((leftPlane.isWithin(x, y, z)) || (rightPlane.isWithin(x, y, z)));
	}

	public double getRadius() {
		final double centerAngle = ((rightLon) - (((rightLon) + (leftLon)) * 0.5)) * (cosMiddleLat);
		final double bottomAngle = centerPoint.arcDistance(LLHC);
		return Math.max(centerAngle, bottomAngle);
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
		return ((geoShape.intersects(bottomPlane, bottomPlanePoints, eitherBound)) || (geoShape.intersects(leftPlane, leftPlanePoints, bottomPlane))) || (geoShape.intersects(rightPlane, rightPlanePoints, bottomPlane));
	}

	public void getBounds(Bounds bounds) {
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		final double LRHCDistance = distanceStyle.computeDistance(LRHC, x, y, z);
		final double LLHCDistance = distanceStyle.computeDistance(LLHC, x, y, z);
		return 0d;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoWideNorthRectangle))
			return false;

		GeoWideNorthRectangle other = ((GeoWideNorthRectangle) (o));
		return ((super.equals(other)) && (other.LLHC.equals(LLHC))) && (other.LRHC.equals(LRHC));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (LLHC.hashCode());
		result = (31 * result) + (LRHC.hashCode());
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
		public boolean isWithin(final Vector v) {
			return (leftPlane.isWithin(v)) || (rightPlane.isWithin(v));
		}

		@Override
		public boolean isWithin(final double x, final double y, final double z) {
			return (leftPlane.isWithin(x, y, z)) || (rightPlane.isWithin(x, y, z));
		}
	}
}

