

import org.apache.lucene.spatial3d.geom.Bounds;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.Vector;


public class XYZBounds implements Bounds {
	private static final double FUDGE_FACTOR = (Vector.MINIMUM_RESOLUTION) * 1000.0;

	private Double minX = null;

	private Double maxX = null;

	private Double minY = null;

	private Double maxY = null;

	private Double minZ = null;

	private Double maxZ = null;

	private boolean noLongitudeBound = false;

	private boolean noTopLatitudeBound = false;

	private boolean noBottomLatitudeBound = false;

	public XYZBounds() {
	}

	public Double getMinimumX() {
		return minX;
	}

	public Double getMaximumX() {
		return maxX;
	}

	public Double getMinimumY() {
		return minY;
	}

	public Double getMaximumY() {
		return maxY;
	}

	public Double getMinimumZ() {
		return minZ;
	}

	public Double getMaximumZ() {
		return maxZ;
	}

	public boolean isSmallestMinX(final PlanetModel planetModel) {
		if ((minX) == null)
			return false;

		return ((minX) - (planetModel.getMinimumXValue())) < (Vector.MINIMUM_RESOLUTION);
	}

	public boolean isLargestMaxX(final PlanetModel planetModel) {
		if ((maxX) == null)
			return false;

		return ((planetModel.getMaximumXValue()) - (maxX)) < (Vector.MINIMUM_RESOLUTION);
	}

	public boolean isSmallestMinY(final PlanetModel planetModel) {
		if ((minY) == null)
			return false;

		return ((minY) - (planetModel.getMinimumYValue())) < (Vector.MINIMUM_RESOLUTION);
	}

	public boolean isLargestMaxY(final PlanetModel planetModel) {
		if ((maxY) == null)
			return false;

		return ((planetModel.getMaximumYValue()) - (maxY)) < (Vector.MINIMUM_RESOLUTION);
	}

	public boolean isSmallestMinZ(final PlanetModel planetModel) {
		if ((minZ) == null)
			return false;

		return ((minZ) - (planetModel.getMinimumZValue())) < (Vector.MINIMUM_RESOLUTION);
	}

	public boolean isLargestMaxZ(final PlanetModel planetModel) {
		if ((maxZ) == null)
			return false;

		return ((planetModel.getMaximumZValue()) - (maxZ)) < (Vector.MINIMUM_RESOLUTION);
	}

	@Override
	public Bounds addPlane(final PlanetModel planetModel, final Plane plane, final Membership... bounds) {
		return this;
	}

	public Bounds addHorizontalPlane(final PlanetModel planetModel, final double latitude, final Plane horizontalPlane, final Membership... bounds) {
		return addPlane(planetModel, horizontalPlane, bounds);
	}

	public Bounds addVerticalPlane(final PlanetModel planetModel, final double longitude, final Plane verticalPlane, final Membership... bounds) {
		return addPlane(planetModel, verticalPlane, bounds);
	}

	@Override
	public Bounds addXValue(final GeoPoint point) {
		return addXValue(point.x);
	}

	public Bounds addXValue(final double x) {
		final double small = x - (XYZBounds.FUDGE_FACTOR);
		if (((minX) == null) || ((minX) > small)) {
			minX = new Double(small);
		}
		final double large = x + (XYZBounds.FUDGE_FACTOR);
		if (((maxX) == null) || ((maxX) < large)) {
			maxX = new Double(large);
		}
		return this;
	}

	@Override
	public Bounds addYValue(final GeoPoint point) {
		return addYValue(point.y);
	}

	public Bounds addYValue(final double y) {
		final double small = y - (XYZBounds.FUDGE_FACTOR);
		if (((minY) == null) || ((minY) > small)) {
			minY = new Double(small);
		}
		final double large = y + (XYZBounds.FUDGE_FACTOR);
		if (((maxY) == null) || ((maxY) < large)) {
			maxY = new Double(large);
		}
		return this;
	}

	@Override
	public Bounds addZValue(final GeoPoint point) {
		return addZValue(point.z);
	}

	public Bounds addZValue(final double z) {
		final double small = z - (XYZBounds.FUDGE_FACTOR);
		if (((minZ) == null) || ((minZ) > small)) {
			minZ = new Double(small);
		}
		final double large = z + (XYZBounds.FUDGE_FACTOR);
		if (((maxZ) == null) || ((maxZ) < large)) {
			maxZ = new Double(large);
		}
		return this;
	}

	@Override
	public Bounds addIntersection(final PlanetModel planetModel, final Plane plane1, final Plane plane2, final Membership... bounds) {
		return this;
	}

	@Override
	public Bounds addPoint(final GeoPoint point) {
		return addXValue(point).addYValue(point).addZValue(point);
	}

	@Override
	public Bounds isWide() {
		return this;
	}

	@Override
	public Bounds noLongitudeBound() {
		return this;
	}

	@Override
	public Bounds noTopLatitudeBound() {
		return this;
	}

	@Override
	public Bounds noBottomLatitudeBound() {
		return this;
	}

	@Override
	public Bounds noBound(final PlanetModel planetModel) {
		minX = planetModel.getMinimumXValue();
		maxX = planetModel.getMaximumXValue();
		minY = planetModel.getMinimumYValue();
		maxY = planetModel.getMaximumYValue();
		minZ = planetModel.getMinimumZValue();
		maxZ = planetModel.getMaximumZValue();
		return this;
	}

	@Override
	public String toString() {
		return ((((((((((("XYZBounds: [xmin=" + (minX)) + " xmax=") + (maxX)) + " ymin=") + (minY)) + " ymax=") + (maxY)) + " zmin=") + (minZ)) + " zmax=") + (maxZ)) + "]";
	}
}

