

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFChartExtensionList;
import org.apache.poi.xddf.usermodel.chart.XDDFLayout;
import org.apache.poi.xddf.usermodel.chart.XDDFLegendEntry;
import org.apache.poi.xddf.usermodel.chart.XDDFManualLayout;
import org.apache.poi.xddf.usermodel.text.TextContainer;
import org.apache.poi.xddf.usermodel.text.XDDFTextBody;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLayout;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLegend;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLegendPos;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties;


@Beta
public final class XDDFChartLegend implements TextContainer {
	private CTLegend legend;

	public XDDFChartLegend(CTChart ctChart) {
		this.legend = (ctChart.isSetLegend()) ? ctChart.getLegend() : ctChart.addNewLegend();
		setDefaults();
	}

	private void setDefaults() {
		if (!(legend.isSetOverlay())) {
			legend.addNewOverlay();
		}
		legend.getOverlay().setVal(false);
	}

	@org.apache.poi.util.Internal
	protected CTLegend getXmlObject() {
		return legend;
	}

	@org.apache.poi.util.Internal
	public CTShapeProperties getShapeProperties() {
		if (legend.isSetSpPr()) {
			return legend.getSpPr();
		}else {
			return null;
		}
	}

	@org.apache.poi.util.Internal
	public void setShapeProperties(CTShapeProperties properties) {
		if (properties == null) {
			if (legend.isSetSpPr()) {
				legend.unsetSpPr();
			}
		}else {
			legend.setSpPr(properties);
		}
	}

	public XDDFTextBody getTextBody() {
		if (legend.isSetTxPr()) {
			return new XDDFTextBody(this, legend.getTxPr());
		}else {
			return null;
		}
	}

	public void setTextBody(XDDFTextBody body) {
		if (body == null) {
			if (legend.isSetTxPr()) {
				legend.unsetTxPr();
			}
		}else {
			legend.setTxPr(body.getXmlObject());
		}
	}

	public XDDFLegendEntry addEntry() {
		return null;
	}

	public XDDFLegendEntry getEntry(int index) {
		return null;
	}

	public List<XDDFLegendEntry> getEntries() {
		return null;
	}

	public void setExtensionList(XDDFChartExtensionList list) {
		if (list == null) {
			if (legend.isSetExtLst()) {
				legend.unsetExtLst();
			}
		}else {
			legend.setExtLst(list.getXmlObject());
		}
	}

	public XDDFChartExtensionList getExtensionList() {
		if (legend.isSetExtLst()) {
		}else {
			return null;
		}
		return null;
	}

	public void setLayout(XDDFLayout layout) {
		if (layout == null) {
			if (legend.isSetLayout()) {
				legend.unsetLayout();
			}
		}else {
		}
	}

	public XDDFLayout getLayout() {
		if (legend.isSetLayout()) {
		}else {
			return null;
		}
		return null;
	}

	public void setPosition(LegendPosition position) {
		if (!(legend.isSetLegendPos())) {
			legend.addNewLegendPos();
		}
	}

	public LegendPosition getPosition() {
		if (legend.isSetLegendPos()) {
		}else {
			return LegendPosition.RIGHT;
		}
		return null;
	}

	public XDDFManualLayout getOrAddManualLayout() {
		if (!(legend.isSetLayout())) {
			legend.addNewLayout();
		}
		return new XDDFManualLayout(legend.getLayout());
	}

	public boolean isOverlay() {
		return legend.getOverlay().getVal();
	}

	public void setOverlay(boolean value) {
		legend.getOverlay().setVal(value);
	}

	public <R> Optional<R> findDefinedParagraphProperty(Function<CTTextParagraphProperties, Boolean> isSet, Function<CTTextParagraphProperties, R> getter) {
		return Optional.empty();
	}

	public <R> Optional<R> findDefinedRunProperty(Function<CTTextCharacterProperties, Boolean> isSet, Function<CTTextCharacterProperties, R> getter) {
		return Optional.empty();
	}
}

