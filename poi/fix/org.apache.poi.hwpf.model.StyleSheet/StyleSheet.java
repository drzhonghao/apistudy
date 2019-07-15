

import java.io.IOException;
import java.io.OutputStream;
import org.apache.poi.hwpf.model.StyleDescription;
import org.apache.poi.hwpf.sprm.CharacterSprmUncompressor;
import org.apache.poi.hwpf.sprm.ParagraphSprmUncompressor;
import org.apache.poi.hwpf.usermodel.CharacterProperties;
import org.apache.poi.hwpf.usermodel.ParagraphProperties;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;


@Internal
public final class StyleSheet {
	public static final int NIL_STYLE = 4095;

	@Deprecated
	private static final ParagraphProperties NIL_PAP = new ParagraphProperties();

	@Deprecated
	private static final CharacterProperties NIL_CHP = new CharacterProperties();

	private static final byte[] NIL_CHPX = new byte[]{  };

	private static final byte[] NIL_PAPX = new byte[]{ 0, 0 };

	private int _cbStshi;

	StyleDescription[] _styleDescriptions;

	public StyleSheet(byte[] tableStream, int offset) {
		int startOffset = offset;
		_cbStshi = LittleEndian.getShort(tableStream, offset);
		offset += LittleEndian.SHORT_SIZE;
		offset = (startOffset + (LittleEndian.SHORT_SIZE)) + (_cbStshi);
		for (int x = 0; x < (_styleDescriptions.length); x++) {
			if ((_styleDescriptions[x]) != null) {
				createPap(x);
				createChp(x);
			}
		}
	}

	public void writeTo(OutputStream out) throws IOException {
		int offset = 0;
		this._cbStshi = 18;
		byte[] buf = new byte[(_cbStshi) + 2];
		LittleEndian.putUShort(buf, offset, ((short) (_cbStshi)));
		offset += LittleEndian.SHORT_SIZE;
		out.write(buf);
		byte[] sizeHolder = new byte[2];
		for (int x = 0; x < (_styleDescriptions.length); x++) {
			if ((_styleDescriptions[x]) != null) {
				byte[] std = _styleDescriptions[x].toByteArray();
				LittleEndian.putShort(sizeHolder, 0, ((short) ((std.length) + ((std.length) % 2))));
				out.write(sizeHolder);
				out.write(std);
				if (((std.length) % 2) == 1) {
					out.write('\u0000');
				}
			}else {
				sizeHolder[0] = 0;
				sizeHolder[1] = 0;
				out.write(sizeHolder);
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof StyleSheet))
			return false;

		StyleSheet ss = ((StyleSheet) (o));
		for (int i = 0; i < (_styleDescriptions.length); i++) {
			StyleDescription tsd = this._styleDescriptions[i];
			StyleDescription osd = ss._styleDescriptions[i];
			if ((tsd == null) && (osd == null))
				continue;

			if (((tsd == null) || (osd == null)) || (!(osd.equals(tsd))))
				return false;

		}
		return true;
	}

	@Override
	public int hashCode() {
		assert false : "hashCode not designed";
		return 42;
	}

	@Deprecated
	private void createPap(int istd) {
		StyleDescription sd = _styleDescriptions[istd];
		ParagraphProperties pap = sd.getPAP();
		byte[] papx = sd.getPAPX();
		int baseIndex = sd.getBaseStyle();
		if ((pap == null) && (papx != null)) {
			ParagraphProperties parentPAP = new ParagraphProperties();
			if (baseIndex != (StyleSheet.NIL_STYLE)) {
				parentPAP = _styleDescriptions[baseIndex].getPAP();
				if (parentPAP == null) {
					if (baseIndex == istd) {
						throw new IllegalStateException((("Pap style " + istd) + " claimed to have itself as its parent, which isn't allowed"));
					}
					createPap(baseIndex);
					parentPAP = _styleDescriptions[baseIndex].getPAP();
				}
			}
			if (parentPAP == null) {
				parentPAP = new ParagraphProperties();
			}
			pap = ParagraphSprmUncompressor.uncompressPAP(parentPAP, papx, 2);
		}
	}

	@Deprecated
	private void createChp(int istd) {
		StyleDescription sd = _styleDescriptions[istd];
		CharacterProperties chp = sd.getCHP();
		byte[] chpx = sd.getCHPX();
		int baseIndex = sd.getBaseStyle();
		if (baseIndex == istd) {
			baseIndex = StyleSheet.NIL_STYLE;
		}
		if ((chp == null) && (chpx != null)) {
			CharacterProperties parentCHP = new CharacterProperties();
			if (baseIndex != (StyleSheet.NIL_STYLE)) {
				parentCHP = _styleDescriptions[baseIndex].getCHP();
				if (parentCHP == null) {
					createChp(baseIndex);
					parentCHP = _styleDescriptions[baseIndex].getCHP();
				}
				if (parentCHP == null) {
					parentCHP = new CharacterProperties();
				}
			}
			chp = CharacterSprmUncompressor.uncompressCHP(parentCHP, chpx, 0);
		}
	}

	public int numStyles() {
		return _styleDescriptions.length;
	}

	public StyleDescription getStyleDescription(int styleIndex) {
		return _styleDescriptions[styleIndex];
	}

	@Deprecated
	public CharacterProperties getCharacterStyle(int styleIndex) {
		if (styleIndex == (StyleSheet.NIL_STYLE)) {
			return StyleSheet.NIL_CHP;
		}
		if (styleIndex >= (_styleDescriptions.length)) {
			return StyleSheet.NIL_CHP;
		}
		if (styleIndex == (-1)) {
			return StyleSheet.NIL_CHP;
		}
		return (_styleDescriptions[styleIndex]) != null ? _styleDescriptions[styleIndex].getCHP() : StyleSheet.NIL_CHP;
	}

	@Deprecated
	public ParagraphProperties getParagraphStyle(int styleIndex) {
		if (styleIndex == (StyleSheet.NIL_STYLE)) {
			return StyleSheet.NIL_PAP;
		}
		if (styleIndex >= (_styleDescriptions.length)) {
			return StyleSheet.NIL_PAP;
		}
		if (styleIndex == (-1)) {
			return StyleSheet.NIL_PAP;
		}
		if ((_styleDescriptions[styleIndex]) == null) {
			return StyleSheet.NIL_PAP;
		}
		if ((_styleDescriptions[styleIndex].getPAP()) == null) {
			return StyleSheet.NIL_PAP;
		}
		return _styleDescriptions[styleIndex].getPAP();
	}

	public byte[] getCHPX(int styleIndex) {
		if (styleIndex == (StyleSheet.NIL_STYLE)) {
			return StyleSheet.NIL_CHPX;
		}
		if (styleIndex >= (_styleDescriptions.length)) {
			return StyleSheet.NIL_CHPX;
		}
		if (styleIndex == (-1)) {
			return StyleSheet.NIL_CHPX;
		}
		if ((_styleDescriptions[styleIndex]) == null) {
			return StyleSheet.NIL_CHPX;
		}
		if ((_styleDescriptions[styleIndex].getCHPX()) == null) {
			return StyleSheet.NIL_CHPX;
		}
		return _styleDescriptions[styleIndex].getCHPX();
	}

	public byte[] getPAPX(int styleIndex) {
		if (styleIndex == (StyleSheet.NIL_STYLE)) {
			return StyleSheet.NIL_PAPX;
		}
		if (styleIndex >= (_styleDescriptions.length)) {
			return StyleSheet.NIL_PAPX;
		}
		if (styleIndex == (-1)) {
			return StyleSheet.NIL_PAPX;
		}
		if ((_styleDescriptions[styleIndex]) == null) {
			return StyleSheet.NIL_PAPX;
		}
		if ((_styleDescriptions[styleIndex].getPAPX()) == null) {
			return StyleSheet.NIL_PAPX;
		}
		return _styleDescriptions[styleIndex].getPAPX();
	}
}

