

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.lucene.spatial3d.geom.GeoArea;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.apache.lucene.spatial3d.geom.Vector;


class dXdYZSolid {
	protected final double X;

	protected final double Y;

	protected final double minZ;

	protected final double maxZ;

	protected final GeoPoint[] surfacePoints;

	public dXdYZSolid(final PlanetModel planetModel, final double X, final double Y, final double minZ, final double maxZ) {
		if ((maxZ - minZ) < (Vector.MINIMUM_RESOLUTION))
			throw new IllegalArgumentException("Z values in wrong order or identical");

		this.X = X;
		this.Y = Y;
		this.minZ = minZ;
		this.maxZ = maxZ;
		surfacePoints = null;
	}

	public dXdYZSolid(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, X);
		SerializableObject.writeDouble(outputStream, Y);
		SerializableObject.writeDouble(outputStream, minZ);
		SerializableObject.writeDouble(outputStream, maxZ);
	}

	protected GeoPoint[] getEdgePoints() {
		return surfacePoints;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		for (final GeoPoint p : surfacePoints) {
			if (p.isIdentical(x, y, z))
				return true;

		}
		return false;
	}

	public int getRelationship(final GeoShape path) {
		return GeoArea.DISJOINT;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof dXdYZSolid))
			return false;

		dXdYZSolid other = ((dXdYZSolid) (o));
		if ((!(super.equals(other))) || ((surfacePoints.length) != (other.surfacePoints.length))) {
			return false;
		}
		for (int i = 0; i < (surfacePoints.length); i++) {
			if (!(surfacePoints[i].equals(other.surfacePoints[i])))
				return false;

		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		for (final GeoPoint p : surfacePoints) {
			result = (31 * result) + (p.hashCode());
		}
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final GeoPoint p : surfacePoints) {
			sb.append(" ").append(p).append(" ");
		}
		return null;
	}
}

