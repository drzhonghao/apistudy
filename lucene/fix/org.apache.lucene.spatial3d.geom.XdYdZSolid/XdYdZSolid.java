

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.lucene.spatial3d.geom.GeoArea;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.apache.lucene.spatial3d.geom.Vector;


class XdYdZSolid {
	protected final double minX;

	protected final double maxX;

	protected final double Y;

	protected final double Z;

	protected final GeoPoint[] surfacePoints;

	public XdYdZSolid(final PlanetModel planetModel, final double minX, final double maxX, final double Y, final double Z) {
		if ((maxX - minX) < (Vector.MINIMUM_RESOLUTION))
			throw new IllegalArgumentException("X values in wrong order or identical");

		this.minX = minX;
		this.maxX = maxX;
		this.Y = Y;
		this.Z = Z;
		surfacePoints = null;
	}

	public XdYdZSolid(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream), SerializableObject.readDouble(inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writeDouble(outputStream, minX);
		SerializableObject.writeDouble(outputStream, maxX);
		SerializableObject.writeDouble(outputStream, Y);
		SerializableObject.writeDouble(outputStream, Z);
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
		if (!(o instanceof XdYdZSolid))
			return false;

		XdYdZSolid other = ((XdYdZSolid) (o));
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

