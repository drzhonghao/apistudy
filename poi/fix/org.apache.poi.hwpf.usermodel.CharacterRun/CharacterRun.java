

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.HWPFDocumentCore;
import org.apache.poi.hwpf.HWPFOldDocument;
import org.apache.poi.hwpf.model.FFData;
import org.apache.poi.hwpf.model.Ffn;
import org.apache.poi.hwpf.model.FontTable;
import org.apache.poi.hwpf.model.NilPICFAndBinData;
import org.apache.poi.hwpf.model.OldFontTable;
import org.apache.poi.hwpf.model.types.CHPAbstractType;
import org.apache.poi.hwpf.sprm.SprmBuffer;
import org.apache.poi.hwpf.usermodel.BorderCode;
import org.apache.poi.hwpf.usermodel.CharacterProperties;
import org.apache.poi.hwpf.usermodel.DateAndTime;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.ShadingDescriptor;


public final class CharacterRun extends Range implements Cloneable , org.apache.poi.wp.usermodel.CharacterRun {
	public static final short SPRM_FRMARKDEL = ((short) (2048));

	public static final short SPRM_FRMARK = 2049;

	public static final short SPRM_FFLDVANISH = 2050;

	public static final short SPRM_PICLOCATION = 27139;

	public static final short SPRM_IBSTRMARK = 18436;

	public static final short SPRM_DTTMRMARK = 26629;

	public static final short SPRM_FDATA = 2054;

	public static final short SPRM_SYMBOL = 27145;

	public static final short SPRM_FOLE2 = 2058;

	public static final short SPRM_HIGHLIGHT = 10764;

	public static final short SPRM_OBJLOCATION = 26638;

	public static final short SPRM_ISTD = 18992;

	public static final short SPRM_FBOLD = 2101;

	public static final short SPRM_FITALIC = 2102;

	public static final short SPRM_FSTRIKE = 2103;

	public static final short SPRM_FOUTLINE = 2104;

	public static final short SPRM_FSHADOW = 2105;

	public static final short SPRM_FSMALLCAPS = 2106;

	public static final short SPRM_FCAPS = 2107;

	public static final short SPRM_FVANISH = 2108;

	public static final short SPRM_KUL = 10814;

	public static final short SPRM_DXASPACE = ((short) (34880));

	public static final short SPRM_LID = 19009;

	public static final short SPRM_ICO = 10818;

	public static final short SPRM_HPS = 19011;

	public static final short SPRM_HPSPOS = 18501;

	public static final short SPRM_ISS = 10824;

	public static final short SPRM_HPSKERN = 18507;

	public static final short SPRM_YSRI = 18510;

	public static final short SPRM_RGFTCASCII = 19023;

	public static final short SPRM_RGFTCFAREAST = 19024;

	public static final short SPRM_RGFTCNOTFAREAST = 19025;

	public static final short SPRM_CHARSCALE = 18514;

	public static final short SPRM_FDSTRIKE = 10835;

	public static final short SPRM_FIMPRINT = 2132;

	public static final short SPRM_FSPEC = 2133;

	public static final short SPRM_FOBJ = 2134;

	public static final short SPRM_PROPRMARK = ((short) (51799));

	public static final short SPRM_FEMBOSS = 2136;

	public static final short SPRM_SFXTEXT = 10329;

	public static final short SPRM_DISPFLDRMARK = ((short) (51810));

	public static final short SPRM_IBSTRMARKDEL = 18531;

	public static final short SPRM_DTTMRMARKDEL = 26724;

	public static final short SPRM_BRC = 26725;

	public static final short SPRM_SHD = 18534;

	public static final short SPRM_IDSIRMARKDEL = 18535;

	public static final short SPRM_CPG = 18539;

	public static final short SPRM_NONFELID = 18541;

	public static final short SPRM_FELID = 18542;

	public static final short SPRM_IDCTHINT = 10351;

	protected short _istd;

	protected SprmBuffer _chpx;

	protected CharacterProperties _props;

	@SuppressWarnings("deprecation")
	public int type() {
		return Range.TYPE_CHARACTER;
	}

	public boolean isMarkedDeleted() {
		return _props.isFRMarkDel();
	}

	public void markDeleted(boolean mark) {
		_props.setFRMarkDel(mark);
		byte newVal = ((byte) ((mark) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FRMARKDEL, newVal);
	}

	public boolean isBold() {
		return _props.isFBold();
	}

	public void setBold(boolean bold) {
		_props.setFBold(bold);
		byte newVal = ((byte) ((bold) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FBOLD, newVal);
	}

	public boolean isItalic() {
		return _props.isFItalic();
	}

	public void setItalic(boolean italic) {
		_props.setFItalic(italic);
		byte newVal = ((byte) ((italic) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FITALIC, newVal);
	}

	public boolean isOutlined() {
		return _props.isFOutline();
	}

	public void setOutline(boolean outlined) {
		_props.setFOutline(outlined);
		byte newVal = ((byte) ((outlined) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FOUTLINE, newVal);
	}

	public boolean isFldVanished() {
		return _props.isFFldVanish();
	}

	public void setFldVanish(boolean fldVanish) {
		_props.setFFldVanish(fldVanish);
		byte newVal = ((byte) ((fldVanish) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FFLDVANISH, newVal);
	}

	public boolean isSmallCaps() {
		return _props.isFSmallCaps();
	}

	public void setSmallCaps(boolean smallCaps) {
		_props.setFSmallCaps(smallCaps);
		byte newVal = ((byte) ((smallCaps) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FSMALLCAPS, newVal);
	}

	public boolean isCapitalized() {
		return _props.isFCaps();
	}

	public void setCapitalized(boolean caps) {
		_props.setFCaps(caps);
		byte newVal = ((byte) ((caps) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FCAPS, newVal);
	}

	public boolean isVanished() {
		return _props.isFVanish();
	}

	public void setVanished(boolean vanish) {
		_props.setFVanish(vanish);
		byte newVal = ((byte) ((vanish) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FVANISH, newVal);
	}

	public boolean isMarkedInserted() {
		return _props.isFRMark();
	}

	public void markInserted(boolean mark) {
		_props.setFRMark(mark);
		byte newVal = ((byte) ((mark) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FRMARK, newVal);
	}

	public boolean isStrikeThrough() {
		return _props.isFStrike();
	}

	public void setStrikeThrough(boolean strike) {
		strikeThrough(strike);
	}

	public void strikeThrough(boolean strike) {
		_props.setFStrike(strike);
		byte newVal = ((byte) ((strike) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FSTRIKE, newVal);
	}

	public boolean isShadowed() {
		return _props.isFShadow();
	}

	public void setShadow(boolean shadow) {
		_props.setFShadow(shadow);
		byte newVal = ((byte) ((shadow) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FSHADOW, newVal);
	}

	public boolean isEmbossed() {
		return _props.isFEmboss();
	}

	public void setEmbossed(boolean emboss) {
		_props.setFEmboss(emboss);
		byte newVal = ((byte) ((emboss) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FEMBOSS, newVal);
	}

	public boolean isImprinted() {
		return _props.isFImprint();
	}

	public void setImprinted(boolean imprint) {
		_props.setFImprint(imprint);
		byte newVal = ((byte) ((imprint) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FIMPRINT, newVal);
	}

	public boolean isDoubleStrikeThrough() {
		return _props.isFDStrike();
	}

	public void setDoubleStrikethrough(boolean dstrike) {
		_props.setFDStrike(dstrike);
		byte newVal = ((byte) ((dstrike) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FDSTRIKE, newVal);
	}

	public void setFtcAscii(int ftcAscii) {
		_props.setFtcAscii(ftcAscii);
		_chpx.updateSprm(CharacterRun.SPRM_RGFTCASCII, ((short) (ftcAscii)));
	}

	public void setFtcFE(int ftcFE) {
		_props.setFtcFE(ftcFE);
		_chpx.updateSprm(CharacterRun.SPRM_RGFTCFAREAST, ((short) (ftcFE)));
	}

	public void setFtcOther(int ftcOther) {
		_props.setFtcOther(ftcOther);
		_chpx.updateSprm(CharacterRun.SPRM_RGFTCNOTFAREAST, ((short) (ftcOther)));
	}

	public int getFontSize() {
		return _props.getHps();
	}

	public void setFontSize(int halfPoints) {
		_props.setHps(halfPoints);
		_chpx.updateSprm(CharacterRun.SPRM_HPS, ((short) (halfPoints)));
	}

	public int getCharacterSpacing() {
		return _props.getDxaSpace();
	}

	public void setCharacterSpacing(int twips) {
		_props.setDxaSpace(twips);
		_chpx.updateSprm(CharacterRun.SPRM_DXASPACE, twips);
	}

	public short getSubSuperScriptIndex() {
		return _props.getIss();
	}

	public void setSubSuperScriptIndex(short iss) {
		_props.setDxaSpace(iss);
		_chpx.updateSprm(CharacterRun.SPRM_DXASPACE, iss);
	}

	public int getUnderlineCode() {
		return _props.getKul();
	}

	public void setUnderlineCode(int kul) {
		_props.setKul(((byte) (kul)));
		_chpx.updateSprm(CharacterRun.SPRM_KUL, ((byte) (kul)));
	}

	public int getColor() {
		return _props.getIco();
	}

	public void setColor(int color) {
		_props.setIco(((byte) (color)));
		_chpx.updateSprm(CharacterRun.SPRM_ICO, ((byte) (color)));
	}

	public int getVerticalOffset() {
		return _props.getHpsPos();
	}

	public void setVerticalOffset(int hpsPos) {
		_props.setHpsPos(((short) (hpsPos)));
		_chpx.updateSprm(CharacterRun.SPRM_HPSPOS, ((byte) (hpsPos)));
	}

	public int getKerning() {
		return _props.getHpsKern();
	}

	public void setKerning(int kern) {
		_props.setHpsKern(kern);
		_chpx.updateSprm(CharacterRun.SPRM_HPSKERN, ((short) (kern)));
	}

	public boolean isHighlighted() {
		return _props.isFHighlight();
	}

	public byte getHighlightedColor() {
		return _props.getIcoHighlight();
	}

	public void setHighlighted(byte color) {
		_props.setFHighlight(true);
		_props.setIcoHighlight(color);
		_chpx.updateSprm(CharacterRun.SPRM_HIGHLIGHT, color);
	}

	public String getFontName() {
		if ((_doc) instanceof HWPFOldDocument) {
			return ((HWPFOldDocument) (_doc)).getOldFontTable().getMainFont(_props.getFtcAscii());
		}
		if ((_doc.getFontTable()) == null)
			return null;

		return _doc.getFontTable().getMainFont(_props.getFtcAscii());
	}

	public boolean isSpecialCharacter() {
		return _props.isFSpec();
	}

	public void setSpecialCharacter(boolean spec) {
		_props.setFSpec(spec);
		byte newVal = ((byte) ((spec) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FSPEC, newVal);
	}

	public boolean isObj() {
		return _props.isFObj();
	}

	public void setObj(boolean obj) {
		_props.setFObj(obj);
		byte newVal = ((byte) ((obj) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FOBJ, newVal);
	}

	public int getPicOffset() {
		return _props.getFcPic();
	}

	public void setPicOffset(int offset) {
		_props.setFcPic(offset);
		_chpx.updateSprm(CharacterRun.SPRM_PICLOCATION, offset);
	}

	public boolean isData() {
		return _props.isFData();
	}

	public void setData(boolean data) {
		_props.setFData(data);
		byte newVal = ((byte) ((data) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FOBJ, newVal);
	}

	public boolean isOle2() {
		return _props.isFOle2();
	}

	public void setOle2(boolean ole) {
		_props.setFOle2(ole);
		byte newVal = ((byte) ((ole) ? 1 : 0));
		_chpx.updateSprm(CharacterRun.SPRM_FOBJ, newVal);
	}

	public int getObjOffset() {
		return _props.getFcObj();
	}

	public void setObjOffset(int obj) {
		_props.setFcObj(obj);
		_chpx.updateSprm(CharacterRun.SPRM_OBJLOCATION, obj);
	}

	public int getIco24() {
		return _props.getIco24();
	}

	public void setIco24(int colour24) {
		_props.setIco24(colour24);
	}

	public Object clone() throws CloneNotSupportedException {
		CharacterRun cp = ((CharacterRun) (super.clone()));
		cp._props.setDttmRMark(((DateAndTime) (_props.getDttmRMark().clone())));
		cp._props.setDttmRMarkDel(((DateAndTime) (_props.getDttmRMarkDel().clone())));
		cp._props.setDttmPropRMark(((DateAndTime) (_props.getDttmPropRMark().clone())));
		cp._props.setDttmDispFldRMark(((DateAndTime) (_props.getDttmDispFldRMark().clone())));
		cp._props.setXstDispFldRMark(_props.getXstDispFldRMark().clone());
		cp._props.setShd(_props.getShd().clone());
		return cp;
	}

	public boolean isSymbol() {
		return (isSpecialCharacter()) && (text().equals("("));
	}

	public char getSymbolCharacter() {
		if (isSymbol()) {
			return ((char) (_props.getXchSym()));
		}else
			throw new IllegalStateException("Not a symbol CharacterRun");

	}

	public Ffn getSymbolFont() {
		if (isSymbol()) {
			if ((_doc.getFontTable()) == null)
				return null;

			Ffn[] fontNames = _doc.getFontTable().getFontNames();
			if ((fontNames.length) <= (_props.getFtcSym()))
				return null;

			return fontNames[_props.getFtcSym()];
		}else
			throw new IllegalStateException("Not a symbol CharacterRun");

	}

	public BorderCode getBorder() {
		return _props.getBrc();
	}

	public int getLanguageCode() {
		return _props.getLidDefault();
	}

	public short getStyleIndex() {
		return _istd;
	}

	public String toString() {
		String text = text();
		return (("CharacterRun of " + (text.length())) + " characters - ") + text;
	}

	public String[] getDropDownListValues() {
		if ((getDocument()) instanceof HWPFDocument) {
			char c = _text.charAt(_start);
			if (c == 1) {
				NilPICFAndBinData data = new NilPICFAndBinData(((HWPFDocument) (getDocument())).getDataStream(), getPicOffset());
				FFData ffData = new FFData(data.getBinData(), 0);
				return ffData.getDropList();
			}
		}
		return null;
	}

	public Integer getDropDownListDefaultItemIndex() {
		if ((getDocument()) instanceof HWPFDocument) {
			char c = _text.charAt(_start);
			if (c == 1) {
				NilPICFAndBinData data = new NilPICFAndBinData(((HWPFDocument) (getDocument())).getDataStream(), getPicOffset());
				FFData ffData = new FFData(data.getBinData(), 0);
				return Integer.valueOf(ffData.getDefaultDropDownItemIndex());
			}
		}
		return null;
	}
}

