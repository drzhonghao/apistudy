

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


class GeoSouthRectangle {
	protected final double topLat;

	protected final double leftLon;

	protected final double rightLon;

	protected final double cosMiddleLat;

	protected final GeoPoint ULHC;

	protected final GeoPoint URHC;

	protected final SidedPlane topPlane;

	protected final SidedPlane leftPlane;

	protected final SidedPlane rightPlane;

	protected final GeoPoint[] topPlanePoints;

	protected final GeoPoint[] leftPlanePoints;

	protected final GeoPoint[] rightPlanePoints;

	protected final GeoPoint centerPoint;

	protected final GeoPoint[] edgePoints;

	public GeoSouthRectangle(final PlanetModel planetModel, final double topLat, final double leftLon, double rightLon) {
		if ((topLat > ((Math.PI) * 0.5)) || (topLat < ((-(Math.PI)) * 0.5)))
			throw new IllegalArgumentException("Top latitude out of range");

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

		this.topLat = topLat;
		this.leftLon = leftLon;
		this.rightLon = rightLon;
		final double sinTopLat = Math.sin(topLat);
		final double cosTopLat = Math.cos(topLat);
		final double sinLeftLon = Math.sin(leftLon);
		final double cosLeftLon = Math.cos(leftLon);
		final double sinRightLon = Math.sin(rightLon);
		final double cosRightLon = Math.cos(rightLon);
		this.ULHC = new GeoPoint(planetModel, sinTopLat, sinLeftLon, cosTopLat, cosLeftLon, topLat, leftLon);
		this.URHC = new GeoPoint(planetModel, sinTopLat, sinRightLon, cosTopLat, cosRightLon, topLat, rightLon);
		final double middleLat = (topLat - ((Math.PI) * 0.5)) * 0.5;
		final double sinMiddleLat = Math.sin(middleLat);
		this.cosMiddleLat = Math.cos(middleLat);
		while (leftLon > rightLon) {
			rightLon += (Math.PI) * 2.0;
		} 
		final double middleLon = (leftLon + rightLon) * 0.5;
		final double sinMiddleLon = Math.sin(middleLon);
		final double cosMiddleLon = Math.cos(middleLon);
		this.centerPoint = new GeoPoint(planetModel, sinMiddleLat, sinMiddleLon, cosMiddleLat, cosMiddleLon);
		this.topPlane = new SidedPlane(centerPoint, planetModel, sinTopLat);
		this.leftPlane = new SidedPlane(centerPoint, cosLeftLon, sinLeftLon);
		this.rightPlane = new SidedPlane(centerPoint, cosRightLon, sinRightLon);
		this.topPlanePoints = new GeoPoint[]{ ULHC, URHC };
		this.leftPlanePoints = new GeoPoint[]{ ULHC, planetModel.SOUTH_POLE };
		this.rightPlanePoints = new GeoPoint[]{ URHC, planetModel.SOUTH_POLE };
		this.edgePoints = new GeoPoint[]{ planetModel.SOUTH_POLE };
	}

	public GeoSouthRectangle(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, topLat);
		SerializableObject.writeDouble(outputStream, leftLon);
		SerializableObject.writeDouble(outputStream, rightLon);
	}

	public GeoBBox expand(final double angle) {
		final double newTopLat = (topLat) + angle;
		final double newBottomLat = (-(Math.PI)) * 0.5;
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
		return ((topPlane.isWithin(x, y, z)) && (leftPlane.isWithin(x, y, z))) && (rightPlane.isWithin(x, y, z));
	}

	public double getRadius() {
		final double centerAngle = ((rightLon) - (((rightLon) + (leftLon)) * 0.5)) * (cosMiddleLat);
		final double topAngle = centerPoint.arcDistance(URHC);
		return Math.max(centerAngle, topAngle);
	}

	public GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public GeoPoint getCenter() {
		return centerPoint;
	}

	public boolean intersects(final Plane p, final GeoPoint[] notablePoints, final Membership... bounds) {
		return false;
	}

	public boolean intersects(final GeoShape geoShape) {
		return ((geoShape.intersects(topPlane, topPlanePoints, leftPlane, rightPlane)) || (geoShape.intersects(leftPlane, leftPlanePoints, rightPlane, topPlane))) || (geoShape.intersects(rightPlane, rightPlanePoints, leftPlane, topPlane));
	}

	public void getBounds(Bounds bounds) {
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		final double ULHCDistance = distanceStyle.computeDistance(ULHC, x, y, z);
		final double URHCDistance = distanceStyle.computeDistance(URHC, x, y, z);
		return 0d;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoSouthRectangle))
			return false;

		GeoSouthRectangle other = ((GeoSouthRectangle) (o));
		return ((super.equals(other)) && (other.ULHC.equals(ULHC))) && (other.URHC.equals(URHC));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (ULHC.hashCode());
		result = (31 * result) + (URHC.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

