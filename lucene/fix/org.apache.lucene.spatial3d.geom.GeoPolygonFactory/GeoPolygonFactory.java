

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.lucene.spatial3d.geom.GeoCompositePolygon;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoPolygon;
import org.apache.lucene.spatial3d.geom.Membership;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.SidedPlane;
import org.apache.lucene.spatial3d.geom.Vector;


public class GeoPolygonFactory {
	private GeoPolygonFactory() {
	}

	private static final int SMALL_POLYGON_CUTOFF_EDGES = 100;

	public static GeoPolygon makeGeoConcavePolygon(final PlanetModel planetModel, final List<GeoPoint> pointList) {
		return null;
	}

	public static GeoPolygon makeGeoConvexPolygon(final PlanetModel planetModel, final List<GeoPoint> pointList) {
		return null;
	}

	public static GeoPolygon makeGeoConcavePolygon(final PlanetModel planetModel, final List<GeoPoint> pointList, final List<GeoPolygon> holes) {
		return null;
	}

	public static GeoPolygon makeGeoConvexPolygon(final PlanetModel planetModel, final List<GeoPoint> pointList, final List<GeoPolygon> holes) {
		return null;
	}

	public static class PolygonDescription {
		public final List<? extends GeoPoint> points;

		public final List<? extends GeoPolygonFactory.PolygonDescription> holes;

		public PolygonDescription(final List<? extends GeoPoint> points) {
			this(points, new ArrayList<>());
		}

		public PolygonDescription(final List<? extends GeoPoint> points, final List<? extends GeoPolygonFactory.PolygonDescription> holes) {
			this.points = points;
			this.holes = holes;
		}
	}

	public static GeoPolygon makeGeoPolygon(final PlanetModel planetModel, final GeoPolygonFactory.PolygonDescription description) {
		return GeoPolygonFactory.makeGeoPolygon(planetModel, description, 0.0);
	}

	public static GeoPolygon makeGeoPolygon(final PlanetModel planetModel, final GeoPolygonFactory.PolygonDescription description, final double leniencyValue) {
		final List<GeoPolygon> holes;
		if (((description.holes) != null) && ((description.holes.size()) > 0)) {
			holes = new ArrayList<>(description.holes.size());
			for (final GeoPolygonFactory.PolygonDescription holeDescription : description.holes) {
				final GeoPolygon gp = GeoPolygonFactory.makeGeoPolygon(planetModel, holeDescription, leniencyValue);
				if (gp == null) {
					return null;
				}
				holes.add(gp);
			}
		}else {
			holes = null;
		}
		if ((description.points.size()) <= (GeoPolygonFactory.SMALL_POLYGON_CUTOFF_EDGES)) {
			final List<GeoPoint> firstFilteredPointList = GeoPolygonFactory.filterPoints(description.points);
			if (firstFilteredPointList == null) {
				return null;
			}
			final List<GeoPoint> filteredPointList = GeoPolygonFactory.filterEdges(firstFilteredPointList, leniencyValue);
			if (filteredPointList == null) {
				return null;
			}
			try {
				final GeoPoint centerOfMass = GeoPolygonFactory.getCenterOfMass(planetModel, filteredPointList);
				final Boolean isCenterOfMassInside = GeoPolygonFactory.isInsidePolygon(centerOfMass, filteredPointList);
				if (isCenterOfMassInside != null) {
					return GeoPolygonFactory.generateGeoPolygon(planetModel, filteredPointList, holes, centerOfMass, isCenterOfMassInside);
				}
				final Random generator = new Random(1234);
				for (int counter = 0; counter < 1000000; counter++) {
					final GeoPoint pole = GeoPolygonFactory.pickPole(generator, planetModel, filteredPointList);
					final Boolean isPoleInside = GeoPolygonFactory.isInsidePolygon(pole, filteredPointList);
					if (isPoleInside != null) {
						return GeoPolygonFactory.generateGeoPolygon(planetModel, filteredPointList, holes, pole, isPoleInside);
					}
				}
				throw new IllegalArgumentException(("cannot find a point that is inside the polygon " + filteredPointList));
			} catch (GeoPolygonFactory.TileException e) {
			}
		}
		final List<GeoPolygonFactory.PolygonDescription> pd = new ArrayList<>(1);
		pd.add(description);
		return GeoPolygonFactory.makeLargeGeoPolygon(planetModel, pd);
	}

	public static GeoPolygon makeGeoPolygon(final PlanetModel planetModel, final List<GeoPoint> pointList) {
		return GeoPolygonFactory.makeGeoPolygon(planetModel, pointList, null);
	}

	public static GeoPolygon makeGeoPolygon(final PlanetModel planetModel, final List<GeoPoint> pointList, final List<GeoPolygon> holes) {
		return GeoPolygonFactory.makeGeoPolygon(planetModel, pointList, holes, 0.0);
	}

	public static GeoPolygon makeGeoPolygon(final PlanetModel planetModel, final List<GeoPoint> pointList, final List<GeoPolygon> holes, final double leniencyValue) {
		final List<GeoPoint> firstFilteredPointList = GeoPolygonFactory.filterPoints(pointList);
		if (firstFilteredPointList == null) {
			return null;
		}
		final List<GeoPoint> filteredPointList = GeoPolygonFactory.filterEdges(firstFilteredPointList, leniencyValue);
		if (filteredPointList == null) {
			return null;
		}
		try {
			final GeoPoint centerOfMass = GeoPolygonFactory.getCenterOfMass(planetModel, filteredPointList);
			final Boolean isCenterOfMassInside = GeoPolygonFactory.isInsidePolygon(centerOfMass, filteredPointList);
			if (isCenterOfMassInside != null) {
				return GeoPolygonFactory.generateGeoPolygon(planetModel, filteredPointList, holes, centerOfMass, isCenterOfMassInside);
			}
			final Random generator = new Random(1234);
			for (int counter = 0; counter < 1000000; counter++) {
				final GeoPoint pole = GeoPolygonFactory.pickPole(generator, planetModel, filteredPointList);
				final Boolean isPoleInside = GeoPolygonFactory.isInsidePolygon(pole, filteredPointList);
				if (isPoleInside != null) {
					return GeoPolygonFactory.generateGeoPolygon(planetModel, filteredPointList, holes, pole, isPoleInside);
				}
			}
			throw new IllegalArgumentException(("cannot find a point that is inside the polygon " + filteredPointList));
		} catch (GeoPolygonFactory.TileException e) {
			if ((holes != null) && ((holes.size()) > 0)) {
				throw new IllegalArgumentException(e.getMessage());
			}
			final List<GeoPolygonFactory.PolygonDescription> description = new ArrayList<>(1);
			description.add(new GeoPolygonFactory.PolygonDescription(pointList));
			return GeoPolygonFactory.makeLargeGeoPolygon(planetModel, description);
		}
	}

	private static GeoPoint getCenterOfMass(final PlanetModel planetModel, final List<GeoPoint> points) {
		double x = 0;
		double y = 0;
		double z = 0;
		for (final GeoPoint point : points) {
			x += point.x;
			y += point.y;
			z += point.z;
		}
		return planetModel.createSurfacePoint(x, y, z);
	}

	public static GeoPolygon makeLargeGeoPolygon(final PlanetModel planetModel, final List<GeoPolygonFactory.PolygonDescription> shapesList) {
		final List<List<GeoPoint>> pointsList = new ArrayList<>();
		GeoPolygonFactory.BestShape testPointShape = null;
		for (final GeoPolygonFactory.PolygonDescription shape : shapesList) {
			testPointShape = GeoPolygonFactory.convertPolygon(pointsList, shape, testPointShape, true);
		}
		if (testPointShape == null) {
			throw new IllegalArgumentException("couldn't find a non-degenerate polygon for in-set determination");
		}
		final GeoPoint centerOfMass = GeoPolygonFactory.getCenterOfMass(planetModel, testPointShape.points);
		final Random generator = new Random(1234);
		for (int counter = 0; counter < 1000000; counter++) {
			final GeoPoint pole = GeoPolygonFactory.pickPole(generator, planetModel, testPointShape.points);
		}
		throw new IllegalArgumentException(("cannot find a point that is inside the polygon " + testPointShape));
	}

	private static GeoPolygonFactory.BestShape convertPolygon(final List<List<GeoPoint>> pointsList, final GeoPolygonFactory.PolygonDescription shape, GeoPolygonFactory.BestShape testPointShape, final boolean mustBeInside) {
		final List<GeoPoint> filteredPoints = GeoPolygonFactory.filterPoints(shape.points);
		if (filteredPoints == null) {
			return testPointShape;
		}
		if ((shape.holes.size()) == 0) {
			if ((testPointShape == null) || ((testPointShape.points.size()) > (filteredPoints.size()))) {
				testPointShape = new GeoPolygonFactory.BestShape(filteredPoints, mustBeInside);
			}
		}
		pointsList.add(filteredPoints);
		for (final GeoPolygonFactory.PolygonDescription hole : shape.holes) {
			testPointShape = GeoPolygonFactory.convertPolygon(pointsList, hole, testPointShape, (!mustBeInside));
		}
		return testPointShape;
	}

	private static class BestShape {
		public final List<GeoPoint> points;

		public boolean poleMustBeInside;

		public BestShape(final List<GeoPoint> points, final boolean poleMustBeInside) {
			this.points = points;
			this.poleMustBeInside = poleMustBeInside;
		}
	}

	static GeoPolygon generateGeoPolygon(final PlanetModel planetModel, final List<GeoPoint> filteredPointList, final List<GeoPolygon> holes, final GeoPoint testPoint, final boolean testPointInside) throws GeoPolygonFactory.TileException {
		final SidedPlane initialPlane = new SidedPlane(testPoint, filteredPointList.get(0), filteredPointList.get(1));
		GeoCompositePolygon rval = new GeoCompositePolygon(planetModel);
		GeoPolygonFactory.MutableBoolean seenConcave = new GeoPolygonFactory.MutableBoolean();
		if ((GeoPolygonFactory.buildPolygonShape(rval, seenConcave, planetModel, filteredPointList, new BitSet(), 0, 1, initialPlane, holes, testPoint)) == false) {
			if (testPointInside) {
				rval = new GeoCompositePolygon(planetModel);
				seenConcave = new GeoPolygonFactory.MutableBoolean();
				GeoPolygonFactory.buildPolygonShape(rval, seenConcave, planetModel, filteredPointList, new BitSet(), 0, 1, initialPlane, holes, null);
				return rval;
			}
			rval = new GeoCompositePolygon(planetModel);
			seenConcave = new GeoPolygonFactory.MutableBoolean();
			GeoPolygonFactory.buildPolygonShape(rval, seenConcave, planetModel, filteredPointList, new BitSet(), 0, 1, new SidedPlane(initialPlane), holes, null);
			return rval;
		}else {
			if (!testPointInside) {
				return rval;
			}
			rval = new GeoCompositePolygon(planetModel);
			seenConcave = new GeoPolygonFactory.MutableBoolean();
			GeoPolygonFactory.buildPolygonShape(rval, seenConcave, planetModel, filteredPointList, new BitSet(), 0, 1, new SidedPlane(initialPlane), holes, null);
			return rval;
		}
	}

	static List<GeoPoint> filterPoints(final List<? extends GeoPoint> input) {
		final List<GeoPoint> noIdenticalPoints = new ArrayList<>(input.size());
		int startIndex = -1;
		final GeoPoint comparePoint = input.get(0);
		for (int i = 0; i < ((input.size()) - 1); i++) {
			final GeoPoint thePoint = input.get(GeoPolygonFactory.getLegalIndex(((-i) - 1), input.size()));
			if (!(thePoint.isNumericallyIdentical(comparePoint))) {
				startIndex = GeoPolygonFactory.getLegalIndex((-i), input.size());
				break;
			}
		}
		if (startIndex == (-1)) {
			return null;
		}
		int currentIndex = startIndex;
		while (true) {
			final GeoPoint currentPoint = input.get(currentIndex);
			noIdenticalPoints.add(currentPoint);
			while (true) {
				currentIndex = GeoPolygonFactory.getLegalIndex((currentIndex + 1), input.size());
				if (currentIndex == startIndex) {
					break;
				}
				final GeoPoint nextNonIdenticalPoint = input.get(currentIndex);
				if (!(nextNonIdenticalPoint.isNumericallyIdentical(currentPoint))) {
					break;
				}
			} 
			if (currentIndex == startIndex) {
				break;
			}
		} 
		if ((noIdenticalPoints.size()) < 3) {
			return null;
		}
		return noIdenticalPoints;
	}

	static List<GeoPoint> filterEdges(final List<GeoPoint> noIdenticalPoints, final double leniencyValue) {
		for (int i = 0; i < (noIdenticalPoints.size()); i++) {
			final GeoPolygonFactory.SafePath resultPath = GeoPolygonFactory.findSafePath(noIdenticalPoints, i, leniencyValue);
			if ((resultPath != null) && ((resultPath.previous) != null)) {
				final List<GeoPoint> rval = new ArrayList<>(noIdenticalPoints.size());
				resultPath.fillInList(rval);
				return rval;
			}
		}
		return null;
	}

	private static GeoPolygonFactory.SafePath findSafePath(final List<GeoPoint> points, final int startIndex, final double leniencyValue) {
		GeoPolygonFactory.SafePath safePath = null;
		for (int i = startIndex; i < (startIndex + (points.size())); i++) {
			final int startPointIndex = GeoPolygonFactory.getLegalIndex((i - 1), points.size());
			final GeoPoint startPoint = points.get(startPointIndex);
			int endPointIndex = GeoPolygonFactory.getLegalIndex(i, points.size());
			GeoPoint endPoint = points.get(endPointIndex);
			if (startPoint.isNumericallyIdentical(endPoint)) {
				continue;
			}
			while (true) {
				int nextPointIndex = GeoPolygonFactory.getLegalIndex((endPointIndex + 1), points.size());
				final GeoPoint nextPoint = points.get(nextPointIndex);
				if (startPoint.isNumericallyIdentical(nextPoint)) {
					return null;
				}
				if (!(Plane.arePointsCoplanar(startPoint, endPoint, nextPoint))) {
					break;
				}
				if (endPointIndex == startIndex) {
					return null;
				}
				endPointIndex = nextPointIndex;
				endPoint = nextPoint;
				i++;
			} 
			if ((safePath != null) && (endPointIndex == startIndex)) {
				break;
			}
			Plane currentPlane = new Plane(startPoint, endPoint);
			safePath = new GeoPolygonFactory.SafePath(safePath, endPoint, endPointIndex, currentPlane);
		}
		return safePath;
	}

	private static GeoPoint pickPole(final Random generator, final PlanetModel planetModel, final List<GeoPoint> points) {
		final int pointIndex = generator.nextInt(points.size());
		final GeoPoint closePoint = points.get(pointIndex);
		final double angle = (((generator.nextDouble()) * (Math.PI)) * 2.0) - (Math.PI);
		double maxArcDistance = points.get(0).arcDistance(points.get(1));
		double trialArcDistance = points.get(0).arcDistance(points.get(2));
		if (trialArcDistance > maxArcDistance) {
			maxArcDistance = trialArcDistance;
		}
		final double arcDistance = maxArcDistance - ((generator.nextDouble()) * maxArcDistance);
		final double x = Math.cos(arcDistance);
		final double sinArcDistance = Math.sin(arcDistance);
		final double y = (Math.cos(angle)) * sinArcDistance;
		final double z = (Math.sin(angle)) * sinArcDistance;
		final double sinLatitude = Math.sin(closePoint.getLatitude());
		final double cosLatitude = Math.cos(closePoint.getLatitude());
		final double sinLongitude = Math.sin(closePoint.getLongitude());
		final double cosLongitude = Math.cos(closePoint.getLongitude());
		final double x1 = (x * cosLatitude) - (z * sinLatitude);
		final double y1 = y;
		final double z1 = (x * sinLatitude) + (z * cosLatitude);
		final double x2 = (x1 * cosLongitude) - (y1 * sinLongitude);
		final double y2 = (x1 * sinLongitude) + (y1 * cosLongitude);
		final double z2 = z1;
		return planetModel.createSurfacePoint(x2, y2, z2);
	}

	private static Boolean isInsidePolygon(final GeoPoint point, final List<GeoPoint> polyPoints) {
		final double latitude = point.getLatitude();
		final double longitude = point.getLongitude();
		final double sinLatitude = Math.sin(latitude);
		final double cosLatitude = Math.cos(latitude);
		final double sinLongitude = Math.sin(longitude);
		final double cosLongitude = Math.cos(longitude);
		double arcDistance = 0.0;
		Double prevAngle = null;
		for (final GeoPoint polyPoint : polyPoints) {
			final Double angle = GeoPolygonFactory.computeAngle(polyPoint, sinLatitude, cosLatitude, sinLongitude, cosLongitude);
			if (angle == null) {
				return null;
			}
			if (prevAngle != null) {
				double angleDelta = angle - prevAngle;
				if (angleDelta < (-(Math.PI))) {
					angleDelta += (Math.PI) * 2.0;
				}
				if (angleDelta > (Math.PI)) {
					angleDelta -= (Math.PI) * 2.0;
				}
				if ((Math.abs((angleDelta - (Math.PI)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
					return null;
				}
				arcDistance += angleDelta;
			}
			prevAngle = angle;
		}
		if (prevAngle != null) {
			final Double lastAngle = GeoPolygonFactory.computeAngle(polyPoints.get(0), sinLatitude, cosLatitude, sinLongitude, cosLongitude);
			if (lastAngle == null) {
				return null;
			}
			double angleDelta = lastAngle - prevAngle;
			if (angleDelta < (-(Math.PI))) {
				angleDelta += (Math.PI) * 2.0;
			}
			if (angleDelta > (Math.PI)) {
				angleDelta -= (Math.PI) * 2.0;
			}
			if ((Math.abs((angleDelta - (Math.PI)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
				return null;
			}
			arcDistance += angleDelta;
		}
		if ((Math.abs(arcDistance)) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
			return null;
		}
		return arcDistance > 0.0;
	}

	private static Double computeAngle(final GeoPoint point, final double sinLatitude, final double cosLatitude, final double sinLongitude, final double cosLongitude) {
		final double x1 = ((point.x) * cosLongitude) + ((point.y) * sinLongitude);
		final double y1 = ((-(point.x)) * sinLongitude) + ((point.y) * cosLongitude);
		final double z1 = point.z;
		final double y2 = y1;
		final double z2 = ((-x1) * sinLatitude) + (z1 * cosLatitude);
		if ((Math.sqrt(((y2 * y2) + (z2 * z2)))) < (Vector.MINIMUM_RESOLUTION)) {
			return null;
		}
		return Math.atan2(z2, y2);
	}

	static boolean buildPolygonShape(final GeoCompositePolygon rval, final GeoPolygonFactory.MutableBoolean seenConcave, final PlanetModel planetModel, final List<GeoPoint> pointsList, final BitSet internalEdges, final int startPointIndex, final int endPointIndex, final SidedPlane startingEdge, final List<GeoPolygon> holes, final GeoPoint testPoint) throws GeoPolygonFactory.TileException {
		final GeoPolygonFactory.EdgeBuffer edgeBuffer = new GeoPolygonFactory.EdgeBuffer(pointsList, internalEdges, startPointIndex, endPointIndex, startingEdge);
		GeoPolygonFactory.Edge stoppingPoint = edgeBuffer.pickOne();
		GeoPolygonFactory.Edge currentEdge = stoppingPoint;
		while (true) {
			if (currentEdge == null) {
				break;
			}
			final Boolean foundIt = GeoPolygonFactory.findConvexPolygon(planetModel, currentEdge, rval, edgeBuffer, holes, testPoint);
			if (foundIt == null) {
				return false;
			}
			if (foundIt) {
				stoppingPoint = edgeBuffer.pickOne();
				currentEdge = stoppingPoint;
				continue;
			}
			currentEdge = edgeBuffer.getNext(currentEdge);
			if (currentEdge == stoppingPoint) {
				break;
			}
		} 
		boolean foundBadEdge = false;
		final Iterator<GeoPolygonFactory.Edge> checkIterator = edgeBuffer.iterator();
		while (checkIterator.hasNext()) {
			final GeoPolygonFactory.Edge checkEdge = checkIterator.next();
			final SidedPlane flippedPlane = new SidedPlane(checkEdge.plane);
			final Iterator<GeoPolygonFactory.Edge> confirmIterator = edgeBuffer.iterator();
			while (confirmIterator.hasNext()) {
				final GeoPolygonFactory.Edge confirmEdge = confirmIterator.next();
				if (confirmEdge == checkEdge) {
					continue;
				}
				final GeoPoint thePoint;
				if ((((checkEdge.startPoint) != (confirmEdge.startPoint)) && ((checkEdge.endPoint) != (confirmEdge.startPoint))) && (!(flippedPlane.isWithin(confirmEdge.startPoint)))) {
					thePoint = confirmEdge.startPoint;
				}else
					if ((((checkEdge.startPoint) != (confirmEdge.endPoint)) && ((checkEdge.endPoint) != (confirmEdge.endPoint))) && (!(flippedPlane.isWithin(confirmEdge.endPoint)))) {
						thePoint = confirmEdge.endPoint;
					}else {
						thePoint = null;
					}

				if (thePoint != null) {
					foundBadEdge = true;
					if (Plane.arePointsCoplanar(checkEdge.startPoint, checkEdge.endPoint, thePoint)) {
						continue;
					}
					final List<GeoPoint> thirdPartPoints = new ArrayList<>(3);
					final BitSet thirdPartInternal = new BitSet();
					thirdPartPoints.add(checkEdge.startPoint);
					thirdPartInternal.set(0, checkEdge.isInternal);
					thirdPartPoints.add(checkEdge.endPoint);
					thirdPartInternal.set(1, true);
					thirdPartPoints.add(thePoint);
					assert checkEdge.plane.isWithin(thePoint) : "Point was on wrong side of complementary plane, so must be on the right side of the non-complementary plane!";
					GeoPolygonFactory.Edge loopEdge = edgeBuffer.getPrevious(checkEdge);
					final List<GeoPoint> firstPartPoints = new ArrayList<>();
					final BitSet firstPartInternal = new BitSet();
					int i = 0;
					while (true) {
						firstPartPoints.add(loopEdge.endPoint);
						if ((loopEdge.endPoint) == thePoint) {
							break;
						}
						firstPartInternal.set((i++), loopEdge.isInternal);
						loopEdge = edgeBuffer.getPrevious(loopEdge);
					} 
					firstPartInternal.set(i, true);
					if ((GeoPolygonFactory.buildPolygonShape(rval, seenConcave, planetModel, firstPartPoints, firstPartInternal, ((firstPartPoints.size()) - 1), 0, new SidedPlane(checkEdge.endPoint, false, checkEdge.startPoint, thePoint), holes, testPoint)) == false) {
						return false;
					}
					final List<GeoPoint> secondPartPoints = new ArrayList<>();
					final BitSet secondPartInternal = new BitSet();
					loopEdge = edgeBuffer.getNext(checkEdge);
					i = 0;
					while (true) {
						secondPartPoints.add(loopEdge.startPoint);
						if ((loopEdge.startPoint) == thePoint) {
							break;
						}
						secondPartInternal.set((i++), loopEdge.isInternal);
						loopEdge = edgeBuffer.getNext(loopEdge);
					} 
					secondPartInternal.set(i, true);
					if ((GeoPolygonFactory.buildPolygonShape(rval, seenConcave, planetModel, secondPartPoints, secondPartInternal, ((secondPartPoints.size()) - 1), 0, new SidedPlane(checkEdge.startPoint, false, checkEdge.endPoint, thePoint), holes, testPoint)) == false) {
						return false;
					}
					return true;
				}
			} 
		} 
		if (foundBadEdge) {
			throw new GeoPolygonFactory.TileException("Could not tile polygon; found a pathological coplanarity that couldn't be addressed");
		}
		if ((GeoPolygonFactory.makeConcavePolygon(planetModel, rval, seenConcave, edgeBuffer, holes, testPoint)) == false) {
			return false;
		}
		return true;
	}

	private static boolean makeConcavePolygon(final PlanetModel planetModel, final GeoCompositePolygon rval, final GeoPolygonFactory.MutableBoolean seenConcave, final GeoPolygonFactory.EdgeBuffer edgeBuffer, final List<GeoPolygon> holes, final GeoPoint testPoint) throws GeoPolygonFactory.TileException {
		if ((edgeBuffer.size()) == 0) {
			return true;
		}
		if (seenConcave.value) {
			throw new IllegalArgumentException("Illegal polygon; polygon edges intersect each other");
		}
		seenConcave.value = true;
		if ((edgeBuffer.size()) < 3) {
			throw new IllegalArgumentException("Illegal polygon; polygon edges intersect each other");
		}
		final List<GeoPoint> points = new ArrayList<GeoPoint>(edgeBuffer.size());
		final BitSet internalEdges = new BitSet(((edgeBuffer.size()) - 1));
		GeoPolygonFactory.Edge edge = edgeBuffer.pickOne();
		boolean isInternal = false;
		for (int i = 0; i < (edgeBuffer.size()); i++) {
			points.add(edge.startPoint);
			if (i < ((edgeBuffer.size()) - 1)) {
				internalEdges.set(i, edge.isInternal);
			}else {
				isInternal = edge.isInternal;
			}
			edge = edgeBuffer.getNext(edge);
		}
		try {
			if (((testPoint != null) && (holes != null)) && ((holes.size()) > 0)) {
			}
			if ((testPoint != null) && ((holes == null) || ((holes.size()) == 0))) {
			}
			return true;
		} catch (IllegalArgumentException e) {
			throw new GeoPolygonFactory.TileException(e.getMessage());
		}
	}

	private static Boolean findConvexPolygon(final PlanetModel planetModel, final GeoPolygonFactory.Edge currentEdge, final GeoCompositePolygon rval, final GeoPolygonFactory.EdgeBuffer edgeBuffer, final List<GeoPolygon> holes, final GeoPoint testPoint) throws GeoPolygonFactory.TileException {
		final Set<GeoPolygonFactory.Edge> includedEdges = new HashSet<>();
		includedEdges.add(currentEdge);
		GeoPolygonFactory.Edge firstEdge = currentEdge;
		GeoPolygonFactory.Edge lastEdge = currentEdge;
		while (true) {
			if ((firstEdge.startPoint) == (lastEdge.endPoint)) {
				break;
			}
			final GeoPolygonFactory.Edge newLastEdge = edgeBuffer.getNext(lastEdge);
			if (Plane.arePointsCoplanar(lastEdge.startPoint, lastEdge.endPoint, newLastEdge.endPoint)) {
				break;
			}
			if (lastEdge.plane.isFunctionallyIdentical(newLastEdge.plane)) {
				throw new GeoPolygonFactory.TileException("Two adjacent edge planes are effectively parallel despite filtering; give up on tiling");
			}
			if (GeoPolygonFactory.isWithin(newLastEdge.endPoint, includedEdges)) {
				final SidedPlane returnBoundary;
				if ((firstEdge.startPoint) != (newLastEdge.endPoint)) {
					if ((Plane.arePointsCoplanar(firstEdge.endPoint, firstEdge.startPoint, newLastEdge.endPoint)) || (Plane.arePointsCoplanar(firstEdge.startPoint, newLastEdge.endPoint, newLastEdge.startPoint))) {
						break;
					}
					returnBoundary = new SidedPlane(firstEdge.endPoint, firstEdge.startPoint, newLastEdge.endPoint);
				}else {
					returnBoundary = null;
				}
				boolean foundPointInside = false;
				final Iterator<GeoPolygonFactory.Edge> edgeIterator = edgeBuffer.iterator();
				while (edgeIterator.hasNext()) {
					final GeoPolygonFactory.Edge edge = edgeIterator.next();
					if ((!(includedEdges.contains(edge))) && (edge != newLastEdge)) {
						if ((edge.startPoint) != (newLastEdge.endPoint)) {
							if (GeoPolygonFactory.isWithin(edge.startPoint, includedEdges, newLastEdge, returnBoundary)) {
								foundPointInside = true;
								break;
							}
						}
						if ((edge.endPoint) != (firstEdge.startPoint)) {
							if (GeoPolygonFactory.isWithin(edge.endPoint, includedEdges, newLastEdge, returnBoundary)) {
								foundPointInside = true;
								break;
							}
						}
					}
				} 
				if (!foundPointInside) {
					includedEdges.add(newLastEdge);
					lastEdge = newLastEdge;
					continue;
				}
			}
			break;
		} 
		while (true) {
			if ((firstEdge.startPoint) == (lastEdge.endPoint)) {
				break;
			}
			final GeoPolygonFactory.Edge newFirstEdge = edgeBuffer.getPrevious(firstEdge);
			if (Plane.arePointsCoplanar(newFirstEdge.startPoint, newFirstEdge.endPoint, firstEdge.endPoint)) {
				break;
			}
			if (firstEdge.plane.isFunctionallyIdentical(newFirstEdge.plane)) {
				throw new GeoPolygonFactory.TileException("Two adjacent edge planes are effectively parallel despite filtering; give up on tiling");
			}
			if (GeoPolygonFactory.isWithin(newFirstEdge.startPoint, includedEdges)) {
				final SidedPlane returnBoundary;
				if ((newFirstEdge.startPoint) != (lastEdge.endPoint)) {
					if ((Plane.arePointsCoplanar(lastEdge.startPoint, lastEdge.endPoint, newFirstEdge.startPoint)) || (Plane.arePointsCoplanar(lastEdge.endPoint, newFirstEdge.startPoint, newFirstEdge.endPoint))) {
						break;
					}
					returnBoundary = new SidedPlane(lastEdge.startPoint, lastEdge.endPoint, newFirstEdge.startPoint);
				}else {
					returnBoundary = null;
				}
				boolean foundPointInside = false;
				final Iterator<GeoPolygonFactory.Edge> edgeIterator = edgeBuffer.iterator();
				while (edgeIterator.hasNext()) {
					final GeoPolygonFactory.Edge edge = edgeIterator.next();
					if ((!(includedEdges.contains(edge))) && (edge != newFirstEdge)) {
						if ((edge.startPoint) != (lastEdge.endPoint)) {
							if (GeoPolygonFactory.isWithin(edge.startPoint, includedEdges, newFirstEdge, returnBoundary)) {
								foundPointInside = true;
								break;
							}
						}
						if ((edge.endPoint) != (newFirstEdge.startPoint)) {
							if (GeoPolygonFactory.isWithin(edge.endPoint, includedEdges, newFirstEdge, returnBoundary)) {
								foundPointInside = true;
								break;
							}
						}
					}
				} 
				if (!foundPointInside) {
					includedEdges.add(newFirstEdge);
					firstEdge = newFirstEdge;
					continue;
				}
			}
			break;
		} 
		if ((includedEdges.size()) < 2) {
			return false;
		}
		final List<GeoPoint> points = new ArrayList<GeoPoint>(((includedEdges.size()) + 1));
		final BitSet internalEdges = new BitSet(includedEdges.size());
		final boolean returnIsInternal;
		if ((firstEdge.startPoint) == (lastEdge.endPoint)) {
			if ((includedEdges.size()) < 3) {
				return false;
			}
			if (firstEdge.plane.isFunctionallyIdentical(lastEdge.plane)) {
				throw new GeoPolygonFactory.TileException("Two adjacent edge planes are effectively parallel despite filtering; give up on tiling");
			}
			GeoPolygonFactory.Edge edge = firstEdge;
			points.add(edge.startPoint);
			int k = 0;
			while (true) {
				if (edge == lastEdge) {
					break;
				}
				points.add(edge.endPoint);
				internalEdges.set((k++), edge.isInternal);
				edge = edgeBuffer.getNext(edge);
			} 
			returnIsInternal = lastEdge.isInternal;
			edgeBuffer.clear();
		}else {
			final SidedPlane returnSidedPlane = new SidedPlane(firstEdge.endPoint, false, firstEdge.startPoint, lastEdge.endPoint);
			final GeoPolygonFactory.Edge returnEdge = new GeoPolygonFactory.Edge(firstEdge.startPoint, lastEdge.endPoint, returnSidedPlane, true);
			if ((returnEdge.plane.isFunctionallyIdentical(lastEdge.plane)) || (returnEdge.plane.isFunctionallyIdentical(firstEdge.plane))) {
				throw new GeoPolygonFactory.TileException("Two adjacent edge planes are effectively parallel despite filtering; give up on tiling");
			}
			final List<GeoPolygonFactory.Edge> edges = new ArrayList<GeoPolygonFactory.Edge>(includedEdges.size());
			returnIsInternal = true;
			GeoPolygonFactory.Edge edge = firstEdge;
			points.add(edge.startPoint);
			int k = 0;
			while (true) {
				points.add(edge.endPoint);
				internalEdges.set((k++), edge.isInternal);
				edges.add(edge);
				if (edge == lastEdge) {
					break;
				}
				edge = edgeBuffer.getNext(edge);
			} 
			edgeBuffer.replace(edges, returnEdge);
		}
		if (((testPoint != null) && (holes != null)) && ((holes.size()) > 0)) {
		}
		if ((testPoint != null) && ((holes == null) || ((holes.size()) == 0))) {
		}
		return true;
	}

	private static boolean isWithin(final GeoPoint point, final Set<GeoPolygonFactory.Edge> edgeSet, final GeoPolygonFactory.Edge extension, final SidedPlane returnBoundary) {
		if (!(extension.plane.isWithin(point))) {
			return false;
		}
		if ((returnBoundary != null) && (!(returnBoundary.isWithin(point)))) {
			return false;
		}
		return GeoPolygonFactory.isWithin(point, edgeSet);
	}

	private static boolean isWithin(final GeoPoint point, final Set<GeoPolygonFactory.Edge> edgeSet) {
		for (final GeoPolygonFactory.Edge edge : edgeSet) {
			if (!(edge.plane.isWithin(point))) {
				return false;
			}
		}
		return true;
	}

	private static int getLegalIndex(int index, int size) {
		while (index < 0) {
			index += size;
		} 
		while (index >= size) {
			index -= size;
		} 
		return index;
	}

	private static class Edge {
		public final SidedPlane plane;

		public final GeoPoint startPoint;

		public final GeoPoint endPoint;

		public final boolean isInternal;

		public Edge(final GeoPoint startPoint, final GeoPoint endPoint, final SidedPlane plane, final boolean isInternal) {
			this.startPoint = startPoint;
			this.endPoint = endPoint;
			this.plane = plane;
			this.isInternal = isInternal;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(this);
		}

		@Override
		public boolean equals(final Object o) {
			return o == (this);
		}
	}

	private static class EdgeBufferIterator implements Iterator<GeoPolygonFactory.Edge> {
		protected final GeoPolygonFactory.EdgeBuffer edgeBuffer;

		protected final GeoPolygonFactory.Edge firstEdge;

		protected GeoPolygonFactory.Edge currentEdge;

		public EdgeBufferIterator(final GeoPolygonFactory.EdgeBuffer edgeBuffer) {
			this.edgeBuffer = edgeBuffer;
			this.currentEdge = edgeBuffer.pickOne();
			this.firstEdge = currentEdge;
		}

		@Override
		public boolean hasNext() {
			return (currentEdge) != null;
		}

		@Override
		public GeoPolygonFactory.Edge next() {
			final GeoPolygonFactory.Edge rval = currentEdge;
			if ((currentEdge) != null) {
				currentEdge = edgeBuffer.getNext(currentEdge);
				if ((currentEdge) == (firstEdge)) {
					currentEdge = null;
				}
			}
			return rval;
		}

		@Override
		public void remove() {
			throw new RuntimeException("Unsupported operation");
		}
	}

	private static class EdgeBuffer {
		protected GeoPolygonFactory.Edge oneEdge;

		protected final Set<GeoPolygonFactory.Edge> edges = new HashSet<>();

		protected final Map<GeoPolygonFactory.Edge, GeoPolygonFactory.Edge> previousEdges = new HashMap<>();

		protected final Map<GeoPolygonFactory.Edge, GeoPolygonFactory.Edge> nextEdges = new HashMap<>();

		public EdgeBuffer(final List<GeoPoint> pointList, final BitSet internalEdges, final int startPlaneStartIndex, final int startPlaneEndIndex, final SidedPlane startPlane) {
			final GeoPolygonFactory.Edge startEdge = new GeoPolygonFactory.Edge(pointList.get(startPlaneStartIndex), pointList.get(startPlaneEndIndex), startPlane, internalEdges.get(startPlaneStartIndex));
			GeoPolygonFactory.Edge currentEdge = startEdge;
			int startIndex = startPlaneStartIndex;
			int endIndex = startPlaneEndIndex;
			while (true) {
				if ((currentEdge.endPoint) == (startEdge.startPoint)) {
					previousEdges.put(startEdge, currentEdge);
					nextEdges.put(currentEdge, startEdge);
					edges.add(startEdge);
					break;
				}
				startIndex = endIndex;
				endIndex++;
				if (endIndex >= (pointList.size())) {
					endIndex -= pointList.size();
				}
				final GeoPoint newPoint = pointList.get(endIndex);
				final boolean isNewPointWithin = currentEdge.plane.isWithin(newPoint);
				final GeoPoint pointToPresent = currentEdge.startPoint;
				final SidedPlane newPlane = new SidedPlane(pointToPresent, isNewPointWithin, pointList.get(startIndex), newPoint);
				final GeoPolygonFactory.Edge newEdge = new GeoPolygonFactory.Edge(pointList.get(startIndex), pointList.get(endIndex), newPlane, internalEdges.get(startIndex));
				previousEdges.put(newEdge, currentEdge);
				nextEdges.put(currentEdge, newEdge);
				edges.add(newEdge);
				currentEdge = newEdge;
			} 
			oneEdge = startEdge;
		}

		public GeoPolygonFactory.Edge getPrevious(final GeoPolygonFactory.Edge currentEdge) {
			return previousEdges.get(currentEdge);
		}

		public GeoPolygonFactory.Edge getNext(final GeoPolygonFactory.Edge currentEdge) {
			return nextEdges.get(currentEdge);
		}

		public void replace(final List<GeoPolygonFactory.Edge> removeList, final GeoPolygonFactory.Edge newEdge) {
			final GeoPolygonFactory.Edge previous = previousEdges.get(removeList.get(0));
			final GeoPolygonFactory.Edge next = nextEdges.get(removeList.get(((removeList.size()) - 1)));
			edges.add(newEdge);
			previousEdges.put(newEdge, previous);
			nextEdges.put(previous, newEdge);
			previousEdges.put(next, newEdge);
			nextEdges.put(newEdge, next);
			for (final GeoPolygonFactory.Edge edge : removeList) {
				if (edge == (oneEdge)) {
					oneEdge = newEdge;
				}
				edges.remove(edge);
				previousEdges.remove(edge);
				nextEdges.remove(edge);
			}
		}

		public void clear() {
			edges.clear();
			previousEdges.clear();
			nextEdges.clear();
			oneEdge = null;
		}

		public int size() {
			return edges.size();
		}

		public Iterator<GeoPolygonFactory.Edge> iterator() {
			return new GeoPolygonFactory.EdgeBufferIterator(this);
		}

		public GeoPolygonFactory.Edge pickOne() {
			return oneEdge;
		}
	}

	private static class SafePath {
		public final GeoPoint lastPoint;

		public final int lastPointIndex;

		public final Plane lastPlane;

		public final GeoPolygonFactory.SafePath previous;

		public SafePath(final GeoPolygonFactory.SafePath previous, final GeoPoint lastPoint, final int lastPointIndex, final Plane lastPlane) {
			this.lastPoint = lastPoint;
			this.lastPointIndex = lastPointIndex;
			this.lastPlane = lastPlane;
			this.previous = previous;
		}

		public void fillInList(final List<GeoPoint> pointList) {
			GeoPolygonFactory.SafePath safePath = this;
			while ((safePath.previous) != null) {
				pointList.add(safePath.lastPoint);
				safePath = safePath.previous;
			} 
			pointList.add(safePath.lastPoint);
			Collections.reverse(pointList);
		}
	}

	static class MutableBoolean {
		public boolean value = false;
	}

	private static class TileException extends Exception {
		public TileException(final String msg) {
			super(msg);
		}
	}
}

