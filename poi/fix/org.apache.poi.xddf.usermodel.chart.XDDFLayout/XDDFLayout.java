

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.chart.XDDFChartExtensionList;
import org.apache.poi.xddf.usermodel.chart.XDDFManualLayout;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLayout;

import static org.openxmlformats.schemas.drawingml.x2006.chart.CTLayout.Factory.newInstance;


@Beta
public class XDDFLayout {
	private CTLayout layout;

	public XDDFLayout() {
		this(newInstance());
	}

	@Internal
	protected XDDFLayout(CTLayout layout) {
		this.layout = layout;
	}

	@Internal
	protected CTLayout getXmlObject() {
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

	public void setManualLayout(XDDFManualLayout manual) {
		if (manual == null) {
			if (layout.isSetManualLayout()) {
				layout.unsetManualLayout();
			}
		}else {
		}
	}

	public XDDFManualLayout getManualLayout() {
		if (layout.isSetManualLayout()) {
			return new XDDFManualLayout(layout);
		}else {
			return null;
		}
	}
}

