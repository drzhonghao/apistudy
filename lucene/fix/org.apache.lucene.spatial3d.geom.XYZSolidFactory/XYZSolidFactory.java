

import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.Vector;
import org.apache.lucene.spatial3d.geom.XYZBounds;
import org.apache.lucene.spatial3d.geom.XYZSolid;


public class XYZSolidFactory {
	private XYZSolidFactory() {
	}

	public static XYZSolid makeXYZSolid(final PlanetModel planetModel, final double minX, final double maxX, final double minY, final double maxY, final double minZ, final double maxZ) {
		if ((Math.abs((maxX - minX))) < (Vector.MINIMUM_RESOLUTION)) {
			if ((Math.abs((maxY - minY))) < (Vector.MINIMUM_RESOLUTION)) {
				if ((Math.abs((maxZ - minZ))) < (Vector.MINIMUM_RESOLUTION)) {
				}else {
				}
			}else {
				if ((Math.abs((maxZ - minZ))) < (Vector.MINIMUM_RESOLUTION)) {
				}else {
				}
			}
		}
		if ((Math.abs((maxY - minY))) < (Vector.MINIMUM_RESOLUTION)) {
			if ((Math.abs((maxZ - minZ))) < (Vector.MINIMUM_RESOLUTION)) {
			}else {
			}
		}
		if ((Math.abs((maxZ - minZ))) < (Vector.MINIMUM_RESOLUTION)) {
		}
		return null;
	}

	public static XYZSolid makeXYZSolid(final PlanetModel planetModel, final XYZBounds bounds) {
		return XYZSolidFactory.makeXYZSolid(planetModel, bounds.getMinimumX(), bounds.getMaximumX(), bounds.getMinimumY(), bounds.getMaximumY(), bounds.getMinimumZ(), bounds.getMaximumZ());
	}
}

