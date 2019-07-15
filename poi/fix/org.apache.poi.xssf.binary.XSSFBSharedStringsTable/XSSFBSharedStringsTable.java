

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.util.Internal;
import org.apache.poi.xssf.binary.XSSFBParseException;
import org.apache.poi.xssf.binary.XSSFBParser;
import org.apache.poi.xssf.binary.XSSFBRecordType;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.SAXException;


@Internal
public class XSSFBSharedStringsTable implements SharedStrings {
	private int count;

	private int uniqueCount;

	private List<String> strings = new ArrayList<>();

	public XSSFBSharedStringsTable(OPCPackage pkg) throws IOException, SAXException {
	}

	XSSFBSharedStringsTable(PackagePart part) throws IOException, SAXException {
		readFrom(part.getInputStream());
	}

	private void readFrom(InputStream inputStream) throws IOException {
		XSSFBSharedStringsTable.SSTBinaryReader reader = new XSSFBSharedStringsTable.SSTBinaryReader(inputStream);
		reader.parse();
	}

	@org.apache.poi.util.Removal(version = "4.2")
	@Deprecated
	public List<String> getItems() {
		List<String> ret = new ArrayList<>(strings.size());
		ret.addAll(strings);
		return ret;
	}

	@org.apache.poi.util.Removal(version = "4.2")
	@Deprecated
	public String getEntryAt(int idx) {
		return strings.get(idx);
	}

	@Override
	public RichTextString getItemAt(int idx) {
		return new XSSFRichTextString(getEntryAt(idx));
	}

	@Override
	public int getCount() {
		return this.count;
	}

	@Override
	public int getUniqueCount() {
		return this.uniqueCount;
	}

	private class SSTBinaryReader extends XSSFBParser {
		SSTBinaryReader(InputStream is) {
			super(is);
		}

		@Override
		public void handleRecord(int recordType, byte[] data) throws XSSFBParseException {
			XSSFBRecordType type = XSSFBRecordType.lookup(recordType);
		}
	}
}

