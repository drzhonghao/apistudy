

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.lucene.spatial3d.geom.Bounds;
import org.apache.lucene.spatial3d.geom.DistanceStyle;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.apache.lucene.spatial3d.geom.SidedPlane;
import org.apache.lucene.spatial3d.geom.Vector;


class GeoS2Shape {
	protected final GeoPoint point1;

	protected final GeoPoint point2;

	protected final GeoPoint point3;

	protected final GeoPoint point4;

	protected final SidedPlane plane1;

	protected final SidedPlane plane2;

	protected final SidedPlane plane3;

	protected final SidedPlane plane4;

	protected final GeoPoint[] plane1Points;

	protected final GeoPoint[] plane2Points;

	protected final GeoPoint[] plane3Points;

	protected final GeoPoint[] plane4Points;

	protected final GeoPoint[] edgePoints;

	public GeoS2Shape(final PlanetModel planetModel, GeoPoint point1, GeoPoint point2, GeoPoint point3, GeoPoint point4) {
		this.point1 = point1;
		this.point2 = point2;
		this.point3 = point3;
		this.point4 = point4;
		this.plane1 = new SidedPlane(point4, point1, point2);
		this.plane2 = new SidedPlane(point1, point2, point3);
		this.plane3 = new SidedPlane(point2, point3, point4);
		this.plane4 = new SidedPlane(point3, point4, point1);
		this.plane1Points = new GeoPoint[]{ point1, point2 };
		this.plane2Points = new GeoPoint[]{ point2, point3 };
		this.plane3Points = new GeoPoint[]{ point3, point4 };
		this.plane4Points = new GeoPoint[]{ point4, point1 };
		this.edgePoints = new GeoPoint[]{ point1 };
	}

	public GeoS2Shape(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, ((GeoPoint) (SerializableObject.readObject(inputStream))), ((GeoPoint) (SerializableObject.readObject(inputStream))), ((GeoPoint) (SerializableObject.readObject(inputStream))), ((GeoPoint) (SerializableObject.readObject(inputStream))));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeObject(outputStream, point1);
		SerializableObject.writeObject(outputStream, point2);
		SerializableObject.writeObject(outputStream, point3);
		SerializableObject.writeObject(outputStream, point4);
	}

	public boolean isWithin(final double x, final double y, final double z) {
		return (((plane1.isWithin(x, y, z)) && (plane2.isWithin(x, y, z))) && (plane3.isWithin(x, y, z))) && (plane4.isWithin(x, y, z));
	}

	public GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public boolean intersects(final Plane p, final GeoPoint[] notablePoints, final Membership... bounds) {
		return false;
	}

	public boolean intersects(GeoShape geoShape) {
		return (((geoShape.intersects(plane1, plane1Points, plane2, plane4)) || (geoShape.intersects(plane2, plane2Points, plane3, plane1))) || (geoShape.intersects(plane3, plane3Points, plane4, plane2))) || (geoShape.intersects(plane4, plane4Points, plane1, plane3));
	}

	public void getBounds(Bounds bounds) {
	}

	public double outsideDistance(DistanceStyle distanceStyle, double x, double y, double z) {
		final double pointDistance1 = distanceStyle.computeDistance(point1, x, y, z);
		final double pointDistance2 = distanceStyle.computeDistance(point2, x, y, z);
		final double pointDistance3 = distanceStyle.computeDistance(point3, x, y, z);
		final double pointDistance4 = distanceStyle.computeDistance(point4, x, y, z);
		return 0d;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoS2Shape))
			return false;

		GeoS2Shape other = ((GeoS2Shape) (o));
		return ((((super.equals(other)) && (other.point1.equals(point1))) && (other.point2.equals(point2))) && (other.point3.equals(point3))) && (other.point4.equals(point4));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (point1.hashCode());
		result = (31 * result) + (point2.hashCode());
		result = (31 * result) + (point3.hashCode());
		result = (31 * result) + (point4.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

