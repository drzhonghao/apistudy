

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.spatial3d.geom.Bounded;
import org.apache.lucene.spatial3d.geom.Bounds;
import org.apache.lucene.spatial3d.geom.DistanceStyle;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SerializableObject;
import org.apache.lucene.spatial3d.geom.SidedPlane;
import org.apache.lucene.spatial3d.geom.Vector;
import org.apache.lucene.spatial3d.geom.XYZBounds;


class GeoComplexPolygon {
	private final GeoComplexPolygon.Tree xTree;

	private final GeoComplexPolygon.Tree yTree;

	private final GeoComplexPolygon.Tree zTree;

	private final List<List<GeoPoint>> pointsList;

	private final boolean testPoint1InSet;

	private final GeoPoint testPoint1;

	private final boolean testPoint2InSet;

	private final GeoPoint testPoint2;

	private final Plane testPoint1FixedYPlane;

	private final Plane testPoint1FixedYAbovePlane;

	private final Plane testPoint1FixedYBelowPlane;

	private final Plane testPoint1FixedXPlane;

	private final Plane testPoint1FixedXAbovePlane;

	private final Plane testPoint1FixedXBelowPlane;

	private final Plane testPoint1FixedZPlane;

	private final Plane testPoint1FixedZAbovePlane;

	private final Plane testPoint1FixedZBelowPlane;

	private final Plane testPoint2FixedYPlane;

	private final Plane testPoint2FixedYAbovePlane;

	private final Plane testPoint2FixedYBelowPlane;

	private final Plane testPoint2FixedXPlane;

	private final Plane testPoint2FixedXAbovePlane;

	private final Plane testPoint2FixedXBelowPlane;

	private final Plane testPoint2FixedZPlane;

	private final Plane testPoint2FixedZAbovePlane;

	private final Plane testPoint2FixedZBelowPlane;

	private final GeoPoint[] edgePoints;

	private final GeoComplexPolygon.Edge[] shapeStartEdges;

	private static final double NEAR_EDGE_CUTOFF = (-(Vector.MINIMUM_RESOLUTION)) * 10000.0;

	public GeoComplexPolygon(final PlanetModel planetModel, final List<List<GeoPoint>> pointsList, final GeoPoint testPoint, final boolean testPointInSet) {
		assert planetModel.pointOnSurface(testPoint.x, testPoint.y, testPoint.z) : "Test point is not on the ellipsoid surface";
		this.pointsList = pointsList;
		this.edgePoints = new GeoPoint[pointsList.size()];
		this.shapeStartEdges = new GeoComplexPolygon.Edge[pointsList.size()];
		final ArrayList<GeoComplexPolygon.Edge> allEdges = new ArrayList<>();
		int edgePointIndex = 0;
		for (final List<GeoPoint> shapePoints : pointsList) {
			allEdges.ensureCapacity(((allEdges.size()) + (shapePoints.size())));
			GeoPoint lastGeoPoint = shapePoints.get(((shapePoints.size()) - 1));
			edgePoints[edgePointIndex] = lastGeoPoint;
			GeoComplexPolygon.Edge lastEdge = null;
			GeoComplexPolygon.Edge firstEdge = null;
			for (final GeoPoint thisGeoPoint : shapePoints) {
				assert planetModel.pointOnSurface(thisGeoPoint) : ("Polygon edge point must be on surface; " + thisGeoPoint) + " is not";
				final GeoComplexPolygon.Edge edge = new GeoComplexPolygon.Edge(planetModel, lastGeoPoint, thisGeoPoint);
				allEdges.add(edge);
				if (firstEdge == null) {
					firstEdge = edge;
				}
				if (lastEdge != null) {
					lastEdge.next = edge;
					edge.previous = lastEdge;
				}
				lastEdge = edge;
				lastGeoPoint = thisGeoPoint;
			}
			firstEdge.previous = lastEdge;
			lastEdge.next = firstEdge;
			shapeStartEdges[edgePointIndex] = firstEdge;
			edgePointIndex++;
		}
		xTree = new GeoComplexPolygon.XTree(allEdges);
		yTree = new GeoComplexPolygon.YTree(allEdges);
		zTree = new GeoComplexPolygon.ZTree(allEdges);
		this.testPoint1 = testPoint;
		this.testPoint2 = new GeoPoint((-(testPoint.x)), (-(testPoint.y)), (-(testPoint.z)));
		assert planetModel.pointOnSurface(testPoint2.x, testPoint2.y, testPoint2.z) : "Test point 2 is off of ellipsoid";
		this.testPoint1FixedYPlane = new Plane(0.0, 1.0, 0.0, (-(testPoint1.y)));
		this.testPoint1FixedXPlane = new Plane(1.0, 0.0, 0.0, (-(testPoint1.x)));
		this.testPoint1FixedZPlane = new Plane(0.0, 0.0, 1.0, (-(testPoint1.z)));
		Plane testPoint1FixedYAbovePlane = new Plane(testPoint1FixedYPlane, true);
		if ((((-(testPoint1FixedYAbovePlane.D)) - (planetModel.getMaximumYValue())) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF)) || (((planetModel.getMinimumYValue()) + (testPoint1FixedYAbovePlane.D)) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF))) {
			testPoint1FixedYAbovePlane = null;
		}
		this.testPoint1FixedYAbovePlane = testPoint1FixedYAbovePlane;
		Plane testPoint1FixedYBelowPlane = new Plane(testPoint1FixedYPlane, false);
		if ((((-(testPoint1FixedYBelowPlane.D)) - (planetModel.getMaximumYValue())) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF)) || (((planetModel.getMinimumYValue()) + (testPoint1FixedYBelowPlane.D)) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF))) {
			testPoint1FixedYBelowPlane = null;
		}
		this.testPoint1FixedYBelowPlane = testPoint1FixedYBelowPlane;
		Plane testPoint1FixedXAbovePlane = new Plane(testPoint1FixedXPlane, true);
		if ((((-(testPoint1FixedXAbovePlane.D)) - (planetModel.getMaximumXValue())) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF)) || (((planetModel.getMinimumXValue()) + (testPoint1FixedXAbovePlane.D)) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF))) {
			testPoint1FixedXAbovePlane = null;
		}
		this.testPoint1FixedXAbovePlane = testPoint1FixedXAbovePlane;
		Plane testPoint1FixedXBelowPlane = new Plane(testPoint1FixedXPlane, false);
		if ((((-(testPoint1FixedXBelowPlane.D)) - (planetModel.getMaximumXValue())) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF)) || (((planetModel.getMinimumXValue()) + (testPoint1FixedXBelowPlane.D)) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF))) {
			testPoint1FixedXBelowPlane = null;
		}
		this.testPoint1FixedXBelowPlane = testPoint1FixedXBelowPlane;
		Plane testPoint1FixedZAbovePlane = new Plane(testPoint1FixedZPlane, true);
		if ((((-(testPoint1FixedZAbovePlane.D)) - (planetModel.getMaximumZValue())) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF)) || (((planetModel.getMinimumZValue()) + (testPoint1FixedZAbovePlane.D)) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF))) {
			testPoint1FixedZAbovePlane = null;
		}
		this.testPoint1FixedZAbovePlane = testPoint1FixedZAbovePlane;
		Plane testPoint1FixedZBelowPlane = new Plane(testPoint1FixedZPlane, false);
		if ((((-(testPoint1FixedZBelowPlane.D)) - (planetModel.getMaximumZValue())) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF)) || (((planetModel.getMinimumZValue()) + (testPoint1FixedZBelowPlane.D)) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF))) {
			testPoint1FixedZBelowPlane = null;
		}
		this.testPoint1FixedZBelowPlane = testPoint1FixedZBelowPlane;
		this.testPoint2FixedYPlane = new Plane(0.0, 1.0, 0.0, (-(testPoint2.y)));
		this.testPoint2FixedXPlane = new Plane(1.0, 0.0, 0.0, (-(testPoint2.x)));
		this.testPoint2FixedZPlane = new Plane(0.0, 0.0, 1.0, (-(testPoint2.z)));
		Plane testPoint2FixedYAbovePlane = new Plane(testPoint2FixedYPlane, true);
		if ((((-(testPoint2FixedYAbovePlane.D)) - (planetModel.getMaximumYValue())) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF)) || (((planetModel.getMinimumYValue()) + (testPoint2FixedYAbovePlane.D)) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF))) {
			testPoint2FixedYAbovePlane = null;
		}
		this.testPoint2FixedYAbovePlane = testPoint2FixedYAbovePlane;
		Plane testPoint2FixedYBelowPlane = new Plane(testPoint2FixedYPlane, false);
		if ((((-(testPoint2FixedYBelowPlane.D)) - (planetModel.getMaximumYValue())) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF)) || (((planetModel.getMinimumYValue()) + (testPoint2FixedYBelowPlane.D)) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF))) {
			testPoint2FixedYBelowPlane = null;
		}
		this.testPoint2FixedYBelowPlane = testPoint2FixedYBelowPlane;
		Plane testPoint2FixedXAbovePlane = new Plane(testPoint2FixedXPlane, true);
		if ((((-(testPoint2FixedXAbovePlane.D)) - (planetModel.getMaximumXValue())) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF)) || (((planetModel.getMinimumXValue()) + (testPoint2FixedXAbovePlane.D)) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF))) {
			testPoint2FixedXAbovePlane = null;
		}
		this.testPoint2FixedXAbovePlane = testPoint2FixedXAbovePlane;
		Plane testPoint2FixedXBelowPlane = new Plane(testPoint2FixedXPlane, false);
		if ((((-(testPoint2FixedXBelowPlane.D)) - (planetModel.getMaximumXValue())) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF)) || (((planetModel.getMinimumXValue()) + (testPoint2FixedXBelowPlane.D)) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF))) {
			testPoint2FixedXBelowPlane = null;
		}
		this.testPoint2FixedXBelowPlane = testPoint2FixedXBelowPlane;
		Plane testPoint2FixedZAbovePlane = new Plane(testPoint2FixedZPlane, true);
		if ((((-(testPoint2FixedZAbovePlane.D)) - (planetModel.getMaximumZValue())) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF)) || (((planetModel.getMinimumZValue()) + (testPoint2FixedZAbovePlane.D)) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF))) {
			testPoint2FixedZAbovePlane = null;
		}
		this.testPoint2FixedZAbovePlane = testPoint2FixedZAbovePlane;
		Plane testPoint2FixedZBelowPlane = new Plane(testPoint2FixedZPlane, false);
		if ((((-(testPoint2FixedZBelowPlane.D)) - (planetModel.getMaximumZValue())) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF)) || (((planetModel.getMinimumZValue()) + (testPoint2FixedZBelowPlane.D)) > (GeoComplexPolygon.NEAR_EDGE_CUTOFF))) {
			testPoint2FixedZBelowPlane = null;
		}
		this.testPoint2FixedZBelowPlane = testPoint2FixedZBelowPlane;
		this.testPoint1InSet = testPointInSet;
		this.testPoint2InSet = isInSet(testPoint2.x, testPoint2.y, testPoint2.z, testPoint1, testPoint1InSet, testPoint1FixedXPlane, testPoint1FixedXAbovePlane, testPoint1FixedXBelowPlane, testPoint1FixedYPlane, testPoint1FixedYAbovePlane, testPoint1FixedYBelowPlane, testPoint1FixedZPlane, testPoint1FixedZAbovePlane, testPoint1FixedZBelowPlane);
		assert (isInSet(testPoint1.x, testPoint1.y, testPoint1.z, testPoint2, testPoint2InSet, testPoint2FixedXPlane, testPoint2FixedXAbovePlane, testPoint2FixedXBelowPlane, testPoint2FixedYPlane, testPoint2FixedYAbovePlane, testPoint2FixedYBelowPlane, testPoint2FixedZPlane, testPoint2FixedZAbovePlane, testPoint2FixedZBelowPlane)) == (testPoint1InSet) : "Test point1 not correctly in/out of set according to test point2";
	}

	public GeoComplexPolygon(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, GeoComplexPolygon.readPointsList(planetModel, inputStream), new GeoPoint(planetModel, inputStream), SerializableObject.readBoolean(inputStream));
	}

	private static List<List<GeoPoint>> readPointsList(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		final int count = SerializableObject.readInt(inputStream);
		final List<List<GeoPoint>> array = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			array.add(Arrays.asList(SerializableObject.readPointArray(planetModel, inputStream)));
		}
		return array;
	}

	public void write(final OutputStream outputStream) throws IOException {
		GeoComplexPolygon.writePointsList(outputStream, pointsList);
		testPoint1.write(outputStream);
		SerializableObject.writeBoolean(outputStream, testPoint1InSet);
	}

	private static void writePointsList(final OutputStream outputStream, final List<List<GeoPoint>> pointsList) throws IOException {
		SerializableObject.writeInt(outputStream, pointsList.size());
		for (final List<GeoPoint> points : pointsList) {
			SerializableObject.writePointArray(outputStream, points);
		}
	}

	public boolean isWithin(final double x, final double y, final double z) {
		try {
			return isInSet(x, y, z, testPoint1, testPoint1InSet, testPoint1FixedXPlane, testPoint1FixedXAbovePlane, testPoint1FixedXBelowPlane, testPoint1FixedYPlane, testPoint1FixedYAbovePlane, testPoint1FixedYBelowPlane, testPoint1FixedZPlane, testPoint1FixedZAbovePlane, testPoint1FixedZBelowPlane);
		} catch (IllegalArgumentException e) {
			return isInSet(x, y, z, testPoint2, testPoint2InSet, testPoint2FixedXPlane, testPoint2FixedXAbovePlane, testPoint2FixedXBelowPlane, testPoint2FixedYPlane, testPoint2FixedYAbovePlane, testPoint2FixedYBelowPlane, testPoint2FixedZPlane, testPoint2FixedZAbovePlane, testPoint2FixedZBelowPlane);
		}
	}

	private boolean isInSet(final double x, final double y, final double z, final GeoPoint testPoint, final boolean testPointInSet, final Plane testPointFixedXPlane, final Plane testPointFixedXAbovePlane, final Plane testPointFixedXBelowPlane, final Plane testPointFixedYPlane, final Plane testPointFixedYAbovePlane, final Plane testPointFixedYBelowPlane, final Plane testPointFixedZPlane, final Plane testPointFixedZAbovePlane, final Plane testPointFixedZBelowPlane) {
		if (testPoint.isNumericallyIdentical(x, y, z)) {
			return testPointInSet;
		}
		if (((testPointFixedYAbovePlane != null) && (testPointFixedYBelowPlane != null)) && (testPointFixedYPlane.evaluateIsZero(x, y, z))) {
			final GeoComplexPolygon.CountingEdgeIterator crossingEdgeIterator = createLinearCrossingEdgeIterator(testPoint, testPointFixedYPlane, testPointFixedYAbovePlane, testPointFixedYBelowPlane, x, y, z);
			yTree.traverse(crossingEdgeIterator, testPoint.y);
			return (crossingEdgeIterator.isOnEdge()) || (((crossingEdgeIterator.getCrossingCount()) & 1) == 0 ? testPointInSet : !testPointInSet);
		}else
			if (((testPointFixedXAbovePlane != null) && (testPointFixedXBelowPlane != null)) && (testPointFixedXPlane.evaluateIsZero(x, y, z))) {
				final GeoComplexPolygon.CountingEdgeIterator crossingEdgeIterator = createLinearCrossingEdgeIterator(testPoint, testPointFixedXPlane, testPointFixedXAbovePlane, testPointFixedXBelowPlane, x, y, z);
				xTree.traverse(crossingEdgeIterator, testPoint.x);
				return (crossingEdgeIterator.isOnEdge()) || (((crossingEdgeIterator.getCrossingCount()) & 1) == 0 ? testPointInSet : !testPointInSet);
			}else
				if (((testPointFixedZAbovePlane != null) && (testPointFixedZBelowPlane != null)) && (testPointFixedZPlane.evaluateIsZero(x, y, z))) {
					final GeoComplexPolygon.CountingEdgeIterator crossingEdgeIterator = createLinearCrossingEdgeIterator(testPoint, testPointFixedZPlane, testPointFixedZAbovePlane, testPointFixedZBelowPlane, x, y, z);
					zTree.traverse(crossingEdgeIterator, testPoint.z);
					return (crossingEdgeIterator.isOnEdge()) || (((crossingEdgeIterator.getCrossingCount()) & 1) == 0 ? testPointInSet : !testPointInSet);
				}else {
					final Plane travelPlaneFixedX = new Plane(1.0, 0.0, 0.0, (-x));
					final Plane travelPlaneFixedY = new Plane(0.0, 1.0, 0.0, (-y));
					final Plane travelPlaneFixedZ = new Plane(0.0, 0.0, 1.0, (-z));
					Plane fixedYAbovePlane = new Plane(travelPlaneFixedY, true);
					Plane fixedYBelowPlane = new Plane(travelPlaneFixedY, false);
					Plane fixedXAbovePlane = new Plane(travelPlaneFixedX, true);
					Plane fixedXBelowPlane = new Plane(travelPlaneFixedX, false);
					Plane fixedZAbovePlane = new Plane(travelPlaneFixedZ, true);
					Plane fixedZBelowPlane = new Plane(travelPlaneFixedZ, false);
					final List<GeoComplexPolygon.TraversalStrategy> traversalStrategies = new ArrayList<>(12);
					if ((((testPointFixedYAbovePlane != null) && (testPointFixedYBelowPlane != null)) && (fixedXAbovePlane != null)) && (fixedXBelowPlane != null)) {
					}
					if ((((testPointFixedZAbovePlane != null) && (testPointFixedZBelowPlane != null)) && (fixedXAbovePlane != null)) && (fixedXBelowPlane != null)) {
					}
					if ((((testPointFixedXAbovePlane != null) && (testPointFixedXBelowPlane != null)) && (fixedYAbovePlane != null)) && (fixedYBelowPlane != null)) {
					}
					if ((((testPointFixedZAbovePlane != null) && (testPointFixedZBelowPlane != null)) && (fixedYAbovePlane != null)) && (fixedYBelowPlane != null)) {
					}
					if ((((testPointFixedXAbovePlane != null) && (testPointFixedXBelowPlane != null)) && (fixedZAbovePlane != null)) && (fixedZBelowPlane != null)) {
					}
					if ((((testPointFixedYAbovePlane != null) && (testPointFixedYBelowPlane != null)) && (fixedZAbovePlane != null)) && (fixedZBelowPlane != null)) {
					}
					Collections.sort(traversalStrategies);
					if ((traversalStrategies.size()) == 0) {
						throw new IllegalArgumentException("No dual-plane travel strategies were found");
					}
					for (final GeoComplexPolygon.TraversalStrategy ts : traversalStrategies) {
						try {
							return ts.apply(testPoint, testPointInSet, x, y, z);
						} catch (IllegalArgumentException e) {
						}
					}
					throw new IllegalArgumentException("Exhausted all traversal strategies");
				}


	}

	public GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public boolean intersects(final Plane p, final GeoPoint[] notablePoints, final Membership... bounds) {
		final GeoComplexPolygon.EdgeIterator intersector = new GeoComplexPolygon.IntersectorEdgeIterator(p, notablePoints, bounds);
		final XYZBounds xyzBounds = new XYZBounds();
		for (final GeoPoint point : notablePoints) {
			xyzBounds.addPoint(point);
		}
		if (((((((xyzBounds.getMaximumX()) == null) || ((xyzBounds.getMinimumX()) == null)) || ((xyzBounds.getMaximumY()) == null)) || ((xyzBounds.getMinimumY()) == null)) || ((xyzBounds.getMaximumZ()) == null)) || ((xyzBounds.getMinimumZ()) == null)) {
			return false;
		}
		final double xDelta = (xyzBounds.getMaximumX()) - (xyzBounds.getMinimumX());
		final double yDelta = (xyzBounds.getMaximumY()) - (xyzBounds.getMinimumY());
		final double zDelta = (xyzBounds.getMaximumZ()) - (xyzBounds.getMinimumZ());
		if ((xDelta <= yDelta) && (xDelta <= zDelta)) {
			return !(xTree.traverse(intersector, xyzBounds.getMinimumX(), xyzBounds.getMaximumX()));
		}else
			if ((yDelta <= xDelta) && (yDelta <= zDelta)) {
				return !(yTree.traverse(intersector, xyzBounds.getMinimumY(), xyzBounds.getMaximumY()));
			}else
				if ((zDelta <= xDelta) && (zDelta <= yDelta)) {
					return !(zTree.traverse(intersector, xyzBounds.getMinimumZ(), xyzBounds.getMaximumZ()));
				}


		return true;
	}

	public boolean intersects(GeoShape geoShape) {
		final GeoComplexPolygon.EdgeIterator intersector = new GeoComplexPolygon.IntersectorShapeIterator(geoShape);
		final XYZBounds xyzBounds = new XYZBounds();
		geoShape.getBounds(xyzBounds);
		final double xDelta = (xyzBounds.getMaximumX()) - (xyzBounds.getMinimumX());
		final double yDelta = (xyzBounds.getMaximumY()) - (xyzBounds.getMinimumY());
		final double zDelta = (xyzBounds.getMaximumZ()) - (xyzBounds.getMinimumZ());
		if ((xDelta <= yDelta) && (xDelta <= zDelta)) {
			return !(xTree.traverse(intersector, xyzBounds.getMinimumX(), xyzBounds.getMaximumX()));
		}else
			if ((yDelta <= xDelta) && (yDelta <= zDelta)) {
				return !(yTree.traverse(intersector, xyzBounds.getMinimumY(), xyzBounds.getMaximumY()));
			}else
				if ((zDelta <= xDelta) && (zDelta <= yDelta)) {
					return !(zTree.traverse(intersector, xyzBounds.getMinimumZ(), xyzBounds.getMaximumZ()));
				}


		return true;
	}

	public void getBounds(Bounds bounds) {
		for (final GeoComplexPolygon.Edge startEdge : shapeStartEdges) {
			GeoComplexPolygon.Edge currentEdge = startEdge;
			while (true) {
				bounds.addPoint(currentEdge.startPoint);
				currentEdge = currentEdge.next;
				if (currentEdge == startEdge) {
					break;
				}
			} 
		}
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		double minimumDistance = Double.POSITIVE_INFINITY;
		for (final GeoComplexPolygon.Edge shapeStartEdge : shapeStartEdges) {
			GeoComplexPolygon.Edge shapeEdge = shapeStartEdge;
			while (true) {
				final double newDist = distanceStyle.computeDistance(shapeEdge.startPoint, x, y, z);
				if (newDist < minimumDistance) {
					minimumDistance = newDist;
				}
				shapeEdge = shapeEdge.next;
				if (shapeEdge == shapeStartEdge) {
					break;
				}
			} 
		}
		return minimumDistance;
	}

	private GeoComplexPolygon.CountingEdgeIterator createLinearCrossingEdgeIterator(final GeoPoint testPoint, final Plane plane, final Plane abovePlane, final Plane belowPlane, final double thePointX, final double thePointY, final double thePointZ) {
		try {
			return new GeoComplexPolygon.SectorLinearCrossingEdgeIterator(testPoint, plane, abovePlane, belowPlane, thePointX, thePointY, thePointZ);
		} catch (IllegalArgumentException e) {
			return new GeoComplexPolygon.FullLinearCrossingEdgeIterator(testPoint, plane, abovePlane, belowPlane, thePointX, thePointY, thePointZ);
		}
	}

	private static final double[] halfProportions = new double[]{ 0.5 };

	private static class Edge {
		public final GeoPoint startPoint;

		public final GeoPoint endPoint;

		public final GeoPoint[] notablePoints;

		public final SidedPlane startPlane;

		public final SidedPlane endPlane;

		public final SidedPlane backingPlane;

		public final Plane plane;

		public final XYZBounds planeBounds;

		public GeoComplexPolygon.Edge previous = null;

		public GeoComplexPolygon.Edge next = null;

		public Edge(final PlanetModel pm, final GeoPoint startPoint, final GeoPoint endPoint) {
			this.startPoint = startPoint;
			this.endPoint = endPoint;
			this.notablePoints = new GeoPoint[]{ startPoint, endPoint };
			this.plane = new Plane(startPoint, endPoint);
			this.startPlane = new SidedPlane(endPoint, plane, startPoint);
			this.endPlane = new SidedPlane(startPoint, plane, endPoint);
			final GeoPoint interpolationPoint = plane.interpolate(startPoint, endPoint, GeoComplexPolygon.halfProportions)[0];
			this.backingPlane = new SidedPlane(interpolationPoint, interpolationPoint, 0.0);
			this.planeBounds = new XYZBounds();
			this.planeBounds.addPoint(startPoint);
			this.planeBounds.addPoint(endPoint);
			this.planeBounds.addPlane(pm, this.plane, this.startPlane, this.endPlane, this.backingPlane);
		}

		public boolean isWithin(final double thePointX, final double thePointY, final double thePointZ) {
			return (((plane.evaluateIsZero(thePointX, thePointY, thePointZ)) && (startPlane.isWithin(thePointX, thePointY, thePointZ))) && (endPlane.isWithin(thePointX, thePointY, thePointZ))) && (backingPlane.isWithin(thePointX, thePointY, thePointZ));
		}
	}

	private class TraversalStrategy implements Comparable<GeoComplexPolygon.TraversalStrategy> {
		private final double traversalDistance;

		private final double firstLegValue;

		private final double secondLegValue;

		private final Plane firstLegPlane;

		private final Plane firstLegAbovePlane;

		private final Plane firstLegBelowPlane;

		private final Plane secondLegPlane;

		private final Plane secondLegAbovePlane;

		private final Plane secondLegBelowPlane;

		private final GeoComplexPolygon.Tree firstLegTree;

		private final GeoComplexPolygon.Tree secondLegTree;

		private final GeoPoint intersectionPoint;

		public TraversalStrategy(final double traversalDistance, final double firstLegValue, final double secondLegValue, final Plane firstLegPlane, final Plane firstLegAbovePlane, final Plane firstLegBelowPlane, final Plane secondLegPlane, final Plane secondLegAbovePlane, final Plane secondLegBelowPlane, final GeoComplexPolygon.Tree firstLegTree, final GeoComplexPolygon.Tree secondLegTree, final GeoPoint intersectionPoint) {
			this.traversalDistance = traversalDistance;
			this.firstLegValue = firstLegValue;
			this.secondLegValue = secondLegValue;
			this.firstLegPlane = firstLegPlane;
			this.firstLegAbovePlane = firstLegAbovePlane;
			this.firstLegBelowPlane = firstLegBelowPlane;
			this.secondLegPlane = secondLegPlane;
			this.secondLegAbovePlane = secondLegAbovePlane;
			this.secondLegBelowPlane = secondLegBelowPlane;
			this.firstLegTree = firstLegTree;
			this.secondLegTree = secondLegTree;
			this.intersectionPoint = intersectionPoint;
		}

		public boolean apply(final GeoPoint testPoint, final boolean testPointInSet, final double x, final double y, final double z) {
			try {
				final GeoComplexPolygon.CountingEdgeIterator testPointEdgeIterator = createLinearCrossingEdgeIterator(testPoint, firstLegPlane, firstLegAbovePlane, firstLegBelowPlane, intersectionPoint.x, intersectionPoint.y, intersectionPoint.z);
				firstLegTree.traverse(testPointEdgeIterator, firstLegValue);
				final boolean intersectionPointOnEdge = testPointEdgeIterator.isOnEdge();
				if (intersectionPointOnEdge) {
					throw new IllegalArgumentException("Intersection point landed on an edge -- illegal path");
				}
				final boolean intersectionPointInSet = intersectionPointOnEdge || (((testPointEdgeIterator.getCrossingCount()) & 1) == 0 ? testPointInSet : !testPointInSet);
				final GeoComplexPolygon.CountingEdgeIterator travelEdgeIterator = createLinearCrossingEdgeIterator(intersectionPoint, secondLegPlane, secondLegAbovePlane, secondLegBelowPlane, x, y, z);
				secondLegTree.traverse(travelEdgeIterator, secondLegValue);
				final boolean rval = (travelEdgeIterator.isOnEdge()) || (((travelEdgeIterator.getCrossingCount()) & 1) == 0 ? intersectionPointInSet : !intersectionPointInSet);
				return rval;
			} catch (IllegalArgumentException e) {
				final GeoComplexPolygon.CountingEdgeIterator edgeIterator = new GeoComplexPolygon.DualCrossingEdgeIterator(testPoint, firstLegPlane, firstLegAbovePlane, firstLegBelowPlane, secondLegPlane, secondLegAbovePlane, secondLegBelowPlane, x, y, z, intersectionPoint);
				firstLegTree.traverse(edgeIterator, firstLegValue);
				if (edgeIterator.isOnEdge()) {
					return true;
				}
				secondLegTree.traverse(edgeIterator, secondLegValue);
				return (edgeIterator.isOnEdge()) || (((edgeIterator.getCrossingCount()) & 1) == 0 ? testPointInSet : !testPointInSet);
			}
		}

		@Override
		public int compareTo(final GeoComplexPolygon.TraversalStrategy other) {
			if ((traversalDistance) < (other.traversalDistance)) {
				return -1;
			}else
				if ((traversalDistance) > (other.traversalDistance)) {
					return 1;
				}

			return 0;
		}
	}

	private static interface EdgeIterator {
		public abstract boolean matches(final GeoComplexPolygon.Edge edge);
	}

	private static interface CountingEdgeIterator extends GeoComplexPolygon.EdgeIterator {
		public abstract int getCrossingCount();

		public abstract boolean isOnEdge();
	}

	private static class Node {
		public final GeoComplexPolygon.Edge edge;

		public final double low;

		public final double high;

		public GeoComplexPolygon.Node left = null;

		public GeoComplexPolygon.Node right = null;

		public double max;

		public Node(final GeoComplexPolygon.Edge edge, final double minimumValue, final double maximumValue) {
			this.edge = edge;
			this.low = minimumValue;
			this.high = maximumValue;
			this.max = maximumValue;
		}

		public boolean traverse(final GeoComplexPolygon.EdgeIterator edgeIterator, final double minValue, final double maxValue) {
			if (minValue <= (max)) {
				if ((minValue <= (high)) && (maxValue >= (low))) {
					if ((edgeIterator.matches(edge)) == false) {
						return false;
					}
				}
				if (((left) != null) && ((left.traverse(edgeIterator, minValue, maxValue)) == false)) {
					return false;
				}
				if ((((right) != null) && (maxValue >= (low))) && ((right.traverse(edgeIterator, minValue, maxValue)) == false)) {
					return false;
				}
			}
			return true;
		}
	}

	private static abstract class Tree {
		private final GeoComplexPolygon.Node rootNode;

		protected static final GeoComplexPolygon.Edge[] EMPTY_ARRAY = new GeoComplexPolygon.Edge[0];

		public Tree(final List<GeoComplexPolygon.Edge> allEdges) {
			final GeoComplexPolygon.Node[] edges = new GeoComplexPolygon.Node[allEdges.size()];
			int i = 0;
			for (final GeoComplexPolygon.Edge edge : allEdges) {
				edges[(i++)] = new GeoComplexPolygon.Node(edge, getMinimum(edge), getMaximum(edge));
			}
			Arrays.sort(edges, ( left, right) -> {
				int ret = Double.compare(left.low, right.low);
				if (ret == 0) {
					ret = Double.compare(left.max, right.max);
				}
				return ret;
			});
			rootNode = GeoComplexPolygon.Tree.createTree(edges, 0, ((edges.length) - 1));
		}

		private static GeoComplexPolygon.Node createTree(final GeoComplexPolygon.Node[] edges, final int low, final int high) {
			if (low > high) {
				return null;
			}
			int mid = (low + high) >>> 1;
			final GeoComplexPolygon.Node newNode = edges[mid];
			newNode.left = GeoComplexPolygon.Tree.createTree(edges, low, (mid - 1));
			newNode.right = GeoComplexPolygon.Tree.createTree(edges, (mid + 1), high);
			if ((newNode.left) != null) {
				newNode.max = Math.max(newNode.max, newNode.left.max);
			}
			if ((newNode.right) != null) {
				newNode.max = Math.max(newNode.max, newNode.right.max);
			}
			return newNode;
		}

		protected abstract double getMinimum(final GeoComplexPolygon.Edge edge);

		protected abstract double getMaximum(final GeoComplexPolygon.Edge edge);

		public boolean traverse(final GeoComplexPolygon.EdgeIterator edgeIterator, final double value) {
			return traverse(edgeIterator, value, value);
		}

		public boolean traverse(final GeoComplexPolygon.EdgeIterator edgeIterator, final double minValue, final double maxValue) {
			if ((rootNode) == null) {
				return true;
			}
			return rootNode.traverse(edgeIterator, minValue, maxValue);
		}
	}

	private static class ZTree extends GeoComplexPolygon.Tree {
		public GeoComplexPolygon.Node rootNode = null;

		public ZTree(final List<GeoComplexPolygon.Edge> allEdges) {
			super(allEdges);
		}

		@Override
		protected double getMinimum(final GeoComplexPolygon.Edge edge) {
			return edge.planeBounds.getMinimumZ();
		}

		@Override
		protected double getMaximum(final GeoComplexPolygon.Edge edge) {
			return edge.planeBounds.getMaximumZ();
		}
	}

	private static class YTree extends GeoComplexPolygon.Tree {
		public YTree(final List<GeoComplexPolygon.Edge> allEdges) {
			super(allEdges);
		}

		@Override
		protected double getMinimum(final GeoComplexPolygon.Edge edge) {
			return edge.planeBounds.getMinimumY();
		}

		@Override
		protected double getMaximum(final GeoComplexPolygon.Edge edge) {
			return edge.planeBounds.getMaximumY();
		}
	}

	private static class XTree extends GeoComplexPolygon.Tree {
		public XTree(final List<GeoComplexPolygon.Edge> allEdges) {
			super(allEdges);
		}

		@Override
		protected double getMinimum(final GeoComplexPolygon.Edge edge) {
			return edge.planeBounds.getMinimumX();
		}

		@Override
		protected double getMaximum(final GeoComplexPolygon.Edge edge) {
			return edge.planeBounds.getMaximumX();
		}
	}

	private class IntersectorEdgeIterator implements GeoComplexPolygon.EdgeIterator {
		private final Plane plane;

		private final GeoPoint[] notablePoints;

		private final Membership[] bounds;

		public IntersectorEdgeIterator(final Plane plane, final GeoPoint[] notablePoints, final Membership... bounds) {
			this.plane = plane;
			this.notablePoints = notablePoints;
			this.bounds = bounds;
		}

		@Override
		public boolean matches(final GeoComplexPolygon.Edge edge) {
			return false;
		}
	}

	private class IntersectorShapeIterator implements GeoComplexPolygon.EdgeIterator {
		private final GeoShape shape;

		public IntersectorShapeIterator(final GeoShape shape) {
			this.shape = shape;
		}

		@Override
		public boolean matches(final GeoComplexPolygon.Edge edge) {
			return !(shape.intersects(edge.plane, edge.notablePoints, edge.startPlane, edge.endPlane));
		}
	}

	private class FullLinearCrossingEdgeIterator implements GeoComplexPolygon.CountingEdgeIterator {
		private final GeoPoint testPoint;

		private final Plane plane;

		private final Plane abovePlane;

		private final Plane belowPlane;

		private final Membership bound;

		private final double thePointX;

		private final double thePointY;

		private final double thePointZ;

		private boolean onEdge = false;

		private int aboveCrossingCount = 0;

		private int belowCrossingCount = 0;

		public FullLinearCrossingEdgeIterator(final GeoPoint testPoint, final Plane plane, final Plane abovePlane, final Plane belowPlane, final double thePointX, final double thePointY, final double thePointZ) {
			assert plane.evaluateIsZero(thePointX, thePointY, thePointZ) : "Check point is not on travel plane";
			assert plane.evaluateIsZero(testPoint) : "Test point is not on travel plane";
			this.testPoint = testPoint;
			this.plane = plane;
			this.abovePlane = abovePlane;
			this.belowPlane = belowPlane;
			if (plane.isNumericallyIdentical(testPoint)) {
				throw new IllegalArgumentException("Plane vector identical to testpoint vector");
			}
			this.bound = new SidedPlane(plane, testPoint);
			this.thePointX = thePointX;
			this.thePointY = thePointY;
			this.thePointZ = thePointZ;
		}

		@Override
		public int getCrossingCount() {
			return Math.min(aboveCrossingCount, belowCrossingCount);
		}

		@Override
		public boolean isOnEdge() {
			return onEdge;
		}

		@Override
		public boolean matches(final GeoComplexPolygon.Edge edge) {
			if (edge.isWithin(thePointX, thePointY, thePointZ)) {
				onEdge = true;
				return false;
			}
			final int aboveCrossings = countCrossings(edge, abovePlane, bound);
			aboveCrossingCount += aboveCrossings;
			final int belowCrossings = countCrossings(edge, belowPlane, bound);
			belowCrossingCount += belowCrossings;
			return true;
		}

		private int countCrossings(final GeoComplexPolygon.Edge edge, final Plane envelopePlane, final Membership envelopeBound) {
			int crossings = 0;
			return crossings;
		}

		private boolean edgeCrossesEnvelope(final Plane edgePlane, final GeoPoint intersectionPoint, final Plane envelopePlane) {
			final GeoPoint[] adjoiningPoints = findAdjoiningPoints(edgePlane, intersectionPoint, envelopePlane);
			if (adjoiningPoints == null) {
				return true;
			}
			int withinCount = 0;
			for (final GeoPoint adjoining : adjoiningPoints) {
				if ((plane.evaluateIsZero(adjoining)) && (bound.isWithin(adjoining))) {
					withinCount++;
				}
			}
			return (withinCount & 1) != 0;
		}
	}

	private class SectorLinearCrossingEdgeIterator implements GeoComplexPolygon.CountingEdgeIterator {
		private final GeoPoint testPoint;

		private final Plane plane;

		private final Plane abovePlane;

		private final Plane belowPlane;

		private final Membership bound1;

		private final Membership bound2;

		private final double thePointX;

		private final double thePointY;

		private final double thePointZ;

		private boolean onEdge = false;

		private int aboveCrossingCount = 0;

		private int belowCrossingCount = 0;

		public SectorLinearCrossingEdgeIterator(final GeoPoint testPoint, final Plane plane, final Plane abovePlane, final Plane belowPlane, final double thePointX, final double thePointY, final double thePointZ) {
			assert plane.evaluateIsZero(thePointX, thePointY, thePointZ) : "Check point is not on travel plane";
			assert plane.evaluateIsZero(testPoint) : "Test point is not on travel plane";
			this.testPoint = testPoint;
			this.plane = plane;
			this.abovePlane = abovePlane;
			this.belowPlane = belowPlane;
			final SidedPlane bound1Plane = new SidedPlane(thePointX, thePointY, thePointZ, plane, testPoint);
			final SidedPlane bound2Plane = new SidedPlane(testPoint, plane, thePointX, thePointY, thePointZ);
			if (bound1Plane.isNumericallyIdentical(bound2Plane)) {
				throw new IllegalArgumentException("Sector iterator unreliable when bounds planes are numerically identical");
			}
			this.bound1 = bound1Plane;
			this.bound2 = bound2Plane;
			this.thePointX = thePointX;
			this.thePointY = thePointY;
			this.thePointZ = thePointZ;
		}

		@Override
		public int getCrossingCount() {
			return Math.min(aboveCrossingCount, belowCrossingCount);
		}

		@Override
		public boolean isOnEdge() {
			return onEdge;
		}

		@Override
		public boolean matches(final GeoComplexPolygon.Edge edge) {
			if (edge.isWithin(thePointX, thePointY, thePointZ)) {
				onEdge = true;
				return false;
			}
			final int aboveCrossings = countCrossings(edge, abovePlane, bound1, bound2);
			aboveCrossingCount += aboveCrossings;
			final int belowCrossings = countCrossings(edge, belowPlane, bound1, bound2);
			belowCrossingCount += belowCrossings;
			return true;
		}

		private int countCrossings(final GeoComplexPolygon.Edge edge, final Plane envelopePlane, final Membership envelopeBound1, final Membership envelopeBound2) {
			int crossings = 0;
			return crossings;
		}

		private boolean edgeCrossesEnvelope(final Plane edgePlane, final GeoPoint intersectionPoint, final Plane envelopePlane) {
			final GeoPoint[] adjoiningPoints = findAdjoiningPoints(edgePlane, intersectionPoint, envelopePlane);
			if (adjoiningPoints == null) {
				return true;
			}
			int withinCount = 0;
			for (final GeoPoint adjoining : adjoiningPoints) {
				if (((plane.evaluateIsZero(adjoining)) && (bound1.isWithin(adjoining))) && (bound2.isWithin(adjoining))) {
					withinCount++;
				}else {
				}
			}
			return (withinCount & 1) != 0;
		}
	}

	private class DualCrossingEdgeIterator implements GeoComplexPolygon.CountingEdgeIterator {
		private Set<GeoComplexPolygon.Edge> seenEdges = null;

		private final GeoPoint testPoint;

		private final Plane testPointPlane;

		private final Plane testPointAbovePlane;

		private final Plane testPointBelowPlane;

		private final Plane travelPlane;

		private final Plane travelAbovePlane;

		private final Plane travelBelowPlane;

		private final double thePointX;

		private final double thePointY;

		private final double thePointZ;

		private final GeoPoint intersectionPoint;

		private final SidedPlane testPointCutoffPlane;

		private final SidedPlane checkPointCutoffPlane;

		private final SidedPlane testPointOtherCutoffPlane;

		private final SidedPlane checkPointOtherCutoffPlane;

		private boolean computedInsideOutside = false;

		private Plane testPointInsidePlane;

		private Plane testPointOutsidePlane;

		private Plane travelInsidePlane;

		private Plane travelOutsidePlane;

		private SidedPlane insideTestPointCutoffPlane;

		private SidedPlane insideTravelCutoffPlane;

		private SidedPlane outsideTestPointCutoffPlane;

		private SidedPlane outsideTravelCutoffPlane;

		private boolean onEdge = false;

		private int innerCrossingCount = 0;

		private int outerCrossingCount = 0;

		public DualCrossingEdgeIterator(final GeoPoint testPoint, final Plane testPointPlane, final Plane testPointAbovePlane, final Plane testPointBelowPlane, final Plane travelPlane, final Plane travelAbovePlane, final Plane travelBelowPlane, final double thePointX, final double thePointY, final double thePointZ, final GeoPoint intersectionPoint) {
			this.testPoint = testPoint;
			this.testPointPlane = testPointPlane;
			this.testPointAbovePlane = testPointAbovePlane;
			this.testPointBelowPlane = testPointBelowPlane;
			this.travelPlane = travelPlane;
			this.travelAbovePlane = travelAbovePlane;
			this.travelBelowPlane = travelBelowPlane;
			this.thePointX = thePointX;
			this.thePointY = thePointY;
			this.thePointZ = thePointZ;
			this.intersectionPoint = intersectionPoint;
			assert travelPlane.evaluateIsZero(intersectionPoint) : "intersection point must be on travel plane";
			assert testPointPlane.evaluateIsZero(intersectionPoint) : "intersection point must be on test point plane";
			assert !(testPoint.isNumericallyIdentical(intersectionPoint)) : "test point is the same as intersection point";
			assert !(intersectionPoint.isNumericallyIdentical(thePointX, thePointY, thePointZ)) : "check point is same as intersection point";
			final SidedPlane testPointBound1 = new SidedPlane(intersectionPoint, testPointPlane, testPoint);
			final SidedPlane testPointBound2 = new SidedPlane(testPoint, testPointPlane, intersectionPoint);
			if (testPointBound1.isNumericallyIdentical(testPointBound2)) {
				throw new IllegalArgumentException("Dual iterator unreliable when bounds planes are numerically identical");
			}
			this.testPointCutoffPlane = testPointBound1;
			this.testPointOtherCutoffPlane = testPointBound2;
			final SidedPlane checkPointBound1 = new SidedPlane(intersectionPoint, travelPlane, thePointX, thePointY, thePointZ);
			final SidedPlane checkPointBound2 = new SidedPlane(thePointX, thePointY, thePointZ, travelPlane, intersectionPoint);
			if (checkPointBound1.isNumericallyIdentical(checkPointBound2)) {
				throw new IllegalArgumentException("Dual iterator unreliable when bounds planes are numerically identical");
			}
			this.checkPointCutoffPlane = checkPointBound1;
			this.checkPointOtherCutoffPlane = checkPointBound2;
			assert testPointCutoffPlane.isWithin(intersectionPoint) : "intersection must be within testPointCutoffPlane";
			assert testPointOtherCutoffPlane.isWithin(intersectionPoint) : "intersection must be within testPointOtherCutoffPlane";
			assert checkPointCutoffPlane.isWithin(intersectionPoint) : "intersection must be within checkPointCutoffPlane";
			assert checkPointOtherCutoffPlane.isWithin(intersectionPoint) : "intersection must be within checkPointOtherCutoffPlane";
		}

		protected void computeInsideOutside() {
			if (!(computedInsideOutside)) {
				final Membership intersectionBound1 = new SidedPlane(testPoint, travelPlane, travelPlane.D);
				final Membership intersectionBound2 = new SidedPlane(thePointX, thePointY, thePointZ, testPointPlane, testPointPlane.D);
				assert intersectionBound1.isWithin(intersectionPoint) : "intersection must be within intersectionBound1";
				assert intersectionBound2.isWithin(intersectionPoint) : "intersection must be within intersectionBound2";
				final GeoPoint[] insideInsidePoints;
				insideInsidePoints = null;
				final GeoPoint insideInsidePoint = pickProximate(insideInsidePoints);
				insideTravelCutoffPlane = new SidedPlane(thePointX, thePointY, thePointZ, travelInsidePlane, insideInsidePoint);
				insideTestPointCutoffPlane = new SidedPlane(testPoint, testPointInsidePlane, insideInsidePoint);
				computedInsideOutside = true;
			}
		}

		private GeoPoint pickProximate(final GeoPoint[] points) {
			if ((points.length) == 0) {
				throw new IllegalArgumentException("No off-plane intersection points were found; can't compute traversal");
			}else
				if ((points.length) == 1) {
					return points[0];
				}else {
					final double p1dist = GeoComplexPolygon.computeSquaredDistance(points[0], intersectionPoint);
					final double p2dist = GeoComplexPolygon.computeSquaredDistance(points[1], intersectionPoint);
					if (p1dist < p2dist) {
						return points[0];
					}else
						if (p2dist < p1dist) {
							return points[1];
						}else {
							throw new IllegalArgumentException(((((("Neither off-plane intersection point matched intersection point; intersection = " + (intersectionPoint)) + "; offplane choice 0: ") + (points[0])) + "; offplane choice 1: ") + (points[1])));
						}

				}

		}

		@Override
		public int getCrossingCount() {
			return Math.min(innerCrossingCount, outerCrossingCount);
		}

		@Override
		public boolean isOnEdge() {
			return onEdge;
		}

		@Override
		public boolean matches(final GeoComplexPolygon.Edge edge) {
			if (edge.isWithin(thePointX, thePointY, thePointZ)) {
				onEdge = true;
				return false;
			}
			if (((seenEdges) != null) && (seenEdges.contains(edge))) {
				return true;
			}
			if ((seenEdges) == null) {
				seenEdges = new HashSet<>();
			}
			seenEdges.add(edge);
			computeInsideOutside();
			innerCrossingCount += countCrossings(edge, travelInsidePlane, checkPointCutoffPlane, insideTravelCutoffPlane, testPointInsidePlane, testPointCutoffPlane, insideTestPointCutoffPlane);
			outerCrossingCount += countCrossings(edge, travelOutsidePlane, checkPointCutoffPlane, outsideTravelCutoffPlane, testPointOutsidePlane, testPointCutoffPlane, outsideTestPointCutoffPlane);
			return true;
		}

		private int countCrossings(final GeoComplexPolygon.Edge edge, final Plane travelEnvelopePlane, final Membership travelEnvelopeBound1, final Membership travelEnvelopeBound2, final Plane testPointEnvelopePlane, final Membership testPointEnvelopeBound1, final Membership testPointEnvelopeBound2) {
			int crossings = 0;
			return crossings;
		}

		private boolean edgeCrossesEnvelope(final Plane edgePlane, final GeoPoint intersectionPoint, final Plane envelopePlane) {
			final GeoPoint[] adjoiningPoints = findAdjoiningPoints(edgePlane, intersectionPoint, envelopePlane);
			if (adjoiningPoints == null) {
				return true;
			}
			int withinCount = 0;
			for (final GeoPoint adjoining : adjoiningPoints) {
				if ((((travelPlane.evaluateIsZero(adjoining)) && (checkPointCutoffPlane.isWithin(adjoining))) && (checkPointOtherCutoffPlane.isWithin(adjoining))) || (((testPointPlane.evaluateIsZero(adjoining)) && (testPointCutoffPlane.isWithin(adjoining))) && (testPointOtherCutoffPlane.isWithin(adjoining)))) {
					withinCount++;
				}else {
				}
			}
			return (withinCount & 1) != 0;
		}
	}

	private static final double DELTA_DISTANCE = Vector.MINIMUM_RESOLUTION;

	private static final int MAX_ITERATIONS = 100;

	private static final double OFF_PLANE_AMOUNT = (Vector.MINIMUM_RESOLUTION) * 0.1;

	private GeoPoint[] findAdjoiningPoints(final Plane plane, final GeoPoint pointOnPlane, final Plane envelopePlane) {
		final Vector perpendicular = new Vector(plane, pointOnPlane);
		double distanceFactor = 0.0;
		for (int i = 0; i < (GeoComplexPolygon.MAX_ITERATIONS); i++) {
			distanceFactor += GeoComplexPolygon.DELTA_DISTANCE;
		}
		return null;
	}

	private static double computeSquaredDistance(final GeoPoint checkPoint, final GeoPoint intersectionPoint) {
		final double distanceX = (checkPoint.x) - (intersectionPoint.x);
		final double distanceY = (checkPoint.y) - (intersectionPoint.y);
		final double distanceZ = (checkPoint.z) - (intersectionPoint.z);
		return ((distanceX * distanceX) + (distanceY * distanceY)) + (distanceZ * distanceZ);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoComplexPolygon))
			return false;

		final GeoComplexPolygon other = ((GeoComplexPolygon) (o));
		return (((super.equals(other)) && ((testPoint1InSet) == (other.testPoint1InSet))) && (testPoint1.equals(testPoint1))) && (pointsList.equals(other.pointsList));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (Boolean.hashCode(testPoint1InSet));
		result = (31 * result) + (testPoint1.hashCode());
		result = (31 * result) + (pointsList.hashCode());
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder edgeDescription = new StringBuilder();
		for (final GeoComplexPolygon.Edge shapeStartEdge : shapeStartEdges) {
			GeoComplexPolygon.fillInEdgeDescription(edgeDescription, shapeStartEdge);
		}
		return null;
	}

	private static void fillInEdgeDescription(final StringBuilder description, final GeoComplexPolygon.Edge startEdge) {
		description.append(" {");
		GeoComplexPolygon.Edge currentEdge = startEdge;
		int edgeCounter = 0;
		while (true) {
			if (edgeCounter > 0) {
				description.append(", ");
			}
			if (edgeCounter >= 20) {
				description.append("...");
				break;
			}
			description.append(currentEdge.startPoint);
			currentEdge = currentEdge.next;
			if (currentEdge == startEdge) {
				break;
			}
			edgeCounter++;
		} 
	}
}

