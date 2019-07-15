

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.lucene.spatial3d.geom.GeoArea;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.apache.lucene.spatial3d.geom.Vector;


class dXdYdZSolid {
	protected final double X;

	protected final double Y;

	protected final double Z;

	protected final boolean isOnSurface;

	protected final GeoPoint thePoint;

	protected final GeoPoint[] edgePoints;

	protected static final GeoPoint[] nullPoints = new GeoPoint[0];

	public dXdYdZSolid(final PlanetModel planetModel, final double X, final double Y, final double Z) {
		this.X = X;
		this.Y = Y;
		this.Z = Z;
		isOnSurface = planetModel.pointOnSurface(X, Y, Z);
		if (isOnSurface) {
			thePoint = new GeoPoint(X, Y, Z);
			edgePoints = new GeoPoint[]{ thePoint };
		}else {
			thePoint = null;
			edgePoints = dXdYdZSolid.nullPoints;
		}
	}

	public dXdYdZSolid(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, X);
		SerializableObject.writeDouble(outputStream, Y);
		SerializableObject.writeDouble(outputStream, Z);
	}

	protected GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		if (!(isOnSurface)) {
			return false;
		}
		return thePoint.isIdentical(x, y, z);
	}

	public int getRelationship(final GeoShape path) {
		if (!(isOnSurface)) {
			return GeoArea.DISJOINT;
		}
		return GeoArea.DISJOINT;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof dXdYdZSolid))
			return false;

		dXdYdZSolid other = ((dXdYdZSolid) (o));
		if ((!(super.equals(other))) || ((other.isOnSurface) != (isOnSurface))) {
			return false;
		}
		if (isOnSurface) {
			return other.thePoint.equals(thePoint);
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (isOnSurface ? 1 : 0);
		if (isOnSurface) {
			result = (31 * result) + (thePoint.hashCode());
		}
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

