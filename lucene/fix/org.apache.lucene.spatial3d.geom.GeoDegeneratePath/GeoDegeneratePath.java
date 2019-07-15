

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


class GeoDegeneratePath {
	protected final List<GeoPoint> points = new ArrayList<GeoPoint>();

	protected List<GeoDegeneratePath.SegmentEndpoint> endPoints;

	protected List<GeoDegeneratePath.PathSegment> segments;

	protected GeoPoint[] edgePoints;

	protected boolean isDone = false;

	public GeoDegeneratePath(final PlanetModel planetModel, final GeoPoint[] pathPoints) {
		this(planetModel);
		Collections.addAll(points, pathPoints);
		done();
	}

	public GeoDegeneratePath(final PlanetModel planetModel) {
	}

	public void addPoint(final double lat, final double lon) {
		if (isDone)
			throw new IllegalStateException("Can't call addPoint() if done() already called");

	}

	public void done() {
		if (isDone)
			throw new IllegalStateException("Can't call done() twice");

		if ((points.size()) == 0)
			throw new IllegalArgumentException("Path must have at least one point");

		isDone = true;
		endPoints = new ArrayList<>(points.size());
		segments = new ArrayList<>(points.size());
		GeoPoint lastPoint = null;
		for (final GeoPoint end : points) {
			if (lastPoint != null) {
				final Plane normalizedConnectingPlane = new Plane(lastPoint, end);
				if (normalizedConnectingPlane == null) {
					continue;
				}
			}
			lastPoint = end;
		}
		if ((segments.size()) == 0) {
			final GeoPoint point = points.get(0);
			final GeoDegeneratePath.SegmentEndpoint onlyEndpoint = new GeoDegeneratePath.SegmentEndpoint(point);
			endPoints.add(onlyEndpoint);
			this.edgePoints = new GeoPoint[]{ point };
			return;
		}
		for (int i = 0; i < (segments.size()); i++) {
			final GeoDegeneratePath.PathSegment currentSegment = segments.get(i);
			if (i == 0) {
				final GeoDegeneratePath.SegmentEndpoint startEndpoint = new GeoDegeneratePath.SegmentEndpoint(currentSegment.start, currentSegment.startCutoffPlane);
				endPoints.add(startEndpoint);
				this.edgePoints = new GeoPoint[]{ currentSegment.start };
				continue;
			}
			endPoints.add(new GeoDegeneratePath.SegmentEndpoint(currentSegment.start, segments.get((i - 1)).endCutoffPlane, currentSegment.startCutoffPlane));
		}
		final GeoDegeneratePath.PathSegment lastSegment = segments.get(((segments.size()) - 1));
		endPoints.add(new GeoDegeneratePath.SegmentEndpoint(lastSegment.end, lastSegment.endCutoffPlane));
	}

	public GeoDegeneratePath(final PlanetModel planetModel, final InputStream inputStream) throws IOException {
		this(planetModel, SerializableObject.readPointArray(planetModel, inputStream));
	}

	public void write(final OutputStream outputStream) throws IOException {
		SerializableObject.writePointArray(outputStream, points);
	}

	public double computePathCenterDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		double closestDistance = Double.POSITIVE_INFINITY;
		for (GeoDegeneratePath.PathSegment segment : segments) {
		}
		for (GeoDegeneratePath.SegmentEndpoint endpoint : endPoints) {
			final double endpointDistance = endpoint.pathCenterDistance(distanceStyle, x, y, z);
			if (endpointDistance < closestDistance) {
				closestDistance = endpointDistance;
			}
		}
		return closestDistance;
	}

	public double computeNearestDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		double currentDistance = 0.0;
		double minPathCenterDistance = Double.POSITIVE_INFINITY;
		double bestDistance = Double.POSITIVE_INFINITY;
		int segmentIndex = 0;
		for (GeoDegeneratePath.SegmentEndpoint endpoint : endPoints) {
			final double endpointPathCenterDistance = endpoint.pathCenterDistance(distanceStyle, x, y, z);
			if (endpointPathCenterDistance < minPathCenterDistance) {
				minPathCenterDistance = endpointPathCenterDistance;
				bestDistance = currentDistance;
			}
			if (segmentIndex < (segments.size())) {
				final GeoDegeneratePath.PathSegment segment = segments.get((segmentIndex++));
				currentDistance = distanceStyle.aggregateDistances(currentDistance, segment.fullPathDistance(distanceStyle));
			}
		}
		return bestDistance;
	}

	protected double distance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		double currentDistance = 0.0;
		for (GeoDegeneratePath.PathSegment segment : segments) {
			currentDistance = distanceStyle.aggregateDistances(currentDistance, segment.fullPathDistance(distanceStyle));
		}
		int segmentIndex = 0;
		currentDistance = 0.0;
		for (GeoDegeneratePath.SegmentEndpoint endpoint : endPoints) {
			double distance = endpoint.pathDistance(distanceStyle, x, y, z);
			if (distance != (Double.POSITIVE_INFINITY))
				return distanceStyle.fromAggregationForm(distanceStyle.aggregateDistances(currentDistance, distance));

			if (segmentIndex < (segments.size()))
				currentDistance = distanceStyle.aggregateDistances(currentDistance, segments.get((segmentIndex++)).fullPathDistance(distanceStyle));

		}
		return Double.POSITIVE_INFINITY;
	}

	protected double deltaDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		return 0.0;
	}

	protected void distanceBounds(final Bounds bounds, final DistanceStyle distanceStyle, final double distanceValue) {
		getBounds(bounds);
	}

	protected double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
		double minDistance = Double.POSITIVE_INFINITY;
		for (final GeoDegeneratePath.SegmentEndpoint endpoint : endPoints) {
			final double newDistance = endpoint.outsideDistance(distanceStyle, x, y, z);
			if (newDistance < minDistance)
				minDistance = newDistance;

		}
		for (final GeoDegeneratePath.PathSegment segment : segments) {
		}
		return minDistance;
	}

	public boolean isWithin(final double x, final double y, final double z) {
		for (GeoDegeneratePath.SegmentEndpoint pathPoint : endPoints) {
			if (pathPoint.isWithin(x, y, z)) {
				return true;
			}
		}
		for (GeoDegeneratePath.PathSegment pathSegment : segments) {
			if (pathSegment.isWithin(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	public GeoPoint[] getEdgePoints() {
		return edgePoints;
	}

	public boolean intersects(final Plane plane, final GeoPoint[] notablePoints, final Membership... bounds) {
		if ((endPoints.size()) == 1) {
		}
		for (final GeoDegeneratePath.PathSegment pathSegment : segments) {
		}
		return false;
	}

	public boolean intersects(GeoShape geoShape) {
		if ((endPoints.size()) == 1) {
			return endPoints.get(0).intersects(geoShape);
		}
		for (final GeoDegeneratePath.PathSegment pathSegment : segments) {
			if (pathSegment.intersects(geoShape)) {
				return true;
			}
		}
		return false;
	}

	public void getBounds(Bounds bounds) {
		for (GeoDegeneratePath.PathSegment pathSegment : segments) {
		}
		if ((endPoints.size()) == 1) {
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GeoDegeneratePath))
			return false;

		GeoDegeneratePath p = ((GeoDegeneratePath) (o));
		if (!(super.equals(p)))
			return false;

		return points.equals(p.points);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = (31 * result) + (points.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return null;
	}

	private static class SegmentEndpoint {
		public final GeoPoint point;

		public final Membership[] cutoffPlanes;

		public final GeoPoint[] notablePoints;

		public static final GeoPoint[] circlePoints = new GeoPoint[0];

		public static final Membership[] NO_MEMBERSHIP = new Membership[0];

		public SegmentEndpoint(final GeoPoint point) {
			this.point = point;
			this.cutoffPlanes = GeoDegeneratePath.SegmentEndpoint.NO_MEMBERSHIP;
			this.notablePoints = GeoDegeneratePath.SegmentEndpoint.circlePoints;
		}

		public SegmentEndpoint(final GeoPoint point, final SidedPlane cutoffPlane) {
			this.point = point;
			this.cutoffPlanes = new Membership[]{ new SidedPlane(cutoffPlane) };
			this.notablePoints = new GeoPoint[]{ point };
		}

		public SegmentEndpoint(final GeoPoint point, final SidedPlane cutoffPlane1, final SidedPlane cutoffPlane2) {
			this.point = point;
			this.cutoffPlanes = new Membership[]{ new SidedPlane(cutoffPlane1), new SidedPlane(cutoffPlane2) };
			this.notablePoints = new GeoPoint[]{ point };
		}

		public boolean isWithin(final Vector point) {
			return this.point.isIdentical(point.x, point.y, point.z);
		}

		public boolean isWithin(final double x, final double y, final double z) {
			return this.point.isIdentical(x, y, z);
		}

		public double pathDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
			if (!(isWithin(x, y, z)))
				return Double.POSITIVE_INFINITY;

			return distanceStyle.toAggregationForm(distanceStyle.computeDistance(this.point, x, y, z));
		}

		public double nearestPathDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
			for (final Membership m : cutoffPlanes) {
				if (!(m.isWithin(x, y, z))) {
					return Double.POSITIVE_INFINITY;
				}
			}
			return distanceStyle.toAggregationForm(0.0);
		}

		public double pathCenterDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
			for (final Membership m : cutoffPlanes) {
				if (!(m.isWithin(x, y, z))) {
					return Double.POSITIVE_INFINITY;
				}
			}
			return distanceStyle.computeDistance(this.point, x, y, z);
		}

		public double outsideDistance(final DistanceStyle distanceStyle, final double x, final double y, final double z) {
			return distanceStyle.computeDistance(this.point, x, y, z);
		}

		public boolean intersects(final PlanetModel planetModel, final Plane p, final GeoPoint[] notablePoints, final Membership[] bounds) {
			if (!(p.evaluateIsZero(point)))
				return false;

			for (Membership m : bounds) {
				if (!(m.isWithin(point)))
					return false;

			}
			return true;
		}

		public boolean intersects(final GeoShape geoShape) {
			return geoShape.isWithin(point);
		}

		public void getBounds(final PlanetModel planetModel, Bounds bounds) {
			bounds.addPoint(point);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof GeoDegeneratePath.SegmentEndpoint))
				return false;

			GeoDegeneratePath.SegmentEndpoint other = ((GeoDegeneratePath.SegmentEndpoint) (o));
			return point.equals(other.point);
		}

		@Override
		public int hashCode() {
			return point.hashCode();
		}

		@Override
		public String toString() {
			return point.toString();
		}
	}

	private static class PathSegment {
		public final GeoPoint start;

		public final GeoPoint end;

		public final Map<DistanceStyle, Double> fullDistanceCache = new HashMap<DistanceStyle, Double>();

		public final Plane normalizedConnectingPlane;

		public final SidedPlane startCutoffPlane;

		public final SidedPlane endCutoffPlane;

		public final GeoPoint[] connectingPlanePoints;

		public PathSegment(final PlanetModel planetModel, final GeoPoint start, final GeoPoint end, final Plane normalizedConnectingPlane) {
			this.start = start;
			this.end = end;
			this.normalizedConnectingPlane = normalizedConnectingPlane;
			startCutoffPlane = new SidedPlane(end, normalizedConnectingPlane, start);
			endCutoffPlane = new SidedPlane(start, normalizedConnectingPlane, end);
			connectingPlanePoints = new GeoPoint[]{ start, end };
		}

		public double fullPathDistance(final DistanceStyle distanceStyle) {
			synchronized(fullDistanceCache) {
				Double dist = fullDistanceCache.get(distanceStyle);
				if (dist == null) {
					dist = new Double(distanceStyle.toAggregationForm(distanceStyle.computeDistance(start, end.x, end.y, end.z)));
					fullDistanceCache.put(distanceStyle, dist);
				}
				return dist.doubleValue();
			}
		}

		public boolean isWithin(final Vector point) {
			return ((startCutoffPlane.isWithin(point)) && (endCutoffPlane.isWithin(point))) && (normalizedConnectingPlane.evaluateIsZero(point));
		}

		public boolean isWithin(final double x, final double y, final double z) {
			return ((startCutoffPlane.isWithin(x, y, z)) && (endCutoffPlane.isWithin(x, y, z))) && (normalizedConnectingPlane.evaluateIsZero(x, y, z));
		}

		public double pathCenterDistance(final PlanetModel planetModel, final DistanceStyle distanceStyle, final double x, final double y, final double z) {
			if ((!(startCutoffPlane.isWithin(x, y, z))) || (!(endCutoffPlane.isWithin(x, y, z)))) {
				return Double.POSITIVE_INFINITY;
			}
			final double perpX = ((normalizedConnectingPlane.y) * z) - ((normalizedConnectingPlane.z) * y);
			final double perpY = ((normalizedConnectingPlane.z) * x) - ((normalizedConnectingPlane.x) * z);
			final double perpZ = ((normalizedConnectingPlane.x) * y) - ((normalizedConnectingPlane.y) * x);
			final double magnitude = Math.sqrt((((perpX * perpX) + (perpY * perpY)) + (perpZ * perpZ)));
			if ((Math.abs(magnitude)) < (Vector.MINIMUM_RESOLUTION))
				return distanceStyle.computeDistance(start, x, y, z);

			final double normFactor = 1.0 / magnitude;
			final Plane normalizedPerpPlane = new Plane((perpX * normFactor), (perpY * normFactor), (perpZ * normFactor), 0.0);
			final GeoPoint[] intersectionPoints = normalizedConnectingPlane.findIntersections(planetModel, normalizedPerpPlane);
			GeoPoint thePoint;
			if ((intersectionPoints.length) == 0)
				throw new RuntimeException(((((("Can't find world intersection for point x=" + x) + " y=") + y) + " z=") + z));
			else
				if ((intersectionPoints.length) == 1)
					thePoint = intersectionPoints[0];
				else {
					if ((startCutoffPlane.isWithin(intersectionPoints[0])) && (endCutoffPlane.isWithin(intersectionPoints[0])))
						thePoint = intersectionPoints[0];
					else
						if ((startCutoffPlane.isWithin(intersectionPoints[1])) && (endCutoffPlane.isWithin(intersectionPoints[1])))
							thePoint = intersectionPoints[1];
						else
							throw new RuntimeException(((((("Can't find world intersection for point x=" + x) + " y=") + y) + " z=") + z));


				}

			return distanceStyle.computeDistance(thePoint, x, y, z);
		}

		public double nearestPathDistance(final PlanetModel planetModel, final DistanceStyle distanceStyle, final double x, final double y, final double z) {
			if ((!(startCutoffPlane.isWithin(x, y, z))) || (!(endCutoffPlane.isWithin(x, y, z)))) {
				return Double.POSITIVE_INFINITY;
			}
			final double perpX = ((normalizedConnectingPlane.y) * z) - ((normalizedConnectingPlane.z) * y);
			final double perpY = ((normalizedConnectingPlane.z) * x) - ((normalizedConnectingPlane.x) * z);
			final double perpZ = ((normalizedConnectingPlane.x) * y) - ((normalizedConnectingPlane.y) * x);
			final double magnitude = Math.sqrt((((perpX * perpX) + (perpY * perpY)) + (perpZ * perpZ)));
			if ((Math.abs(magnitude)) < (Vector.MINIMUM_RESOLUTION))
				return distanceStyle.toAggregationForm(0.0);

			final double normFactor = 1.0 / magnitude;
			final Plane normalizedPerpPlane = new Plane((perpX * normFactor), (perpY * normFactor), (perpZ * normFactor), 0.0);
			final GeoPoint[] intersectionPoints = normalizedConnectingPlane.findIntersections(planetModel, normalizedPerpPlane);
			GeoPoint thePoint;
			if ((intersectionPoints.length) == 0)
				throw new RuntimeException(((((("Can't find world intersection for point x=" + x) + " y=") + y) + " z=") + z));
			else
				if ((intersectionPoints.length) == 1)
					thePoint = intersectionPoints[0];
				else {
					if ((startCutoffPlane.isWithin(intersectionPoints[0])) && (endCutoffPlane.isWithin(intersectionPoints[0])))
						thePoint = intersectionPoints[0];
					else
						if ((startCutoffPlane.isWithin(intersectionPoints[1])) && (endCutoffPlane.isWithin(intersectionPoints[1])))
							thePoint = intersectionPoints[1];
						else
							throw new RuntimeException(((((("Can't find world intersection for point x=" + x) + " y=") + y) + " z=") + z));


				}

			return distanceStyle.toAggregationForm(distanceStyle.computeDistance(start, thePoint.x, thePoint.y, thePoint.z));
		}

		public double pathDistance(final PlanetModel planetModel, final DistanceStyle distanceStyle, final double x, final double y, final double z) {
			if (!(isWithin(x, y, z)))
				return Double.POSITIVE_INFINITY;

			final double perpX = ((normalizedConnectingPlane.y) * z) - ((normalizedConnectingPlane.z) * y);
			final double perpY = ((normalizedConnectingPlane.z) * x) - ((normalizedConnectingPlane.x) * z);
			final double perpZ = ((normalizedConnectingPlane.x) * y) - ((normalizedConnectingPlane.y) * x);
			final double magnitude = Math.sqrt((((perpX * perpX) + (perpY * perpY)) + (perpZ * perpZ)));
			if ((Math.abs(magnitude)) < (Vector.MINIMUM_RESOLUTION))
				return distanceStyle.toAggregationForm(distanceStyle.computeDistance(start, x, y, z));

			final double normFactor = 1.0 / magnitude;
			final Plane normalizedPerpPlane = new Plane((perpX * normFactor), (perpY * normFactor), (perpZ * normFactor), 0.0);
			final GeoPoint[] intersectionPoints = normalizedConnectingPlane.findIntersections(planetModel, normalizedPerpPlane);
			GeoPoint thePoint;
			if ((intersectionPoints.length) == 0)
				throw new RuntimeException(((((("Can't find world intersection for point x=" + x) + " y=") + y) + " z=") + z));
			else
				if ((intersectionPoints.length) == 1)
					thePoint = intersectionPoints[0];
				else {
					if ((startCutoffPlane.isWithin(intersectionPoints[0])) && (endCutoffPlane.isWithin(intersectionPoints[0])))
						thePoint = intersectionPoints[0];
					else
						if ((startCutoffPlane.isWithin(intersectionPoints[1])) && (endCutoffPlane.isWithin(intersectionPoints[1])))
							thePoint = intersectionPoints[1];
						else
							throw new RuntimeException(((((("Can't find world intersection for point x=" + x) + " y=") + y) + " z=") + z));


				}

			return distanceStyle.aggregateDistances(distanceStyle.toAggregationForm(distanceStyle.computeDistance(thePoint, x, y, z)), distanceStyle.toAggregationForm(distanceStyle.computeDistance(start, thePoint.x, thePoint.y, thePoint.z)));
		}

		public double outsideDistance(final PlanetModel planetModel, final DistanceStyle distanceStyle, final double x, final double y, final double z) {
			final double distance = distanceStyle.computeDistance(planetModel, normalizedConnectingPlane, x, y, z, startCutoffPlane, endCutoffPlane);
			final double startDistance = distanceStyle.computeDistance(start, x, y, z);
			final double endDistance = distanceStyle.computeDistance(end, x, y, z);
			return Math.min(Math.min(startDistance, endDistance), distance);
		}

		public boolean intersects(final PlanetModel planetModel, final Plane p, final GeoPoint[] notablePoints, final Membership[] bounds) {
			return normalizedConnectingPlane.intersects(planetModel, p, connectingPlanePoints, notablePoints, bounds, startCutoffPlane, endCutoffPlane);
		}

		public boolean intersects(final GeoShape geoShape) {
			return geoShape.intersects(normalizedConnectingPlane, connectingPlanePoints, startCutoffPlane, endCutoffPlane);
		}

		public void getBounds(final PlanetModel planetModel, Bounds bounds) {
			bounds.addPoint(start).addPoint(end).addPlane(planetModel, normalizedConnectingPlane, startCutoffPlane, endCutoffPlane);
		}
	}
}

