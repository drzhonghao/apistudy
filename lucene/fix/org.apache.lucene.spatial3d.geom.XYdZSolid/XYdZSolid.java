

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.lucene.spatial3d.geom.GeoArea;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.apache.lucene.spatial3d.geom.SidedPlane;
import org.apache.lucene.spatial3d.geom.Vector;


class XYdZSolid {
	protected final double minX;

	protected final double maxX;

	protected final double minY;

	protected final double maxY;

	protected final double Z;

	protected final SidedPlane minXPlane = null;

	protected final SidedPlane maxXPlane = null;

	protected final SidedPlane minYPlane = null;

	protected final SidedPlane maxYPlane = null;

	protected Plane zPlane = null;

	protected final GeoPoint[] edgePoints;

	protected final GeoPoint[] notableZPoints;

	public XYdZSolid(final PlanetModel planetModel, final double minX, final double maxX, final double minY, final double maxY, final double Z) {
		if ((maxX - minX) < (Vector.MINIMUM_RESOLUTION))
			throw new IllegalArgumentException("X values in wrong order or identical");

		if ((maxY - minY) < (Vector.MINIMUM_RESOLUTION))
			throw new IllegalArgumentException("Y values in wrong order or identical");

		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		this.Z = Z;
		final double worldMinZ = planetModel.getMinimumZValue();
		final double worldMaxZ = planetModel.getMaximumZValue();
		final GeoPoint[] minXZ = minXPlane.findIntersections(planetModel, zPlane, maxXPlane, minYPlane, maxYPlane);
		final GeoPoint[] maxXZ = maxXPlane.findIntersections(planetModel, zPlane, minXPlane, minYPlane, maxYPlane);
		final GeoPoint[] minYZ = minYPlane.findIntersections(planetModel, zPlane, maxYPlane, minXPlane, maxXPlane);
		final GeoPoint[] maxYZ = maxYPlane.findIntersections(planetModel, zPlane, minYPlane, minXPlane, maxXPlane);
		final boolean minXminYZ = planetModel.pointOutside(minX, minY, Z);
		final boolean minXmaxYZ = planetModel.pointOutside(minX, maxY, Z);
		final boolean maxXminYZ = planetModel.pointOutside(maxX, minY, Z);
		final boolean maxXmaxYZ = planetModel.pointOutside(maxX, maxY, Z);
		final GeoPoint[] zEdges;
		if (((((((((((Z - worldMinZ) >= (-(Vector.MINIMUM_RESOLUTION))) && ((Z - worldMaxZ) <= (Vector.MINIMUM_RESOLUTION))) && (minX < 0.0)) && (maxX > 0.0)) && (minY < 0.0)) && (maxY > 0.0)) && minXminYZ) && minXmaxYZ) && maxXminYZ) && maxXmaxYZ) {
		}else {
		}
		zPlane = null;
		notableZPoints = null;
		edgePoints = null;
	}

	public XYdZSolid(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, minX);
		SerializableObject.writeDouble(outputStream, maxX);
		SerializableObject.writeDouble(outputStream, minY);
		SerializableObject.writeDouble(outputStream, maxY);
		SerializableObject.writeDouble(outputStream, Z);
	}

	protected GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		return ((((minXPlane.isWithin(x, y, z)) && (maxXPlane.isWithin(x, y, z))) && (minYPlane.isWithin(x, y, z))) && (maxYPlane.isWithin(x, y, z))) && (zPlane.evaluateIsZero(x, y, z));
	}

	public int getRelationship(final GeoShape path) {
		if (path.intersects(zPlane, notableZPoints, minXPlane, maxXPlane, minYPlane, maxYPlane)) {
			return GeoArea.OVERLAPS;
		}
		return GeoArea.DISJOINT;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof XYdZSolid))
			return false;

		XYdZSolid other = ((XYdZSolid) (o));
		if (!(super.equals(other))) {
			return false;
		}
		return ((((other.minXPlane.equals(minXPlane)) && (other.maxXPlane.equals(maxXPlane))) && (other.minYPlane.equals(minYPlane))) && (other.maxYPlane.equals(maxYPlane))) && (other.zPlane.equals(zPlane));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (minXPlane.hashCode());
		result = (31 * result) + (maxXPlane.hashCode());
		result = (31 * result) + (minYPlane.hashCode());
		result = (31 * result) + (maxYPlane.hashCode());
		result = (31 * result) + (zPlane.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

