

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.spatial3d.geom.Bounds;
import org.apache.lucene.spatial3d.geom.DistanceStyle;
import org.apache.lucene.spatial3d.geom.GeoAreaShape;
import org.apache.lucene.spatial3d.geom.GeoOutsideDistance;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoPolygon;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.apache.lucene.spatial3d.geom.SidedPlane;
import org.apache.lucene.spatial3d.geom.Vector;


class GeoConvexPolygon {
	protected final List<GeoPoint> points;

	protected final BitSet isInternalEdges;

	protected final List<GeoPolygon> holes;

	protected SidedPlane[] edges = null;

	protected GeoPoint[][] notableEdgePoints = null;

	protected GeoPoint[] edgePoints = null;

	protected boolean isDone = false;

	protected Map<SidedPlane, Membership> eitherBounds = null;

	protected Map<SidedPlane, SidedPlane> prevBrotherMap = null;

	protected Map<SidedPlane, SidedPlane> nextBrotherMap = null;

	public GeoConvexPolygon(final PlanetModel planetModel, final List<GeoPoint> pointList) {
		this(planetModel, pointList, null);
	}

	public GeoConvexPolygon(final PlanetModel planetModel, final List<GeoPoint> pointList, final List<GeoPolygon> holes) {
		this.points = pointList;
		if ((holes != null) && ((holes.size()) == 0)) {
			this.holes = null;
		}else {
			this.holes = holes;
		}
		this.isInternalEdges = new BitSet();
		done(false);
	}

	public GeoConvexPolygon(final PlanetModel planetModel, final List<GeoPoint> pointList, final BitSet internalEdgeFlags, final boolean returnEdgeInternal) {
		this(planetModel, pointList, null, internalEdgeFlags, returnEdgeInternal);
	}

	public GeoConvexPolygon(final PlanetModel planetModel, final List<GeoPoint> pointList, final List<GeoPolygon> holes, final BitSet internalEdgeFlags, final boolean returnEdgeInternal) {
		this.points = pointList;
		if ((holes != null) && ((holes.size()) == 0)) {
			this.holes = null;
		}else {
			this.holes = holes;
		}
		this.isInternalEdges = internalEdgeFlags;
		done(returnEdgeInternal);
	}

	public GeoConvexPolygon(final PlanetModel planetModel, final double startLatitude, final double startLongitude) {
		this(planetModel, startLatitude, startLongitude, null);
	}

	public GeoConvexPolygon(final PlanetModel planetModel, final double startLatitude, final double startLongitude, final List<GeoPolygon> holes) {
		points = new ArrayList<>();
		if ((holes != null) && ((holes.size()) == 0)) {
			this.holes = null;
		}else {
			this.holes = holes;
		}
		isInternalEdges = new BitSet();
		points.add(new GeoPoint(planetModel, startLatitude, startLongitude));
	}

	public void addPoint(final double latitude, final double longitude, final boolean isInternalEdge) {
		if (isDone)
			throw new IllegalStateException("Can't call addPoint() if done() already called");

		if (isInternalEdge)
			isInternalEdges.set(((points.size()) - 1));

	}

	public void done(final boolean isInternalReturnEdge) {
		if (isDone)
			throw new IllegalStateException("Can't call done() more than once");

		if ((points.size()) < 3)
			throw new IllegalArgumentException("Polygon needs at least three points.");

		if (isInternalReturnEdge)
			isInternalEdges.set(((points.size()) - 1));

		isDone = true;
		edges = new SidedPlane[points.size()];
		notableEdgePoints = new GeoPoint[points.size()][];
		for (int i = 0; i < (points.size()); i++) {
			final GeoPoint start = points.get(i);
			final GeoPoint end = points.get(legalIndex((i + 1)));
			final Plane planeToFind = new Plane(start, end);
			int endPointIndex = -1;
			for (int j = 0; j < (points.size()); j++) {
				final int index = legalIndex(((j + i) + 2));
				if (!(planeToFind.evaluateIsZero(points.get(index)))) {
					endPointIndex = index;
					break;
				}
			}
			if (endPointIndex == (-1)) {
				throw new IllegalArgumentException(("Polygon points are all coplanar: " + (points)));
			}
			final GeoPoint check = points.get(endPointIndex);
			final SidedPlane sp = new SidedPlane(check, start, end);
			edges[i] = sp;
			notableEdgePoints[i] = new GeoPoint[]{ start, end };
		}
		eitherBounds = new HashMap<>(edges.length);
		prevBrotherMap = new HashMap<>(edges.length);
		nextBrotherMap = new HashMap<>(edges.length);
		for (int edgeIndex = 0; edgeIndex < (edges.length); edgeIndex++) {
			final SidedPlane edge = edges[edgeIndex];
			int bound1Index = legalIndex((edgeIndex + 1));
			while (edges[bound1Index].isNumericallyIdentical(edge)) {
				if (bound1Index == edgeIndex) {
					throw new IllegalArgumentException(("Constructed planes are all coplanar: " + (points)));
				}
				bound1Index = legalIndex((bound1Index + 1));
			} 
			int bound2Index = legalIndex((edgeIndex - 1));
			while (edges[bound2Index].isNumericallyIdentical(edge)) {
				if (bound2Index == edgeIndex) {
					throw new IllegalArgumentException(("Constructed planes are all coplanar: " + (points)));
				}
				bound2Index = legalIndex((bound2Index - 1));
			} 
			int startingIndex = bound2Index;
			while (true) {
				startingIndex = legalIndex((startingIndex + 1));
				if (startingIndex == bound1Index) {
					break;
				}
				final GeoPoint interiorPoint = points.get(startingIndex);
				if ((!(edges[bound1Index].isWithin(interiorPoint))) || (!(edges[bound2Index].isWithin(interiorPoint)))) {
					throw new IllegalArgumentException("Convex polygon has a side that is more than 180 degrees");
				}
			} 
			eitherBounds.put(edge, new GeoConvexPolygon.EitherBound(edges[bound1Index], edges[bound2Index]));
			nextBrotherMap.put(edge, edges[bound1Index]);
			prevBrotherMap.put(edge, edges[bound2Index]);
		}
		int edgePointCount = 1;
		if ((holes) != null) {
			for (final GeoPolygon hole : holes) {
				edgePointCount += hole.getEdgePoints().length;
			}
		}
		edgePoints = new GeoPoint[edgePointCount];
		edgePointCount = 0;
		edgePoints[(edgePointCount++)] = points.get(0);
		if ((holes) != null) {
			for (final GeoPolygon hole : holes) {
				final GeoPoint[] holeEdgePoints = hole.getEdgePoints();
				for (final GeoPoint p : holeEdgePoints) {
					edgePoints[(edgePointCount++)] = p;
				}
			}
		}
		if (isWithinHoles(points.get(0))) {
			throw new IllegalArgumentException("Polygon edge intersects a polygon hole; not allowed");
		}
	}

	protected boolean isWithinHoles(final GeoPoint point) {
		if ((holes) != null) {
			for (final GeoPolygon hole : holes) {
				if (!(hole.isWithin(point))) {
					return true;
				}
			}
		}
		return false;
	}

	protected int legalIndex(int index) {
		while (index >= (points.size())) {
			index -= points.size();
		} 
		while (index < 0) {
			index += points.size();
		} 
		return index;
	}

	public GeoConvexPolygon(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this.points = Arrays.asList(SerializableObject.readPointArray(planetModel, inputStream));
		final List<GeoPolygon> holes = Arrays.asList(SerializableObject.readPolygonArray(planetModel, inputStream));
		if ((holes != null) && ((holes.size()) == 0)) {
			this.holes = null;
		}else {
			this.holes = holes;
		}
		this.isInternalEdges = SerializableObject.readBitSet(inputStream);
		done(this.isInternalEdges.get(((points.size()) - 1)));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writePointArray(outputStream, points);
		SerializableObject.writePolygonArray(outputStream, holes);
		SerializableObject.writeBitSet(outputStream, isInternalEdges);
	}

	public boolean isWithin(final double x, final double y, final double z) {
		if (!(localIsWithin(x, y, z))) {
			return false;
		}
		if ((holes) != null) {
			for (final GeoPolygon polygon : holes) {
				if (!(polygon.isWithin(x, y, z))) {
					return false;
				}
			}
		}
		return true;
	}

	protected boolean localIsWithin(final Vector v) {
		return localIsWithin(v.x, v.y, v.z);
	}

	protected boolean localIsWithin(final double x, final double y, final double z) {
		for (final SidedPlane edge : edges) {
			if (!(edge.isWithin(x, y, z)))
				return false;

		}
		return true;
	}

	public GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public boolean intersects(final Plane p, final GeoPoint[] notablePoints, final Membership... bounds) {
		for (int edgeIndex = 0; edgeIndex < (edges.length); edgeIndex++) {
			final SidedPlane edge = edges[edgeIndex];
			final GeoPoint[] points = this.notableEdgePoints[edgeIndex];
			if (!(isInternalEdges.get(edgeIndex))) {
			}
		}
		if ((holes) != null) {
			for (final GeoPolygon hole : holes) {
				if (hole.intersects(p, notablePoints, bounds)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean intersects(GeoShape shape) {
		for (int edgeIndex = 0; edgeIndex < (edges.length); edgeIndex++) {
			final SidedPlane edge = edges[edgeIndex];
			final GeoPoint[] points = this.notableEdgePoints[edgeIndex];
			if (!(isInternalEdges.get(edgeIndex))) {
				if (shape.intersects(edge, points, eitherBounds.get(edge))) {
					return true;
				}
			}
		}
		if ((holes) != null) {
			for (final GeoPolygon hole : holes) {
				if (hole.intersects(shape)) {
					return true;
				}
			}
		}
		return false;
	}

	protected static class EitherBound implements Membership {
		protected final SidedPlane sideBound1;

		protected final SidedPlane sideBound2;

		public EitherBound(final SidedPlane sideBound1, final SidedPlane sideBound2) {
			this.sideBound1 = sideBound1;
			this.sideBound2 = sideBound2;
		}

		@Override
		public boolean isWithin(final Vector v) {
			return (sideBound1.isWithin(v)) && (sideBound2.isWithin(v));
		}

		@Override
		public boolean isWithin(final double x, final double y, final double z) {
			return (sideBound1.isWithin(x, y, z)) && (sideBound2.isWithin(x, y, z));
		}

		@Override
		public String toString() {
			return ((("(" + (sideBound1)) + ",") + (sideBound2)) + ")";
		}
	}

	public void getBounds(Bounds bounds) {
		for (final GeoPoint point : points) {
			bounds.addPoint(point);
		}
		for (final SidedPlane edge : edges) {
			final SidedPlane nextEdge = nextBrotherMap.get(edge);
		}
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		double minimumDistance = Double.POSITIVE_INFINITY;
		for (final GeoPoint edgePoint : points) {
			final double newDist = distanceStyle.computeDistance(edgePoint, x, y, z);
			if (newDist < minimumDistance) {
				minimumDistance = newDist;
			}
		}
		for (final SidedPlane edgePlane : edges) {
		}
		if ((holes) != null) {
			for (final GeoPolygon hole : holes) {
				double holeDistance = hole.computeOutsideDistance(distanceStyle, x, y, z);
				if ((holeDistance != 0.0) && (holeDistance < minimumDistance)) {
					minimumDistance = holeDistance;
				}
			}
		}
		return minimumDistance;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoConvexPolygon))
			return false;

		final GeoConvexPolygon other = ((GeoConvexPolygon) (o));
		if (!(super.equals(other)))
			return false;

		if (!(other.isInternalEdges.equals(isInternalEdges)))
			return false;

		if (((other.holes) != null) || ((holes) != null)) {
			if (((other.holes) == null) || ((holes) == null)) {
				return false;
			}
			if (!(other.holes.equals(holes))) {
				return false;
			}
		}
		return other.points.equals(points);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (points.hashCode());
		if ((holes) != null) {
			result = (31 * result) + (holes.hashCode());
		}
		return result;
	}

	@Override
	public String toString() {
		return null;
	}
}

