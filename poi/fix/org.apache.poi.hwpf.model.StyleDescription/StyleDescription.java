

import java.util.Arrays;
import org.apache.poi.hwpf.model.UPX;
import org.apache.poi.hwpf.usermodel.CharacterProperties;
import org.apache.poi.hwpf.usermodel.ParagraphProperties;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StringUtil;


@Internal
public final class StyleDescription {
	private static final POILogger logger = POILogFactory.getLogger(StyleDescription.class);

	private static final int MAX_RECORD_LENGTH = 100000;

	private static final int PARAGRAPH_STYLE = 1;

	private static final int CHARACTER_STYLE = 2;

	private int _baseLength;

	UPX[] _upxs;

	String _name;

	@Deprecated
	ParagraphProperties _pap;

	@Deprecated
	CharacterProperties _chp;

	public StyleDescription() {
	}

	public StyleDescription(byte[] std, int baseLength, int offset, boolean word9) {
		_baseLength = baseLength;
		int nameStart = offset + baseLength;
		boolean readStdfPost2000 = false;
		if (baseLength == 18) {
			readStdfPost2000 = true;
		}else
			if (baseLength == 10) {
				readStdfPost2000 = false;
			}else {
				StyleDescription.logger.log(POILogger.WARN, "Style definition has non-standard size of ", Integer.valueOf(baseLength));
			}

		if (readStdfPost2000) {
		}
		int nameLength = 0;
		int multiplier = 1;
		if (word9) {
			nameLength = LittleEndian.getShort(std, nameStart);
			multiplier = 2;
			nameStart += LittleEndian.SHORT_SIZE;
		}else {
			nameLength = std[nameStart];
		}
		_name = StringUtil.getFromUnicodeLE(std, nameStart, ((nameLength * multiplier) / 2));
		int varOffset = ((nameLength + 1) * multiplier) + nameStart;
	}

	public int getBaseStyle() {
		return 0;
	}

	public byte[] getCHPX() {
		return null;
	}

	public byte[] getPAPX() {
		return null;
	}

	@Deprecated
	public ParagraphProperties getPAP() {
		return _pap;
	}

	@Deprecated
	public CharacterProperties getCHP() {
		return _chp;
	}

	@Deprecated
	void setPAP(ParagraphProperties pap) {
		_pap = pap;
	}

	@Deprecated
	void setCHP(CharacterProperties chp) {
		_chp = chp;
	}

	public String getName() {
		return _name;
	}

	public byte[] toByteArray() {
		int size = ((_baseLength) + 2) + (((_name.length()) + 1) * 2);
		size += (_upxs[0].size()) + 2;
		for (int x = 1; x < (_upxs.length); x++) {
			size += (_upxs[(x - 1)].size()) % 2;
			size += (_upxs[x].size()) + 2;
		}
		byte[] buf = new byte[size];
		int offset = _baseLength;
		char[] letters = _name.toCharArray();
		LittleEndian.putShort(buf, _baseLength, ((short) (letters.length)));
		offset += LittleEndian.SHORT_SIZE;
		for (int x = 0; x < (letters.length); x++) {
			LittleEndian.putShort(buf, offset, ((short) (letters[x])));
			offset += LittleEndian.SHORT_SIZE;
		}
		offset += LittleEndian.SHORT_SIZE;
		for (int x = 0; x < (_upxs.length); x++) {
			short upxSize = ((short) (_upxs[x].size()));
			LittleEndian.putShort(buf, offset, upxSize);
			offset += LittleEndian.SHORT_SIZE;
			System.arraycopy(_upxs[x].getUPX(), 0, buf, offset, upxSize);
			offset += upxSize + (upxSize % 2);
		}
		return buf;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((_name) == null ? 0 : _name.hashCode());
		result = (prime * result) + (Arrays.hashCode(_upxs));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ((this) == obj)
			return true;

		if (obj == null)
			return false;

		if ((getClass()) != (obj.getClass()))
			return false;

		StyleDescription other = ((StyleDescription) (obj));
		if ((_name) == null) {
			if ((other._name) != null)
				return false;

		}else
			if (!(_name.equals(other._name)))
				return false;


		if (!(Arrays.equals(_upxs, other._upxs)))
			return false;

		return true;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("[STD]: '");
		result.append(_name);
		result.append("'");
		for (UPX upx : _upxs) {
			result.append(("\nUPX:\t" + upx).replaceAll("\n", "\n    "));
		}
		return result.toString();
	}
}

