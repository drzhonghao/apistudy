

import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.spatial3d.geom.GeoPath;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.Vector;


public class GeoPathFactory {
	private GeoPathFactory() {
	}

	public static GeoPath makeGeoPath(final PlanetModel planetModel, final double maxCutoffAngle, final GeoPoint[] pathPoints) {
		if (maxCutoffAngle < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
		}
		return null;
	}

	private static GeoPoint[] filterPoints(final GeoPoint[] pathPoints) {
		final List<GeoPoint> noIdenticalPoints = new ArrayList<>(pathPoints.length);
		for (int i = 0; i < ((pathPoints.length) - 1); i++) {
			if (!(pathPoints[i].isNumericallyIdentical(pathPoints[(i + 1)]))) {
				noIdenticalPoints.add(pathPoints[i]);
			}
		}
		noIdenticalPoints.add(pathPoints[((pathPoints.length) - 1)]);
		return noIdenticalPoints.toArray(new GeoPoint[noIdenticalPoints.size()]);
	}
}

