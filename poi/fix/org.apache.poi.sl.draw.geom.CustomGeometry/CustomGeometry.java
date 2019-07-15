

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.sl.draw.binding.CTCustomGeometry2D;
import org.apache.poi.sl.draw.binding.CTGeomGuide;
import org.apache.poi.sl.draw.binding.CTGeomGuideList;
import org.apache.poi.sl.draw.binding.CTGeomRect;
import org.apache.poi.sl.draw.binding.CTPath2D;
import org.apache.poi.sl.draw.binding.CTPath2DList;
import org.apache.poi.sl.draw.geom.AdjustValue;
import org.apache.poi.sl.draw.geom.Guide;
import org.apache.poi.sl.draw.geom.Path;


public class CustomGeometry implements Iterable<Path> {
	final List<Guide> adjusts = new ArrayList<>();

	final List<Guide> guides = new ArrayList<>();

	final List<Path> paths = new ArrayList<>();

	Path textBounds;

	public CustomGeometry(CTCustomGeometry2D geom) {
		CTGeomGuideList avLst = geom.getAvLst();
		if (avLst != null) {
			for (CTGeomGuide gd : avLst.getGd()) {
				adjusts.add(new AdjustValue(gd));
			}
		}
		CTGeomGuideList gdLst = geom.getGdLst();
		if (gdLst != null) {
			for (CTGeomGuide gd : gdLst.getGd()) {
				guides.add(new Guide(gd));
			}
		}
		CTPath2DList pathLst = geom.getPathLst();
		if (pathLst != null) {
			for (CTPath2D spPath : pathLst.getPath()) {
				paths.add(new Path(spPath));
			}
		}
		CTGeomRect rect = geom.getRect();
		if (rect != null) {
			textBounds = new Path();
		}
	}

	@Override
	public Iterator<Path> iterator() {
		return paths.iterator();
	}

	public Path getTextBounds() {
		return textBounds;
	}
}

