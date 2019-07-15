

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


class dXYZSolid {
	protected final double X;

	protected final double minY;

	protected final double maxY;

	protected final double minZ;

	protected final double maxZ;

	protected Plane xPlane = null;

	protected final SidedPlane minYPlane = null;

	protected final SidedPlane maxYPlane = null;

	protected final SidedPlane minZPlane = null;

	protected final SidedPlane maxZPlane = null;

	protected final GeoPoint[] edgePoints;

	protected final GeoPoint[] notableXPoints;

	public dXYZSolid(final PlanetModel planetModel, final double X, final double minY, final double maxY, final double minZ, final double maxZ) {
		if ((maxY - minY) < (Vector.MINIMUM_RESOLUTION))
			throw new IllegalArgumentException("Y values in wrong order or identical");

		if ((maxZ - minZ) < (Vector.MINIMUM_RESOLUTION))
			throw new IllegalArgumentException("Z values in wrong order or identical");

		this.X = X;
		this.minY = minY;
		this.maxY = maxY;
		this.minZ = minZ;
		this.maxZ = maxZ;
		final double worldMinX = planetModel.getMinimumXValue();
		final double worldMaxX = planetModel.getMaximumXValue();
		final GeoPoint[] XminY = xPlane.findIntersections(planetModel, minYPlane, maxYPlane, minZPlane, maxZPlane);
		final GeoPoint[] XmaxY = xPlane.findIntersections(planetModel, maxYPlane, minYPlane, minZPlane, maxZPlane);
		final GeoPoint[] XminZ = xPlane.findIntersections(planetModel, minZPlane, maxZPlane, minYPlane, maxYPlane);
		final GeoPoint[] XmaxZ = xPlane.findIntersections(planetModel, maxZPlane, minZPlane, minYPlane, maxYPlane);
		final boolean XminYminZ = planetModel.pointOutside(X, minY, minZ);
		final boolean XminYmaxZ = planetModel.pointOutside(X, minY, maxZ);
		final boolean XmaxYminZ = planetModel.pointOutside(X, maxY, minZ);
		final boolean XmaxYmaxZ = planetModel.pointOutside(X, maxY, maxZ);
		final GeoPoint[] xEdges;
		if (((((((((((X - worldMinX) >= (-(Vector.MINIMUM_RESOLUTION))) && ((X - worldMaxX) <= (Vector.MINIMUM_RESOLUTION))) && (minY < 0.0)) && (maxY > 0.0)) && (minZ < 0.0)) && (maxZ > 0.0)) && XminYminZ) && XminYmaxZ) && XmaxYminZ) && XmaxYmaxZ) {
		}else {
		}
		xPlane = null;
		notableXPoints = null;
		edgePoints = null;
	}

	public dXYZSolid(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, X);
		SerializableObject.writeDouble(outputStream, minY);
		SerializableObject.writeDouble(outputStream, maxY);
		SerializableObject.writeDouble(outputStream, minZ);
		SerializableObject.writeDouble(outputStream, maxZ);
	}

	protected GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		return ((((xPlane.evaluateIsZero(x, y, z)) && (minYPlane.isWithin(x, y, z))) && (maxYPlane.isWithin(x, y, z))) && (minZPlane.isWithin(x, y, z))) && (maxZPlane.isWithin(x, y, z));
	}

	public int getRelationship(final GeoShape path) {
		if (path.intersects(xPlane, notableXPoints, minYPlane, maxYPlane, minZPlane, maxZPlane)) {
			return GeoArea.OVERLAPS;
		}
		return GeoArea.DISJOINT;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof dXYZSolid))
			return false;

		dXYZSolid other = ((dXYZSolid) (o));
		if (!(super.equals(other))) {
			return false;
		}
		return ((((other.xPlane.equals(xPlane)) && (other.minYPlane.equals(minYPlane))) && (other.maxYPlane.equals(maxYPlane))) && (other.minZPlane.equals(minZPlane))) && (other.maxZPlane.equals(maxZPlane));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (xPlane.hashCode());
		result = (31 * result) + (minYPlane.hashCode());
		result = (31 * result) + (maxYPlane.hashCode());
		result = (31 * result) + (minZPlane.hashCode());
		result = (31 * result) + (maxZPlane.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

