

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.chart.LayoutMode;
import org.apache.poi.xddf.usermodel.chart.LayoutTarget;
import org.apache.poi.xddf.usermodel.chart.XDDFChartExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDouble;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLayout;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLayoutMode;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLayoutTarget;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTManualLayout;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;


@Beta
public final class XDDFManualLayout {
	private CTManualLayout layout;

	private static final LayoutMode defaultLayoutMode = LayoutMode.EDGE;

	private static final LayoutTarget defaultLayoutTarget = LayoutTarget.INNER;

	public XDDFManualLayout(CTLayout ctLayout) {
		initializeLayout(ctLayout);
	}

	public XDDFManualLayout(CTPlotArea ctPlotArea) {
		CTLayout ctLayout = (ctPlotArea.isSetLayout()) ? ctPlotArea.getLayout() : ctPlotArea.addNewLayout();
		initializeLayout(ctLayout);
	}

	@org.apache.poi.util.Internal
	protected CTManualLayout getXmlObject() {
		return layout;
	}

	public void setExtensionList(XDDFChartExtensionList list) {
		if (list == null) {
			if (layout.isSetExtLst()) {
				layout.unsetExtLst();
			}
		}else {
			layout.setExtLst(list.getXmlObject());
		}
	}

	public XDDFChartExtensionList getExtensionList() {
		if (layout.isSetExtLst()) {
		}else {
			return null;
		}
		return null;
	}

	public void setWidthRatio(double ratio) {
		if (!(layout.isSetW())) {
			layout.addNewW();
		}
		layout.getW().setVal(ratio);
	}

	public double getWidthRatio() {
		if (!(layout.isSetW())) {
			return 0.0;
		}
		return layout.getW().getVal();
	}

	public void setHeightRatio(double ratio) {
		if (!(layout.isSetH())) {
			layout.addNewH();
		}
		layout.getH().setVal(ratio);
	}

	public double getHeightRatio() {
		if (!(layout.isSetH())) {
			return 0.0;
		}
		return layout.getH().getVal();
	}

	public LayoutTarget getTarget() {
		if (!(layout.isSetLayoutTarget())) {
			return XDDFManualLayout.defaultLayoutTarget;
		}
		return null;
	}

	public void setTarget(LayoutTarget target) {
		if (!(layout.isSetLayoutTarget())) {
			layout.addNewLayoutTarget();
		}
	}

	public LayoutMode getXMode() {
		if (!(layout.isSetXMode())) {
			return XDDFManualLayout.defaultLayoutMode;
		}
		return null;
	}

	public void setXMode(LayoutMode mode) {
		if (!(layout.isSetXMode())) {
			layout.addNewXMode();
		}
	}

	public LayoutMode getYMode() {
		if (!(layout.isSetYMode())) {
			return XDDFManualLayout.defaultLayoutMode;
		}
		return null;
	}

	public void setYMode(LayoutMode mode) {
		if (!(layout.isSetYMode())) {
			layout.addNewYMode();
		}
	}

	public double getX() {
		if (!(layout.isSetX())) {
			return 0.0;
		}
		return layout.getX().getVal();
	}

	public void setX(double x) {
		if (!(layout.isSetX())) {
			layout.addNewX();
		}
		layout.getX().setVal(x);
	}

	public double getY() {
		if (!(layout.isSetY())) {
			return 0.0;
		}
		return layout.getY().getVal();
	}

	public void setY(double y) {
		if (!(layout.isSetY())) {
			layout.addNewY();
		}
		layout.getY().setVal(y);
	}

	public LayoutMode getWidthMode() {
		if (!(layout.isSetWMode())) {
			return XDDFManualLayout.defaultLayoutMode;
		}
		return null;
	}

	public void setWidthMode(LayoutMode mode) {
		if (!(layout.isSetWMode())) {
			layout.addNewWMode();
		}
	}

	public LayoutMode getHeightMode() {
		if (!(layout.isSetHMode())) {
			return XDDFManualLayout.defaultLayoutMode;
		}
		return null;
	}

	public void setHeightMode(LayoutMode mode) {
		if (!(layout.isSetHMode())) {
			layout.addNewHMode();
		}
	}

	private void initializeLayout(CTLayout ctLayout) {
		if (ctLayout.isSetManualLayout()) {
			this.layout = ctLayout.getManualLayout();
		}else {
			this.layout = ctLayout.addNewManualLayout();
		}
	}
}

