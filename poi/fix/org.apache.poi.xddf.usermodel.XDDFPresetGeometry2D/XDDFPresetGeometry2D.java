

import java.util.Collections;
import java.util.List;
import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.PresetGeometry;
import org.apache.poi.xddf.usermodel.XDDFGeometryGuide;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomGuideList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetGeometry2D;


@Beta
public class XDDFPresetGeometry2D {
	private CTPresetGeometry2D geometry;

	protected XDDFPresetGeometry2D(CTPresetGeometry2D geometry) {
		this.geometry = geometry;
	}

	@org.apache.poi.util.Internal
	protected CTPresetGeometry2D getXmlObject() {
		return geometry;
	}

	public PresetGeometry getGeometry() {
		return null;
	}

	public void setGeometry(PresetGeometry preset) {
	}

	public XDDFGeometryGuide addAdjustValue() {
		if (!(geometry.isSetAvLst())) {
			geometry.addNewAvLst();
		}
		return null;
	}

	public XDDFGeometryGuide insertAdjustValue(int index) {
		if (!(geometry.isSetAvLst())) {
			geometry.addNewAvLst();
		}
		return null;
	}

	public void removeAdjustValue(int index) {
		if (geometry.isSetAvLst()) {
			geometry.getAvLst().removeGd(index);
		}
	}

	public XDDFGeometryGuide getAdjustValue(int index) {
		if (geometry.isSetAvLst()) {
		}else {
			return null;
		}
		return null;
	}

	public List<XDDFGeometryGuide> getAdjustValues() {
		if (geometry.isSetAvLst()) {
		}else {
			return Collections.emptyList();
		}
		return null;
	}
}

