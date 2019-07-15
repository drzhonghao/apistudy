

import org.apache.lucene.spatial3d.geom.GeoBBox;
import org.apache.lucene.spatial3d.geom.GeoDegenerateVerticalLine;
import org.apache.lucene.spatial3d.geom.LatLonBounds;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.Vector;


public class GeoBBoxFactory {
	private GeoBBoxFactory() {
	}

	public static GeoBBox makeGeoBBox(final PlanetModel planetModel, double topLat, double bottomLat, double leftLon, double rightLon) {
		if (topLat > ((Math.PI) * 0.5))
			topLat = (Math.PI) * 0.5;

		if (bottomLat < ((-(Math.PI)) * 0.5))
			bottomLat = (-(Math.PI)) * 0.5;

		if (leftLon < (-(Math.PI)))
			leftLon = -(Math.PI);

		if (rightLon > (Math.PI))
			rightLon = Math.PI;

		if ((((Math.abs((leftLon + (Math.PI)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) && ((Math.abs((rightLon - (Math.PI)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION))) || (((Math.abs((rightLon + (Math.PI)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) && ((Math.abs((leftLon - (Math.PI)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)))) {
			if (((Math.abs((topLat - ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) && ((Math.abs((bottomLat + ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION))) {
			}
			if ((Math.abs((topLat - bottomLat))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
				if (((Math.abs((topLat - ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) || ((Math.abs((topLat + ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION))) {
				}
			}
			if ((Math.abs((topLat - ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
			}else
				if ((Math.abs((bottomLat + ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
				}

		}
		double extent = rightLon - leftLon;
		if (extent < 0.0)
			extent += (Math.PI) * 2.0;

		if ((topLat == ((Math.PI) * 0.5)) && (bottomLat == ((-(Math.PI)) * 0.5))) {
			if ((Math.abs((leftLon - rightLon))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
			}
			if (extent >= (Math.PI)) {
			}
		}
		if ((Math.abs((leftLon - rightLon))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
			if ((Math.abs((topLat - bottomLat))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
			}
			return new GeoDegenerateVerticalLine(planetModel, topLat, bottomLat, leftLon);
		}
		if (extent >= (Math.PI)) {
			if ((Math.abs((topLat - bottomLat))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
				if ((Math.abs((topLat - ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
				}else
					if ((Math.abs((bottomLat + ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
					}

			}
			if ((Math.abs((topLat - ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
			}else
				if ((Math.abs((bottomLat + ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
				}

		}
		if ((Math.abs((topLat - bottomLat))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
			if ((Math.abs((topLat - ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
			}else
				if ((Math.abs((bottomLat + ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
				}

		}
		if ((Math.abs((topLat - ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
		}else
			if ((Math.abs((bottomLat + ((Math.PI) * 0.5)))) < (Vector.MINIMUM_ANGULAR_RESOLUTION)) {
			}

		return null;
	}

	public static GeoBBox makeGeoBBox(final PlanetModel planetModel, LatLonBounds bounds) {
		final double topLat = (bounds.checkNoTopLatitudeBound()) ? (Math.PI) * 0.5 : bounds.getMaxLatitude();
		final double bottomLat = (bounds.checkNoBottomLatitudeBound()) ? (-(Math.PI)) * 0.5 : bounds.getMinLatitude();
		final double leftLon = (bounds.checkNoLongitudeBound()) ? -(Math.PI) : bounds.getLeftLongitude();
		final double rightLon = (bounds.checkNoLongitudeBound()) ? Math.PI : bounds.getRightLongitude();
		return GeoBBoxFactory.makeGeoBBox(planetModel, topLat, bottomLat, leftLon, rightLon);
	}
}

