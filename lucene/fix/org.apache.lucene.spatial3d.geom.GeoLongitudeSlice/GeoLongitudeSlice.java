

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


class GeoLongitudeSlice {
	protected final double leftLon;

	protected final double rightLon;

	protected final SidedPlane leftPlane;

	protected final SidedPlane rightPlane;

	protected final GeoPoint[] planePoints;

	protected final GeoPoint centerPoint;

	protected final GeoPoint[] edgePoints;

	public GeoLongitudeSlice(final PlanetModel planetModel, final double leftLon, double rightLon) {
		if ((leftLon < (-(Math.PI))) || (leftLon > (Math.PI)))
			throw new IllegalArgumentException("Left longitude out of range");

		if ((rightLon < (-(Math.PI))) || (rightLon > (Math.PI)))
			throw new IllegalArgumentException("Right longitude out of range");

		double extent = rightLon - leftLon;
		if (extent < 0.0) {
			extent += 2.0 * (Math.PI);
		}
		if (extent > (Math.PI))
			throw new IllegalArgumentException("Width of rectangle too great");

		this.leftLon = leftLon;
		this.rightLon = rightLon;
		final double sinLeftLon = Math.sin(leftLon);
		final double cosLeftLon = Math.cos(leftLon);
		final double sinRightLon = Math.sin(rightLon);
		final double cosRightLon = Math.cos(rightLon);
		while (leftLon > rightLon) {
			rightLon += (Math.PI) * 2.0;
		} 
		final double middleLon = (leftLon + rightLon) * 0.5;
		this.centerPoint = new GeoPoint(planetModel, 0.0, middleLon);
		this.leftPlane = new SidedPlane(centerPoint, cosLeftLon, sinLeftLon);
		this.rightPlane = new SidedPlane(centerPoint, cosRightLon, sinRightLon);
		this.planePoints = new GeoPoint[]{ planetModel.NORTH_POLE, planetModel.SOUTH_POLE };
		this.edgePoints = new GeoPoint[]{ planetModel.NORTH_POLE };
	}

	public GeoLongitudeSlice(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, leftLon);
		SerializableObject.writeDouble(outputStream, rightLon);
	}

	public GeoBBox expand(final double angle) {
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
		return (leftPlane.isWithin(x, y, z)) && (rightPlane.isWithin(x, y, z));
	}

	public double getRadius() {
		double extent = (rightLon) - (leftLon);
		if (extent < 0.0)
			extent += (Math.PI) * 2.0;

		return Math.max(((Math.PI) * 0.5), (extent * 0.5));
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
		return (geoShape.intersects(leftPlane, planePoints, rightPlane)) || (geoShape.intersects(rightPlane, planePoints, leftPlane));
	}

	public void getBounds(Bounds bounds) {
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		return 0d;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoLongitudeSlice))
			return false;

		GeoLongitudeSlice other = ((GeoLongitudeSlice) (o));
		return ((super.equals(other)) && ((other.leftLon) == (leftLon))) && ((other.rightLon) == (rightLon));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		long temp = Double.doubleToLongBits(leftLon);
		result = (31 * result) + ((int) (temp ^ (temp >>> 32)));
		temp = Double.doubleToLongBits(rightLon);
		result = (31 * result) + ((int) (temp ^ (temp >>> 32)));
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

