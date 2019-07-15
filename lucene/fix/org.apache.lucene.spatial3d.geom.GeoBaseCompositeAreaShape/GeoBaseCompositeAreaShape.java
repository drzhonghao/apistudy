

import java.io.IOException;
import java.io.InputStream;
import org.apache.lucene.spatial3d.geom.GeoArea;
import org.apache.lucene.spatial3d.geom.GeoAreaShape;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.PlanetModel;


abstract class GeoBaseCompositeAreaShape<T extends GeoAreaShape> implements GeoAreaShape {
	protected static final int ALL_INSIDE = 0;

	protected static final int SOME_INSIDE = 1;

	protected static final int NONE_INSIDE = 2;

	public GeoBaseCompositeAreaShape(PlanetModel planetModel) {
	}

	public GeoBaseCompositeAreaShape(final PlanetModel planetModel, final InputStream inputStream, final Class<T> clazz) throws IOException {
	}

	@Override
	public boolean intersects(GeoShape geoShape) {
		return false;
	}

	@Override
	public int getRelationship(GeoShape geoShape) {
		final int insideGeoAreaShape = isShapeInsideGeoAreaShape(geoShape);
		if (insideGeoAreaShape == (GeoBaseCompositeAreaShape.SOME_INSIDE)) {
			return GeoArea.OVERLAPS;
		}
		final int insideShape = isGeoAreaShapeInsideShape(geoShape);
		if (insideShape == (GeoBaseCompositeAreaShape.SOME_INSIDE)) {
			return GeoArea.OVERLAPS;
		}
		if ((insideGeoAreaShape == (GeoBaseCompositeAreaShape.ALL_INSIDE)) && (insideShape == (GeoBaseCompositeAreaShape.ALL_INSIDE))) {
			return GeoArea.OVERLAPS;
		}
		if (intersects(geoShape)) {
			return GeoArea.OVERLAPS;
		}
		if (insideGeoAreaShape == (GeoBaseCompositeAreaShape.ALL_INSIDE)) {
			return GeoArea.WITHIN;
		}
		if (insideShape == (GeoBaseCompositeAreaShape.ALL_INSIDE)) {
			return GeoArea.CONTAINS;
		}
		return GeoArea.DISJOINT;
	}

	protected int isShapeInsideGeoAreaShape(final GeoShape geoShape) {
		boolean foundOutside = false;
		boolean foundInside = false;
		for (GeoPoint p : geoShape.getEdgePoints()) {
			if (isWithin(p)) {
				foundInside = true;
			}else {
				foundOutside = true;
			}
			if (foundInside && foundOutside) {
				return GeoBaseCompositeAreaShape.SOME_INSIDE;
			}
		}
		if ((!foundInside) && (!foundOutside))
			return GeoBaseCompositeAreaShape.NONE_INSIDE;

		if (foundInside && (!foundOutside))
			return GeoBaseCompositeAreaShape.ALL_INSIDE;

		if (foundOutside && (!foundInside))
			return GeoBaseCompositeAreaShape.NONE_INSIDE;

		return GeoBaseCompositeAreaShape.SOME_INSIDE;
	}

	protected int isGeoAreaShapeInsideShape(final GeoShape geoshape) {
		boolean foundOutside = false;
		boolean foundInside = false;
		for (GeoPoint p : getEdgePoints()) {
			if (geoshape.isWithin(p)) {
				foundInside = true;
			}else {
				foundOutside = true;
			}
			if (foundInside && foundOutside) {
				return GeoBaseCompositeAreaShape.SOME_INSIDE;
			}
		}
		if ((!foundInside) && (!foundOutside))
			return GeoBaseCompositeAreaShape.NONE_INSIDE;

		if (foundInside && (!foundOutside))
			return GeoBaseCompositeAreaShape.ALL_INSIDE;

		if (foundOutside && (!foundInside))
			return GeoBaseCompositeAreaShape.NONE_INSIDE;

		return GeoBaseCompositeAreaShape.SOME_INSIDE;
	}
}

