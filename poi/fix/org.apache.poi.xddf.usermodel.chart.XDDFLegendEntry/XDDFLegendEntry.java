

import java.util.Optional;
import java.util.function.Function;
import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.chart.XDDFChartExtensionList;
import org.apache.poi.xddf.usermodel.text.TextContainer;
import org.apache.poi.xddf.usermodel.text.XDDFTextBody;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLegendEntry;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties;


@Beta
public class XDDFLegendEntry implements TextContainer {
	private CTLegendEntry entry;

	@Internal
	protected XDDFLegendEntry(CTLegendEntry entry) {
		this.entry = entry;
	}

	@Internal
	protected CTLegendEntry getXmlObject() {
		return entry;
	}

	public XDDFTextBody getTextBody() {
		if (entry.isSetTxPr()) {
			return new XDDFTextBody(this, entry.getTxPr());
		}else {
			return null;
		}
	}

	public void setTextBody(XDDFTextBody body) {
		if (body == null) {
			if (entry.isSetTxPr()) {
				entry.unsetTxPr();
			}
		}else {
			entry.setTxPr(body.getXmlObject());
		}
	}

	public boolean getDelete() {
		if (entry.isSetDelete()) {
			return entry.getDelete().getVal();
		}else {
			return false;
		}
	}

	public void setDelete(Boolean delete) {
		if (delete == null) {
			if (entry.isSetDelete()) {
				entry.unsetDelete();
			}
		}else {
			if (entry.isSetDelete()) {
				entry.getDelete().setVal(delete);
			}else {
				entry.addNewDelete().setVal(delete);
			}
		}
	}

	public long getIndex() {
		return entry.getIdx().getVal();
	}

	public void setIndex(long index) {
		entry.getIdx().setVal(index);
	}

	public void setExtensionList(XDDFChartExtensionList list) {
		if (list == null) {
			if (entry.isSetExtLst()) {
				entry.unsetExtLst();
			}
		}else {
			entry.setExtLst(list.getXmlObject());
		}
	}

	public XDDFChartExtensionList getExtensionList() {
		if (entry.isSetExtLst()) {
		}else {
			return null;
		}
		return null;
	}

	public <R> Optional<R> findDefinedParagraphProperty(Function<CTTextParagraphProperties, Boolean> isSet, Function<CTTextParagraphProperties, R> getter) {
		return Optional.empty();
	}

	public <R> Optional<R> findDefinedRunProperty(Function<CTTextCharacterProperties, Boolean> isSet, Function<CTTextCharacterProperties, R> getter) {
		return Optional.empty();
	}
}

