

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.lucene.spatial3d.geom.Bounds;
import org.apache.lucene.spatial3d.geom.DistanceStyle;
import org.apache.lucene.spatial3d.geom.GeoArea;
import org.apache.lucene.spatial3d.geom.GeoBBox;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.PlanetModel;


class GeoWorld {
	protected static final GeoPoint[] edgePoints = new GeoPoint[0];

	protected final GeoPoint originPoint;

	public GeoWorld(final PlanetModel planetModel) {
		originPoint = new GeoPoint(planetModel.ab, 1.0, 0.0, 0.0);
	}

	public GeoWorld(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel);
	}

	public void write(final OutputStream outputStream) throws IOException {
	}

	public GeoBBox expand(final double angle) {
		return null;
	}

	public double getRadius() {
		return Math.PI;
	}

	public GeoPoint getCenter() {
		return originPoint;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		return true;
	}

	public GeoPoint[] getEdgePoints() {
		return GeoWorld.edgePoints;
	}

	public boolean intersects(final Plane p, final GeoPoint[] notablePoints, final Membership... bounds) {
		return false;
	}

	public boolean intersects(final GeoShape geoShape) {
		return false;
	}

	public void getBounds(Bounds bounds) {
	}

	public int getRelationship(final GeoShape path) {
		if ((path.getEdgePoints().length) > 0)
			return GeoArea.WITHIN;

		return GeoArea.OVERLAPS;
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		return 0.0;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoWorld))
			return false;

		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return null;
	}
}

