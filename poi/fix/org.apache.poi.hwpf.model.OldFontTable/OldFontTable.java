

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.hwpf.model.OldFfn;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


@Internal
public final class OldFontTable {
	private static final POILogger _logger = POILogFactory.getLogger(OldFontTable.class);

	private final OldFfn[] _fontNames;

	public OldFontTable(byte[] buf, int offset, int length) {
		List<OldFfn> ffns = new ArrayList<>();
		int fontTableLength = LittleEndian.getShort(buf, offset);
		int endOfTableOffset = offset + length;
		int startOffset = offset + (LittleEndian.SHORT_SIZE);
		while (true) {
		} 
	}

	public OldFfn[] getFontNames() {
		return _fontNames;
	}

	public String getMainFont(int chpFtc) {
		if (chpFtc >= (_fontNames.length)) {
			OldFontTable._logger.log(POILogger.INFO, "Mismatch in chpFtc with stringCount");
			return null;
		}
		return _fontNames[chpFtc].getMainFontName();
	}

	@Override
	public String toString() {
		return (("OldFontTable{" + "_fontNames=") + (Arrays.toString(_fontNames))) + '}';
	}
}

