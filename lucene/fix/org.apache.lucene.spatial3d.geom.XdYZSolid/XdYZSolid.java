

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


class XdYZSolid {
	protected final double minX;

	protected final double maxX;

	protected final double Y;

	protected final double minZ;

	protected final double maxZ;

	protected final SidedPlane minXPlane = null;

	protected final SidedPlane maxXPlane = null;

	protected Plane yPlane = null;

	protected final SidedPlane minZPlane = null;

	protected final SidedPlane maxZPlane = null;

	protected final GeoPoint[] edgePoints;

	protected final GeoPoint[] notableYPoints;

	public XdYZSolid(final PlanetModel planetModel, final double minX, final double maxX, final double Y, final double minZ, final double maxZ) {
		if ((maxX - minX) < (Vector.MINIMUM_RESOLUTION))
			throw new IllegalArgumentException("X values in wrong order or identical");

		if ((maxZ - minZ) < (Vector.MINIMUM_RESOLUTION))
			throw new IllegalArgumentException("Z values in wrong order or identical");

		this.minX = minX;
		this.maxX = maxX;
		this.Y = Y;
		this.minZ = minZ;
		this.maxZ = maxZ;
		final double worldMinY = planetModel.getMinimumYValue();
		final double worldMaxY = planetModel.getMaximumYValue();
		final GeoPoint[] minXY = minXPlane.findIntersections(planetModel, yPlane, maxXPlane, minZPlane, maxZPlane);
		final GeoPoint[] maxXY = maxXPlane.findIntersections(planetModel, yPlane, minXPlane, minZPlane, maxZPlane);
		final GeoPoint[] YminZ = yPlane.findIntersections(planetModel, minZPlane, maxZPlane, minXPlane, maxXPlane);
		final GeoPoint[] YmaxZ = yPlane.findIntersections(planetModel, maxZPlane, minZPlane, minXPlane, maxXPlane);
		final boolean minXYminZ = planetModel.pointOutside(minX, Y, minZ);
		final boolean minXYmaxZ = planetModel.pointOutside(minX, Y, maxZ);
		final boolean maxXYminZ = planetModel.pointOutside(maxX, Y, minZ);
		final boolean maxXYmaxZ = planetModel.pointOutside(maxX, Y, maxZ);
		final GeoPoint[] yEdges;
		if (((((((((((Y - worldMinY) >= (-(Vector.MINIMUM_RESOLUTION))) && ((Y - worldMaxY) <= (Vector.MINIMUM_RESOLUTION))) && (minX < 0.0)) && (maxX > 0.0)) && (minZ < 0.0)) && (maxZ > 0.0)) && minXYminZ) && minXYmaxZ) && maxXYminZ) && maxXYmaxZ) {
		}else {
		}
		yPlane = null;
		notableYPoints = null;
		edgePoints = null;
	}

	public XdYZSolid(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, minX);
		SerializableObject.writeDouble(outputStream, maxX);
		SerializableObject.writeDouble(outputStream, Y);
		SerializableObject.writeDouble(outputStream, minZ);
		SerializableObject.writeDouble(outputStream, maxZ);
	}

	protected GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		return ((((minXPlane.isWithin(x, y, z)) && (maxXPlane.isWithin(x, y, z))) && (yPlane.evaluateIsZero(x, y, z))) && (minZPlane.isWithin(x, y, z))) && (maxZPlane.isWithin(x, y, z));
	}

	public int getRelationship(final GeoShape path) {
		if (path.intersects(yPlane, notableYPoints, minXPlane, maxXPlane, minZPlane, maxZPlane)) {
			return GeoArea.OVERLAPS;
		}
		return GeoArea.DISJOINT;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof XdYZSolid))
			return false;

		XdYZSolid other = ((XdYZSolid) (o));
		if (!(super.equals(other))) {
			return false;
		}
		return ((((other.minXPlane.equals(minXPlane)) && (other.maxXPlane.equals(maxXPlane))) && (other.yPlane.equals(yPlane))) && (other.minZPlane.equals(minZPlane))) && (other.maxZPlane.equals(maxZPlane));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (minXPlane.hashCode());
		result = (31 * result) + (maxXPlane.hashCode());
		result = (31 * result) + (yPlane.hashCode());
		result = (31 * result) + (minZPlane.hashCode());
		result = (31 * result) + (maxZPlane.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

