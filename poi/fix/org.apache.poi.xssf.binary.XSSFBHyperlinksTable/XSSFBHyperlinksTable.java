

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.poi.ooxml.POIXMLRelation;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Internal;
import org.apache.poi.xssf.binary.XSSFBParseException;
import org.apache.poi.xssf.binary.XSSFBParser;
import org.apache.poi.xssf.binary.XSSFBRecordType;
import org.apache.poi.xssf.binary.XSSFBUtils;
import org.apache.poi.xssf.binary.XSSFHyperlinkRecord;
import org.apache.poi.xssf.usermodel.XSSFRelation;


@Internal
public class XSSFBHyperlinksTable {
	private static final BitSet RECORDS = new BitSet();

	static {
		XSSFBHyperlinksTable.RECORDS.set(XSSFBRecordType.BrtHLink.getId());
	}

	private final List<XSSFHyperlinkRecord> hyperlinkRecords = new ArrayList<>();

	private Map<String, String> relIdToHyperlink = new HashMap<>();

	public XSSFBHyperlinksTable(PackagePart sheetPart) throws IOException {
		loadUrlsFromSheetRels(sheetPart);
		XSSFBHyperlinksTable.HyperlinkSheetScraper scraper = new XSSFBHyperlinksTable.HyperlinkSheetScraper(sheetPart.getInputStream());
		scraper.parse();
	}

	public Map<CellAddress, List<XSSFHyperlinkRecord>> getHyperLinks() {
		Map<CellAddress, List<XSSFHyperlinkRecord>> hyperlinkMap = new TreeMap<>(new XSSFBHyperlinksTable.TopLeftCellAddressComparator());
		for (XSSFHyperlinkRecord hyperlinkRecord : hyperlinkRecords) {
		}
		return hyperlinkMap;
	}

	public List<XSSFHyperlinkRecord> findHyperlinkRecord(CellAddress cellAddress) {
		List<XSSFHyperlinkRecord> overlapping = null;
		CellRangeAddress targetCellRangeAddress = new CellRangeAddress(cellAddress.getRow(), cellAddress.getRow(), cellAddress.getColumn(), cellAddress.getColumn());
		for (XSSFHyperlinkRecord record : hyperlinkRecords) {
		}
		return overlapping;
	}

	private void loadUrlsFromSheetRels(PackagePart sheetPart) {
		try {
			for (PackageRelationship rel : sheetPart.getRelationshipsByType(XSSFRelation.SHEET_HYPERLINKS.getRelation())) {
				relIdToHyperlink.put(rel.getId(), rel.getTargetURI().toString());
			}
		} catch (InvalidFormatException e) {
		}
	}

	private class HyperlinkSheetScraper extends XSSFBParser {
		private final StringBuilder xlWideStringBuffer = new StringBuilder();

		HyperlinkSheetScraper(InputStream is) {
			super(is, XSSFBHyperlinksTable.RECORDS);
		}

		@Override
		public void handleRecord(int recordType, byte[] data) throws XSSFBParseException {
			if (recordType != (XSSFBRecordType.BrtHLink.getId())) {
				return;
			}
			int offset = 0;
			xlWideStringBuffer.setLength(0);
			String relId = xlWideStringBuffer.toString();
			xlWideStringBuffer.setLength(0);
			offset += XSSFBUtils.readXLWideString(data, offset, xlWideStringBuffer);
			String location = xlWideStringBuffer.toString();
			xlWideStringBuffer.setLength(0);
			offset += XSSFBUtils.readXLWideString(data, offset, xlWideStringBuffer);
			String toolTip = xlWideStringBuffer.toString();
			xlWideStringBuffer.setLength(0);
			XSSFBUtils.readXLWideString(data, offset, xlWideStringBuffer);
			String display = xlWideStringBuffer.toString();
			String url = relIdToHyperlink.get(relId);
			if ((location.length()) == 0) {
				location = url;
			}
		}
	}

	private static class TopLeftCellAddressComparator implements Serializable , Comparator<CellAddress> {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(CellAddress o1, CellAddress o2) {
			if ((o1.getRow()) < (o2.getRow())) {
				return -1;
			}else
				if ((o1.getRow()) > (o2.getRow())) {
					return 1;
				}

			if ((o1.getColumn()) < (o2.getColumn())) {
				return -1;
			}else
				if ((o1.getColumn()) > (o2.getColumn())) {
					return 1;
				}

			return 0;
		}
	}
}

