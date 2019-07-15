

import org.apache.poi.hwpf.model.types.SEPAbstractType;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.SectionProperties;


public final class Section extends Range {
	private SectionProperties _props;

	public Object clone() throws CloneNotSupportedException {
		Section s = ((Section) (super.clone()));
		s._props = ((SectionProperties) (_props.clone()));
		return s;
	}

	public int getDistanceBetweenColumns() {
		return _props.getDxaColumns();
	}

	public int getMarginBottom() {
		return _props.getDyaBottom();
	}

	public int getMarginLeft() {
		return _props.getDxaLeft();
	}

	public int getMarginRight() {
		return _props.getDxaRight();
	}

	public int getMarginTop() {
		return _props.getDyaTop();
	}

	public int getNumColumns() {
		return (_props.getCcolM1()) + 1;
	}

	public int getPageHeight() {
		return _props.getYaPage();
	}

	public int getPageWidth() {
		return _props.getXaPage();
	}

	public void setMarginBottom(int marginWidth) {
		this._props.setDyaBottom(marginWidth);
	}

	public void setMarginLeft(int marginWidth) {
		this._props.setDxaLeft(marginWidth);
	}

	public void setMarginRight(int marginWidth) {
		this._props.setDxaRight(marginWidth);
	}

	public void setMarginTop(int marginWidth) {
		this._props.setDyaTop(marginWidth);
	}

	public boolean isColumnsEvenlySpaced() {
		return _props.getFEvenlySpaced();
	}

	public short getFootnoteRestartQualifier() {
		return _props.getRncFtn();
	}

	public int getFootnoteNumberingOffset() {
		return _props.getNFtn();
	}

	public int getFootnoteNumberingFormat() {
		return _props.getNfcFtnRef();
	}

	public short getEndnoteRestartQualifier() {
		return _props.getRncEdn();
	}

	public int getEndnoteNumberingOffset() {
		return _props.getNEdn();
	}

	public int getEndnoteNumberingFormat() {
		return _props.getNfcEdnRef();
	}

	@Override
	public String toString() {
		return ((("Section [" + (getStartOffset())) + "; ") + (getEndOffset())) + ")";
	}

	public int type() {
		return Range.TYPE_SECTION;
	}
}

